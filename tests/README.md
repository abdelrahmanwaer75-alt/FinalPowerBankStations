# Tests

## Smoke test (end to end, against the running backend)
Start the backend on port 8080 first, then from this folder in PowerShell:

    .\smoke-test.ps1

Optional arguments:
- `-MysqlExe "C:\...\mysql.exe" -MysqlUser root -MysqlPassword "..."` : also runs the admin tests
  (grants a staff role to a new test account in MySQL, then checks the approval flow and audit log).
- `-PhotoPath "C:\path\photo.jpg"` : also uploads a photo to the return.

The script prints PASS or FAIL for every check, and exits with code 1 if any check fails.
Each run creates new accounts with unique emails, so it can be repeated.

If a check fails, send the FAIL line and the server's error message.

## Not included yet
- Backend unit tests (JUnit): they need Maven to compile the project first.
- Frontend browser tests: need a browser automation tool.
