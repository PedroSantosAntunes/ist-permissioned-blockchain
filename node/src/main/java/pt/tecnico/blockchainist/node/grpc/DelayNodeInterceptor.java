package pt.tecnico.blockchainist.node.grpc;

import io.grpc.*;

public class DelayNodeInterceptor implements ServerInterceptor {

    static final Metadata.Key<String> DELAY_HEADER_KEY =
        Metadata.Key.of("delay", Metadata.ASCII_STRING_MARSHALLER);

    public static final Context.Key<Integer> DELAY_CONTEXT_KEY = Context.key("delay");

    @Override
    public <ReqT, RespT> ServerCall.Listener<ReqT> interceptCall(
        ServerCall<ReqT, RespT> call,
        final Metadata headers,
        ServerCallHandler<ReqT, RespT> next
    ) {
        String delayValue = headers.get(DELAY_HEADER_KEY);
        if (delayValue != null) {
            try {
                Integer delay = Integer.parseInt(delayValue);
                
                if(delay > 0) {
                    Thread.sleep(delay*1000);
                }

            } catch (NumberFormatException e) {
                System.err.println("Invalid delay value received: " + delayValue);
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        }
        return next.startCall(call, headers);
    }
}

