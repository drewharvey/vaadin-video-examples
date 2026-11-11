# Results

80 headless Claude Code sessions, Nov 3, 2026. Claude Code 2.1.317, Claude Opus. The data is in [results/](results/).

## Setup

**Project:** the start.vaadin.com starter for Vaadin 25.3.2 with the Task List example view (a grid of tasks with due dates).
Every session got a fresh copy.

**DevLoop** is installed with `./mvnw vaadin:install-dev-cli`. It adds `.vaadin/vaadin-dev`, a command-line tool over a
background process that runs the app, and a `vaadin-devloop` skill that tells the agent how to use it. Its main commands:
`status`, `start` and `apply`. `apply` hot-swaps Java changes and pushes CSS changes into the running app, then reports
whether that worked. Nothing else differs between the two projects.

**Four setups, 20 runs each:**

| Setup | DevLoop | App when the session starts |
|---|---|---|
| DevLoop cold | installed | not running |
| Vanilla cold | not installed | not running |
| DevLoop pre-started | installed | running (`vaadin-dev start`) |
| Vanilla pre-started | not installed | running (`./mvnw`, which runs `spring-boot:run`) |

**Prompt:**

```
Give the row items more breathing room by using vertical padding.  Show rows that are past due in red.  You must verify the changes are correct and working.
```

The pre-started setups add a blank line and `The app is already running.`

**Isolation:** no user-level Claude configuration (CLAUDE.md, settings, skills, plugins, hooks or MCP servers). Each session
loaded only the project's own `.claude/` folder and its two MCP servers: Vaadin docs and Playwright (headless).

**Order:** 20 rounds. Each round ran all four setups one after another, in an order that rotated every round.

**Time** is measured from launching `claude` to the agent's final answer.

## Numbers

Medians of 20 runs. Δ = DevLoop compared with vanilla in the same starting state (negative means DevLoop used less).

| | DevLoop cold | Vanilla cold | Δ | DevLoop pre-started | Vanilla pre-started | Δ |
|---|---|---|---|---|---|---|
| **Time** | **89s** | 113s | **−21%** | **64s** | 116s | **−45%** |
| **Cost** | **$0.54** | $0.60 | **−10%** | **$0.46** | $0.58 | **−21%** |
| Time waiting on the model | 56s | 67s | −17% | 44s | 69s | −37% |
| Output tokens | 5.5k | 7.3k | −24% | 4.6k | 7.3k | −36% |
| Turns | 21 | 21 | 0% | 18.5 | 24.5 | −24% |
| Tool calls | 19 | 20 | −5% | 16.5 | 23.5 | −30% |
| Shell commands | 9 | 11 | −18% | 6 | 12.5 | −52% |
| Browser calls | 7 | 6.5 | +8% | 7 | 8 | −13% |
| Page loads and reloads | 1 | 2 | −50% | 1 | 3 | −67% |
| Port and process checks (`lsof`, `ps`, `curl`) | 0 | 2 | −100% | 0 | 5 | −100% |
| Time on app commands and waiting for the app | 7s | 23s | −68% | 1.5s | 13s | −89% |
| Time to first browser call | 30s | 53s | −43% | 12s | 22s | −47% |

**Spread of times** (middle half of runs, then fastest and slowest):

| | Middle half | Fastest | Slowest |
|---|---|---|---|
| DevLoop cold | 71–110s | 48s | 284s |
| Vanilla cold | 104–135s | 77s | 220s |
| DevLoop pre-started | 57–79s | 50s | 172s |
| Vanilla pre-started | 96–140s | 83s | 190s |

**Round by round:**

| | Cold | Pre-started |
|---|---|---|
| DevLoop faster | 16 of 20 | 19 of 20 |
| DevLoop cheaper | 15 of 20 | 20 of 20 |

**What the runs did** (number of runs out of 20):

| | DevLoop cold | Vanilla cold | DevLoop pre-started | Vanilla pre-started |
|---|---|---|---|---|
| Killed and relaunched the app | 0 | n/a | 0 | 19 |
| Left the app running at the end | 20 | 0 | 20 | 16 |
| Added a unit test | 4 | 19 | 0 | 1 |
| Copied CSS into `target/classes` by hand | 0 | 0 | 0 | 2 |
| Checked the result in the browser | 20 | 20 | 20 | 20 |
| Passed the tests afterwards | 20 | 20 | 20 | 20 |

All 80 runs built the same feature: a part-name generator that tags past-due rows, plus CSS for the padding and the red rows.

## How the agents worked

### Checking whether the app is running

**Vanilla** agents pieced it together from shell commands. Cold runs checked the port with `lsof`, then polled the app's log
until it started:

```bash
for i in $(seq 1 120); do
  grep -qE "Started Application|APPLICATION FAILED|ERROR" /tmp/app.log && break
  sleep 2
done
```

Pre-started runs found the process with `ps`, read its command line and inspected its JVM flags to work out whether it could
reload code. It couldn't:

> "The running JVM has no hot-reload agent, so the Java change only takes effect after a restart."

**DevLoop** agents ran `.vaadin/vaadin-dev status` first, every time. None of the 40 runs used `lsof`, `ps` or `curl`.

```
running  pid=99005  owner=daemon  registered=true  runtime=main
frontend no-dev-server(DevBundleBuildingHandler)
daemon pid=98872 port=63237 up=18s
```

### Starting, restarting and stopping

**Vanilla cold:** edit the code, run the tests, launch `./mvnw spring-boot:run` in the background, wait on its log, check in
the browser, then stop the app with `pkill`. All 20 runs stopped the app before finishing.

**Vanilla pre-started:** 19 of 20 runs killed the running app and relaunched it, usually after `./mvnw compile`. A typical
restart:

```bash
ps -o command= -p 99987 > /tmp/appcmd.txt
kill 99987
while lsof -iTCP:8080 -sTCP:LISTEN >/dev/null; do sleep 1; done
nohup sh -c "$(cat /tmp/appcmd.txt) --vaadin.launch-browser=false" > /tmp/app.log 2>&1 &
for i in $(seq 90); do grep -q "Started Application" /tmp/app.log && break; sleep 2; done
```

The one run that didn't restart started a second copy of the app on port 8081 instead.

**DevLoop:** cold runs ran `vaadin-dev start` and edited code while the app booted. No DevLoop run restarted or stopped the
app. It kept running in the background, ready for the next task.

### Confirming a change is live

**Vanilla:** nothing told the agent whether a change had reached the app. Agents relaunched after editing, reloaded the page
and measured the styles. Two runs found a CSS change missing and copied the file into the build output by hand:

> "The CSS is still the old version because `spring-boot:run` serves resources from `target/classes`. I'll sync the file and reload."

**DevLoop:** every edit was followed by `.vaadin/vaadin-dev apply`, which says exactly what happened:

```
change-set: 2 file(s): .../TaskListView.java, .../styles.css
resources: copied 1 to the classpath
compiling → runtime → Stable   (1.13s)
hmr: 1 resource(s) copied, pushed 2 stylesheet(s) in place
hot-reload: redefineClasses(1); onHotswap completed=true
exit=0
```

Agents took `exit=0` as proof the change was live and used the browser only to check that it looked right.

## Other findings

- **Vanilla cold runs wrote unit tests.** 19 of 20 added a test for the past-due logic, and ran the tests before ever
  starting the app. DevLoop cold runs did so in 4 of 20.
- **The time gap is mostly the model, not the tools.** Vanilla pre-started runs spent 25s more waiting on the model and wrote
  57% more output, working out how the app was started and how to restart it safely.
- **DevLoop was more consistent with a running app.** 18 of its 20 pre-started runs beat the fastest vanilla pre-started run.
- **The slowest DevLoop runs weren't caused by DevLoop.** The 284s run waited 200s on a browser call that hung. The 181s and
  172s runs each had one model response that took over 80s.
- **Relaunched apps keep headless sessions alive.** In one vanilla run, `claude -p` stayed open 14 minutes after the final
  answer because the agent had relaunched the app as a background task. That time isn't counted.

## Limitations

- One small task on one small app. This starter restarts in seconds. A larger app takes longer to restart, which would likely
  widen the gap.
- The task only touched a view and a stylesheet, which DevLoop can hot-swap. Some changes (new entities, configuration)
  need a restart with DevLoop too.
- One model (Claude Opus) and one Claude Code version.
- "Passed the tests" checks that nothing broke. It doesn't grade how good the result looks.
