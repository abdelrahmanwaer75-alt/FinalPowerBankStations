# Starts the PowerBank Stations backend against local MySQL.
#
# Usage (from the backend folder):
#   .\run-local.ps1
#   .\run-local.ps1 -DbHost "127.0.0.1" -DbUser "root" -DbPort 3306
#
# The database password is asked for at the prompt and is never written to a file.

param(
    [string]$DbHost = "localhost",
    [string]$DbUser = "root",
    [int]$DbPort = 3306,
    [string]$JwtSecret = "AbdelrahmanMohamedAhmedElsayedWaer_abcXYZ123secretKey2026_dev_only"
)

$ErrorActionPreference = "Stop"
Set-Location -Path $PSScriptRoot

# 1) Password (hidden input, kept only in memory)
$secure = Read-Host "MySQL database password" -AsSecureString
$bstr = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($secure)
$password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($bstr)
[Runtime.InteropServices.Marshal]::ZeroFreeBSTR($bstr)
if ([string]::IsNullOrWhiteSpace($password)) { Write-Host "Password is empty." -ForegroundColor Red; exit 1 }

# 2) Network check before starting Maven
Write-Host "Checking $DbHost`:$DbPort ..." -ForegroundColor Cyan
try {
    $dns = Resolve-DnsName -Name $DbHost -ErrorAction Stop | Select-Object -First 1
    Write-Host "  DNS ok: $($dns.IPAddress)" -ForegroundColor Green
} catch {
    Write-Host "  DNS failed: this host cannot be resolved." -ForegroundColor Red
    exit 1
}
$tcp = Test-NetConnection -ComputerName $DbHost -Port $DbPort -WarningAction SilentlyContinue
if (-not $tcp.TcpTestSucceeded) {
    Write-Host "  MySQL is not accepting connections on this host and port. Start the MySQL service and check its port." -ForegroundColor Red
    exit 1
}
Write-Host "  TCP ok" -ForegroundColor Green

# 3) Environment for Spring Boot
$env:DB_URL = "jdbc:mysql://$DbHost`:$DbPort/powerbank_stations?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC"
$env:DB_USER = $DbUser
$env:DB_PASSWORD = $password
$env:JWT_SECRET = $JwtSecret

# 4) Start the backend
Write-Host "Starting backend on http://localhost:8080 ..." -ForegroundColor Cyan
mvn clean spring-boot:run
