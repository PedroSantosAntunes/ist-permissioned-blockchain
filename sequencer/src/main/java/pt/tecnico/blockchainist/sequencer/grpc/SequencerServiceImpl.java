package pt.tecnico.blockchainist.sequencer.grpc;


import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.grpc.BlockRecordToBlock;
import pt.tecnico.blockchainist.grpc.RecordToTransaction;
import pt.tecnico.blockchainist.grpc.TransactionToRecord;
import pt.tecnico.blockchainist.block.BlockRecord;
import pt.tecnico.blockchainist.sequencer.domain.SequencerState;
import pt.tecnico.blockchainist.record.*;

public class SequencerServiceImpl extends SequencerServiceGrpc.SequencerServiceImplBase{
    private final SequencerState state;

    public SequencerServiceImpl(SequencerState state) {
        this.state = state;
    }

    /**
     * Node sends a new transaction to add to the sequencer blockchain
     * @param request
     * @param responseObserver
     */
    @Override
    public void broadcast(BroadcastRequest request, StreamObserver<BroadcastResponse> responseObserver){

        Transaction transaction = request.getTransaction();

        Debug.log("\n-----\nSequencer: Broadcast request received!\n" + request);
        
        TransactionRecord record =  TransactionToRecord.transactionToRecord(transaction);

        int sequence_number = state.broadcast(record);

        BroadcastResponse response = BroadcastResponse.newBuilder().setSequenceNumber(sequence_number).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    };
  

    /**
     * Node requests the data from a certain transaction to be sent
     * @param resquest
     * @param responseObserver
     */
    @Override
    public void deliverBlock(DeliverBlockRequest request, StreamObserver<DeliverBlockResponse> responseObserver){

        int block_number = request.getBlockNumber();

        Debug.log("\n-----\nSequencer: Deliver block request received!\n" + request);

        BlockRecord record = state.deliverBlock(block_number);
        Block block = BlockRecordToBlock.blockRecordToBlock(record);

        Debug.log("Delivering block to node:\n" + block);

        DeliverBlockResponse response = DeliverBlockResponse.newBuilder().setBlock(block).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    };
    
}
