package pt.tecnico.blockchainist.sequencer;
import java.io.IOException;

import io.grpc.ServerBuilder;
import io.grpc.Server;

public class SequencerMain {
    public static void main(String[] args) throws IOException, InterruptedException {
        // check arguments
        if (args.length < 1) {
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
        Server server = ServerBuilder.forPort(port).build(); // TODO: ADD SERVICE
        server.start();
        System.out.println("Sequencer started");
        server.awaitTermination();
    }


    private static void printUsage() {
        System.err.println("Usage: mvn exec:java -Dexec.args=\"<port>\"");
    }
}