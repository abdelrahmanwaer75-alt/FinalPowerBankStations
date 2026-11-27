<#
Runs unapplied SQL migrations from database/migrations against the local MySQL database.
Applied migration files are immutable: edit by adding a new migration instead.
#>
param(
    [string]$DbHost = 'localhost',
    [int]$DbPort = 3306,
    [string]$DbUser = 'root',
    [ValidatePattern('^[A-Za-z0-9_]+$')]
    [string]$DatabaseName = 'powerbank_stations',
    [string]$MySqlExe = ''
)

$ErrorActionPreference = 'Stop'
$migrationsDir = Join-Path $PSScriptRoot 'migrations'

function Resolve-MySqlClient {
    param([string]$RequestedPath)

    function Resolve-ClientFile([string]$Path) {
        if ($Path -and (Test-Path -LiteralPath $Path -PathType Leaf)) {
            return (Resolve-Path -LiteralPath $Path).Path
        }
        return $null
    }

    function Find-ClientInHome([string]$InstallRoot) {
        if ([string]::IsNullOrWhiteSpace($InstallRoot)) { return $null }
        foreach ($candidate in @(
            (Join-Path $InstallRoot 'bin\mysql.exe'),
            (Join-Path $InstallRoot 'mysql.exe'),
            (Join-Path $InstallRoot 'bin\mariadb.exe')
        )) {
            $found = Resolve-ClientFile $candidate
            if ($found) { return $found }
        }
        return $null
    }

    if ($RequestedPath) {
        $requestedClient = Resolve-ClientFile $RequestedPath
        if (-not $requestedClient) {
            throw "MySQL client was not found at '$RequestedPath'."
        }
        return $requestedClient
    }

    if (-not [string]::IsNullOrWhiteSpace($env:MYSQL_EXE)) {
        $environmentClient = Resolve-ClientFile $env:MYSQL_EXE
        if (-not $environmentClient) {
            throw "MYSQL_EXE points to a file that does not exist: '$env:MYSQL_EXE'."
        }
        return $environmentClient
    }

    $homeClient = Find-ClientInHome $env:MYSQL_HOME
    if ($homeClient) { return $homeClient }

    $candidateFiles = New-Object 'System.Collections.Generic.List[string]'
    if ($env:ChocolateyInstall) {
        [void]$candidateFiles.Add((Join-Path $env:ChocolateyInstall 'bin\mysql.exe'))
        [void]$candidateFiles.Add((Join-Path $env:ChocolateyInstall 'bin\mariadb.exe'))
    }
    if ($env:ProgramData) {
        [void]$candidateFiles.Add((Join-Path $env:ProgramData 'chocolatey\bin\mysql.exe'))
        [void]$candidateFiles.Add((Join-Path $env:ProgramData 'chocolatey\bin\mariadb.exe'))
    }
    if ($env:USERPROFILE) {
        [void]$candidateFiles.Add((Join-Path $env:USERPROFILE 'scoop\apps\mysql\current\bin\mysql.exe'))
        [void]$candidateFiles.Add((Join-Path $env:USERPROFILE 'scoop\apps\mariadb\current\bin\mariadb.exe'))
    }
    if ($env:SystemDrive) {
        foreach ($product in @('xampp', 'wamp64', 'laragon')) {
            foreach ($client in @('mysql.exe', 'mariadb.exe')) {
                [void]$candidateFiles.Add((Join-Path $env:SystemDrive "$product\mysql\bin\$client"))
            }
        }
    }
    foreach ($candidate in $candidateFiles) {
        $found = Resolve-ClientFile $candidate
        if ($found) { return $found }
    }

    $command = Get-Command mysql.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }
    $command = Get-Command mariadb.exe -ErrorAction SilentlyContinue | Select-Object -First 1
    if ($command) { return $command.Source }

    foreach ($programRoot in @($env:ProgramFiles, ${env:ProgramFiles(x86)})) {
        if (-not $programRoot -or -not (Test-Path -LiteralPath $programRoot)) { continue }
        $databaseFolders = Get-ChildItem -LiteralPath $programRoot -Directory -ErrorAction SilentlyContinue |
            Where-Object { $_.Name -match '^(MySQL|MariaDB)(\s|$)' }
        foreach ($folder in $databaseFolders) {
            $found = Find-ClientInHome $folder.FullName
            if ($found) { return $found }
        }
    }

    $servicesRegistryPath = 'HKLM:\SYSTEM\CurrentControlSet\Services'
    if (Test-Path -LiteralPath $servicesRegistryPath) {
        $serviceKeys = Get-ChildItem -LiteralPath $servicesRegistryPath -ErrorAction SilentlyContinue |
            Where-Object { $_.PSChildName -match '^(MySQL|MariaDB)' }
        foreach ($serviceKey in $serviceKeys) {
            $imagePath = (Get-ItemProperty -LiteralPath $serviceKey.PSPath -Name ImagePath -ErrorAction SilentlyContinue).ImagePath
            if (-not $imagePath) { continue }
            $match = [regex]::Match($imagePath, '^\s*"?([^"\s]+\.exe)', [System.Text.RegularExpressions.RegexOptions]::IgnoreCase)
            if ($match.Success) {
                $serverFolder = Split-Path -Parent $match.Groups[1].Value
                foreach ($clientName in @('mysql.exe', 'mariadb.exe')) {
                    $found = Resolve-ClientFile (Join-Path $serverFolder $clientName)
                    if ($found) { return $found }
                }
                $found = Find-ClientInHome (Split-Path -Parent $serverFolder)
                if ($found) { return $found }
            }
        }
    }

    $message = @'
MySQL command-line client was not found. Install MySQL Server (which includes the client), add its bin folder to PATH, or set MYSQL_EXE to mysql.exe/mariadb.exe. You can also pass -MySqlExe 'C:\path\to\mysql.exe'.
'@
    throw $message.Trim()
}

function Invoke-MySql {
    param(
        [Parameter(Mandatory = $true)][string]$Sql,
        [switch]$Quiet
    )

    $startInfo = New-Object System.Diagnostics.ProcessStartInfo
    $startInfo.FileName = $script:mysqlClient
    $startInfo.Arguments = "--host=`"$DbHost`" --port=$DbPort --user=`"$DbUser`" --database=`"$DatabaseName`" --batch --skip-column-names"
    if ($Quiet) { $startInfo.Arguments += ' --silent' }
    $startInfo.UseShellExecute = $false
    $startInfo.CreateNoWindow = $true
    $startInfo.RedirectStandardInput = $true
    $startInfo.RedirectStandardOutput = $true
    $startInfo.RedirectStandardError = $true

    $process = New-Object System.Diagnostics.Process
    $process.StartInfo = $startInfo
    [void]$process.Start()
    $process.StandardInput.Write($Sql)
    $process.StandardInput.Close()
    $stdout = $process.StandardOutput.ReadToEnd()
    $stderr = $process.StandardError.ReadToEnd()
    $process.WaitForExit()

    if ($process.ExitCode -ne 0) {
        $details = ($stderr + "`n" + $stdout).Trim()
        throw "MySQL command failed (exit $($process.ExitCode)): $details"
    }
    return $stdout.Trim()
}

function Get-FileChecksum {
    param([string]$Path)

    $sha256 = [System.Security.Cryptography.SHA256]::Create()
    try {
        $bytes = $sha256.ComputeHash([System.IO.File]::ReadAllBytes($Path))
        return [System.BitConverter]::ToString($bytes).Replace('-', '').ToLowerInvariant()
    } finally {
        $sha256.Dispose()
    }
}

function Add-BaselineMigration {
    param([System.IO.FileInfo]$MigrationFile)

    $name = $MigrationFile.Name.Replace("'", "''")
    $checksum = Get-FileChecksum -Path $MigrationFile.FullName
    [void](Invoke-MySql -Quiet -Sql "INSERT IGNORE INTO schema_migrations (migration_name, checksum) VALUES ('$name', '$checksum');")
    Write-Host "  Baseline recorded: $($MigrationFile.Name)"
}

$previousMysqlPassword = $env:MYSQL_PWD
$password = $env:DB_PASSWORD
if ([string]::IsNullOrWhiteSpace($password)) {
    $securePassword = Read-Host 'MySQL database password' -AsSecureString
    $passwordPointer = [Runtime.InteropServices.Marshal]::SecureStringToBSTR($securePassword)
    try { $password = [Runtime.InteropServices.Marshal]::PtrToStringBSTR($passwordPointer) }
    finally { [Runtime.InteropServices.Marshal]::ZeroFreeBSTR($passwordPointer) }
}
if ([string]::IsNullOrWhiteSpace($password)) { throw 'MySQL password is empty.' }

# Leave DB_PASSWORD available to the backend launcher; keep the MySQL CLI password out of arguments.
$env:DB_PASSWORD = $password
$script:mysqlClient = Resolve-MySqlClient -RequestedPath $MySqlExe
$env:MYSQL_PWD = $password

try {
    if (-not (Test-Path -LiteralPath $migrationsDir -PathType Container)) {
        throw "Migration folder is missing: $migrationsDir"
    }

    # The migration ledger only contains application schema migrations, not seed or schema reset scripts.
    $ledgerWasPresent = [int](Invoke-MySql -Quiet -Sql "SELECT COUNT(*) FROM information_schema.tables WHERE table_schema = DATABASE() AND table_name = 'schema_migrations';") -gt 0
    [void](Invoke-MySql -Quiet -Sql @'
CREATE TABLE IF NOT EXISTS schema_migrations (
    migration_name VARCHAR(255) NOT NULL PRIMARY KEY,
    checksum CHAR(64) NOT NULL,
    applied_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
) ENGINE=InnoDB;
'@)
    $migrationFiles = @(Get-ChildItem -LiteralPath $migrationsDir -Filter '*.sql' -File | Sort-Object Name)

    # When adopting a database without a ledger, detect whether the two older migrations
    # are already reflected in its columns. This avoids replaying legacy ALTER statements.
    if (-not $ledgerWasPresent) {
        $reservationLimitRemoved = [int](Invoke-MySql -Quiet -Sql @'
SELECT (
    NOT EXISTS(SELECT 1 FROM information_schema.statistics WHERE table_schema=DATABASE() AND table_name='reservations' AND index_name='uq_reservations_open_user')
    AND NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='reservations' AND column_name='open_user_key')
);
'@) -eq 1
        if ($reservationLimitRemoved) {
            $legacyFile = $migrationFiles | Where-Object Name -eq '2026-11-21-allow-multiple-user-rentals.sql' | Select-Object -First 1
            if ($legacyFile) { Add-BaselineMigration -MigrationFile $legacyFile }
        }

        $minutePricingInstalled = [int](Invoke-MySql -Quiet -Sql @'
SELECT (
    EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pricing_plans' AND column_name='price_per_minute')
    AND NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='pricing_plans' AND column_name='price_per_hour')
    AND NOT EXISTS(SELECT 1 FROM information_schema.columns WHERE table_schema=DATABASE() AND table_name='rentals' AND column_name='active_user_key')
);
'@) -eq 1
        if ($minutePricingInstalled) {
            $legacyFile = $migrationFiles | Where-Object Name -eq '2026-11-21-minute-pricing-and-multiple-active-rentals.sql' | Select-Object -First 1
            if ($legacyFile) { Add-BaselineMigration -MigrationFile $legacyFile }
        }
    }

    if ($migrationFiles.Count -eq 0) {
        Write-Host 'No SQL migrations found.'
    }

    foreach ($migration in $migrationFiles) {
        $name = $migration.Name.Replace("'", "''")
        $checksum = Get-FileChecksum -Path $migration.FullName
        $storedChecksum = Invoke-MySql -Quiet -Sql "SELECT checksum FROM schema_migrations WHERE migration_name = '$name';"

        if ($storedChecksum) {
            if ($storedChecksum.Trim() -ne $checksum) {
                throw "Applied migration '$($migration.Name)' was edited. Keep applied migrations unchanged and add a new .sql file for the follow-up change."
            }
            Write-Host "  Already applied: $($migration.Name)"
            continue
        }

        Write-Host "  Applying: $($migration.Name)"
        $sql = [System.IO.File]::ReadAllText($migration.FullName)
        [void](Invoke-MySql -Sql $sql)
        [void](Invoke-MySql -Quiet -Sql "INSERT INTO schema_migrations (migration_name, checksum) VALUES ('$name', '$checksum');")
        Write-Host "  Applied: $($migration.Name)"
    }

    Write-Host 'Database migrations are up to date.' -ForegroundColor Green
} finally {
    if ($null -eq $previousMysqlPassword) { Remove-Item Env:MYSQL_PWD -ErrorAction SilentlyContinue }
    else { $env:MYSQL_PWD = $previousMysqlPassword }
}
