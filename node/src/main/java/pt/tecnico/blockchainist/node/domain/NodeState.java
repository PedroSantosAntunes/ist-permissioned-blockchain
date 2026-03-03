package pt.tecnico.blockchainist.node.domain;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Checks
import java.util.regex.Pattern;

import pt.tecnico.blockchainist.contract.BroadcastRequest;
import pt.tecnico.blockchainist.contract.CreateWalletRequest;
import pt.tecnico.blockchainist.contract.DeleteWalletRequest;
import pt.tecnico.blockchainist.contract.DeliverTransactionRequest;
import pt.tecnico.blockchainist.contract.InternalResponseStatus;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.contract.TransferRequest;
import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import pt.tecnico.blockchainist.transaction.domain.*;

import pt.tecnico.blockchainist.debug.Debug;

public class NodeState {
    
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    private final ArrayList<TransactionRecord> transactions = new ArrayList<TransactionRecord>();
    int local_transaction_counter = 0;

    private final Map<String, Wallet> wallets = new ConcurrentHashMap<>();

    public static final String BC_WALLET = "bc";
    public static final String BC_NAME = "BC";
    public static final long BC_INIT_BALANCE = 1000L;

    private final NodeSequencerService sequencer;

    public NodeState(NodeSequencerService sequencer) {
        Wallet wallet = new Wallet(BC_WALLET, BC_NAME, BC_INIT_BALANCE);

        wallets.put(BC_WALLET, wallet);
    
        this.sequencer = sequencer;
    }
    
    public synchronized InternalResponseStatus createWallet(String userId, String walletId) {
        
        InternalResponseStatus argsInternalResponseStatus = checkCreateWalletArgs(userId, walletId);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        // Send to sequencer new BroadcastRequest

        // Send DeliverTransactionRequest's to sequencer until the local transaction counter is equal 
        // to the transaction number from the BroadcastResponse


        Transaction transaction = Transaction.newBuilder()
        .setCreateWallet(
            CreateWalletRequest.newBuilder()
            .setUserId(userId)
            .setWalletId(walletId)
            .build()
        ).build();
        BroadcastRequest request = BroadcastRequest.newBuilder().setTransaction(transaction).build();

        int target_transaction = sequencer.broadcast(request).getSequenceNumber();

        pullTransactions(target_transaction);

        Debug.log("Wallet created!\nWallets:\n" + wallets.values());

        return InternalResponseStatus.OK;
    }

    public synchronized InternalResponseStatus deleteWallet(String userId, String walletId) {
        
        InternalResponseStatus argsInternalResponseStatus = checkDeleteWalletArgs(userId, walletId);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }
        
        Transaction transaction = Transaction.newBuilder()
                .setDeleteWallet(
                    DeleteWalletRequest.newBuilder()
                        .setUserId(userId)
                        .setWalletId(walletId)
                        .build()
                ).build();
        BroadcastRequest request = BroadcastRequest.newBuilder().setTransaction(transaction).build();

        int target_transaction = sequencer.broadcast(request).getSequenceNumber();

        pullTransactions(target_transaction);
        
        Debug.log("Wallet deleted!\nWallets:\n" + wallets.values());

        return InternalResponseStatus.OK;
    }

    public synchronized InternalResponseStatus transfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {

        InternalResponseStatus argsInternalResponseStatus = checkTransferArgs(srcUserId, srcWalletId, dstWalletId, amount);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        Transaction transaction = Transaction.newBuilder()
                .setTransfer(
                    TransferRequest.newBuilder()
                        .setSrcUserId(srcUserId)
                        .setSrcWalletId(srcWalletId)
                        .setDstWalletId(dstWalletId)
                        .setValue(amount)
                        .build()
                ).build();
        BroadcastRequest request = BroadcastRequest.newBuilder().setTransaction(transaction).build();

        int target_transaction = sequencer.broadcast(request).getSequenceNumber();

        pullTransactions(target_transaction);

        Debug.log("Currency transfered!\nWallets:\n" + wallets.values());

        return InternalResponseStatus.OK;
    }

    public long readBalance(String walletId) {
        
        Wallet wallet = wallets.getOrDefault(walletId, null);
        if (wallet == null){ return -1L; }

        Debug.log("Current balance: " + wallet.getBalance());
        return wallet.getBalance();  
    }

    // todo change return type to list of transactions
    public ArrayList<TransactionRecord> getBlockchainState(){  
		
        return transactions;
    }

    private void pullTransactions(int target_transaction){
        // Send DeliverTransactionRequest's to sequencer until the local transaction counter is equal 
        // to the transaction number from the BroadcastResponse
        
        while(local_transaction_counter < target_transaction){
            int next_transaction = local_transaction_counter + 1;
            DeliverTransactionRequest request = DeliverTransactionRequest.newBuilder().setSequenceNumber(next_transaction).build();

            Transaction transaction = sequencer.deliverTransaction(request).getTransaction();

            TransactionRecord txRecord = TransactionRecord.transactionToRecord(transaction, next_transaction);

            ExecutionVisitor executor = new ExecutionVisitor(wallets);            
            txRecord.accept(executor);
            
            transactions.add(txRecord);
            local_transaction_counter++;
        }
    }


    private InternalResponseStatus checkCreateWalletArgs(String userId, String walletId) {
        if (userId == null || !validFormat(userId)) {
            System.err.println("Bad user id: " + userId);
            return InternalResponseStatus.BAD_USER_FORMAT;
        }
        if (walletId == null || !validFormat(walletId)) {
            System.err.println("Bad wallet id: " + walletId);
            return InternalResponseStatus.BAD_WALLET_FORMAT;
        }
        if (walletExists(walletId)) {
            System.err.println("Wallet id already exists: " + walletId);
            return InternalResponseStatus.WALLET_ALREADY_EXISTS;
        }
        return InternalResponseStatus.OK;
    }

    private InternalResponseStatus checkDeleteWalletArgs(String userId, String walletId) {
        if (userId == null || !validFormat(userId)) {
            System.err.println("Bad user id: " + userId);
            return InternalResponseStatus.BAD_USER_FORMAT;
        }
        if (walletId == null || !validFormat(walletId)) {
            System.err.println("Bad wallet id: " + walletId);
            return InternalResponseStatus.BAD_WALLET_FORMAT;
        }
        if (!walletExists(walletId)) {
            System.err.println("Wallet id does not exist: " + walletId);
            return InternalResponseStatus.WALLET_NOT_FOUND; 
        }
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


    private InternalResponseStatus checkTransferArgs(String srcUserId, String srcWalletId, String dstWalletId, Long amount) { 
        if (!walletExists(srcWalletId)){ 
            System.err.println("Wallet id does not exist: " + srcWalletId);
            return InternalResponseStatus.WALLET_NOT_FOUND; 
        }
        if (!walletExists(dstWalletId)){ 
            System.err.println("Wallet id does not exist: " + dstWalletId);
            return InternalResponseStatus.WALLET_NOT_FOUND; 
        }
        if (!isAuthorized(srcWalletId, srcUserId)) {
            System.err.println("Wallet id " + srcWalletId + "does not belong to user " + srcUserId);
            return InternalResponseStatus.NOT_AUTHORIZED;
        }

        long srcBalance = wallets.get(srcWalletId).getBalance();
        if (!hasEnoughBalance(srcBalance, amount)) {
            System.err.println("Wallet id " + srcWalletId + "has not enough money (" + srcBalance + ") to transfer " + amount);
            return InternalResponseStatus.INSUFFICIENT_BALANCE;
        }
        if (!isPositiveAmount(amount)) {
            System.err.println(amount + "should be positive");
            return InternalResponseStatus.NEGATIVE_AMOUNT;
        } 
        return InternalResponseStatus.OK;
    }


    private boolean validFormat(String input) {
        if (!ID_PATTERN.matcher(input).matches() || input.isBlank()) {
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
    private boolean isAuthorized(String walletId, String userId) {
        return wallets.get(walletId).getUserId().equals(userId);
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
