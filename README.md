# PowerBank Stations - Project

Current state. Read this before running anything.

## Verified
- Database: MySQL 8 schema and sample data are in `database/`.
- Frontend: all JavaScript files pass a syntax check. Every page link and script path exists.
  Every frontend API call matches a backend route (static check).

## Not verified
- Backend (`backend/`, 76 Java files): NEVER compiled or started. Maven Central was unreachable from the
  build environment. Expect small compile errors. Send the first error lines and they will be fixed.
- Frontend in a browser: never opened or clicked through.
- End to end flow: not tested.
- Google Maps: needs a NEW browser key in `frontend/js/config.js`. Keys pasted in chat must be deleted.

## Scope
- Customer flow in the backend: register, login, stations, reserve, start rental, return, upload photos.
- Admin in the backend: return approval/rejection/manual review, station and power-bank management,
  pricing plans, audit log entries for admin changes. Staff check uses the `user_roles` table.
- Frontend pages connected: map, login/register, stations list, My Rental (rentals, history, return photos),
  admin (stations and returns queue).
- Not built: about 34 of the 41 screens in the spec (welcome, verification, password reset, route planning,
  dashboard, profile, most admin screens), customer-facing audit, roles management, maintenance,
  inspection records, reports, settings, Arabic (RTL), dark mode on every page.

## First run
1. Start MySQL 8, then load `database/schema.sql` followed by `database/sample_data.sql`.
2. Start the backend with `cd backend; .\run-local.ps1` and provide MySQL credentials.
3. Register and verify `0009ardh@gmail.com`, then run `mysql -u root -p powerbank_stations < database/make_admin.sql` to grant the first `SUPER_ADMIN` role.
4. `cd frontend && npx serve -l 3000 .` then open http://localhost:3000/pages/ for the customer site. Admin screens live under `/pages/admin/`.

See `backend/README.md` and `frontend/README.md` for details.
