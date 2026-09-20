#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
COMMAND="${1:-all}"
if [[ $# -gt 0 ]]; then shift; fi
case "$COMMAND" in
  test|validate|build|package|all) exec python3 "$ROOT/scripts/project.py" "$COMMAND" "$@" ;;
  mcp) export PYTHONPATH="$ROOT/mcp/src${PYTHONPATH:+:$PYTHONPATH}"; exec python3 -m opensagetv_vibe_core_mcp.server "$@" ;;
  *) echo "Unknown command: $COMMAND" >&2; exit 2 ;;
esac

