package com.messaging.controller;

import com.messaging.model.Message;
import com.messaging.model.SendMessageRequest;
import com.messaging.service.MessageService;
import jakarta.validation.Valid;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
public class MessageController {
    private final MessageService messageService;

    public MessageController(MessageService messageService) {
        this.messageService = messageService;
    }

    @PostMapping("/send")
    @ResponseStatus(HttpStatus.CREATED)
    public Message send(@Valid @RequestBody SendMessageRequest request) {
        return messageService.handleClientSend(request);
    }

    @GetMapping("/messages")
    public List<Message> getMessages() {
        return messageService.getAllMessages();
    }

    @PostMapping("/internal/replicate")
    public Message replicate(@RequestBody Message message) {
        return messageService.receiveReplication(message);
    }

    @GetMapping("/internal/messages")
    public List<Message> internalMessages() {
        return messageService.getAllMessages();
    }
}
