package pt.tecnico.blockchainist.node.domain;

import java.util.concurrent.locks.ReentrantLock;

// TODO: ISTO FOI SÓ COPY PASTE DO QUE ESTAVA NA WALLET.
public class PendingWalletTransactions {

    private int pendingDeleteCounter;
    // private int pendingCreateCounter;
    private long pendingDeficitAmount;
    private final ReentrantLock lock = new ReentrantLock();

    public PendingWalletTransactions() {
        this.pendingDeleteCounter = 0;
        // this.pendingCreateCounter = 0;
        this.pendingDeficitAmount = 0L;
    }

    public ReentrantLock getLock() {
        return lock;
    }

    // TODO REMOVER CREATE?
    // public boolean isEmpty() {
    //     return pendingDeleteCounter == 0 &&
    //         pendingCreateCounter == 0 &&
    //         pendingDeficitAmount == 0;
    // }

    // public void incrementCreate() {
    //     pendingCreateCounter++;
    // }

    // public void decrementCreate() {
    //     pendingCreateCounter--;
    // }

    // public int getCreateCounter() {
    //     return pendingCreateCounter;
    // }

    public void incrementDelete() {
        pendingDeleteCounter++;
    }

    public void decrementDelete() {
        pendingDeleteCounter--;
    }

    public int getDeleteCounter() {
        return pendingDeleteCounter;
    }

    public void incrementDeficit(Long amount) {
        pendingDeficitAmount += amount;
    }

    public void decrementDeficit(Long amount) {
        pendingDeficitAmount -= amount;
    }

    public long getDeficitAmount() {
        return pendingDeficitAmount;
    }

    @Override
    public String toString() {
        return "Pending Wallet Transactions: " + pendingDeleteCounter + " " + pendingDeficitAmount + "\n";
    }
}
