package pt.tecnico.blockchainist.record;

public class DeleteWalletRecord extends TransactionRecord {
    private String userId;
    private String walletId;

    public DeleteWalletRecord(String transactionUuid, String userId, String walletId){
        super(transactionUuid);
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

    @Override
    public String toString() {
        return "sequenceNumber: " + getSequenceNumber() + "\n"
            + "deleteWallet {\n"
            + "  userId: \"" + getUserId() + "\"\n"
            + "  walletId: \"" + getWalletId() + "\"\n"
            + "}";
    }
}
