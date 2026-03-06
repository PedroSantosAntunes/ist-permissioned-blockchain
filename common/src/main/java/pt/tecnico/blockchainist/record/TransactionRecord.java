package pt.tecnico.blockchainist.record;


public abstract class TransactionRecord {

    public enum TransactionType {
        CREATE_WALLET,
        DELETE_WALLET,
        TRANSFER
    }

    private int sequence_number;

    public TransactionRecord(int sequence_number){
        this.sequence_number = sequence_number;
    }

    public int getSequenceNumber() {
        return this.sequence_number;
    }

    public abstract TransactionType getType();

}

