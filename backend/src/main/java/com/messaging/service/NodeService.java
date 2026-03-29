package com.messaging.service;

import com.messaging.model.NodeInfo;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class NodeService {
    private static final Logger log = LoggerFactory.getLogger(NodeService.class);

    private final NodeInfo currentNode;
    private final Map<String, NodeInfo> activeNodes = new ConcurrentHashMap<>();
    private volatile String leaderNodeId;

    public NodeService(@Value("${app.node.id}") String nodeId,
                       @Value("${app.node.url}") String nodeUrl) {
        this.currentNode = new NodeInfo(nodeId, nodeUrl, "FOLLOWER", null, false);
        this.activeNodes.put(nodeId, copy(currentNode));
    }

    public synchronized NodeInfo getCurrentNode() {
        return copy(currentNode);
    }

    public synchronized void updateCurrentElectionPath(String electionPath) {
        currentNode.setElectionZnode(electionPath);
        activeNodes.put(currentNode.getNodeId(), copy(currentNode));
    }

    public synchronized void setLeader(boolean isLeader) {
        currentNode.setLeader(isLeader);
        currentNode.setRole(isLeader ? "LEADER" : "FOLLOWER");
        activeNodes.put(currentNode.getNodeId(), copy(currentNode));
        log.info("Node {} role updated to {}", currentNode.getNodeId(), currentNode.getRole());
    }

    public boolean isLeader() {
        return currentNode.isLeader();
    }

    public synchronized void updateLeaderNodeId(String nodeId) {
        this.leaderNodeId = nodeId;
    }

    public synchronized NodeInfo getLeader() {
        if (leaderNodeId == null) {
            return null;
        }
        NodeInfo leader = activeNodes.get(leaderNodeId);
        if (leader == null && leaderNodeId.equals(currentNode.getNodeId())) {
            return copy(currentNode);
        }
        return leader == null ? null : copy(leader);
    }

    public synchronized String getLeaderUrl() {
        NodeInfo leader = getLeader();
        return leader == null ? null : leader.getUrl();
    }

    public synchronized void updateActiveNodes(List<NodeInfo> nodes) {
        activeNodes.clear();
        for (NodeInfo node : nodes) {
            activeNodes.put(node.getNodeId(), copy(node));
        }
        activeNodes.put(currentNode.getNodeId(), copy(currentNode));
    }

    public List<NodeInfo> getAllNodes() {
        return activeNodes.values().stream()
                .map(this::copy)
                .sorted(Comparator.comparing(NodeInfo::getNodeId))
                .toList();
    }

    private NodeInfo copy(NodeInfo source) {
        if (source == null) {
            return null;
        }
        return new NodeInfo(
                source.getNodeId(),
                source.getUrl(),
                source.getRole(),
                source.getElectionZnode(),
                source.isLeader()
        );
    }
}