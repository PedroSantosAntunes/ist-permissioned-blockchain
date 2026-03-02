package pt.tecnico.blockchainist.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
import io.grpc.StatusRuntimeException;
import pt.tecnico.blockchainist.contract.*;

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



	public CreateWalletResponse createWallet(CreateWalletRequest request){
		return stub.createWallet(request);
	}

	public DeleteWalletResponse deleteWallet(DeleteWalletRequest request){
		return stub.deleteWallet(request);
	}

	public ReadBalanceResponse readBalance(ReadBalanceRequest request){
		return stub.readBalance(request);
	}

	public TransferResponse transfer(TransferRequest request){
		return stub.transfer(request);
	}

	public GetBlockchainStateResponse getBlockchainState(GetBlockchainStateRequest request){
		return stub.getBlockchainState(request);
	}





	public String getOrganization(){
		return this.organization;
	}


	public NodeServiceGrpc.NodeServiceBlockingStub getStub(){
		return this.stub;
	}


	public void closeChannel(){
		channel.shutdownNow();
	}

}
