# Inscribe

**AI-Powered Workflow Engine** — generic, plugin-based, event-driven.

Inscribe is a workflow engine where adding a new domain requires only **3 artifacts**: a YAML file, a StepHandler, and an ItemCatalog. Zero changes to the core.

## Architecture

```
┌─────────────────────────────────────────────────────┐
│                   Angular Frontend                    │
│          Dashboard · AI Chat · Status Tracker         │
└──────────────────────┬──────────────────────────────┘
                       │ REST / SSE
┌──────────────────────▼──────────────────────────────┐
│                   inscribe-api                        │
│         REST Controllers · SSE Broadcaster            │
└──────────────────────┬──────────────────────────────┘
                       │
┌──────────────────────▼──────────────────────────────┐
│                  inscribe-core                        │
│   Orchestrator · LockManager · AI Resolver            │
│   StepHandlerRegistry · Outbox · WorkflowLoader       │
└──────┬───────────────────────────────────┬──────────┘
       │                                   │
┌──────▼──────┐                   ┌────────▼────────┐
│  medical    │                   │   ecommerce     │
│   plugin    │                   │     plugin      │
└─────────────┘                   └─────────────────┘
```

## Quick Start

```bash
# 1. Start infrastructure
cd infrastructure && docker-compose up -d

# 2. Build the project
./mvnw clean install

# 3. Set your OpenAI key
export OPENAI_API_KEY=sk-your-key-here

# 4. Run the application
./mvnw spring-boot:run -pl inscribe-api
```

## API Examples

```bash
# Create a medical prescription
curl -X POST http://localhost:8080/api/containers \
  -H "Content-Type: application/json" \
  -d '{"workflowName": "medical", "label": "Ricetta Mario Rossi"}'

# Add exams via AI
curl -X POST http://localhost:8080/api/containers/{id}/items/ai \
  -H "Content-Type: application/json" \
  -d '{"prompt": "Vorrei un emocromo e la glicemia"}'

# Stream real-time updates
curl -N http://localhost:8080/api/containers/{id}/stream
```

## Adding a New Plugin

Only 3 files needed — zero changes to the core:

1. **`workflow/your-domain.yml`** — Defines steps, handlers, topics, AI prompt
2. **`YourValidationHandler.java`** — Implements `StepHandler` interface
3. **`YourItemCatalog.java`** — Implements `ItemCatalog` interface

See [`inscribe-medical`](inscribe-medical/) for a complete example.

## Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Java 21 + Spring Boot 3.x |
| Distributed Lock | Redis + Redisson |
| Events (internal) | Spring ApplicationEventPublisher |
| Events (external) | Kafka / RabbitMQ (Strategy pattern) |
| Persistence | PostgreSQL + Spring Data JPA |
| AI | Spring AI + OpenAI (function calling) |
| Real-time | Server-Sent Events (SSE) |
| Containers | Docker + Docker Compose |

## Project Structure

```
inscribe/
├── inscribe-core/        ← Generic engine (never modified for new domains)
├── inscribe-medical/     ← Medical plugin (prescriptions + exams)
├── inscribe-ecommerce/   ← E-commerce plugin (orders + products)
├── inscribe-api/         ← REST API + SSE + Spring Boot app
├── infrastructure/       ← Docker Compose, K8s manifests
└── docs/                 ← C4 diagrams, ADRs
```

## License

MIT
