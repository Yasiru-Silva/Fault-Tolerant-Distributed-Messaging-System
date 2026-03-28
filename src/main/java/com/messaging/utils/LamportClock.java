package com.messaging.utils;

import org.springframework.stereotype.Component;

import java.util.concurrent.atomic.AtomicLong;

@Component
public class LamportClock {
    private final AtomicLong currentTime = new AtomicLong(0);

    public long tick() {
        return currentTime.incrementAndGet();
    }

    public long update(long externalTime) {
        return currentTime.updateAndGet(local -> Math.max(local, externalTime) + 1);
    }

    public long current() {
        return currentTime.get();
    }

    public void setIfGreater(long value) {
        currentTime.updateAndGet(local -> Math.max(local, value));
    }
}