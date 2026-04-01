package pt.tecnico.blockchainist.node.domain;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Wallet {

    private String walletId;
    private String userId;
    private long balance;

    // private int pendingDeleteCounter;
    // private long pendingDeficitAmount;
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Wallet(String walletId, String userId, long balance){
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        // this.pendingDeficitAmount = 0;
        // this.pendingDeleteCounter = 0;
    }

    public String getWalletId(){
        return this.walletId;
    }

    public String getUserId(){
        return this.userId;
    }

    public long getBalance(){
        return this.balance;
    }

    public void setBalance(long newBalance){
        this.balance = newBalance;
    }

    public ReentrantReadWriteLock.ReadLock readLock() {
        return lock.readLock();
    }

    public ReentrantReadWriteLock.WriteLock writeLock() {
        return lock.writeLock();
    }

    // Delete counter methods
    // public boolean isDeleting() {
    //     return pendingDeleteCounter > 0;
    // }

    // public void incrementDeleting() {
    //     pendingDeleteCounter++;
    // }

    // public void decrementDeleting() {
    //     pendingDeleteCounter--;
    // }

    // // Transfer counter methods
    // public boolean hasSpendableBalance(long value) {
    //     return value <= balance - pendingDeficitAmount;
    // }

    // public void incrementTransfer(Long amount) {
    //     pendingDeficitAmount += amount;
    // }

    // public void decrementTransfer(Long amount) {
    //     pendingDeficitAmount -= amount;
    // }

    @Override
    public String toString() {
        return "Wallet\n\tWalletId: " + walletId + "\n\tUserID: " + userId + "\n\tBalance: " + balance + "\n";
    }
}
