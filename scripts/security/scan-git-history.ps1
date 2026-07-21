# ============================================================================
# Git History Security Scanner
# ============================================================================
# Scans Git history for sensitive files that were previously committed.
#
# Usage:
#   powershell -ExecutionPolicy Bypass -File .\scripts\security\scan-git-history.ps1
# ============================================================================

Write-Host ""
Write-Host "============================================" -ForegroundColor Cyan
Write-Host "GIT HISTORY SECURITY SCAN" -ForegroundColor Cyan
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
Write-Host "Scanning Git history (this may take a moment)..." -ForegroundColor Cyan
Write-Host ""

# Get all files ever committed to Git
$allHistoricalFiles = git log --all --pretty=format: --name-only --diff-filter=A 2>$null |
    Sort-Object -Unique |
    Where-Object { $_ -ne "" }

$detectedInHistory = @()

foreach ($file in $allHistoricalFiles) {
    $fileName = Split-Path -Leaf $file
    
    foreach ($pattern in $patterns) {
        $matched = $false
        
        if ($pattern -like "*.*") {
            # Extension pattern
            $ext = $pattern.Replace("*", "")
            if ($fileName -like "*$ext") {
                $matched = $true
            }
        } elseif ($pattern -like "*`**") {
            # Prefix wildcard
            $prefix = $pattern.Replace("*", "")
            if ($fileName -like "$prefix*") {
                $matched = $true
            }
        } else {
            # Exact match
            if ($fileName -eq $pattern -or ($file -replace '\\', '/') -eq $pattern) {
                $matched = $true
            }
        }
        
        if ($matched) {
            # Check if file is currently tracked
            $isCurrentlyTracked = $false
            try {
                $lsFiles = git ls-files $file 2>$null
                if ($lsFiles -and $LASTEXITCODE -eq 0) {
                    $isCurrentlyTracked = $true
                }
            } catch {}
            
            $detectedInHistory += [PSCustomObject]@{
                Path = $file
                Pattern = $pattern
                CurrentlyTracked = $isCurrentlyTracked
            }
            break
        }
    }
}

# Display results
if ($detectedInHistory.Count -eq 0) {
    Write-Host "[OK] No sensitive files found in Git history." -ForegroundColor Green
    exit 0
}

Write-Host "[SECURITY WARNING] Sensitive files found in Git history!" -ForegroundColor Red
Write-Host ""
Write-Host "Found $($detectedInHistory.Count) sensitive file(s) in Git history:" -ForegroundColor Yellow
Write-Host ""

foreach ($file in $detectedInHistory) {
    Write-Host "  File: $($file.Path)" -ForegroundColor Red
    Write-Host "  Pattern: $($file.Pattern)" -ForegroundColor Gray
    
    Write-Host "  Currently Tracked: " -NoNewline -ForegroundColor White
    if ($file.CurrentlyTracked) {
        Write-Host "YES" -ForegroundColor Red
    } else {
        Write-Host "No (removed from working tree)" -ForegroundColor Yellow
    }
    Write-Host ""
}

# Remediation instructions
Write-Host ""
Write-Host "============================================" -ForegroundColor Yellow
Write-Host "REMEDIATION REQUIRED" -ForegroundColor Yellow
Write-Host "============================================" -ForegroundColor Yellow
Write-Host ""

Write-Host "These files exist in Git history and may be visible to anyone with repository access." -ForegroundColor Cyan
Write-Host "To remove them from Git history, use git-filter-repo:" -ForegroundColor Cyan
Write-Host ""

Write-Host "Step 1 - Install git-filter-repo:" -ForegroundColor White
Write-Host "   pip install git-filter-repo" -ForegroundColor Gray
Write-Host ""

Write-Host "Step 2 - Remove sensitive files from history:" -ForegroundColor White
foreach ($file in $detectedInHistory | Select-Object -First 5) {
    Write-Host "   git filter-repo --path `"$($file.Path)`" --invert-paths" -ForegroundColor Gray
}
if ($detectedInHistory.Count -gt 5) {
    Write-Host "   ... and $($detectedInHistory.Count - 5) more file(s)" -ForegroundColor Gray
}
Write-Host ""

Write-Host "Step 3 - Force push to update remote repository:" -ForegroundColor White
Write-Host "   git push origin --force --all" -ForegroundColor Gray
Write-Host ""

Write-Host "[WARNING] Rewriting Git history affects ALL contributors!" -ForegroundColor Yellow
Write-Host "  - Coordinate with your team before force-pushing" -ForegroundColor Cyan
Write-Host "  - All team members must re-clone or hard-reset their local copies" -ForegroundColor Cyan
Write-Host "  - If actual credentials were exposed, REVOKE and ROTATE them immediately" -ForegroundColor Cyan
Write-Host "  - Removing files from Git history does NOT revoke exposed credentials" -ForegroundColor Cyan

exit 1
