package pt.tecnico.blockchainist.grpc;

import java.util.ArrayList;
import java.util.List;

import pt.tecnico.blockchainist.contract.Block;
import pt.tecnico.blockchainist.contract.Transaction;
import pt.tecnico.blockchainist.record.BlockRecord;
import pt.tecnico.blockchainist.record.TransactionRecord;

public abstract class BlockToBlockRecord {

    public static BlockRecord blockToBlockRecord(Block block) {
        List<TransactionRecord> transactions = new ArrayList<>();

        for (Transaction tx : block.getTransactionsList()) {
            transactions.add(TransactionToRecord.transactionToRecord(tx));
        }

        return new BlockRecord(block.getBlockNumber(), transactions);
    }
}
