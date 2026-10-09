from __future__ import annotations

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
