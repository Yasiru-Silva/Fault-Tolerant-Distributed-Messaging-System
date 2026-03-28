package com.messaging.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.model.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class WALManager {
    private static final Logger log = LoggerFactory.getLogger(WALManager.class);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Path walPath;

    public WALManager(@Value("${app.wal.path:wal.log}") String walPath) {
        this.walPath = Path.of(walPath);
    }

    @PostConstruct
    public void initialize() throws IOException {
        if (walPath.getParent() != null) {
            Files.createDirectories(walPath.getParent());
        }
        if (Files.notExists(walPath)) {
            Files.createFile(walPath);
        }
        log.info("WAL initialized at {}", walPath.toAbsolutePath());
    }

    public synchronized void append(Message message) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                walPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(objectMapper.writeValueAsString(message));
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write message to WAL", exception);
        }
    }

    public synchronized List<Message> recover() {
        List<Message> messages = new ArrayList<>();
        if (Files.notExists(walPath)) {
            return messages;
        }

        try {
            for (String line : Files.readAllLines(walPath)) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                messages.add(objectMapper.readValue(line, Message.class));
            }
            log.info("Recovered {} messages from WAL", messages.size());
            return messages;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to recover messages from WAL", exception);
        }
    }
}package com.messaging.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.messaging.model.Message;
import jakarta.annotation.PostConstruct;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.ArrayList;
import java.util.List;

@Component
public class WALManager {
    private static final Logger log = LoggerFactory.getLogger(WALManager.class);

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();
    private final Path walPath;

    public WALManager(@Value("${app.wal.path:wal.log}") String walPath) {
        this.walPath = Path.of(walPath);
    }

    @PostConstruct
    public void initialize() throws IOException {
        if (walPath.getParent() != null) {
            Files.createDirectories(walPath.getParent());
        }
        if (Files.notExists(walPath)) {
            Files.createFile(walPath);
        }
        log.info("WAL initialized at {}", walPath.toAbsolutePath());
    }

    public synchronized void append(Message message) {
        try (BufferedWriter writer = Files.newBufferedWriter(
                walPath,
                StandardOpenOption.CREATE,
                StandardOpenOption.APPEND)) {
            writer.write(objectMapper.writeValueAsString(message));
            writer.newLine();
            writer.flush();
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to write message to WAL", exception);
        }
    }

    public synchronized List<Message> recover() {
        List<Message> messages = new ArrayList<>();
        if (Files.notExists(walPath)) {
            return messages;
        }

        try {
            for (String line : Files.readAllLines(walPath)) {
                if (line == null || line.isBlank()) {
                    continue;
                }
                messages.add(objectMapper.readValue(line, Message.class));
            }
            log.info("Recovered {} messages from WAL", messages.size());
            return messages;
        } catch (IOException exception) {
            throw new IllegalStateException("Failed to recover messages from WAL", exception);
        }
    }
}