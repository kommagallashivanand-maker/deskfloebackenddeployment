@echo off
setlocal enabledelayedexpansion

echo ===================================================
echo DeskFlow AI: Rebuilding virtual environment
echo ===================================================

:: Check if Python is installed
where python >nul 2>nul
if %errorlevel% neq 0 (
    echo [ERROR] Python was not found in your PATH.
    echo Please install Python ^(3.10+ recommended^) and add it to your system PATH.
    pause
    exit /b 1
)

:: Get Python version
for /f "tokens=2" %%i in ('python --version 2^>^&1') do set PY_VER=%%i
echo Found Python version: !PY_VER!

:: If .venv directory exists, delete it to ensure a clean build
if exist .venv (
    echo [INFO] Deleting existing .venv folder for a clean build...
    rmdir /s /q .venv
    if %errorlevel% neq 0 (
        echo [ERROR] Could not delete .venv. Ensure no processes are using it.
        pause
        exit /b 1
    )
)

echo [INFO] Creating new virtual environment in .venv...
python -m venv .venv
if %errorlevel% neq 0 (
    echo [ERROR] Failed to create virtual environment.
    pause
    exit /b 1
)

echo [INFO] Upgrading pip...
.venv\Scripts\python -m pip install --upgrade pip

echo [INFO] Installing requirements from requirements.txt...
echo This will install fastapi, uvicorn, pydantic, spacy, yake, and the spaCy en_core_web_sm model...
.venv\Scripts\python -m pip install -r requirements.txt
if %errorlevel% neq 0 (
    echo [ERROR] Failed to install requirements.
    pause
    exit /b 1
)

echo.
echo ===================================================
echo [SUCCESS] Virtual environment rebuilt successfully!
echo To activate the virtual environment, run:
echo    .venv\Scripts\activate
echo.
echo To run the test suite, run:
echo    .venv\Scripts\pytest tests/ -v
echo ===================================================
pause
