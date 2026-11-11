#!/usr/bin/env python3
"""Parse benchmark results into per-run metrics and per-setup summaries.

Usage: python3 analyze.py [results_dir]
Writes results/runs.csv, results/summary.json and prints a markdown summary.
"""
import csv, json, os, re, statistics, sys
from collections import Counter

RES = sys.argv[1] if len(sys.argv) > 1 else os.path.join(os.path.dirname(os.path.abspath(__file__)), "results")
SETUPS = ["devloop-cold", "vanilla-cold", "devloop-prestarted", "vanilla-prestarted"]

RE_LAUNCH = re.compile(r"spring-boot:run|(^|[;&|(\s])\./mvnw(\s+-q)?\s*(>|&|$)|java .*com\.example\.Application|nohup sh -c")
RE_KILL = re.compile(r"\b(kill|pkill)\b")
RE_CP_TARGET = re.compile(r"\b(cp|rsync)\b[^\n]*target/classes")
RE_POLL = re.compile(r"for i in \$\(seq|until |while .*sleep|while .*lsof")
RE_TEST = re.compile(r"mvnw[^\n|;&]*\b(test|verify)\b")
RE_COMPILE = re.compile(r"mvnw[^\n|;&]*\bcompile\b")


def load(run_dir):
    events = []
    with open(os.path.join(run_dir, "stream.tsv")) as f:
        for line in f:
            ts, _, js = line.partition("\t")
            try:
                events.append((float(ts), json.loads(js)))
            except ValueError:
                pass
    return events


def text_of(content):
    if isinstance(content, list):
        return "".join(c.get("text", "") for c in content if isinstance(c, dict))
    return content or ""


def analyze(run_dir):
    name = os.path.basename(run_dir)
    setup = name.rsplit("-", 1)[0]
    events = load(run_dir)
    wall = json.load(open(os.path.join(run_dir, "wall.json")))
    t0 = wall["start"]
    uses, results = {}, {}
    for ts, e in events:
        if e.get("type") == "assistant":
            for c in e["message"].get("content", []):
                if c.get("type") == "tool_use":
                    uses[c["id"]] = (ts, c)
        elif e.get("type") == "user":
            content = e.get("message", {}).get("content")
            if isinstance(content, list):
                for c in content:
                    if isinstance(c, dict) and c.get("type") == "tool_result":
                        results[c["tool_use_id"]] = (ts, c)
    final, final_ts = next(((e, ts) for ts, e in reversed(events) if e.get("type") == "result"), ({}, None))
    # Agent time ends at its result event. Headless claude can linger afterwards waiting for a
    # background task the agent left running; that lag is reported separately, not counted.
    agent_wall = (final_ts - t0) if final_ts else wall["wall"]

    m = Counter()
    tool_time = Counter()
    first_edit = first_browser = None
    vd = Counter()
    for uid, (ts, c) in sorted(uses.items(), key=lambda kv: kv[1][0]):
        tool = c["name"]
        inp = c.get("input", {})
        res_ts, res = results.get(uid, (ts, {}))
        dur = max(0.0, res_ts - ts)
        out = text_of(res.get("content"))
        m["tool_calls"] += 1
        if res.get("is_error"):
            m["tool_errors"] += 1
        cat = "other"
        if tool == "Bash":
            cmd = inp.get("command", "")
            m["bash"] += 1
            if inp.get("run_in_background"):
                m["bash_background"] += 1
            subs = re.findall(r"vaadin-dev\s+(\w+)", cmd)
            for sub in subs:
                vd[sub] += 1
            if subs:
                cat = "app"
            if RE_TEST.search(cmd):
                m["mvn_test"] += 1; cat = "tests"
            if RE_COMPILE.search(cmd):
                m["mvn_compile"] += 1; cat = "tests"
            # Ignore process names inside pkill/pgrep patterns (pkill -f spring-boot:run is a stop, not a launch).
            launch_cmd = re.sub(r"(pkill|pgrep)\s+(-\w+\s+)*(\"[^\"]*\"|'[^']*'|\S+)", "", cmd)
            if RE_LAUNCH.search(launch_cmd):
                m["app_launches"] += 1; cat = "app"
            if RE_KILL.search(cmd) and ("8080" in cmd or "Application" in cmd or "spring-boot" in cmd or re.search(r"kill \d+", cmd)):
                m["app_kills"] += 1; cat = "app"
            if RE_CP_TARGET.search(cmd):
                m["cp_to_target"] += 1
            if RE_POLL.search(cmd):
                m["poll_loops"] += 1
            # Waiting (a sleep or a poll loop) counts as time spent on the app, unless the command also runs tests.
            if (RE_POLL.search(cmd) or re.search(r"(^|[;&\s])sleep \d", cmd)) and cat == "other":
                cat = "app"
            if re.search(r"\b(lsof|ps -o|ps aux|pgrep|curl[^\n]*localhost)", cmd):
                m["status_probes"] += 1
            if re.search(r"(src/main/[^\s'\"]+\.(java|css))", cmd) and re.search(r"python3 -|sed -i|perl -[0p]*i|cat >", cmd):
                first_edit = first_edit or ts
            if "Blocked: sleep" in out:
                m["sleep_blocked"] += 1
        elif tool in ("Edit", "Write", "MultiEdit"):
            if "/src/" in inp.get("file_path", ""):
                first_edit = first_edit or ts
            m["file_edits"] += 1
        elif tool.startswith("mcp__playwright"):
            m["browser"] += 1; cat = "browser"
            first_browser = first_browser or ts
            code = json.dumps(inp)
            if tool.endswith("navigate"):
                m["page_loads"] += 1
            m["page_loads"] += len(re.findall(r"page\.(goto|reload)|location\.reload", code))
            if tool.endswith("take_screenshot"):
                m["screenshots"] += 1
        elif tool.startswith("mcp__Vaadin") or tool.startswith("mcp__vaadin"):
            m["docs"] += 1; cat = "docs"
        elif tool == "Skill":
            if "devloop" in json.dumps(inp):
                m["devloop_skill"] += 1
        elif tool == "Monitor":
            m["monitor"] += 1
        tool_time[cat] += dur
    usage = final.get("usage", {})
    diff = open(os.path.join(run_dir, "diff.patch")).read()
    files = re.findall(r"^\+\+\+ b/(.+)$", diff, re.M)
    row = {
        "run": name, "setup": setup,
        "result": final.get("subtype", "missing"),
        "wall_s": round(agent_wall, 1),
        "exit_lag_s": round(wall["wall"] - agent_wall, 1),
        "cost_usd": round(final.get("total_cost_usd", 0), 3),
        "turns": final.get("num_turns", 0),
        "api_s": round(final.get("duration_api_ms", 0) / 1000, 1),
        "output_tokens": usage.get("output_tokens", 0),
        "time_to_first_edit_s": round(first_edit - t0, 1) if first_edit else None,
        "time_to_first_browser_s": round(first_browser - t0, 1) if first_browser else None,
        "tool_s_app": round(tool_time["app"], 1),
        "tool_s_tests": round(tool_time["tests"], 1),
        "tool_s_browser": round(tool_time["browser"], 1),
        "tool_s_docs": round(tool_time["docs"], 1),
        "tool_s_other": round(tool_time["other"], 1),
        "vd_status": vd["status"], "vd_start": vd["start"], "vd_apply": vd["apply"],
        "vd_restart": vd["restart"], "vd_stop": vd["stop"] + vd["shutdown"], "vd_other": sum(v for k, v in vd.items() if k not in ("status", "start", "apply", "restart", "stop", "shutdown")),
        "files_changed": len(files),
        "test_files_changed": sum(1 for f in files if f.startswith("src/test/")),
        "lines_added": len(re.findall(r"^\+(?!\+\+)", diff, re.M)),
        "tests_pass": open(os.path.join(run_dir, "test.exit")).read().strip() == "0",
        "app_running_after": open(os.path.join(run_dir, "app-after.txt")).read().strip() == "running",
        "prestart_failed": os.path.exists(os.path.join(run_dir, "prestart.failed")),
    }
    for k in ["tool_calls", "tool_errors", "bash", "bash_background", "browser", "page_loads", "screenshots", "docs",
              "mvn_test", "mvn_compile", "app_launches", "app_kills", "cp_to_target", "poll_loops", "status_probes",
              "sleep_blocked", "file_edits", "devloop_skill", "monitor"]:
        row[k] = m[k]
    return row


def stats(vals):
    vals = [v for v in vals if v is not None]
    if not vals:
        return None
    vals = sorted(vals)
    q = statistics.quantiles(vals, n=4) if len(vals) > 1 else [vals[0]] * 3
    return {"n": len(vals), "mean": round(statistics.mean(vals), 2), "median": round(statistics.median(vals), 2),
            "p25": round(q[0], 2), "p75": round(q[2], 2), "min": vals[0], "max": vals[-1]}


def main():
    rows = []
    for d in sorted(os.listdir(RES)):
        p = os.path.join(RES, d)
        if os.path.isdir(p) and os.path.exists(os.path.join(p, "wall.json")):
            rows.append(analyze(p))
    if not rows:
        print("no runs found"); return
    with open(os.path.join(RES, "runs.csv"), "w", newline="") as f:
        w = csv.DictWriter(f, fieldnames=list(rows[0].keys()))
        w.writeheader(); w.writerows(rows)
    numeric = [k for k, v in rows[0].items() if isinstance(v, (int, float)) and not isinstance(v, bool) or v is None]
    numeric = [k for k in rows[0] if k not in ("run", "setup", "result")]
    summary = {}
    for s in SETUPS:
        rs = [r for r in rows if r["setup"] == s]
        if not rs:
            continue
        summary[s] = {k: stats([float(r[k]) if not isinstance(r[k], bool) else (1.0 if r[k] else 0.0) for r in rs if r[k] is not None]) for k in numeric}
        summary[s]["runs"] = len(rs)
        summary[s]["results"] = dict(Counter(r["result"] for r in rs))
    json.dump(summary, open(os.path.join(RES, "summary.json"), "w"), indent=1)
    keys = ["wall_s", "cost_usd", "turns", "api_s", "output_tokens", "tool_calls", "bash", "browser", "page_loads",
            "app_launches", "app_kills", "cp_to_target", "poll_loops", "status_probes", "mvn_test", "docs",
            "tool_s_app", "tool_s_tests", "tool_s_browser", "time_to_first_edit_s", "time_to_first_browser_s",
            "vd_status", "vd_start", "vd_apply", "vd_restart", "lines_added", "test_files_changed", "tests_pass",
            "app_running_after", "sleep_blocked", "tool_errors", "devloop_skill"]
    present = [s for s in SETUPS if s in summary]
    print("| metric (median, mean) | " + " | ".join(f"{s} (n={summary[s]['runs']})" for s in present) + " |")
    print("|---|" + "---|" * len(present))
    for k in keys:
        cells = []
        for s in present:
            st = summary[s].get(k)
            cells.append("–" if not st else f"{st['median']:g} / {st['mean']:g}")
        print(f"| {k} | " + " | ".join(cells) + " |")


if __name__ == "__main__":
    main()
