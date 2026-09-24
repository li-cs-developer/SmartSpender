# =====================================================================
# SmartSpender — bring the whole stack up
# Usage: .\scripts\up.ps1
# =====================================================================

Write-Host "============================================================" -ForegroundColor Cyan
Write-Host "  SmartSpender — starting full stack" -ForegroundColor Cyan
Write-Host "============================================================" -ForegroundColor Cyan
Write-Host ""

# 1. Ensure .env exists
if (-not (Test-Path .env)) {
    Write-Host "Creating .env from .env.example..." -ForegroundColor Yellow
    Copy-Item .env.example .env
}

# 2. Kill any lingering services on the target ports
foreach ($port in 80, 8080, 5173) {
    Get-NetTCPConnection -LocalPort $port -State Listen -ErrorAction SilentlyContinue |
        ForEach-Object {
            Write-Host "Killing process on port $port (PID $($_.OwningProcess))" -ForegroundColor Yellow
            Stop-Process -Id $_.OwningProcess -Force -ErrorAction SilentlyContinue
        }
}

# 3. Build and start
Write-Host ""
Write-Host "Building and starting containers..." -ForegroundColor Green
docker compose up -d --build

# 4. Wait for backend to be healthy
Write-Host ""
Write-Host "Waiting for backend to become healthy..." -ForegroundColor Green
$ready = $false
for ($i = 1; $i -le 60; $i++) {
    Start-Sleep -Seconds 2
    try {
        $resp = Invoke-WebRequest -Uri "http://localhost:8080/api/auth/me" -Method Get -ErrorAction Stop -TimeoutSec 2
    } catch {
        if ($_.Exception.Response.StatusCode.value__ -eq 401) {
            $ready = $true
            break
        }
    }
    Write-Host "  ... still waiting ($i/60)" -ForegroundColor DarkGray
}

Write-Host ""
if ($ready) {
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host "  ✅ SmartSpender is running" -ForegroundColor Green
    Write-Host "============================================================" -ForegroundColor Green
    Write-Host ""
    Write-Host "  App:      http://localhost" -ForegroundColor White
    Write-Host "  API:      http://localhost:8080" -ForegroundColor White
    Write-Host "  pgAdmin:  http://localhost:5050  (admin@smartspender.local / admin)" -ForegroundColor White
    Write-Host ""
    Write-Host "  Test user: smartspender@test.local / hunter2hunter2" -ForegroundColor Yellow
    Write-Host ""
} else {
    Write-Host "Backend didn't become healthy in time." -ForegroundColor Red
    Write-Host "Check logs with: docker compose logs backend" -ForegroundColor Yellow
}
