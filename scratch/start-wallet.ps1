$scriptDir = (Get-Location).Path
$jarPath = Join-Path $scriptDir "backend\wallet-service\target\wallet-service-1.0.0.jar"
$stdoutLog = Join-Path $scriptDir "logs\wallet_service-stdout.log"
$stderrLog = Join-Path $scriptDir "logs\wallet_service-stderr.log"

$jvmArgs = "-Dspring.datasource.password=Admin@123 -DDB_PASSWORD=Admin@123 -DMYSQL_PASSWORD=Admin@123 -DINTERNAL_SERVICE_SECRET=ESHOPPING_INTERNAL_SECRET_2026 -DRAZORPAY_KEY_ID=rzp_test_5yq4Y3m9x12AbC -DRAZORPAY_KEY_SECRET=secret_mock_rzp_2026_test"
$cmd = "cmd.exe /c start `"WalletService`" /B java $jvmArgs -jar `"$jarPath`" > `"$stdoutLog`" 2> `"$stderrLog`""

$result = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
    CommandLine = $cmd
    CurrentDirectory = $scriptDir
}
Write-Host "Spawned wrapper PID: $($result.ProcessId)"

$up = $false
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8085/actuator/health" -TimeoutSec 2 -UseBasicParsing
        if ($res.status -eq "UP") {
            $up = $true
            break
        }
    } catch {}
}
Write-Host "Wallet Service UP: $up"

if ($up) {
    $conn = Get-NetTCPConnection -LocalPort 8085 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        Write-Host "Wallet Service Java PID: $($conn.OwningProcess)"
    }
}
