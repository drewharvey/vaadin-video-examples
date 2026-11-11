# Benchmark harness

| File | What it does |
|---|---|
| `prompt.txt` | The prompt every session gets |
| `starter-app.zip` | The Vaadin 25.3.2 starter project with the Task List view |
| `setup.sh` | Builds the two project templates (with and without DevLoop) |
| `run_one.sh` | Runs one session: `./run_one.sh <devloop\|vanilla> <cold\|prestarted> <number>` |
| `run_all.sh` | Runs N rounds of all four setups: `./run_all.sh 20` |
| `analyze.py` | Turns the logs into `runs.csv` and `summary.json` |

See [RUNNING.md](../RUNNING.md) for how to use them.
