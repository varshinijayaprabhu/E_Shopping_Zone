$scriptDir = (Get-Location).Path
$jarPath = Join-Path $scriptDir "backend\product-service\target\product-service-1.0.0.jar"
$stdoutLog = Join-Path $scriptDir "logs\product_service-stdout.log"
$stderrLog = Join-Path $scriptDir "logs\product_service-stderr.log"

$jvmArgs = "-Dspring.datasource.password=Admin@123 -DDB_PASSWORD=Admin@123 -DMYSQL_PASSWORD=Admin@123 -DINTERNAL_SERVICE_SECRET=ESHOPPING_INTERNAL_111D7F2A2D8F57C82F5A2513"
$cmd = "cmd.exe /c start `"ProductService`" /B java $jvmArgs -jar `"$jarPath`" > `"$stdoutLog`" 2> `"$stderrLog`""

$result = Invoke-CimMethod -ClassName Win32_Process -MethodName Create -Arguments @{
    CommandLine = $cmd
    CurrentDirectory = $scriptDir
}
Write-Host "Spawned wrapper PID: $($result.ProcessId)"

$up = $false
for ($i = 0; $i -lt 40; $i++) {
    Start-Sleep -Seconds 1
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -TimeoutSec 2 -UseBasicParsing
        if ($res.status -eq "UP") {
            $up = $true
            break
        }
    } catch {}
}
Write-Host "Product Service UP: $up"

if ($up) {
    # Find actual java PID
    $conn = Get-NetTCPConnection -LocalPort 8082 -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($conn) {
        Write-Host "Product Service Java PID: $($conn.OwningProcess)"
        $jsonFile = Join-Path $scriptDir ".running-processes.json"
        if (Test-Path $jsonFile) {
            $running = Get-Content $jsonFile | ConvertFrom-Json
            $running."Product Service" = $conn.OwningProcess
            $running | ConvertTo-Json | Set-Content $jsonFile
        }
    }
}
