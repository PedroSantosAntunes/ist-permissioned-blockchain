package pt.tecnico.blockchainist.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;

public class ClientNodeService {

	final ManagedChannel channel;
	private NodeServiceGrpc.NodeServiceBlockingStub stub;
	private String organization;


	public ClientNodeService(String host, int port, String organization) {

        final String target = host + ":" + port;

		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.stub = NodeServiceGrpc.newBlockingStub(channel);
		this.organization = organization;
    }

	public void createWallet(String userId, String walletId){
		CreateWalletRequest request = CreateWalletRequest.newBuilder()
			.setUserId(userId)
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending create wallet request!\n" + request);

		stub.createWallet(request);
	}

	public void deleteWallet(String userId, String walletId){
		DeleteWalletRequest request = DeleteWalletRequest.newBuilder()
			.setUserId(userId)
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending delete wallet request!\n" + request);

		stub.deleteWallet(request);
	}

	public long readBalance(String walletId){
		ReadBalanceRequest request = ReadBalanceRequest.newBuilder()
			.setWalletId(walletId)
			.build();

		Debug.log("\n-----\nClient: Sending read balance request!\n" + request);

		ReadBalanceResponse response = stub.readBalance(request);
		return response.getBalance();
	}

	public void transfer(String srcUserId, String srcWalletId, String dstWalletId, long value){
		TransferRequest request = TransferRequest.newBuilder()
			.setSrcUserId(srcUserId)
			.setSrcWalletId(srcWalletId)
			.setDstWalletId(dstWalletId)
			.setValue(value)
			.build();

		Debug.log("\n-----\nClient: Sending transfer request!\n" + request);

		stub.transfer(request);
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

}
