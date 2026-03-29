package com.messaging.zk;

import com.messaging.service.NodeService;
import com.messaging.service.RecoverySyncService;
import jakarta.annotation.PostConstruct;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException;
import org.apache.zookeeper.WatchedEvent;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

@Component
public class LeaderElection implements Watcher {
    private static final Logger log = LoggerFactory.getLogger(LeaderElection.class);
    private static final String ELECTION_ROOT = "/election";

    private final ZooKeeper zooKeeper;
    private final NodeService nodeService;
    private final NodeWatcher nodeWatcher;
    private final RecoverySyncService recoverySyncService;
    private String currentElectionZnode;

    public LeaderElection(ZooKeeper zooKeeper,
                          NodeService nodeService,
                          NodeWatcher nodeWatcher,
                          RecoverySyncService recoverySyncService) {
        this.zooKeeper = zooKeeper;
        this.nodeService = nodeService;
        this.nodeWatcher = nodeWatcher;
        this.recoverySyncService = recoverySyncService;
    }

    @PostConstruct
    public void start() throws Exception {
        ensurePath(ELECTION_ROOT);
        joinElection();
        electLeader();
        log.info("Leader election started for node {}", nodeService.getCurrentNode().getNodeId());
    }

    @Override
    public void process(WatchedEvent event) {
        try {
            if (event.getType() == Event.EventType.NodeChildrenChanged || event.getType() == Event.EventType.None) {
                electLeader();
            }
        } catch (Exception exception) {
            log.error("Leader election update failed", exception);
        }
    }

    private synchronized void joinElection() throws KeeperException, InterruptedException {
        String nodeId = nodeService.getCurrentNode().getNodeId();
        String path = zooKeeper.create(
                ELECTION_ROOT + "/node-",
                nodeId.getBytes(StandardCharsets.UTF_8),
                ZooDefs.Ids.OPEN_ACL_UNSAFE,
                CreateMode.EPHEMERAL_SEQUENTIAL
        );
        currentElectionZnode = path;
        nodeService.updateCurrentElectionPath(path);
        nodeWatcher.updateCurrentNodeData();
        log.info("Node {} joined leader election as {}", nodeId, path);
    }

    private synchronized void electLeader() throws Exception {
        List<String> children = zooKeeper.getChildren(ELECTION_ROOT, this);
        if (children.isEmpty()) {
            throw new IllegalStateException("No nodes are currently participating in election");
        }

        Collections.sort(children);
        String leaderChild = children.get(0);
        String leaderPath = ELECTION_ROOT + "/" + leaderChild;
        String leaderNodeId = new String(zooKeeper.getData(leaderPath, false, null), StandardCharsets.UTF_8);
        String myShortName = currentElectionZnode.substring(ELECTION_ROOT.length() + 1);
        boolean iAmLeader = myShortName.equals(leaderChild);

        nodeService.updateLeaderNodeId(leaderNodeId);
        nodeService.setLeader(iAmLeader);
        nodeWatcher.updateCurrentNodeData();

        if (iAmLeader) {
            log.info("Node {} became LEADER using znode {}", leaderNodeId, leaderPath);
        } else {
            log.info("Node {} is FOLLOWER. Current leader is {} using znode {}",
                    nodeService.getCurrentNode().getNodeId(), leaderNodeId, leaderPath);
            recoverySyncService.syncFromLeaderIfNeeded();
        }
    }

    private void ensurePath(String path) throws KeeperException, InterruptedException {
        if (zooKeeper.exists(path, false) == null) {
            zooKeeper.create(path, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
        }
    }
}