@echo off
setlocal
python "%~dp0scripts\create_ai_handoff_zip.py"
exit /b %ERRORLEVEL%
