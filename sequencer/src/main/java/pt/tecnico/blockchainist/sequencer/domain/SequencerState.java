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

    private final int CREATE_BLOCK_SECONDS = 3;
    private final Object timerLock = new Object();
    private Thread timerThread;
    private boolean resetTimer = false;

    public SequencerState(){
        startCountdownThread();
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
    public synchronized BlockRecord deliverBlock(int blockNumber){
        if (blockNumber <= 0) {
            return null;
        }

        while (blockNumber > blockChain.size()) {
            try {
                wait();
            } catch (InterruptedException e) {
                //TODO tratamento de erros????!!!!
                Thread.currentThread().interrupt();
                return null;
            }
        }

        return blockChain.get(blockNumber - 1);
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
        synchronized (timerLock) {
            resetTimer = true;
            timerLock.notify(); // wakes countdown thread to reset
        }

        List<TransactionRecord> blockTransactions = new ArrayList<>();

        while (!pendingTransactions.isEmpty() && blockTransactions.size() < MAX_TRANSACTIONS_PER_BLOCK) {
            blockTransactions.add(pendingTransactions.removeFirst());
        }

        BlockRecord block = new BlockRecord(next_block_number++, blockTransactions);
        blockChain.add(block);
        notifyAll();

        Debug.log("New block added to blockchain:\n" + block);
    }

    private void startCountdownThread() {
        timerThread = new Thread(() -> {
            while (true) {
                synchronized (timerLock) {
                    resetTimer = false;
                    try {
                        // Wait 5 seconds unless resetTimer becomes true
                        timerLock.wait(CREATE_BLOCK_SECONDS * 1000);
                        if (!resetTimer) {
                            synchronized (this) {
                                createBlock();
                            }
                        }
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        break; // exit thread
                    }
                }
            }
        });
        timerThread.setDaemon(true);
        timerThread.start();
    }
}
