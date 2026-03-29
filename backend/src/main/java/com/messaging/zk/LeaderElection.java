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

