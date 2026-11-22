# 0001. Use microservices with a database per service on one PostgreSQL instance

- **Status:** Accepted
- **Date:** 2026-11-22
- **Phase / card:** P0-07 (implemented in P0-03, hardened in H-01)

## Context
Dropline is a portfolio project built by one person in a few weeks, but it should demonstrate how a real microservice system is structured: independent services, independent data, communication through events.

A core microservices rule is that a service owns its data: no other service reads or writes its tables. That normally means one database (or cluster) per service. Running six PostgreSQL servers locally and in a small GKE cluster would cost memory, money and operational time without showing anything extra.

Options considered:

- **A. One shared database, shared schema:** simplest, but services couple through tables; a reviewer would rightly call it a distributed monolith.
- **B. One PostgreSQL server per service:** strongest isolation, but ~5 extra pods/instances, more RAM, more backups, more cost on GCP.
- **C. One PostgreSQL instance, one database + one login role per service:** logical isolation enforced by PostgreSQL permissions, physical resources shared.

## Decision
We will run **one PostgreSQL instance** containing **one database per service** (`catalog`, `reservation`, `orders`, `payments`, `identity`). Each service connects with **its own role that can only connect to its own database**, and manages its own schema with Flyway. Services never query another service's database; they exchange data via REST or Kafka events, referencing each other only by IDs.

## Consequences
- ✅ Same ownership boundaries as option B, at the cost of a single Postgres container/pod.
- ✅ Each database can later move to its own instance (or Cloud SQL) by changing a connection string only.
- ✅ No cross-service foreign keys or joins, so the code is already shaped for real separation.
- ⚠️ Shared failure domain: if the instance goes down, every service loses its database. Acceptable for a demo; not for production.
- ⚠️ Noisy neighbours: a heavy query in one database can slow the others.
- ⚠️ Isolation depends on correct grants. PostgreSQL grants `CONNECT` to `PUBLIC` on new databases by default, so the init script must `REVOKE ALL ON DATABASE ... FROM PUBLIC`. Found in P0-03, tracked as Trello bug H-01 with an automated `verify-db-isolation.sh` check.
- ⚠️ Cross-service queries (e.g. "orders with event titles") need API composition or denormalised data in events.
- 🔁 Revisit when a service needs a different engine, independent scaling or a different backup policy; then split it onto its own instance.
