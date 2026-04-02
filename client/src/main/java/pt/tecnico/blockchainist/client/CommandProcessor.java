package pt.tecnico.blockchainist.client;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SignatureException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;
import java.util.Scanner;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;
import java.util.regex.Pattern;

import java.util.UUID;

import io.grpc.StatusRuntimeException;

import pt.tecnico.blockchainist.client.domain.PendingRequest;
import pt.tecnico.blockchainist.client.grpc.ClientNodeService;
import pt.tecnico.blockchainist.auth.AuthInfo;
import javax.management.RuntimeErrorException;

public class CommandProcessor {

    private static final String SPACE = " ";
    private static final String CREATE_BLOCKING = "C";
    private static final String CREATE_ASYNC = "c";
    private static final String DELETE_BLOCKING = "E";
    private static final String DELETE_ASYNC = "e";
    private static final String BALANCE_BLOCKING = "S";
    private static final String BALANCE_ASYNC = "s";
    private static final String TRANSFER_BLOCKING = "T";
    private static final String TRANSFER_ASYNC = "t";
    private static final String DEBUG_BLOCKCHAIN_STATE = "B";
    private static final String PAUSE = "P";
    private static final String EXIT = "X";

    private static final Pattern ID_PATTERN = Pattern.compile("^[a-zA-Z0-9]+$");

    private final AtomicLong commandCounter = new AtomicLong(0);

    private Map<Integer, ClientNodeService> nodes = new HashMap<>(); // <ORG_INDEX_TO_THIS_CLIENT, ClientNodeService>

    private final ConcurrentHashMap<String, PendingRequest> pendingRequests = new ConcurrentHashMap<>(); // <UUID, PENDING REQUEST>
    private int lastReadBlock = 0;

    public CommandProcessor(Map<Integer, ClientNodeService> nodes) {
        this.nodes = nodes;
    }

    void userInputLoop() {
        Scanner scanner = new Scanner(System.in);
        boolean exit = false;

        while (!exit) {
            System.out.print("\n> ");
            String line = scanner.nextLine().trim();
            String[] split = line.split(SPACE);
            try {
                exit = selectCommand(exit, split);
            } catch (IllegalArgumentException e) {
                System.err.println("Error: " + e.getMessage());
                printUsage();
            }
        }
        scanner.close();
    }

    private boolean selectCommand(boolean exit, String[] split) {
        switch (split[0]) {
            case CREATE_BLOCKING: 
                this.create(split, true); 
                break;

            case CREATE_ASYNC:
                this.create(split, false);
                break;

            case DELETE_BLOCKING:
                this.delete(split, true);
                break;

            case DELETE_ASYNC:
                this.delete(split, false);
                break;

            case BALANCE_BLOCKING:
                this.balance(split, true);
                break;

            case BALANCE_ASYNC:
                this.balance(split, false);
                break;

            case TRANSFER_BLOCKING:
                this.transfer(split, true);
                break;

            case TRANSFER_ASYNC:
                this.transfer(split, false);
                break;

            case DEBUG_BLOCKCHAIN_STATE:
                this.debugBlockchainState(split);
                break;

            case PAUSE:
                this.pause(split);
                break;

            case EXIT:
                exit = true;
                break;

            default:
                printUsage();
                break;
        }
        return exit;
    }

    /**
     * Request for the creation of a wallet (criaCarteira)
     * @param split - contains: command, userId, walletId, nodeIndex, nodeDelay
     * @param isBlocking
     */
    private void create(String[] split, boolean isBlocking) {
        this.checkCreateCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();
        String uuid = UUID.randomUUID().toString();
        String type = isBlocking ? CREATE_BLOCKING : CREATE_ASYNC;
        Integer orgIndex = Integer.parseInt(split[3]);

        PendingRequest request = new PendingRequest(commandNumber, type, uuid, orgIndex, split.clone(), isBlocking);
        pendingRequests.put(uuid, request);
        callNode(request);
    }

    /**
     * Request for the deletion of a wallet (eliminaCarteira)
     * @param split
     * @param isBlocking
     */
    private void delete(String[] split, boolean isBlocking) {
        this.checkDeleteCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();
        String uuid = UUID.randomUUID().toString();
        String type = isBlocking ? DELETE_BLOCKING : DELETE_ASYNC;
        Integer orgIndex = Integer.parseInt(split[3]);
        
        PendingRequest request = new PendingRequest(commandNumber, type, uuid, orgIndex, split.clone(), isBlocking);
        pendingRequests.put(uuid, request);
        
        callNode(request);
    }

    /**
     * Request the balance of a wallet (leSaldo)
     * @param split
     * @param isBlocking
     */
    private void balance(String[] split, boolean isBlocking) {
        this.checkBalanceCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        String uuid = UUID.randomUUID().toString();
        String type = isBlocking ? BALANCE_BLOCKING : BALANCE_ASYNC;
        Integer orgIndex = Integer.parseInt(split[2]);

        PendingRequest request = new PendingRequest(commandNumber, type, uuid, orgIndex, split.clone(), isBlocking);
        pendingRequests.put(uuid, request);
        
        callNode(request);
    }

    /**
     * Request to transfer a certain amount from one wallet to another (transfere)
     * @param split
     * @param isBlocking
     */
    private void transfer(String[] split, boolean isBlocking) {
        this.checkTransferCommandArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        String uuid = UUID.randomUUID().toString();
        String type = isBlocking ? TRANSFER_BLOCKING : TRANSFER_ASYNC;
        Integer orgIndex = Integer.parseInt(split[5]);

        PendingRequest request = new PendingRequest(commandNumber, type, uuid, orgIndex, split.clone(), isBlocking);
        pendingRequests.put(uuid, request);
        callNode(request);
    }

    private void callNode(PendingRequest request) {
        ClientNodeService node = nodes.get(request.getOrgIndex());
        String requestType = request.getType();
        String resultToPrint = null;
        try {
            switch(requestType) {
                case CREATE_ASYNC:
                    node.createWallet(request.getUuid(), request.getSplit()[1], request.getSplit()[2], Integer.parseInt(request.getSplit()[4]), false);
                    break;
                case CREATE_BLOCKING:
                    node.createWallet(request.getUuid(), request.getSplit()[1], request.getSplit()[2], Integer.parseInt(request.getSplit()[4]), true);
                    break;
                case DELETE_ASYNC: 
                    node.deleteWallet(request.getUuid(), request.getSplit()[1], request.getSplit()[2], Integer.parseInt(request.getSplit()[4]), false);
                    break;
                case DELETE_BLOCKING:
                    node.deleteWallet(request.getUuid(), request.getSplit()[1], request.getSplit()[2], Integer.parseInt(request.getSplit()[4]), true);
                    break;
                case TRANSFER_ASYNC:
                    node.transfer(request.getUuid(), request.getSplit()[1], request.getSplit()[2], request.getSplit()[3], Long.parseLong(request.getSplit()[4]), Integer.parseInt(request.getSplit()[6]), false);
                    break;
                case TRANSFER_BLOCKING:
                    node.transfer(request.getUuid(), request.getSplit()[1], request.getSplit()[2], request.getSplit()[3], Long.parseLong(request.getSplit()[4]), Integer.parseInt(request.getSplit()[6]), true);
                    break;
                case BALANCE_ASYNC:
                    node.readBalance(request.getUuid(), request.getSplit()[1], Integer.parseInt(request.getSplit()[3]), lastReadBlock, false);
                    break;
                case BALANCE_BLOCKING:
                    long balance = node.readBalance(request.getUuid(), request.getSplit()[1], Integer.parseInt(request.getSplit()[3]), lastReadBlock, true);
                    resultToPrint = String.valueOf(balance);
                    break;
            }

            if (request.getIsBlocking()) {
                concludeOperation(request.getUuid(), resultToPrint);
            }

        } catch (StatusRuntimeException error) {
            handleError(request.getUuid(), error.getStatus().getDescription());
        } catch (SignatureException | NoSuchAlgorithmException | InvalidKeyException e) {
            handleError(request.getUuid(), "Error occurred while signing the request");
        }
    }

    public void concludeOperation(String uuid, String resultToPrint){
        PendingRequest request = pendingRequests.get(uuid);
        pendingRequests.remove(uuid);
        displaySuccessOperation(request.getCommandNumber(),"OK", resultToPrint);
    }

    public void handleError(String uuid, String errorMessage) {
        PendingRequest request = pendingRequests.get(uuid); 
        pendingRequests.remove(uuid);
        displayErrorOperation(request.getCommandNumber(), errorMessage);

        switch (errorMessage) {
            case "UNAVAILABLE", "DEADLINE_EXCEEDED", "CANCELLED":
                if(request.tryNextNode())
                    callNode(request); 
                else {
                    pendingRequests.remove(uuid);
                    displayErrorOperation(request.getCommandNumber(), "every node failed");
                }
                break;

                default:
                    pendingRequests.remove(uuid);
                    displayErrorOperation(request.getCommandNumber(), errorMessage); 
                    break;
        }

    }

    public synchronized static void displaySuccessOperation(Long commandNumber, String statusMessage, String extraOutput) {
        System.out.println(statusMessage + " " + commandNumber);
        if (extraOutput != null) {
            System.out.println(extraOutput);
        }
    }

    private synchronized static void displayErrorOperation(Long commandNumber, String statusMessage) {
        System.err.println(statusMessage + " " + commandNumber);
    }

    public synchronized void setLastReadBlock(int newReadBlock) {
		if (newReadBlock > lastReadBlock){
			lastReadBlock = newReadBlock;
		}
	}

    /**
     * Request current blockchain state (leBlockchain)
     * @param split
     */
    private void debugBlockchainState(String[] split) {
        this.checkDebugBlockchainStateArgs(split);

        Long commandNumber = this.commandCounter.incrementAndGet();

        Integer orgIndex = Integer.parseInt(split[1]);
        ClientNodeService node = nodes.get(orgIndex);
        
        try{
            String transactions = node.getBlockchainState();
            displaySuccessOperation(commandNumber, "OK", null);
            System.out.println(transactions);
        } catch (StatusRuntimeException e) {
            displayErrorOperation(commandNumber, e.getStatus().getDescription());
        }
    }

    private void pause(String[] split) {
        this.checkPauseArgs(split);

        Integer time;

        time = Integer.parseInt(split[1]);

        try {
            Thread.sleep(time * 1000);
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
    }

    private void checkCreateCommandArgs(String[] split) {
        // C|c <user_id> <wallet_id> <node_index> <node_delay>
        if (split.length != 5) {
            throw new IllegalArgumentException("Expected 5 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected User ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        if (!ID_PATTERN.matcher(split[2]).matches()) {
            throw new IllegalArgumentException("Expected Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[2] + "\"");
        }

        try {
            int nodeIndex = Integer.parseInt(split[3]);
            if (!this.nodes.containsKey(nodeIndex)) {
                throw new IllegalArgumentException("No connection found for node " + nodeIndex);
            }
            if (Integer.parseInt(split[4]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected node number, and node delay to be integers");
        }
    }

    private void checkDeleteCommandArgs(String[] split) {
        // E|e <user_id> <wallet_id> <node_index> <node_delay>
        if (split.length != 5) {
            throw new IllegalArgumentException("Expected 5 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected User ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        if (!ID_PATTERN.matcher(split[2]).matches()) {
            throw new IllegalArgumentException("Expected Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[2] + "\"");
        }

        try {
            int nodeIndex = Integer.parseInt(split[3]);
            if (!this.nodes.containsKey(nodeIndex)) {
                throw new IllegalArgumentException("No connection found for node " + nodeIndex);
            }
            if (Integer.parseInt(split[4]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected node number, and node delay to be integers");
        }
    }

    private void checkBalanceCommandArgs(String[] split) {
        // S|s <wallet_id> <node_index> <node_delay>
        if (split.length != 4) {
            throw new IllegalArgumentException("Expected 4 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        try {
            int nodeIndex = Integer.parseInt(split[2]);
            if (!this.nodes.containsKey(nodeIndex)) {
                throw new IllegalArgumentException("No connection found for node " + nodeIndex);
            }
            if (Integer.parseInt(split[3]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected node number and node delay to be integers");
        }
    }

    private void checkTransferCommandArgs(String[] split) {
        // T|t <source_user_id> <source_wallet_id> <destination_wallet_id> <amount> <node_index> <node_delay>
        if (split.length != 7) {
            throw new IllegalArgumentException("Expected 7 arguments, got " + split.length);
        }

        if (!ID_PATTERN.matcher(split[1]).matches()) {
            throw new IllegalArgumentException("Expected Source User ID to be composed of ASCII alphanumeric characters, got \"" + split[1] + "\"");
        }

        if (!ID_PATTERN.matcher(split[2]).matches()) {
            throw new IllegalArgumentException("Expected Source Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[2] + "\"");
        }

        if (!ID_PATTERN.matcher(split[3]).matches()) {
            throw new IllegalArgumentException("Expected Destination Wallet ID to be composed of ASCII alphanumeric characters, got \"" + split[3] + "\"");
        }

        try {
            if (Long.parseLong(split[4]) < 0) {
                throw new IllegalArgumentException("Amount cannot be negative");
            }
            int nodeIndex = Integer.parseInt(split[5]);
            if (!this.nodes.containsKey(nodeIndex)) {
                throw new IllegalArgumentException("No connection found for node " + nodeIndex);
            }
            if (Integer.parseInt(split[6]) < 0) {
                throw new IllegalArgumentException("Node delay cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected amount, node number, and node delay to be integers");
        }
    }

    private void checkDebugBlockchainStateArgs(String[] split) {
        // B <node_index>
        if (split.length != 2) {
            throw new IllegalArgumentException("Expected 2 arguments, got " + split.length);
        }

        try {
            int nodeIndex = Integer.parseInt(split[1]);
            if (!this.nodes.containsKey(nodeIndex)) {
                throw new IllegalArgumentException("No connection found for node " + nodeIndex);
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected node index to be an integer");
        }
    }

    private void checkPauseArgs(String[] split) {
        // P <integer>
        if (split.length != 2) {
            throw new IllegalArgumentException("Expected 2 arguments, got " + split.length);
        }

        try {
            if (Integer.parseInt(split[1]) < 0) {
                throw new IllegalArgumentException("Pause time cannot be negative");
            }
        } catch (NumberFormatException e) {
            throw new IllegalArgumentException("Expected pause time to be an integer");
        }
    }

    private static void printUsage() {
        System.err.println("Usage:\n" +
                "- C|c <user_id> <wallet_id> <node_index> <node_delay>\n" +
                "- E|e <user_id> <wallet_id> <node_index> <node_delay>\n" +
                "- S|s <wallet_id> <node_index> <node_delay>\n" +
                "- T|t <source_user_id> <source_wallet_id> <destination_wallet_id> <amount> <node_index> <node_delay>\n" +
                "- B <node_index>\n" +
                "- P <integer>\n" +
                "- X\n");
    }
}