package pt.tecnico.blockchainist.sequencer.grpc;


import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.sequencer.domain.SequencerState;

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

        Debug.log("Broadcast request received!\n"+ transaction);
        
        int sequence_number = state.Broadcast(transaction);

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
    public void deliverTransaction(DeliverTransactionRequest resquest, StreamObserver<DeliverTransactionResponse> responseObserver){

        int sequence_number = resquest.getSequenceNumber();

        Debug.log("Deliver transaction request received!\n\tSequence number: " + sequence_number);

        Transaction transaction = state.DeliverTransaction(sequence_number);

        DeliverTransactionResponse response = DeliverTransactionResponse.newBuilder().setTransaction(transaction).build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    };

}
