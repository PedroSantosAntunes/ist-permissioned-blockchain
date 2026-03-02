package pt.tecnico.blockchainist.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.blockchainist.contract.*;
import java.util.List;

public class ClientNodeService {

	final ManagedChannel channel;
	private NodeServiceGrpc.NodeServiceBlockingStub stub;
	private String organization;


	public ClientNodeService(String host, int port, String organization) {
        // TODO: create channel/stub

        final String target = host + ":" + port;

		// Channel is the abstraction to connect to a service endpoint.
		// Let us use plaintext communication because we do not have certificates.
		this.channel  = ManagedChannelBuilder.forTarget(target).usePlaintext().build();

		this.stub = NodeServiceGrpc.newBlockingStub(channel);
		this.organization = organization;
    }

	public void createWallet(String userId, String walletId){
		CreateWalletRequest request = CreateWalletRequest.newBuilder()
			.setUserId(userId)
			.setWalletId(walletId)
			.build();
		stub.createWallet(request);
	}

	public void deleteWallet(String userId, String walletId){
		DeleteWalletRequest request = DeleteWalletRequest.newBuilder()
			.setUserId(userId)
			.setWalletId(walletId)
			.build();
		stub.deleteWallet(request);
	}

	public long readBalance(String walletId){
		ReadBalanceRequest request = ReadBalanceRequest.newBuilder()
			.setWalletId(walletId)
			.build();
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
		stub.transfer(request);
	}

	public List<Transaction> getBlockchainState(){
		GetBlockchainStateRequest request = GetBlockchainStateRequest.getDefaultInstance();
		GetBlockchainStateResponse response = stub.getBlockchainState(request);
		//TODO: isto vai devolver uma lista de transaction. Mudar para Transaction de outro tipo?
		return response.getTransactionsList();
	}

	public String getOrganization(){
		return this.organization;
	}

	public void closeChannel(){
		channel.shutdownNow();
	}

}
