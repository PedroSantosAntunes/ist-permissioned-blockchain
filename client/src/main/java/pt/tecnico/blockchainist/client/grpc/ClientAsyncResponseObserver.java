package pt.tecnico.blockchainist.client.grpc;

import io.grpc.stub.StreamObserver;
import pt.tecnico.blockchainist.client.*;
import io.grpc.StatusRuntimeException;
import pt.tecnico.blockchainist.contract.*;
import pt.tecnico.blockchainist.debug.Debug;

public class ClientAsyncResponseObserver<Response> implements StreamObserver<Response> {
    
    private CommandProcessor processor;
    private String uuid;

    public ClientAsyncResponseObserver(CommandProcessor processor, String uuid) {
        this.processor = processor;
        this.uuid = uuid;
    }

    @Override
    public void onNext(Response response) {
        //Aqui deve estar o código a executar no caso de resposta normal
        String extraOutput = null;
        if (response instanceof ReadBalanceResponse){
            ReadBalanceResponse readResponse = (ReadBalanceResponse) response;
            extraOutput = String.valueOf(readResponse.getBalance());
            processor.setLastReadBlock(readResponse.getBlockNumber());
        }
        Debug.log("\n-----\nClient: Received async response of type: " + response.getClass().getSimpleName() + "!\n");
        processor.concludeOperation(uuid, extraOutput);
    }

    @Override
    public void onError(Throwable throwable) {
        processor.handleError(this.uuid, (StatusRuntimeException) throwable );
    }

    @Override
    public void onCompleted() {}
}


