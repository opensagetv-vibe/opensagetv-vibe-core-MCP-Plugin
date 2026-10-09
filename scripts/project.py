#!/usr/bin/env python3
from __future__ import annotations

import datetime as dt
import hashlib
import os
from pathlib import Path
import re
import shutil
import stat
import subprocess
import sys
import unittest
import zipfile


ROOT = Path(__file__).resolve().parents[1]
BUILD = ROOT / "build"
CLASSES = BUILD / "classes"
TEST_CLASSES = BUILD / "test-classes"
DIST = ROOT / "dist"
OUTPUT = ROOT / "output"


def run(args: list[str], **kwargs) -> None:
    print("+", " ".join(map(str, args)), flush=True)
    subprocess.run(args, check=True, **kwargs)


def version() -> str:
    for line in (ROOT / "release.properties").read_text(encoding="utf-8").splitlines():
        if line.startswith("VERSION="):
            return line.split("=", 1)[1].strip()
    raise RuntimeError("VERSION missing from release.properties")


def release_properties() -> dict[str, str]:
    values: dict[str, str] = {}
    for line in (ROOT / "release.properties").read_text(encoding="utf-8").splitlines():
        stripped = line.strip()
        if not stripped or stripped.startswith("#"):
            continue
        key, separator, value = stripped.partition("=")
        if not separator:
            raise RuntimeError(f"Invalid release.properties line: {line}")
        values[key.strip()] = value.strip()
    return values


def stock_jar() -> Path:
    explicit = os.environ.get("SAGETV_JAR", "").strip()
    candidate = Path(explicit) if explicit else ROOT / ".deps" / "stock" / "Sage.jar"
    candidate = candidate.resolve()
    if not candidate.is_file():
        raise RuntimeError("Unmodified stock Sage.jar is required through SAGETV_JAR or .deps/stock/Sage.jar")
    return candidate


def source_epoch() -> int:
    value = os.environ.get("SOURCE_DATE_EPOCH", "").strip()
    if value:
        return int(value)
    return 1789920000  # 2026-09-20T16:00:00Z, stable local pre-release epoch


def zip_time(epoch: int) -> tuple[int, int, int, int, int, int]:
    value = dt.datetime.fromtimestamp(max(epoch, 315532800), tz=dt.timezone.utc)
    return value.year, value.month, value.day, value.hour, value.minute, value.second - value.second % 2


def write_zip(path: Path, entries: list[tuple[str, bytes, int]], epoch: int) -> None:
    path.parent.mkdir(parents=True, exist_ok=True)
    temporary = path.with_suffix(path.suffix + ".tmp")
    with zipfile.ZipFile(temporary, "w") as archive:
        for name, data, mode in sorted(entries):
            info = zipfile.ZipInfo(name, zip_time(epoch))
            info.create_system = 3
            info.external_attr = (stat.S_IFREG | mode) << 16
            info.compress_type = zipfile.ZIP_DEFLATED
            archive.writestr(info, data)
    os.replace(temporary, path)


def build() -> None:
    jar = stock_jar()
    shutil.rmtree(BUILD, ignore_errors=True)
    CLASSES.mkdir(parents=True)
    sources = sorted(str(path) for path in (ROOT / "src" / "main" / "java").rglob("*.java"))
    run(["javac", "--release", "8", "-cp", str(jar), "-d", str(CLASSES), *sources])
    entries = [(path.relative_to(CLASSES).as_posix(), path.read_bytes(), 0o644)
               for path in CLASSES.rglob("*.class")]
    entries.append(("META-INF/MANIFEST.MF", b"Manifest-Version: 1.0\r\n\r\n", 0o644))
    DIST.mkdir(parents=True, exist_ok=True)
    write_zip(DIST / "OpenSageTVVibeCoreMCPPlugin.jar", entries, source_epoch())
    with zipfile.ZipFile(DIST / "OpenSageTVVibeCoreMCPPlugin.jar") as archive:
        leaked = [name for name in archive.namelist() if name.startswith("sage/")]
        if leaked:
            raise RuntimeError(f"Stock SageTV classes leaked into plugin JAR: {leaked[:5]}")


def test() -> None:
    build()
    jar = stock_jar()
    TEST_CLASSES.mkdir(parents=True, exist_ok=True)
    tests = sorted(str(path) for path in (ROOT / "src" / "test" / "java").rglob("*.java"))
    run(["javac", "--release", "8", "-cp", os.pathsep.join((str(jar), str(CLASSES))),
         "-d", str(TEST_CLASSES), *tests])
    run(["java", "--add-modules", "jdk.httpserver", "-cp",
         os.pathsep.join((str(jar), str(CLASSES), str(TEST_CLASSES))),
         "org.opensagetv.vibe.coremcp.ContractTest"])
    run(["java", "-cp", os.pathsep.join((str(jar), str(CLASSES), str(TEST_CLASSES))),
         "org.opensagetv.vibe.coremcp.PluginCaptionSettingsTest"])
    run(["java", "-cp", os.pathsep.join((str(jar), str(CLASSES), str(TEST_CLASSES))),
         "org.opensagetv.vibe.coremcp.CompanionSettingsTest"])
    run(["java", "-cp", os.pathsep.join((str(jar), str(CLASSES), str(TEST_CLASSES))),
         "org.opensagetv.vibe.coremcp.ExactPathLookupTest"])
    run(["java", "-cp", os.pathsep.join((str(jar), str(CLASSES), str(TEST_CLASSES))),
         "org.opensagetv.vibe.coremcp.CompanionInstallerTest"])
    suite = unittest.defaultTestLoader.discover(str(ROOT / "tests"), pattern="test_*.py")
    result = unittest.TextTestRunner(verbosity=2).run(suite)
    if not result.wasSuccessful():
        raise RuntimeError("Python tests failed")


def validate() -> None:
    required_public_files = {
        ".github/workflows/repository-checks.yml",
        ".gitignore",
        "AGENTS.md",
        "CHANGELOG.md",
        "CONTRIBUTING.md",
        "HANDOFF.md",
        "LICENSE",
        "README.md",
        "SECURITY.md",
        "TASKS.md",
        "THIRD_PARTY_NOTICES.md",
        "WORKFLOW.md",
        "config/core-mcp.example.toml",
        "release.properties",
    }
    missing = sorted(name for name in required_public_files if not (ROOT / name).is_file())
    if missing:
        raise RuntimeError(f"Missing required public files: {missing}")

    properties = release_properties()
    expected_properties = {
        "VERSION": "0.1.5",
        "PACKAGE_ID": "opensagetv-vibe-core-MCP-Plugin",
        "REQUIRES_BUILD": "true",
        "PUBLISH_APPROVED": "true",
    }
    for key, expected in expected_properties.items():
        if properties.get(key) != expected:
            raise RuntimeError(f"release.properties {key} must be {expected!r}")

    release = properties["VERSION"]
    java_source = (ROOT / "src/main/java/org/opensagetv/vibe/coremcp/SageTVCoreMcpPlugin.java").read_text(encoding="utf-8")
    if f'VERSION = "{release}"' not in java_source:
        raise RuntimeError("Java plugin version does not match release.properties")
    pyproject = (ROOT / "mcp/pyproject.toml").read_text(encoding="utf-8")
    if not re.search(rf'^version\s*=\s*"{re.escape(release)}"\s*$', pyproject, re.MULTILINE):
        raise RuntimeError("MCP adapter version does not match release.properties")

    if (ROOT / ".git").exists():
        tracked_config = subprocess.run(
            ["git", "ls-files", "--error-unmatch", "config/core-mcp.toml"],
            cwd=ROOT,
            stdout=subprocess.DEVNULL,
            stderr=subprocess.DEVNULL,
            check=False,
        )
        if tracked_config.returncode == 0:
            raise RuntimeError("Local config/core-mcp.toml must never be tracked")
    example = (ROOT / "config/core-mcp.example.toml").read_text(encoding="utf-8")
    private_markers = ("192.168.", "10.0.", "172.16.", "password =", "token = \"vibe-")
    for marker in private_markers:
        if marker in example:
            raise RuntimeError(f"Unsafe example configuration marker found: {marker}")

    forbidden = {
        "Runtime.getRuntime(": "shell execution",
        "new ProcessBuilder(": "process execution",
        "Class.forName(": "dynamic class loading",
        "getDeclaredMethod(": "private reflection",
        "getDeclaredField(": "private reflection",
    }
    java_text = "\n".join(path.read_text(encoding="utf-8")
                           for path in (ROOT / "src" / "main" / "java").rglob("*.java"))
    for marker, reason in forbidden.items():
        if marker in java_text:
            raise RuntimeError(f"Forbidden {reason} marker found: {marker}")
    tracked = list(ROOT.rglob("*"))
    for path in tracked:
        if path.is_file() and any(part in {"build", "dist", "output", "artifacts", ".git"} for part in path.parts):
            continue
        if path.is_file() and path.suffix.lower() in {".md", ".java", ".py", ".toml", ".xml", ".sh"}:
            text = path.read_text(encoding="utf-8")
            if "\r" in text:
                raise RuntimeError(f"Non-LF source file: {path}")
    run([sys.executable, "-m", "compileall", "-q", str(ROOT / "mcp" / "src")])
    run([sys.executable, "-m", "py_compile", str(ROOT / "scripts" / "create_ai_handoff_zip.py")])
    print("PASS source validation")


def package() -> None:
    build()
    OUTPUT.mkdir(parents=True, exist_ok=True)
    package_dir = OUTPUT / "packages"
    shutil.rmtree(package_dir, ignore_errors=True)
    package_dir.mkdir(parents=True)
    release = version()
    jar = DIST / "OpenSageTVVibeCoreMCPPlugin.jar"
    jar_zip = package_dir / f"OpenSageTVVibeCoreMCPPlugin-jar-{release}.zip"
    write_zip(jar_zip, [(jar.name, jar.read_bytes(), 0o644)], source_epoch())
    digest = hashlib.md5(jar_zip.read_bytes()).hexdigest()
    date = dt.datetime.fromtimestamp(source_epoch(), tz=dt.timezone.utc).strftime("%Y.%m.%d")
    template = (ROOT / "manifests" / "OpenSageTVVibeCoreMCPPlugin.xml.template").read_text(encoding="utf-8")
    rendered = template.replace("@DATE@", date).replace("@VERSION@", release).replace("@JAR_ZIP_MD5@", digest)
    (package_dir / "OpenSageTVVibeCoreMCPPlugin.xml").write_text(rendered, encoding="utf-8", newline="\n")
    dev = rendered.replace(
        f"https://github.com/opensagetv-vibe/opensagetv-vibe-core-MCP-Plugin/releases/download/v{release}/{jar_zip.name}",
        f"file:/opt/sagetv/server/SageTVPluginsDev.d/{jar_zip.name}",
    )
    (package_dir / "OpenSageTVVibeCoreMCPPlugin-dev.xml").write_text(dev, encoding="utf-8", newline="\n")
    files = sorted(path for path in package_dir.iterdir() if path.is_file())
    sums = "".join(f"{hashlib.sha256(path.read_bytes()).hexdigest()}  {path.name}\n" for path in files)
    (package_dir / "SHA256SUMS").write_text(sums, encoding="utf-8", newline="\n")
    report = [
        "# OpenSageTV Vibe Core MCP Plugin build report", "",
        f"- Version: {release}",
        "- Stock Sage.jar compile contract: PASS",
        "- Bundled SageTV classes: none",
        "- Core modification required: no", "", "## Artifacts", "",
    ]
    report.extend(f"- `{path.name}`: `{hashlib.sha256(path.read_bytes()).hexdigest()}`" for path in files)
    (OUTPUT / "BUILD_REPORT.md").write_text("\n".join(report) + "\n", encoding="utf-8", newline="\n")
    print(f"Packaged {jar_zip.name} md5={digest}")


def main() -> int:
    command = sys.argv[1] if len(sys.argv) > 1 else "all"
    if command == "build": build()
    elif command == "test": test()
    elif command == "validate": validate()
    elif command == "package": package()
    elif command == "all":
        validate()
        test()
        package()
    else:
        raise RuntimeError(f"Unknown command: {command}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
