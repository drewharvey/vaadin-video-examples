#!/bin/bash
# Builds the two project templates the runs are copied from.
# Usage: ./setup.sh <path to starter zip>   (the zip holds an app/ folder: the Vaadin starter with the Task List view)
set -euo pipefail
B="$(cd "$(dirname "$0")" && pwd)"
ZIP="$(cd "$(dirname "$1")" && pwd)/$(basename "$1")"
rm -rf "$B/templates"; mkdir -p "$B/templates"
for arm in vanilla devloop; do
  t="$B/templates/$arm"; tmp="$B/templates/.tmp-$arm"
  mkdir "$tmp" && unzip -q "$ZIP" -d "$tmp" && mv "$tmp/app" "$t" && rmdir "$tmp"
  cd "$t"
  # Warm the local Maven cache so the first run doesn't pay for downloads.
  ./mvnw -q -DskipTests compile >/dev/null
  [ "$arm" = devloop ] && ./mvnw -q vaadin:install-dev-cli
  # Headless, in-memory Playwright for unattended runs. Same in both arms.
  jq '.mcpServers.playwright.args=["@playwright/mcp@0.0.73","--headless","--isolated"]' .mcp.json > .mcp.json.tmp && mv .mcp.json.tmp .mcp.json
  rm -rf target
  # Give the project its own repo so the baseline commit never lands in a parent repo.
  [ -d .git ] || git init -q
  git add -A && git -c user.name=bench -c user.email=bench@example.com commit -qm baseline
  echo "template $arm ready: $(git log --oneline | head -1)"
done
