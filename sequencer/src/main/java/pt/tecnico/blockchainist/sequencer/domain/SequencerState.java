package pt.tecnico.blockchainist.sequencer.domain;

import java.util.ArrayList;

import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.transaction.domain.*;

import pt.tecnico.blockchainist.debug.Debug;

public class SequencerState {


    int global_transaction_counter = 0;

    private final ArrayList<TransactionRecord> transactions = new ArrayList<TransactionRecord>();


    public SequencerState(){

    }

    /**
     * Store transaction and return its sequence number
     * @param tx
     * @return
     */
    public synchronized int Broadcast(Transaction tx){

        global_transaction_counter++;
        
        TransactionRecord transaction = TransactionRecord.transactionToRecord(tx, global_transaction_counter);
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
        
        TransactionRecord txRecord = transactions.get(sequence_number-1);
        
        return txRecord.recordToTransaction();
    }

    

}
