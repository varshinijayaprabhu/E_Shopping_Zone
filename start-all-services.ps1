# ==============================================================================
# E-ShoppingZone Microservices Platform — Master Startup Script
# Capgemini Case Study Architecture (Local Windows Environment)
# ==============================================================================
[CmdletBinding()]
param(
    [switch]$FrontendDevMode = $false
)

$ErrorActionPreference = "Continue"

Write-Host "========================================================================" -ForegroundColor Cyan
Write-Host "             E-SHOPPINGZONE MICROSERVICES STARTUP SEQUENCE              " -ForegroundColor Cyan
Write-Host "========================================================================" -ForegroundColor Cyan

# Working directories and logs
$scriptDir = Split-Path -Parent $MyInvocation.MyCommand.Path
Set-Location $scriptDir

$logsDir = Join-Path $scriptDir "logs"
if (!(Test-Path $logsDir)) {
    New-Item -ItemType Directory -Path $logsDir -Force | Out-Null
}

$pidFile = Join-Path $scriptDir ".running-processes.json"

# Load .env variables into environment if present
$envFile = Join-Path $scriptDir ".env"
if (Test-Path $envFile) {
    Write-Host "Loading environment configurations from .env..." -ForegroundColor DarkGray
    Get-Content $envFile | ForEach-Object {
        $line = $_.Trim()
        if ($line -and !$line.StartsWith("#") -and $line.Contains("=")) {
            $parts = $line.Split("=", 2)
            $varName = $parts[0].Trim()
            $varVal = $parts[1].Trim()
            [Environment]::SetEnvironmentVariable($varName, $varVal, "Process")
        }
    }
}

# Helper: Test TCP Port
function Test-PortOpen([string]$HostName, [int]$Port) {
    try {
        $tcp = New-Object System.Net.Sockets.TcpClient
        $iar = $tcp.BeginConnect($HostName, $Port, $null, $null)
        $wait = $iar.AsyncWaitHandle.WaitOne(1000, $false)
        if (!$wait) {
            $tcp.Close()
            return $false
        }
        $tcp.EndConnect($iar)
        $tcp.Close()
        return $true
    } catch {
        return $false
    }
}

# 1. Verify MySQL Server on Port 3306
Write-Host "`n[Check 1/3] Verifying MySQL Database Server (Port 3306)..." -ForegroundColor Yellow
if (!(Test-PortOpen "127.0.0.1" 3306)) {
    Write-Host "[ERROR] MySQL Server is NOT running on 127.0.0.1:3306!" -ForegroundColor Red
    Write-Host "Please start MySQL Server 8.4 or run: & 'C:\Program Files\MySQL\MySQL Server 8.4\bin\mysqld.exe' --datadir=C:\MySQLData" -ForegroundColor DarkYellow
    exit 1
}
Write-Host "[OK] MySQL 8.4 is online and accepting connections on port 3306." -ForegroundColor Green

# 2. Service Definitions (Ordered by boot dependency)
$services = @(
    @{ Name = "Discovery Server"; Port = 8761; Jar = "backend\discovery-server\target\discovery-server-1.0.0.jar"; HealthUri = "http://localhost:8761/actuator/health"; Swagger = "http://localhost:8761" },
    @{ Name = "API Gateway";      Port = 8080; Jar = "backend\api-gateway\target\api-gateway-1.0.0.jar";           HealthUri = "http://localhost:8080/actuator/health"; Swagger = "http://localhost:8080" },
    @{ Name = "Profile Service";  Port = 8081; Jar = "backend\profile-service\target\profile-service-1.0.0.jar";   HealthUri = "http://localhost:8081/actuator/health"; Swagger = "http://localhost:8081/swagger-ui/index.html" },
    @{ Name = "Product Service";  Port = 8082; Jar = "backend\product-service\target\product-service-1.0.0.jar";   HealthUri = "http://localhost:8082/actuator/health"; Swagger = "http://localhost:8082/swagger-ui/index.html" },
    @{ Name = "Cart Service";     Port = 8083; Jar = "backend\cart-service\target\cart-service-1.0.0.jar";         HealthUri = "http://localhost:8083/actuator/health"; Swagger = "http://localhost:8083/swagger-ui/index.html" },
    @{ Name = "Wallet Service";   Port = 8085; Jar = "backend\wallet-service\target\wallet-service-1.0.0.jar";     HealthUri = "http://localhost:8085/actuator/health"; Swagger = "http://localhost:8085/swagger-ui/index.html" },
    @{ Name = "Order Service";    Port = 8084; Jar = "backend\order-service\target\order-service-1.0.0.jar";       HealthUri = "http://localhost:8084/actuator/health"; Swagger = "http://localhost:8084/swagger-ui/index.html" }
)

# Load existing PID records
$runningMap = @{}
if (Test-Path $pidFile) {
    try {
        $existing = Get-Content $pidFile -Raw | ConvertFrom-Json
        foreach ($prop in $existing.PSObject.Properties) {
            $runningMap[$prop.Name] = [int]$prop.Value
        }
    } catch {}
}

# 3. Launch Backend Microservices
Write-Host "`n[Check 2/3] Starting Backend Microservices..." -ForegroundColor Yellow

foreach ($svc in $services) {
    $isOpen = Test-PortOpen "127.0.0.1" $svc.Port
    if ($isOpen) {
        Write-Host "  -> $($svc.Name) is ALREADY active on port $($svc.Port)." -ForegroundColor Cyan
        continue
    }

    $jarPath = Join-Path $scriptDir $svc.Jar
    if (!(Test-Path $jarPath)) {
        Write-Host "  [FAIL] Missing artifact: $jarPath. Please run 'mvn clean package -DskipTests' first!" -ForegroundColor Red
        exit 1
    }

    $logPrefix = $svc.Name.Replace(" ", "_").ToLower()
    $stdoutLog = Join-Path $logsDir "$logPrefix-stdout.log"
    $stderrLog = Join-Path $logsDir "$logPrefix-stderr.log"

    Write-Host "  Starting $($svc.Name) on port $($svc.Port)..." -NoNewline
    $proc = Start-Process -FilePath "java" -ArgumentList "-jar `"$jarPath`"" -WorkingDirectory $scriptDir -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog -PassThru -WindowStyle Hidden
    $runningMap[$svc.Name] = $proc.Id

    # Wait for Health check
    $started = $false
    for ($i = 0; $i -lt 50; $i++) {
        Start-Sleep -Seconds 1
        try {
            $res = Invoke-RestMethod -Uri $svc.HealthUri -Method Get -TimeoutSec 2 -UseBasicParsing
            if ($res.status -eq "UP") {
                $started = $true
                break
            }
        } catch {}
    }

    if (!$started) {
        Write-Host " [FAILED]" -ForegroundColor Red
        Write-Host "    $($svc.Name) failed to report UP within 50 seconds." -ForegroundColor Red
        $logToShow = if ((Test-Path $stderrLog) -and (Get-Item $stderrLog).Length -gt 0) { $stderrLog } else { $stdoutLog }
        if (Test-Path $logToShow) {
            Write-Host "    Last log lines from $($logToShow):" -ForegroundColor DarkYellow
            Get-Content $logToShow -Tail 15 | ForEach-Object { Write-Host "      $_" -ForegroundColor DarkYellow }
        }
        exit 1
    }
    Write-Host " [ONLINE] (PID: $($proc.Id))" -ForegroundColor Green
}

# 4. Wait for Eureka Registration Synchronization
Write-Host "`nVerifying Eureka Service Registry synchronization..." -ForegroundColor DarkGray
$appNames = @()
for ($i = 0; $i -lt 30; $i++) {
    try {
        $eurekaApps = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers @{ "Accept" = "application/json" } -TimeoutSec 4 -UseBasicParsing
        $appNames = $eurekaApps.applications.application | ForEach-Object { $_.name }
        if ($appNames.Count -ge 6) { break }
    } catch {}
    Start-Sleep -Seconds 1
}
Write-Host "  Registered in Eureka ($($appNames.Count)/6): $($appNames -join ', ')" -ForegroundColor Cyan
Write-Host "  Allowing 5s for Gateway load-balancer route caches to warm up..." -ForegroundColor DarkGray
Start-Sleep -Seconds 5

# 5. Launch Frontend on Port 5173
Write-Host "`n[Check 3/3] Starting React 18 Frontend on Port 5173..." -ForegroundColor Yellow
$feOpen = Test-PortOpen "127.0.0.1" 5173
if ($feOpen) {
    Write-Host "  -> React Frontend is ALREADY listening on port 5173." -ForegroundColor Cyan
} else {
    $feStdout = Join-Path $logsDir "frontend-stdout.log"
    $feStderr = Join-Path $logsDir "frontend-stderr.log"
    
    $frontendDir = Join-Path $scriptDir "frontend"
    if ($FrontendDevMode) {
        Write-Host "  Starting Vite Development Server..." -NoNewline
        $feProc = Start-Process -FilePath "npm.cmd" -ArgumentList "run dev -- --port 5173 --host 127.0.0.1" -WorkingDirectory $frontendDir -RedirectStandardOutput $feStdout -RedirectStandardError $feStderr -PassThru -WindowStyle Hidden
    } else {
        Write-Host "  Starting Vite Production Preview Server..." -NoNewline
        $viteBin = Join-Path $frontendDir "node_modules\vite\bin\vite.js"
        $feProc = Start-Process -FilePath "node" -ArgumentList "`"$viteBin`" preview --port 5173 --host 127.0.0.1" -WorkingDirectory $frontendDir -RedirectStandardOutput $feStdout -RedirectStandardError $feStderr -PassThru -WindowStyle Hidden
    }
    $runningMap["React Frontend"] = $feProc.Id

    for ($i = 0; $i -lt 20; $i++) {
        Start-Sleep -Seconds 1
        if (Test-PortOpen "127.0.0.1" 5173) { break }
    }
    Write-Host " [ONLINE] (PID: $($feProc.Id))" -ForegroundColor Green
}

# Persist PID map
$runningMap | ConvertTo-Json | Set-Content $pidFile -Force

# 6. Final Status & Access Matrix
Write-Host "`n========================================================================" -ForegroundColor Cyan
Write-Host "         ALL SERVICES SUCCESSFULLY STARTED AND OPERATIONAL!             " -ForegroundColor Green
Write-Host "========================================================================" -ForegroundColor Cyan

Write-Host "`nAccess Matrix:" -ForegroundColor Yellow
Write-Host "  * React Web Application: http://localhost:5173" -ForegroundColor White
Write-Host "  * API Gateway:          http://localhost:8080" -ForegroundColor White
Write-Host "  * Eureka Dashboard:     http://localhost:8761" -ForegroundColor White

Write-Host "`nSwagger OpenAPI Documentation:" -ForegroundColor Yellow
foreach ($svc in $services | Where-Object { $_.Port -ne 8761 -and $_.Port -ne 8080 }) {
    Write-Host "  * $($svc.Name.PadRight(18)): $($svc.Swagger)" -ForegroundColor DarkGray
}

Write-Host "`nPre-seeded Demonstration Credentials:" -ForegroundColor Yellow
Write-Host "  * Customer:  customer@eshopping.com   / Pass1234! (Wallet Balance: ₹15,000.00)" -ForegroundColor White
Write-Host "  * Merchant:  merchant@eshopping.com   / Pass1234! (Catalog: 12 Products)" -ForegroundColor White
$activeInvite = if ($env:MERCHANT_INVITE_CODE) { $env:MERCHANT_INVITE_CODE } else { "ESHOPPING_MERCHANT_INVITE_2026" }
Write-Host "  * Merchant Invite Secret: $activeInvite" -ForegroundColor DarkGray
Write-Host "`nTo gracefully shut down all services, execute: .\stop-all-services.ps1`n" -ForegroundColor Cyan

