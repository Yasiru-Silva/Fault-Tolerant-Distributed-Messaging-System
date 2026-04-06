# Fault-Tolerant Distributed Messaging System

A complete prototype for a fault-tolerant distributed messaging system, consisting of a **Spring Boot** backend and a **React + Vite** frontend.

## Features

- **Distributed Backend Map:** Multiple Spring Boot messaging nodes communicating over REST.
- **Leader Election:** ZooKeeper-based leader election using **ephemeral sequential znodes**.
- **Fault Tolerance & Recovery:** Automatic failover when the leader crashes. In-memory storage with **Write-Ahead Log (WAL)** recovery on restart.
- **Replication:** Message replication from the leader to followers, with follower catch-up capabilities after rejoining.
- **Consistency & Ordering:** UUID-based message deduplication and Lamport logical clocks for distributed ordering.
- **Interactive UI:** React frontend to send messages through any backend node, view the current leader, discovered nodes, live node health, and poll for messages. Displays Lamport timestamps, message IDs, and creation times.

## Architecture Summary

- **Coordination model:** ZooKeeper-managed leader election and node membership.
- **Replication model:** Primary-backup / leader-follower replication.
- **Consistency model:** Coordinated leader-based write path with follower replication and UUID deduplication.
- **Ordering model:** Lamport timestamps + creation time + UUID as a stable tie-break order.

## Requirements

- **Java 17+**
- **Maven 3.9+**
- **Node.js & npm** (for frontend)
- **Docker Desktop** (for ZooKeeper)

---

## Getting Started

### Step 1: Start ZooKeeper Cluster

From the `backend/` folder:

```bash
cd backend
docker compose up -d
```
Check containers with `docker ps`. You should see `zk1`, `zk2`, and `zk3` running.

### Step 2: Build and Run Backend Nodes

Inside the `backend/` directory, build the project:

```bash
mvn clean install
```

Run 3 Spring Boot nodes in 3 separate terminals:

**Node 1:**
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DSERVER_PORT=8081 -DNODE_ID=node-1 -DNODE_HOST=localhost -DZK_CONNECT=localhost:2181,localhost:2182,localhost:2183 -DWAL_PATH=./data/wal-8081.log"
```

**Node 2:**
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DSERVER_PORT=8082 -DNODE_ID=node-2 -DNODE_HOST=localhost -DZK_CONNECT=localhost:2181,localhost:2182,localhost:2183 -DWAL_PATH=./data/wal-8082.log"
```

**Node 3:**
```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DSERVER_PORT=8083 -DNODE_ID=node-3 -DNODE_HOST=localhost -DZK_CONNECT=localhost:2181,localhost:2182,localhost:2183 -DWAL_PATH=./data/wal-8083.log"
```

*(Note: Ensure your backend has CORS configured to accept requests from the frontend on port `5173`. Check `backend/src/main/java/com/messaging/config/WebConfig.java` to verify `http://localhost:5173` is allowed).*

### Step 3: Run the Frontend UI

Open a new terminal and navigate to the `frontend/` directory:

```bash
cd frontend
npm install
npm run dev
```

The frontend runs on `http://localhost:5173`. Open this URL in your browser to interact with the distributed system!

---

## Failure Simulation & Fault Tolerance

### Simulating a Crash
1. Find the current leader via the UI or by a GET request to `http://localhost:8081/leader`
2. Stop the leader's terminal using `CTRL + C`.
3. The UI will automatically update to show the new leader seamlessly. *(ZooKeeper detects the failure via ephemeral node disappearance and triggers re-election.)*

### Restarting a Crashed Node
Run the node's start command again.
On recovery:
- WAL (Write-Ahead Log) is replayed to restore state.
- The node re-registers in ZooKeeper and rejoins the election.
- The follower synchronizes any missing messages from the current leader's snapshot.

---

## API Reference

### External APIs
- `POST /send` - Submit a message body `{"senderId": "user-1", "content": "Hello world"}`
- `GET /messages` - Fetch all ordered messages.
- `GET /leader` - Return information about the current cluster leader.
- `GET /nodes` - List all active nodes in the cluster.
- `GET /health` - Fetch node health status endpoints.

### Internal APIs
- `POST /internal/replicate`
- `GET /internal/messages`
