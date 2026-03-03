package pt.tecnico.blockchainist.node.domain;

public class Wallet {

    private String walletId;
    private String userId;
    private long balance;

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

    @Override
    public String toString() {
        return "Wallet\n\tWalletId: " + walletId + "\n\tUserID: " + userId + "\n\tBalance: " + balance + "\n";
    }
}
