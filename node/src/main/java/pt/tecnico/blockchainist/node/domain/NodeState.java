package pt.tecnico.blockchainist.node.domain;

import java.util.LinkedList;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Checks
import java.util.regex.Pattern;

import pt.tecnico.blockchainist.contract.ReadBalanceResponse;
import pt.tecnico.blockchainist.contract.Status;
import pt.tecnico.blockchainist.transaction.*;

public class NodeState {
    
    // TODO Declare state maintained by each node
    // - The set of wallets, indexed by their identifiers, and their owner user identifiers (including the 'bc' wallet)
    // - The balance of each wallet
    // - The transaction ledger (up to A.2, a chain of individual transactions; after B.1, a chain of blocks)

    private final LinkedList<Transaction> transactions = new LinkedList<Transaction>();
    int local_transaction_counter = 0;

    private final Map<String, String> wallets = new ConcurrentHashMap<>();
    private final Map<String, Long> balances = new ConcurrentHashMap<>();

    public static final String BC_WALLET = "bc";
    public static final String BC_NAME = "BC";
    public static final long BC_INIT_BALANCE = 1000L;

    public NodeState() {
        wallets.put(BC_WALLET, BC_NAME);
        balances.put(BC_WALLET, BC_INIT_BALANCE);
    }

    public synchronized Status createWallet(String userId, String walletId) {

        System.err.println("NodeState: createWallet called!\n\t" + userId + "\n\t" + walletId);

        if (userId == null || !checkFormat(userId)) {
            System.err.println("Bad user id: " + userId);
            return Status.BAD_USER_ERR;
        }
        if (walletId == null || !checkFormat(walletId)) {
            System.err.println("Bad wallet id: " + walletId);
            return Status.BAD_WALLET_ERR;
        }
        if (checkUserUniqueness(userId)) {
            System.err.println("User id already exists: " + userId);
            return Status.UNIQUE_USER_ERR;
        }
        if (checkWalletUniqueness(walletId)) {
            System.err.println("Wallet id already exists: " + walletId);
            return Status.UNIQUE_WALLET_ERR;
        }
        // Transaction
        // TODO A.2
        Transaction transaction = new CreateWalletTransaction(local_transaction_counter, userId, walletId);
        transactions.addFirst(transaction);
        local_transaction_counter++;

        // Execute
        wallets.put(walletId, userId);
        balances.put(walletId, 0L);

        System.err.println("\twallets = " + wallets);
        System.err.println("\tbalances = " + balances);
        System.err.println("\ttransaction = " + transaction);
        return Status.OK;
    }

    public synchronized Status deleteWallet(String userId, String walletId) {
        
        System.out.println("NodeState: deleteWallet called!\n\t" + userId + "\n\t" + walletId); 
        
        if (userId == null || !checkFormat(userId)) {
            System.err.println("Bad user id: " + userId);
            return Status.BAD_USER_ERR;
        }
        if (walletId == null || !checkFormat(walletId)) {
            System.err.println("Bad wallet id: " + walletId);
            return Status.BAD_WALLET_ERR;
        }
        if (!checkWalletUniqueness(walletId)) {
            System.err.println("Wallet id does not exist: " + walletId);
            return Status.UNIQUE_WALLET_ERR; 
        }
        if (!checkUserUniqueness(userId)) {
            System.err.println("User id does not exist: " + userId);
            return Status.UNIQUE_USER_ERR; 
        }
        if (balances.get(walletId) != 0) {
            System.err.println("Wallet id with balance other than 0: " + balances.get(walletId));
            return Status.BALANCE_ERR;
        } 
        if (!checkAuthorization(walletId, userId)) {
            System.err.println("Wallet id " + walletId + "does not belong to user " + userId);
            return Status.AUTHORIZATION_ERR;
        }
        
        // Transaction
        // TODO A.2
        Transaction transaction = new DeleteWalletTransaction(local_transaction_counter, userId, walletId);
        transactions.addFirst(transaction);
        local_transaction_counter++;

        // Execute
        wallets.remove(walletId);
        balances.remove(walletId);
        
        System.err.println("\ttransaction = " + transaction);
        System.err.println("\tSuccessfully Removed!");
        return Status.OK;
    }

    public synchronized Status transfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
 
        System.out.println("NodeState: tranfer called!\n\t" + srcUserId + "\n\tsrc: " + srcWalletId + "\n\tdst: " + dstWalletId + "\n\tamount: " + amount); 

        if (!checkWalletUniqueness(srcWalletId)){ 
            System.err.println("Wallet id does not exist: " + srcWalletId);
            return Status.UNIQUE_WALLET_ERR; 
        }
        if (!checkWalletUniqueness(dstWalletId)){ 
            System.err.println("Wallet id does not exist: " + dstWalletId);
            return Status.UNIQUE_WALLET_ERR; 
        }
        if (!checkAuthorization(srcWalletId, srcUserId)) {
            System.err.println("Wallet id " + srcWalletId + "does not belong to user " + srcUserId);
            return Status.AUTHORIZATION_ERR;
        }

        long srcBalance = balances.get(srcWalletId);
        if (!checkEnoughBalance(srcBalance, amount)) {
            System.err.println("Wallet id " + srcWalletId + "has not enough money (" + srcBalance + ") to transfer " + amount);
            return Status.BALANCE_ERR;
        }
        if (amount <= 0) {
            System.err.println(amount + "should be positive");
            return Status.BAD_AMOUNT;
        } 

        // Transaction
        // TODO A.2
        Transaction transaction = new TransferTransaction(local_transaction_counter, srcUserId, srcWalletId, dstWalletId, amount);
        transactions.addFirst(transaction);
        local_transaction_counter++;

        // EXECUTE
        long dstBalance = balances.get(dstWalletId);
        balances.replace(srcWalletId, srcBalance - amount);
        balances.replace(dstWalletId, dstBalance + amount);

        System.err.println("\ntransaction = " + transaction);
        System.err.println("\tSuccessfully Transferred!");
        return Status.OK;
    }

    // Sync not needed
    public long readBalance(String walletId) {
        System.out.println("NodeState: readBalance called!\n\t" + walletId); 
        long balance = balances.getOrDefault(walletId, -1L);
        System.out.println("\t" + balance);
        return balance;  
    }

    // TODO: Ask teacher: this checks are also important in the server side
    private boolean checkFormat(String input) {
        Pattern pattern = Pattern.compile("^[A-Za-z0-9]+");
        if (!pattern.matcher(input).matches() || input.isBlank()) {
            return false;
        }
        return true;
    }

    private boolean checkUserUniqueness(String userId) {
        return wallets.containsValue(userId);
    }

    private boolean checkWalletUniqueness(String walletId) {
        return wallets.containsKey(walletId);
    }

    // Checks if the wallet belongs to the user
    private boolean checkAuthorization(String walletId, String userId) {
        return wallets.get(walletId).equals(userId);
    }

    private boolean checkEnoughBalance (long balanceSrc, long amount){
        return balanceSrc - amount > 0; 
    }
    // TODO Add other operations (e.g., getBlockchainState)
}
