package pt.tecnico.blockchainist.node.domain;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import pt.tecnico.blockchainist.contract.CreateWalletResponse;

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

    public synchronized void createWallet(String userId, String walletId) {
        // TODO
        System.out.println("NodeState: createWallet called!\n" + userId + "\n" + walletId);

        if (userId == null || userId.isBlank()) throw new IllegalArgumentException("Bad user id: " + userId);
        if (walletId == null || userId.isBlank()) throw new IllegalArgumentException("Bad wallet id: " + walletId);
        if (wallets.containsKey(walletId)) throw new IllegalArgumentException("Wallet id already exists: " + walletId);

        wallets.put(walletId, userId);
        balances.put(walletId, 0L);

        System.out.println("wallets = " + wallets);
        System.out.println("balances    = " + balances);
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

    // TODO Add other operations (e.g., getBlockchainState)

}
