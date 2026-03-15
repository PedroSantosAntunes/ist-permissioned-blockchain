package pt.tecnico.blockchainist.block;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import pt.tecnico.blockchainist.record.TransactionRecord;

public class BlockRecord {
    private final int blockNumber;
    private final List<TransactionRecord> transactions;

    public BlockRecord(int blockNumber, List<TransactionRecord> transactions) {
        this.blockNumber = blockNumber;
        this.transactions = new ArrayList<>(transactions);
    }

    public int getBlockNumber() {
        return this.blockNumber;
    }

    public List<TransactionRecord> getTransactions() {
        return Collections.unmodifiableList(transactions);
    }

    public int size() {
        return transactions.size();
    }
}
