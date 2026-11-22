# 0002. Use Kafka (KRaft mode) as the event backbone, with a JSON envelope now and Avro later

- **Status:** Accepted
- **Date:** 2026-11-22
- **Phase / card:** P0-07 (topics in P0-04)

## Context
The checkout flow spans reservation, order, payment and live services. Making them call each other synchronously would chain their availability and latency: if payment is slow, holding seats becomes slow. We also need to fan out seat changes to many live-service replicas and replay events when a consumer was down.

Requirements:
- Asynchronous, durable messages with ordering per entity (per event's seat map, per order).
- Consumers can be offline and catch up.
- Well known in the job market (portfolio goal) and runnable locally in Docker and on Kubernetes.

Options considered:
- **RabbitMQ:** great for work queues, but no log replay and weaker per-key ordering at scale.
- **GCP Pub/Sub:** managed and cheap, but ties local development to an emulator and to GCP; ordering keys exist but replay is limited.
- **Redis Streams:** already in the stack, but less tooling and weaker durability story.
- **Apache Kafka:** partitioned log, per-key ordering, replay, consumer groups; heavy only if run with ZooKeeper.

For the message format:
- **JSON:** human-readable, zero extra infrastructure, easy to debug in Kafka UI.
- **Avro / Protobuf + Schema Registry:** enforced contracts and compatibility checks, but one more component to run and learn.

## Decision
We will use **Apache Kafka in KRaft mode** (no ZooKeeper): the official `apache/kafka` image locally (single broker) and the **Strimzi** operator on GKE. Topics are declared as code (`infra/kafka/topics.txt`, later Strimzi `KafkaTopic`), auto-creation is disabled, and keys are chosen for ordering (`eventId` for seats, `orderId` for orders and payments).

Messages are **JSON in a versioned envelope**:

```json
{ "id": "uuid", "type": "SeatHeld", "version": 1, "occurredAt": "...", "payload": { } }
```

Event records live in a shared `shared-events` Maven module. Moving to **Avro + a Schema Registry** (Apicurio or Confluent) is a planned stretch goal.

## Consequences
- ✅ Services are decoupled in time: payment can be slow or down without blocking holds.
- ✅ Per-key ordering and replay make the saga and the live seat map correct after restarts.
- ✅ KRaft removes ZooKeeper: one container locally, fewer pods on GKE.
- ✅ JSON is easy to inspect in Kafka UI and in tests.
- ⚠️ Eventual consistency: the UI must show "PENDING" states, and the saga must handle duplicates (idempotent consumers) and failures (dead-letter topics).
- ⚠️ Kafka is the heaviest component on a small cluster (RAM). Mitigation: single broker, small heap, tear the cloud cluster down between demos.
- ⚠️ JSON has no enforced schema: a breaking change is only caught by tests. Mitigation: the `version` field, contract tests on the `shared-events` module, additive changes only.
- 🔁 Revisit the format when more than one team/consumer evolves independently, or when a breaking change slips through: introduce Avro + Schema Registry with backward-compatibility checks.
