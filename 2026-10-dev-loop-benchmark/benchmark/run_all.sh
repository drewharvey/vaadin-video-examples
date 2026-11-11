#!/bin/bash
# Runs N rounds; each round runs all four setups once, in a rotating order.
# Usage: ./run_all.sh [N=20]
set -u
B="$(cd "$(dirname "$0")" && pwd)"
N=${1:-20}
if lsof -iTCP:8080 -sTCP:LISTEN >/dev/null 2>&1; then echo "Port 8080 is in use. Stop that process first."; exit 1; fi
mkdir -p "$B/results"
SETUPS=("devloop cold" "vanilla cold" "devloop prestarted" "vanilla prestarted")
for i in $(seq 1 "$N"); do
  for k in 0 1 2 3; do
    s=${SETUPS[$(( (k + i - 1) % 4 ))]}
    "$B/run_one.sh" $s "$i"
  done
done
echo "ALL DONE" | tee -a "$B/results/progress.log"
