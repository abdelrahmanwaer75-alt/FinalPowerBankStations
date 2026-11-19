# PowerBank Stations - Project

Current state. Read this before running anything.

## Verified
- Database: `database/schema.sql` (24 tables) and `sample_data.sql` load on MariaDB 10.11.
  The constraint tests (open-reservation uniqueness, one active rental per customer, append-only audit log,
  maintenance target check, station delete behaviour) passed on MariaDB 10.11. Not run on MySQL 8 yet.
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
1. `mysql -u root -p < database/schema.sql` then `mysql -u root -p powerbank_stations < database/sample_data.sql`
2. `cd backend && mvn spring-boot:run` (set `DB_USER`, `DB_PASSWORD`, `JWT_SECRET`)
3. Register an account in the app, edit the email in `database/make_admin.sql`, run it.
4. `cd frontend && npx serve -l 3000 .` then open http://localhost:3000/map.html

See `backend/README.md` and `frontend/README.md` for details.
