package pt.tecnico.blockchainist.node.grpc;

import pt.tecnico.blockchainist.contract.*;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.node.domain.NodeState;

public class NodeServiceImpl extends NodeServiceGrpc.NodeServiceImplBase{
    private final NodeState state;

    public NodeServiceImpl(NodeState state) {
        this.state = state;
    }

    @Override
    public void createWallet(CreateWalletRequest request, StreamObserver<CreateWalletResponse> responseObserver) {

        System.out.println("NodeServiceImpl: createWallet");
        String userId = request.getUserId();
        String walletId = request.getWalletId();

        state.createWallet(userId, walletId);

        CreateWalletResponse response = CreateWalletResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
