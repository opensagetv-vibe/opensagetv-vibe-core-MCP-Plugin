# Workflow

Use the location-independent project entry points:

```text
dev.cmd test
dev.cmd validate
dev.cmd build
dev.cmd package
dev.cmd all
dev.cmd mcp
```

Release compilation requires an unmodified stock `Sage.jar` through `SAGETV_JAR` or `.deps/stock/Sage.jar`.

Physical installation is a separate commissioning operation. A build or package command must never modify a SageTV server.

Local server addresses, tokens, and credentials belong only in ignored TOML configuration. The tracked example must contain documentation-safe placeholders.

