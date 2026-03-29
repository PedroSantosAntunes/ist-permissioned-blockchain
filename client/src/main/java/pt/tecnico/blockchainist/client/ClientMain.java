package pt.tecnico.blockchainist.client;

import java.util.HashMap;
import java.util.Map;

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

        // parse arguments
        // TODO REMOVER DESNECESSARIO
        // ArrayList<ClientNodeService> nodes = new ArrayList<>(args.length);
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
                // TODO PERGUNTAR: se houver varios nodes para mesma org qual comportamentO?
                if (nodes.containsKey(organization)) {
                    System.err.println("Node for org already exists: " + organization);
                    printUsage();
                    return;
                }
                nodes.put(organization, new ClientNodeService(host, port, organization));
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

    private static void printUsage() {
        System.err.println("Usage: mvn exec:java -Dexec.args=\"<host>:<port>:<organization> [<host>:<port>:<organization> ...]\"");
    }
}
