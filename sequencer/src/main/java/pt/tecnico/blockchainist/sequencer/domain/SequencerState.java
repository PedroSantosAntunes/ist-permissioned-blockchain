package pt.tecnico.blockchainist.sequencer.domain;

import java.util.LinkedList;

import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.transaction.*;

public class SequencerState {


    int global_transaction_counter = 0;

    private final LinkedList<TransactionRecord> transactions = new LinkedList<TransactionRecord>();


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
        transactions.addFirst(transaction);

        return global_transaction_counter;
    }


    /**
     * Get the transaction with the given sequence number 
     * @param sequence_number
     * @return
     */
    public synchronized Transaction DeliverTransaction(int sequence_number){
        Transaction transaction = getTransaction(sequence_number);

        return transaction;
    }


    private synchronized Transaction getTransaction(int sequence_number){
        
        for (TransactionRecord tx : transactions) {            
            if (tx.getSequenceNumber() == sequence_number) {                
                Transaction transaction = tx.recordToTransaction();
                return transaction;
            }
        }
        return null;
    }

    

}
