package com.messaging.service;

import com.messaging.model.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.util.List;

@Service
public class RecoverySyncService {
    private static final Logger log = LoggerFactory.getLogger(RecoverySyncService.class);

    private final NodeService nodeService;
    private final MessageService messageService;
    private final RestTemplate restTemplate;
    private final boolean syncOnStartup;

    public RecoverySyncService(NodeService nodeService,
                               MessageService messageService,
                               RestTemplate restTemplate,
                               @Value("${app.recovery.sync-on-startup:true}") boolean syncOnStartup) {
        this.nodeService = nodeService;
        this.messageService = messageService;
        this.restTemplate = restTemplate;
        this.syncOnStartup = syncOnStartup;
    }

    @PostConstruct
    public void initialSync() {
        if (syncOnStartup) {
            syncFromLeaderIfNeeded();
        }
    }

    public void syncFromLeaderIfNeeded() {
        if (nodeService.isLeader()) {
            return;
        }

        String leaderUrl = nodeService.getLeaderUrl();
        if (leaderUrl == null || leaderUrl.isBlank()) {
            return;
        }

        try {
            ResponseEntity<List<Message>> response = restTemplate.exchange(
                    leaderUrl + "/internal/messages",
                    HttpMethod.GET,
                    null,
                    new ParameterizedTypeReference<>() {
                    }
            );

            List<Message> snapshot = response.getBody();
            if (snapshot != null && !snapshot.isEmpty()) {
                messageService.mergeRecoveredMessages(snapshot, "leader-sync");
                log.info("Follower {} synchronized {} messages from leader {}",
                        nodeService.getCurrentNode().getNodeId(), snapshot.size(), leaderUrl);
            }
        } catch (RestClientException exception) {
            log.warn("Follower {} could not synchronize from leader {}. Cause: {}",
                    nodeService.getCurrentNode().getNodeId(), leaderUrl, exception.getMessage());
        }
    }
}