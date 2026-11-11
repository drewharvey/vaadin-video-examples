#!/bin/bash
# One benchmark run.
# Usage: ./run_one.sh <devloop|vanilla> <cold|prestarted> <index>
set -u
B="$(cd "$(dirname "$0")" && pwd)"
arm=$1; mode=$2; idx=$3
name=$arm-$mode-$(printf %02d "$idx")
RUN="$B/runs/$name"; RES="$B/results/$name"
PROMPT="$(cat "$B/prompt.txt")"
[ "$mode" = prestarted ] && PROMPT="$PROMPT

The app is already running."
export VAADIN_LAUNCHBROWSER=false

log() { echo "[$(date '+%H:%M:%S')] $name: $*" | tee -a "$B/results/progress.log"; }
now() { perl -MTime::HiRes=time -e 'printf "%.3f", time'; }
port_up() { [ "$(curl -s -o /dev/null -w '%{http_code}' http://localhost:8080/)" = 200 ]; }
cleanup() {
  [ -x "$RUN/.vaadin/vaadin-dev" ] && (cd "$RUN" && .vaadin/vaadin-dev shutdown >/dev/null 2>&1)
  pkill -f "$RUN" 2>/dev/null; sleep 1; pkill -9 -f "$RUN" 2>/dev/null
  # Anything still on 8080 was started by this run (run_all.sh checks the port is free before starting).
  pids=$(lsof -tiTCP:8080 -sTCP:LISTEN 2>/dev/null); [ -n "$pids" ] && kill $pids 2>/dev/null; sleep 1
  pids=$(lsof -tiTCP:8080 -sTCP:LISTEN 2>/dev/null); [ -n "$pids" ] && kill -9 $pids 2>/dev/null; true
}

rm -rf "$RUN" "$RES"; mkdir -p "$RES" "$B/runs"
rsync -a "$B/templates/$arm/" "$RUN/"
echo "$PROMPT" > "$RES/prompt.txt"

if [ "$mode" = prestarted ]; then
  t0=$(now)
  if [ "$arm" = devloop ]; then (cd "$RUN" && .vaadin/vaadin-dev start) > "$RES/prestart.log" 2>&1
  else (cd "$RUN" && nohup ./mvnw > "$RES/prestart.log" 2>&1 &); fi
  for i in $(seq 300); do port_up && break; sleep 1; done
  if port_up; then log "app up after $(perl -e "printf '%.0f', $(now)-$t0")s"; else log "APP FAILED TO START"; touch "$RES/prestart.failed"; fi
fi

log "agent start"
t0=$(now)
# Isolation: only the project's own settings, skills and MCP servers load.
#   --setting-sources project,local : skips ~/.claude/settings.json, ~/.claude/CLAUDE.md, user skills, user plugins, user hooks
#   --strict-mcp-config --mcp-config .mcp.json : only the project's MCP servers (no user-level or claude.ai connectors)
(cd "$RUN" && exec perl -e 'alarm shift; exec @ARGV' 1800 \
  claude -p "$PROMPT" --model opus \
    --setting-sources project,local --strict-mcp-config --mcp-config .mcp.json \
    --dangerously-skip-permissions --verbose --output-format stream-json \
    < /dev/null 2> "$RES/stderr.log") \
  > >(perl -MTime::HiRes=time -ne 'BEGIN{$|=1} printf "%.3f\t%s", time, $_' > "$RES/stream.tsv") &
CPID=$!
# Headless claude waits for any background task the agent left running (e.g. spring-boot:run) before it exits.
# The agent is finished once it emits its result, so end the process 15s after that.
while kill -0 $CPID 2>/dev/null; do
  if grep -q '"type":"result"' "$RES/stream.tsv" 2>/dev/null; then
    sleep 15; kill -0 $CPID 2>/dev/null && { echo killed-after-result > "$RES/exit-lag.txt"; kill $CPID 2>/dev/null; }
    break
  fi
  sleep 2
done
wait $CPID 2>/dev/null
sleep 1
t1=$(now)
echo "{\"start\":$t0,\"end\":$t1,\"wall\":$(perl -e "printf '%.3f', $t1-$t0")}" > "$RES/wall.json"
log "agent done in $(perl -e "printf '%.0f', $t1-$t0")s"

port_up && echo running > "$RES/app-after.txt" || echo stopped > "$RES/app-after.txt"
[ -d "$RUN/target/devloop" ] && mkdir -p "$RES/devloop-logs" && cp "$RUN"/target/devloop/*.log "$RES/devloop-logs/" 2>/dev/null
(cd "$RUN" && git status --short > "$RES/git-status.txt"; git add -A >/dev/null 2>&1; git diff --cached HEAD -- src > "$RES/diff.patch")
cleanup

# Grading: does the project still pass its tests after the agent's change?
(cd "$RUN" && ./mvnw -q test > "$RES/test.log" 2>&1; echo $? > "$RES/test.exit")
cleanup
log "done (tests exit $(cat "$RES/test.exit"))"
