import ast
from pathlib import Path
from unittest.mock import Mock
import unittest

class BitrateToolTests(unittest.TestCase):
    def test_fixed_action_confirm_selected_server_and_failure(self):
        source=Path(__file__).resolve().parents[1]/'mcp/src/opensagetv_vibe_core_mcp/server.py'
        function=next(n for n in ast.parse(source.read_text()).body if isinstance(n,ast.FunctionDef) and n.name=='sage_bitrate_test')
        self.assertEqual([n.arg for n in function.args.args],['start','confirm','server'])
        function.decorator_list=[];bridge=Mock();bridge.call.return_value={'status':'Not run'}
        client=Mock(return_value=('vibe',bridge));space={'_client':client,'Any':object}
        exec(compile(ast.Module(body=[function],type_ignores=[]),str(source),'exec'),space)
        self.assertEqual(space[function.name](server='vibe'),{'status':'Not run','server':'vibe'})
        bridge.call.assert_called_once_with('plugin.bitrate_test',plugin_id='sagetvffmpegpluginlinux',start=False,confirm=False)
        for args in ({'start':True},{'start':'true','confirm':True},{'start':True,'confirm':1}):
            with self.assertRaises(ValueError):space[function.name](**args)
        bridge.call.side_effect=RuntimeError('unsupported')
        with self.assertRaisesRegex(RuntimeError,'unsupported'):space[function.name](True,True)
