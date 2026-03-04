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

		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.stub = SequencerServiceGrpc.newBlockingStub(channel);
    }

	public int broadcast(Transaction transaction){		
        BroadcastRequest request = BroadcastRequest.newBuilder().setTransaction(transaction).build();

		Debug.log("Sent broadcast request to sequencer!\n" + request);
		return stub.broadcast(request).getSequenceNumber();
	}

	public Transaction deliverTransaction(int next_transaction){
		DeliverTransactionRequest request = DeliverTransactionRequest.newBuilder().setSequenceNumber(next_transaction).build();
		
		Debug.log("Sent deliver transaction request to sequencer!\n" + request);
		return stub.deliverTransaction(request).getTransaction();
	}

	public void closeChannel(){
		channel.shutdownNow();
	}

}
