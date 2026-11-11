# Claude Code with and without Vaadin DevLoop

A benchmark of how Claude Code works on a running Vaadin app, with and without
Vaadin DevLoop. 80 headless sessions gave the same small UI task to the same
Vaadin 25.3 project, 20 times in each of four setups.

> **This is a simple experiment, made for demonstration.** It measures one small styling task on one
> small starter app. It isn't a robust benchmark and doesn't cover the broad range of real development
> work, so treat the numbers as an illustration of how the agent behaves, not as general performance figures.

## Results at a glance

Medians of 20 runs. Δ compares DevLoop with vanilla in the same starting state.

| | DevLoop | Vanilla | Δ |
|---|---|---|---|
| **App already running:** time | 64s | 116s | **−45%** |
| **App already running:** cost | $0.46 | $0.58 | **−21%** |
| **Cold start:** time | 89s | 113s | **−21%** |
| **Cold start:** cost | $0.54 | $0.60 | **−10%** |

- With the app already running, vanilla Claude killed and restarted it in **19 of 20** runs. With DevLoop: **0 of 40** runs.
- DevLoop runs never checked ports or processes. Vanilla runs did so a median of 5 times per run when the app was already running.
- DevLoop's `apply` command told the agent whether a change was live. Vanilla agents restarted the app and reloaded the page to find out.
- All 80 runs built the same feature and passed the project's tests.

## What's here

| | |
|---|---|
| [RESULTS.md](RESULTS.md) | The full results: setup, numbers, and how the agents behaved |
| [RUNNING.md](RUNNING.md) | How to run the benchmark yourself |
| [results/](results/) | The data: every metric for each of the 80 sessions, and summaries per setup |
| [benchmark/](benchmark/) | The prompt, the scripts, and the starter project |

## The task

Every session got this prompt:

```
Give the row items more breathing room by using vertical padding.  Show rows that are past due in red.  You must verify the changes are correct and working.
```

When the app was already running, the prompt also said `The app is already running.`

Run on Nov 3, 2026 with Claude Code 2.1.317 and Claude Opus. Total cost $43.56.
