# Tasks

This file records only the current project state.

- [x] Build the Standard plugin against stock SageTV.
- [x] Pass host unit, contract, package, and MCP adapter tests.
- [x] Install on stock `.175` without changing `Sage.jar`.
- [x] Prove authenticated capability, context, exact-path, watch, playback-state, seek, command, channel, caption, scan, and watched-state controls.
- [x] Integrate bridge-first discovery into Android commissioning MCP with Sagex/Web fallback.
- [x] Re-run required Android stock-server control tests on the non-Pro Fire TV.
- [x] Remove Core-only commissioning events 230-232 after proving their public bridge replacements.
- [x] Update the Core upstream/MCP evaluation with measured results and remaining gaps.
- [x] Prove public `Seek(long)` against server-owned DVD playback and record
  that no Core API correction is required for a stable title session.
- [x] Expose stock DVD menu/chapter/audio/subtitle Sage commands and read-only
  DVD/menu state for deterministic authored-disc commissioning.
- [x] Prepare and publish the source repository under the `opensagetv-vibe`
  organization with Vibe documentation, security, CI, and reproducibility
  contracts.
- [ ] Publish a versioned GitHub release or SageTV plugin-catalog entry only
  after separate explicit approval.
