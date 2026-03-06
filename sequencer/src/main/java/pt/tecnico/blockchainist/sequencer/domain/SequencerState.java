package pt.tecnico.blockchainist.sequencer.domain;

import java.util.ArrayList;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;

public class SequencerState {

    private final ArrayList<TransactionRecord> transactions = new ArrayList<TransactionRecord>();
    int global_transaction_counter = 0;

    public SequencerState(){
    }

    /**
     * Store transaction and return its sequence number
     * @param tx
     * @return
     */
    public synchronized int Broadcast(TransactionRecord transaction){

        global_transaction_counter++;
        transaction.setSequenceNumber(global_transaction_counter);
        transactions.add(transaction);

        Debug.log("Transaction added to transactions:\n" + transaction);
        return global_transaction_counter;
    }


    /**
     * Get the transaction with the given sequence number 
     * @param sequence_number
     * @return
     */
    public synchronized TransactionRecord DeliverTransaction(int sequence_number){

        TransactionRecord transaction = getTransaction(sequence_number);

        return transaction;
    }

    private synchronized TransactionRecord getTransaction(int sequence_number){
        return transactions.get(sequence_number-1);
    }
}
