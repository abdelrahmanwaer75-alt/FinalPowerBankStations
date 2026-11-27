param(
    [int]$MySqlPort = 3306,
    [int]$BackendPort = 8080,
    [int]$FrontendPort = 3000,
    [string]$DbHost = 'localhost',
    [string]$DbUser = 'root',
    [string]$MySqlExe = ''
)

$ErrorActionPreference = 'Stop'
$projectRoot = $PSScriptRoot
$backendDir = Join-Path $projectRoot 'backend'
$frontendDir = Join-Path $projectRoot 'frontend'

function Test-LocalPort([int]$Port) {
    return Test-NetConnection -ComputerName 127.0.0.1 -Port $Port -InformationLevel Quiet -WarningAction SilentlyContinue
}

# Start the installed local MySQL service when it is not already listening.
if (-not (Test-LocalPort $MySqlPort)) {
    $mysqlServices = @(
        Get-Service -Name 'MySQL*' -ErrorAction SilentlyContinue
        Get-Service -Name 'MariaDB*' -ErrorAction SilentlyContinue
    ) | Sort-Object Name -Unique
    $mysqlService = $mysqlServices | Where-Object Status -eq 'Running' | Select-Object -First 1
    if (-not $mysqlService) { $mysqlService = $mysqlServices | Select-Object -First 1 }
    if (-not $mysqlService) {
        throw "No MySQL service was found and port $MySqlPort is closed. Install MySQL Server 8 or start your MySQL service, then run this script again."
    }
    if ($mysqlService.Status -ne 'Running') {
        Write-Host "Starting MySQL service '$($mysqlService.Name)'..." -ForegroundColor Cyan
        try { Start-Service -Name $mysqlService.Name }
        catch { throw "Could not start MySQL service '$($mysqlService.Name)'. Open PowerShell as Administrator, run this script again, or start the MySQL service manually. $($_.Exception.Message)" }
    }
    $deadline = (Get-Date).AddSeconds(30)
    while (-not (Test-LocalPort $MySqlPort) -and (Get-Date) -lt $deadline) { Start-Sleep -Seconds 1 }
    if (-not (Test-LocalPort $MySqlPort)) { throw "MySQL did not start listening on port $MySqlPort within 30 seconds." }
}

if (-not (Get-Command python -ErrorAction SilentlyContinue)) {
    throw 'Python was not found. Install Python 3 and make sure python.exe is on PATH.'
}

$backendAlreadyRunning = Test-LocalPort $BackendPort
$restoreDbPassword = $env:DB_PASSWORD
$hadDbPassword = Test-Path Env:DB_PASSWORD

try {
    if (-not $backendAlreadyRunning) {
        Write-Host 'Applying pending database migrations...' -ForegroundColor Cyan
        $migrationScript = Join-Path $projectRoot 'database\run-migrations.ps1'
        $migrationArgs = @{
            DbHost = $DbHost
            DbPort = $MySqlPort
            DbUser = $DbUser
        }
        if ($MySqlExe) { $migrationArgs.MySqlExe = $MySqlExe }
        & $migrationScript @migrationArgs

        Write-Host "Opening backend console on port $BackendPort..." -ForegroundColor Cyan
        $backendCommand = "& .\run-local.ps1 -DbHost '$DbHost' -DbUser '$DbUser' -DbPort $MySqlPort"
        Start-Process -FilePath 'powershell.exe' -WorkingDirectory $backendDir -ArgumentList @(
            '-NoExit', '-ExecutionPolicy', 'Bypass', '-Command', $backendCommand
        )
    } else {
        Write-Host "Port $BackendPort is already in use; the running backend was left alone and migrations were skipped. Stop it and rerun this script to apply database changes." -ForegroundColor Yellow
    }
} finally {
    # Pass the password only to the backend process; don't leave it exposed to the frontend server.
    if ($hadDbPassword) { $env:DB_PASSWORD = $restoreDbPassword }
    else { Remove-Item Env:DB_PASSWORD -ErrorAction SilentlyContinue }
}

if (-not (Test-LocalPort $FrontendPort)) {
    Write-Host "Opening frontend console on port $FrontendPort..." -ForegroundColor Cyan
    Start-Process -FilePath 'powershell.exe' -WorkingDirectory $frontendDir -ArgumentList @(
        '-NoExit', '-Command', "python -m http.server $FrontendPort --bind 127.0.0.1"
    )
} else { Write-Host "Port $FrontendPort is already in use; leaving the existing process alone." -ForegroundColor Yellow }

Write-Host ''
Write-Host 'Startup requested. Check both new PowerShell windows for errors.' -ForegroundColor Green
Write-Host "Customer site: http://localhost:$FrontendPort/pages/customer/index.html"
Write-Host "Admin login:   http://localhost:$FrontendPort/pages/customer/login.html?mode=admin"
Write-Host "Backend API:   http://localhost:$BackendPort"
Write-Host 'MySQL data is not reinitialized or deleted by this script.' -ForegroundColor Green
