# Distributed Messaging System

A complete Spring Boot prototype for a fault-tolerant distributed messaging system.

## What this implementation provides

- Multiple Spring Boot messaging nodes communicating over REST
- ZooKeeper-based leader election using **ephemeral sequential znodes**
- Automatic failover when the leader crashes
- In-memory storage with **Write-Ahead Log (WAL)** recovery on restart
- Message replication from leader to followers
- UUID-based message deduplication
- Lamport logical clocks for distributed ordering
- Follower catch-up from leader after rejoining
- Postman-ready external APIs

## Architecture summary

- **Coordination model:** ZooKeeper-managed leader election and node membership
- **Replication model:** Primary-backup / leader-follower replication
- **Consistency model:** Coordinated leader-based write path with follower replication and UUID deduplication
- **Ordering model:** Lamport timestamps + creation time + UUID as a stable tie-break order

## Project structure

```text
distributed-messaging/
├─ pom.xml
├─ docker-compose.yml
├─ README.md
├─ src/main/java/com/messaging/
│   ├─ DistributedMessagingApplication.java
│   ├─ controller/
│   │   ├─ GlobalExceptionHandler.java
│   │   ├─ MessageController.java
│   │   ├─ NodeController.java
│   ├─ service/
│   │   ├─ MessageService.java
│   │   ├─ NodeService.java
│   │   ├─ RecoverySyncService.java
│   ├─ model/
│   │   ├─ Message.java
│   │   ├─ NodeInfo.java
│   │   ├─ SendMessageRequest.java
│   ├─ utils/
│   │   ├─ LamportClock.java
│   │   ├─ WALManager.java
│   │   ├─ UUIDGenerator.java
│   ├─ zk/
│   │   ├─ ZooKeeperConfig.java
│   │   ├─ LeaderElection.java
│   │   ├─ NodeWatcher.java
└─ src/main/resources/
    ├─ application.properties
```

## Requirements

- Java 17+
- Maven 3.9+
- Docker Desktop

## Step 1: Start ZooKeeper cluster

From the project folder:

```bash
docker compose up -d
```

Check containers:

```bash
docker ps
```

You should see `zk1`, `zk2`, and `zk3`.

## Step 2: Build the project

```bash
mvn clean install
```

## Step 3: Run 3 Spring Boot nodes in 3 terminals

### Node 1

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DSERVER_PORT=8081 -DNODE_ID=node-1 -DNODE_HOST=localhost -DZK_CONNECT=localhost:2181,localhost:2182,localhost:2183 -DWAL_PATH=./data/wal-8081.log"
```

### Node 2

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DSERVER_PORT=8082 -DNODE_ID=node-2 -DNODE_HOST=localhost -DZK_CONNECT=localhost:2181,localhost:2182,localhost:2183 -DWAL_PATH=./data/wal-8082.log"
```

### Node 3

```bash
mvn spring-boot:run -Dspring-boot.run.jvmArguments="-DSERVER_PORT=8083 -DNODE_ID=node-3 -DNODE_HOST=localhost -DZK_CONNECT=localhost:2181,localhost:2182,localhost:2183 -DWAL_PATH=./data/wal-8083.log"
```

## External APIs

### Send message

`POST /send`

Example request body:

```json
{
  "senderId": "user-1",
  "content": "Hello distributed world"
}
```

### Get all messages

`GET /messages`

### Get current leader

`GET /leader`

### Get active nodes

`GET /nodes`

## Internal APIs used by the system

- `POST /internal/replicate`
- `GET /internal/messages`

## Postman examples

### Send a message to any node

```text
POST http://localhost:8082/send
Content-Type: application/json
```

Body:

```json
{
  "senderId": "client-A",
  "content": "Message sent from Postman"
}
```

### Read messages from a node

```text
GET http://localhost:8083/messages
```

### Check leader

```text
GET http://localhost:8081/leader
```

### Check nodes

```text
GET http://localhost:8081/nodes
```

## Failure simulation

### Kill the current leader

Find leader:

```bash
curl http://localhost:8081/leader
```

Then stop the leader terminal with `CTRL + C`.

### Verify failover

```bash
curl http://localhost:8081/leader
```

A new node should now be leader.

### Restart crashed node

Run its `mvn spring-boot:run ...` command again.

What happens on recovery:

- WAL is replayed
- node re-registers in ZooKeeper
- node rejoins election
- follower synchronizes missing messages from current leader



### Fault tolerance

- Node failure detection is handled through ZooKeeper ephemeral node disappearance
- Automatic failover is handled by leader re-election
- WAL supports restart recovery
- Leader-to-follower replication provides redundancy

### Replication and consistency

- This implementation uses **leader-follower replication**
- UUID deduplication prevents duplicate deliveries
- Followers can catch up by pulling a snapshot from the leader after rejoining

### Time synchronization and ordering

- Lamport clocks provide logical ordering across distributed events
- Physical creation time is still stored for debugging and tie-breaking
- Message retrieval returns messages ordered by Lamport timestamp, then creation time, then UUID

### Consensus choice

- This project uses **ZooKeeper for coordination and leader election**, which is acceptable for this assignment because the coordination responsibility is delegated to a proven distributed system rather than re-implementing Raft or Paxos inside the application nodes



## Common commands

Start ZooKeeper:

```bash
docker compose up -d
```

Stop ZooKeeper:

```bash
docker compose down
```

Build project:

```bash
mvn clean install
```

Check leader:

```bash
curl http://localhost:8081/leader
```
