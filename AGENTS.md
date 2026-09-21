# Agent instructions

Read `HANDOFF.md`, `README.md`, `TASKS.md`, and `WORKFLOW.md` before changing this project.

- The Java bridge must compile and run against an unmodified stock SageTV `Sage.jar`.
- Use supported `sage.SageTV.api` and `sage.SageTV.apiUI` calls only. Do not use reflection into Core internals.
- Never expose arbitrary Sage expressions, Java reflection, class loading, shell execution, or unrestricted filesystem access.
- The HTTP bridge is disabled unless the Standard plugin is enabled, binds to loopback by default, and requires a bearer token for control calls.
- The external MCP adapter owns MCP protocol handling; the SageTV JVM owns only the bounded control bridge.
- Do not publish a repository, release, or SageTV plugin-catalog entry without explicit user approval.


## Stock-server test-control policy

- For any new testing, commissioning, diagnostic, or automation control, first
  implement or extend the stock-compatible `opensagetv-vibe-core-MCP-Plugin`
  using supported `sage.SageTV.api`/`apiUI` calls and verify it against an
  unmodified stock SageTV server.
- Do not patch `Sage.jar`, add private MiniClient events, or change Core merely
  to make a test easier. Existing public APIs, the bounded MCP bridge, and
  external test tooling are the required first option.
- Change Core only when the required production runtime behavior cannot be
  expressed through the stock plugin/API boundary. Document the proven API
  gap, keep the extension optional and negotiated with a safe stock fallback,
  and verify older clients and installations remain unaffected.

