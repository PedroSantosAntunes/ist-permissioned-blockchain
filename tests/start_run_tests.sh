#!/usr/bin/env bash
set -euo pipefail

################################################
MVN_ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/.." && pwd)"
MVN_ROOT_POM="$MVN_ROOT_DIR/pom.xml"

TESTS_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
INPUTS="${TESTS_DIR}/inputs"
EXPECTED_OUTPUTS="${TESTS_DIR}/outputs"
TEST_OUTPUTS="${TESTS_DIR}/test-outputs"
################################################

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'

PIDS=()

cleanup() {
    echo "Cleaning up processes..."
    for pid in "${PIDS[@]:-}"; do
        kill "$pid" 2>/dev/null || true
    done
}
trap cleanup EXIT

###############################################
# Start services

echo "Starting sequencer..."
mvn --quiet -f "$MVN_ROOT_POM" -pl sequencer exec:java &
PIDS+=($!)
sleep 3

echo "Starting node1..."
mvn --quiet -f "$MVN_ROOT_POM" -pl node exec:java -Dexec.args="2001 OrgA localhost:3001" &
PIDS+=($!)
sleep 3

echo "Starting node2..."
mvn --quiet -f "$MVN_ROOT_POM" -pl node exec:java -Dexec.args="2002 OrgB localhost:3001" &
PIDS+=($!)
sleep 3

echo "Starting node3..."
mvn --quiet -f "$MVN_ROOT_POM" -pl node exec:java -Dexec.args="2003 OrgC localhost:3001" &
PIDS+=($!)
sleep 3

###############################################
# Run tests

exec_client() {
    mvn --quiet -f "$MVN_ROOT_POM" -pl client exec:java \
        -Dexec.args="localhost:2001:OrgA localhost:2002:OrgB localhost:2003:OrgC" \
        < "$1" > "$2"
}

echo "Running tests..."

rm -rf "$TEST_OUTPUTS"
mkdir -p "$TEST_OUTPUTS"

i=1
while true; do
    TEST=$(printf "%02d" $i)
    if [ -e "${INPUTS}/input${TEST}.txt" ]; then 
        exec_client "${INPUTS}/input${TEST}.txt" "${TEST_OUTPUTS}/out${TEST}.txt"

        if diff -u "${TEST_OUTPUTS}/out${TEST}.txt" "${EXPECTED_OUTPUTS}/out${TEST}.txt"; then
            printf "${GREEN}[%s] TEST PASSED${NC}\n" "${TEST}"
        else
            printf "${RED}[%s] TEST FAILED${NC}\n" "${TEST}"
        fi

        i=$((i+1))
    else
        break
    fi
done

echo "Check outputs in ${TEST_OUTPUTS}"