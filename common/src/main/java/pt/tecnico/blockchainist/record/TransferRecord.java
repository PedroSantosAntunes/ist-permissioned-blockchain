package pt.tecnico.blockchainist.record;


public class TransferRecord extends TransactionRecord {

    private String srcUserId;
    private String srcWalletId;
    private String dstWalletId;
    private long amount;

    public TransferRecord(String srcUserId, String srcWalletId, String dstWalletId, long amount){
        this.srcUserId = srcUserId;
        this.srcWalletId = srcWalletId;
        this.dstWalletId = dstWalletId;
        this.amount = amount;
    }

    public String getSrcUserId(){
        return this.srcUserId;
    }

    public String getSrcWalletId(){
        return this.srcWalletId;
    }

    public String getDstWalletId(){
        return this.dstWalletId;
    }

    public long getAmount(){
        return this.amount;
    }

    @Override
    public TransactionType getType(){
        return TransactionType.TRANSFER;
    }
}