#!/usr/bin/env python3
from __future__ import annotations

from pathlib import Path
import stat
import zipfile


ROOT = Path(__file__).resolve().parents[1]
OUTPUT = ROOT / "output" / "opensagetv-vibe-core-MCP-Plugin-handoff.zip"
EXCLUDED_PARTS = {".git", ".deps", ".venv", "artifacts", "build", "dist", "output", "__pycache__"}


def included(path: Path) -> bool:
    relative = path.relative_to(ROOT)
    if any(part in EXCLUDED_PARTS for part in relative.parts):
        return False
    if path.suffix.lower() in {".pyc", ".pyo"}:
        return False
    if relative.parts and relative.parts[0] == "config":
        return relative.as_posix() == "config/core-mcp.example.toml"
    return True


def main() -> int:
    files = sorted(path for path in ROOT.rglob("*") if path.is_file() and included(path))
    OUTPUT.parent.mkdir(parents=True, exist_ok=True)
    temporary = OUTPUT.with_suffix(".zip.tmp")
    with zipfile.ZipFile(temporary, "w", zipfile.ZIP_DEFLATED) as archive:
        for path in files:
            name = f"opensagetv-vibe-core-MCP-Plugin/{path.relative_to(ROOT).as_posix()}"
            data = path.read_bytes()
            executable = path.suffix.lower() in {".py", ".sh"} or data.startswith(b"#!")
            info = zipfile.ZipInfo(name, (2026, 9, 20, 16, 0, 0))
            info.create_system = 3
            info.external_attr = (stat.S_IFREG | (0o755 if executable else 0o644)) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data)
    temporary.replace(OUTPUT)
    print(f"Created {OUTPUT} with {len(files)} source files (local secrets excluded)")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
