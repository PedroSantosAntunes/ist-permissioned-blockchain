package pt.tecnico.blockchainist.client.grpc;

import io.grpc.ClientInterceptor;
import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import io.grpc.Metadata;
import io.grpc.stub.MetadataUtils;

public class ClientNodeService {

	final ManagedChannel channel;
	private NodeServiceGrpc.NodeServiceBlockingStub stub;
	private String organization;

	static final Metadata.Key<String> DELAY_HEADER_KEY =
        Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

	public ClientNodeService(String host, int port, String organization) {
        final String target = host + ":" + port;

		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.stub = NodeServiceGrpc.newBlockingStub(channel);
		this.organization = organization;
    }

	public void createWallet(String userId, String walletId, Integer delay){
		CreateWalletRequest request = CreateWalletRequest.newBuilder()
			.setUserId(userId)
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending create wallet request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		stub.withInterceptors(delayInterceptor).createWallet(request);
	}

	public void deleteWallet(String userId, String walletId, Integer delay){
		DeleteWalletRequest request = DeleteWalletRequest.newBuilder()
			.setUserId(userId)
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending delete wallet request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		stub.withInterceptors(delayInterceptor).deleteWallet(request);
	}

	public long readBalance(String walletId, Integer delay){
		ReadBalanceRequest request = ReadBalanceRequest.newBuilder()
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending read balance request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		return stub.withInterceptors(delayInterceptor).readBalance(request).getBalance();
	}

	public void transfer(String srcUserId, String srcWalletId, String dstWalletId, long value, Integer delay){
		TransferRequest request = TransferRequest.newBuilder()
			.setSrcUserId(srcUserId)
			.setSrcWalletId(srcWalletId)
			.setDstWalletId(dstWalletId)
			.setValue(value)
			.build();

		Debug.log("\n-----\nClient: Sending transfer request!\n" + request);

		ClientInterceptor delayInterceptor = withDelayHeader(delay);
		stub.withInterceptors(delayInterceptor).transfer(request);
	}

	public String getBlockchainState(){
		GetBlockchainStateRequest request = GetBlockchainStateRequest.getDefaultInstance();

		Debug.log("\n-----\nClient: Sending blockchain state request!\n" + request);

		GetBlockchainStateResponse response = stub.getBlockchainState(request);
		
		return response.toString();
	}

	public String getOrganization(){
		return this.organization;
	}

	public void closeChannel(){
		channel.shutdownNow();
	}

	// for now this is only for blocking stubs
	private ClientInterceptor withDelayHeader(Integer nodeDelay) {
		Metadata metadata = new Metadata();
		metadata.put(DELAY_HEADER_KEY, String.valueOf(nodeDelay));

		return MetadataUtils.newAttachHeadersInterceptor((metadata));
	}
}
