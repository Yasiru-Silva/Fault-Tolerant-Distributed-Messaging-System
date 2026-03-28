package com.messaging.service;

import com.messaging.model.Message;
import com.messaging.model.NodeInfo;
import com.messaging.model.SendMessageRequest;
import com.messaging.utils.LamportClock;
import com.messaging.utils.UUIDGenerator;
import com.messaging.utils.WALManager;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class MessageService {
    private static final Logger log = LoggerFactory.getLogger(MessageService.class);

    private final Map<String, Message> messageStore = new ConcurrentHashMap<>();
    private final Set<String> knownMessageIds = ConcurrentHashMap.newKeySet();

    private final NodeService nodeService;
    private final LamportClock lamportClock;
    private final WALManager walManager;
    private final UUIDGenerator uuidGenerator;
    private final RestTemplate restTemplate;
    private final String nodeId;

    public MessageService(NodeService nodeService,
                          LamportClock lamportClock,
                          WALManager walManager,
                          UUIDGenerator uuidGenerator,
                          RestTemplate restTemplate,
                          @Value("${app.node.id}") String nodeId) {
        this.nodeService = nodeService;
        this.lamportClock = lamportClock;
        this.walManager = walManager;
        this.uuidGenerator = uuidGenerator;
        this.restTemplate = restTemplate;
        this.nodeId = nodeId;
    }

    @PostConstruct
    public void recoverFromWal() {
        mergeMessages(walManager.recover(), "wal-recovery", false);
        log.info("Node {} loaded {} messages into memory during startup", nodeId, messageStore.size());
    }

    public Message handleClientSend(SendMessageRequest request) {
        if (!nodeService.isLeader()) {
            return forwardToLeader(request);
        }

        Message message = new Message(
                uuidGenerator.generate(),
                request.getSenderId(),
                request.getContent(),
                lamportClock.tick(),
                Instant.now()
        );

        persistMessage(message, "leader-send", true);
        replicateToFollowers(message);
        return message;
    }

    public Message receiveReplication(Message message) {
        lamportClock.update(message.getLamportTimestamp());
        persistMessage(message, "replication", true);
        return message;
    }

    public void mergeRecoveredMessages(List<Message> messages, String source) {
        mergeMessages(messages, source, false);
    }

    public List<Message> getAllMessages() {
        List<Message> messages = new ArrayList<>(messageStore.values());
        messages.sort(Comparator
                .comparingLong(Message::getLamportTimestamp)
                .thenComparing(Message::getCreationTime)
                .thenComparing(Message::getId));
        return messages;
    }

    private Message forwardToLeader(SendMessageRequest request) {
        String leaderUrl = nodeService.getLeaderUrl();
        if (leaderUrl == null || leaderUrl.isBlank()) {
            throw new IllegalStateException("Leader is not available right now");
        }

        try {
            ResponseEntity<Message> response = restTemplate.postForEntity(leaderUrl + "/send", request, Message.class);
            if (response.getBody() == null) {
                throw new IllegalStateException("Leader returned an empty response");
            }
            log.info("Follower {} forwarded message from sender {} to leader {}",
                    nodeId, request.getSenderId(), leaderUrl);
            return response.getBody();
        } catch (RestClientException exception) {
            throw new IllegalStateException("Failed to forward client request to leader at " + leaderUrl, exception);
        }
    }

    private void replicateToFollowers(Message message) {
        List<NodeInfo> nodes = nodeService.getAllNodes();
        for (NodeInfo node : nodes) {
            if (node.getNodeId().equals(nodeId)) {
                continue;
            }

            try {
                HttpHeaders headers = new HttpHeaders();
                headers.setContentType(MediaType.APPLICATION_JSON);
                HttpEntity<Message> entity = new HttpEntity<>(message, headers);
                restTemplate.postForEntity(node.getUrl() + "/internal/replicate", entity, Message.class);
                log.info("Leader {} replicated message {} to follower {}",
                        nodeId, message.getId(), node.getNodeId());
            } catch (RestClientException exception) {
                log.warn("Replication to follower {} failed for message {}. Cause: {}",
                        node.getNodeId(), message.getId(), exception.getMessage());
            }
        }
    }

    private void mergeMessages(List<Message> messages, String source, boolean persistToWal) {
        long maxLamport = lamportClock.current();
        for (Message message : messages) {
            if (message == null || message.getId() == null) {
                continue;
            }
            if (knownMessageIds.add(message.getId())) {
                messageStore.put(message.getId(), message);
                if (persistToWal) {
                    walManager.append(message);
                }
                maxLamport = Math.max(maxLamport, message.getLamportTimestamp());
                log.info("Node {} stored message {} via {} with Lamport {}",
                        nodeId, message.getId(), source, message.getLamportTimestamp());
            } else {
                log.info("Node {} ignored duplicate message {} via {}",
                        nodeId, message.getId(), source);
            }
        }
        lamportClock.setIfGreater(maxLamport);
    }

    private void persistMessage(Message message, String source, boolean persistToWal) {
        mergeMessages(List.of(message), source, persistToWal);
    }
}