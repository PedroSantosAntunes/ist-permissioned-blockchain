package pt.tecnico.blockchainist.node.domain;

import java.util.concurrent.locks.ReentrantLock;

public class PendingWalletTransactions {

    private int pendingDeleteCounter;
    private long pendingDeficitAmount;
    private final ReentrantLock lock = new ReentrantLock();

    public PendingWalletTransactions() {
        this.pendingDeleteCounter = 0;
        this.pendingDeficitAmount = 0L;
    }

    public ReentrantLock getLock() {
        return lock;
    }

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
        return "Pending Wallet:\n" +
        "- Pending deletes: " + pendingDeleteCounter + "\n" +
        "- Deficit amount: " + pendingDeficitAmount + "\n";
    }
}
