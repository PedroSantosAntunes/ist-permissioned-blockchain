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

    @Override
    public void deleteWallet(DeleteWalletRequest request, StreamObserver<DeleteWalletResponse> responseObserver) { 
        
        System.out.println("NodeServiceImpl: deleteWallet");
        String userId = request.getUserId();
        String walletId = request.getWalletId();

        state.deleteWallet(userId, walletId);

        DeleteWalletResponse response = DeleteWalletResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }

    @Override
    public void readBalance(ReadBalanceRequest request, StreamObserver<ReadBalanceResponse> responseObserver) {
        System.out.println("NodeServiceImpl: readBalance");

        String walletId = request.getWalletId();

        state.readBalance(walletId);

        ReadBalanceResponse response = ReadBalanceResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();

    }

    @Override
    public void transfer(TransferRequest request, StreamObserver<TransferResponse> responseObserver){
        System.out.println("NodeServiceImpl: transfer");

        String srcUserId = request.getSrcUserId();
        String srcWalletId = request.getSrcWalletId();
        String dstWalletId = request.getDstWalletId();
        Long amount = request.getValue();

        state.transfer(srcUserId, srcWalletId, dstWalletId, amount);

        TransferResponse response = TransferResponse.newBuilder().build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }
}
