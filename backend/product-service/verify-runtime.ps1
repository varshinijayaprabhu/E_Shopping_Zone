$ErrorActionPreference = "Continue"

Write-Host "=== Starting Discovery Server on port 8761 ==="
$discoveryProcess = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\discovery-server\target\discovery-server-1.0.0.jar" -PassThru -WindowStyle Hidden

# Wait for Eureka to be healthy
$eurekaReady = $false
for ($i = 0; $i -lt 30; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8761/actuator/health" -Method Get -TimeoutSec 2
        if ($res.status -eq "UP") {
            $eurekaReady = $true
            Write-Host "Eureka Discovery Server is UP on port 8761!"
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

if (-not $eurekaReady) {
    Write-Host "Timed out waiting for Discovery Server to start."
    Stop-Process -Id $discoveryProcess.Id -Force
    exit 1
}

Write-Host "=== Starting Product Service on port 8082 ==="
$productProcess = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\product-service\target\product-service-1.0.0.jar" -PassThru -WindowStyle Hidden

# Wait for Product Service to be healthy
$productReady = $false
for ($i = 0; $i -lt 40; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -Method Get -TimeoutSec 2
        if ($res.status -eq "UP") {
            $productReady = $true
            Write-Host "Product Service is UP on port 8082!"
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

if (-not $productReady) {
    Write-Host "Timed out waiting for Product Service to start."
    Stop-Process -Id $productProcess.Id -Force
    Stop-Process -Id $discoveryProcess.Id -Force
    exit 1
}

Write-Host "=== Verifying GET /api/categories ==="
$categories = Invoke-RestMethod -Uri "http://localhost:8082/api/categories" -Method Get
Write-Host "Categories Count: $($categories.data.Count)"
$categories.data | ForEach-Object { Write-Host " - $($_.id): $($_.name) (slug: $($_.slug))" }

Write-Host "=== Verifying GET /api/products ==="
$products = Invoke-RestMethod -Uri "http://localhost:8082/api/products?page=0&size=5" -Method Get
Write-Host "Total Products: $($products.pagination.totalElements), Total Pages: $($products.pagination.totalPages)"
$products.data | ForEach-Object { Write-Host " - [$($_.sku)] $($_.name) - Price: Rs. $($_.price) - Stock: $($_.availableStock)" }

Write-Host "=== Verifying Eureka Registration ==="
try {
    $eurekaApp = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/PRODUCT-SERVICE" -Headers @{ "Accept" = "application/json" } -Method Get
    Write-Host "Eureka Registered App Name: $($eurekaApp.application.name)"
    Write-Host "Eureka Instance Status: $($eurekaApp.application.instance[0].status)"
    Write-Host "Eureka Port: $($eurekaApp.application.instance[0].port.'$')"
} catch {
    Write-Host "Eureka check returned: $_"
}

Write-Host "=== Stopping Runtime Test Processes ==="
Stop-Process -Id $productProcess.Id -Force
Stop-Process -Id $discoveryProcess.Id -Force
Write-Host "=== Runtime Verification Complete ==="

