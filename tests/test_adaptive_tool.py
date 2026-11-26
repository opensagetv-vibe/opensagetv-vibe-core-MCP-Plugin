import ast
from pathlib import Path
from unittest.mock import Mock
import re
import unittest


class AdaptiveToolTests(unittest.TestCase):
    def test_scope_validation_and_server_routing(self):
        source = Path(__file__).resolve().parents[1] / 'mcp/src/opensagetv_vibe_core_mcp/server.py'
        node = next(n for n in ast.parse(source.read_text()).body
                    if isinstance(n, ast.FunctionDef) and n.name == 'sage_adaptive_bitrate_test')
        node.decorator_list = []
        call = Mock(return_value={'sessions': '[]'})
        scope = {'_call': call, 'Any': object, 're': re}
        exec(compile(ast.Module(body=[node], type_ignores=[]), str(source), 'exec'), scope)
        tool = scope[node.name]
        tool(server='vibe')
        self.assertEqual(call.call_args.args, ('plugin.adaptive_test', 'vibe'))
        self.assertEqual(call.call_args.kwargs['plugin_id'], 'sagetvffmpegpluginlinux')
        handle = '0123456789abcdef0123456789abcdef'
        tool('override', handle, 1, 2000, 120, True, 'windows', 'test')
        self.assertEqual(call.call_args.kwargs['plugin_id'], 'sagetvffmpegpluginwinx64')
        for invalid in ({'operation': 'shell'}, {'platform': 'other'}, {'confirm': 1},
                        {'operation': 'override', 'session_id': handle},
                        {'operation': 'override', 'session_id': handle, 'confirm': True,
                         'target_kbps': 2000, 'lease_seconds': 301}):
            with self.assertRaises(ValueError):
                tool(**invalid)
        tool('clear', handle, 2, confirm=True)
        self.assertEqual(call.call_args.kwargs['target_kbps'], 0)
        self.assertEqual(call.call_args.kwargs['lease_seconds'], 0)
        call.side_effect = RuntimeError('unsupported')
        with self.assertRaisesRegex(RuntimeError, 'unsupported'):
            tool()
