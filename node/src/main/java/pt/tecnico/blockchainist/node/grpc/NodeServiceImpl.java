package pt.tecnico.blockchainist.node.grpc;

import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.node.domain.NodeState;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.record.*;
import pt.tecnico.blockchainist.status.InternalResponseStatus;
import pt.tecnico.blockchainist.grpc.*;

import static io.grpc.Status.*;
import io.grpc.Context;


import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.Signature;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import pt.tecnico.blockchainist.debug.Debug;

public class NodeServiceImpl extends NodeServiceGrpc.NodeServiceImplBase{
    
    private final NodeState state;
    private final Map<String, PublicKey> userPublicKeys;

    public NodeServiceImpl(NodeState state, Map<String, PublicKey> userPublicKeys) {
        this.state = state;
        this.userPublicKeys = userPublicKeys;
    }

    @Override
    public void createWallet(SignedTransaction signedRequest, StreamObserver<CreateWalletResponse> responseObserver) {
        if(!isTransactionValid(signedRequest)) {
            responseObserver.onError(PERMISSION_DENIED.withDescription("Permission Denied: invalid transaction signature").asRuntimeException());
            return;
        }
        
        CreateWalletRequest request = signedRequest.getTransaction().getCreateWallet();
        String uuid = request.getUuid();
        String userId = request.getUserId();
        String walletId = request.getWalletId();

        Debug.log("\n-----\nNode: Create wallet request received!\n" + request);

        InternalResponseStatus result = state.createWallet(uuid, userId, walletId);  

        if(!isError(result, responseObserver)) {
            CreateWalletResponse response = CreateWalletResponse.getDefaultInstance();
            Debug.log("\n-----\nNode: Sending create wallet response!\n" + response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        };
    }

    @Override
    public void deleteWallet(SignedTransaction signedRequest, StreamObserver<DeleteWalletResponse> responseObserver) {
        if(!isTransactionValid(signedRequest)) {
            responseObserver.onError(PERMISSION_DENIED.withDescription("Permission Denied: invalid transaction signature").asRuntimeException());
            return;
        }

        DeleteWalletRequest request = signedRequest.getTransaction().getDeleteWallet();
        String uuid = request.getUuid();
        String userId = request.getUserId();
        String walletId = request.getWalletId();

        Debug.log("\n-----\nNode: Delete wallet request received!\n" + request);

        InternalResponseStatus result = state.deleteWallet(uuid, userId, walletId);
        if(!isError(result, responseObserver)) {
            DeleteWalletResponse response = DeleteWalletResponse.getDefaultInstance();
            Debug.log("\n-----\nNode: Sending delete wallet response!\n" + response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        } 
    }

    @Override
    public void transfer(SignedTransaction signedRequest, StreamObserver<TransferResponse> responseObserver){
        if(!isTransactionValid(signedRequest)) {
            responseObserver.onError(PERMISSION_DENIED.withDescription("Permission Denied: invalid transaction signature").asRuntimeException());
            return;
        }

        TransferRequest request = signedRequest.getTransaction().getTransfer();
        String uuid = request.getUuid();
        String srcUserId = request.getSrcUserId();
        String srcWalletId = request.getSrcWalletId();
        String dstWalletId = request.getDstWalletId();
        Long amount = request.getValue();

        Debug.log("\n-----\nNode: Transfer currency request received!\n" + request);

        InternalResponseStatus result = state.transfer(uuid, srcUserId, srcWalletId, dstWalletId, amount);
        // failed and optimized
        if (isError(result, responseObserver)) {
            return;
        }
        if (result == InternalResponseStatus.EXECUTED_LOCALY) {
            TransferResponse response = TransferResponse.getDefaultInstance();
            Debug.log("\n-----\nNode: Sending transfer response!\n" + response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
            state.optimizedBroadcastTransfer(uuid, srcUserId, srcWalletId, dstWalletId, amount);
            return;
        }

        if(!isError(result, responseObserver)) {
            TransferResponse response = TransferResponse.getDefaultInstance();
            Debug.log("\n-----\nNode: Sending transfer response!\n" + response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }  
    }

    @Override
    public void readBalance(ReadBalanceRequest request, StreamObserver<ReadBalanceResponse> responseObserver) {
        String walletId = request.getWalletId();
        int requestedBlockNumber = request.getBlockNumber();
        Debug.log("\n-----\nNode: Read balance request received!\n" + request);

        NodeState.ReadResult result = state.readBalance(walletId, requestedBlockNumber);
        if (result.balance() == -1L) {
            responseObserver.onError(NOT_FOUND.withDescription("Not Found Wallet: wallet does not exist").asRuntimeException());
        }
        else {
            ReadBalanceResponse response = ReadBalanceResponse.newBuilder().setBalance(result.balance()).setBlockNumber(result.blockNumber()).build();
            Debug.log("\n-----\nNode: Sending read balance response!\n" + response);
            responseObserver.onNext(response);
            responseObserver.onCompleted();
        }
    }

    @Override
    public void getBlockchainState(GetBlockchainStateRequest request, StreamObserver<GetBlockchainStateResponse> responseObserver){
        
		Debug.log("\n-----\nNode: Get blockchain state request received!\n" + request);

        ArrayList<BlockRecord> blocks = state.getBlockchainState();

        GetBlockchainStateResponse.Builder builder = GetBlockchainStateResponse.newBuilder();

        for (BlockRecord blockRecord : blocks){
            List<TransactionRecord> records = blockRecord.getTransactions();
            
            for (TransactionRecord record : records) {
                Transaction tx = RecordToTransaction.recordToTransaction(record);
                if(tx == null) continue;
                builder.addTransactions(tx);
            }
            
        }

        GetBlockchainStateResponse response = builder.build();
        Debug.log("\n-----\nNode: Sending get blockchain state response!\n" + response);
        responseObserver.onNext(response);
        responseObserver.onCompleted();
    }


    private boolean isTransactionValid(SignedTransaction signedRequest){
        Transaction transaction = signedRequest.getTransaction();
        ClientSignature receivedSignature = signedRequest.getSignature();
        try {
			Debug.log("\n-----\nNode: Validating transaction signature!\n");
			Signature sig = Signature.getInstance("SHA256withRSA");
			String identifier = receivedSignature.getSignerIdentifier();
            sig.initVerify(userPublicKeys.get(identifier));
			sig.update(transaction.toByteArray());
			return sig.verify(receivedSignature.getSignature().toByteArray());
		} catch (Exception e) {
			Debug.log("\n-----\nNode: Transaction signature validation failed!\n");
			return false;
		}
    }

    private boolean isError(InternalResponseStatus status, StreamObserver<?> responseObserver) {
        if (status == InternalResponseStatus.OK || status == InternalResponseStatus.EXECUTED_LOCALY) {
            return false;
        }

        switch (status) {
            case BAD_USER_FORMAT:
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid User Format: use only numbers and letters without spaces").asRuntimeException());
                break;
            case BAD_WALLET_FORMAT:
                responseObserver.onError(INVALID_ARGUMENT.withDescription("Invalid Wallet Format: use only numbers and letters without spaces").asRuntimeException());
                break;
            case DUPLICATE_TRANSACTION:
                responseObserver.onError(ALREADY_EXISTS.withDescription("Duplicate transaction").asRuntimeException());
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
            case WRONG_ORGANIZATION:
                responseObserver.onError(PERMISSION_DENIED.withDescription("Permission Required: User does not belong to this organization").asRuntimeException());
                break;
            default:
                responseObserver.onError(UNKNOWN.withDescription("Unknown internal error").asRuntimeException());
                break;
        }
        return true;
    }
}
