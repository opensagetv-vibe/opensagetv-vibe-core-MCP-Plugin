# Handoff

## PLUGIN-RELEASE-001 published and catalog submitted (2026-10-09)

Beta0.1.4: https://github.com/opensagetv-vibe/opensagetv-vibe-core-MCP-Plugin/releases/tag/v0.1.4
Tag c71792407c18c74248b1f080c70a563ada80fac0 has green CI and4 independently
downloaded/digest-verified assets. Catalog manifest MD5 matches public JAR ZIP.
Catalog PR127 OPEN/MERGEABLE: https://github.com/OpenSageTV/sagetv-plugin-repo/pull/127
Ordinary plugin-manager availability awaits merge/aggregate generation.
MCP-ACTIVITY-001 and MCP-TIMESCROLL-001 physical commissioning remain open;
no server installation/restart or recording interruption. Documentation-only
closure does not rewrite tag/runtime. Older pending-publication notes below
are historical, not current authority or a new deployment result.

Qualification detail: Core MCP0.1.4 stock-JAR Java8/JDK11,
three Java suites/five Python contracts/source validation pass. Package epoch
2026-10-09 yields JAR ZIP3934a21b7cd0c620578eddfee059d7172d853cf103829a675ae3cf3af0775a4e,
MD5 2fef6e12bbdca3ca3a8068dcbad82165. Verify exact-HEAD CI/public hashes
before catalog submission. New server.activity physical commissioning remains
open; publication neither installs a server nor grants recording interruption.

## Current next gate: MCP-ACTIVITY-001

Read-only server.activity source/stock-Java8/contracts/validation pass. It
counts current recordings/UI/connected clients and fails closed on unknown
arrays, not just zero UI clients. Stock175 currently reports3 recordings via
its existing exact read-only Sagex API; no replacement/restart while active.
Installed bridgebe765e9a lacks this new action; deployment waits for idle.
Corrected VCE comparison shows stock also resumes title after title exposure;
unproven playback changes withdrawn, local runtime exactly e25263da.175 still
has experimental27e loaded; restore after safe idle/explicit recording authority.
Window renewal remains valid through2026-10-09 03:52:59 UTC, not recording-stop
authority. Tool commissioning remains complete; parent physical gate is open.

2026-10-09 00:31 UTC follow-up: actual175 deployment preflight rejected active
recordings before opening SSH or replacing JARs; no recordings stopped.
Java stock-JAR build, three Java contracts, five Python adapter/transport
tests and source validation pass. New sage_server_activity MCP route only
observes activity, propagates bridge failures and grants no restart authority.
An independent232 bootstrap attempt stopped before SSH: its configured Sagex
recording route returned404 and installed bridge lacks server.activity. No
232 JAR replacement/restart occurred; unknown recording activity stays unsafe.
Deployment helper removes only its own hash-verified disposable .pending
stages on failure; rollback backups and local candidates remain recoverable.

Stock175 restart renewal received2026-10-08 23:52:59 UTC, four hours through
2026-10-09 03:52:59 UTC, unless revoked. This supersedes expiry/approval-pending
notes below. Check time before each plugin-only guarded restart.

## Current control state (supersedes the work-in-progress notes below)

MCP-COMPANION-001 control commissioning closes on stock175 candidatebe765e9a:
real installed options/hook write/readback/restoration and five precise HTTP
negative guards pass;113 app prefs/power and original four options restored,
effective hook blank, contexts empty, captionfalse unchanged. Local stock
Java8/linkage/contracts/three adapters/validation pass. Compact result under
artifacts/results/MCP-COMPANION-001. Third-party refusal remains local proof,
not a physical old-client claim. VCE-002 physical recovery/fallback gates stay
open. The later matched stock title test disproved the earlier root-only
Stop/restart oracle; the speculative PlaybackStopped cancellation change was
withdrawn. Restore qualified e252 runtime when idle under the renewed window
above. No publication or normal-runtime MCP dependency is authorized.

## Current work: MCP-COMPANION-001 (2026-10-08)

VCE-001 foundation qualifies, but its remaining per-UI companion gates need
bounded configuration controls. New test-only companion.config_get/set and
companion.dvd_hook use typed installed plugin/public apiUI, four non-secret
Booleans and one known adapter/blank property only. Confirm/expected/readback,
connected12-hex MiniClient and third-party refusal guards implemented; stock
compile/local contracts pass.175 candidatebe765e9a now deployed alongside
Client Extensione25263da with zero clients before restart, rollback copies
and unchanged stock Sage.jar/root FFmpeg. First physical gate rejected the
old companion's absent dvd.tv_skip_keys option; all borrowed values restored.
New metadata guard names this explicit unsupported option, never inventing a
Boolean default. Current non-Pro physical restoration/fallback gate runs. No
stock Sage.jar change, arbitrary API proxy or production playback dependency.
175 restart authorization expires2026-10-08 23:31:05 UTC unless revoked;
check time before restart. No publication.

## MCP-PLUGIN-CONFIG-001 / stock175 commissioning (2026-10-08)

Typed installed-plugin caption-listener Boolean control is implemented using
supported plugin APIs only: fixed FFmpeg Linux/Windows plugin IDs, enabled
typed plugin, fixed caption_side_channel.enabled key, confirm plus expected
value and readback. Java8 stock linkage, scope/restore contracts and3 Python
adapter tests pass. JAR6fff42f0 is installed on175; public health/actions and
false/true/false setting restoration passed, protected Sage.jar/root FFmpeg
unchanged. The parent has now restored the originalfalse value using this API
with readback after zero owned sessions. MCP-PLUGIN-CONFIG-001 closes; no production
playback dependency, arbitrary setting proxy or raw INI edits.
Time Scroll is now included in the installed175 bridge, but that physical
command's own gate is not implied by caption-listener proof. No publication.

## Stock Time Scroll test control (2026-10-06)

The installed `.175` bridge rejected `Time Scroll` as not allowlisted during
Android DVD-002 testing. Added only that standard UI command to ControlService
and its contract test. Stock Java 8 linkage, Java contracts and all three
Python adapter tests pass. No JAR was installed, no server was restarted and
no release was published. The next bridge deployment must verify the command
through its authenticated endpoint. Android additionally exposes existing
MiniClient event 10 as `time_scroll`, so the production stock-wire sequence
can be physically checked without deploying this test-bridge update.

## 0.1.4 Vibe `.232` test-control refresh (2026-10-03)

During the MIMFIX-003 authored-caption A/B cleanup, `.232`'s older Core MCP
JAR lacked `library.remove_import_path`. The current stock-API-compatible
0.1.4 source passed `dev.cmd all` against stock Sage.jar and was installed
only on the Vibe `.232` test container. Its installed JAR SHA-256 is
`4b5cb1b801957fbb073fea1b98bbffedfe2c8b3b91df56ca695e9976110b1288`;
the prior JAR SHA-256 was
`b73d97df92160f6920cef3dc09f866664f1e98005d9cb2debb51efb8e0b92167`
and remains recoverable as a non-`.jar` backup beside the installed JAR.
The container restarted at expected IP `.232`, `/health` reported 0.1.4,
and the exact temporary import was removed through stock
`RemoveLibraryImportPath` and rescanned. Sage.jar and production `.175` were
not modified. No commit or publication was made for this test checkpoint.

## 0.1.4 Windows Watch follow-up (2026-10-03)

Clean stock-Core Windows `.185` now runs the unreleased Core MCP 0.1.4 JAR.
Ordinary exact-path `Watch` acknowledges without decoder-state polling;
explicit watch-from-beginning retains its bounded wait and reports whether
the initial seek was applied. The Android MCP adapter and outer test timeout
were updated to match. The additional allowlisted
`library.remove_import_path` action calls stock `RemoveLibraryImportPath`
with an existing exact path and `confirm=true`, allowing temporary test
imports to be removed without editing Sage.properties.

Java 8 compilation against stock `Sage.jar`, Java/Python contract tests,
source validation, and deterministic v0.1.4 packaging pass (`dev.cmd all`).
The revised local ZIP SHA-256 is
`20e83740b93db092f6637b3b78f872e91d25bd34ff2f203a5235279650255da3`;
the installed JAR SHA-256 is
`4235f8a34245ae867df95a33ef41332878ffe508cde659f843464521ec22dee7`.
The original 0.1.1 JAR remains recoverable beside it with a non-`.jar`
backup suffix. Stock `Sage.jar` stayed unchanged at
`d76ded981b9bc51e25b9cec821b6abeb771b46c2996dc45e453349b5e703fcb0`.
The formerly locked desktop SageTV processes were absent when replacement
began; no process was terminated for this deployment. Only `SageTV64` was
restarted. Core `/health` reports 0.1.4.

The `.185` media database still indexed a `V:` drive that no longer existed.
The generated 1.125 GB seek/CEA fixture was copied to an explicitly named
temporary local directory, added/scanned through the stock API, and played
on non-Pro `.25` with `MIM_DIRECT` ownership and advancing A/V. A focused
FF/REW gate passed on repeat; one earlier attempt failed and remains a
stability finding. The caption side channel attached and was active, but
rendered no non-empty cue, so the combined caption gate remains open in the
Android/FFmpeg MIMFIX-003 backlog. The temporary import was removed through
the new API, the verified duplicate file/directory was deleted, and a final
scan restored the original import-path property. The Android test restored
all 108 checkpointed settings and device stay-awake values. No publication
has occurred.

## 0.1.3 release preparation (2026-09-29)

Version 0.1.3 synchronizes the repository policy/documentation updates with a
versioned rebuild of the existing bounded bridge. Runtime controls are
unchanged from 0.1.2. The release must still pass stock-Sage API linkage,
security contracts, Java/Python tests, deterministic packaging, manifest, and
catalog validation before publication.

## 0.1.2 public prerelease (2026-09-29)

Version 0.1.2 passes source validation, Java 8 compilation against the stock
Sage.jar, the stock API/JSON contract test, MCP adapter authentication/config
tests, and deterministic packaging. The packaged plugin ZIP SHA-256 is
`058e061fd733cfce7f860694bb719bd55a530ae36f280e5c58f4782913d589ff`.
This release extends only allowlisted stock APIs and does not modify Sage.jar.

## Objective

Provide deterministic SageTV server control to MCP tooling while leaving stock `Sage.jar` unchanged.

## Architecture

```text
AI/MCP client
  -> external Python MCP stdio adapter
  -> authenticated form/JSON HTTP bridge
  -> SageTV Standard plugin
  -> sage.SageTV.api / sage.SageTV.apiUI
  -> stock SageTV 9.2.17+
```

## Non-negotiable boundaries

- No private MiniClient events.
- No Core reflection.
- No arbitrary Sage API proxy.
- No shell or unrestricted path execution.
- Exact-path playback must resolve an already indexed SageTV MediaFile.
- The plugin must fail closed when its token, context, path, or capability is invalid.

## Commissioning target

The first physical target is the unmodified SageTV server at `.175`. Its stock `Sage.jar` hash must remain unchanged before and after installation. Existing Sagex/Web Interface plugins may remain installed, but bridge tests must call the new bridge endpoint directly.

## Commissioning result

- Plugin version: `0.1.1`.
- Stock server: SageTV `9.2.17.1056`, Java `11.0.15`, `.175`.
- Stock `Sage.jar` SHA-256 before and after: `d76ded981b9bc51e25b9cec821b6abeb771b46c2996dc45e453349b5e703fcb0`.
- Plugin JAR SHA-256 installed separately: `1f5bd8894314e330b63cd6c893493160d282b9792ef3a8ca4bece9cc054ccbb8`.
- Exact fixture: `/var/media/OpenSageTV_Vibe_Tests/VibeSeekTest-1080i-MPEG2-AC3-CC.ts` (`MediaFileID 65513439`).
- Direct bridge gate: all allowlisted control families passed.
- Android integration gate: non-Pro Fire TV `.25`, Media3, hardware decoding,
  SageTV Pull, MPEG-2/AC-3, exact-path start, seek, pause/resume, fullscreen,
  live channel `2.1`, and no crash signature passed.

The first exact-path lookup after a cold index cache measured about seven
seconds; the cached lookup measured milliseconds. The bridge cache is bounded
to 60 seconds and is invalidated by a requested library scan.

DVD roots now resolve through both their indexed `VIDEO_TS` path and the parent
disc directory. On stock `.175`, ALADDIN resolved as MediaFile `42983134` and
the authored fixture resolved as `65513423`. With ALADDIN's main title stable,
public `Seek(long)` moved from 505,537 ms to 900,399 ms within one second and
from 621,386 ms to exactly 240,000 ms on the first sample, then continued
advancing. This proves that event 233 is unnecessary for external MCP
automation. Android now handles display-mode recovery with a local Media3
Surface refresh that does not seek or replace the server stream, so the Vibe
Core event-233 handler is removed as well.

## Remaining Core boundary

The plugin replaces test and commissioning uses of private MiniClient events,
not the underlying media protocol. It cannot add native/hybrid/MIM DVD
transport, media command 30, `MEDIA_STATE_URL`, new caption payload delivery,
decoder scheduling, reconnect fixes, or DVD VM behavior. Those remain explicit
Core/client capabilities until accepted upstream as versioned protocol work.

## Publication status

Public source and the approved v0.1.2 prerelease are published under the
`opensagetv-vibe` organization. The downloaded release ZIP matches the SHA-256
above, current GitHub repository checks pass, and the verified SageTV catalog
entry is submitted in OpenSageTV/sagetv-plugin-repo pull request 126.

## Container integration

The Vibe container and unified build environment treat this plugin as a required
versioned component. Runtime images seed the exact packaged JAR into appdata,
register it as a SageTV Standard plugin, preserve local listener/token policy,
and fail validation unless SageTV loads it and its loopback health endpoint is
ready. Version `0.1.1` is installed and healthy in the rebuilt `.232` Vibe test
container; the previous container is retained under a rollback name.
