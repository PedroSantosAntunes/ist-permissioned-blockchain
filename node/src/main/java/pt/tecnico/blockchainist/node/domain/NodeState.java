package pt.tecnico.blockchainist.node.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Pattern;

import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import pt.tecnico.blockchainist.auth.AuthInfo;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;
import pt.tecnico.blockchainist.status.InternalResponseStatus;

public class NodeState {
    String organization;
    
    public record ReadResult(long balance, int blockNumber) {}
    
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private final Map<String, Wallet> wallets = new ConcurrentHashMap<>();
    private final Map<String, CompletableFuture<InternalResponseStatus>> pendingTransactions = new ConcurrentHashMap<>();
    private final Map<String, InternalResponseStatus> completedTransactions = new ConcurrentHashMap<>();
    private final ReentrantReadWriteLock completedTransactionsLock = new ReentrantReadWriteLock();

    private int node_block_counter = 0;
    private final ArrayList<BlockRecord> blocks = new ArrayList<BlockRecord>();
    private final ReentrantReadWriteLock blockCompleted = new ReentrantReadWriteLock();
    private final Map<Integer, CompletableFuture<Void>> pendingBlock = new ConcurrentHashMap<>();
    private final Object blocksLock = new Object();

    private final Map<String, PendingWalletTransactions> pendingWalletTransactions = new ConcurrentHashMap<>();

    public static final String BC_WALLET = "bc";
    public static final String BC_NAME = "BC";
    public static final long BC_INIT_BALANCE = 1000L;

    private final NodeSequencerService sequencer;

    public NodeState(String org, NodeSequencerService sequencer) {
        
        Wallet wallet = new Wallet(BC_WALLET, BC_NAME, BC_INIT_BALANCE);
        wallets.put(BC_WALLET, wallet);

        this.organization = org;
    
        this.sequencer = sequencer;
    }
    
    public InternalResponseStatus createWallet(String uuid, String userId, String walletId) {
        CompletableFuture<InternalResponseStatus> future;

        InternalResponseStatus isOrgUser = verifyUserOrganization(userId);
        if (isOrgUser != InternalResponseStatus.OK) { return InternalResponseStatus.WRONG_ORGANIZATION; }

        completedTransactionsLock.readLock().lock();
        try {
            InternalResponseStatus completed = completedTransactions.get(uuid);
            if (completed != null) {
                return completed;
            }

            InternalResponseStatus argsInternalResponseStatus = validateCreateWalletArgs(userId, walletId);

            if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

            future = pendingTransactions.computeIfAbsent(uuid, k -> new CompletableFuture<>());
        } finally {
            completedTransactionsLock.readLock().unlock();
        }

        sequencer.broadcastCreateWallet(uuid, userId, walletId);

        try {
            Debug.log("\n-----\nNode: waiting for create wallet transaction to be executed!\n");
            return future.get();
        } catch (ExecutionException | InterruptedException e ) {
            return InternalResponseStatus.UNKNOWN;
        }  
    }

    public InternalResponseStatus deleteWallet(String uuid, String userId, String walletId) {
        CompletableFuture<InternalResponseStatus> future;

        InternalResponseStatus isOrgUser = verifyUserOrganization(userId);
        if (isOrgUser != InternalResponseStatus.OK) { return InternalResponseStatus.WRONG_ORGANIZATION; }

        completedTransactionsLock.readLock().lock();
        try {
            InternalResponseStatus completed = completedTransactions.get(uuid);
            if (completed != null) {
                return completed;
            }

            InternalResponseStatus argsInternalResponseStatus = validateDeleteWalletArgs(userId, walletId);
            if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

            PendingWalletTransactions pwt = pendingWalletTransactions.computeIfAbsent(walletId, k -> new PendingWalletTransactions());
            pwt.getLock().lock();
            try {
                pwt.incrementDelete();
            } finally {
                pwt.getLock().unlock();
            }

            future = pendingTransactions.computeIfAbsent(uuid, k -> new CompletableFuture<>());
        } finally {
            completedTransactionsLock.readLock().unlock();
        }

        sequencer.broadcastDeleteWallet(uuid, userId, walletId);

        try {
            Debug.log("\n-----\nNode: waiting for delete wallet transaction to be executed!\n");
            return future.get();
        } catch (ExecutionException | InterruptedException e ) {
            return InternalResponseStatus.UNKNOWN;
        } 
    }

    public InternalResponseStatus transfer(String uuid, String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
        CompletableFuture<InternalResponseStatus> future;

        InternalResponseStatus isOrgUser = verifyUserOrganization(srcUserId);
        if (isOrgUser != InternalResponseStatus.OK) { return InternalResponseStatus.WRONG_ORGANIZATION; }

        InternalResponseStatus argsInternalResponseStatus = validateTransferArgs(uuid, srcUserId, srcWalletId, dstWalletId, amount);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        Wallet srcWallet = this.wallets.get(srcWalletId);
        Wallet dstWallet = this.wallets.get(dstWalletId);

        Wallet first = srcWalletId.compareTo(dstWalletId) <= 0
            ? srcWallet
            : dstWallet;

        Wallet second = srcWalletId.compareTo(dstWalletId) <= 0
            ? dstWallet
            : srcWallet;

        if (srcWallet != null && dstWallet != null) {
            first.writeLock().lock();
            second.writeLock().lock();

            InternalResponseStatus impossibleResponseStatus = validateImpossibleTransfer(srcUserId, srcWallet, dstWallet, amount);
            if (impossibleResponseStatus != InternalResponseStatus.OK) {
                Debug.log("Optimized: Impossible transfer.\n");
                second.writeLock().unlock();
                first.writeLock().unlock();
                return impossibleResponseStatus;
            }

            InternalResponseStatus executedResponseStatus = validateExecuteTransfer(srcWallet, dstWallet, amount);
            if (executedResponseStatus == InternalResponseStatus.EXECUTED_LOCALY) {
                completedTransactionsLock.writeLock().lock();

                if (completedTransactions.containsKey(uuid)) {
                    System.err.println("Duplicate transaction: " + uuid);
                    completedTransactionsLock.writeLock().unlock();
                    second.writeLock().unlock();
                    first.writeLock().unlock();
                    return InternalResponseStatus.DUPLICATE_TRANSACTION;
                }

                srcWallet.setBalance(srcWallet.getBalance() - amount);
                dstWallet.setBalance(dstWallet.getBalance() + amount);
                completedTransactions.put(uuid, InternalResponseStatus.OK);
                Debug.log("Optimized: Executing transfer locally.\n");
                completedTransactionsLock.writeLock().unlock();


                return executedResponseStatus;
            }
            second.writeLock().unlock();
            first.writeLock().unlock();
        }

        completedTransactionsLock.readLock().lock();
        try {
            InternalResponseStatus completed = completedTransactions.get(uuid);
            if (completed != null) {
                return completed;
            }

            PendingWalletTransactions pwt = pendingWalletTransactions.computeIfAbsent(srcWalletId, k -> new PendingWalletTransactions());
            pwt.getLock().lock();
            try {
                pwt.incrementDeficit(amount);
            } finally {
                pwt.getLock().unlock();
            }

            future = pendingTransactions.computeIfAbsent(uuid, k -> new CompletableFuture<>());
        } finally {
            completedTransactionsLock.readLock().unlock();
        }

        sequencer.broadcastTransfer(uuid, srcUserId, srcWalletId, dstWalletId, amount);

        try {
            Debug.log("\n-----\nNode: waiting for transfer transaction to be executed!\n");
            return future.get();
        } catch (ExecutionException | InterruptedException e ) {
            return InternalResponseStatus.UNKNOWN;
        }
    }

    /**
     * Validates static condtions to Transfer
     * (if any state allows this transaction to be executed)
     */
    private InternalResponseStatus validateTransferArgs(String uuid, String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
        if (pendingTransactions.containsKey(uuid) || completedTransactions.containsKey(uuid)) {
            System.err.println("Duplicate transaction: " + uuid);
            return InternalResponseStatus.DUPLICATE_TRANSACTION;
        }
        if (srcUserId == null || !validFormat(srcUserId)) {
            System.err.println("Bad user id: " + srcUserId);
            return InternalResponseStatus.BAD_USER_FORMAT;
        }
        if (srcWalletId == null || !validFormat(srcWalletId)) {
            System.err.println("Bad wallet id: " + srcWalletId);
            return InternalResponseStatus.BAD_WALLET_FORMAT;
        }
        if (dstWalletId == null || !validFormat(dstWalletId)) {
            System.err.println("Bad wallet id: " + dstWalletId);
            return InternalResponseStatus.BAD_WALLET_FORMAT;
        }
        if (!isPositiveAmount(amount)) {
            System.err.println(amount + "should be positive");
            return InternalResponseStatus.NEGATIVE_AMOUNT;
        }
        
        return InternalResponseStatus.OK;
    }

    private InternalResponseStatus validateImpossibleTransfer(String userId, Wallet srcWallet, Wallet dstWallet, Long amount) {
        if (!srcWallet.getUserId().equals(userId)) return InternalResponseStatus.NOT_AUTHORIZED;
        return InternalResponseStatus.OK;
    }

    private InternalResponseStatus validateExecuteTransfer(Wallet srcWallet, Wallet dstWallet, Long amount) {
        PendingWalletTransactions srcPwt = pendingWalletTransactions.computeIfAbsent(srcWallet.getWalletId(), k -> new PendingWalletTransactions());
        PendingWalletTransactions dstPwt = pendingWalletTransactions.computeIfAbsent(dstWallet.getWalletId(), k -> new PendingWalletTransactions());

        PendingWalletTransactions first = srcPwt;
        PendingWalletTransactions second = dstPwt;

        String srcId = srcWallet.getWalletId();
        String dstId = dstWallet.getWalletId();

        if (srcId.compareTo(dstId) > 0) {
            first = dstPwt;
            second = srcPwt;
        }

        first.getLock().lock();
        second.getLock().lock();

        boolean orgMatches = AuthInfo.getOrganization(dstWallet.getUserId()).equals(this.organization);
        boolean srcNotDeleted = srcPwt.getDeleteCounter() == 0;
        boolean dstNotDeleted = dstPwt.getDeleteCounter() == 0;
        boolean sufficientBalance = (srcWallet.getBalance() - srcPwt.getDeficitAmount()) >= amount;

        if (orgMatches && srcNotDeleted && dstNotDeleted && sufficientBalance) {
            Debug.log("Node: Transaction can be optimized:\n" +
                    " - both wallets bellong to the node organization: " + this.organization + "\n" +
                    " - there are no pending delete requests (src delete counter = " + srcPwt.getDeleteCounter() + ", dst delete counter = " + dstPwt.getDeleteCounter() + ")\n" +
                    " - there is enough balance, even if all the pending transactions are concluded: Real balance ("+srcWallet.getBalance()+") - Deficit ("+srcPwt.getDeficitAmount()+") >= "+amount+"\n");
            return InternalResponseStatus.EXECUTED_LOCALY;
        }
        if (!orgMatches) Debug.log("Node: Transaction not optimized, organization does not match!\n");
        if (!srcNotDeleted) Debug.log("Node: Transaction not optimized, source delete counter is: " + srcPwt.getDeleteCounter()+"\n");
        if (!dstNotDeleted) Debug.log("Node: Transaction not optimized, destination delete counter is: " + dstPwt.getDeleteCounter()+"\n");
        if (!sufficientBalance) Debug.log("Node: Transaction not optimized, insufficient balance. Real balance ("+srcWallet.getBalance()+") - Deficit ("+srcPwt.getDeficitAmount()+") < "+amount+"\n");

        second.getLock().unlock();
        first.getLock().unlock();
        return InternalResponseStatus.OK;
    }

    public void optimizedBroadcastTransfer(String uuid, String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
        sequencer.broadcastTransfer(uuid, srcUserId, srcWalletId, dstWalletId, amount);

        Wallet srcWallet = this.wallets.get(srcWalletId);
        Wallet dstWallet = this.wallets.get(dstWalletId);
        
        PendingWalletTransactions srcPwt = pendingWalletTransactions.get(srcWallet.getWalletId());
        PendingWalletTransactions dstPwt = pendingWalletTransactions.get(dstWallet.getWalletId());

        PendingWalletTransactions pwdFirst = srcPwt;
        PendingWalletTransactions pwdSecond = dstPwt;

        String srcId = srcWallet.getWalletId();
        String dstId = dstWallet.getWalletId();

        if (srcId.compareTo(dstId) > 0) {
            pwdFirst = dstPwt;
            pwdSecond = srcPwt;
        }

        Wallet first = srcWallet.getWalletId().compareTo(dstWallet.getWalletId()) <= 0
            ? srcWallet
            : dstWallet;

        Wallet second = srcWallet.getWalletId().compareTo(dstWallet.getWalletId()) <= 0
            ? dstWallet
            : srcWallet;

        pwdSecond.getLock().unlock();
        pwdFirst.getLock().unlock();

        second.writeLock().unlock();
        first.writeLock().unlock();
    }

    public ReadResult readBalance(String walletId, int blockNumber) {

        blockCompleted.readLock().lock();
        if (node_block_counter < blockNumber) { 
            CompletableFuture<Void> future = pendingBlock.computeIfAbsent(blockNumber, k -> new CompletableFuture<>());
            try {
                Debug.log("\n-----\nNode: waiting for node to update its blockchain to send an updated balance!\n");
                future.get();
            } catch (ExecutionException | InterruptedException e ) {
                blockCompleted.readLock().unlock();
                return new ReadResult(-1L, -1);
            } 
        }
        blockCompleted.readLock().unlock();

        Wallet wallet = wallets.getOrDefault(walletId, null);
        if (wallet == null){ return new ReadResult(-1L, -1); }

        wallet.readLock().lock();
        try {
            return new ReadResult(wallet.getBalance(), blockNumber);
        } finally {
            wallet.readLock().unlock();
        }
    }

    public ArrayList<BlockRecord> getBlockchainState(){
        // Avoids reading the list of transaction while another thread is changing it
        synchronized (blocksLock) {
            return new ArrayList<>(blocks);
        }
    }

    public void getBlock () { 
        BlockRecord block;
        try{
            block = sequencer.deliverBlock(node_block_counter + 1);
        } catch (Exception e) {
            Debug.log("\n-----\nNode: Failed to receive block from sequencer!\n");
            return;
        }

        Debug.log("\n-----\nNode: Received block from sequencer:" + (node_block_counter + 1) + "\n");
        executeBlock(block);
    }
	

    private void executeBlock(BlockRecord block) {
        List<TransactionRecord> block_transactions = block.getTransactions();

        for (TransactionRecord record : block_transactions) {
            // Avoids to duplicate a transfer that was optimizable and executed locally 
            if (completedTransactions.containsKey(record.getUuid())) {continue;}

            InternalResponseStatus requestStatus = executeTransaction(record);

            completedTransactionsLock.writeLock().lock();
            try {
                CompletableFuture<InternalResponseStatus> future = pendingTransactions.remove(record.getUuid());

                if (future != null) {
                    future.complete(requestStatus);
                }

                completedTransactions.put(record.getUuid(), requestStatus);
            } finally {
                completedTransactionsLock.writeLock().unlock();
            }
        }
        synchronized (blocksLock) {
            blocks.add(block);
            
            blockCompleted.writeLock().lock();
            node_block_counter++;
            CompletableFuture<Void> future = pendingBlock.remove(block.getBlockNumber());
            if (future != null) {
                future.complete(null);
            }
            blockCompleted.writeLock().unlock();
        }
    }

    private InternalResponseStatus executeTransaction(TransactionRecord record) {
        switch (record.getType()) {
            case CREATE_WALLET:
                return executeCreateWallet((CreateWalletRecord) record);
            case DELETE_WALLET:
                return executeDeleteWallet((DeleteWalletRecord) record);
            case TRANSFER:
                return executeTransfer((TransferRecord) record);
            default:
                return InternalResponseStatus.UNKNOWN;
        }
    }

    private InternalResponseStatus executeCreateWallet(CreateWalletRecord record) {
        InternalResponseStatus status = canCreateWallet(record.getWalletId());
        if (status != InternalResponseStatus.OK) { return status; }

        Wallet wallet = new Wallet(record.getWalletId(), record.getUserId(), 0L);
        
        wallets.put(record.getWalletId(), wallet);
        Debug.log("Wallet created: " + record.getWalletId());
        
        return InternalResponseStatus.OK;
    }

    private InternalResponseStatus executeDeleteWallet(DeleteWalletRecord record) {
        // Only increments pending wallet delete counter if the wallet belongs to the node organization 
        if (AuthInfo.getOrganization(record.getUserId()).equals(this.organization)) {
            PendingWalletTransactions pwt = pendingWalletTransactions.get(record.getWalletId());
            pwt.getLock().lock();
            try {
                pwt.decrementDelete();
            } finally {
                pwt.getLock().unlock();
            }
        }

        Wallet wallet = wallets.get(record.getWalletId());
        if (wallet == null) {
            System.err.println("Wallet id does not exist: " + record.getWalletId());
            return InternalResponseStatus.WALLET_NOT_FOUND; 
        }
        wallet.writeLock().lock();

        try {
            InternalResponseStatus status = canDeleteWallet(wallet, record.getUserId());
            
            if (status != InternalResponseStatus.OK) { 
                return status; 
            }
        
            wallets.remove(record.getWalletId());

            Debug.log("Wallet deleted: " + record.getWalletId());

            return InternalResponseStatus.OK;
        } finally {
            wallet.writeLock().unlock();
        } 
    }

    private InternalResponseStatus executeTransfer(TransferRecord record) {
        if (AuthInfo.getOrganization(record.getSrcUserId()).equals(this.organization)) {
            PendingWalletTransactions pwt = pendingWalletTransactions.get(record.getSrcWalletId());
            pwt.getLock().lock();
            try {
                pwt.decrementDeficit(record.getAmount());
            } finally {
                pwt.getLock().unlock();
            }
        }
        

        Wallet srcWallet;
        Wallet dstWallet;

        if (record.getSrcWalletId().equals(record.getDstWalletId())) {
            return InternalResponseStatus.OK;
        }

        srcWallet = wallets.get(record.getSrcWalletId());
        dstWallet = wallets.get(record.getDstWalletId());

        if (srcWallet == null) { 
            System.err.println("Wallet id does not exist: " + record.getSrcWalletId());
            return InternalResponseStatus.WALLET_NOT_FOUND;
        }
        if (dstWallet == null){ 
            System.err.println("Wallet id does not exist: " + record.getDstWalletId());
            return InternalResponseStatus.WALLET_NOT_FOUND; 
        }

        Wallet first = srcWallet.getWalletId().compareTo(dstWallet.getWalletId()) <= 0
            ? srcWallet
            : dstWallet;

        Wallet second = srcWallet.getWalletId().compareTo(dstWallet.getWalletId()) <= 0
            ? dstWallet
            : srcWallet;

        first.writeLock().lock();
        second.writeLock().lock();

        try {
            InternalResponseStatus status = canTransfer(srcWallet, record.getSrcUserId(), record.getAmount());

            if (status != InternalResponseStatus.OK) { return status; }

            srcWallet.setBalance(srcWallet.getBalance() - record.getAmount());
            dstWallet.setBalance(dstWallet.getBalance() + record.getAmount());

            Debug.log("Transferred: " + record.getAmount() + " : " + record.getSrcWalletId() + " > " + record.getDstWalletId());
            return InternalResponseStatus.OK;
        } finally {
            second.writeLock().unlock();
            first.writeLock().unlock();
        }
    }
    
    /**
     * Validates static conditions to Create_Wallet
     * (if any state allows this transaction to be executed)
     */
    private InternalResponseStatus validateCreateWalletArgs(String userId, String walletId) { 
        if (userId == null || !validFormat(userId)) {
            System.err.println("Bad user id: " + userId);
            return InternalResponseStatus.BAD_USER_FORMAT;
        }
        if (walletId == null || !validFormat(walletId)) {
            System.err.println("Bad wallet id: " + walletId);
            return InternalResponseStatus.BAD_WALLET_FORMAT;
        }
        return InternalResponseStatus.OK;
    }

    /**
     * Validates static conditions to Delete_Wallet
     * (if any state allows this transaction to be executed)
     */
    private InternalResponseStatus validateDeleteWalletArgs(String userId, String walletId) {
        if (userId == null || !validFormat(userId)) {
            System.err.println("Bad user id: " + userId);
            return InternalResponseStatus.BAD_USER_FORMAT;
        }
        if (walletId == null || !validFormat(walletId)) {
            System.err.println("Bad wallet id: " + walletId);
            return InternalResponseStatus.BAD_WALLET_FORMAT;
        }
        return InternalResponseStatus.OK;
    }

    /**
     * Validates conditions to Create_Wallet
     * (if the current state allows this transaction to be executed)
     */
    private InternalResponseStatus canCreateWallet(String walletId) { 
        if (walletExists(walletId)) {
            System.err.println("Wallet id already exists: " + walletId);
            return InternalResponseStatus.WALLET_ALREADY_EXISTS;
        }
        return InternalResponseStatus.OK;
    }

    /**
     * Validates dynamic conditions to Delete_Wallet
     * (if the current state allows this transaction to be executed)
     */
    private InternalResponseStatus canDeleteWallet(Wallet wallet, String userId) {
        long balance = wallet.getBalance();
        if (!isZeroBalance(balance)) {
            System.err.println("Wallet id with balance other than 0: " + balance);
            return InternalResponseStatus.REMAINING_BALANCE;
        } 
        if (!isAuthorized(wallet, userId)) {
            System.err.println("Wallet id " + wallet.getWalletId() + " does not belong to user " + userId);
            return InternalResponseStatus.NOT_AUTHORIZED;
        }
        return InternalResponseStatus.OK;
    }

    /**
     * Validates dynamic conditions to Transfer
     * (if the current state allows this transaction to be executed)
     */
    private InternalResponseStatus canTransfer(Wallet srcWallet, String userId, long amount) {
        
        if (!isAuthorized(srcWallet, userId)) {
            System.err.println("Wallet id " + srcWallet.getWalletId() + " does not belong to user " + userId);
            return InternalResponseStatus.NOT_AUTHORIZED;
        }

        long srcBalance = srcWallet.getBalance();
        if (!hasEnoughBalance(srcBalance, amount)) {
            System.err.println("Wallet id " + srcWallet.getWalletId() + " has not enough money (" + srcWallet.getBalance() + ") to transfer " + amount);
            return InternalResponseStatus.INSUFFICIENT_BALANCE;
        }
        return InternalResponseStatus.OK;    
    }

    private boolean validFormat(String input) {
        if (input == null || !ID_PATTERN.matcher(input).matches() || input.isBlank()) {
            return false;
        }
        return true;
    }

    private boolean walletExists(String walletId) {
        return wallets.containsKey(walletId);
    }

    // Checks if the wallet belongs to the user
    private boolean isAuthorized(Wallet wallet, String userId) {
        return wallet.getUserId().equals(userId);
    }

    private boolean hasEnoughBalance(long balanceSrc, long amount){
        return balanceSrc >= amount; 
    }

    private boolean isPositiveAmount(long amount) {
        return amount >= 0;
    }

    private boolean isZeroBalance(long balance) {
        return balance == 0;
    }

    private InternalResponseStatus verifyUserOrganization(String userId) {
        if (!AuthInfo.getOrganization(userId).equals(this.organization)) {
            System.err.println("User is of wrong organization");
            return InternalResponseStatus.WRONG_ORGANIZATION;
        }

        return InternalResponseStatus.OK;
    }

}