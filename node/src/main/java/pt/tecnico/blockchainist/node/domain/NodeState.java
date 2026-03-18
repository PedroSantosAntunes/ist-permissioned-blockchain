package pt.tecnico.blockchainist.node.domain;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Pattern;

import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import pt.tecnico.blockchainist.block.BlockRecord;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;
import pt.tecnico.blockchainist.status.InternalResponseStatus;

public class NodeState {
    //TODO provavelmente 
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    private final Map<String, Wallet> wallets = new ConcurrentHashMap<>();
    //TODO ArrayList mau, mudar
    private final ArrayList<BlockRecord> blocks = new ArrayList<BlockRecord>();

    private final Map<String, CompletableFuture<InternalResponseStatus>> pendingTransactions = new ConcurrentHashMap<>();
    private final Map<String, InternalResponseStatus> completedTransactions = new ConcurrentHashMap<>();

    private int node_transaction_counter = 0;
    private int node_block_counter = 0;
    
    private final Object stateLock = new Object();

    public static final String BC_WALLET = "bc";
    public static final String BC_NAME = "BC";
    public static final long BC_INIT_BALANCE = 1000L;

    private final NodeSequencerService sequencer;

    public NodeState(NodeSequencerService sequencer) {
        Wallet wallet = new Wallet(BC_WALLET, BC_NAME, BC_INIT_BALANCE);

        wallets.put(BC_WALLET, wallet);
    
        this.sequencer = sequencer;
    }
    
    public InternalResponseStatus createWallet(String uuid, String userId, String walletId) {
        InternalResponseStatus completed = completedTransactions.get(uuid);
        if (completed != null) {
            return completed;
        }
        
        InternalResponseStatus argsInternalResponseStatus = validateCreateWalletArgs(userId, walletId);
                
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        CompletableFuture<InternalResponseStatus> future = pendingTransactions.computeIfAbsent(uuid, k -> new CompletableFuture<>());

        sequencer.broadcastCreateWallet(uuid, userId, walletId);

        try {
            return future.get();
        } catch (Exception e) {
            //TODO
            return InternalResponseStatus.OK;
        }  
    }

    public InternalResponseStatus deleteWallet(String uuid, String userId, String walletId) {
        
        InternalResponseStatus completed = completedTransactions.get(uuid);
        if (completed != null) {
            return completed;
        }

        InternalResponseStatus argsInternalResponseStatus = validateDeleteWalletArgs(userId, walletId);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        CompletableFuture<InternalResponseStatus> future = pendingTransactions.computeIfAbsent(uuid, k -> new CompletableFuture<>());
        sequencer.broadcastDeleteWallet(uuid, userId, walletId);

        try {
            return future.get();
        } catch (Exception e) {
            //TODO
            return InternalResponseStatus.OK;
        } 
    }

    public InternalResponseStatus transfer(String uuid, String srcUserId, String srcWalletId, String dstWalletId, Long amount) {

        InternalResponseStatus completed = completedTransactions.get(uuid);
        if (completed != null) {
            return completed;
        }

        InternalResponseStatus argsInternalResponseStatus = validateTransferArgs(srcUserId, srcWalletId, dstWalletId, amount);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        CompletableFuture<InternalResponseStatus> future = pendingTransactions.computeIfAbsent(uuid, k -> new CompletableFuture<>());
        sequencer.broadcastTransfer(uuid, srcUserId, srcWalletId, dstWalletId, amount);

        try {
            return future.get();
        } catch (Exception e) {
            //TODO
            return InternalResponseStatus.OK;
        }
    }

    public long readBalance(String walletId) {
        Wallet wallet = wallets.getOrDefault(walletId, null);
        if (wallet == null){ return -1L; }
        return wallet.getBalance();
    }

    public ArrayList<BlockRecord> getBlockchainState(){		
        // Avoids reading the list of transaction while another thread is changing it
        synchronized (stateLock) {
            return new ArrayList<>(blocks);
        }
    }

    public void getBlock () { 
        Debug.log("\n-----\nNode: Requesting block from sequencer:" + (node_block_counter + 1) + "\n");
        BlockRecord block = sequencer.deliverBlock(node_block_counter + 1);
        
        Debug.log("\n-----\nNode: Received block from sequencer:" + (node_block_counter + 1) + "\n");
        executeBlock(block);
    }
	

    private void executeBlock(BlockRecord block) {
        //TODO locks e paralelismo
        List<TransactionRecord> block_transactions = block.getTransactions();

        for (TransactionRecord record : block_transactions) {
            InternalResponseStatus requestStatus = executeTransaction(record);

            //TODO precisa lock?
            node_transaction_counter++;

            CompletableFuture<InternalResponseStatus> future = pendingTransactions.remove(record.getId());
            
            System.out.println("\nset future:" + requestStatus + "\n");

            if (future != null) {
                future.complete(requestStatus);
            }

            completedTransactions.put(record.getId(), requestStatus);
        }

        blocks.add(block);
        node_block_counter++;
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
        Wallet wallet = wallets.get(record.getWalletId());
        if (wallet == null) {
            System.err.println("Wallet id does not exist: " + record.getWalletId());
            return InternalResponseStatus.WALLET_NOT_FOUND; 
        }
        wallet.writeLock().lock();

        try {
            InternalResponseStatus status = canDeleteWallet(record.getUserId(), record.getWalletId());
            if (status != InternalResponseStatus.OK) { return status; }
        
            wallets.remove(record.getWalletId());
            Debug.log("Wallet deleted: " + record.getWalletId());

            return InternalResponseStatus.OK;
        } finally {
            wallet.writeLock().unlock();
        } 
    }

    private InternalResponseStatus executeTransfer(TransferRecord record) {
        Wallet firstLock;
        Wallet secondLock;
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

        if (record.getSrcWalletId().compareTo(record.getDstWalletId()) < 0) {
            firstLock = srcWallet;
            secondLock = dstWallet;
        } else {
            firstLock = dstWallet;
            secondLock = srcWallet;
        }

        firstLock.writeLock().lock();
        secondLock.writeLock().lock();

        try {
            InternalResponseStatus status = canTransfer(srcWallet, record.getSrcUserId(), record.getAmount());

            if (status != InternalResponseStatus.OK) { return status; }

            srcWallet.setBalance(srcWallet.getBalance() - record.getAmount());
            dstWallet.setBalance(dstWallet.getBalance() + record.getAmount());

            Debug.log("Transferred: " + record.getAmount() + " : " + record.getSrcWalletId() + " > " + record.getDstWalletId());
            return InternalResponseStatus.OK;
        } finally {
            secondLock.writeLock().unlock();
            firstLock.writeLock().unlock();
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
     * Validates static condtions to Transfer
     * (if any state allows this transaction to be executed)
     */
    private InternalResponseStatus validateTransferArgs(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
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
    private InternalResponseStatus canDeleteWallet(String userId, String walletId) {
        if (!userExists(userId)) {
            System.err.println("User id does not exist: " + userId);
            return InternalResponseStatus.USER_NOT_FOUND;
        }

        long balance = wallets.get(walletId).getBalance();
        if (!isZeroBalance(balance)) {
            System.err.println("Wallet id with balance other than 0: " + balance);
            return InternalResponseStatus.REMAINING_BALANCE;
        } 
        if (!isAuthorized(walletId, userId)) {
            System.err.println("Wallet id " + walletId + "does not belong to user " + userId);
            return InternalResponseStatus.NOT_AUTHORIZED;
        }
        return InternalResponseStatus.OK;
    }

    /**
     * Validates dynamic conditions to Transfer
     * (if the current state allows this transaction to be executed)
     */
    private InternalResponseStatus canTransfer(Wallet srcWallet, String userId, long amount) {
        
        if (!isAuthorized(srcWallet.getUserId(), userId)) {
            System.err.println("Wallet id " + srcWallet.getUserId() + "does not belong to user " + userId);
            return InternalResponseStatus.NOT_AUTHORIZED;
        }

        long srcBalance = srcWallet.getBalance();
        if (!hasEnoughBalance(srcBalance, amount)) {
            System.err.println("Wallet id " + srcWallet.getWalletId() + "has not enough money (" + srcWallet.getBalance() + ") to transfer " + amount);
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

    private boolean userExists(String userId) {
        for (Wallet wallet : wallets.values()) {
            if (wallet.getUserId().equals(userId)) {
                return true;
            }
        }
        return false;
    }

    private boolean walletExists(String walletId) {
        return wallets.containsKey(walletId);
    }

    // Checks if the wallet belongs to the user
    private boolean isAuthorized(String walletUserId, String userId) {
        return walletUserId.equals(userId);
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

}
