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

	private int broadcast(Transaction transaction){		
        BroadcastRequest request = BroadcastRequest.newBuilder().setTransaction(transaction).build();

		Debug.log("Sent broadcast request to sequencer!\n" + request);
		return stub.broadcast(request).getSequenceNumber();
	}


	public int broadcastCreateWallet(String userId, String walletId){
		Transaction transaction = Transaction.newBuilder()
        .setCreateWallet(
            CreateWalletRequest.newBuilder()
            .setUserId(userId)
            .setWalletId(walletId)
            .build()
        ).build();
		return broadcast(transaction);
	}

	public int broadcastDeleteWallet(String userId, String walletId){
		Transaction transaction = Transaction.newBuilder()
                .setDeleteWallet(
                    DeleteWalletRequest.newBuilder()
                        .setUserId(userId)
                        .setWalletId(walletId)
                        .build()
                ).build();
		return broadcast(transaction);
	}

	public int broadcastTransfer(String srcUserId, String srcWalletId, String dstWalletId, Long amount){
		Transaction transaction = Transaction.newBuilder()
                .setTransfer(
                    TransferRequest.newBuilder()
                        .setSrcUserId(srcUserId)
                        .setSrcWalletId(srcWalletId)
                        .setDstWalletId(dstWalletId)
                        .setValue(amount)
                        .build()
                ).build();
		return broadcast(transaction);
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
