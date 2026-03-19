package pt.tecnico.blockchainist.sequencer;
import java.io.IOException;

import io.grpc.ServerBuilder;
import pt.tecnico.blockchainist.debug.Debug;
import pt.tecnico.blockchainist.sequencer.domain.SequencerState;
import pt.tecnico.blockchainist.sequencer.grpc.SequencerServiceImpl;
import io.grpc.BindableService;
import io.grpc.Server;

public class SequencerMain {
    public static void main(String[] args) throws IOException, InterruptedException {

        System.out.println(SequencerMain.class.getSimpleName());
        Debug.log("Debug is ON");

        // check arguments
        if (args.length < 3) {
            System.err.println("Argument(s) missing!");
            printUsage();
            return;
        }
        int port = -1;
        try {
            port = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid port (" + args[0] + ") in argument");
            printUsage();
            return;
        }
        if (port > 65535 || port < 0) {
            System.err.println("Port number out of range (0-65535): " + port);
            printUsage();
            return;
        } 

        int blockSize = -1;
        try {
            blockSize = Integer.parseInt(args[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid block size N (" + args[1] + ") in argument");
            printUsage();
            return;
        }
        if(blockSize < 0){
            System.err.println("Block size N out of range (0 - 2^32-1): " + blockSize);
            printUsage();
            return;
        }

        int createBlockTimeout = -1;
        try {
            createBlockTimeout = Integer.parseInt(args[2]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid create block timeout T (" + args[2] + ") in argument");
            printUsage();
            return;
        }
        if(createBlockTimeout < 0){
            System.err.println("create block timeout T out of range (0 < T): " + createBlockTimeout);
            printUsage();
            return;
        }

        SequencerState state = new SequencerState(blockSize, createBlockTimeout);

        final BindableService impl = new SequencerServiceImpl(state);

        Server server = ServerBuilder.forPort(port).addService(impl).build();
        server.start();
        Debug.log("Sequencer started");
        server.awaitTermination();
    }


    private static void printUsage() {
        System.err.println("Usage: mvn exec:java -Dexec.args=\"<port> <N> <T>\"");
    }
}