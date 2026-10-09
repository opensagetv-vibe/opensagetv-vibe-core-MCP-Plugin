from __future__ import annotations

import re
from typing import Any

from mcp.server import MCPServer

from .client import BridgeClient
from .config import load_config


mcp = MCPServer("OpenSageTV Vibe Core MCP")


def _client(server: str = "") -> tuple[str, BridgeClient]:
    active, servers = load_config()
    selected = server.strip() or active
    if selected not in servers:
        raise ValueError(f"Unknown server {selected!r}; available={sorted(servers)}")
    return selected, BridgeClient(servers[selected])


def _call(action: str, server: str = "", **parameters: Any) -> dict[str, Any]:
    selected, client = _client(server)
    result = client.call(action, **parameters)
    result["server"] = selected
    return result


@mcp.tool()
def sage_server_health(server: str = "") -> dict[str, Any]:
    selected, client = _client(server)
    result = client.health()
    result["server"] = selected
    return result


@mcp.tool()
def sage_capabilities(server: str = "") -> dict[str, Any]:
    return _call("capabilities", server)


@mcp.tool()
def sage_server_activity(server: str = "") -> dict[str, Any]:
    """Observe recordings/clients before a restart; never restart or stop them.

    Recheck immediately before an authorized restart. A healthy zero-count
    snapshot does not itself grant restart authority or reserve future idle.
    Missing/failed bridge actions remain errors, never an idle fallback.
    """
    return _call("server.activity", server)


@mcp.tool()
def sage_companion_available(refresh: bool = False, server: str = "") -> dict[str, Any]:
    """Inspect only Client Extension metadata; refresh the repository explicitly.

    This never installs a package or restarts SageTV. It cannot accept a plugin
    ID, URL, path or class name from the caller.
    """
    if type(refresh) is not bool:
        raise ValueError("refresh must be a Boolean")
    return _call("companion.available", server, refresh=refresh)


@mcp.tool()
def sage_companion_status(server: str = "") -> dict[str, Any]:
    """Read the fixed Client Extension's installed version and enabled state."""
    return _call("companion.status", server)


@mcp.tool()
def sage_companion_install(expected_version: str, confirm: bool = False,
                           server: str = "") -> dict[str, Any]:
    """Commission the exact available Client Extension through stock plugin APIs.

    Requires explicit confirmation, numeric version and independently idle
    recording/client counts. RESTART means pending installation, not that this
    tool restarted SageTV. Restart authorization and a fresh preflight remain
    separate; normal playback never requires this MCP commissioning tool.
    """
    if confirm is not True:
        raise ValueError("confirm=true is required for companion installation")
    if not isinstance(expected_version, str) or not re.fullmatch(
            r"(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)\.(0|[1-9][0-9]*)", expected_version):
        raise ValueError("expected_version must be exact numeric major.minor.patch")
    return _call("companion.install", server, expected_version=expected_version, confirm=True)


@mcp.tool()
def sage_ui_contexts(server: str = "") -> dict[str, Any]:
    return _call("ui.list", server)


@mcp.tool()
def sage_playback_state(context: str, server: str = "") -> dict[str, Any]:
    return _call("ui.state", server, context=context)


@mcp.tool()
def sage_resolve_media_path(path: str, server: str = "") -> dict[str, Any]:
    return _call("media.resolve_exact_path", server, path=path)


@mcp.tool()
def sage_watch_media(context: str, path: str = "", media_file_id: int = 0,
                     from_beginning: bool = False, wait_ms: int | None = None,
                     server: str = "") -> dict[str, Any]:
    """Request Watch; verify actual playback separately with state/diagnostics.

    Ordinary Watch acknowledges promptly by default. Supply ``wait_ms`` only
    when a bounded loaded-media observation is explicitly required.
    """
    return _call("media.watch", server, context=context, path=path,
                 media_id=media_file_id or None, from_beginning=from_beginning,
                 wait_ms=wait_ms)


@mcp.tool()
def sage_seek(context: str, target_ms: int, absolute: bool = False,
              server: str = "") -> dict[str, Any]:
    """Seek to media-relative milliseconds by default; absolute uses SageTV timeline units."""
    return _call("media.seek", server, context=context, target_ms=target_ms,
                 absolute=absolute)


@mcp.tool()
def sage_playback_control(context: str, operation: str, rate: float = 1.0,
                          server: str = "") -> dict[str, Any]:
    return _call("media.control", server, context=context, operation=operation, rate=rate)


@mcp.tool()
def sage_tune_channel(context: str, channel: str, server: str = "") -> dict[str, Any]:
    return _call("channel.tune", server, context=context, channel=channel)


@mcp.tool()
def sage_caption_state(context: str, state: str = "", server: str = "") -> dict[str, Any]:
    return _call("captions.set" if state else "captions.get", server,
                 context=context, state=state or None)


@mcp.tool()
def sage_ui_command(context: str, command: str, server: str = "") -> dict[str, Any]:
    return _call("ui.command", server, context=context, command=command)


@mcp.tool()
def sage_scan_library(wait_until_done: bool = False, server: str = "") -> dict[str, Any]:
    return _call("library.scan", server, wait_until_done=wait_until_done)


@mcp.tool()
def sage_add_import_path(path: str, server: str = "") -> dict[str, Any]:
    return _call("library.add_import_path", server, path=path)


@mcp.tool()
def sage_remove_import_path(path: str, confirm: bool = False,
                            server: str = "") -> dict[str, Any]:
    if not confirm:
        raise ValueError("confirm=true is required because this changes library settings")
    return _call("library.remove_import_path", server, path=path, confirm=True)


@mcp.tool()
def sage_clear_watched(media_file_id: int, confirm: bool = False,
                       server: str = "") -> dict[str, Any]:
    if not confirm:
        raise ValueError("confirm=true is required because this changes SageTV watch history")
    return _call("media.clear_watched", server, media_id=media_file_id, confirm=True)


@mcp.tool()
def sage_diagnostics(context: str = "", server: str = "") -> dict[str, Any]:
    return _call("diagnostics.snapshot", server, context=context or None)


def main() -> None:
    mcp.run(transport="stdio")


if __name__ == "__main__":
    main()
