# Kafka

Kafka is used for asynchronous communication between microservices.

## Topics

Topic definitions are maintained in:

`infra/kafka/topics.txt`

Topics are automatically created by the `kafka-init` service
when the infrastructure profile is started.

| Topic | Partitions | Message key |
|---|---:|---|
| seat-events | 6 | eventId |
| order-events | 3 | orderId |
| payment-commands | 3 | orderId |
| payment-events | 3 | orderId |
| catalog-events | 1 | eventId |

Each application topic has a corresponding dead-letter topic
with the `.DLT` suffix and one partition.

All topics use a retention period of seven days.

Local Kafka uses replication factor 1.

## Message keys

### seat-events

Messages use `eventId` as their key.

This ensures events associated with the same seat event
are consistently routed to the same partition, preserving
ordering for that event.

### order-events

Messages use `orderId` as their key.

This ensures events belonging to the same order are routed
to the same partition, preserving ordering throughout
the order lifecycle.

### payment-commands

Messages use `orderId` as their key.

This ensures payment commands associated with the same
order are processed in order.

### payment-events

Messages use `orderId` as their key.

This ensures payment events associated with the same order
are ordered consistently.

### catalog-events

Messages use `eventId` as their key.

The topic currently uses one partition because catalog
event throughput is expected to be relatively low.

## Topic initialization

The `kafka-init` Docker Compose service executes:

`infra/kafka/create-topics.sh`

The script reads `infra/kafka/topics.txt` and creates
application topics and their dead-letter topics.

Topic creation is idempotent.

In Phase 6, these topic definitions will be migrated
to Strimzi `KafkaTopic` resources.