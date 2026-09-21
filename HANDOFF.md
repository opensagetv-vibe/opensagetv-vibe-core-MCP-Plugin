# Handoff

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

Local development only. Do not create a GitHub repository, release, or SageTV plugin-catalog submission without explicit approval.

## Container integration

The Vibe container and unified build environment treat this plugin as a required
versioned component. Runtime images seed the exact packaged JAR into appdata,
register it as a SageTV Standard plugin, preserve local listener/token policy,
and fail validation unless SageTV loads it and its loopback health endpoint is
ready. Version `0.1.1` is installed and healthy in the rebuilt `.232` Vibe test
container; the previous container is retained under a rollback name.
