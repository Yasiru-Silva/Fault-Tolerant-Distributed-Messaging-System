package com.messaging.model;

public class NodeInfo {
    private String nodeId;
    private String url;
    private String role;
    private String electionZnode;
    private boolean leader;

    public NodeInfo() {
    }

    public NodeInfo(String nodeId, String url, String role, String electionZnode, boolean leader) {
        this.nodeId = nodeId;
        this.url = url;
        this.role = role;
        this.electionZnode = electionZnode;
        this.leader = leader;
    }

    public String getNodeId() {
        return nodeId;
    }

    public void setNodeId(String nodeId) {
        this.nodeId = nodeId;
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getRole() {
        return role;
    }

    public void setRole(String role) {
        this.role = role;
    }

    public String getElectionZnode() {
        return electionZnode;
    }

    public void setElectionZnode(String electionZnode) {
        this.electionZnode = electionZnode;
    }

    public boolean isLeader() {
        return leader;
    }

    public void setLeader(boolean leader) {
        this.leader = leader;
    }
}