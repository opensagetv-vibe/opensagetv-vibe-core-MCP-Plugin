# Security

Report security problems through the repository's private GitHub security
advisory form:

`https://github.com/opensagetv-vibe/opensagetv-vibe-core-MCP-Plugin/security/advisories/new`

Do not put tokens, credentials, private addresses, diagnostic bundles, or
exploit details in a public issue.

The control bridge can change active playback and inspect SageTV state. Keep it bound to loopback unless LAN access is required, use a unique generated token, place TLS at a trusted reverse proxy for untrusted networks, and never commit real tokens or server credentials.

The project intentionally rejects arbitrary Sage API methods, arbitrary filesystem playback, Java reflection, class loading, and shell execution.

Authenticated0.1.5 commissioning can install only the fixed Vibe Client
Extension from SageTV's configured plugin repository. This is a privileged
plugin installation, not a read-only operation: require explicit confirmation,
an exact numeric version and trusted repository/package metadata. The bridge
does not accept caller-supplied URLs, paths, identifiers or class names.
Keep the development catalog under the existing administrator-controlled
deployment workflow; do not expose its writable directory to untrusted users.
Independent recording/UI/client checks fail closed, and a pending `RESTART`
never grants permission to restart or stop recordings. Availability/status
tools are read-only; only an explicit refresh contacts the repository.
