#!/usr/bin/env bash
set -euo pipefail

################################################
MVN_ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
MVN_ROOT_POM="$MVN_ROOT_DIR/pom.xml"
################################################

PIDS=()

cleanup() {
    echo -e "\nShutting down..."
    for pid in "${PIDS[@]:-}"; do
        kill "$pid" 2>/dev/null || true
    done
    wait
    echo "All processes stopped."
}
trap cleanup INT

wait_for_port() {
    local port=$1
    while ! nc -z localhost "$port" 2>/dev/null; do
        sleep 2
    done
}

###############################################
echo "Starting sequencer..."
mvn --quiet -f "$MVN_ROOT_POM" -pl sequencer exec:java &
PIDS+=($!)

wait_for_port 3001
echo "Sequencer ready"

###############################################
echo "Starting node1..."
mvn --quiet -f "$MVN_ROOT_POM" -pl node -Ddebug=1 exec:java -Dexec.args="2001 OrgA localhost:3001" &
PIDS+=($!)

wait_for_port 2001
echo "Node1 ready"

###############################################
echo "Starting node2..."
mvn --quiet -f "$MVN_ROOT_POM" -pl node -Ddebug=1 exec:java -Dexec.args="2002 OrgB localhost:3001" &
PIDS+=($!)

wait_for_port 2002
echo "Node2 ready"

###############################################
echo "Starting node3..."
mvn --quiet -f "$MVN_ROOT_POM" -pl node -Ddebug=1 exec:java -Dexec.args="2003 OrgC localhost:3001" &
PIDS+=($!)

wait_for_port 2003
echo "Node3 ready"

###############################################
echo "All services running. Press Ctrl+C to stop."

# Keep script alive
while true; do
    sleep 1
done