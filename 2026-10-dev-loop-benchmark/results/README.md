# Data

| File | What it is |
|---|---|
| [runs.csv](runs.csv) | One row per session (80 rows) with every metric below |
| [summary.json](summary.json) | Median, mean, quartiles, min and max of each metric per setup |

Sessions are named `<setup>-<run number>`, for example `devloop-prestarted-07`.

The metrics were extracted from each session's verbose Claude Code log by
[`benchmark/analyze.py`](../benchmark/analyze.py). The logs themselves aren't included here. Running the benchmark
yourself produces them (see [RUNNING.md](../RUNNING.md)). Counts of commands come from matching the shell commands in
each log; the exact patterns are at the top of the script.

## Columns

| Column | Meaning |
|---|---|
| `wall_s` | Seconds from launching `claude` to the agent's final answer |
| `exit_lag_s` | Seconds the process stayed open after that (not counted) |
| `cost_usd`, `turns`, `api_s`, `output_tokens` | From the session's final result: cost, turns, seconds waiting on the model, tokens written |
| `tool_calls`, `tool_errors` | All tool calls, and those that returned an error |
| `bash`, `bash_background` | Shell commands, and those run in the background |
| `browser`, `page_loads`, `screenshots` | Playwright calls, page loads and reloads, screenshots |
| `docs` | Vaadin docs MCP calls |
| `app_launches`, `app_kills` | Shell commands that start or kill the app |
| `cp_to_target` | Files copied into `target/classes` by hand |
| `status_probes` | `lsof`, `ps`, `pgrep` or `curl localhost` checks |
| `poll_loops` | `for`/`until`/`while` loops that wait for something |
| `mvn_test`, `mvn_compile` | Maven test and compile commands |
| `vd_status`, `vd_start`, `vd_apply`, `vd_restart`, `vd_stop`, `vd_other` | `vaadin-dev` commands |
| `devloop_skill` | Times the `vaadin-devloop` skill was loaded |
| `tool_s_app`, `tool_s_tests`, `tool_s_browser`, `tool_s_docs`, `tool_s_other` | Seconds spent waiting on each kind of tool call (`app` includes sleeps and wait loops) |
| `time_to_first_edit_s`, `time_to_first_browser_s` | When the agent first edited source code, and first used the browser |
| `files_changed`, `test_files_changed`, `lines_added` | Size of the change |
| `tests_pass` | The project's tests passed after the session |
| `app_running_after` | The app was still running when the agent finished |
| `prestart_failed` | The app failed to start before a pre-started session (never happened) |
| `sleep_blocked`, `file_edits`, `monitor` | Sleeps Claude Code blocked, Edit/Write calls, Monitor calls |

`vanilla-cold-02` reports 1 turn and 81 output tokens in its final result, a logging quirk of that session. Its time, cost
and tool calls are correct.
