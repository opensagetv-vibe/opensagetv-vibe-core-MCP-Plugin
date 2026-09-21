# Workflow

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

Local server addresses, tokens, and credentials belong only in ignored TOML configuration. The tracked example must contain documentation-safe placeholders.
