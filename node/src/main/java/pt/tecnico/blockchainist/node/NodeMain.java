package pt.tecnico.blockchainist.node;

import io.grpc.BindableService;
import io.grpc.Server;
import io.grpc.ServerBuilder;
import pt.tecnico.blockchainist.node.domain.NodeState;
import pt.tecnico.blockchainist.node.grpc.NodeSequencerService;
import pt.tecnico.blockchainist.node.grpc.NodeServiceImpl;
import pt.tecnico.blockchainist.node.domain.BlockFetcher;
import pt.tecnico.blockchainist.auth.AuthInfo;
import pt.tecnico.blockchainist.debug.Debug;
import io.grpc.ServerInterceptors;
import pt.tecnico.blockchainist.node.grpc.DelayNodeInterceptor;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PublicKey;
import java.security.spec.X509EncodedKeySpec;
import java.util.HashMap;
import java.util.Map;

public class NodeMain {
    public static void main(String[] args)  throws IOException, InterruptedException {

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

        try {
            sequencer.loadPublicKey();
        } catch (RuntimeException e) {
            System.err.println("\n-----\nNode: Failed to load sequencer public key!\n");
            return;
        }

        NodeState state = new NodeState(sequencer);
        
        BlockFetcher fetcher = new BlockFetcher(state);
        fetcher.setDaemon(true);
        fetcher.start();


        // <UserId,PublicKey>
        Map<String, PublicKey> userPublicKeys = new HashMap<>();
        for (String userId : AuthInfo.getAllUsers()) {
            try {
                PublicKey publicKey = loadPublicKey(userId + ".pub");
                userPublicKeys.put(userId, publicKey);
                System.out.println("User Public Key for " + userId + " loaded successfully.");
            } catch (Exception e) {
                System.err.println("Error loading public key for user " + userId + ": " + e.getMessage());
                return;
            }
        }

        final BindableService impl = new NodeServiceImpl(state, userPublicKeys);

        Server server = ServerBuilder.forPort(nodePort)
            .addService(ServerInterceptors.intercept(
                impl,
                new DelayNodeInterceptor()
            ))
            .build();

        server.start();
        Debug.log("Node started");
        server.awaitTermination();
        sequencer.closeChannel();
    }

    private static void printUsage() {
        System.err.println("Usage: mvn exec:java -Dexec.args=\"<host>:<port>:<organization> [<host>:<port>:<organization> ...]\"");
    }

    // To Node service Imple

    private static PublicKey loadPublicKey(String resourcePath) throws Exception {
        byte[] keyBytes = readResource(resourcePath);
        X509EncodedKeySpec spec = new X509EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePublic(spec);
    }
    private static byte[] readResource(String path) throws Exception {
        try (InputStream is = NodeMain.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Ficheiro não encontrado nos resources: " + path);
            }
            return is.readAllBytes();
        }
    }

}
