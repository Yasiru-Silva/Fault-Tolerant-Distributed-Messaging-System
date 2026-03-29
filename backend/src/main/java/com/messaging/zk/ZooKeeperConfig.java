package com.messaging.zk;

import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

@Configuration
public class ZooKeeperConfig {
    private static final Logger log = LoggerFactory.getLogger(ZooKeeperConfig.class);

    @Bean(destroyMethod = "close")
    public ZooKeeper zooKeeper(@Value("${app.zookeeper.connect-string}") String connectString,
                               @Value("${app.zookeeper.session-timeout:5000}") int sessionTimeout) throws IOException, InterruptedException {
        CountDownLatch connectedLatch = new CountDownLatch(1);
        ZooKeeper zooKeeper = new ZooKeeper(connectString, sessionTimeout, event -> {
            if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                connectedLatch.countDown();
            }
            log.info("ZooKeeper event: type={}, state={}, path={}", event.getType(), event.getState(), event.getPath());
        });

        if (!connectedLatch.await(10, TimeUnit.SECONDS)) {
            throw new IllegalStateException("Timed out connecting to ZooKeeper cluster");
        }

        log.info("Connected to ZooKeeper cluster at {}", connectString);
        return zooKeeper;
    }
}