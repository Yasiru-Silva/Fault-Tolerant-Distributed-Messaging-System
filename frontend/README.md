# Distributed Messaging Frontend

React + Vite demo UI for the distributed messaging backend.

## Features
- Send messages through any backend node
- View current leader
- View discovered nodes
- View live node health
- Poll messages every 2.5 seconds
- Show Lamport timestamps, message IDs, and creation times

## 1. Install

```bash
npm install
```

## 2. Run

```bash
npm run dev
```

Frontend runs on:

```text
http://localhost:5173
```

## 3. Backend requirement

Start your Spring Boot nodes first:
- `http://localhost:8081`
- `http://localhost:8082`
- `http://localhost:8083`

## 4. CORS setup required on backend

Because the frontend runs on port `5173` and backend runs on `8081/8082/8083`, you must allow CORS in the Spring Boot backend.

### Easiest option: add this class to backend

Create:

```text
src/main/java/com/messaging/config/WebConfig.java
```

Paste:

```java
package com.messaging.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
public class WebConfig implements WebMvcConfigurer {
    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOrigins("http://localhost:5173")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*");
    }
}
```

Then restart all backend nodes.

## 5. Connection flow

Frontend calls these backend APIs:
- `GET /leader`
- `GET /nodes`
- `GET /health`
- `GET /messages`
- `POST /send`

## 6. Demo flow

1. Start ZooKeeper
2. Start node 1, node 2, node 3
3. Add the backend CORS config and restart backend
4. Run frontend
5. Open `http://localhost:5173`
6. Send messages and show replication/failover demo
