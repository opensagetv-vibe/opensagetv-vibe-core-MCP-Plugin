# Changelog

## 0.1.3 - 2026-09-29

- Standardized impact-based release validation and the stock-server MCP-first
  test-control policy across the Vibe repositories.
- Rebuilt the unchanged bounded stock-SageTV control surface with synchronized
  Java and Python adapter version metadata. No new control capability, Core
  patch, reflection path, or unrestricted filesystem/shell access was added.

## 0.1.2 - 2026-09-29

- Allowlist SageTV's existing DVD menu, return, chapter, audio, and subtitle
  commands for deterministic physical-disc commissioning, and expose read-only
  DVD/menu state without adding a private Core protocol.
- Prepared the project for public source publication with explicit security,
  dependency-license, executable-mode, metadata, and CI contracts.
- Made the stock-server MCP bridge the required first option for new testing,
  commissioning, diagnostic, and automation controls across Vibe projects.
- Adopted the common twelve-repository Vibe workflow, update, handoff, and
  release metadata contract.
- Integrated version `0.1.1` as a required seeded component in every Vibe
  production/debug server image and verified SageTV plugin loading plus the
  loopback health endpoint in the rebuilt `.232` test container.

- Resolve indexed DVD media by both `VIDEO_TS` and parent disc-root paths so
  stock-server exact-path commissioning can use the same directory users see.
- Created a stock-compatible SageTV Standard control bridge and external MCP adapter project.
- Added bounded, authenticated controls for UI contexts, exact indexed media, playback, channels, captions, library scans, and diagnostics.
- Added deterministic build/package, source validation, and stock-Core compatibility tests.
- Translate MCP media-relative seek targets to SageTV's absolute recording timeline while retaining explicit absolute mode.
- Cache the exact indexed-path lookup for one minute and invalidate it after a library scan.
- Use stock `GetMediaFileAiring` for confirmation-guarded watched-state clearing.
- Installed version 0.1.1 on stock `.175` and passed the direct control gate,
  non-Pro Android exact-path playback/seek/pause gate, and live-channel gate.
  The stock `Sage.jar` remained byte-identical.
- Physically proved the public `Seek(long)` API against stock `.175`'s
  server-owned ALADDIN DVD Push session. Stable backward and forward targets
  landed within one VOBU and continued playback; the earlier timeout was a
  commissioning/startup race, not a missing DVD API implementation.
