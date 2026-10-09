# OpenSageTV Vibe Core MCP Plugin

Read-only `server.activity` reports recording, UI-context and connected-client
counts through stock APIs, exposed as the `sage_server_activity` MCP tool.
`safeToRestart` requires every count to be zero;
unknown status is an error, not idle. Always recheck immediately before a
restart; a user-approved window alone does not authorize interrupting
recordings. This action's physical commissioning is tracked in MCP-ACTIVITY-001.

Companion commissioning optionally uses `companion.config_get/set` for only
`enabled`, `reload.recovery`, `trace.dvd`, `dvd.tv_skip_keys`; writes require
`confirm=true`, Boolean `value` and matching `expected`. `companion.dvd_hook`
inspects one exact connected12-hex MiniClient; an explicit Boolean `enabled`
requires confirmation and the exact prior blank/known-adapter `expected`.
It never accepts arbitrary class names or replaces another third-party player.
Checkpoint and restore every borrowed value, including the UI hook, even on
failure. This is test control, not a normal playback dependency.

Stock-compatible SageTV server control for deterministic commissioning and MCP automation.

The project contains two deliberately separated components:

- **Vibe Core Control Bridge** — a Java 8-compatible SageTV Standard plugin that invokes only supported stock SageTV APIs.
- **Vibe Core MCP adapter** — an external Python MCP stdio server that calls the authenticated bridge API.

The bridge does not patch `Sage.jar`, add MiniClient wire events, replace Sagex, execute arbitrary Sage expressions, or expose a shell.

## Supported controls

- Discover UI contexts and connected clients.
- Resolve a SageTV-indexed MediaFile by exact server path or MediaFile ID.
- Watch media, including a verified watch-from-beginning sequence.
- Query active media, playback time, duration, seek window, rate, and caption state.
- Commission only the installed FFmpeg plugin's caption-listener Boolean
  through bounded public plugin APIs, with confirmation, expected-value
  protection and readback. Restore the checkpointed value after testing;
  this is not a general plugin-settings proxy or playback dependency.
- Play, pause, stop, seek, skip, and set supported playback rates.
- Tune an exact channel.
- Get/set stock SageTV CC state.
- Send a small allowlisted set of Sage commands such as `TV`, `Back`, `Home`,
  and the stock DVD menu/chapter/audio/subtitle commands.
  The current validation candidate also permits `Time Scroll`. This is a
  stateful STV seek-cursor command, not a timeline keepalive: send it once to
  enter, adjust with primary Skip Fwd/Bkwd, then send it once to commit.
- Run the ordinary SageTV library import scan.
- Clear watched state for one explicitly identified MediaFile.
- Return a bounded diagnostic snapshot.

## Security defaults

- Bind address: `127.0.0.1`
- Port: `8270`
- LAN access: disabled
- Bearer token: generated on first start
- Maximum request body: 64 KiB
- No arbitrary file paths: playback resolves only SageTV-indexed MediaFiles
- No arbitrary Sage API names: actions and command names are allowlisted

For a container using host networking, enable LAN access and set the bind address to `0.0.0.0` through the SageTV plugin configuration before connecting from another host.

## Build

Provide an unmodified stock `Sage.jar`:

```powershell
$env:SAGETV_JAR = "C:\path\to\stock\Sage.jar"
.\dev.cmd all
```

Or place it at `.deps/stock/Sage.jar`. Compile-only SageTV classes are never packaged in the plugin JAR.

## Publication status

The source repository is public at
<https://github.com/opensagetv-vibe/opensagetv-vibe-core-MCP-Plugin>.
Version 0.1.4 is an unreleased validation candidate. Versioned release artifacts
and the SageTV plugin-catalog entry are published only from the deterministic
packages produced by `dev.cmd all`.

Ordinary `media.watch` now acknowledges the stock `Watch` request without a
decoder-state wait; the caller must then verify playback through `ui.state` or
client diagnostics. `from_beginning=true` retains a bounded load/seek wait and
reports `fromBeginningApplied=false` if the seek could not yet be applied.

## Local MCP configuration

Copy the example and keep real tokens only in the ignored file:

```powershell
Copy-Item config\core-mcp.example.toml config\core-mcp.toml
```

Run the stdio server:

```powershell
.\dev.cmd mcp
```

## Compatibility boundary

The bridge replaces server-side commissioning controls, not MiniClient playback implementation. Native/Hybrid/MIM DVD transport, new caption payload delivery, decoder scheduling, and other internal player changes remain Core/client capabilities and are reported as unavailable when stock SageTV cannot provide them.

## Commissioned stock-server result

Version 0.1.1 was installed as a Standard plugin on unmodified SageTV
9.2.17.1056 at the `.175` compatibility server. The stock `Sage.jar` remained
byte-identical before and after installation. The bridge passed authenticated
capability discovery, UI-context selection, exact indexed-path resolution,
watch/from-beginning, state, relative seek, play/pause, exact channel tune,
caption get/set, library scan, watched-state clearing, and diagnostics.

The Android commissioning MCP then passed Media3 hardware Pull playback on the
non-Pro Fire TV `.25`, including MPEG-2 video, AC-3 audio, fullscreen, seek,
pause/resume, live-TV channel control, and crash-log gates. See `HANDOFF.md` for
the exact boundary and `output/BUILD_REPORT.md` after running `dev.cmd all`.
