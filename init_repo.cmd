@echo off
echo ========================================================
echo   Mercedes C180 (W205) OBD-II App - Git Setup Helper
echo ========================================================
echo.

cd /d "%~dp0"

if not exist ".git" (
    echo [1/3] Initializing local git repository...
    git init
    git branch -M main
) else (
    echo Git repository already initialized.
)

echo.
echo [2/3] Adding files to git...
git add .
git commit -m "feat: Mercedes C180 W205 ELM327 OBD-II Android App with GitHub Actions CI"

echo.
echo ========================================================
echo [3/3] Ready to push to GitHub!
echo.
echo Next steps:
echo 1. Go to https://github.com/new and create a new repository (e.g. MercedesW205_OBD2)
echo 2. Copy the repository URL (e.g. https://github.com/username/MercedesW205_OBD2.git)
echo 3. Run the following two commands:
echo.
echo    git remote add origin YOUR_REPOSITORY_URL
echo    git push -u origin main
echo.
echo Once pushed, GitHub Actions will compile the APK automatically!
echo ========================================================
pause
