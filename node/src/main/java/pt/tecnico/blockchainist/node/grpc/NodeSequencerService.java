package pt.tecnico.blockchainist.node.grpc;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.security.Signature;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.record.BlockRecord;
import pt.tecnico.blockchainist.grpc.BlockToBlockRecord;
import pt.tecnico.blockchainist.debug.Debug;

public class NodeSequencerService {

	final ManagedChannel channel;
	private SequencerServiceGrpc.SequencerServiceBlockingStub stub;
	private PublicKey publicKey;

	public NodeSequencerService(String host, int port) {
        final String target = host + ":" + port;
		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.stub = SequencerServiceGrpc.newBlockingStub(channel);
    }

	private void broadcast(Transaction transaction){		
        BroadcastRequest request = BroadcastRequest.newBuilder().setTransaction(transaction).build();

		Debug.log("Sending broadcast request to sequencer!\n" + request);

		stub.broadcast(request);
	}


	public void broadcastCreateWallet(String uuid, String userId, String walletId){
		Transaction transaction = Transaction.newBuilder()
        .setCreateWallet(
            CreateWalletRequest.newBuilder()
			.setUuid(uuid)
            .setUserId(userId)
            .setWalletId(walletId)
            .build()
        ).build();

		broadcast(transaction);
	}

	public void broadcastDeleteWallet(String uuid, String userId, String walletId){
		Transaction transaction = Transaction.newBuilder()
            .setDeleteWallet(
                DeleteWalletRequest.newBuilder()
					.setUuid(uuid)
                    .setUserId(userId)
                    .setWalletId(walletId)
                    .build()
            ).build();

		broadcast(transaction);
	}

	public void broadcastTransfer(String uuid, String srcUserId, String srcWalletId, String dstWalletId, Long amount){
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

		broadcast(transaction);
	}

	public BlockRecord deliverBlock(int next_block){
		DeliverBlockRequest request = DeliverBlockRequest.newBuilder().setBlockNumber(next_block).build();
		
		Debug.log("\n-----\nNode: Requesting block from sequencer:" + next_block + "!\n");

		SignedDeliverBlockResponse SignedDeliverBlockResponse = stub.deliverBlock(request);

		Block block = SignedDeliverBlockResponse.getResponse().getBlock();
		SequencerSignature signature = SignedDeliverBlockResponse.getSignature();

		// if signature is invalid
		if(!isBlockValid(block, signature)) {
			Debug.log("\n-----\nNode: Invalid block signature!\n");
			throw new RuntimeException("Failed to validate signature");
		}

		// Block to block record
		BlockRecord record = BlockToBlockRecord.blockToBlockRecord(block);

		return record;
	}


	private boolean isBlockValid(Block block, SequencerSignature receivedSignature){
		try {
			Debug.log("\n-----\nNode: Validating block signature!\n");
			Signature sig = Signature.getInstance("SHA256withRSA");
			sig.initVerify(publicKey);
			
			sig.update(block.toByteArray());
			return sig.verify(receivedSignature.getSignatureValue().toByteArray());
		} catch (Exception e) {
			Debug.log("\n-----\nNode: Block signature validation failed!\n");
			return false;
		}
	}

	public void loadPublicKey() {
		try {
            byte[] keyBytes = readResource();
			X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
			KeyFactory kf = KeyFactory.getInstance("RSA");
			this.publicKey = kf.generatePublic(spec);

            Debug.log("\n-----\nNode: Loaded public key!\n");
        } catch (Exception e) {
            System.err.println("\n-----\nNode: Failed to load public key!\n");
            throw new RuntimeException(e);
        }
    }

    private byte[] readResource() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("seq/Seq.pub")) {
            if (is == null) {
                throw new IllegalArgumentException("Public key file not found");
            }
            return is.readAllBytes();
        }
    }

	public void closeChannel(){
		channel.shutdownNow();
	}

}
