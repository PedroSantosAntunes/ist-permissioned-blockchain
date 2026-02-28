package pt.tecnico.blockchainist.client.grpc;

import io.grpc.ManagedChannel;
import io.grpc.ManagedChannelBuilder;
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
