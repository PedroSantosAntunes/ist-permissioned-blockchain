package pt.tecnico.blockchainist.client.grpc;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;
import pt.tecnico.blockchainist.client.grpc.*;
import java.util.concurrent.TimeUnit;
import pt.tecnico.blockchainist.client.*;

public class ClientNodeService {

	final ManagedChannel channel;
	private NodeServiceGrpc.NodeServiceBlockingStub syncStub;
	private NodeServiceGrpc.NodeServiceStub asyncStub;
	private String organization;
	
	private CommandProcessor processor;

	private final static long TIME_OUT_SECONDS = 10;
	private static final Metadata.Key<String> DELAY_HEADER_KEY =
        Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

	public ClientNodeService(String host, int port, String organization) {
        final String target = host + ":" + port;

		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.syncStub = NodeServiceGrpc.newBlockingStub(channel);
		this.asyncStub = NodeServiceGrpc.newStub(channel);
		this.organization = organization;
    }

	public void createWallet(String uuid, String userId, String walletId, Integer delay, Boolean isBlocking){
		CreateWalletRequest request = CreateWalletRequest.newBuilder()
			.setUuid(uuid)
			.setUserId(userId)
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending create wallet request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		if (isBlocking){
			syncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.createWallet(request);
		} else {
			asyncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.createWallet(request, new ClientAsyncResponseObserver<CreateWalletResponse>(this.processor, uuid));
		}
	}

	public void deleteWallet(String uuid, String userId, String walletId, Integer delay, Boolean isBlocking){
		DeleteWalletRequest request = DeleteWalletRequest.newBuilder()
			.setUuid(uuid)
			.setUserId(userId)
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending delete wallet request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		if (isBlocking) {
			syncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.deleteWallet(request);
		}
		else {
			asyncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.deleteWallet(request, new ClientAsyncResponseObserver<DeleteWalletResponse>(this.processor, uuid));
		}
	}

	public long readBalance(String uuid, String walletId, Integer delay, Boolean isBlocking){
		ReadBalanceRequest request = ReadBalanceRequest.newBuilder()
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending read balance request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		long balance;
		if (isBlocking){
			return syncStub
					.withInterceptors(delayInterceptor)
					.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
					.readBalance(request).getBalance();
		} else {
			asyncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.readBalance(request, new ClientAsyncResponseObserver<ReadBalanceResponse>(this.processor, uuid));
		}
		return -1L; // Placeholder return value for async case
	}

	public void transfer(String uuid, String srcUserId, String srcWalletId, String dstWalletId, long value, Integer delay, Boolean isBlocking){
		TransferRequest request = TransferRequest.newBuilder()
			.setUuid(uuid)
			.setSrcUserId(srcUserId)
			.setSrcWalletId(srcWalletId)
			.setDstWalletId(dstWalletId)
			.setValue(value)
			.build();

		Debug.log("\n-----\nClient: Sending transfer request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		if (isBlocking) {
			syncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.transfer(request);
			}
		else {
			asyncStub
				.withInterceptors(delayInterceptor)
				.withDeadlineAfter(TIME_OUT_SECONDS, TimeUnit.SECONDS)
				.transfer(request, new ClientAsyncResponseObserver<TransferResponse>(this.processor, uuid));
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
