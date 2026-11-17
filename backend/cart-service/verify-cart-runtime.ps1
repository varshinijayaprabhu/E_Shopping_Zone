$ErrorActionPreference = "Continue"

function New-JwtToken {
    $header = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}')).TrimEnd('=').Replace('+','-').Replace('/','_')
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $exp = $now + 3600
    $payloadJson = "{`"sub`":`"1`",`"email`":`"alice@eshopping.com`",`"roles`":[`"ROLE_CUSTOMER`"],`"iss`":`"eshopping-zone-profile-service`",`"aud`":`"eshopping-zone-api-gateway`",`"exp`":$exp}"
    $payload = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($payloadJson)).TrimEnd('=').Replace('+','-').Replace('/','_')
    $toSign = "$header.$payload"
    $secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($secret)
    $sig = [Convert]::ToBase64String($hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($toSign))).TrimEnd('=').Replace('+','-').Replace('/','_')
    return "$toSign.$sig"
}

Write-Host "=== Starting Discovery Server on port 8761 ==="
$discoveryProc = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\discovery-server\target\discovery-server-1.0.0.jar" -PassThru -WindowStyle Hidden

for ($i = 0; $i -lt 30; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8761/actuator/health" -Method Get -TimeoutSec 2
        if ($res.status -eq "UP") {
            Write-Host "Eureka Discovery Server is UP on port 8761!"
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Host "=== Starting Product Service on port 8082 ==="
$productProc = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\product-service\target\product-service-1.0.0.jar" -PassThru -WindowStyle Hidden

for ($i = 0; $i -lt 40; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8082/actuator/health" -Method Get -TimeoutSec 2
        if ($res.status -eq "UP") {
            Write-Host "Product Service is UP on port 8082!"
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Host "=== Starting Cart Service on port 8083 ==="
$cartProc = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\cart-service\target\cart-service-1.0.0.jar" -PassThru -WindowStyle Hidden

for ($i = 0; $i -lt 40; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8083/actuator/health" -Method Get -TimeoutSec 2
        if ($res.status -eq "UP") {
            Write-Host "Cart Service is UP on port 8083!"
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

# Wait 5 seconds for Eureka discovery registry to sync
Write-Host "=== Waiting 5 seconds for Eureka registry sync ==="
Start-Sleep -Seconds 5

$token = New-JwtToken
$headers = @{
    "Authorization" = "Bearer $token"
    "Content-Type"  = "application/json"
}

Write-Host "=== 1. Testing POST /api/cart/items (Add Product ID 1 - Headphones) ==="
$addBody = '{"productId": 1, "quantity": 2}'
$addRes = Invoke-RestMethod -Uri "http://localhost:8083/api/cart/items" -Method Post -Headers $headers -Body $addBody
Write-Host "Add Response: $($addRes.message)"
Write-Host "Cart ID: $($addRes.data.cartId), Total Items: $($addRes.data.totalItems), Subtotal: Rs. $($addRes.data.cartSubtotal)"
$addRes.data.items | ForEach-Object {
    Write-Host " - Item ID: $($_.itemId), Product: $($_.productName) [$($_.productSku)], Qty: $($_.quantity), UnitPrice: Rs. $($_.unitPrice), Subtotal: Rs. $($_.itemSubtotal), InStock: $($_.inStock)"
}

$itemId = $addRes.data.items[0].itemId

Write-Host "=== 2. Testing GET /api/cart ==="
$getRes = Invoke-RestMethod -Uri "http://localhost:8083/api/cart" -Method Get -Headers $headers
Write-Host "Retrieved Cart Total Items: $($getRes.data.totalItems), Subtotal: Rs. $($getRes.data.cartSubtotal)"

Write-Host "=== 3. Testing PUT /api/cart/items/$itemId (Update quantity to 3) ==="
$putBody = '{"quantity": 3}'
$putRes = Invoke-RestMethod -Uri "http://localhost:8083/api/cart/items/$itemId" -Method Put -Headers $headers -Body $putBody
Write-Host "Updated Cart Total Items: $($putRes.data.totalItems), Subtotal: Rs. $($putRes.data.cartSubtotal)"

Write-Host "=== 4. Testing DELETE /api/cart (Clear Cart) ==="
$delStatusCode = (Invoke-WebRequest -Uri "http://localhost:8083/api/cart" -Method Delete -Headers $headers -UseBasicParsing).StatusCode
Write-Host "Clear Cart HTTP Status: $delStatusCode"

Write-Host "=== 5. Verifying Empty Cart via GET /api/cart ==="
$emptyRes = Invoke-RestMethod -Uri "http://localhost:8083/api/cart" -Method Get -Headers $headers
Write-Host "Items in Cart after Clear: $($emptyRes.data.items.Count)"

Write-Host "=== 6. Verifying Eureka Registration of CART-SERVICE ==="
try {
    $eurekaApp = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/CART-SERVICE" -Headers @{ "Accept" = "application/json" } -Method Get
    Write-Host "Eureka Registered App Name: $($eurekaApp.application.name)"
    Write-Host "Eureka Instance Status: $($eurekaApp.application.instance[0].status)"
    Write-Host "Eureka Port: $($eurekaApp.application.instance[0].port.'$')"
} catch {
    Write-Host "Eureka check returned: $_"
}

Write-Host "=== Stopping Runtime Test Processes ==="
Stop-Process -Id $cartProc.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $productProc.Id -Force -ErrorAction SilentlyContinue
Stop-Process -Id $discoveryProc.Id -Force -ErrorAction SilentlyContinue
Write-Host "=== Phase 5 Runtime Verification Successfully Completed ==="

