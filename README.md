# Inscribe

![Java](https://img.shields.io/badge/Java-21-orange?logo=openjdk)
![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.4-6DB33F?logo=springboot&logoColor=white)
![Spring AI](https://img.shields.io/badge/Spring%20AI-1.0-6DB33F?logo=spring&logoColor=white)
![Angular](https://img.shields.io/badge/Angular-18-DD0031?logo=angular&logoColor=white)
![Redis](https://img.shields.io/badge/Redis-7-DC382D?logo=redis&logoColor=white)
![PostgreSQL](https://img.shields.io/badge/PostgreSQL-16-4169E1?logo=postgresql&logoColor=white)
![Kafka](https://img.shields.io/badge/Kafka-3.7-231F20?logo=apachekafka&logoColor=white)
![License](https://img.shields.io/badge/License-MIT-blue)

**An AI-powered, plugin-based workflow engine built with Java 21 and Spring Boot.**

Inscribe orchestrates multi-step workflows — validation, insertion, notification — driven by YAML configuration. Adding a new business domain (medical, e-commerce, logistics, …) requires **zero changes to the engine**. Just drop in a plugin.

The engine ships with two working plugins: **medical prescriptions** and **e-commerce carts**.

---

## What It Does

A user (or an AI prompt) submits an item. Inscribe:

1. **Enqueues** the request in a per-entity Redis queue
2. **Acquires a distributed lock** (Redisson) to guarantee ordering
3. **Runs each workflow step** defined in YAML — validation, insertion, post-processing
4. **Publishes events** via SSE (real-time) and Outbox pattern (at-least-once to Kafka/RabbitMQ)

Each step can `REJECT` (discard), `RETRY` (re-enqueue), or `SUCCESS` (continue). The entire step pipeline runs inside a single `@Transactional` boundary — if any write fails, everything rolls back.

---

## Architecture

```
                     REST / SSE       Angular :4200
                         │                │
          ┌──────────────▼────────────────▼──────────────┐
          │                  inscribe-api                 │
          │          SSE · Workflow endpoints             │
          └──────────────────┬───────────────────────────┘
                             │
          ┌──────────────────▼───────────────────────────┐
          │                inscribe-core                  │
          │  Orchestrator · CommandProcessor · LockManager│
          │  AI Resolver · Outbox · SPI                   │
          └──────┬──────────────────────┬────────────────┘
                 │                      │
       ┌─────────▼───┐          ┌───────▼────────┐
       │   medical   │          │   ecommerce    │
       │   plugin    │          │     plugin     │
       └─────────────┘          └────────────────┘
                 │                      │
          ┌──────▼──────────────────────▼──────┐
          │     Outbox → Kafka (KRaft)          │
          └────────────────────────────────────┘
```

**`inscribe-core`** defines the SPI contracts (`StepHandler`, `ItemCatalog`, `StepResult`) and orchestration logic. It has **no domain knowledge** and **no domain tables**.

**Plugins** own everything domain-specific: entities, repositories, controllers, validation logic, insertion logic, and YAML workflow definitions.

---

## Prerequisites

- **Java 21**
- **Docker** (for PostgreSQL, Redis and Kafka)
- **Node 18+ and Angular CLI 18** *(only for the frontend)*
- **An OpenAI API key** *(optional — only needed for AI-assisted item resolution)*

---

## Quick Start

```bash
# 1. Clone
git clone https://github.com/gtoniaccini/inscribe.git
cd inscribe

# 2. Start PostgreSQL, Redis (and optionally Kafka)
docker compose -f infrastructure/docker-compose.yml up -d

# 3. Set your OpenAI key (optional)
export OPENAI_API_KEY=sk-your-key-here

# 4. Build
./mvnw clean install

# 5. Run
./mvnw spring-boot:run -pl inscribe-api
```

### Running Without AI

To run Inscribe without OpenAI, activate the `noai` profile:

```bash
./mvnw spring-boot:run -pl inscribe-api -Dspring-boot.run.profiles=noai
```

All manual endpoints (create, add items, list, detail) work normally. Only the `/ai` endpoints will return `503 Service Unavailable`.

The API is available at `http://localhost:8080`.

### Running With Kafka

To enable Kafka event publishing, activate the `kafka` profile:

```bash
# Make sure Kafka is running
docker compose -f infrastructure/docker-compose.yml up -d kafka

# Start the app with the kafka profile
./mvnw spring-boot:run -pl inscribe-api -Dspring-boot.run.profiles=kafka
```

With this profile, the `OutboxRelayJob` will publish pending events to the configured Kafka topics (`medical.exam.inserted`, `medical.exam.rejected`, etc.) instead of the default no-op logger.

### Running the Frontend

```bash
cd frontend
ng serve
```

The Angular app is available at `http://localhost:4200`. All `/api/*` calls are automatically proxied to the Spring Boot backend at `localhost:8080` — no CORS configuration needed in development.

---

## API Examples

### Medical — Prescriptions

```bash
# Create a prescription
curl -s -X POST http://localhost:8080/api/prescriptions \
  -H "Content-Type: application/json" \
  -d '{
    "patientName": "Mario Rossi",
    "patientFiscalCode": "RSSMRA85M01H501Z",
    "doctorName": "Dr. Bianchi"
  }'

# Add an exam manually
curl -s -X POST http://localhost:8080/api/prescriptions/{id}/exams \
  -H "Content-Type: application/json" \
  -d '{"examCode": "EMO", "examName": "Emocromo"}'

# Add exams via AI (natural language)
curl -s -X POST http://localhost:8080/api/prescriptions/{id}/exams/ai \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Vorrei un emocromo e la glicemia"}'

# Get prescription details with exams
curl -s http://localhost:8080/api/prescriptions/{id}
```

### E-Commerce — Carts

```bash
# Create a cart
curl -s -X POST http://localhost:8080/api/carts \
  -H "Content-Type: application/json" \
  -d '{
    "customerName": "Luca Verdi",
    "customerEmail": "luca@example.com",
    "shippingAddress": "Via Roma 1, Milano",
    "currency": "EUR"
  }'

# Add a product
curl -s -X POST http://localhost:8080/api/carts/{id}/items \
  -H "Content-Type: application/json" \
  -d '{"sku": "LAPTOP-01", "productName": "MacBook Pro"}'

# Add products via AI
curl -s -X POST http://localhost:8080/api/carts/{id}/items/ai \
  -H "Content-Type: application/json" \
  -d '{"prompt": "I need a laptop and a mouse"}'
```

### Real-time Events (SSE)

```bash
# Stream workflow events for any entity
curl -N http://localhost:8080/api/stream/{id}
```

Events: `ITEM_REQUESTED`, `ITEM_VALIDATED`, `ITEM_REJECTED`, `ITEM_INSERTED`.

---

## How Plugins Work

A plugin is a Maven module that depends on `inscribe-core` and provides:

| Artifact | Purpose |
|----------|---------|
| `workflow/*.yml` | Defines steps, failure policies, AI prompt, and outbox topics |
| `StepHandler` implementations | Business logic — validation, insertion, notification |
| `ItemCatalog` implementation | Exposes the catalog of items for AI resolution |
| JPA entities + repositories | Plugin-owned tables — the core never touches them |
| REST controller | Domain-specific endpoints |

**Example — `medical.yml`:**

```yaml
workflow:
  name: "medical"
  container-label: "Ricetta"
  item-label: "Esame"
  ai-prompt: "Dato il catalogo degli esami medici, restituisci gli ID corrispondenti a: {userInput}"
  steps:
    - name: "validazione"
      handler: "MedicalValidationHandler"
      on-failure: REJECT
    - name: "inserimento"
      handler: "MedicalInsertionHandler"
      on-failure: RETRY
  on-complete:
    topic: "medical.exam.inserted"
  on-reject:
    topic: "medical.exam.rejected"
```

Steps execute sequentially. You can add as many as you need — pre-validation, approval, insertion, post-processing, notification — just by adding entries to the YAML and registering handlers as Spring beans.

---

## Project Structure

```
inscribe/
├── inscribe-core/           # Generic engine — SPI, orchestrator, AI, outbox
│   └── src/main/java/dev/inscribe/core/
│       ├── engine/          # Orchestrator, CommandProcessor, LockManager, StepHandlerRegistry
│       ├── spi/             # StepHandler, ItemCatalog, StepResult, WorkflowContext
│       ├── ai/              # AiResolver (natural language → catalog items)
│       ├── command/         # InsertItemCommand
│       ├── event/           # WorkflowEvent sealed interface + 4 event records
│       ├── outbox/          # OutboxEvent, OutboxWriter, OutboxRelayJob
│       ├── transport/       # ExternalEventPublisher (Kafka / RabbitMQ / Logging)
│       └── workflow/        # WorkflowDefinition, WorkflowDefinitionLoader
│
├── inscribe-medical/        # Medical plugin — prescriptions + exams
│   └── src/main/
│       ├── java/.../medical/
│       │   ├── controller/  # PrescriptionController (REST)
│       │   ├── handler/     # MedicalValidationHandler, MedicalInsertionHandler
│       │   ├── model/       # Prescription, PrescriptionExam, MedicalExam
│       │   └── repository/  # JPA repositories
│       └── resources/
│           ├── workflow/medical.yml
│           └── db/changelog/001-seed-medical-exams.yml
│
├── inscribe-ecommerce/      # E-commerce plugin — carts + products
│   └── src/main/
│       ├── java/.../ecommerce/
│       │   ├── controller/  # CartController (REST)
│       │   ├── handler/     # StockValidationHandler, EcommerceInsertionHandler
│       │   ├── model/       # Cart, OrderLineItem, Product
│       │   └── repository/  # JPA repositories
│       └── resources/
│           ├── workflow/ecommerce.yml
│           └── db/changelog/001-seed-products.yml
│
├── inscribe-api/            # Spring Boot application + SSE
│   └── src/main/
│       ├── java/.../api/
│       │   ├── controller/  # SseController, WorkflowController
│       │   └── sse/         # SseEventBroadcaster
│       └── resources/
│           ├── application.yml
│           ├── application-noai.yml
│           ├── application-kafka.yml
│           └── db/changelog/db.changelog-master.yaml
│
├── frontend/                # Angular 18 SPA
│   ├── proxy.conf.json      # /api → localhost:8080 (dev proxy)
│   └── src/app/
│
└── infrastructure/          # Docker Compose (PostgreSQL 16, Redis 7, Kafka 3.7)
```

---

## Tech Stack

| Component | Technology |
|-----------|-----------|
| Language | Java 21 (records, sealed interfaces, pattern matching) |
| Framework | Spring Boot 3.4 |
| AI | Spring AI 1.0 + OpenAI (gpt-4o-mini) |
| Frontend | Angular 18 (standalone components, SCSS) |
| Distributed locking | Redis + Redisson (watchdog auto-renewal) |
| Queue | Redis per-entity deques |
| Persistence | PostgreSQL 16 + Spring Data JPA |
| Migrations / Seed | Liquibase |
| Real-time | Server-Sent Events (SSE) |
| Async delivery | Outbox pattern → Kafka 3.7 KRaft (no Zookeeper) |
| Build | Maven multi-module |
| Infrastructure | Docker Compose |

---

## Key Design Decisions

- **Plugin-owned everything** — each plugin owns its entities, tables, controllers, and business logic. The core is a pure orchestration engine.
- **YAML-driven workflows** — steps, failure policies, and AI prompts are configured declaratively. No code changes to add or reorder steps.
- **Distributed lock per entity** — guarantees ordered processing even across multiple instances. Uses Redisson `RLock` with watchdog.
- **Outbox pattern** — guarantees at-least-once delivery of domain events to external brokers, decoupled from the main transaction via scheduled polling.
- **Kafka KRaft** — single-node Kafka without Zookeeper. Activated via the `kafka` Spring profile; falls back to a no-op logger when the profile is inactive.
- **Sealed interfaces** — `StepResult` (Success | Reject | Retry) and `WorkflowEvent` use Java 21 sealed types for exhaustive pattern matching.
- **AI as a first-class citizen** — natural language input is resolved to catalog item IDs via Spring AI and OpenAI, pluggable per workflow.
- **Modular monolith, microservices-ready** — all plugins run in a single JVM today. Each plugin can be extracted into an independent service with minimal changes: the outbox and SPI contracts are already designed for network-based communication.

---

## License

MIT
