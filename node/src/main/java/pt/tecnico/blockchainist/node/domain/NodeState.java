package pt.tecnico.blockchainist.node.domain;

import java.util.ArrayList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import java.util.regex.Pattern;

import com.google.protobuf.Internal;

import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;
import pt.tecnico.blockchainist.status.InternalResponseStatus;

public class NodeState {
    
    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");
    
    private final Map<String, Wallet> wallets = new ConcurrentHashMap<>();
    private final ArrayList<TransactionRecord> transactions = new ArrayList<TransactionRecord>();
    int local_transaction_counter = 0;
    
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
    
    public InternalResponseStatus createWallet(String userId, String walletId) {
        
        InternalResponseStatus argsInternalResponseStatus = validateCreateWalletArgs(userId, walletId);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }
        
        int target_transaction = sequencer.broadcastCreateWallet(userId, walletId);
        return pullMissingTransactions(target_transaction);
    }

    public InternalResponseStatus deleteWallet(String userId, String walletId) {
        
        InternalResponseStatus argsInternalResponseStatus = validateDeleteWalletArgs(userId, walletId);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }
        
        int target_transaction = sequencer.broadcastDeleteWallet(userId, walletId);
        return pullMissingTransactions(target_transaction);
    }

    public InternalResponseStatus transfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {

        InternalResponseStatus argsInternalResponseStatus = validateTranferArgs(srcUserId, srcWalletId, dstWalletId, amount);
        if (argsInternalResponseStatus != InternalResponseStatus.OK) { return argsInternalResponseStatus; }

        int target_transaction = sequencer.broadcastTransfer(srcUserId, srcWalletId, dstWalletId, amount);
        
        return pullMissingTransactions(target_transaction);
    }

    public long readBalance(String walletId) {
        
        synchronized (stateLock) {
            Wallet wallet = wallets.getOrDefault(walletId, null);
            if (wallet == null){ return -1L; }
            return wallet.getBalance();
        }

    }

    public ArrayList<TransactionRecord> getBlockchainState(){		
        synchronized (stateLock) {
            return new ArrayList<>(transactions);
        }
    }

    public InternalResponseStatus pullMissingTransactions (int target) {
		while (true) {
			int next_transaction;
			synchronized (stateLock) {
				next_transaction = local_transaction_counter + 1;
			}

			if (next_transaction > target) return InternalResponseStatus.OK;

			TransactionRecord record = sequencer.deliverTransaction(next_transaction);

			synchronized (stateLock) {
				if (local_transaction_counter >= record.getSequenceNumber()) continue;

				InternalResponseStatus status = executeTransaction(record);

                if (record.getSequenceNumber() == target && status != InternalResponseStatus.OK){
                    return status;
                }

				transactions.add(record);

				local_transaction_counter++;
			}
		}
	}


    private InternalResponseStatus executeTransaction(TransactionRecord record) {
        InternalResponseStatus argsInternalResponseStatus;
        switch (record.getType()) {
            case CREATE_WALLET:
                CreateWalletRecord createRecord = (CreateWalletRecord) record;
                argsInternalResponseStatus = canCreateWallet(createRecord.getUserId(), createRecord.getWalletId());
                if (argsInternalResponseStatus!= InternalResponseStatus.OK) return argsInternalResponseStatus;
                wallets.put(createRecord.getWalletId(), new Wallet(createRecord.getWalletId(), createRecord.getUserId(), 0L));
                Debug.log("Wallet created: " + createRecord.getWalletId());
                return InternalResponseStatus.OK;
                
            case DELETE_WALLET:
                DeleteWalletRecord deleteRecord = (DeleteWalletRecord) record;
                argsInternalResponseStatus = canDeleteWallet(deleteRecord.getUserId(), deleteRecord.getWalletId());
                if (argsInternalResponseStatus != InternalResponseStatus.OK) return argsInternalResponseStatus;
                wallets.remove(deleteRecord.getWalletId());
                Debug.log("Wallet deleted: " + deleteRecord.getWalletId());
                return InternalResponseStatus.OK;
                
            case TRANSFER:
                TransferRecord transferRecord = (TransferRecord) record;
                
                argsInternalResponseStatus = canTranfer(transferRecord.getSrcUserId(), transferRecord.getSrcWalletId(), transferRecord.getDstWalletId(), transferRecord.getAmount());
                if (argsInternalResponseStatus != InternalResponseStatus.OK) return argsInternalResponseStatus; 

                Wallet srcWallet = wallets.get(transferRecord.getSrcWalletId());
                Wallet dstWallet = wallets.get(transferRecord.getDstWalletId());
                srcWallet.setBalance(srcWallet.getBalance() - transferRecord.getAmount());
                dstWallet.setBalance(dstWallet.getBalance() + transferRecord.getAmount());
                Debug.log("Transferred: " + transferRecord.getAmount() + " : " + transferRecord.getSrcWalletId() + " > " + transferRecord.getDstWalletId());
                return InternalResponseStatus.OK;
            default:
                return InternalResponseStatus.UNKNOWN;
            }
    }


    private InternalResponseStatus validateCreateWalletArgs(String userId, String walletId) { 
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

    // Para aplicar Depois das transações serem aplicadas
    private InternalResponseStatus canCreateWallet(String userId, String walletId) { 
        if (walletExists(walletId)) {
            System.err.println("Wallet id already exists: " + walletId);
            return InternalResponseStatus.WALLET_ALREADY_EXISTS;
        }
        return InternalResponseStatus.OK;
    }

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

    private InternalResponseStatus validateTranferArgs(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
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

    private InternalResponseStatus canTranfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
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
