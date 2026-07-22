# ============================================================================
# Working Directory Security Scanner
# ============================================================================
# Scans the working directory for sensitive files before staging.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\scripts\security\scan-working-dir.ps1
# ============================================================================

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "WORKING DIRECTORY SECURITY SCAN" -ForegroundColor Cyan
Write-Host "============================================" -ForegroundColor Cyan
Write-Host ""

# Load sensitive patterns
$patternsFile = "scripts\security\sensitive-patterns.txt"
if (-not (Test-Path $patternsFile)) {
    Write-Host "[ERROR] Patterns file not found: $patternsFile" -ForegroundColor Red
    exit 1
}

$patterns = Get-Content $patternsFile | Where-Object {
    $_.Trim() -ne "" -and -not $_.StartsWith("#")
}

Write-Host "Loaded $($patterns.Count) sensitive file patterns." -ForegroundColor Cyan
Write-Host "Scanning working directory..." -ForegroundColor Cyan
Write-Host ""

# Get all files in working directory (excluding .git)
$allFiles = Get-ChildItem -Recurse -File | Where-Object {
    $_.FullName -notmatch '[\\/]\.git[\\/]'
}

$detectedFiles = @()

foreach ($file in $allFiles) {
    $relativePath = $file.FullName.Replace((Get-Location).Path + "\", "").Replace("\", "/")
    $fileName = $file.Name
    
    foreach ($pattern in $patterns) {
        $matched = $false
        
        if ($pattern -like "*.*") {
            # Extension pattern (e.g., *.pem)
            $ext = $pattern.Replace("*", "")
            if ($fileName -like "*$ext") {
                $matched = $true
            }
        } elseif ($pattern -like "*`**") {
            # Prefix wildcard (e.g., .env.*)
            $prefix = $pattern.Replace("*", "")
            if ($fileName -like "$prefix*") {
                $matched = $true
            }
        } else {
            # Exact match
            if ($fileName -eq $pattern -or $relativePath -eq $pattern) {
                $matched = $true
            }
        }
        
        if ($matched) {
            # Check if file is in .gitignore
            $isIgnored = $false
            try {
                $checkIgnore = git check-ignore $relativePath 2>$null
                if ($LASTEXITCODE -eq 0) {
                    $isIgnored = $true
                }
            } catch {}
            
            # Check if file is tracked
            $isTracked = $false
            try {
                $lsFiles = git ls-files $relativePath 2>$null
                if ($lsFiles -and $LASTEXITCODE -eq 0) {
                    $isTracked = $true
                }
            } catch {}
            
            $detectedFiles += [PSCustomObject]@{
                Path = $relativePath
                Pattern = $pattern
                IsIgnored = $isIgnored
                IsTracked = $isTracked
            }
            break
        }
    }
}

# Display results
if ($detectedFiles.Count -eq 0) {
    Write-Host "[OK] No sensitive files detected in working directory." -ForegroundColor Green
    exit 0
}

Write-Host "[WARN] Detected $($detectedFiles.Count) sensitive file(s):" -ForegroundColor Yellow
Write-Host ""

foreach ($file in $detectedFiles) {
    Write-Host "  File: $($file.Path)" -ForegroundColor Yellow
    Write-Host "  Pattern: $($file.Pattern)" -ForegroundColor Gray
    
    Write-Host "  Ignored: " -NoNewline -ForegroundColor White
    if ($file.IsIgnored) {
        Write-Host "Yes (via .gitignore)" -ForegroundColor Green
    } else {
        Write-Host "NO - NOT PROTECTED" -ForegroundColor Red
    }
    
    Write-Host "  Tracked: " -NoNewline -ForegroundColor White
    if ($file.IsTracked) {
        Write-Host "YES - ALREADY IN GIT" -ForegroundColor Red
    } else {
        Write-Host "No" -ForegroundColor Green
    }
    Write-Host ""
}

# Check for files that need attention
$needsAttention = $detectedFiles | Where-Object { -not $_.IsIgnored -or $_.IsTracked }

if ($needsAttention.Count -gt 0) {
    Write-Host "[ACTION REQUIRED]" -ForegroundColor Yellow
    Write-Host "  - Add unprotected files to .gitignore" -ForegroundColor Cyan
    Write-Host "  - If files are already tracked, consider removing them from Git history" -ForegroundColor Cyan
    Write-Host "  - Run setup.ps1 to check Git history for sensitive files" -ForegroundColor Cyan
}

exit 0
