


public class DeleteWalletTransaction extends Transaction {

    private String userId;
    private String walletId;


    public DeleteWalletTransaction(int sequence_number, String userId, String walletId){
        super(sequence_number);
        this.userId = userId;
        this.walletId = walletId;
    }

    public String getUserId(){
        return this.userId;
    }

    public String getWalletId(){
        return this.walletId;
    }
}
