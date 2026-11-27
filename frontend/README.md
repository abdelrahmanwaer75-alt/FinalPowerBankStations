# PowerBank Stations - Frontend

Static HTML, CSS, and JavaScript. Talks to the Spring Boot backend in `../backend`.

## Run
1. Start the backend (see `../backend/README.md`) on port 8080.
2. Serve this folder on port 3000 (the backend allows this origin):
       npx serve -l 3000 .
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
| `pages/customer/map.html` | GET /stations, /stations/{id}/availability, /pricing/active, POST /reservations | Station map and reservation entry point. |
| `pages/customer/stations.html` | Redirect to `map.html` | Compatibility link for the old station-list URL. |
| `pages/customer/login.html` | POST /auth/login, GET /admin/stations | Shared customer/admin login; the backend checks staff access for Admin mode. |
| `pages/customer/register.html` | POST /auth/register | Required phone input, password checklist, then email verification. |
| `pages/customer/rentals.html` | GET /reservations/active, /rentals/active, /rentals, POST /rentals/start, DELETE /reservations/{id}, POST /returns | Rental and reservation history. |
| `pages/customer/verify.html` | POST /auth/verify, /auth/resend-verification | Account verification using a one-time code. |
| `pages/customer/forgot-password.html`, `reset-password.html` | POST /auth/forgot-password, /auth/reset-password | Password recovery. |
| `pages/customer/return-photos.html`, `return-inspection.html` | POST /returns/{id}/images, GET /returns/{id}, /returns/{id}/status | Uploads return photos and displays review status. |
| `pages/customer/route.html` | OpenStreetMap, Nominatim, and OSRM | Station map, address lookup, driving route; walking is an approximate direct line. |
| `pages/admin/admin.html` | GET/POST/PUT /admin/stations, POST /admin/stations/{id}/powerbanks | Station management. |
| `pages/admin/admin-dashboard.html` | GET /admin/stations, GET /admin/returns | Live operational overview. |
| `pages/admin/admin-inventory.html` | GET /admin/stations | Station capacity and availability; no per-device API exists. |
| `pages/admin/admin-returns.html` | GET /admin/returns and review actions | Staff return-review queue. |
| `pages/admin/admin-pricing.html` | GET/POST /admin/pricing, POST /admin/pricing/{id}/activate | Create and activate pricing plans. |
| Other `pages/admin/` screens | No matching read/write API yet | Maintenance, audit, reports, settings, and user roles show clear backend-unavailable states. |

## Shared files
- `js/api.js`: the only place that calls `fetch`. Drops the token on a 401 response.
- `js/common.js`: sign-in state, station loading, reserving, and small helpers.
- `css/tokens.css`: design tokens for light and dark themes.

## Remaining gaps
- The Figma file contains additional admin views for maintenance, audit, reports, and settings. There are no matching backend endpoints for those flows yet, so they are not presented as working features.
- Device-level inventory actions are not available; the current API exposes station capacity and availability counts only.
- Arabic (RTL) layout and full dark-theme coverage are not implemented across every page.
- Some customer flows still need a final visual comparison with each responsive Figma frame; their in-app links now lead to the intended customer workflows.
