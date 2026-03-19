package pt.tecnico.blockchainist.record;


public abstract class TransactionRecord {

    public enum TransactionType {
        CREATE_WALLET,
        DELETE_WALLET,
        TRANSFER
    }

    private int sequence_number = -1;
    private String transactionUuid;

    public TransactionRecord(String Uuid){
        this.transactionUuid = Uuid;
    }

    public String getUuid() {
        return this.transactionUuid;
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

