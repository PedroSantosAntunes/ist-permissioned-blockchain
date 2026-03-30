package pt.tecnico.blockchainist.client;

import java.io.InputStream;
import java.security.KeyFactory;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import pt.tecnico.blockchainist.auth.AuthInfo;
import pt.tecnico.blockchainist.client.grpc.ClientNodeService;

import pt.tecnico.blockchainist.debug.Debug;


public class ClientMain {

    public static void main(String[] args) {
        Map<String, ClientNodeService> nodes = new HashMap<>();

        System.out.println(ClientMain.class.getSimpleName());
        Debug.log("Debug is ON");

        // check arguments
        if (args.length < 1) {
            System.err.println("Argument(s) missing!");
            printUsage();
            return;
        }

        // Load all users private keys to send to NodeClientService
        Map<String, PrivateKey> privateKeys = new HashMap<>();
        for (String userId : AuthInfo.getAllUsers()) {
            try {
                PrivateKey privateKey;
                privateKey = loadPrivateKey(userId + ".priv");
                privateKeys.put(userId, privateKey);
                System.out.println("User Private Key for " + userId + " loaded successfully."); // TODO melhorar get good meter debugs do mike mentira pedro
            } catch (Exception e) {
                System.err.println("Error loading private key for user " + userId + ": " + e.getMessage());
                return;
            }
        }

        // parse arguments
        try {
            for (String arg : args) {
                String[] split = arg.split(":");
                if (split.length != 3) {
                    System.err.println("Invalid argument: " + arg);
                    printUsage();
                    return;
                }
                String host = split[0];
                int port = -1;
                try {
                    port = Integer.parseInt(split[1]);
                } catch (NumberFormatException e) {
                    System.err.println("Invalid port (" + split[1] + ") in argument: " + arg);
                    printUsage();
                    return;
                }
                if (port > 65535 || port < 0) {
                    System.err.println("Port number out of range (0-65535): " + port);
                    printUsage();
                    return;
                }
                String organization = split[2];
                if (nodes.containsKey(organization)) {
                    System.err.println("Node for org already exists: " + organization);
                    printUsage();
                    return;
                }
                nodes.put(organization, new ClientNodeService(host, port, organization, privateKeys));
            }

            CommandProcessor processor = new CommandProcessor(nodes);

            for(ClientNodeService node : nodes.values()) {
                node.setProcessor(processor);
            }
            
            processor.userInputLoop();
        
        } finally {
            terminateNodeChannels(nodes);
        }
    }

    private static void terminateNodeChannels(Map<String, ClientNodeService> nodes) {
        for (ClientNodeService node : nodes.values()) {
            if (node != null) 
                node.closeChannel();
        }
    }

    private static byte[] readResource(String path) throws Exception {
        try (InputStream is = ClientMain.class.getClassLoader().getResourceAsStream(path)) {
            if (is == null) {
                throw new IllegalArgumentException("Ficheiro não encontrado: " + path);
            }
            return is.readAllBytes();
        }
    }

    private static PrivateKey loadPrivateKey(String resourcePath) throws Exception {
        byte[] keyBytes = readResource(resourcePath);
        PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(keyBytes);
        KeyFactory kf = KeyFactory.getInstance("RSA");
        return kf.generatePrivate(spec);
    }

    private static void printUsage() {
        System.err.println("Usage: mvn exec:java -Dexec.args=\"<host>:<port>:<organization> [<host>:<port>:<organization> ...]\"");
    }

}
