package pt.tecnico.blockchainist.sequencer.domain;

import java.util.ArrayList;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.debug.Debug;

public class SequencerState {

    private final ArrayList<Transaction> transactions = new ArrayList<Transaction>();
    int global_transaction_counter = 0;

    public SequencerState(){
    }

    /**
     * Store transaction and return its sequence number
     * @param tx
     * @return
     */
    public synchronized int Broadcast(Transaction transaction){

        global_transaction_counter++;
        transactions.add(transaction);

        Debug.log("Transaction added to transactions:\n" + transaction);
        return global_transaction_counter;
    }


    /**
     * Get the transaction with the given sequence number 
     * @param sequence_number
     * @return
     */
    public synchronized Transaction DeliverTransaction(int sequence_number){

        Transaction transaction = getTransaction(sequence_number);

        Debug.log("delivering transaction:\n" + transaction);
        return transaction;
    }

    private synchronized Transaction getTransaction(int sequence_number){
        return transactions.get(sequence_number-1);
    }
}
