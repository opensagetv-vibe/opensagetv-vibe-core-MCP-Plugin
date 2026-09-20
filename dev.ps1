[CmdletBinding(PositionalBinding=$false)]
param([Parameter(ValueFromRemainingArguments=$true)][string[]]$Arguments)
$ErrorActionPreference = 'Stop'
$root = Split-Path -Parent $MyInvocation.MyCommand.Path
$command = if ($Arguments.Count) { $Arguments[0] } else { 'all' }
$rest = if ($Arguments.Count -gt 1) { $Arguments[1..($Arguments.Count - 1)] } else { @() }
switch ($command) {
  'test' { & python "$root\scripts\project.py" test @rest }
  'validate' { & python "$root\scripts\project.py" validate @rest }
  'build' { & python "$root\scripts\project.py" build @rest }
  'package' { & python "$root\scripts\project.py" package @rest }
  'all' { & python "$root\scripts\project.py" all @rest }
  'mcp' {
    $env:PYTHONPATH = "$root\mcp\src" + [IO.Path]::PathSeparator + $env:PYTHONPATH
    & python -m opensagetv_vibe_core_mcp.server @rest
  }
  default { throw "Unknown command: $command" }
}
exit $LASTEXITCODE

