package pt.tecnico.blockchainist.record;

public class CreateWalletRecord extends TransactionRecord {
    private String userId;
    private String walletId;

    public CreateWalletRecord(String userId, String walletId){
        this.userId = userId;
        this.walletId = walletId;
    }

    public String getUserId() {
        return this.userId;
    }

    public String getWalletId() {
        return this.walletId;
    }

    @Override
    public TransactionType getType(){
        return TransactionType.CREATE_WALLET;
    }
}

