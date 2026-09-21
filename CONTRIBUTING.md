# Contributing

Changes must preserve stock SageTV compatibility and the security boundary described in `README.md` and `AGENTS.md`.

New testing, commissioning, diagnostic, or automation controls must first use
or extend this stock-compatible bridge through supported `sage.SageTV.api` or
`apiUI` calls. A Core change is a last resort and requires a documented API gap,
an optional negotiated contract, a safe stock fallback, and regression evidence
for older clients.

Run `dev.cmd all` before submitting changes. Add a focused test for each new
action, response field, authentication rule, or failure mode. New generic API
passthrough and arbitrary command execution are not accepted. Never commit a
real bridge token, SageTV credential, private test address, or generated build
output.
