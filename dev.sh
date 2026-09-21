#!/usr/bin/env bash
set -euo pipefail
ROOT="$(cd "$(dirname "$0")" && pwd)"
COMMAND="${1:-all}"
if [[ $# -gt 0 ]]; then shift; fi
case "$COMMAND" in
  test|validate|build|install|all)
    BUILD_ENV="${OPENSAGETV_VIBE_BUILD_ENV_ROOT:-$ROOT/../opensagetv-vibe-build-env}"
    exec bash "$BUILD_ENV/scripts/component-dev.sh" "$ROOT" "$COMMAND" "$@"
    ;;
  package) exec python3 "$ROOT/scripts/project.py" package "$@" ;;
  mcp) export PYTHONPATH="$ROOT/mcp/src${PYTHONPATH:+:$PYTHONPATH}"; exec python3 -m opensagetv_vibe_core_mcp.server "$@" ;;
  help|-h|--help)
    echo 'Usage: ./dev.sh {test|validate|build|install|package|all|mcp|help}'
    ;;
  *) echo "Unknown command: $COMMAND" >&2; exit 2 ;;
esac
