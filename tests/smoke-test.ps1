# PowerBank Stations - end-to-end smoke test (Windows PowerShell 5.1 or PowerShell 7)
#
# Usage (backend must be running on port 8080):
#   cd G:\project\PowerBankStations\tests
#   .\smoke-test.ps1
#
# The admin part needs a staff role. The script creates an admin account, prints one SQL statement,
# and waits for you to run it in the Supabase SQL editor. Use -SkipAdmin to skip that part.
# Each run creates new accounts with unique emails, so it can be run again.

param(
    [string]$BaseUrl = "http://localhost:8080/api",
    [switch]$SkipAdmin,
    [string]$PhotoPath = ""
)

$ErrorActionPreference = "Stop"
$script:passed = 0
$script:failed = 0
$script:failures = @()

function Check([string]$name, [scriptblock]$test) {
    try {
        $ok = & $test
        if ($ok -eq $true) {
            $script:passed++
            Write-Host "  PASS  $name" -ForegroundColor Green
        }
        else {
            $script:failed++
            $script:failures += $name
            Write-Host "  FAIL  $name" -ForegroundColor Red
        }
    }
    catch {
        $script:failed++
        $script:failures += "$name -> $($_.Exception.Message)"
        Write-Host "  FAIL  $name : $($_.Exception.Message)" -ForegroundColor Red
    }
}

function Api([string]$method, [string]$path, $body = $null, $token = $null) {
    $headers = @{}
    if ($token) { $headers.Authorization = "Bearer $token" }
    $params = @{ Method = $method; Uri = "$BaseUrl$path"; Headers = $headers }
    if ($null -ne $body) {
        $params.ContentType = "application/json"
        $params.Body = ($body | ConvertTo-Json -Depth 5)
    }
    Invoke-RestMethod @params
}

# Returns the error code of a request that is expected to fail, or $null if it did not fail.
function ErrorCode([scriptblock]$request) {
    try {
        & $request | Out-Null
        return $null
    }
    catch {
        try {
            $raw = $_.ErrorDetails.Message
            if (-not $raw) { return "HTTP_ERROR" }
            return ($raw | ConvertFrom-Json).code
        }
        catch {
            return "HTTP_ERROR"
        }
    }
}

$stamp = Get-Date -Format "yyyyMMddHHmmss"
Write-Host ""
Write-Host "== Public endpoints ==" -ForegroundColor Cyan

$stations = $null
Check "GET /stations returns the sample stations" {
    $script:stations = Api GET "/stations"
    $script:stations.Count -ge 9
}

Check "every station has a numeric availableCount" {
    ($script:stations | Where-Object { $_.availableCount -is [int] -or $_.availableCount -is [long] }).Count -eq $script:stations.Count
}

Check "GET /pricing/active returns a plan" {
    $p = Api GET "/pricing/active"
    $p.pricePerMinute -gt 0
}

Check "GET /stations/{id}/availability works" {
    $a = Api GET "/stations/1/availability"
    $a.totalCapacity -gt 0
}

Check "unknown station returns STATION_NOT_FOUND" {
    (ErrorCode { Api GET "/stations/999999" }) -eq "STATION_NOT_FOUND"
}

Write-Host ""
Write-Host "== Accounts and validation ==" -ForegroundColor Cyan

$email = "smoke.$stamp@example.com"
$reg = $null
Check "register a new customer" {
    $script:reg = Api POST "/auth/register" @{ fullName = "Smoke Test"; email = $email; password = "Password123" }
    [bool]$script:reg.token
}

Check "register with the same email is refused (EMAIL_TAKEN)" {
    (ErrorCode { Api POST "/auth/register" @{ fullName = "Dup"; email = $email; password = "Password123" } }) -eq "EMAIL_TAKEN"
}

Check "short password is refused (VALIDATION_FAILED)" {
    (ErrorCode { Api POST "/auth/register" @{ fullName = "Short"; email = "short.$stamp@example.com"; password = "123" } }) -eq "VALIDATION_FAILED"
}

Check "wrong password is refused (BAD_CREDENTIALS)" {
    (ErrorCode { Api POST "/auth/login" @{ email = $email; password = "WrongPassword" } }) -eq "BAD_CREDENTIALS"
}

$token = $reg.token
Check "login with the right password returns a token" {
    $login = Api POST "/auth/login" @{ email = $email; password = "Password123" }
    [bool]$login.token
}

Check "protected endpoint without a token is refused" {
    (ErrorCode { Api GET "/rentals" }) -ne $null
}

Write-Host ""
Write-Host "== Customer flow: reserve, rent, return, photos ==" -ForegroundColor Cyan

$reservation = $null
$stationId = ($script:stations | Where-Object { $_.availableCount -gt 0 } | Select-Object -First 1).id

Check "reserve a PowerBank at an open station" {
    $script:reservation = Api POST "/reservations" @{ stationId = $stationId } $token
    $script:reservation.status -eq "PENDING" -and $script:reservation.powerBankSerial
}

Check "a second reservation is refused (ACTIVE_RESERVATION_EXISTS)" {
    (ErrorCode { Api POST "/reservations" @{ stationId = $stationId } $token }) -eq "ACTIVE_RESERVATION_EXISTS"
}

Check "the active hold is listed" {
    $holds = Api GET "/reservations/active" $null $token
    ($holds | Where-Object { $_.id -eq $script:reservation.id }).Count -eq 1
}

$rental = $null
Check "start the rental from the reservation" {
    $script:rental = Api POST "/rentals/start" @{ reservationId = $script:reservation.id } $token
    $script:rental.status -eq "ACTIVE"
}

Check "starting the same reservation again is refused" {
    (ErrorCode { Api POST "/rentals/start" @{ reservationId = $script:reservation.id } $token }) -eq "RESERVATION_NOT_ACTIVE"
}

Check "the rental appears in the active list" {
    $active = Api GET "/rentals/active" $null $token
    ($active | Where-Object { $_.id -eq $script:rental.id }).Count -eq 1
}

$returnStation = ($script:stations | Where-Object { $_.id -ne $stationId } | Select-Object -First 1).id
$ret = $null
Check "return the PowerBank to another station" {
    $script:ret = Api POST "/returns" @{ rentalId = $script:rental.id; returnStationId = $returnStation } $token
    $script:ret.status -eq "PENDING_REVIEW"
}

Check "the rental is closed after the return" {
    (Api GET "/rentals/$($script:rental.id)" $null $token).status -eq "COMPLETED"
}

Check "returning the same rental again is refused (RENTAL_NOT_ACTIVE)" {
    (ErrorCode { Api POST "/returns" @{ rentalId = $script:rental.id; returnStationId = $returnStation } $token }) -eq "RENTAL_NOT_ACTIVE"
}

Check "return status endpoint returns PENDING_REVIEW" {
    (Api GET "/returns/$($script:ret.id)/status" $null $token).status -eq "PENDING_REVIEW"
}

if ($PhotoPath -and (Test-Path $PhotoPath)) {
    Check "upload one photo to the return" {
        $form = @{ files = Get-Item $PhotoPath }
        $up = Invoke-RestMethod -Method Post -Uri "$BaseUrl/returns/$($script:ret.id)/images" -Headers @{ Authorization = "Bearer $token" } -Form $form
        $up.Count -eq 1
    }
}
else {
    Write-Host "  SKIP  photo upload (pass -PhotoPath to test it)" -ForegroundColor Yellow
}

Check "unknown rental returns RENTAL_NOT_FOUND" {
    (ErrorCode { Api GET "/rentals/999999" $null $token }) -eq "RENTAL_NOT_FOUND"
}

Write-Host ""
Write-Host "== Admin flow ==" -ForegroundColor Cyan

if ($SkipAdmin) {
    Write-Host "  SKIP  admin tests (-SkipAdmin)" -ForegroundColor Yellow
}
else {
    $adminEmail = "admin.$stamp@example.com"
    $adminReg = Api POST "/auth/register" @{ fullName = "Smoke Admin"; email = $adminEmail; password = "Password123" }
    $adminToken = $adminReg.token

    Write-Host ""
    Write-Host "  Run this in the Supabase SQL editor, then press Enter:" -ForegroundColor Yellow
    Write-Host "  insert into public.user_roles (user_id, role_id) select u.id, r.id from public.users u join public.roles r on r.name='SUPER_ADMIN' where u.email='$adminEmail' on conflict do nothing;" -ForegroundColor Cyan
    [void](Read-Host "Press Enter after running the SQL")

    Check "a customer cannot open the admin queue (NOT_AUTHORIZED)" {
        (ErrorCode { Api GET "/admin/returns" $null $token }) -eq "NOT_AUTHORIZED"
    }

    Check "staff can see the returns waiting for review" {
        $queue = Api GET "/admin/returns?status=PENDING_REVIEW" $null $adminToken
        ($queue | Where-Object { $_.id -eq $script:ret.id }).Count -eq 1
    }

    Check "approve the return: the PowerBank becomes available again" {
        Api POST "/admin/returns/$($script:ret.id)/approve" $null $adminToken | Out-Null
        $st = Api GET "/stations/$returnStation/availability"
        $st.available -ge 1
    }

    Check "approving the same return again is refused (RETURN_NOT_REVIEWABLE)" {
        (ErrorCode { Api POST "/admin/returns/$($script:ret.id)/approve" $null $adminToken }) -eq "RETURN_NOT_REVIEWABLE"
    }

    Check "create a station and add devices to it" {
        $s = Api POST "/admin/stations" @{
            name = "Smoke Station $stamp"; address = "Test street"; city = "Cairo";
            latitude = 30.05; longitude = 31.24; totalCapacity = 4; status = "ACTIVE"
        } $adminToken
        $withBanks = Api POST "/admin/stations/$($s.id)/powerbanks" @{ count = 2 } $adminToken
        $withBanks.availableCount -eq 2
    }
}

Write-Host ""
Write-Host "== Result ==" -ForegroundColor Cyan
Write-Host "Passed: $script:passed   Failed: $script:failed"
if ($script:failed -gt 0) {
    Write-Host "Failures:" -ForegroundColor Red
    $script:failures | ForEach-Object { Write-Host "  - $_" -ForegroundColor Red }
    exit 1
}
exit 0
