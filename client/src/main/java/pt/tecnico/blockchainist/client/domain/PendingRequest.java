package pt.tecnico.blockchainist.client.domain;

import pt.tecnico.blockchainist.auth.AuthInfo;

public class PendingRequest {

    private long commandNumber;
    private String type;

    private String uuid;
    private Integer orgIndex;
    private String[] split;
    private Boolean isBlocking;

    public PendingRequest(long commandNumber, String type, String uuid, Integer orgIndex, String[] split, boolean isBlocking) {
        this.commandNumber = commandNumber; 
        this.type = type;
        this.orgIndex = orgIndex;
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

    public Integer getOrgIndex() {
        return this.orgIndex;
    }

    public boolean tryNextNode() {
        if (orgIndex < AuthInfo.getAllOrganizations().size() - 1) { 
            orgIndex++; 
            return true; 
        }
        return false;
    }
}
