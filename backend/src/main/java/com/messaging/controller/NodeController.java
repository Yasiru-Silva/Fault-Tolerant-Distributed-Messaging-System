package com.messaging.controller;

import com.messaging.model.NodeInfo;
import com.messaging.service.NodeService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
public class NodeController {
    private final NodeService nodeService;

    public NodeController(NodeService nodeService) {
        this.nodeService = nodeService;
    }

    @GetMapping("/leader")
    public NodeInfo getLeader() {
        NodeInfo leader = nodeService.getLeader();
        if (leader == null) {
            throw new IllegalStateException("Leader is not available yet");
        }
        return leader;
    }

    @GetMapping("/nodes")
    public List<NodeInfo> getNodes() {
        return nodeService.getAllNodes();
    }

    @GetMapping("/health")
    public Map<String, Object> health() {
        NodeInfo current = nodeService.getCurrentNode();
        return Map.of(
                "nodeId", current.getNodeId(),
                "role", current.getRole(),
                "leader", current.isLeader(),
                "url", current.getUrl()
        );
    }
}