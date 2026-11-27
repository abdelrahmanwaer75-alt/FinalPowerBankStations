# PowerBank Stations - Frontend

Static HTML, CSS, and JavaScript. Talks to the Spring Boot backend in `../backend`.

## Run
1. Start the backend (see `../backend/README.md`) on port 8080.
2. Serve this folder on port 3000 (the backend allows this origin). From the project root, `run-project.ps1` starts both servers and applies pending database migrations. For a frontend-only session, run `python -m http.server 3000 --bind 127.0.0.1` from this folder.
3. Open http://localhost:3000/pages/ (customer landing page), or open an individual page under `/pages/customer/` or `/pages/admin/`.

## Page organization
- All HTML is under `pages/`.
- Customer pages are under `pages/customer/`.
- Staff pages are under `pages/admin/`.
- Shared styles and scripts stay in `css/` and `js/` at the frontend root.

## Configuration (`js/config.js`)
- `API_BASE_URL`: backend address, default `http://localhost:8080/api`.
- Station maps and location pages use Leaflet with OpenStreetMap; no Google Maps key is needed.

## Pages and what they use
| Page | Connected to | Notes |
|---|---|---|
| `pages/customer/index.html` | GET /stations, /stations/{id}/availability, /pricing/active | Redesigned customer landing page. The primary actions open the map or account flow; network counters are loaded live. |
| `pages/customer/map.html` | GET /stations, /stations/{id}/availability, /pricing/active, POST /reservations, POST/GET/DELETE /waitlist | Leaflet/OpenStreetMap map, station availability, reservations, and waiting list. |
| `pages/customer/station-details.html` | GET /stations/{id}, /availability, /pricing/active, waitlist endpoints, POST /reservations | Station details, reservation duration, and waiting-list controls. |
| `pages/customer/stations.html` | Redirect to `map.html` | Compatibility link for the old station-list URL. |
| `pages/customer/login.html` | POST /auth/login, GET /admin/stations | Shared customer/admin login; the backend checks staff access for Admin mode. |
| `pages/customer/register.html` | POST /auth/register | Required phone input, password checklist, then email verification. |
| `pages/customer/active-rental.html` | GET /reservations/active, /rentals/active, /stations, POST /rentals/start, DELETE /reservations/{id} | One page for holds and active rentals, including switching between multiple PowerBanks. |
| `pages/customer/rentals.html` | GET /reservations/active, /rentals/active, /rentals, POST /rentals/start, DELETE /reservations/{id} | Completed rental history and reservation shortcuts. |
| `pages/customer/verify.html` | POST /auth/verify, /auth/resend-verification | Account verification using a one-time code. |
| `pages/customer/forgot-password.html`, `reset-password.html` | POST /auth/forgot-password, /auth/reset-password | Password recovery. |
| `pages/customer/return-photos.html`, `return-inspection.html` | POST /returns/{id}/images, GET /returns/{id}, /returns/{id}/status | Uploads return photos and displays review status. |
| `pages/customer/route.html` | OpenStreetMap, Nominatim, and OSRM | Station map, address lookup, driving route; walking is an approximate direct line. |
| `pages/admin/admin.html` | GET/POST/PUT /admin/stations, POST /admin/stations/{id}/powerbanks | Station management. |
| `pages/admin/admin-dashboard.html` | GET /admin/stations, GET /admin/returns | Live operational overview. |
| `pages/admin/admin-inventory.html` | GET /admin/stations | Station capacity and availability; no per-device API exists. |
| `pages/admin/admin-returns.html` | GET /admin/returns and review actions | Staff return-review queue using the backend's `REQUIRES_MANUAL_REVIEW` status. |
| `pages/admin/admin-pricing.html` | GET/POST /admin/pricing, POST /admin/pricing/{id}/activate | Create and activate pricing plans. |
| `pages/admin/admin-users.html` | GET/POST /admin/account-requests, approve/reject requests | Staff request accounts; only the Super Admin can approve and create them. |
| Other `pages/admin/` screens | No matching read/write API yet | Maintenance, audit, reports, and settings show clear backend-unavailable states. |

The old `pages/customer/reserve.html` address redirects to `active-rental.html` so holds and rentals stay in one place.

For Render, set `API_BASE_URL` to the HTTPS origin of the deployed backend. Also set the backend's `CORS_ALLOWED_ORIGINS` to the deployed frontend origin. The `render-build.sh` script writes the API URL into `js/config.js` using a path relative to the script itself.

## Shared files
- `js/api.js`: the only place that calls `fetch`. Drops the token on a 401 response.
- `js/common.js`: sign-in state, station loading, reserving, and small helpers.
- `css/tokens.css`: design tokens for light and dark themes.

## Remaining gaps
- The Figma file contains additional admin views for maintenance, audit, reports, and settings. There are no matching backend endpoints for those flows yet, so they are not presented as working features.
- Device-level inventory actions are not available; the current API exposes station capacity and availability counts only.
- Arabic (RTL) layout and full dark-theme coverage are not implemented across every page.
- Some customer flows still need a final visual comparison with each responsive Figma frame; their in-app links now lead to the intended customer workflows.
