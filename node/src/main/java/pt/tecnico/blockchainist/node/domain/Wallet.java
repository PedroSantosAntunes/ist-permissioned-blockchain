package pt.tecnico.blockchainist.node.domain;

import java.util.concurrent.locks.ReentrantReadWriteLock;

public class Wallet {

    private String walletId;
    private String userId;
    private long balance;

    private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();

    public Wallet(String walletId, String userId, long balance){
        this.walletId = walletId;
        this.userId = userId;
        this.balance = balance;
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

    @Override
    public String toString() {
        return "Wallet\n\tWalletId: " + walletId + "\n\tUserID: " + userId + "\n\tBalance: " + balance + "\n";
    }
}
