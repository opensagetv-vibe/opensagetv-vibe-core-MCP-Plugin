# Third-party notices

The project compiles against the
[SageTV](https://github.com/google/sagetv) API under the Apache License 2.0.

The external Python adapter declares these runtime dependencies without
bundling their source or wheels:

- [`mcp[cli]` 2.1.1](https://github.com/modelcontextprotocol/python-sdk), MIT License.
- [`tomli` 2.4.1](https://github.com/hukkin/tomli), MIT License, used only on
  Python versions earlier than 3.11.

No SageTV classes, MCP package code, or third-party binary dependencies are bundled into the Java plugin JAR.
