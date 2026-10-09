# Workflow

## Task fix and server-boundary policy

Apply this policy to every task workflow, including dependency fixes across
Vibe repositories. Fix and test necessary plugins and update the test-server
plugin without asking again solely for repository-boundary approval.

Prefer the Android client, then a stock-compatible plugin. Change non-stock
`.232` Core only for a proven production defect that neither can correct;
document the API gap and alternatives, keep optional negotiation and safe
stock/older-client fallback, and run affected compatibility tests. Never patch
Core merely to simplify testing.

Stock `.175` installation changes are limited to plugin installation/update.
Do not modify its stock Sage.jar, stock FFmpeg, Core binaries, or server
installation/configuration files. Preserve user settings, recordings and
unrelated clients; reversible supported SageTV playback APIs remain allowed.

Non-stock `.232` restarts are authorized for task updates without asking
again; coordinate them with active test guards and preserve data/settings.
  Always ask the user before restarting stock `.175`, even when it appears idle,
  unless an explicit user-granted bounded restart window is active. Record
  its UTC expiry in the task/handoff, and check expiry and revocation before
  every restart. After expiry or revocation, ask again; stock files stay protected.

Update owning TASKS.md, linked dependencies and the workspace suggested order
as work changes; move completed checkoffs into the checklist change ledger.
Test only affected gates, preserve unrelated completed matrices, and do not
stop independent authorized work for a status question or a dependency-only
permission request. Unrelated work, publication, destructive actions and
interruption of recordings/other users still require their own authority.

Use the location-independent project entry points:

```text
dev.cmd test
dev.cmd validate
dev.cmd build
dev.cmd install
dev.cmd package
dev.cmd all
dev.cmd mcp
```

The standard `test`, `validate`, `build`, `install`, and `all` commands delegate
to the sibling unified build environment. `install` is intentionally an
artifact-only no-op; physical server commissioning remains explicit. Update and
handoff packages use `update.cmd` and `create_ai_handoff_zip.cmd` like the other
OpenSageTV Vibe repositories.

Release compilation requires an unmodified stock `Sage.jar` through `SAGETV_JAR` or `.deps/stock/Sage.jar`.

Physical installation is a separate commissioning operation. A build or package command must never modify a SageTV server.

Before replacement/restart, verify zero recordings and zero connected clients,
not just a restart permission window. Prefer server.activity on the installed
bridge. If that action is absent, document the gap and use only the existing
exact read-only recording API alongside bridge UI discovery; bridge health or
unknown activity failures must abort. Recheck before the final replacement.

Local server addresses, tokens, and credentials belong only in ignored TOML configuration. The tracked example must contain documentation-safe placeholders.
