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

## Fresh Client Extension commissioning

1. Use `sage_companion_available(refresh=True)` only when a repository refresh
   is explicitly wanted; subsequent metadata reads should use its default
   false. Stage any approved development catalog through the existing guarded
   deployment workflow, not an MCP-supplied arbitrary file or URL.
2. Inspect `sage_companion_status` and checkpoint current plugin/UI settings.
   Check `sage_server_activity` and preserve recordings and other clients.
3. Call `sage_companion_install` only with the exact available numeric version
   and `confirm=True`. Server-side metadata/compatibility and independent
   recording/UI/client guards fail closed. Never edit the plugin registry as
   an alternative to the supported installer.
4. Treat `RESTART` as a successfully staged package requiring a separately
   authorized, freshly idle restart. The installer never restarts SageTV or
   enables pending classes. Verify post-restart installed version, enabled
   state, health and the expected runtime JAR before physical DVD tests.
5. Qualify only affected stock Windows/Linux, configuration, fallback and
   restoration gates. Normal client playback must work without this MCP
   commissioning dependency. Record compact results and retire completed raw
   staging/captures using the workspace artifact workflow.

For0.1.5 release validation, include ExactPathLookupTest,
CompanionInstallerTest, existing security/config/restoration contracts and
Python selected-server/confirmation/version/error tests. Source tests model
failure paths; actual server installs/playback and public-release verification
must be recorded separately. Do not repeat unrelated device/player matrices.
