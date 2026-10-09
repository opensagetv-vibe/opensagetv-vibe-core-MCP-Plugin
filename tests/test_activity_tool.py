"""Pure routing contract without starting MCP or contacting a real server."""
from __future__ import annotations

import ast
from pathlib import Path
from unittest.mock import Mock
import unittest


class ActivityToolTests(unittest.TestCase):
    def activity_tool(self, caller):
        source = Path(__file__).resolve().parents[1] / "mcp/src/opensagetv_vibe_core_mcp/server.py"
        tree = ast.parse(source.read_text(encoding="utf-8"))
        functions = [node for node in tree.body if isinstance(node, ast.FunctionDef)
                     and node.name == "sage_server_activity"]
        self.assertEqual(len(functions), 1)
        function = functions[0]
        self.assertEqual([ast.unparse(node) for node in function.decorator_list], ["mcp.tool()"])
        # Compile only this small routing function. Its real bridge transport and
        # authentication remain separately covered by test_mcp.py.
        function.decorator_list = []
        namespace = {"_call": caller, "Any": object}
        exec(compile(ast.Module(body=[function], type_ignores=[]), str(source), "exec"), namespace)
        return namespace[function.name]

    def test_read_only_activity_routes_exact_selected_server(self):
        caller = Mock(return_value={"recordingCount": 3, "safeToRestart": False})
        result = self.activity_tool(caller)("stock")
        caller.assert_called_once_with("server.activity", "stock")
        self.assertEqual(result, {"recordingCount": 3, "safeToRestart": False})

    def test_bridge_failure_is_not_reported_as_idle(self):
        caller = Mock(side_effect=RuntimeError("activity unavailable"))
        with self.assertRaisesRegex(RuntimeError, "activity unavailable"):
            self.activity_tool(caller)()
        caller.assert_called_once_with("server.activity", "")


if __name__ == "__main__":
    unittest.main()
