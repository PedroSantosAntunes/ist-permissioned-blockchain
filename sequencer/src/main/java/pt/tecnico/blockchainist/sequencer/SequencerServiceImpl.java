package pt.tecnico.blockchainist.sequencer;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.contract.BroadcastRequest;
import pt.tecnico.blockchainist.contract.BroadcastResponse;
import pt.tecnico.blockchainist.contract.DeliverTransactionRequest;
import pt.tecnico.blockchainist.contract.DeliverTransactionResponse;
import pt.tecnico.blockchainist.contract.SequencerServiceGrpc;

public class SequencerServiceImpl extends SequencerServiceGrpc.SequencerServiceImplBase {
    
    @Override
    public void broadcast(BroadcastRequest request, StreamObserver<BroadcastResponse> responseObserver) {
        BroadcastResponse response = null; // TODO
        // Send a single response through the stream.
        responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
    }
    
    @Override
    public void deliverTransaction(DeliverTransactionRequest request, StreamObserver<DeliverTransactionResponse> responseObserver) {
        DeliverTransactionResponse response = null; // TODO
        // Send a single response through the stream.
        responseObserver.onNext(response);
		// Notify the client that the operation has been completed.
		responseObserver.onCompleted();
    }

} 