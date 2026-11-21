# PowerBank Stations - Backend

## Run (Windows PowerShell)
    cd G:\project\PowerBankStations\backend
    .\run-local.ps1

Install and start MySQL 8, then initialize the local database with `database/schema.sql`
and `database/sample_data.sql` from the project root. The script prompts for the MySQL
password and starts the backend on http://localhost:8080.

## Configuration
- DB_URL, DB_USER, DB_PASSWORD, JWT_SECRET are set by `run-local.ps1`.
- Recovery and verification codes print to the backend console by default. To email them, set `MAIL_ENABLED=true`, `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, and `MAIL_FROM` before starting the app. For Gmail use an App Password. See `../RUN_COMMANDS.md` for PowerShell commands.
- Phone numbers still use the local console flow; SMS needs a separate provider.
- Hibernate does not change the schema (ddl-auto: none).

## First Super Admin
1. Register the account in the customer app and verify it so its status is `ACTIVE`.
2. Run `mysql -u root -p powerbank_stations < database/make_admin.sql`. The script promotes
   `0009ardh@gmail.com` to `SUPER_ADMIN` and can be run more than once safely.
3. Sign in through the separate Admin portal.

## Notes
- Do not commit passwords. The password is only typed at the prompt.
- Keep local database credentials out of source control.
