# Security

Report security problems through the repository's private GitHub security
advisory form:

`https://github.com/opensagetv-vibe/opensagetv-vibe-core-MCP-Plugin/security/advisories/new`

Do not put tokens, credentials, private addresses, diagnostic bundles, or
exploit details in a public issue.

The control bridge can change active playback and inspect SageTV state. Keep it bound to loopback unless LAN access is required, use a unique generated token, place TLS at a trusted reverse proxy for untrusted networks, and never commit real tokens or server credentials.

The project intentionally rejects arbitrary Sage API methods, arbitrary filesystem playback, Java reflection, class loading, and shell execution.
