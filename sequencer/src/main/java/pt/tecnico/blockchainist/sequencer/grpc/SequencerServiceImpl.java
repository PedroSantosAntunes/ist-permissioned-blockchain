package pt.tecnico.blockchainist.sequencer.grpc;


import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.grpc.RecordToTransaction;
import pt.tecnico.blockchainist.grpc.TransactionToRecord;
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

        int sequence_number = state.Broadcast(record);

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
    public void deliverTransaction(DeliverTransactionRequest request, StreamObserver<DeliverTransactionResponse> responseObserver){

        int sequence_number = request.getSequenceNumber();

        Debug.log("\n-----\nSequencer: Deliver transaction request received!\n" + request);

        TransactionRecord record = state.DeliverTransaction(sequence_number);
        Transaction transaction = RecordToTransaction.recordToTransaction(record);

        DeliverTransactionResponse response = DeliverTransactionResponse.newBuilder().setTransaction(transaction).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    };



    



}
