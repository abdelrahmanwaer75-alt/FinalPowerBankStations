# Tests

## Smoke test (end to end, against the running backend)
Start the backend on port 8080 first, then from this folder in PowerShell:

    .\smoke-test.ps1

Options:
- `-PhotoPath "C:\path\photo.jpg"`: also uploads a photo to the return.
- `-SkipAdmin`: skips the admin checks.

The admin checks create a new admin account and print one SQL statement. Run it in the Supabase SQL editor,
then press Enter. The script prints PASS or FAIL for every check and exits with code 1 if any check fails.
Each run creates new accounts with unique emails, so it can be repeated.

## Not included yet
- Backend unit tests (JUnit): the copied data-structures tests are empty placeholders.
- Frontend browser tests.
