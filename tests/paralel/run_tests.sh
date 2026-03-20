#!/usr/bin/env bash
set -euo pipefail

################################################
MVN_CLIENT_MODULE=client
MVN_ROOT_DIR="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")/../.." && pwd)"
MVN_ROOT_POM="$MVN_ROOT_DIR/pom.xml"

ROOT_DIR="$MVN_ROOT_DIR"

INPUT_CONCURRENT="./client_input/transferAndBack.txt"
INPUT_CHECK="./client_input/checkBalance.txt"
EXPECTED_OUTPUT="./expected_output/balanceZero.txt"

TMP_DIR="./tmp-test"
N_CLIENTS=9
################################################

GREEN='\033[0;32m'
RED='\033[0;31m'
NC='\033[0m'



COMPILE=true
for arg in "$@"; do
    if [[ "$arg" == "--no-compile" ]]; then
        COMPILE=false
    fi
done

mkdir -p "$TMP_DIR"

exec_client() {
    mvn --quiet -f "$MVN_ROOT_POM" -pl "$MVN_CLIENT_MODULE" exec:java < "$1" > "$2"
}


###################### Starting node and sequencer ########################

# removed because couldnt make node and sequencer work and shutdown properly

# compile

# start sequencer

# start node




##############################################################



echo "Running $N_CLIENTS concurrent clients..."

for i in $(seq 1 $N_CLIENTS); do
    exec_client "$INPUT_CONCURRENT" "$TMP_DIR/out_$i.txt" &
done

wait

echo "All concurrent clients finished."

echo "Running verification client..."

exec_client "$INPUT_CHECK" "$TMP_DIR/check_output.txt"

if diff -u "$TMP_DIR/check_output.txt" "$EXPECTED_OUTPUT"; then
    printf "${GREEN}TEST PASSED${NC}\n"
else
    printf "${RED}TEST FAILED${NC}\n"
fi