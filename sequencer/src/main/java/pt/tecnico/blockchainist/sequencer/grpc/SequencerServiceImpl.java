package pt.tecnico.blockchainist.sequencer.grpc;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.security.Signature;
import io.grpc.stub.StreamObserver;
import com.google.protobuf.ByteString;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.grpc.BlockRecordToBlock;
import pt.tecnico.blockchainist.grpc.RecordToTransaction;
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
    public void deliverBlock(DeliverBlockRequest request, StreamObserver<DeliverSignedBlockResponse> responseObserver){

        int block_number = request.getBlockNumber();

        Debug.log("\n-----\nSequencer: Deliver block request received!\n" + request);

        BlockRecord record = state.deliverBlock(block_number);
        Block block = BlockRecordToBlock.blockRecordToBlock(record);


        DeliverSignedBlockResponse response = createSignedResponse(block);

        Debug.log("Delivering block to node:\n" + block);

        responseObserver.onNext(response);
        responseObserver.onCompleted();
    };

    private DeliverSignedBlockResponse createSignedResponse(Block block) {
        try {
            byte[] signatureBytes = signBlock(block.toByteArray());

            SequencerSignature signature = SequencerSignature.newBuilder()
                .setSignatureValue(ByteString.copyFrom(signatureBytes))
                .build();
            DeliverSignedBlockResponse signedResponse = DeliverSignedBlockResponse.newBuilder()
                .setBlock(block)
                .setSignature(signature)
                .build();
            
            return signedResponse;
        } catch (Exception e) {
            // TODO rever exceptions, muita más
            return null;
        }
    }

    private byte[] signBlock(byte[] blockData) throws Exception {
        Signature sig = Signature.getInstance("SHA256withRSA");
        sig.initSign(privateKey);
        sig.update(blockData);
        return sig.sign();
    }

    public void loadPrivateKey() throws RuntimeException {
        try {
            byte[] keyBytes = readResource();
            PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
            KeyFactory kf = KeyFactory.getInstance("RSA");
            this.privateKey = kf.generatePrivate(spec);
            Debug.log("\n-----\nSequencer: Loaded private key!\n");
        } catch (Exception e) {
            System.err.println("\n-----\nSequencer: Failed to load private key!\n");
            throw new RuntimeException(e);
        }
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
