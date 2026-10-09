# Changelog

## 0.1.5 - release preparation

- Resolve an already indexed exact path through stock `GetMediaFileForFilePath`
  before enumerating unrelated media. Preserve canonical spelling, verified
  DVD parent/`VIDEO_TS` aliases, existing segment-cache fallback and invalid-
  path guards. The cold-cache contract models a blocked unrelated library
  without claiming an unmeasured physical speedup.
- Add fixed-ID Client Extension availability, installed-status and confirmed
  installation controls through public SageTV plugin APIs. Require exact
  numeric version, Standard metadata, compatibility and fail-closed independent
  recording/UI/client idle checks. No URLs, arbitrary IDs, registry edits,
  stock Core changes or automatic restart are exposed.
- Expose strict `sage_companion_available`, `sage_companion_status` and
  `sage_companion_install` MCP tools. Only an explicit availability request
  refreshes the repository. Propagate installation failures and pending
  `RESTART` without interpreting either as loaded-runtime success.
- Qualify the control candidate against unmodified stock Linux/Windows:
  JDK11/Java8, all5 Java contracts and10 Python tests pass. Both actual public
  companion installations returnOK with numeric0.1.0/enabledtrue readback,
  no installer restart and three actual HTTP confirmation/version refusals.
  Restore borrowed options; Core/registry files remain untouched.
- Measure indexed DVD parent/VIDEO_TS lookup at0.015-0.031s on Linux and
  0.031-0.078s on Windows, where earlier cold lookup timed out15s. Read-only
  activity observes actual idle counts and an active Windows UI returns
  safeToRestartfalse. Do not infer generic startup or legacy hardware results.
- Repair timestamp-suffixed Linux development metadata through a reviewed
  numeric catalog/public reinstall, not Core or registry edits. The real
  refresh notification parse error is reported; independent availability
  verifies the intended version before install. Public hashes, exact-HEAD
  green CI and catalog submission remain separate release requirements.

### Previously implemented commissioning controls

- Add fail-closed, read-only stock server.activity restart preflight. Zero
  UI clients alone is not idle proof; recording and connected-client counts
  must also be zero. Expose it as sage_server_activity without stop/restart
  authority; routing/error contracts pass. The actual deployment preflight
  refuses active175 recordings before replacement. Physical deployment pending idle.

- Add bounded companion commissioning controls for four fixed Boolean options
  and a known DVD hook on one connected MiniClient, with checkpoint/readback
  guards and third-party refusal. Focused stock175 control/restore and negative
  guards pass; broader DVD/Windows gates remain separate;
  this does not change normal runtime playback or expose an arbitrary API.

## 0.1.4 - 2026-10-09

- Published4 public assets; independent downloads match GitHub digests and
  catalog JAR ZIP MD5. Catalog update submitted in plugin-repo#127, awaiting
  merge/aggregate generation. Physical activity/Time Scroll gates stay open;
  post-release task closure is documentation-only with immutable tag/runtime.

- Prepare the user-approved beta publication with stock-JAR Java8/JDK11,
  three Java suites, five Python contracts and deterministic package validation.
  Existing caption/companion physical results remain scoped; new server.activity
  physical commissioning stays open. Publication never grants recording-stop
  or restart authority and is not a normal-playback dependency.

- Add bounded typed FFmpeg caption-listener commissioning through public
  plugin APIs with confirmation, expected-value guard and readback. Scope/
  restore contracts and authenticated stock175 false/true/false proof pass;
  no arbitrary settings proxy or normal-playback MCP dependency.

- Add the stock `Time Scroll` command to the bounded UI allowlist for
  STV-owned DVD seek-cursor testing. It remains a supported SageCommand call,
  not a new MiniClient protocol event or arbitrary API proxy.

- Acknowledge ordinary exact-path `Watch` without synchronously polling the
  replacing MiniClient decoder. Callers still verify playback separately;
  explicit watch-from-beginning retains a bounded loaded-media wait and now
  reports whether its initial seek was actually applied.
- Preserve the stock `Sage.jar` API boundary and allow an explicit legacy
  `wait_ms` request for commissioning clients that need the old behavior.
- Allow temporary library-import commissioning to be undone through the stock
  `RemoveLibraryImportPath` API, with explicit confirmation and exact-path
  validation. This supports clean stock-server test teardown.

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
