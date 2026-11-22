#!/usr/bin/env bash

set -euo pipefail

BOOTSTRAP_SERVER="${KAFKA_BOOTSTRAP_SERVER:-kafka:29092}"
TOPICS_FILE="${TOPICS_FILE:-/config/topics.txt}"

echo "========================================"
echo "Kafka topic initialization"
echo "Bootstrap server: ${BOOTSTRAP_SERVER}"
echo "Topics file: ${TOPICS_FILE}"
echo "========================================"

if [[ ! -f "${TOPICS_FILE}" ]]; then
    echo "ERROR: Topics file not found: ${TOPICS_FILE}"
    exit 1
fi

while IFS=: read -r topic partitions || [[ -n "${topic:-}" ]]; do

    # Skip empty lines and comments
    [[ -z "${topic:-}" || "${topic}" =~ ^# ]] && continue

    # Remove whitespace
    topic="$(echo "${topic}" | xargs)"
    partitions="$(echo "${partitions}" | xargs)"

    if [[ -z "${topic}" || -z "${partitions}" ]]; then
        echo "ERROR: Invalid topic definition: ${topic}:${partitions}"
        exit 1
    fi

    echo ""
    echo "Creating topic: ${topic}"
    echo "Partitions: ${partitions}"

    /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --create \
        --if-not-exists \
        --topic "${topic}" \
        --partitions "${partitions}" \
        --replication-factor 1 \
        --config retention.ms=604800000

    DLT="${topic}.DLT"

    echo "Creating dead-letter topic: ${DLT}"

    /opt/kafka/bin/kafka-topics.sh \
        --bootstrap-server "${BOOTSTRAP_SERVER}" \
        --create \
        --if-not-exists \
        --topic "${DLT}" \
        --partitions 1 \
        --replication-factor 1 \
        --config retention.ms=604800000

done < "${TOPICS_FILE}"

echo ""
echo "========================================"
echo "Kafka topic initialization completed"
echo "========================================"