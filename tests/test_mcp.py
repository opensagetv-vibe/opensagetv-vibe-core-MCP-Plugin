from __future__ import annotations

from http.server import BaseHTTPRequestHandler, ThreadingHTTPServer
import json
import sys
import tempfile
import threading
from pathlib import Path
import unittest


ROOT = Path(__file__).resolve().parents[1]
sys.path.insert(0, str(ROOT / "mcp" / "src"))

from opensagetv_vibe_core_mcp.client import BridgeClient, BridgeError
from opensagetv_vibe_core_mcp.config import ServerConfig, load_config


class Handler(BaseHTTPRequestHandler):
    token = "test-token-abcdefghijklmnopqrstuvwxyz"

    def log_message(self, format, *args):
        return

    def do_GET(self):
        if self.path != "/health":
            self.send_error(404)
            return
        self.reply(200, {"status": "ok", "capabilityVersion": 1})

    def do_POST(self):
        length = int(self.headers.get("Content-Length", "0"))
        body = self.rfile.read(length).decode("utf-8")
        if self.headers.get("Authorization") != "Bearer " + self.token:
            self.reply(401, {"ok": False, "error": "authentication_required"})
            return
        self.reply(200, {"ok": True, "body": body})

    def reply(self, status, payload):
        body = json.dumps(payload).encode("utf-8")
        self.send_response(status)
        self.send_header("Content-Type", "application/json")
        self.send_header("Content-Length", str(len(body)))
        self.end_headers()
        self.wfile.write(body)


class McpClientTests(unittest.TestCase):
    @classmethod
    def setUpClass(cls):
        cls.httpd = ThreadingHTTPServer(("127.0.0.1", 0), Handler)
        cls.thread = threading.Thread(target=cls.httpd.serve_forever, daemon=True)
        cls.thread.start()
        cls.base = f"http://127.0.0.1:{cls.httpd.server_address[1]}"

    @classmethod
    def tearDownClass(cls):
        cls.httpd.shutdown()
        cls.thread.join(timeout=2)

    def test_health_and_authenticated_call(self):
        client = BridgeClient(ServerConfig("test", self.base, Handler.token))
        self.assertEqual(client.health()["status"], "ok")
        response = client.call("ui.list", context="DEV001")
        self.assertIn("action=ui.list", response["body"])
        self.assertIn("context=DEV001", response["body"])

    def test_authentication_failure_is_not_hidden(self):
        client = BridgeClient(ServerConfig("test", self.base, "wrong-token-value-abcdefghijklmnopqrstuvwxyz"))
        with self.assertRaisesRegex(BridgeError, "authentication_required"):
            client.call("capabilities")

    def test_toml_multiple_servers(self):
        with tempfile.TemporaryDirectory() as temporary:
            path = Path(temporary) / "core-mcp.toml"
            path.write_text(
                'schema=1\nactive_server="one"\n'
                '[servers.one]\nalias="Stock"\nbase_url="http://one:8270"\ntoken="abc"\n'
                '[servers.two]\nalias="Vibe"\nbase_url="http://two:8270"\ntoken="def"\n',
                encoding="utf-8",
            )
            active, servers = load_config(path)
            self.assertEqual(active, "one")
            self.assertEqual(set(servers), {"one", "two"})


if __name__ == "__main__":
    unittest.main()
