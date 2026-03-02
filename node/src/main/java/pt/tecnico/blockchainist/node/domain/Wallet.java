package pt.tecnico.blockchainist.node.domain;

public class Wallet {

    private String walletId;
    private String userId;
    private Long balance;

    public Wallet(String walletId, String userId, Long balance){
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

    public Long getBalance(){
        return this.balance;
    }


    public void setBalance(Long newBalance){
        this.balance = newBalance;
    }
}
