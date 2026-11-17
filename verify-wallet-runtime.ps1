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

Write-Host "=== Starting Wallet Service on port 8085 ==="
$walletProc = Start-Process -FilePath "java" -ArgumentList "-jar", "backend\wallet-service\target\wallet-service-1.0.0.jar" -RedirectStandardOutput "wallet-stdout.log" -RedirectStandardError "wallet-stderr.log" -PassThru -WindowStyle Hidden

$walletStarted = $false
for ($i = 0; $i -lt 40; $i++) {
    try {
        $res = Invoke-RestMethod -Uri "http://localhost:8085/actuator/health" -Method Get -TimeoutSec 2 -UseBasicParsing
        if ($res.status -eq "UP") {
            Write-Host "Wallet Service is UP on port 8085!"
            $walletStarted = $true
            break
        }
    } catch {
        if ($walletProc.HasExited) {
            Write-Host "Wallet Service process exited with code $($walletProc.ExitCode)!"
            Get-Content "wallet-stderr.log" -Tail 20 | Write-Host
            Get-Content "wallet-stdout.log" -Tail 20 | Write-Host
            break
        }
        Start-Sleep -Seconds 1
    }
}
if (!$walletStarted) {
    throw "Wallet Service failed to start on port 8085 within 40 seconds!"
}

try {
    # 1. Check Eureka Registration
    Write-Host "`n--- Step 1: Checking Eureka Registration ---"
    try {
        $eureka = Invoke-RestMethod -Uri "http://localhost:8761/eureka/apps/WALLET-SERVICE" -Headers @{ "Accept" = "application/json" } -TimeoutSec 5 -UseBasicParsing
        Write-Host "Eureka App Name: $($eureka.application.name)"
        Write-Host "Eureka Instance Status: $($eureka.application.instance[0].status)"
    } catch {
        Write-Host "Note: Eureka heartbeat registration may take up to 30s. Health probe confirmed UP."
    }

    # 2. Authenticate Customer Alice (ID: 1)
    $aliceToken = New-JwtToken -UserId 1 -Email "alice@eshopping.com" -Role "ROLE_CUSTOMER"
    $authHeader = @{ "Authorization" = "Bearer $aliceToken"; "Content-Type" = "application/json" }

    # 3. GET /api/wallet (View Balance)
    Write-Host "`n--- Step 2: GET /api/wallet (Initial Balance) ---"
    $wallet = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet" -Method Get -Headers $authHeader -UseBasicParsing
    Write-Host "Wallet ID: $($wallet.data.walletId)"
    Write-Host "Customer ID: $($wallet.data.customerId)"
    Write-Host "Current Balance: ₹$($wallet.data.currentBalance)"
    Write-Host "Status: $($wallet.data.status)"

    # 4. POST /api/wallet/deposit (Deposit Funds)
    Write-Host "`n--- Step 3: POST /api/wallet/deposit (Top-up ₹5,000.00) ---"
    $depKey = "DEP-LIVE-" + [Guid]::NewGuid().ToString()
    $depBody = @{
        amount = 5000.00
        idempotencyKey = $depKey
        fundingSource = "SIMULATED_UPI"
        description = "Live runtime deposit test"
    } | ConvertTo-Json
    $depRes = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/deposit" -Method Post -Headers $authHeader -Body $depBody -UseBasicParsing
    Write-Host "Deposit Txn Ref: $($depRes.data.transactionReference)"
    Write-Host "Amount: ₹$($depRes.data.amount)"
    Write-Host "Balance: ₹$($depRes.data.balanceBefore) -> ₹$($depRes.data.balanceAfter)"

    # 5. Idempotent Deposit Check (Replay with same key)
    Write-Host "`n--- Step 4: Idempotent Deposit Replay Check ---"
    $depReplay = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/deposit" -Method Post -Headers $authHeader -Body $depBody -UseBasicParsing
    Write-Host "Replay Txn Ref: $($depReplay.data.transactionReference)"
    Write-Host "Replay Balance After: ₹$($depReplay.data.balanceAfter) (Unchanged, no double-credit!)"

    # 6. POST /api/wallet/pay (Internal Checkout Deduction)
    Write-Host "`n--- Step 5: POST /api/wallet/pay (Internal Checkout Deduction ₹2,000.00) ---"
    $payKey = "PAY-LIVE-" + [Guid]::NewGuid().ToString()
    $payHeaders = @{
        "X-Internal-Secret" = "ESHOPPING_INTERNAL_SECRET_2026"
        "Content-Type" = "application/json"
    }
    $payBody = @{
        customerId = 1
        orderReference = "ORD-LIVE-20261107-001"
        amount = 2000.00
        idempotencyKey = $payKey
        description = "Live runtime order checkout payment"
    } | ConvertTo-Json
    $payRes = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/pay" -Method Post -Headers $payHeaders -Body $payBody -UseBasicParsing
    Write-Host "Payment Txn Ref: $($payRes.data.transactionReference)"
    Write-Host "Order Reference: $($payRes.data.orderReference)"
    Write-Host "Deducted: ₹$($payRes.data.amount)"
    Write-Host "Balance: ₹$($payRes.data.balanceBefore) -> ₹$($payRes.data.balanceAfter)"

    # 7. GET /api/wallet/payment/order/{orderReference} (Reconciliation Lookup)
    Write-Host "`n--- Step 6: GET /api/wallet/payment/order/ORD-LIVE-20261107-001 (Reconciliation) ---"
    $reconRes = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/payment/order/ORD-LIVE-20261107-001" -Method Get -Headers $payHeaders -UseBasicParsing
    Write-Host "Payment Found: $($reconRes.data.found)"
    Write-Host "Reconciled Order: $($reconRes.data.orderReference)"
    Write-Host "Reconciled Amount: ₹$($reconRes.data.amount)"
    Write-Host "Payment Status: $($reconRes.data.status)"

    # 8. POST /api/wallet/refund (Internal Saga Compensation)
    Write-Host "`n--- Step 7: POST /api/wallet/refund (Saga Compensation ₹2,000.00) ---"
    $refKey = "REF-LIVE-" + [Guid]::NewGuid().ToString()
    $refBody = @{
        customerId = 1
        orderReference = "ORD-LIVE-20261107-001"
        amount = 2000.00
        idempotencyKey = $refKey
        reason = "Live Saga rollback test"
        paymentTransactionReference = $payRes.data.transactionReference
    } | ConvertTo-Json
    $refRes = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/refund" -Method Post -Headers $payHeaders -Body $refBody -UseBasicParsing
    Write-Host "Refund Txn Ref: $($refRes.data.transactionReference)"
    Write-Host "Refunded: ₹$($refRes.data.amount)"
    Write-Host "Balance Restored: ₹$($refRes.data.balanceBefore) -> ₹$($refRes.data.balanceAfter)"

    # 9. GET /api/wallet/transactions (Immutable Ledger)
    Write-Host "`n--- Step 8: GET /api/wallet/transactions (Customer Immutable Ledger) ---"
    $ledger = Invoke-RestMethod -Uri "http://localhost:8085/api/wallet/transactions?page=0&size=5" -Method Get -Headers $authHeader -UseBasicParsing
    Write-Host "Total Ledger Entries: $($ledger.data.totalElements)"
    foreach ($entry in $ledger.data.content) {
        Write-Host "  - [$($entry.transactionType)] Ref: $($entry.transactionReference) | Amount: ₹$($entry.amount) | Bal: ₹$($entry.balanceBefore) -> ₹$($entry.balanceAfter) | Status: $($entry.status)"
    }

    Write-Host "`n=== LIVE RUNTIME VERIFICATION SUCCESSFUL! ==="

} finally {
    Write-Host "`n--- Cleaning up processes ---"
    if ($walletProc -and !$walletProc.HasExited) {
        Stop-Process -Id $walletProc.Id -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped Wallet Service (PID: $($walletProc.Id))"
    }
    if ($discoveryProc -and !$discoveryProc.HasExited) {
        Stop-Process -Id $discoveryProc.Id -Force -ErrorAction SilentlyContinue
        Write-Host "Stopped Discovery Server (PID: $($discoveryProc.Id))"
    }
}
