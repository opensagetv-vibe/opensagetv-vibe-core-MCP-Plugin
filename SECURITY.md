# Security

Report security problems privately to the project maintainer before public disclosure.

The control bridge can change active playback and inspect SageTV state. Keep it bound to loopback unless LAN access is required, use a unique generated token, place TLS at a trusted reverse proxy for untrusted networks, and never commit real tokens or server credentials.

The project intentionally rejects arbitrary Sage API methods, arbitrary filesystem playback, Java reflection, class loading, and shell execution.

