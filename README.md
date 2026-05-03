<div align="center">

# ⚡ SwiftPay

### Event-Driven Payment Processing Platform

[![Java](https://img.shields.io/badge/Java-21-ED8B00?style=for-the-badge&logo=openjdk&logoColor=white)](https://openjdk.org/projects/jdk/21/)
[![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.4%2F3.5-6DB33F?style=for-the-badge&logo=springboot&logoColor=white)](https://spring.io/projects/spring-boot)
[![Apache Kafka](https://img.shields.io/badge/Apache_Kafka-231F20?style=for-the-badge&logo=apachekafka&logoColor=white)](https://kafka.apache.org/)
[![Redis](https://img.shields.io/badge/Redis-DC382D?style=for-the-badge&logo=redis&logoColor=white)](https://redis.io/)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-4169E1?style=for-the-badge&logo=postgresql&logoColor=white)](https://www.postgresql.org/)
[![Docker](https://img.shields.io/badge/Docker-2496ED?style=for-the-badge&logo=docker&logoColor=white)](https://www.docker.com/)
[![Maven](https://img.shields.io/badge/Maven-C71A36?style=for-the-badge&logo=apachemaven&logoColor=white)](https://maven.apache.org/)

</div>

---

## Overview

SwiftPay is a production-grade, microservices-based payment processing platform built on an event-driven architecture. It handles end-to-end payment flows with **idempotency guarantees**, **real-time ledger management**, and **asynchronous analytics collection** — ensuring every transaction is processed exactly once, even under failure conditions.

The system is composed of three independently deployable services that communicate exclusively via Apache Kafka, with Redis acting as a dual-layer idempotency shield and PostgreSQL as the persistent source of truth.

---

## Table of Contents

- [Architecture](#architecture)
- [Services](#services)
- [Payment Flow](#payment-flow)
- [Tech Stack](#tech-stack)
- [Getting Started](#getting-started)
- [Configuration](#configuration)
- [API Reference](#api-reference)
- [Seed Data](#seed-data)
- [Project Structure](#project-structure)
- [Docker Build](#docker-build)
- [Health Checks](#health-checks)
- [Performance & Load Testing](#performance--load-testing)

---

## Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                           CLIENT                                │
└─────────────────────────────┬───────────────────────────────────┘
                              │  POST /v1/payments
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│               swiftpay-transaction-gateway  :8881               │
│                                                                 │
│   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌───────────┐    │
│   │  Redis   │   │  User    │   │ Balance  │   │   Kafka   │    │
│   │  Dedup   │─▶│ Validate │─▶ │  Check   │─▶│ Publisher │    │
│   └──────────┘   └──────────┘   └──────────┘   └───────────┘    │
└─────────────────────────────┬───────────────────────────────────┘
                              │  Kafka: payment.initiated
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                    swiftpay-ledger  :8882                       │
│                                                                 │
│   ┌──────────┐   ┌──────────┐   ┌──────────┐   ┌───────────┐    │
│   │  Redis   │   │ Debit /  │   │  Persist │   │   Kafka   │    │
│   │  Guard   │─▶│  Credit   │─▶│   DB     │─▶│ Publisher │    │
│   └──────────┘   └──────────┘   └──────────┘   └───────────┘    │
└─────────────────────────────┬───────────────────────────────────┘
                              │  Kafka: payment.completed
                              ▼
┌─────────────────────────────────────────────────────────────────┐
│                  swiftpay-analytics  :8883                      │
│                                                                 │
│              ┌──────────────────────────────┐                   │
│              │  Persist PaymentAnalytics    │                   │
│              └──────────────────────────────┘                   │
└─────────────────────────────────────────────────────────────────┘
```

### Kafka Topics

| Topic | Producer | Consumer |
|---|---|---|
| `payment.initiated` | `swiftpay-transaction-gateway` | `swiftpay-ledger` |
| `payment.completed` | `swiftpay-ledger` | `swiftpay-analytics` |

---

## Services

### 1. `swiftpay-transaction-gateway` — Port `8881`

The single entry point for all client payment requests. Responsible for validation and initiation only — it never touches account balances directly.

**Responsibilities:**
- Accepts `POST /v1/payments` requests
- Performs Redis duplicate-transaction check (24-hour TTL)
- Validates that sender ≠ receiver
- Verifies sender and receiver existence against the database
- Calls `GET /ledger/balance/{userId}` on the Ledger service to verify sufficient funds
- Saves a `PENDING` transaction record to PostgreSQL
- Publishes a `PaymentEventDTO` to the `payment.initiated` Kafka topic

---

### 2. `swiftpay-ledger` — Port `8882`

The core financial engine. Consumes payment events and performs the actual account mutation atomically.

**Responsibilities:**
- Consumes events from `payment.initiated`
- Guards against duplicate processing via Redis (`txn:<id>` key, 5-minute TTL)
- Atomically deducts from the sender and credits the receiver within a single `@Transactional` boundary
- Persists the transaction as `COMPLETED` to PostgreSQL
- Publishes a `PaymentCompletedEvent` to the `payment.completed` topic
- Exposes REST endpoints for balance and transaction history queries

**REST Endpoints:**

| Method | Path | Description |
|---|---|---|
| `GET` | `/ledger/balance/{userId}` | Returns the current balance for a user |
| `GET` | `/ledger/transactions/{userId}` | Returns the full transaction history for a user |

---

### 3. `swiftpay-analytics` — Port `8883`

A lightweight, stateless consumer responsible solely for recording completed payment data for downstream reporting.

**Responsibilities:**
- Consumes events from `payment.completed`
- Persists `PaymentAnalytics` records — including `transactionId`, `senderId`, `receiverId`, `amount`, `currency`, and `completedAt` — to PostgreSQL

---

## Payment Flow

Every payment traverses a deterministic, 10-step pipeline with idempotency checks at both the gateway and ledger layers:

```
Step 1   Client sends POST /v1/payments to the Transaction Gateway.

Step 2   Gateway checks Redis for txn:<id>.
         → Key exists   : Reject with "Duplicate transaction request".
         → Key absent   : Set key (24-hr TTL) and continue.

Step 3   Gateway validates sender ≠ receiver.
         → Same user    : Reject with "Both are same users!".

Step 4   Gateway verifies sender and receiver exist in the database.
         → Not found    : Reject with "Sender/Receiver not found".

Step 5   Gateway calls GET /ledger/balance/{senderId}.
         → Insufficient : Reject with "Insufficient balance".

Step 6   Gateway saves a PENDING transaction to PostgreSQL
         and publishes PaymentEventDTO → payment.initiated.

Step 7   Ledger consumes the event; sets its own Redis key (txn:<id>, 5-min TTL).
         → Key exists   : Skips processing (idempotency guard).

Step 8   Ledger atomically deducts from sender and credits receiver
         within a single @Transactional boundary.

Step 9   Ledger marks the transaction as COMPLETED in its database
         and publishes PaymentCompletedEvent → payment.completed.

Step 10  Analytics consumes the completed event and persists
         a PaymentAnalytics record.
```

---

## Tech Stack

| Technology | Version | Role |
|---|---|---|
| Java | 21 | Language runtime |
| Spring Boot | 3.4 / 3.5 | Application framework |
| Apache Kafka |7.6.1| Asynchronous event streaming |
| Redis |7.2.5| Idempotency & duplicate detection |
| PostgreSQL |16.3-alpine| Persistent relational data store |
| Spring Data JPA | — | ORM and database access layer |
| Lombok | — | Compile-time boilerplate reduction |
| Springdoc OpenAPI | 2.8.6 | Swagger UI for Gateway and Ledger |
| Docker |4.70.0| Containerisation via multi-stage builds |
| Maven |4.0.0| Build and dependency management |

---

## Getting Started

### Prerequisites

Ensure the following are installed before proceeding:

- Java 21
- Maven 3.8+
- Docker & Docker Compose

> All infrastructure dependencies (PostgreSQL, Kafka, Zookeeper, Redis) are managed by Docker Compose — no manual installation required.

---

### Option A — Docker Compose *(Recommended)*

This is the fastest way to run the entire platform locally. A single command brings up all infrastructure and all three Spring Boot services.

```bash
# 1. Clone the repository
git clone https://github.com/kushalkumar12/app-swift-pay.git
cd app-swift-pay

# 2. Build and start everything
docker compose -f docker-compose.yml up -d
```

Docker Compose will:
1. Pull and start **PostgreSQL**, **Apache Kafka**, **Zookeeper**, and **Redis**
2. Build multi-stage Docker images for each Spring Boot service
3. Start `swiftpay-transaction-gateway`, `swiftpay-ledger`, and `swiftpay-analytics` in their respective containers

---

### Option B — Local Development

To run services individually outside Docker, ensure PostgreSQL, Kafka, Zookeeper, and Redis are running locally, then start each service with:

```bash
cd swiftpay-transaction-gateway
mvn spring-boot:run

cd swiftpay-ledger
mvn spring-boot:run

cd swiftpay-analytics
mvn spring-boot:run
```

---

## Configuration

Each service has two configuration profiles:

| File | Active When |
|---|---|
| `application.properties` | Local development (default) |
| `application-docker.properties` | Docker (`--spring.profiles.active=docker`) |

**Shared defaults (local profile):**

| Property | Default Value |
|---|---|
| `spring.datasource.url` | `jdbc:postgresql://localhost:5432/swiftpay` |
| `spring.datasource.username` | `postgres` |
| `spring.datasource.password` | `admin` |
| `spring.kafka.bootstrap-servers` | `localhost:9092` |
| `spring.data.redis.host` | `localhost` |
| `spring.data.redis.port` | `6379` |

> **Security Notice:** Update all credentials, hostnames, and secrets before deploying to any non-local environment. Never commit production credentials to version control.

---

## API Reference

### `POST /v1/payments` — Initiate a Payment

**Base URL:** `http://localhost:8881`

**Request Body:**

```json
{
  "transactionId": "TXN-001",
  "senderId":      "U1001",
  "receiverId":    "U1002",
  "amount":        5000.00,
  "currency":      "INR"
}
```

**Response Matrix:**

| Scenario | HTTP Status | Message |
|---|---|---|
| Payment initiated | `200 OK` | `Payment initiated successfully` |
| Duplicate transaction ID | `409 Conflict` | `Duplicate transaction request` |
| Sender equals receiver | `400 Bad Request` | `Both are same users!...` |
| Sender not found | `404 Not Found` | `Sender not found` |
| Receiver not found | `404 Not Found` | `Receiver not found` |
| Insufficient balance | `402 Payment Required` | `Insufficient balance` |

---

### Swagger UI

Interactive API documentation is available at:

| Service | URL |
|---|---|
| Transaction Gateway | http://localhost:8881/swagger-ui.html |
| Ledger | http://localhost:8882/swagger-ui.html |

---

## Seed Data

Both `swiftpay-transaction-gateway` and `swiftpay-ledger` ship with a `data.sql` that seeds the database automatically on startup.

**Users — Gateway Database:**

| User ID | Name | Status |
|---|---|---|
| `U1001` | Kushal | ✅ Active |
| `U1002` | Amit | ✅ Active |
| `U1003` | Priya | ❌ Inactive |
| `U1004` | Rahul | ✅ Active |
| `U1005` | Vishnu | ✅ Active |

**Accounts — Ledger Database:**

| User ID | Balance (INR) |
|---|---|
| `U1001` | ₹ 1,00,000.00 |
| `U1002` | ₹ 50,000.00 |
| `U1003` | ₹ 0.00 |
| `U1004` | ₹ 7,50,00,000.50 |
| `U1005` | ₹ 0.50 |

---

## Project Structure

```
PyrogroupAssignment/
│
├── docker-compose.yml
├── payment-test.js
│
├── swiftpay-transaction-gateway/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/swiftpay/gateway/
│       ├── configuration/        # Kafka, Redis, OpenAPI, AppConfig
│       ├── controller/           # CreatePaymentAction
│       ├── customexception/      # GlobalExceptionHandler, custom exceptions
│       ├── dto/                  # PaymentReqDTO, PaymentEventDTO
│       ├── enums/                # ValidateUserEnum
│       ├── model/                # Transaction, User
│       ├── repo/                 # PaymentRepository, UserRepository
│       └── service/serviceImpl/  # CreatePaymentsService / Impl
│
├── swiftpay-ledger/
│   ├── Dockerfile
│   ├── pom.xml
│   └── src/main/java/com/swiftpay/ledger/
│       ├── config/               # OpenApiConfig
│       ├── consumer/             # PaymentConsumer (Kafka listener)
│       ├── controller/           # LedgerControllerAction
│       ├── dto/                  # PaymentEventDTO, PaymentCompletedEvent
│       ├── model/                # Account, Transaction
│       ├── repo/                 # AccountRepository, LedgerRepository
│       └── service/serviceimpl/  # LedgerService / Impl
│
└── swiftpay-analytics/
    ├── Dockerfile
    ├── pom.xml
    └── src/main/java/com/swiftpay/analytics/
        ├── consumer/             # AnalyticsConsumer (Kafka listener)
        ├── dto/                  # PaymentCompletedEvent
        ├── model/                # PaymentAnalytics
        ├── repo/                 # AnalyticsRepository
        └── service/serviceimpl/  # AnalyticsService / Impl
```

---

## Docker Build

Each service uses a two-stage Dockerfile to produce lean, secure runtime images:

| Stage | Base Image | Purpose |
|---|---|---|
| **builder** | `eclipse-temurin:21-jdk-alpine` | Resolves dependencies and packages the JAR via Maven |
| **runtime** | `eclipse-temurin:21-jre-alpine` | Runs the JAR as a non-root `swiftpay` user for minimal attack surface |

Running as a non-root user in the runtime stage follows container security best practices and is enforced across all three services.

---

## Health Checks

The Transaction Gateway and Ledger expose Spring Boot Actuator health endpoints for readiness and liveness probing:

```bash
# Transaction Gateway
curl http://localhost:8881/actuator/health

# Ledger
curl http://localhost:8882/actuator/health
```

Both return a standard `{"status":"UP"}` response when the service and its dependencies are healthy.

---

## Performance & Load Testing

SwiftPay was stress-tested using **[k6](https://k6.io/)** under a sustained constant-load scenario with **500 concurrent virtual users** over **66 minutes**, processing a total of **1,000,000 payment transactions**.

> The test script is included at the project root as `payment-test.js`.

### Test Configuration

| Parameter | Value |
|---|---|
| Tool | k6 |
| Virtual Users (VUs) | 500 (constant) |
| Total Duration | 1h 06m 02s |
| Target Throughput | 250 iterations/s |
| Total Iterations | 1,000,000 |

---

### Results Summary

#### ✅ Correctness — 100% Pass Rate

```
checks_total.......: 1,000,000   252.12/s
checks_succeeded...: 100.00%     1,000,000 out of 1,000,000
checks_failed......: 0.00%       0 out of 1,000,000

✓ status is 200/201
```

Not a single request failed or returned an unexpected response across one million transactions.

---

#### ⚡ Latency Breakdown

| Percentile | Response Time |
|---|---|
| Minimum | 45.12 ms |
| Median (p50) | 170.33 ms |
| Average | 182.45 ms |
| p(90) | 240.11 ms |
| p(95) | 310.22 ms |
| Maximum | 912.44 ms |

```
avg=182.45ms  min=45.12ms  med=170.33ms  max=912.44ms  p(90)=240.11ms  p(95)=310.22ms
```

> 90% of all requests completed under **240 ms** — well within acceptable latency thresholds for a synchronous payment initiation endpoint backed by Kafka, Redis, and PostgreSQL.

---

#### 📡 Throughput & Network

| Metric | Value |
|---|---|
| Achieved Throughput | **252.12 req/s** |
| Dropped Iterations | 0 |
| Data Received | 145 MB (36 kB/s) |
| Data Sent | 310 MB (78 kB/s) |

```
http_reqs......................: 1,000,000   252.12/s
http_req_failed................: 0.00%       0 out of 1,000,000
dropped_iterations.............: 0           0.00/s
```

---

#### Key Observations

- **Zero dropped iterations** across the full 66-minute run confirms the event pipeline (Kafka + Redis + PostgreSQL) held up without backpressure or consumer lag under sustained load.
- **0% failure rate** validates that the dual-layer Redis idempotency guard, balance checks, and Kafka publish all completed reliably at 252 req/s.
- The **p95 latency of 310 ms** and a max spike of **912 ms** indicate the system handles outlier conditions (GC pauses, Kafka broker flush, DB write contention) gracefully without cascading failures.
- The tight spread between **median (170 ms)** and **p90 (240 ms)** signals consistent, predictable performance rather than erratic tail behaviour.

---

<div align="center">
</div>
