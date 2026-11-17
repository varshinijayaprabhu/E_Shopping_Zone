# ==============================================================================
# E-ShoppingZone Microservices Platform — Master Shutdown Script
# Capgemini Case Study Architecture (Local Windows Environment)
# ==============================================================================
[CmdletBinding()]
param()

$ErrorActionPreference = "SilentlyContinue"

Write-Host "========================================================================" -ForegroundColor Yellow
Write-Host "             E-SHOPPINGZONE MICROSERVICES SHUTDOWN SEQUENCE             " -ForegroundColor Yellow
Write-Host "========================================================================" -ForegroundColor Yellow

$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$pidFile = Join-Path $scriptDir ".running-processes.json"
$ports = @(5173, 8086, 8084, 8085, 8083, 8082, 8081, 8080, 8761)

# 1. Stop tracked PIDs from file
if (Test-Path $pidFile) {
    try {
        $json = Get-Content $pidFile -Raw | ConvertFrom-Json
        foreach ($prop in $json.PSObject.Properties) {
            $name = $prop.Name
            $pId = [int]$prop.Value
            if ($pId -gt 0) {
                Write-Host "Stopping $name (PID: $pId)..." -NoNewline
                try {
                    $proc = Get-Process -Id $pId -ErrorAction SilentlyContinue
                    if ($proc) {
                        Stop-Process -Id $pId -Force -ErrorAction SilentlyContinue
                        Write-Host " [STOPPED]" -ForegroundColor Green
                    } else {
                        Write-Host " [NOT RUNNING]" -ForegroundColor DarkGray
                    }
                } catch {
                    Write-Host " [SKIPPED]" -ForegroundColor DarkGray
                }
            }
        }
    } catch {}
    Remove-Item $pidFile -Force -ErrorAction SilentlyContinue
}

# 2. Sweep target ports using netstat to ensure zero orphaned listeners
Write-Host "`nSweeping target ports to ensure clean release..." -ForegroundColor DarkGray
foreach ($port in $ports) {
    try {
        $netstatOut = netstat -ano | findstr ":$port "
        if ($netstatOut) {
            foreach ($line in ($netstatOut -split "`r`n")) {
                if ($line -match "LISTENING\s+(\d+)$") {
                    $orphanPid = [int]$matches[1]
                    if ($orphanPid -gt 0 -and $orphanPid -ne $PID) {
                        Write-Host "  Freeing port $port held by orphaned PID $orphanPid..." -NoNewline
                        Stop-Process -Id $orphanPid -Force -ErrorAction SilentlyContinue
                        Write-Host " [RELEASED]" -ForegroundColor Green
                    }
                }
            }
        }
    } catch {}
}

Write-Host "`n========================================================================" -ForegroundColor Green
Write-Host "         ALL SERVICES HAVE BEEN SAFELY AND CLEANLY TERMINATED           " -ForegroundColor Green
Write-Host "========================================================================`n" -ForegroundColor Green

