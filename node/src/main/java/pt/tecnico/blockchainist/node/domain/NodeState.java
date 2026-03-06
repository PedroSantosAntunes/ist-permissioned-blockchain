package pt.tecnico.blockchainist.node.domain;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Pattern;

import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;

import pt.tecnico.blockchainist.error.InternalResponseStatus;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;

public class NodeState {
    
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    private final Map<String, Wallet> wallets = new ConcurrentHashMap<>();
    private final ArrayList<TransactionRecord> transactions = new ArrayList<TransactionRecord>();
    int local_transaction_counter = 0;
    
    private final Object pullTransactionLock = new Object();

    public static final String BC_WALLET = "bc";
    public static final String BC_NAME = "BC";
    public static final long BC_INIT_BALANCE = 1000L;

    private final NodeSequencerService sequencer;

    public NodeState(NodeSequencerService sequencer) {
        Wallet wallet = new Wallet(BC_WALLET, BC_NAME, BC_INIT_BALANCE);

        wallets.put(BC_WALLET, wallet);
    
        this.sequencer = sequencer;
    }
    
    public InternalResponseStatus createWallet(String userId, String walletId) {
        
        InternalResponseStatus argsInternalResponseStatus = checkCreateWalletArgs(userId, walletId);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }
        
        int target_transaction = sequencer.broadcastCreateWallet(userId, walletId);
        pullTransactions(target_transaction);
        
        return InternalResponseStatus.OK;
    }

    public InternalResponseStatus deleteWallet(String userId, String walletId) {
        
        InternalResponseStatus argsInternalResponseStatus = checkDeleteWalletArgs(userId, walletId);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }
        
        int target_transaction = sequencer.broadcastDeleteWallet(userId, walletId);
        pullTransactions(target_transaction);

        return InternalResponseStatus.OK;
    }

    public InternalResponseStatus transfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {

        InternalResponseStatus argsInternalResponseStatus = checkTransferArgs(srcUserId, srcWalletId, dstWalletId, amount);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        int target_transaction = sequencer.broadcastTransfer(srcUserId, srcWalletId, dstWalletId, amount);
        pullTransactions(target_transaction);

        return InternalResponseStatus.OK;
    }

    public long readBalance(String walletId) {
        
        Wallet wallet = wallets.getOrDefault(walletId, null);
        if (wallet == null){ return -1L; }

        return wallet.getBalance();  
    }

    public ArrayList<TransactionRecord> getBlockchainState(){  		
        return transactions;
    }

    private void pullTransactions(int target_transaction){
        // Send DeliverTransactionRequest's to sequencer until the local transaction counter is equal 
        // to the transaction number from the BroadcastResponse
        
        while(local_transaction_counter < target_transaction){
            
            int next_transaction = local_transaction_counter + 1;
            
            TransactionRecord transaction = sequencer.deliverTransaction(next_transaction);
            
            executeTransaction(transaction);
            
            transactions.add(transaction);
            //TODO acho que se tem que incrementar antes de por (problema de concorrencia)
            local_transaction_counter++;
        }
    }



    public void pullMissingTransactions(int target){

        while(true){
            boolean done = pullNextTransaction(target);
            if (done) break;
        }
    }

    public boolean pullNextTransaction(int target){
        synchronized(pullTransactionLock) {

            int next_transaction = local_transaction_counter + 1;
            
            if(next_transaction > target) return true;

            TransactionRecord transaction = sequencer.deliverTransaction(next_transaction);

            executeTransaction(transaction);

            transactions.add(transaction);

            local_transaction_counter++;

            return false;
        }
    }



    private void executeTransaction(TransactionRecord record) {
        switch (record.getType()) {
            case CREATE_WALLET:
                CreateWalletRecord createRecord = (CreateWalletRecord) record;
                wallets.put(createRecord.getWalletId(), new Wallet(createRecord.getWalletId(), createRecord.getUserId(), 0L));
                Debug.log("Wallet created: " + createRecord.getWalletId());
                break;
                
            case DELETE_WALLET:
                DeleteWalletRecord deleteRecord = (DeleteWalletRecord) record;
                wallets.remove(deleteRecord.getWalletId());
                Debug.log("Wallet deleted: " + deleteRecord.getWalletId());
                break;
                
            case TRANSFER:
                TransferRecord transferRecord = (TransferRecord) record;
                Wallet srcWallet = wallets.get(transferRecord.getSrcWalletId());
                Wallet dstWallet = wallets.get(transferRecord.getDstWalletId());
                srcWallet.setBalance(srcWallet.getBalance() - transferRecord.getAmount());
                dstWallet.setBalance(dstWallet.getBalance() + transferRecord.getAmount());
                Debug.log("Transferred: " + transferRecord.getAmount() + " : " + transferRecord.getSrcWalletId() + " > " + transferRecord.getDstWalletId());
                break;
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

    // MEGA TODO -> validação dinâmica -> apagar checkCreateWalletArgs depois de usar as próximas duas funções
    // Para aplicar antes das transações serem aplicadas
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

    // Para aplicar Depois das transações serem aplicadas
    private InternalResponseStatus validateCreateWalletLogic(String userId, String walletId) { 
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

    // MEGA TODO -> validação dinâmica -> apagar checkDeleteWalletArgs depois de usar as próximas duas funções
    // Para aplicar antes das transações serem aplicadas
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

    // Para aplicar após das transações serem aplicadas
    private InternalResponseStatus canDeleteWallet(String userId, String walletId) {
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

    private InternalResponseStatus validateTranferArgs(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
        if (srcUserId == null || !validFormat(srcUserId)) {
                System.err.println("Bad user id: " + srcUserId);
                return InternalResponseStatus.BAD_USER_FORMAT;
            }
        if (srcWalletId == null || !validFormat(srcWalletId)) {
            System.err.println("Bad wallet id: " + srcWalletId);
            return InternalResponseStatus.BAD_WALLET_FORMAT;
        }
        if (!isPositiveAmount(amount)) {
            System.err.println(amount + "should be positive");
            return InternalResponseStatus.NEGATIVE_AMOUNT;
        }
        return InternalResponseStatus.OK;
    }

    private InternalResponseStatus validateTranferLogic(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
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
