package pt.tecnico.blockchainist.client.domain;

public class PendingRequest {

    private long commandNumber;
    private String type;

    private String uuid;
    private String[] split;
    private Boolean isBlocking;
    private int nodeIndexPosition;
    private int firstNodeRequested;

    public PendingRequest(long commandNumber, String type, String uuid, String[] split, boolean isBlocking, int nodeIndexPosition) {
        this.commandNumber = commandNumber; 
        this.type = type;

        this.uuid = uuid;
        this.split = split;
        this.isBlocking = isBlocking;
        this.nodeIndexPosition = nodeIndexPosition;
        this.firstNodeRequested = Integer.parseInt(split[nodeIndexPosition]);
    }

    public long getCommandNumber() {
        return this.commandNumber;
    }

    public String getType() {
        return this.type;
    }

    public String getUuid() {
        return this.uuid;
    }

    public String[] getSplit() {
        return this.split;
    }

    public boolean getIsBlocking() {
        return this.isBlocking;
    }

    public boolean tryNextNode(int totalNodes) {
        // change to next node
        Integer nextNodeToTry = (Integer.parseInt(split[nodeIndexPosition]) + 1) % totalNodes;
        split[nodeIndexPosition] = nextNodeToTry.toString();

        if (nextNodeToTry == firstNodeRequested) {
            return false;
        }
        return true;
    }

    public int getNodeIndex() {
        return Integer.parseInt(split[nodeIndexPosition]);
    }
}
