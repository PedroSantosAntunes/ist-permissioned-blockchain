package pt.tecnico.blockchainist.node.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.grpc.TransactionToRecord;
import pt.tecnico.blockchainist.record.TransactionRecord;
import pt.tecnico.blockchainist.grpc.BlockToBlockRecord;
import pt.tecnico.blockchainist.block.BlockRecord;
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

		Debug.log("Sending broadcast request to sequencer!\n" + request);

		return stub.broadcast(request).getSequenceNumber();
	}


	public int broadcastCreateWallet(String uuid, String userId, String walletId){
		Transaction transaction = Transaction.newBuilder()
        .setCreateWallet(
            CreateWalletRequest.newBuilder()
			.setUuid(uuid)
            .setUserId(userId)
            .setWalletId(walletId)
            .build()
        ).build();

		return broadcast(transaction);
	}

	public int broadcastDeleteWallet(String uuid, String userId, String walletId){
		Transaction transaction = Transaction.newBuilder()
            .setDeleteWallet(
                DeleteWalletRequest.newBuilder()
					.setUuid(uuid)
                    .setUserId(userId)
                    .setWalletId(walletId)
                    .build()
            ).build();

		return broadcast(transaction);
	}

	public int broadcastTransfer(String uuid, String srcUserId, String srcWalletId, String dstWalletId, Long amount){
		Transaction transaction = Transaction.newBuilder()
                .setTransfer(
                    TransferRequest.newBuilder()
						.setUuid(uuid)
                        .setSrcUserId(srcUserId)
                        .setSrcWalletId(srcWalletId)
                        .setDstWalletId(dstWalletId)
                        .setValue(amount)
                        .build()
                ).build();

		return broadcast(transaction);
	}



	public BlockRecord deliverBlock(int next_block){
		DeliverBlockRequest request = DeliverBlockRequest.newBuilder().setBlockNumber(next_block).build();
		
		Debug.log("Sending deliver block request to sequencer!\n" + request);
		Block block = stub.deliverBlock(request).getBlock();

		// Block to block record
		BlockRecord record = BlockToBlockRecord.blockToBlockRecord(block);

		return record;
	}



	public void closeChannel(){
		channel.shutdownNow();
	}

}
