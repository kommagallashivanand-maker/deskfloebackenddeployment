@echo off
REM Setup script for Model Quality Dashboard
REM Deletes and recreates virtual environment, then installs dependencies

echo Setting up Model Quality Dashboard...
echo.

REM Remove existing virtual environment if it exists
if exist .venv (
    echo Removing existing virtual environment...
    rmdir /s /q .venv
)

REM Create new virtual environment
echo Creating virtual environment...
py -3.11 -m venv .venv

REM Activate virtual environment and install dependencies
echo Installing dependencies...
call .venv\Scripts\activate.bat
python -m pip install --upgrade pip
pip install -r requirements.txt

echo.
echo Setup complete!
echo.
echo To run the dashboard:
echo   1. Activate the virtual environment: .venv\Scripts\activate.bat
echo   2. Run the app: streamlit run app.py
echo.
pause
