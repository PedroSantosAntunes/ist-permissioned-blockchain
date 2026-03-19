package pt.tecnico.blockchainist.sequencer.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;

public class SequencerState {

    private final ArrayList<BlockRecord> blockChain = new ArrayList<BlockRecord>();
    private int MAX_BLOCK_SIZE;
    private int CREATE_BLOCK_SECONDS;
    
    private final Deque<TransactionRecord> pendingTransactions = new ArrayDeque<>();

    private int global_transaction_counter = 1;
    private int next_block_number = 1;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;

    public SequencerState(int N, int T){
        this.MAX_BLOCK_SIZE = N;
        this.CREATE_BLOCK_SECONDS = T;
        startTimer();
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
    public BlockRecord deliverBlock(int blockNumber){
        if (blockNumber <= 0) {
            return null;
        }

        int size = blockChain.size();

        while (blockNumber > size) {
            try{
                //
                wait();
            } catch (InterruptedException e) {
                return new BlockRecord(-1, new ArrayList<TransactionRecord>());
            }
        }

        return blockChain.get(blockNumber - 1);
    }

    private void addPendingTransaction(TransactionRecord tx) {
        pendingTransactions.addLast(tx);

        if (pendingTransactions.size() >= MAX_BLOCK_SIZE) {
            createBlock();
        }
    }

    private synchronized void createBlock() {
        
        if (pendingTransactions.isEmpty()) {
            startTimer();
            return;
        }

        List<TransactionRecord> blockTransactions = new ArrayList<>();

        while (!pendingTransactions.isEmpty() && blockTransactions.size() < MAX_BLOCK_SIZE) {
            blockTransactions.add(pendingTransactions.removeFirst());
        }

        BlockRecord block = new BlockRecord(next_block_number++, blockTransactions);
        
        blockChain.add(block);

        //
        notifyAll();

        startTimer();
        Debug.log("New block added to blockchain:\n" + block);
    }

    private void startTimer() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
        scheduledTask = scheduler.schedule(() -> createBlock(), CREATE_BLOCK_SECONDS, TimeUnit.SECONDS);
    }
}
