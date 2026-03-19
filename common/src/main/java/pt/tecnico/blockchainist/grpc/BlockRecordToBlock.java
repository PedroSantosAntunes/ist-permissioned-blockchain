package pt.tecnico.blockchainist.grpc;

import pt.tecnico.blockchainist.contract.Block;
import pt.tecnico.blockchainist.record.BlockRecord;
import pt.tecnico.blockchainist.record.TransactionRecord;

public abstract class BlockRecordToBlock {
    
    public static Block blockRecordToBlock(BlockRecord record) {
        Block.Builder builder = Block.newBuilder().setBlockNumber(record.getBlockNumber());

        for (TransactionRecord tx : record.getTransactions()) {
            builder.addTransactions(RecordToTransaction.recordToTransaction(tx));
        }

        return builder.build();
    }
}
