package com.messaging.zk;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.model.NodeInfo;
import com.messaging.service.NodeService;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Component
public class NodeWatcher implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(NodeWatcher.class);
    private static final String SERVERS_ROOT = "/servers";

    private final ZooKeeper zooKeeper;
    private final NodeService nodeService;
    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    public NodeWatcher(ZooKeeper zooKeeper, NodeService nodeService) {
        this.zooKeeper = zooKeeper;
        this.nodeService = nodeService;
    }

    @PostConstruct
    public void start() throws Exception {
        ensurePath(SERVERS_ROOT);
        registerCurrentNode();
        refreshNodes();
        log.info("Node watcher started for node {}", nodeService.getCurrentNode().getNodeId());
    }

    public synchronized void updateCurrentNodeData() {
        try {
            ensurePath(SERVERS_ROOT);
            registerCurrentNode();
            refreshNodes();
        } catch (Exception exception) {
            throw new IllegalStateException("Failed to update current node registration", exception);
        }
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            if (event.getType() == Event.EventType.NodeChildrenChanged || event.getType() == Event.EventType.None) {
                refreshNodes();
            }
        } catch (Exception exception) {
            log.error("Error while refreshing active nodes", exception);
        }
    }

    private synchronized void registerCurrentNode() throws Exception {
        NodeInfo currentNode = nodeService.getCurrentNode();
        String path = SERVERS_ROOT + "/" + currentNode.getNodeId();
        byte[] data = objectMapper.writeValueAsString(currentNode).getBytes(StandardCharsets.UTF_8);

        if (zooKeeper.exists(path, false) == null) {
            zooKeeper.create(path, data, ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL);
            log.info("Registered node {} at {}", currentNode.getNodeId(), path);
        } else {
            zooKeeper.setData(path, data, -1);
        }
    }

    private synchronized void refreshNodes() throws Exception {
        List<String> children = zooKeeper.getChildren(SERVERS_ROOT, this);
        List<NodeInfo> nodes = new ArrayList<>();

        for (String child : children) {
            String path = SERVERS_ROOT + "/" + child;
            try {
                byte[] data = zooKeeper.getData(path, false, null);
                if (data != null && data.length > 0) {
                    nodes.add(objectMapper.readValue(data, NodeInfo.class));
                }
            } catch (KeeperException.NoNodeException ignored) {
                log.warn("Node {} disappeared during refresh", child);
            }
        }

        nodeService.updateActiveNodes(nodes);
        log.info("Active nodes refreshed. Count={}", nodes.size());
    }

    private void ensurePath(String path) throws KeeperException, InterruptedException {
        if (zooKeeper.exists(path, false) == null) {
            zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }
}