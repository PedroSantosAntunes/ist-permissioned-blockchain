package pt.tecnico.blockchainist.sequencer.domain;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.record.*;

public class SequencerState {

    private int MAX_BLOCK_SIZE;
    private int CREATE_BLOCK_SECONDS;

    private final Map<Integer, BlockRecord> blockChain = new ConcurrentHashMap<>();

    private final Deque<TransactionRecord> pendingTransactions = new ArrayDeque<>();

    private int global_transaction_counter = 1;
    private int next_block_number = 1;

    private final ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();
    private ScheduledFuture<?> scheduledTask;

    private Object blockChainLock = new Object();

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
    public synchronized void broadcast(TransactionRecord transaction){

        transaction.setSequenceNumber(global_transaction_counter++);
        addPendingTransaction(transaction);

        return ;
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

        if (blockChain.get(blockNumber) != null) {
            return blockChain.get(blockNumber);
        }

        synchronized (blockChainLock) {
            while (blockChain.get(blockNumber) == null) {
                try{
                    blockChainLock.wait();
                } catch (InterruptedException e) {
                    return new BlockRecord(-1, new ArrayList<TransactionRecord>());
                }
            }
        }
        return blockChain.get(blockNumber);
    }

    private void addPendingTransaction(TransactionRecord tx) {
        pendingTransactions.addLast(tx);

        Debug.log("\n-----\nSequencer: Transaction added to pending transactions:\n" + tx);
        if (pendingTransactions.size() >= MAX_BLOCK_SIZE) {
            createBlock();
        }
    }

    private synchronized void createBlock() {
        try {
            if (pendingTransactions.isEmpty()) {
                return;
            }
            Debug.log("\n-----\nSequencer: Creating block!\n");

            List<TransactionRecord> blockTransactions = new ArrayList<>();

            while (!pendingTransactions.isEmpty() && blockTransactions.size() < MAX_BLOCK_SIZE) {
                blockTransactions.add(pendingTransactions.removeFirst());
            }

            BlockRecord block = new BlockRecord(next_block_number++, blockTransactions);
            
            synchronized (blockChainLock) {
                blockChain.put(block.getBlockNumber(), block);
                blockChainLock.notifyAll();
            }
            Debug.log("New block added to blockchain:\n" + block);
        } finally {
            startTimer();
        }
    }

    private void startTimer() {
        if (scheduledTask != null && !scheduledTask.isDone()) {
            scheduledTask.cancel(false);
        }
        scheduledTask = scheduler.schedule(() -> {
                Debug.log("\n-----\nSequencer: Create block timeout reached!\n");
                createBlock();
            }, CREATE_BLOCK_SECONDS, TimeUnit.SECONDS);
    }
}