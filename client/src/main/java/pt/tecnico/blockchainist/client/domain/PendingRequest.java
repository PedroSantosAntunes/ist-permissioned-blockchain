package pt.tecnico.blockchainist.client.domain;

public class PendingRequest {

    private long commandNumber;
    private String type;

    private String uuid;
    private String org;
    private String[] split;
    private Boolean isBlocking;

    public PendingRequest(long commandNumber, String type, String uuid, String org, String[] split, boolean isBlocking) {
        this.commandNumber = commandNumber; 
        this.type = type;
        this.org = org;
        this.uuid = uuid;
        this.split = split;
        this.isBlocking = isBlocking;
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

    public String getOrganization() {
        return this.org;
    }
}
