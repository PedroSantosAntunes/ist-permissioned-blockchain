package pt.tecnico.blockchainist.node;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.blockchainist.node.domain.NodeState;
import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import pt.tecnico.blockchainist.node.grpc.NodeServiceImpl;

import pt.tecnico.blockchainist.debug.Debug;

import java.io.IOException;

public class NodeMain {
    public static void main(String[] args)  throws IOException, InterruptedException {
        // TODO

        System.out.println(NodeMain.class.getSimpleName());
        Debug.log("Debug is ON");

        int nodePort;
        String nodeOrg;

        String sequencerHost;
        int sequencerPort;

        // check arguments
        if (args.length != 3) {
            System.err.println("Expected: <nodePort> <nodeOrg> <sequencerHost:sequencerPort>");
            System.err.println("Example: 2001 OrgCoin localhost:3001");
            return;
        }

        // nodePort
        try {
            nodePort = Integer.parseInt(args[0]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid nodePort: " + args[0]);
            return;
        }
        if (nodePort < 0 || nodePort > 65535) {
            System.err.println("nodePort out of range (0-65535): " + nodePort);
            return;
        }

        // nodeOrg
        nodeOrg = args[1];

        // sequencer host:port
        String sequencerArg = args[2];
        String[] split = sequencerArg.split(":", 2); // limit=2 so only splits once
        if (split.length != 2) {
            System.err.println("Invalid sequencer (expected host:port): " + sequencerArg);
            printUsage();
            return;
        }
        sequencerHost = split[0];

        try {
            sequencerPort = Integer.parseInt(split[1]);
        } catch (NumberFormatException e) {
            System.err.println("Invalid sequencer port: " + split[1]);
            printUsage();
            return;
        }
        if (sequencerPort < 0 || sequencerPort > 65535) {
            System.err.println("Sequencer port out of range (0-65535): " + sequencerPort);
            printUsage();
            return;
        }

        NodeSequencerService sequencer = new NodeSequencerService(sequencerHost, sequencerPort);


        NodeState state = new NodeState(sequencer);
        
        final BindableService impl = new NodeServiceImpl(state);

        Server server = ServerBuilder.forPort(nodePort).addService(impl).build();
        
        server.start();
        Debug.log("Node started");
        server.awaitTermination();
        sequencer.closeChannel(); 
        // TODO: check if nothing more needs to be closed
        
    }

    private static void printUsage() {
        System.err.println("Usage: mvn exec:java -Dexec.args=\"<host>:<port>:<organization> [<host>:<port>:<organization> ...]\"");
    }
}
