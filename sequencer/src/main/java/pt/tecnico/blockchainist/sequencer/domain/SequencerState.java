package pt.tecnico.blockchainist.sequencer.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;

import pt.tecnico.blockchainist.block.BlockRecord;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;

public class SequencerState {

    private final ArrayList<BlockRecord> blockChain = new ArrayList<BlockRecord>();
    private static final int MAX_TRANSACTIONS_PER_BLOCK = 5;
    private int next_block_number = 1;

    private final Deque<TransactionRecord> pendingTransactions = new ArrayDeque<>();
    private int global_transaction_counter = 1;

    public SequencerState(){
    }

    /**
     * Store transaction and return its sequence number
     * @param tx
     * @return
     */
    public synchronized int broadcast(TransactionRecord transaction){

        transaction.setSequenceNumber(global_transaction_counter++);
        addPendingTransaction(transaction);

        Debug.log("Transaction added to pending transactions:\n" + transaction);

        return global_transaction_counter;
    }


    /**
     * Get the transaction with the given sequence number 
     * @param sequence_number
     * @return
     */
    public synchronized BlockRecord deliverBlock(int sequence_number){

        if (sequence_number <= 0 || sequence_number > blockChain.size()) {
            return null;
        }

        return blockChain.get(sequence_number);
    }

    private void addPendingTransaction(TransactionRecord tx) {
        pendingTransactions.addLast(tx);

        if (pendingTransactions.size() >= MAX_TRANSACTIONS_PER_BLOCK) {
            createBlock();
        }
    }

    private void createBlock() {
        if (pendingTransactions.isEmpty()) {
            return;
        }

        List<TransactionRecord> blockTransactions = new ArrayList<>();

        while (!pendingTransactions.isEmpty() && blockTransactions.size() < MAX_TRANSACTIONS_PER_BLOCK) {
            blockTransactions.add(pendingTransactions.removeFirst());
        }

        BlockRecord block = new BlockRecord(next_block_number++, blockTransactions);
        blockChain.add(block);

        Debug.log("New block added to blockchain:\n" + block);
    }
}
