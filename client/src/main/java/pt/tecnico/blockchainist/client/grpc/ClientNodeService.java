package pt.tecnico.blockchainist.client.grpc;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import pt.tecnico.blockchainist.client.grpc.*;

import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.SignatureException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.security.Signature;

import com.google.protobuf.ByteString;

import pt.tecnico.blockchainist.client.*;

public class ClientNodeService {

	final ManagedChannel channel;
	private NodeServiceGrpc.NodeServiceBlockingStub syncStub;
	private NodeServiceGrpc.NodeServiceStub asyncStub;
	private String organization;
	private Map<String, PrivateKey> privateKeys;
	
	private CommandProcessor processor;

	private final static long TIME_OUT_SECONDS = 25;
	private static final Metadata.Key<String> DELAY_HEADER_KEY =
        Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

	public ClientNodeService(String host, int port, String organization, Map<String, PrivateKey> privateKeys) {
        final String target = host + ":" + port;

		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.syncStub = NodeServiceGrpc.newBlockingStub(channel);
		this.asyncStub = NodeServiceGrpc.newStub(channel);
		this.organization = organization;
		this.privateKeys = privateKeys;
    }


	public void createWallet(String uuid, String userId, String walletId, Integer delay, Boolean isBlocking) {
		// Build Create Transaction
		CreateWalletRequest request = CreateWalletRequest.newBuilder()
			.setUuid(uuid)
			.setUserId(userId)
			.setWalletId(walletId)
			.build();
		Transaction transaction = Transaction.newBuilder().setCreateWallet(request).build();
		
		// Sign Transaction
		SignedTransaction signedRequest = null;
		try {
			signedRequest = signRequest(transaction, userId);
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			System.err.println("Acunamatatá"); // TODO melhorar get good meter debugs do mike
		}

		Debug.log("\n-----\nClient: Sending create wallet request!\n" + request);

		// Call stub with delay header and deadline
		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		if (isBlocking){
			syncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.createWallet(signedRequest);
			
			Debug.log("\n-----\nClient: Received response for create wallet request!\n");
		} else {
			asyncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.createWallet(signedRequest, new ClientAsyncResponseObserver<CreateWalletResponse>(this.processor, uuid));
		}
	}

	public void deleteWallet(String uuid, String userId, String walletId, Integer delay, Boolean isBlocking){
		// Build Delete Transaction
		DeleteWalletRequest request = DeleteWalletRequest.newBuilder()
			.setUuid(uuid)
			.setUserId(userId)
			.setWalletId(walletId)
			.build();
		Transaction transaction = Transaction.newBuilder().setDeleteWallet(request).build();
		
		// Sign Transaction
		SignedTransaction signedRequest = null;
		try {
			signedRequest = signRequest(transaction, userId);
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			System.err.println("Acunamatatá"); // TODO melhorar get good meter debugs do mike
		}
		Debug.log("\n-----\nClient: Sending delete wallet request!\n" + request);

		// Call stub with delay header and deadline
		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		if (isBlocking) {
			syncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.deleteWallet(signedRequest);
			
			Debug.log("\n-----\nClient: Received response for delete wallet request!\n");
		}
		else {
			asyncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.deleteWallet(signedRequest, new ClientAsyncResponseObserver<DeleteWalletResponse>(this.processor, uuid));
		}
	}

	public long readBalance(String uuid, String walletId, Integer delay, int lastReadBlock, Boolean isBlocking){
		ReadBalanceRequest request = ReadBalanceRequest.newBuilder()
			.setBlockNumber(lastReadBlock)
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending read balance request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		if (isBlocking){
			ReadBalanceResponse response = syncStub
							.withInterceptors(delayInterceptor)
							.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
							.readBalance(request);
			
			Debug.log("\n-----\nClient: Received response for read balance request!\n");

			processor.setLastReadBlock(response.getBlockNumber());
			return response.getBalance();
		}
		asyncStub
			.withInterceptors(delayInterceptor)
			.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
			.readBalance(request, new ClientAsyncResponseObserver<ReadBalanceResponse>(this.processor, uuid));
		return -1L; // Placeholder return value for async case
	}

	public void transfer(String uuid, String srcUserId, String srcWalletId, String dstWalletId, long value, Integer delay, Boolean isBlocking){
		// Build Transfer Transaction
		TransferRequest request = TransferRequest.newBuilder()
			.setUuid(uuid)
			.setSrcUserId(srcUserId)
			.setSrcWalletId(srcWalletId)
			.setDstWalletId(dstWalletId)
			.setValue(value)
			.build();
		Transaction transaction = Transaction.newBuilder().setTransfer(request).build();
		
		// Sign Transaction
		SignedTransaction signedRequest = null;
		try {
			signedRequest = signRequest(transaction, srcUserId);
		} catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
			System.err.println("Acunamatatá");	// TODO melhorar get good meter debugs do mike
		}

		Debug.log("\n-----\nClient: Sending transfer request!\n" + request);

		// Call stub with delay header and deadline
		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		if (isBlocking) {
			syncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.transfer(signedRequest);

			Debug.log("\n-----\nClient: Received response for transfer request!\n");
			}
		else {
			asyncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.transfer(signedRequest, new ClientAsyncResponseObserver<TransferResponse>(this.processor, uuid));
		}
	}

	public String getBlockchainState(){
		GetBlockchainStateRequest request = GetBlockchainStateRequest.getDefaultInstance();

		Debug.log("\n-----\nClient: Sending blockchain state request!\n" + request);

		GetBlockchainStateResponse response = syncStub
												.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
												.getBlockchainState(request);
		return response.toString();
	}

	public String getOrganization(){
		return this.organization;
	}

	private SignedTransaction signRequest(Transaction transaction, String userId) throws SignatureException, NoSuchAlgorithmException, InvalidKeyException {
		Signature sig = Signature.getInstance("SHA256withRSA");
		sig.initSign(privateKeys.get(userId));
		sig.update(transaction.toByteArray());
        byte[] signatureBytes = sig.sign();
		ClientSignature signature = ClientSignature.newBuilder().setSignerIdentifier(userId).setSignature(ByteString.copyFrom(signatureBytes)).build();
		return SignedTransaction.newBuilder().setTransaction(transaction).setSignature(signature).build();
	}

	public void closeChannel(){
		channel.shutdownNow();
	}

	private ClientInterceptor withDelayHeader(Integer nodeDelay) {
		Metadata metadata = new Metadata();
		metadata.put(DELAY_HEADER_KEY, String.valueOf(nodeDelay));

		return MetadataUtils.newAttachHeadersInterceptor((metadata));
	}

	public void setProcessor(CommandProcessor processor) {
		this.processor = processor;
	}
	
}
