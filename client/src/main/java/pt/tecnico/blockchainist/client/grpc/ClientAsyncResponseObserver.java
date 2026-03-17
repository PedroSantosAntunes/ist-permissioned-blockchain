package pt.tecnico.blockchainist.client.grpc;

import io.grpc.stub.StreamObserver;
// TODO 
public class ClientAsyncResponseObserver<Response> implements StreamObserver<Response> {
    
    public ClientAsyncResponseObserver() {
    }

    @Override
    public void onNext(Response response) {
        //Aqui deve estar o código a executar no caso de resposta normal
        System.out.println("OK ");
    }

    @Override
    public void onError(Throwable throwable) {
        //Aqui deve estar o código a executar no caso de resposta de erro
        System.out.println("Received error: " + throwable);
    }

    @Override
    public void onCompleted() {}
}


