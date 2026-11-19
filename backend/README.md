# PowerBank Stations - Backend (Spring Boot 3, Java 21)

Status: written, NOT compiled or run in the build environment (Maven Central was unreachable there).
Build and run it on your machine first. If the build fails, send the first error lines.

## Requirements
- JDK 21, Maven 3.9+, MySQL 8
- Load `../database/schema.sql` then `../database/sample_data.sql`

## Run
    export DB_USER=root
    export DB_PASSWORD=your_password
    export JWT_SECRET=$(openssl rand -base64 48)     # at least 32 characters
    export UPLOAD_DIR=./uploads                     # optional
    mvn spring-boot:run

Frontend: serve `../frontend` on port 3000 (CORS allows localhost:3000).

## Endpoints
Auth: all "token" endpoints need `Authorization: Bearer <token>`.

| Method | Path | Auth | What it does |
|---|---|---|---|
| POST | /api/auth/register | public | Create account, returns token |
| POST | /api/auth/login | public | Returns token |
| POST | /api/auth/logout | token | Stateless, returns 204 |
| GET | /api/stations | public | Active stations with available counts |
| GET | /api/stations/nearby?lat&lng&radiusKm | public | Stations within radius, closest first |
| GET | /api/stations/{id} | public | One active station |
| GET | /api/stations/{id}/availability | public | Available, rented, free return slots |
| POST | /api/reservations | token | Hold one PowerBank for 15 minutes |
| GET | /api/reservations/{id} | token | Own reservation only |
| DELETE | /api/reservations/{id} | token | Cancel a held reservation |
| POST | /api/rentals/start | token | Start rental from a reservation |
| GET | /api/rentals/active | token | Own active rentals with live cost |
| GET | /api/rentals/{id} | token | Own rental |
| POST | /api/returns | token | Return to a station, close rental, compute cost |
| GET | /api/returns/{id} | token | Own return with its photos |
| GET | /api/returns/{id}/status | token | Return status only |
| POST | /api/returns/{id}/images | token | Upload photos (multipart field `files`) |

| GET | /api/admin/returns?status=PENDING_REVIEW | staff | Returns waiting for a decision |
| POST | /api/admin/returns/{id}/approve | staff | Approve: PowerBank becomes AVAILABLE |
| POST | /api/admin/returns/{id}/reject | staff | Reject (reason required): PowerBank becomes UNAVAILABLE |
| POST | /api/admin/returns/{id}/manual-review | staff | Keep the PowerBank out of the pool for a person to check |
| GET | /api/admin/stations | staff | All stations with available counts |
| POST | /api/admin/stations | staff | Create a station |
| PUT | /api/admin/stations/{id} | staff | Update a station (capacity cannot drop below docked devices) |
| POST | /api/admin/stations/{id}/powerbanks | staff | Add devices to a station |
| GET | /api/admin/pricing | staff | All pricing plans |
| POST | /api/admin/pricing | staff | Create a plan (not active until activated) |
| POST | /api/admin/pricing/{id}/activate | staff | Make a plan the active one (others deactivate) |

Every admin change writes a row to `audit_logs` (action, entity, id, result). The table blocks UPDATE and DELETE.
Failed admin actions are not logged yet.

"staff" means the account has a SUPER_ADMIN, ADMIN, or STATION_MANAGER role in `user_roles`. The check is on the server.
To make an account staff: register it, then run `database/make_admin.sql` after editing the email in it.

Errors: `{ "code": "...", "message": "...", "fields": {...} }`.

## Business rules enforced
- One PowerBank can have only one open reservation (database unique key + row lock when reserving).
- One customer can have only one open reservation and one active rental (database unique keys).
- Held reservations expire after 15 minutes. A job runs every minute and releases the device.
- A rental copies the pricing into `rental_pricing_snapshots`. Later price changes do not change it.
- Returning to a full or inactive station is refused (`STATION_FULL`, `STATION_UNAVAILABLE`).
- A returned PowerBank becomes `RETURNED_PENDING_REVIEW` and is not available until an admin approves it.
- Photos: JPEG or PNG only, checked by file signature, max 5 MB each, max 6 per return. Files are stored
  outside the web root and are not served directly.

## Not built yet
- Admin: customers, roles and permissions management, maintenance requests, inspection records,
  audit log viewer, reports, settings. Logging of failed admin actions.
- Email/SMS verification and password reset.
- Waiting list, notifications, coupons, loyalty points, favorites, ratings.
- Photo download endpoint for the admin review screen.
