package pt.tecnico.blockchainist.node.grpc;


import pt.tecnico.blockchainist.transaction.domain.*;
import pt.tecnico.blockchainist.contract.*;
import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.node.domain.NodeState;

import static io.grpc.Status.*;

import java.util.ArrayList;


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

        InternalResponseStatus result = state.createWallet(userId, walletId);  
        if(!handleError(result, responseObserver)) {
            CreateWalletResponse response = CreateWalletResponse.getDefaultInstance();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        };
    }

    @Override
    public void deleteWallet(DeleteWalletRequest request, StreamObserver<DeleteWalletResponse> responseObserver) { 
        
        System.out.println("NodeServiceImpl: deleteWallet");
        String userId = request.getUserId();
        String walletId = request.getWalletId();

        InternalResponseStatus result = state.deleteWallet(userId, walletId);
        if(!handleError(result, responseObserver)) {
            DeleteWalletResponse response = DeleteWalletResponse.getDefaultInstance();
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
            ReadBalanceResponse response = ReadBalanceResponse.newBuilder().setBalance(balance).build();
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

        InternalResponseStatus result = state.transfer(srcUserId, srcWalletId, dstWalletId, amount);
        if(!handleError(result, responseObserver)) {
            TransferResponse response = TransferResponse.getDefaultInstance();
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }  
    }

    @Override
    public void getBlockchainState(GetBlockchainStateRequest request, StreamObserver<GetBlockchainStateResponse> responseObserver){
        System.out.println("NodeServiceImpl: getBlockchainState");
        
        ArrayList<TransactionRecord> transactions = state.getBlockchainState(); // get blockchain

        GetBlockchainStateResponse.Builder builder = GetBlockchainStateResponse.newBuilder();

        for (TransactionRecord tx : transactions) {
            builder.addTransactions(tx.recordToTransaction());
        }

        GetBlockchainStateResponse response = builder.build();
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    private boolean handleError(InternalResponseStatus status, StreamObserver<?> responseObserver) {
        if (status == InternalResponseStatus.OK) {
            return false;
        }

        switch (status) {
            case BAD_USER_FORMAT:
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid User Format: use only numbers and letters without spaces").asRuntimeException());
                break;
            case BAD_WALLET_FORMAT:
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Wallet Format: use only numbers and letters without spaces").asRuntimeException());
                break;
            case WALLET_ALREADY_EXISTS:
                responseObserver.onError(ALREADY_EXISTS.withDescription("Repeated Wallet: wallet already exists").asRuntimeException());
                break;
            case WALLET_NOT_FOUND:
                responseObserver.onError(NOT_FOUND.withDescription("Not Found Wallet: wallet does not exist").asRuntimeException());
                break;
            case USER_NOT_FOUND:
                responseObserver.onError(NOT_FOUND.withDescription("Not Found User: user does not exist").asRuntimeException());
                break;
            case NOT_AUTHORIZED:
                responseObserver.onError(PERMISSION_DENIED.withDescription("Permission Required: wallet does not belong to user").asRuntimeException());
                break;
            case REMAINING_BALANCE:
                responseObserver.onError(FAILED_PRECONDITION.withDescription("PreCondition Required: balance needs to be zero").asRuntimeException());
                break;
            case INSUFFICIENT_BALANCE:
                responseObserver.onError(FAILED_PRECONDITION.withDescription("PreCondition Required: balance is not enough").asRuntimeException());
                break;
            case NEGATIVE_AMOUNT:
                responseObserver.onError(INVALID_ARGUMENT.withDescription("PreCondition Required: amount needs to be positive").asRuntimeException());
                break;
            default:
                responseObserver.onError(UNKNOWN.withDescription("Unknown internal error").asRuntimeException());
                break;
        }
        return true;
    }
}
