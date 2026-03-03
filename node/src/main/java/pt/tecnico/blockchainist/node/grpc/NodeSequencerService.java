package pt.tecnico.blockchainist.node.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.blockchainist.contract.*;

import pt.tecnico.blockchainist.debug.Debug;

public class NodeSequencerService {

	final ManagedChannel channel;
	private SequencerServiceGrpc.SequencerServiceBlockingStub stub;


	public NodeSequencerService(String host, int port) {

        final String target = host + ":" + port;


		// Channel is the abstraction to connect to a service endpoint.
		// Let us use plaintext communication because we do not have certificates.
		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.stub = SequencerServiceGrpc.newBlockingStub(channel);
		
    }


	public BroadcastResponse broadcast(BroadcastRequest request){
		Debug.log("Sent broadcast request to sequencer!\n" + request);
		return stub.broadcast(request);
	}

	public DeliverTransactionResponse deliverTransaction(DeliverTransactionRequest request){
		Debug.log("Sent deliver transaction request to sequencer!\n" + request);
		return stub.deliverTransaction(request);
	}


	public SequencerServiceGrpc.SequencerServiceBlockingStub getStub(){
		return this.stub;
	}


	public void closeChannel(){
		channel.shutdownNow();
	}

}
