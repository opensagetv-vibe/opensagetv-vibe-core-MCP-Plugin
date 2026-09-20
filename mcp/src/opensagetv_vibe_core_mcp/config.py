from __future__ import annotations

from dataclasses import dataclass
import os
from pathlib import Path
from typing import Any

try:
    import tomllib
except ModuleNotFoundError:  # pragma: no cover - Python 3.10 compatibility
    import tomli as tomllib


@dataclass(frozen=True)
class ServerConfig:
    alias: str
    base_url: str
    token: str
    timeout_seconds: float = 10.0
    verify_tls: bool = True


def default_path() -> Path:
    explicit = os.environ.get("OPENSAGETV_VIBE_CORE_MCP_CONFIG", "").strip()
    if explicit:
        return Path(explicit).expanduser().resolve()
    return Path(__file__).resolve().parents[3] / "config" / "core-mcp.toml"


def load_config(path: Path | None = None) -> tuple[str, dict[str, ServerConfig]]:
    source = (path or default_path()).resolve()
    if not source.is_file():
        raise RuntimeError(
            f"Core MCP configuration is missing: {source}. Copy config/core-mcp.example.toml first."
        )
    data: dict[str, Any] = tomllib.loads(source.read_text(encoding="utf-8"))
    active = str(data.get("active_server", "")).strip()
    servers: dict[str, ServerConfig] = {}
    for name, raw in dict(data.get("servers", {})).items():
        raw = dict(raw)
        servers[str(name)] = ServerConfig(
            alias=str(raw.get("alias", name)),
            base_url=str(raw.get("base_url", "")).rstrip("/"),
            token=str(raw.get("token", "")),
            timeout_seconds=max(0.5, float(raw.get("timeout_seconds", 10))),
            verify_tls=bool(raw.get("verify_tls", True)),
        )
    if active not in servers:
        raise RuntimeError(f"active_server {active!r} is not defined in {source}")
    selected = servers[active]
    if not selected.base_url or not selected.token:
        raise RuntimeError(f"Server {active!r} requires base_url and token in {source}")
    return active, servers

