package pt.tecnico.blockchainist.node.domain;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Wallet {

    private String walletId;
    private String userId;
    private long balance;
    private boolean pendingDelete;

    private final AtomicBoolean deleting = new AtomicBoolean(false);
    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Wallet(String walletId, String userId, long balance){
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
        this.pendingDelete = false;
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

    public void setPendingDelete(boolean value) {
        this.pendingDelete = value;
    }

    public boolean getPendingDelete() {
        return this.pendingDelete;
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

    public boolean isDeleting() {
        return deleting.get();
    }

    public void setDeleting(boolean value) {
        deleting.set(value);
    }

    @Override
    public String toString() {
        return "Wallet\n\tWalletId: " + walletId + "\n\tUserID: " + userId + "\n\tBalance: " + balance + "\n";
    }
}
