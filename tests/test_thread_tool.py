"""Thread observation has no caller-provided API, thread selector or mutations."""
import ast
from pathlib import Path
from unittest.mock import Mock
import unittest

class ThreadToolTests(unittest.TestCase):
    def test_fixed_action_selected_server_and_failure(self):
        source=Path(__file__).resolve().parents[1]/'mcp/src/opensagetv_vibe_core_mcp/server.py'
        function=next(n for n in ast.parse(source.read_text()).body if isinstance(n,ast.FunctionDef) and n.name=='sage_diagnostics_threads')
        self.assertEqual([n.arg for n in function.args.args],['server'])
        self.assertEqual([ast.unparse(n) for n in function.decorator_list],['mcp.tool()'])
        function.decorator_list=[];caller=Mock(return_value={'threads':[]});space={'_call':caller,'Any':object}
        exec(compile(ast.Module(body=[function],type_ignores=[]),str(source),'exec'),space)
        self.assertEqual(space[function.name]('vibe'),{'threads':[]})
        caller.assert_called_once_with('diagnostics.threads','vibe')
        caller.side_effect=RuntimeError('unavailable')
        with self.assertRaisesRegex(RuntimeError,'unavailable'):space[function.name]()
