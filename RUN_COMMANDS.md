# PowerBank Stations — Run Commands (Windows)

## Start the whole project with one command

From the project root in PowerShell:

```powershell
Set-Location -LiteralPath 'G:\project\PowerBankStations'
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-project.ps1
```

This starts the installed MySQL service if needed, then opens separate backend and frontend consoles. Enter the MySQL password in the backend console. It does not recreate or erase the database. If Windows denies starting the MySQL service, open PowerShell as Administrator or start MySQL from Services first.

## Send account recovery codes by email

Without SMTP settings, the local demo prints the code in the backend PowerShell window. To send codes to email, set these variables in the same PowerShell window before running `run-project.ps1` (use a Gmail App Password, not the account password):

```powershell
$env:MAIL_ENABLED = 'true'
$env:MAIL_HOST = 'smtp.gmail.com'
$env:MAIL_PORT = '587'
$env:MAIL_USERNAME = 'your-sender@gmail.com'
$env:MAIL_PASSWORD = 'your-16-character-app-password'
$env:MAIL_FROM = 'your-sender@gmail.com'
```

These values apply only to that PowerShell session. Do not put real mail passwords in project files or source control. If these settings are missing, use the recovery code printed in the backend window. SMS delivery needs a separate SMS provider and is not configured.

## 1. Start MySQL and load the database (first setup only)

Start the MySQL 8 service, open **MySQL Command Line Client**, and log in. At the `mysql>` prompt run:

```sql
SOURCE G:/project/PowerBankStations/database/schema.sql;
SOURCE G:/project/PowerBankStations/database/sample_data.sql;
```

**Warning:** `schema.sql` drops and recreates `powerbank_stations`. Do not run it again if you need to keep existing data.

## 2. Grant the first Super Admin

Register and verify `0009ardh@gmail.com` in the app first. In MySQL Command Line Client run:

```sql
SOURCE G:/project/PowerBankStations/database/make_admin.sql;
```

The admin login uses that account's existing password. Passwords are stored as hashes; this project does not define a default admin password.

## Add the requested local admin account

In **MySQL Command Line Client** at the `mysql>` prompt, run:

```sql
SOURCE G:/project/PowerBankStations/database/add_admin_009ardh.sql;
```

This script safely creates or updates `009ardh@gmail.com`, activates it, assigns `SUPER_ADMIN`, and stores the password as a BCrypt hash. It does not recreate the database.

## 3. Run the backend (PowerShell window 1)

```powershell
cd G:\project\PowerBankStations\backend
Set-ExecutionPolicy -Scope Process -ExecutionPolicy Bypass
.\run-local.ps1
```

Enter the MySQL `root` password when prompted. Keep this window open. The API listens on `http://localhost:8080`.

## 4. Run the frontend (PowerShell window 2)

```powershell
cd G:\project\PowerBankStations\frontend
python -m http.server 3000 --bind 127.0.0.1
```

Keep this window open. Open these pages in your browser:

- Customer home: <http://localhost:3000/pages/customer/index.html>
- Shared sign-in page: <http://localhost:3000/pages/customer/login.html> (choose Customer or Admin)

## Update an existing database

Stop the backend first. In the MySQL monitor (`mysql>` prompt, not PowerShell), select the project database and apply the migration once:

```sql
USE powerbank_stations;
SOURCE G:/project/PowerBankStations/database/migrations/2026-11-21-minute-pricing-and-multiple-active-rentals.sql;
```

The migration converts stored hourly rates to their equivalent per-minute rates, preserves finished rental totals, and removes the old one-active-rental-per-user database key. Start the backend again after the migration completes.

Press `Ctrl+C` in a server window to stop that server.

## If MySQL is already initialized

Skip section 1. Do not re-run `schema.sql`; it deletes the existing local database before recreating it.
