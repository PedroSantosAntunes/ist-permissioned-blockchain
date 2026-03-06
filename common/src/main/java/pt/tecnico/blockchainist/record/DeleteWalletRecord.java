package pt.tecnico.blockchainist.record;

public class DeleteWalletRecord extends TransactionRecord {
    private String userId;
    private String walletId;

    public DeleteWalletRecord(int sequence_number, String userId, String walletId){
        super(sequence_number);
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
        return TransactionType.DELETE_WALLET;
    }
}
