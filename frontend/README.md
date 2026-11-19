# PowerBank Stations - Frontend

Static HTML, CSS, and JavaScript. Talks to the Spring Boot backend in `../backend`.

## Run
1. Start the backend (see `../backend/README.md`) on port 8080.
2. Serve this folder on port 3000 (the backend allows this origin):
       npx serve -l 3000 .
3. Open http://localhost:3000/map.html

## Configuration (`js/config.js`)
- `API_BASE_URL`: backend address, default `http://localhost:8080/api`.
- `GOOGLE_MAPS_BROWSER_KEY`: paste a NEW browser key restricted to your referrer and Maps JavaScript API.
  Keys that were pasted in chat are compromised and must be deleted in Google Cloud.

## Pages and what they use
| Page | Connected to | Notes |
|---|---|---|
| map.html | GET /stations, /stations/{id}/availability, /pricing/active, POST /reservations | Real Google Maps. Falls back to sample data with a banner if the backend is down. |
| login.html | POST /auth/login | Built from the Figma "Customer Login - Desktop" frame. "Keep me signed in" stores the token in localStorage, otherwise sessionStorage. |
| register.html | POST /auth/register | Same visual style as login. Not yet checked against a Figma frame. Registration keeps the user signed in. |
| stations.html | GET /stations, POST /reservations | Search and filters run in the browser. |
| rentals.html | GET /reservations/active, /rentals/active, /rentals, POST /rentals/start, DELETE /reservations/{id}, POST /returns | Live cost refreshes every 30 seconds. Login required. |
| admin.html | GET /admin/stations, POST/PUT /admin/stations, POST /admin/stations/{id}/powerbanks, GET /admin/returns, approve / reject / manual-review | Staff account only. Shows "staff access required" for other accounts. |
| index.html | nothing | Static landing page. |

## Shared files
- `js/api.js`: the only place that calls `fetch`. Drops the token on a 401 response.
- `js/common.js`: sign-in state, station loading, reserving, and small helpers.
- `js/header.js`: shared header, theme and language preferences (not yet used on every page).
- `css/tokens.css`: design tokens for light and dark themes.

## Not done yet
- Pages from the Figma design that are missing (about 32 of 41). Forgot password is shown but disabled (no page yet).
- Admin pages other than stations and returns review (pricing, customers, roles, maintenance, audit, reports, settings).
- Arabic (RTL) layout.
- Dark mode is defined but not applied on every page.
- Stations page still uses the prototype layout, not the Figma design.
