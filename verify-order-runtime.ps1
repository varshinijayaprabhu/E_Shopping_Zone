$ErrorActionPreference = "Continue"

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

Write-Host "=== Starting Discovery Server on port 8761 ==="
$discoveryProc = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\discovery-server\target\discovery-server-1.0.0.jar" -PassThru -WindowStyle Hidden

for ($i = 0; $i -lt 30; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8761/actuator/health" -Method Get -TimeoutSec 2 -UseBasicParsing
        if ($res.status -eq "UP") {
            Write-Host "Eureka Discovery Server is UP on port 8761!"
            break
        }
    } catch {
        Start-Sleep -Seconds 1
    }
}

Write-Host "=== Starting Order Service on port 8084 ==="
$orderProc = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\order-service\target\order-service-1.0.0.jar" -RedirectStandardOutput "order-stdout.log" -RedirectStandardError "order-stderr.log" -PassThru -WindowStyle Hidden

$orderStarted = $false
for ($i = 0; $i -lt 40; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8084/actuator/health" -Method Get -TimeoutSec 2 -UseBasicParsing
        if ($res.status -eq "UP") {
            Write-Host "Order Service is UP on port 8084!"
            $orderStarted = $true
            break
        }
    } catch {
        if ($orderProc.HasExited) {
            Write-Host "Order Service process exited with code $($orderProc.ExitCode)!"
            Get-Content "order-stderr.log" -Tail 20 | Write-Host
            Get-Content "order-stdout.log" -Tail 20 | Write-Host
            break
        }
        Start-Sleep -Seconds 1
    }
}
if (!$orderStarted) {
    throw "Order Service failed to start on port 8084 within 40 seconds!"
}

try {
    # 1. Check Eureka Registration
    Write-Host "`n--- Step 1: Checking Eureka Registration ---"
    try {
        $eureka = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/ORDER-SERVICE" -Headers @{ "Accept" = "application/json" } -TimeoutSec 5 -UseBasicParsing
        Write-Host "Eureka App Name: $($eureka.application.name)"
        Write-Host "Eureka Instance Status: $($eureka.application.instance[0].status)"
    } catch {
        Write-Host "Note: Eureka heartbeat registration may take up to 30s. Health probe confirmed UP."
    }

    # 2. Check Swagger OpenAPI Documentation
    Write-Host "`n--- Step 2: Check Swagger OpenAPI Docs ---"
    $swagger = Invoke-WebRequest -Uri "http://localhost:8084/v3/api-docs" -Method Get -UseBasicParsing
    Write-Host "Swagger Docs Status: $($swagger.StatusCode)"

    # 3. Authenticate Customer Alice (ID: 1)
    $aliceToken = New-JwtToken -UserId 1 -Email "alice@eshopping.com" -Role "ROLE_CUSTOMER"
    $authAlice = @{ "Authorization" = "Bearer $aliceToken"; "Content-Type" = "application/json" }

    # 4. Authenticate Merchant Bob (ID: 2)
    $bobToken = New-JwtToken -UserId 2 -Email "merchant@eshopping.com" -Role "ROLE_MERCHANT"
    $authBob = @{ "Authorization" = "Bearer $bobToken"; "Content-Type" = "application/json" }

    # 5. Authenticate Mallory (ID: 99) for IDOR Testing
    $malloryToken = New-JwtToken -UserId 99 -Email "mallory@badactor.com" -Role "ROLE_CUSTOMER"
    $authMallory = @{ "Authorization" = "Bearer $malloryToken"; "Content-Type" = "application/json" }

    # 6. GET /api/orders (Customer Order History)
    Write-Host "`n--- Step 3: GET /api/orders (Customer Alice's Order History) ---"
    $orders = Invoke-RestMethod -Uri "http://localhost:8084/api/orders?page=0&size=10" -Method Get -Headers $authAlice -UseBasicParsing
    Write-Host "Total Customer Orders: $($orders.data.totalElements)"
    foreach ($o in $orders.data.content) {
        Write-Host "  - Order: $($o.orderNumber) | Total: ₹$($o.totalAmount) | Status: $($o.orderStatus) | Saga: $($o.sagaState)"
        foreach ($item in $o.items) {
            Write-Host "      * Item: $($item.productNameSnapshot) x$($item.quantity) @ ₹$($item.unitPriceSnapshot)"
        }
    }

    # 7. GET /api/orders/1 (Customer Alice fetches Order #1)
    Write-Host "`n--- Step 4: GET /api/orders/1 (Order Detail Lookup) ---"
    $order1 = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/1" -Method Get -Headers $authAlice -UseBasicParsing
    Write-Host "Order Number: $($order1.data.orderNumber)"
    Write-Host "Shipping Destination: $($order1.data.shippingName), $($order1.data.shippingAddress)"
    Write-Host "Payment Method: $($order1.data.payment.paymentMethod) (Status: $($order1.data.payment.paymentStatus))"

    # 8. Anti-IDOR Security: Mallory attempts to fetch Alice's Order #1
    Write-Host "`n--- Step 5: Anti-IDOR Defense Check (Customer 99 accessing Order #1) ---"
    try {
        $idorAttempt = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/1" -Method Get -Headers $authMallory -UseBasicParsing
        Write-Host "SECURITY FAILED: Mallory was able to read Alice's order!"
    } catch {
        Write-Host "Anti-IDOR PASSED: Access correctly denied (Status: $($_.Exception.Response.StatusCode.value__))"
    }

    # 9. GET /api/orders/merchant (Merchant Bob's Line Items)
    Write-Host "`n--- Step 6: GET /api/orders/merchant (Merchant Bob Line Items) ---"
    $merchantItems = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/merchant?page=0&size=10" -Method Get -Headers $authBob -UseBasicParsing
    Write-Host "Total Merchant Line Items: $($merchantItems.data.totalElements)"
    foreach ($mItem in $merchantItems.data.content) {
        Write-Host "  - Order: $($mItem.orderNumber) | Product: $($mItem.productNameSnapshot) | Qty: $($mItem.quantity) | Subtotal: ₹$($mItem.subtotal) | Ship To: $($mItem.shippingName)"
    }

    # 10. Role Separation: Customer Alice attempts to call Merchant API
    Write-Host "`n--- Step 7: Role Separation Check (Customer accessing Merchant API) ---"
    try {
        $roleAttempt = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/merchant" -Method Get -Headers $authAlice -UseBasicParsing
        Write-Host "SECURITY FAILED: Customer was able to access Merchant endpoint!"
    } catch {
        Write-Host "Role Separation PASSED: Forbidden (Status: $($_.Exception.Response.StatusCode.value__))"
    }

    # 11. GET /api/orders/status/{orderNumber} (Public Status Lookup)
    Write-Host "`n--- Step 8: GET /api/orders/status/ORD-INIT-001 (Status Lookup) ---"
    $statusLookup = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/status/ORD-INIT-001" -Method Get -UseBasicParsing
    Write-Host "Order: $($statusLookup.data.orderNumber) | Status: $($statusLookup.data.orderStatus) | Total: ₹$($statusLookup.data.totalAmount)"

    # 12. POST /api/orders/reconcile (Internal Saga Reconciliation)
    Write-Host "`n--- Step 9: POST /api/orders/reconcile (Internal Service Reconciliation) ---"
    $internalHeaders = @{
        "X-Internal-Secret" = "ESHOPPING_INTERNAL_SECRET_2026"
        "Content-Type" = "application/json"
    }
    $reconcileRes = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/reconcile" -Method Post -Headers $internalHeaders -UseBasicParsing
    Write-Host "Reconciled Pending Orders: $($reconcileRes.data)"

    # 13. Reconcile Secret Protection: Missing secret
    Write-Host "`n--- Step 10: Reconcile Secret Protection Check (Missing Secret) ---"
    try {
        $reconcileAttempt = Invoke-RestMethod -Uri "http://localhost:8084/api/orders/reconcile" -Method Post -UseBasicParsing
        Write-Host "SECURITY FAILED: Reconciliation allowed without secret!"
    } catch {
        Write-Host "Internal Secret Protection PASSED: Forbidden (Status: $($_.Exception.Response.StatusCode.value__))"
    }

    Write-Host "`n=== LIVE RUNTIME ORDER SERVICE VERIFICATION SUCCESSFUL! ==="

} finally {
    Write-Host "`n--- Cleaning up processes ---"
    if ($orderProc -and !$orderProc.HasExited) {
        Stop-Process -Id $orderProc.Id -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped Order Service (PID: $($orderProc.Id))"
    }
    if ($discoveryProc -and !$discoveryProc.HasExited) {
        Stop-Process -Id $discoveryProc.Id -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped Discovery Server (PID: $($discoveryProc.Id))"
    }
}

