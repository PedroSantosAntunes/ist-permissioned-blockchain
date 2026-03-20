package pt.tecnico.blockchainist.node.domain;

import static io.grpc.MethodDescriptor.newBuilder;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Pattern;

import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;
import pt.tecnico.blockchainist.status.InternalResponseStatus;

public class NodeState {
    
    public record ReadResult(long balance, int blockNumber) {}
    
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private final Map<String, Wallet> wallets = new ConcurrentHashMap<>();
    //TODO ArrayList mau, mudar
    private final ArrayList<BlockRecord> blocks = new ArrayList<BlockRecord>();

    private final Map<String, CompletableFuture<InternalResponseStatus>> pendingTransactions = new ConcurrentHashMap<>();
    private final Map<String, InternalResponseStatus> completedTransactions = new ConcurrentHashMap<>();

    private final Map<Integer, CompletableFuture<Void>> pendingBlock = new ConcurrentHashMap<>();

    private int node_transaction_counter = 0;
    private int node_block_counter = 0;
    
    private final Object blocksLock = new Object();

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
            return InternalResponseStatus.UNKNOWN;
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
            return InternalResponseStatus.UNKNOWN;
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
            return InternalResponseStatus.UNKNOWN;
        }
    }

    public ReadResult readBalance(String walletId, int blockNumber) {

        if (node_block_counter < blockNumber) { 
            CompletableFuture<Void> future = pendingBlock.computeIfAbsent(blockNumber, k -> new CompletableFuture<>());
            try {
                future.get();
                
            }
            catch (Exception e) {
                return new ReadResult(-1L, -1);
            }   
        }

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
        Debug.log("\n-----\nNode: Requesting block from sequencer:" + (node_block_counter + 1) + "\n");
        BlockRecord block = sequencer.deliverBlock(node_block_counter + 1);

        if(block.getBlockNumber() == -1) {
            Debug.log("\n-----\nNode: Error fetching block from sequencer\n");
            return;
        }

        Debug.log("\n-----\nNode: Received block from sequencer:" + (node_block_counter + 1) + "\n");
        executeBlock(block);
    }
	

    private void executeBlock(BlockRecord block) {
        List<TransactionRecord> block_transactions = block.getTransactions();

        for (TransactionRecord record : block_transactions) {
            InternalResponseStatus requestStatus = executeTransaction(record);

            node_transaction_counter++;

            CompletableFuture<InternalResponseStatus> future = pendingTransactions.remove(record.getUuid());

            if (future != null) {
                future.complete(requestStatus);
            }

            completedTransactions.put(record.getUuid(), requestStatus);
        }
        synchronized (blocksLock) {
            blocks.add(block);
            node_block_counter = block.getBlockNumber();
        } // todo check lock on everything or just add
        CompletableFuture<Void> future = pendingBlock.remove(block.getBlockNumber());
        if (future != null) {
            future.complete(null);
        }
        
        // node_block_counter++; 

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
            InternalResponseStatus status = canDeleteWallet(wallet, record.getUserId());
            if (status != InternalResponseStatus.OK) { return status; }
        
            wallets.remove(record.getWalletId());
            Debug.log("Wallet deleted: " + record.getWalletId());

            return InternalResponseStatus.OK;
        } finally {
            wallet.writeLock().unlock();
        } 
    }

    private InternalResponseStatus executeTransfer(TransferRecord record) {
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

        srcWallet.writeLock().lock();
        dstWallet.writeLock().lock();

        try {
            InternalResponseStatus status = canTransfer(srcWallet, record.getSrcUserId(), record.getAmount());

            if (status != InternalResponseStatus.OK) { return status; }

            srcWallet.setBalance(srcWallet.getBalance() - record.getAmount());
            dstWallet.setBalance(dstWallet.getBalance() + record.getAmount());

            Debug.log("Transferred: " + record.getAmount() + " : " + record.getSrcWalletId() + " > " + record.getDstWalletId());
            return InternalResponseStatus.OK;
        } finally {
            srcWallet.writeLock().unlock();
            dstWallet.writeLock().unlock();
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
    private InternalResponseStatus canDeleteWallet(Wallet wallet, String userId) {
        long balance = wallet.getBalance();
        if (!isZeroBalance(balance)) {
            System.err.println("Wallet id with balance other than 0: " + balance);
            return InternalResponseStatus.REMAINING_BALANCE;
        } 
        if (!isAuthorized(wallet, userId)) {
            System.err.println("Wallet id " + wallet.getWalletId() + "does not belong to user " + userId);
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
            System.err.println("Wallet id " + srcWallet.getWalletId() + "does not belong to user " + userId);
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

}
