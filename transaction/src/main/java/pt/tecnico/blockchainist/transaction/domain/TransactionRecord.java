package pt.tecnico.blockchainist.transaction;

public abstract class TransactionRecord {
    int sequence_number;

    public TransactionRecord(int sequence_number){
        this.sequence_number = sequence_number;
    }

    public int getSequenceNumber(){
        return this.sequence_number;
    }

}
