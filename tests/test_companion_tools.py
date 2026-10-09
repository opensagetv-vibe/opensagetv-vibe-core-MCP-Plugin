"""Focused adapter guards without launching MCP or mutating a real server."""
from __future__ import annotations

import ast
from pathlib import Path
import re
from unittest.mock import Mock
import unittest


class CompanionToolTests(unittest.TestCase):
    def tool(self, name, caller):
        source = Path(__file__).resolve().parents[1] / "mcp/src/opensagetv_vibe_core_mcp/server.py"
        tree = ast.parse(source.read_text(encoding="utf-8"))
        matches = [node for node in tree.body if isinstance(node, ast.FunctionDef) and node.name == name]
        self.assertEqual(len(matches), 1)
        function = matches[0]
        self.assertEqual([ast.unparse(node) for node in function.decorator_list], ["mcp.tool()"])
        function.decorator_list = []
        namespace = {"_call": caller, "Any": object, "re": re}
        exec(compile(ast.Module(body=[function], type_ignores=[]), str(source), "exec"), namespace)
        return namespace[name]

    def test_available_is_read_only_and_refresh_is_explicit(self):
        caller = Mock(return_value={"available": True, "availableVersion": "0.1.0"})
        tool = self.tool("sage_companion_available", caller)
        tool(server="windows")
        caller.assert_called_once_with("companion.available", "windows", refresh=False)
        caller.reset_mock()
        tool(refresh=True, server="stock")
        caller.assert_called_once_with("companion.available", "stock", refresh=True)
        for invalid in ("true", 1, None):
            caller.reset_mock()
            with self.assertRaises(ValueError):
                tool(refresh=invalid)
            caller.assert_not_called()

    def test_status_accepts_no_plugin_id_or_install_parameter(self):
        caller = Mock(return_value={"installed": False})
        tool = self.tool("sage_companion_status", caller)
        self.assertEqual(tool("stock"), {"installed": False})
        caller.assert_called_once_with("companion.status", "stock")
        with self.assertRaises(TypeError):
            tool(plugin_id="another-plugin")

    def test_install_requires_literal_confirmation_and_numeric_version(self):
        caller = Mock()
        tool = self.tool("sage_companion_install", caller)
        for confirm in (False, "true", 1, None):
            with self.assertRaises(ValueError):
                tool("0.1.0", confirm=confirm)
        for version in ("v0.1.0", "0.1.0-dev", "01.1.0", "0.1", "0.1.0\n", "https://example.invalid/plugin.zip", 1, None):
            with self.assertRaises(ValueError):
                tool(version, confirm=True)
        caller.assert_not_called()

    def test_install_routes_exact_version_without_urls_or_paths(self):
        caller = Mock(return_value={"installResult": "RESTART", "restarted": False})
        tool = self.tool("sage_companion_install", caller)
        result = tool("0.1.0", confirm=True, server="windows")
        caller.assert_called_once_with("companion.install", "windows", expected_version="0.1.0", confirm=True)
        self.assertEqual(result, {"installResult": "RESTART", "restarted": False})
        with self.assertRaises(TypeError):
            tool("0.1.0", confirm=True, path="arbitrary.zip")

    def test_bridge_failures_propagate_without_install_success_fallback(self):
        for name, arguments in (("sage_companion_available", {}), ("sage_companion_status", {}),
                                ("sage_companion_install", {"expected_version": "0.1.0", "confirm": True})):
            with self.subTest(name=name):
                caller = Mock(side_effect=RuntimeError("recording status unavailable"))
                with self.assertRaisesRegex(RuntimeError, "status unavailable"):
                    self.tool(name, caller)(server="stock", **arguments)
                caller.assert_called_once()


if __name__ == "__main__":
    unittest.main()
