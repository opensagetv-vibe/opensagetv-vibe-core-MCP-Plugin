# Agent instructions

Read `HANDOFF.md`, `README.md`, `TASKS.md`, and `WORKFLOW.md` before changing this project.

- The Java bridge must compile and run against an unmodified stock SageTV `Sage.jar`.
- Use supported `sage.SageTV.api` and `sage.SageTV.apiUI` calls only. Do not use reflection into Core internals.
- Never expose arbitrary Sage expressions, Java reflection, class loading, shell execution, or unrestricted filesystem access.
- The HTTP bridge is disabled unless the Standard plugin is enabled, binds to loopback by default, and requires a bearer token for control calls.
- The external MCP adapter owns MCP protocol handling; the SageTV JVM owns only the bounded control bridge.
- Do not publish a repository, release, or SageTV plugin-catalog entry without explicit user approval.

