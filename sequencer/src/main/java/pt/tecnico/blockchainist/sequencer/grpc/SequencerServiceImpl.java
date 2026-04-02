package pt.tecnico.blockchainist.sequencer.grpc;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.Signature;
import static io.grpc.Status.*;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.grpc.BlockRecordToBlock;
import pt.tecnico.blockchainist.grpc.TransactionToRecord;
import pt.tecnico.blockchainist.sequencer.domain.SequencerState;
import pt.tecnico.blockchainist.record.*;

public class SequencerServiceImpl extends SequencerServiceGrpc.SequencerServiceImplBase{
    private final SequencerState state;
    private PrivateKey privateKey;

    public SequencerServiceImpl(SequencerState state) {
        this.state = state;
    }

    /**
     * Node sends a new transaction to add to the sequencer blockchain
     * @param request
     * @param responseObserver
     */

      @Override
    public void broadcast(BroadcastRequest request, StreamObserver<BroadcastResponse> responseObserver){

        Transaction transaction = request.getTransaction();

        Debug.log("\n-----\nSequencer: Broadcast request received!\n" + request);
        
        TransactionRecord record =  TransactionToRecord.transactionToRecord(transaction);

        state.broadcast(record);

        BroadcastResponse response = BroadcastResponse.newBuilder().build();

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    };

    /**
     * Node requests the data from a certain transaction to be sent
     * @param resquest
     * @param responseObserver
     */
    @Override
    public void deliverBlock(DeliverBlockRequest request, StreamObserver<SignedDeliverBlockResponse> responseObserver){

        int block_number = request.getBlockNumber();

        Debug.log("\n-----\nSequencer: Deliver block request received!\n" + request);
        
        BlockRecord record;

        try{
            record = state.deliverBlock(block_number);
        } catch (InterruptedException e) {
            // failed to get block
            responseObserver.onError(NOT_FOUND.withDescription("Not Found Block: failed to obtain block").asRuntimeException());
            return;
        }

        Block block = BlockRecordToBlock.blockRecordToBlock(record);
        SequencerSignature signedBlock = null;

        try {
            signedBlock = signBlock(block);
        } catch (Exception e) {
            responseObserver.onError(UNKNOWN.withDescription("Unknown internal error").asRuntimeException());
            return;
        }

        DeliverBlockResponse response = DeliverBlockResponse.newBuilder()
            .setBlock(block)
            .build();

        SignedDeliverBlockResponse signedResponse = SignedDeliverBlockResponse.newBuilder()
            .setResponse(response)
            .setSignature(signedBlock)
            .build();

        Debug.log("Delivering block to node:\n" + block);

        responseObserver.onNext(signedResponse);
        responseObserver.onCompleted();
    };


    private SequencerSignature signBlock(Block block) throws Exception{
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(block.toByteArray());

        SequencerSignature signature = SequencerSignature.newBuilder()
            .setSignatureValue(ByteString.copyFrom(sig.sign()))
            .build();

        return signature;
    }

    public void loadPrivateKey() throws Exception {
        byte[] keyBytes = readResource();
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        this.privateKey = kf.generatePrivate(spec);
        Debug.log("\n-----\nSequencer: Loaded private key!\n");
    }

    private byte[] readResource() throws Exception {
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("Seq.priv")) {
            if (is == null) {
                throw new IllegalArgumentException("Private key file not found");
            }
            return is.readAllBytes();
        }
    }
}
