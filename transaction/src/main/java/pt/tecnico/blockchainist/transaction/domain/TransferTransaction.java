package pt.tecnico.blockchainist.transaction.domain;



import pt.tecnico.blockchainist.contract.*;

public class TransferTransaction extends TransactionRecord {

    private String srcUserId;
    private String srcWalletId;
    private String dstWalletId;
    private long value;

    public TransferTransaction(int sequence_number, String srcUserId, String srcWalletId, String dstWalletId, long value){
        super(sequence_number);
        this.srcUserId = srcUserId;
        this.srcWalletId = srcWalletId;
        this.dstWalletId = dstWalletId;
        this.value = value;
    }

    public String getSrcUserId(){
        return this.srcUserId;
    }

    public String getScrWalletId(){
        return this.srcWalletId;
    }

    public String getDstWalletId(){
        return this.dstWalletId;
    }

    public long getValue(){
        return this.value;
    }

    public String toString() {
        return "srcUser: " + this.srcUserId + " srcWallet: " + this.srcWalletId + " dtsWallet: " + this.dstWalletId + " value: " + this.value;
    }

    @Override
    public Transaction recordToTransaction(){
        Transaction transaction = Transaction.newBuilder()
                .setTransfer(
                    TransferRequest.newBuilder()
                        .setSrcUserId(this.srcUserId)
                        .setSrcWalletId(this.srcWalletId)
                        .setDstWalletId(this.dstWalletId)
                        .setValue(this.value)
                        .build()
                ).build();
        return transaction;
    }

    @Override
    public void accept(TransactionVisitor visitor) {
        visitor.execute(this);
    }

}
