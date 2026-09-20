from __future__ import annotations

import json
import ssl
from typing import Any
from urllib.error import HTTPError, URLError
from urllib.parse import urlencode
from urllib.request import Request, urlopen

from .config import ServerConfig


class BridgeError(RuntimeError):
    pass


class BridgeClient:
    def __init__(self, config: ServerConfig):
        self.config = config

    def call(self, action: str, **parameters: Any) -> dict[str, Any]:
        values = {"action": action}
        values.update({key: str(value).lower() if isinstance(value, bool) else str(value)
                       for key, value in parameters.items() if value is not None})
        body = urlencode(values).encode("utf-8")
        request = Request(
            self.config.base_url + "/v1/control",
            data=body,
            headers={
                "Accept": "application/json",
                "Authorization": "Bearer " + self.config.token,
                "Content-Type": "application/x-www-form-urlencoded; charset=utf-8",
            },
            method="POST",
        )
        context = None
        if request.full_url.startswith("https://") and not self.config.verify_tls:
            context = ssl._create_unverified_context()
        try:
            with urlopen(request, timeout=self.config.timeout_seconds, context=context) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except HTTPError as exc:
            detail = exc.read().decode("utf-8", errors="replace")
            raise BridgeError(f"HTTP {exc.code}: {detail[:1000]}") from exc
        except (URLError, TimeoutError, OSError, json.JSONDecodeError) as exc:
            raise BridgeError(str(exc)) from exc
        if not isinstance(payload, dict):
            raise BridgeError(f"Invalid bridge response: {payload!r}")
        if not payload.get("ok", False):
            raise BridgeError(str(payload.get("error", payload)))
        return payload

    def health(self) -> dict[str, Any]:
        request = Request(self.config.base_url + "/health", headers={"Accept": "application/json"})
        try:
            with urlopen(request, timeout=self.config.timeout_seconds) as response:
                payload = json.loads(response.read().decode("utf-8"))
        except Exception as exc:
            raise BridgeError(str(exc)) from exc
        if not isinstance(payload, dict) or payload.get("status") != "ok":
            raise BridgeError(f"Unhealthy bridge response: {payload!r}")
        return payload

