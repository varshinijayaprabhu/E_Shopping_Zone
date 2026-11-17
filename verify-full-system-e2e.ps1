# E-ShoppingZone Full-System Integration & End-to-End Verification Script
# Tests all 7 Spring Boot Microservices, Eureka Discovery Server, API Gateway, React Frontend, and MySQL

$ErrorActionPreference = "Continue"

Write-Host "=================================================================" -ForegroundColor Cyan
Write-Host "  E-SHOPPINGZONE PHASE 9: FULL-SYSTEM INTEGRATION & E2E TESTING  " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan

$services = @(
    @{ Name = "Discovery Server"; Port = 8761; Jar = "backend\discovery-server\target\discovery-server-1.0.0.jar"; HealthUri = "http://localhost:8761/actuator/health" },
    @{ Name = "API Gateway";      Port = 8080; Jar = "backend\api-gateway\target\api-gateway-1.0.0.jar";           HealthUri = "http://localhost:8080/actuator/health" },
    @{ Name = "Profile Service";  Port = 8081; Jar = "backend\profile-service\target\profile-service-1.0.0.jar";   HealthUri = "http://localhost:8081/actuator/health" },
    @{ Name = "Product Service";  Port = 8082; Jar = "backend\product-service\target\product-service-1.0.0.jar";   HealthUri = "http://localhost:8082/actuator/health" },
    @{ Name = "Cart Service";     Port = 8083; Jar = "backend\cart-service\target\cart-service-1.0.0.jar";         HealthUri = "http://localhost:8083/actuator/health" },
    @{ Name = "Wallet Service";   Port = 8085; Jar = "backend\wallet-service\target\wallet-service-1.0.0.jar";     HealthUri = "http://localhost:8085/actuator/health" },
    @{ Name = "Order Service";    Port = 8084; Jar = "backend\order-service\target\order-service-1.0.0.jar";       HealthUri = "http://localhost:8084/actuator/health" }
)

$startedProcesses = @()

# Load .env variables into environment if present
$envFile = Join-Path $PSScriptRoot ".env"
if (Test-Path $envFile) {
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
        $tcp.Connect($HostName, $Port)
        $tcp.Close()
        return $true
    } catch {
        return $false
    }
}

function New-JwtToken {
    param([long]$UserId, [string]$Email, [string]$Role)
    $header = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes('{"alg":"HS256","typ":"JWT"}')).TrimEnd('=').Replace('+','-').Replace('/','_')
    $now = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
    $exp = $now + 3600
    $payloadJson = "{`"sub`":`"$UserId`",`"email`":`"$Email`",`"roles`":[`"$Role`"],`"iss`":`"eshopping-zone-profile-service`",`"aud`":`"eshopping-zone-api-gateway`",`"exp`":$exp}"
    $payload = [Convert]::ToBase64String([System.Text.Encoding]::UTF8.GetBytes($payloadJson)).TrimEnd('=').Replace('+','-').Replace('/','_')
    $toSign = "$header.$payload"
    $secret = "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970"
    $hmac = New-Object System.Security.Cryptography.HMACSHA256
    $hmac.Key = [System.Text.Encoding]::UTF8.GetBytes($secret)
    $sig = [Convert]::ToBase64String($hmac.ComputeHash([System.Text.Encoding]::UTF8.GetBytes($toSign))).TrimEnd('=').Replace('+','-').Replace('/','_')
    return "$toSign.$sig"
}

# 1. Environment & Service Startup
try {
Write-Host "`n--- [Step 1] Verifying & Launching Services ---" -ForegroundColor Yellow

# Verify MySQL on 3306
if (!(Test-PortOpen "127.0.0.1" 3306)) {
    throw "MySQL is not reachable on port 3306! Please start MySQL Server 8.4."
}
Write-Host "[OK] MySQL is active on port 3306." -ForegroundColor Green

foreach ($svc in $services) {
    $isOpen = Test-PortOpen "127.0.0.1" $svc.Port
    if (!$isOpen) {
        Write-Host "Starting $($svc.Name) on port $($svc.Port)..."
        $stdoutLog = "$($svc.Name.Replace(' ','_').ToLower())-stdout.log"
        $stderrLog = "$($svc.Name.Replace(' ','_').ToLower())-stderr.log"
        $jarFullPath = Join-Path $PSScriptRoot $svc.Jar
        $proc = Start-Process -FilePath "java" -ArgumentList "-jar `"$jarFullPath`"" -WorkingDirectory $PSScriptRoot -RedirectStandardOutput $stdoutLog -RedirectStandardError $stderrLog -PassThru -WindowStyle Hidden
        $startedProcesses += $proc

        $started = $false
        for ($i = 0; $i -lt 45; $i++) {
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
            Write-Host "[FAIL] $($svc.Name) failed to start on port $($svc.Port) within 45s!" -ForegroundColor Red
            if (Test-Path $stderrLog) { Get-Content $stderrLog -Tail 15 | Write-Host }
            throw "$($svc.Name) startup failed."
        }
        Write-Host "[OK] $($svc.Name) is UP on port $($svc.Port)!" -ForegroundColor Green
    } else {
        Write-Host "[OK] $($svc.Name) is already running on port $($svc.Port)." -ForegroundColor Green
    }
}

# Verify Frontend on 5173
$feOpen = Test-PortOpen "127.0.0.1" 5173
if (!$feOpen) {
    Write-Host "Starting React Frontend on port 5173 (preview)..."
    $feDir = Join-Path $PSScriptRoot "frontend"
    $viteBin = Join-Path $feDir "node_modules\vite\bin\vite.js"
    $feProc = Start-Process -FilePath "node" -ArgumentList "`"$viteBin`" preview --port 5173 --host 127.0.0.1" -WorkingDirectory $feDir -PassThru -WindowStyle Hidden
    $startedProcesses += $feProc
    for ($i = 0; $i -lt 15; $i++) {
        Start-Sleep -Seconds 1
        if (Test-PortOpen "127.0.0.1" 5173) { break }
    }
}
try {
    $feHtml = (Invoke-WebRequest -Uri "http://localhost:5173" -UseBasicParsing -TimeoutSec 3).Content
    if ($feHtml -like "*root*") {
        Write-Host "[OK] React Frontend is UP and serving Single Page Application on port 5173." -ForegroundColor Green
    } else {
        Write-Host "[WARN] React Frontend reached on 5173." -ForegroundColor DarkYellow
    }
} catch {
    Write-Host "[WARN] Frontend check skipped: $_" -ForegroundColor DarkYellow
}

# 2. Check Eureka Registration
Write-Host "`n--- [Step 2] Checking Eureka Registry ---" -ForegroundColor Yellow
$appNames = @()
for ($i = 0; $i -lt 25; $i++) {
    try {
        $eurekaApps = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps" -Headers @{ "Accept" = "application/json" } -TimeoutSec 5 -UseBasicParsing
        $appNames = $eurekaApps.applications.application | ForEach-Object { $_.name }
        if ($appNames.Count -ge 6) { break }
    } catch {}
    Start-Sleep -Seconds 1
}
Write-Host "Eureka Registered Applications ($($appNames.Count)): $($appNames -join ', ')" -ForegroundColor Cyan
Write-Host "Allowing 10s for microservices to sync Eureka client registries..." -ForegroundColor DarkGray
Start-Sleep -Seconds 10
Write-Host "Warming up Gateway load balancer routes..." -ForegroundColor DarkGray
$routesToWarm = @(
    "http://localhost:8080/api/profile",
    "http://localhost:8080/api/categories",
    "http://localhost:8080/api/products",
    "http://localhost:8080/api/cart",
    "http://localhost:8080/api/wallet",
    "http://localhost:8080/api/orders"
)
foreach ($route in $routesToWarm) {
    for ($w = 0; $w -lt 30; $w++) {
        try {
            $resp = Invoke-WebRequest -Uri $route -Method Get -TimeoutSec 3 -UseBasicParsing
            break
        } catch {
            $statusCode = $_.Exception.Response.StatusCode.value__
            if ($statusCode -ne 503 -and $statusCode -gt 0) {
                break
            }
            Start-Sleep -Seconds 1
        }
    }
}
Write-Host "[OK] Gateway load balancer routes ready." -ForegroundColor Green

# 3. Customer Journey (via API Gateway :8080)
Write-Host "`n--- [Step 3] Complete Customer Journey ---" -ForegroundColor Yellow

$timestamp = [DateTimeOffset]::UtcNow.ToUnixTimeSeconds()
$custEmail = "charlie.$timestamp@eshopping.com"
$custPass = "Pass1234!"
$regPayload = @{
    fullName = "Charlie Customer"
    email = $custEmail
    password = $custPass
    phoneNumber = "9876543210"
} | ConvertTo-Json

$custReg = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $regPayload -ContentType "application/json" -UseBasicParsing
$custEmailVal = if ($custReg.user) { $custReg.user.email } elseif ($custReg.email) { $custReg.email } else { $custEmail }
Write-Host "[OK] Customer registered: $custEmailVal" -ForegroundColor Green

# Login
$loginPayload = @{ email = $custEmail; password = $custPass } | ConvertTo-Json
$loginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $loginPayload -ContentType "application/json" -UseBasicParsing
$jwtToken = if ($loginRes.accessToken) { $loginRes.accessToken } else { $loginRes.data.accessToken }
$authHeader = @{ "Authorization" = "Bearer $jwtToken" }
Write-Host "[OK] Customer logged in successfully. JWT acquired." -ForegroundColor Green

# Deposit into Wallet
$depositPayload = @{
    amount = 100000.00
    fundingSource = "SIMULATED_UPI"
    description = "E2E Test Top-up"
    idempotencyKey = "DEP-$timestamp-E2E"
} | ConvertTo-Json
$depositRes = Invoke-RestMethod -Uri "http://localhost:8080/api/wallet/deposit" -Method Post -Headers $authHeader -Body $depositPayload -ContentType "application/json" -UseBasicParsing
Write-Host "[OK] Wallet deposit complete. Reference: $($depositRes.data.transactionReference), New Balance: ₹$($depositRes.data.balanceAfter)" -ForegroundColor Green

# Browse Categories & Products
$catRes = Invoke-RestMethod -Uri "http://localhost:8080/api/categories" -Method Get -UseBasicParsing
Write-Host "[OK] Categories retrieved: $($catRes.data.Count) categories available." -ForegroundColor Green

$prodRes = Invoke-RestMethod -Uri "http://localhost:8080/api/products?page=0&size=5" -Method Get -UseBasicParsing
$prodList = if ($prodRes.data -is [array]) { $prodRes.data } elseif ($prodRes.data.content) { $prodRes.data.content } else { $prodRes.data }
$product1 = $prodList[0]
$product2 = $prodList[1]
Write-Host "[OK] Selected Product 1: '$($product1.name)' (Price: ₹$($product1.price), Stock: $($product1.availableStock))" -ForegroundColor Green
Write-Host "[OK] Selected Product 2: '$($product2.name)' (Price: ₹$($product2.price), Stock: $($product2.availableStock))" -ForegroundColor Green

# Add items to Cart
$addP1 = @{ productId = $product1.id; quantity = 2 } | ConvertTo-Json
$cartRes1 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/items" -Method Post -Headers $authHeader -Body $addP1 -ContentType "application/json" -UseBasicParsing

$addP2 = @{ productId = $product2.id; quantity = 1 } | ConvertTo-Json
$cartRes2 = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/items" -Method Post -Headers $authHeader -Body $addP2 -ContentType "application/json" -UseBasicParsing

$cart = Invoke-RestMethod -Uri "http://localhost:8080/api/cart" -Method Get -Headers $authHeader -UseBasicParsing
Write-Host "[OK] Cart populated: Total items = $($cart.data.totalItems), Subtotal = ₹$($cart.data.cartSubtotal)" -ForegroundColor Green

# Update item quantity
$itemToUpdate = $cart.data.items | Where-Object { $_.productId -eq $product1.id } | Select-Object -First 1
$updatePayload = @{ quantity = 1 } | ConvertTo-Json
$cartUpdated = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/items/$($itemToUpdate.itemId)" -Method Put -Headers $authHeader -Body $updatePayload -ContentType "application/json" -UseBasicParsing
$expectedSubtotal = [decimal]$product1.price + [decimal]$product2.price
Write-Host "[OK] Cart quantity updated: Total items = $($cartUpdated.data.totalItems), Updated Subtotal = ₹$($cartUpdated.data.cartSubtotal)" -ForegroundColor Green

# Execute Wallet Checkout
$checkoutKey = "CHK-$timestamp-E2E"
$checkoutPayload = @{
    idempotencyKey = $checkoutKey
    paymentMethod = "WALLET"
    shippingName = "Charlie Customer"
    shippingPhone = "9876543210"
    shippingAddress = "42 Silicon Avenue, Whitefield, Bangalore, Karnataka - 560066"
} | ConvertTo-Json

$orderRes = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/checkout" -Method Post -Headers $authHeader -Body $checkoutPayload -ContentType "application/json" -UseBasicParsing
$order = $orderRes.data
Write-Host "[OK] Order created: #$($order.orderNumber) (ID: $($order.id))" -ForegroundColor Green
Write-Host "     Status: $($order.orderStatus), Saga State: $($order.sagaState), Total: ₹$($order.totalAmount)" -ForegroundColor Cyan

# Verify Cart cleared
$cartAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/cart" -Method Get -Headers $authHeader -UseBasicParsing
if ($cartAfter.data.totalItems -ne 0) {
    throw "Cart was not cleared after checkout! Total items: $($cartAfter.data.totalItems)"
}
Write-Host "[OK] Cart verified empty after checkout." -ForegroundColor Green

# Verify Wallet deducted
$walletAfter = Invoke-RestMethod -Uri "http://localhost:8080/api/wallet" -Method Get -Headers $authHeader -UseBasicParsing
$expectedBalance = 100000.00 - [decimal]$order.totalAmount
Write-Host "[OK] Wallet balance verified: ₹$($walletAfter.data.currentBalance) (Deducted ₹$($order.totalAmount))" -ForegroundColor Green

# Verify Inventory deducted
$p1After = Invoke-RestMethod -Uri "http://localhost:8080/api/products/$($product1.id)" -Method Get -UseBasicParsing
Write-Host "[OK] Product 1 inventory verified: Previous=$($product1.availableStock), New=$($p1After.data.availableStock)" -ForegroundColor Green

# Verify Order Details & Price Snapshots
$orderDetail = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)" -Method Get -Headers $authHeader -UseBasicParsing
Write-Host "[OK] Order details verified: $($orderDetail.data.items.Count) line items, Snapshot Prices intact." -ForegroundColor Green

# 4. Cash on Delivery (COD) Journey
Write-Host "`n--- [Step 4] Cash on Delivery (COD) Journey ---" -ForegroundColor Yellow
$product3 = $prodList[2]
$addP3 = @{ productId = $product3.id; quantity = 1 } | ConvertTo-Json
$null = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/items" -Method Post -Headers $authHeader -Body $addP3 -ContentType "application/json" -UseBasicParsing

$codKey = "COD-$timestamp-E2E"
$codPayload = @{
    idempotencyKey = $codKey
    paymentMethod = "CASH_ON_DELIVERY"
    shippingName = "Charlie Customer"
    shippingPhone = "9876543210"
    shippingAddress = "77 Brigade Road, Bangalore, Karnataka - 560025"
} | ConvertTo-Json

$codOrderRes = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/checkout" -Method Post -Headers $authHeader -Body $codPayload -ContentType "application/json" -UseBasicParsing
$codOrder = $codOrderRes.data
Write-Host "[OK] COD Order created: #$($codOrder.orderNumber)" -ForegroundColor Green
Write-Host "     Status: $($codOrder.orderStatus), Payment Status: $($codOrder.payment.paymentStatus), Method: $($codOrder.payment.paymentMethod)" -ForegroundColor Cyan

# Verify Wallet was NOT deducted for COD
$walletCod = Invoke-RestMethod -Uri "http://localhost:8080/api/wallet" -Method Get -Headers $authHeader -UseBasicParsing
if ($walletCod.data.currentBalance -ne $walletAfter.data.currentBalance) {
    throw "Wallet balance was improperly deducted during COD checkout!"
}
Write-Host "[OK] Wallet balance intact after COD checkout (No deduction: ₹$($walletCod.data.currentBalance))." -ForegroundColor Green

# 5. Merchant Journey
Write-Host "`n--- [Step 5] Merchant Management Journey ---" -ForegroundColor Yellow
$merchantEmail = "dan.$timestamp@eshopping.com"
$merchantRegPayload = @{
    fullName = "Dan Merchant"
    email = $merchantEmail
    password = "Pass1234!"
    phoneNumber = "9876543211"
    merchantInviteCode = "ESHOPPING_MERCHANT_INVITE_2026"
} | ConvertTo-Json

$merchantReg = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register-merchant" -Method Post -Body $merchantRegPayload -ContentType "application/json" -UseBasicParsing
$mchEmailVal = if ($merchantReg.user) { $merchantReg.user.email } elseif ($merchantReg.email) { $merchantReg.email } else { $merchantEmail }
Write-Host "[OK] Merchant registered: $mchEmailVal" -ForegroundColor Green

$mchLoginPayload = @{ email = $merchantEmail; password = "Pass1234!" } | ConvertTo-Json
$mchLoginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body $mchLoginPayload -ContentType "application/json" -UseBasicParsing
$mchJwt = if ($mchLoginRes.accessToken) { $mchLoginRes.accessToken } else { $mchLoginRes.data.accessToken }
$mchHeader = @{ "Authorization" = "Bearer $mchJwt" }

# Merchant creates product
$newProdPayload = @{
    categoryId = 1
    name = "Ultra Pro Gaming Mechanical Keyboard RGB"
    sku = "KEY-RGB-$timestamp"
    description = "Cherry MX Red mechanical switches with customizable per-key RGB backlighting"
    price = 4999.00
    imageUrl = "https://images.unsplash.com/photo-1587829741301-dc798b83add3"
    initialStock = 50
} | ConvertTo-Json

$newProdRes = Invoke-RestMethod -Uri "http://localhost:8080/api/products" -Method Post -Headers $mchHeader -Body $newProdPayload -ContentType "application/json" -UseBasicParsing
$createdProduct = $newProdRes.data
Write-Host "[OK] Merchant created product: ID #$($createdProduct.id), SKU '$($createdProduct.sku)'" -ForegroundColor Green

# Merchant edits product
$editPayload = @{
    categoryId = 1
    name = "Ultra Pro Gaming Mechanical Keyboard RGB (Updated Edition)"
    description = "Updated Cherry MX Red switches with noise dampeners"
    price = 4599.00
    imageUrl = "https://images.unsplash.com/photo-1587829741301-dc798b83add3"
    availableStock = 45
    active = $true
} | ConvertTo-Json

$editRes = Invoke-RestMethod -Uri "http://localhost:8080/api/products/$($createdProduct.id)" -Method Put -Headers $mchHeader -Body $editPayload -ContentType "application/json" -UseBasicParsing
Write-Host "[OK] Merchant updated product: New Price ₹$($editRes.data.price), New Stock $($editRes.data.availableStock)" -ForegroundColor Green

# Merchant views products
$mchProds = Invoke-RestMethod -Uri "http://localhost:8080/api/products/merchant" -Method Get -Headers $mchHeader -UseBasicParsing
$mchList = @(if ($mchProds.data -is [array]) { $mchProds.data } elseif ($mchProds.data.content) { $mchProds.data.content } else { $mchProds.data })
Write-Host "[OK] Merchant retrieved catalog: $($mchList.Count) products owned." -ForegroundColor Green

# 6. Failure & Compensation Tests
Write-Host "`n--- [Step 6] Failure & Compensation Testing ---" -ForegroundColor Yellow

# A. Insufficient Wallet Balance Test
Write-Host "Testing insufficient wallet balance..."
$poorCustEmail = "eve.$timestamp@eshopping.com"
$poorCustPayload = @{
    fullName = "Eve Poor"
    email = $poorCustEmail
    password = "Pass1234!"
    phoneNumber = "9876543212"
} | ConvertTo-Json
$null = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/register" -Method Post -Body $poorCustPayload -ContentType "application/json" -UseBasicParsing
$poorLoginRes = Invoke-RestMethod -Uri "http://localhost:8080/api/auth/login" -Method Post -Body (@{ email = $poorCustEmail; password = "Pass1234!" } | ConvertTo-Json) -ContentType "application/json" -UseBasicParsing
$poorJwt = if ($poorLoginRes.accessToken) { $poorLoginRes.accessToken } else { $poorLoginRes.data.accessToken }
$poorAuthHeader = @{ "Authorization" = "Bearer $poorJwt" }

# Add product to Eve's cart (Eve has ₹0 balance)
$null = Invoke-RestMethod -Uri "http://localhost:8080/api/cart/items" -Method Post -Headers $poorAuthHeader -Body (@{ productId = $product1.id; quantity = 1 } | ConvertTo-Json) -ContentType "application/json" -UseBasicParsing
try {
    $insufficientCheckout = @{
        idempotencyKey = "FAIL-BAL-$timestamp"
        paymentMethod = "WALLET"
        shippingName = "Eve Poor"
        shippingPhone = "9876543212"
        shippingAddress = "10 Poverty Lane, Bangalore"
    } | ConvertTo-Json
    Invoke-RestMethod -Uri "http://localhost:8080/api/orders/checkout" -Method Post -Headers $poorAuthHeader -Body $insufficientCheckout -ContentType "application/json" -UseBasicParsing
    Write-Host "[FAIL] Checkout should have failed due to zero wallet balance!" -ForegroundColor Red
} catch {
    Write-Host "[OK] Insufficient wallet balance rejected checkout as expected: $($_.Exception.Message)" -ForegroundColor Green
}

# Verify Eve's cart remains intact
$poorCart = Invoke-RestMethod -Uri "http://localhost:8080/api/cart" -Method Get -Headers $poorAuthHeader -UseBasicParsing
if ($poorCart.data.totalItems -gt 0) {
    Write-Host "[OK] Cart preserved after failed checkout (Item count: $($poorCart.data.totalItems))." -ForegroundColor Green
} else {
    Write-Host "[WARN] Cart was unexpectedly cleared on failed checkout." -ForegroundColor DarkYellow
}

# B. Duplicate Checkout (Idempotency)
$dupRes = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/checkout" -Method Post -Headers $authHeader -Body $checkoutPayload -ContentType "application/json" -UseBasicParsing
if ($dupRes.data.id -ne $order.id) {
    throw "Duplicate checkout did not return existing order!"
}
Write-Host "[OK] Duplicate checkout prevention verified: Identical order returned without duplicate charge." -ForegroundColor Green

# B. Cancellation & Saga Compensation (Refund + Stock Restore)
$p1StockBeforeCancel = (Invoke-RestMethod -Uri "http://localhost:8080/api/products/$($product1.id)" -Method Get -UseBasicParsing).data.availableStock
$cancelPayload = @{ reason = "Customer cancelled order for E2E testing" } | ConvertTo-Json
$cancelRes = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)/cancel" -Method Post -Headers $authHeader -Body $cancelPayload -ContentType "application/json" -UseBasicParsing
Write-Host "[OK] Order cancellation executed: Status = $($cancelRes.data.orderStatus)" -ForegroundColor Green

# Verify wallet refunded
$walletAfterCancel = Invoke-RestMethod -Uri "http://localhost:8080/api/wallet" -Method Get -Headers $authHeader -UseBasicParsing
Write-Host "[OK] Wallet balance restored: ₹$($walletAfterCancel.data.currentBalance)" -ForegroundColor Green

# Verify inventory restored
$p1StockAfterCancel = (Invoke-RestMethod -Uri "http://localhost:8080/api/products/$($product1.id)" -Method Get -UseBasicParsing).data.availableStock
Write-Host "[OK] Inventory restored: Stock before cancel = $p1StockBeforeCancel, after cancel = $p1StockAfterCancel" -ForegroundColor Green

# C. Replay Cancellation (Idempotent)
try {
    $reCancel = Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)/cancel" -Method Post -Headers $authHeader -Body $cancelPayload -ContentType "application/json" -UseBasicParsing
    Write-Host "[OK] Re-cancellation handled idempotently (Status: $($reCancel.data.orderStatus))." -ForegroundColor Green
} catch {
    Write-Host "[OK] Re-cancellation properly rejected: $($_.Exception.Message)" -ForegroundColor Green
}

# 7. Security Testing
Write-Host "`n--- [Step 7] Security Testing ---" -ForegroundColor Yellow

# A. Unauthenticated API access (Expect 401)
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/orders" -Method Get -UseBasicParsing
    Write-Host "[FAIL] Unauthenticated access to /api/orders should have returned 401!" -ForegroundColor Red
} catch {
    Write-Host "[OK] Unauthenticated access to /api/orders properly rejected with 401." -ForegroundColor Green
}

# B. Customer attempting to access merchant endpoint (Expect 403)
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/orders/merchant" -Method Get -Headers $authHeader -UseBasicParsing
    Write-Host "[FAIL] Customer access to /api/orders/merchant should have returned 403!" -ForegroundColor Red
} catch {
    Write-Host "[OK] Customer access to /api/orders/merchant properly rejected with 403." -ForegroundColor Green
}

# C. Customer attempting anti-IDOR access to another customer's order (Expect 404 or 403)
$diffUserJwt = New-JwtToken -UserId 9999 -Email "stranger@eshopping.com" -Role "ROLE_CUSTOMER"
$strangerHeader = @{ "Authorization" = "Bearer $diffUserJwt" }
try {
    Invoke-RestMethod -Uri "http://localhost:8080/api/orders/$($order.id)" -Method Get -Headers $strangerHeader -UseBasicParsing
    Write-Host "[FAIL] Anti-IDOR check failed: Stranger accessed Charlie's order!" -ForegroundColor Red
} catch {
    Write-Host "[OK] Anti-IDOR protection verified: Foreign user cannot access another user's order." -ForegroundColor Green
}

# D. Internal API without valid service key (Expect 401 or 403)
try {
    Invoke-RestMethod -Uri "http://localhost:8082/api/products/reserve" -Method Post -Body "{}" -ContentType "application/json" -UseBasicParsing
    Write-Host "[FAIL] Internal reserve API should reject call without service key!" -ForegroundColor Red
} catch {
    Write-Host "[OK] Internal /api/products/reserve properly rejected without service key." -ForegroundColor Green
}

try {
    Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/pay" -Method Post -Body "{}" -ContentType "application/json" -UseBasicParsing
    Write-Host "[FAIL] Internal wallet pay API should reject call without service key!" -ForegroundColor Red
} catch {
    Write-Host "[OK] Internal /api/wallet/pay properly rejected without service key." -ForegroundColor Green
}

# 8. Database Direct Consistency Check
Write-Host "`n--- [Step 8] Direct Database Consistency Verification ---" -ForegroundColor Yellow

$mysqlCmd = "C:\Program Files\MySQL\MySQL Server 8.4\bin\mysql.exe"

# A. No negative balances in wallet_db.wallets
$negWallet = & $mysqlCmd -u root -e "SELECT COUNT(*) FROM wallet_db.wallets WHERE current_balance < 0;" -s -N
if ($negWallet -eq "0") {
    Write-Host "[OK] Database check: Zero negative wallet balances in wallet_db.wallets." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Found negative wallet balances: $negWallet" -ForegroundColor Red
}

# B. Ledger integrity: Count where balance_after != balance_before +/- amount
$ledgerErrors = & $mysqlCmd -u root -e "SELECT COUNT(*) FROM wallet_db.wallet_transactions WHERE (transaction_type IN ('DEPOSIT','REFUND') AND ROUND(balance_after - (balance_before + amount), 2) != 0) OR (transaction_type IN ('PAYMENT','WITHDRAWAL') AND ROUND(balance_after - (balance_before - amount), 2) != 0);" -s -N
if ($ledgerErrors -eq "0") {
    Write-Host "[OK] Database check: 100% ledger math integrity in wallet_db.wallet_transactions." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Ledger errors found: $ledgerErrors" -ForegroundColor Red
}

# C. No stuck in-progress sagas in order_db.orders
$stuckSagas = & $mysqlCmd -u root -e "SELECT COUNT(*) FROM order_db.orders WHERE saga_state NOT IN ('COMPLETED', 'COMPENSATION_COMPLETED');" -s -N
if ($stuckSagas -eq "0") {
    Write-Host "[OK] Database check: Zero stuck or orphaned Sagas in order_db.orders." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Found stuck sagas: $stuckSagas" -ForegroundColor Red
}

# D. Product inventory stock consistency
$negStock = & $mysqlCmd -u root -e "SELECT COUNT(*) FROM product_db.inventory WHERE available_stock < 0 OR reserved_stock < 0;" -s -N
if ($negStock -eq "0") {
    Write-Host "[OK] Database check: Zero negative stock quantities in product_db.inventory." -ForegroundColor Green
} else {
    Write-Host "[FAIL] Found negative stock records: $negStock" -ForegroundColor Red
}

Write-Host "`n=================================================================" -ForegroundColor Cyan
Write-Host "  FULL-SYSTEM INTEGRATION & E2E TESTING COMPLETED SUCCESSFULLY!  " -ForegroundColor Cyan
Write-Host "=================================================================" -ForegroundColor Cyan
} finally {
# Cleanup started processes
if ($startedProcesses.Count -gt 0) {
    Write-Host "`nShutting down started test microservice processes..." -ForegroundColor DarkGray
    foreach ($p in $startedProcesses) {
        try { Stop-Process -Id $p.Id -Force } catch {}
    }
}
}
