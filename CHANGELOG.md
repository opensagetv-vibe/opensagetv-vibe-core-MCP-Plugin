# Changelog

## Unreleased

- Created a stock-compatible SageTV Standard control bridge and external MCP adapter project.
- Added bounded, authenticated controls for UI contexts, exact indexed media, playback, channels, captions, library scans, and diagnostics.
- Added deterministic build/package, source validation, and stock-Core compatibility tests.
- Translate MCP media-relative seek targets to SageTV's absolute recording timeline while retaining explicit absolute mode.
- Cache the exact indexed-path lookup for one minute and invalidate it after a library scan.
- Use stock `GetMediaFileAiring` for confirmation-guarded watched-state clearing.
- Installed version 0.1.1 on stock `.175` and passed the direct control gate,
  non-Pro Android exact-path playback/seek/pause gate, and live-channel gate.
  The stock `Sage.jar` remained byte-identical.
