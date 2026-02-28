

public abstract class Transaction {
    int sequence_number;

    public Transaction(int sequence_number){
        this.sequence_number = sequence_number;
    }

    public int getSequenceNumber(){
        return this.sequence_number;
    }

}
