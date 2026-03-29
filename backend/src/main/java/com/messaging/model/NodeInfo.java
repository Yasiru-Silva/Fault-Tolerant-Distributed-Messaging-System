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

