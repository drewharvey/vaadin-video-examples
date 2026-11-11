# Running the benchmark yourself

## Requirements

- macOS or Linux with `bash`, `perl`, `jq`, `rsync`, `curl`, `lsof`, `unzip` and `git`
- Java 21+ and Node 18+
- [Claude Code](https://claude.com/claude-code), signed in
- Port 8080 free

A full run of 80 sessions takes about 3.5 hours and cost about $45 with Opus. Pass a smaller number to
`run_all.sh` for a quicker check: `./run_all.sh 3` runs 12 sessions.

## Steps

```bash
cd benchmark
./setup.sh starter-app.zip   # builds two copies of the project, one with DevLoop installed
./run_all.sh 20              # 20 rounds x 4 setups, one session at a time
python3 analyze.py           # writes benchmark/results/runs.csv and summary.json, prints a summary
```

- `setup.sh` unzips the starter twice, installs DevLoop (`./mvnw vaadin:install-dev-cli`) in one copy,
  makes the Playwright MCP server headless in both, and commits a baseline.
- `run_all.sh` runs every setup once per round, rotating the order each round.
- Each run copies a fresh project to `benchmark/runs/` and saves its logs to `benchmark/results/`.

## Keeping your own Claude configuration out

Every session runs with:

```bash
claude -p "$PROMPT" --model opus \
  --setting-sources project,local \
  --strict-mcp-config --mcp-config .mcp.json \
  --dangerously-skip-permissions --verbose --output-format stream-json
```

- `--setting-sources project,local` skips your user settings, `~/.claude/CLAUDE.md`, skills, plugins and hooks.
  The project's own `.claude/` folder still loads (that's where the DevLoop skill lives).
- `--strict-mcp-config --mcp-config .mcp.json` loads only the project's MCP servers (Vaadin docs and Playwright).

Nothing in your home folder is changed. To check what a session loads on your machine:

```bash
cd benchmark/templates/devloop
claude -p "Say OK" --model haiku --setting-sources project,local \
  --strict-mcp-config --mcp-config .mcp.json --verbose --output-format stream-json \
  | jq -c 'select(.subtype=="init") | {mcp: [.mcp_servers[].name], skills}'
```

You should see only the `Vaadin` and `playwright` MCP servers, and `vaadin-devloop` should be the only non-built-in skill.

> `--dangerously-skip-permissions` lets the agent run any command without asking. The runs are unattended, so
> use a machine or account you're comfortable with.

## How time is measured

From launching `claude` to the agent's final answer. Headless `claude -p` doesn't exit while a background task
the agent started is still running (for example an app it relaunched), so the harness ends the process 15
seconds after the final answer and doesn't count that gap.
