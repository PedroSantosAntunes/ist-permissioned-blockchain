package pt.tecnico.blockchainist.record;


public abstract class TransactionRecord {

    public enum TransactionType {
        CREATE_WALLET,
        DELETE_WALLET,
        TRANSFER
    }

    private int sequence_number = -1;
    private String transactionId;

    public TransactionRecord(String UUID){
        this.transactionId = UUID;
    }

    public String getId() {
        return this.transactionId;
    }

    public int getSequenceNumber() {
        return this.sequence_number;
    }

    public void setSequenceNumber(int sequence_number){
        this.sequence_number = sequence_number;
    }

    public abstract TransactionType getType();

	@Override
	public abstract String toString();

}

