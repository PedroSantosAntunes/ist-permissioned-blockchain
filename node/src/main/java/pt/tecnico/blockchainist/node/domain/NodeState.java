package pt.tecnico.blockchainist.node.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

// Checks
import java.util.regex.Pattern;
import pt.tecnico.blockchainist.contract.Status;

public class NodeState {
    
    // TODO Declare state maintained by each node
    // - The set of wallets, indexed by their identifiers, and their owner user identifiers (including the 'bc' wallet)
    // - The balance of each wallet
    // - The transaction ledger (up to A.2, a chain of individual transactions; after B.1, a chain of blocks)

    private final Map<String, String> wallets = new ConcurrentHashMap<>();
    private final Map<String, Long> balances = new ConcurrentHashMap<>();

    // TODO Mudar para sequencer depois
    public static final String BC_WALLET = "bc";
    public static final String BC_NAME = "central bank";
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

        wallets.put(walletId, userId);
        balances.put(walletId, 0L);

        System.err.println("\twallets = " + wallets);
        System.err.println("\tbalances = " + balances);
        return Status.OK;
    }

    public void deleteWallet(String userId, String walletId) {
        // TODO
        System.out.println("NodeState: deleteWallet called!\n" + userId + "\n" + walletId); 
    }

    public void transfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount) {
        // TODO
    }

    public long readBalance(String walletId) {
        // TODO
        return -1L;
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
    // TODO Add other operations (e.g., getBlockchainState)
}
