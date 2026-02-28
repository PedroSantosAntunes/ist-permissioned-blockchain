package pt.tecnico.blockchainist.node.grpc;

import pt.tecnico.blockchainist.contract.*;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.node.domain.NodeState;

import static io.grpc.Status.ALREADY_EXISTS;
import static io.grpc.Status.FAILED_PRECONDITION;
import static io.grpc.Status.INVALID_ARGUMENT;
import static io.grpc.Status.NOT_FOUND;
import static io.grpc.Status.OK;
import static io.grpc.Status.PERMISSION_DENIED;

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

        Status result = state.createWallet(userId, walletId);
        if (result == result.BAD_USER_ERR) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid User Format: use only numbers and letter without spaces").asRuntimeException());
        } 
        else if (result == result.BAD_WALLET_ERR) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Wallet Format: use only numbers and letter without spaces").asRuntimeException());
        }
        else if (result == result.UNIQUE_USER_ERR) {
            responseObserver.onError(ALREADY_EXISTS.withDescription("Repeated User: user already exists").asRuntimeException());
        }
        else if (result == result.UNIQUE_WALLET_ERR) {
            responseObserver.onError(ALREADY_EXISTS.withDescription("Repeated Wallet: wallet already exists").asRuntimeException());
        } 
        else {
            CreateWalletResponse response = CreateWalletResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void deleteWallet(DeleteWalletRequest request, StreamObserver<DeleteWalletResponse> responseObserver) { 
        
        System.out.println("NodeServiceImpl: deleteWallet");
        String userId = request.getUserId();
        String walletId = request.getWalletId();

        Status result = state.deleteWallet(userId, walletId);
        if (result == result.BAD_USER_ERR) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid User Format: use only numbers and letter without spaces").asRuntimeException());
        } 
        else if (result == result.BAD_WALLET_ERR) {
            responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Wallet Format: use only numbers and letter without spaces").asRuntimeException());
        }
        else if (result == result.UNIQUE_USER_ERR) {
            responseObserver.onError(NOT_FOUND.withDescription("Not Found User: user does not exist").asRuntimeException());
        }
        else if (result == result.UNIQUE_WALLET_ERR) {
            responseObserver.onError(NOT_FOUND.withDescription("Not Found Wallet: wallet does not exist").asRuntimeException());
        } 
        else if (result == result.BALANCE_ERR) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("PreCondition Required: balance needs to be zero").asRuntimeException());
        }
        else if (result == result.AUTHORIZATION_ERR) {
            responseObserver.onError(PERMISSION_DENIED.withDescription("Permission Required: wallet does not belongs to user").asRuntimeException());
        }
        else {
            DeleteWalletResponse response = DeleteWalletResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void readBalance(ReadBalanceRequest request, StreamObserver<ReadBalanceResponse> responseObserver) {
        
        System.out.println("NodeServiceImpl: readBalance");
        String walletId = request.getWalletId();

        long balance = state.readBalance(walletId);
        if (balance == -1L) {
            responseObserver.onError(NOT_FOUND.withDescription("Not Found Wallet: wallet does not exist").asRuntimeException());
        }
        else {
            ReadBalanceResponse response = ReadBalanceResponse.newBuilder().setBalance(balance).setStatus(Status.OK).build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void transfer(TransferRequest request, StreamObserver<TransferResponse> responseObserver){

        System.out.println("NodeServiceImpl: transfer");
        String srcUserId = request.getSrcUserId();
        String srcWalletId = request.getSrcWalletId();
        String dstWalletId = request.getDstWalletId();
        Long amount = request.getValue();

        Status result = state.transfer(srcUserId, srcWalletId, dstWalletId, amount);
        if (result == Status.UNIQUE_WALLET_ERR) {
            responseObserver.onError(NOT_FOUND.withDescription("Not Found Wallet: wallet does not exist").asRuntimeException());
        } 
        else if (result == Status.AUTHORIZATION_ERR) {
            responseObserver.onError(PERMISSION_DENIED.withDescription("Permission Required: wallet does not belongs to user").asRuntimeException());
        }
        else if (result == Status.BALANCE_ERR) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("PreCondition Required: balance is not enough").asRuntimeException());
        }
        else if (result == Status.BAD_AMOUNT) {
            responseObserver.onError(FAILED_PRECONDITION.withDescription("PreCondition Required: amount needs to be positive").asRuntimeException());
        }
        else {        
            TransferResponse response = TransferResponse.newBuilder().build();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }
}
