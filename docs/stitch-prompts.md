# PowerBank Stations - Google Stitch Prompts

How to use: copy one prompt into Stitch, generate the screen, then export or send the result back. Generate the desktop and mobile versions of each screen. Keep the same design system across all screens.

## Shared design system (applies to every screen)

Modern, clean, professional web app. Primary color deep blue (#1D4ED8), accent amber (#F59E0B), success green (#16A34A), warning amber (#D97706), danger red (#DC2626). Neutral background light gray (#F5F7FB), cards white with soft borders and 12px radius. Font: a clean sans-serif (Inter or similar). Provide light and dark themes. Layouts must work for desktop, tablet, and mobile. Support Arabic with right-to-left layout and English with left-to-right layout. Show loading, empty, and error states where relevant.

---

# CUSTOMER WEBSITE (20 screens)

## C1. Welcome Page
Design the landing page for PowerBank Stations, a shared phone-charging service. Top bar with logo, navigation (Find Stations, How It Works, FAQ), language switch, theme switch, Login and Register buttons. Hero section with a headline about never running out of battery, a short description, a "Get Started" button and an "Explore Stations" button, and a charging-related illustration. Then a three-step "How It Works" section (find, reserve, return), a benefits section with four cards, a "Featured Stations" row with three station cards, an FAQ accordion, and a footer with contact information and links.

## C2. Customer Login
Centered login card on a side-illustration layout. Fields: email or phone, password with show/hide toggle, "Remember me" checkbox. Primary "Log in" button, links for "Forgot password?" and "Create an account". Show an inline error state for wrong credentials. Include a language and theme switch in the top bar.

## C3. Customer Registration
Registration form card. Fields: full name, email, phone number, password (with show/hide and a strength meter), confirm password, and a terms and conditions checkbox. Primary "Create Account" button and a link back to "Log in". Show field-level validation messages for invalid email, weak password, and mismatched passwords.

## C4. Account Verification
Verification screen with a short instruction message, a six-digit code input made of separate boxes, a "Verify Account" button, a "Resend code" link that shows a countdown timer while disabled, and a "Back to registration" link. Show a success state and an invalid-code error state.

## C5. Forgot Password and Reset Password
Two states in one flow. State 1: email or phone input with "Send recovery code" button and a confirmation message. State 2: reset screen with a code or token input, new password field, confirm password field, and "Save new password" button. Show a success confirmation at the end.

## C6. Customer Dashboard
Logged-in home screen with a sidebar (Home, Find Stations, Stations Map, My Reservations, Active Rental, Rental History, My Profile). Top: greeting with the customer's name and a notification bell. Stat cards: active reservations, completed rentals, and current rental status. A prominent "Active Rental" card with elapsed time. Quick action buttons: Find a Station, Reserve a PowerBank, View Active Rental, View Rental History, Manage Profile. Sections for recent rental history (table), nearby stations (cards), recommended stations (cards), and an account notifications list.

## C7. Stations Map
Full-screen map view with a Google Maps-style canvas, a search bar for address or area at the top, a map and list view toggle, and zoom controls. Station markers on the map with a small popup card showing station name, address, available PowerBanks out of total, and status (open or closed). A side panel listing stations. Buttons on the popup: View Details, Get Directions, Reserve. Show a customer location dot.

## C8. Stations List
Searchable list of charging stations as cards. Each card has a station image, name, address, distance, available PowerBanks, total capacity, opening hours, a status badge, and buttons: View Details, View on Map, Get Directions, Reserve. Top filters: nearby, available PowerBanks, open now, and area or city dropdown. Sort dropdown and pagination controls at the bottom.

## C9. Station Details
Detail page for one station. Large station image, name, status badge, and address. A map section with the station location. Info panel with opening hours, contact information, total capacity, available PowerBanks, currently rented devices, and supported return options. Action buttons: Reserve a PowerBank (primary), View Location, Get Directions, and Back to Stations.

## C10. Route Planning
Map-based route screen. Left panel with a starting location input (with a "use my location" option), destination field showing the selected station, travel mode selector (walking or driving), and "Show Route" and "Open in Google Maps" buttons. The map shows the route line. Display estimated distance and estimated travel time in summary cards.

## C11. Reservation
Reservation form page. Summary card for the selected station with name, address, and available device count. Reservation duration selector, a reservation policy text box, and an applicable reservation fee line. Buttons: Confirm Reservation (primary) and Cancel and Return. Show a waiting-list card with a "Join waiting list" button in the state where no PowerBank is available.

## C12. Reservation Confirmation
Confirmation screen with a success icon and the heading "Reservation confirmed". Reservation reference number, status badge, station name and address, reservation date, expiration countdown, assigned PowerBank ID, and step-by-step collection instructions. Buttons: View Reservation, Start Rental, Cancel Reservation, Return to Dashboard.

## C13. Start Rental
Screen to start a rental. Reservation summary card, selected station, device information, and rental instructions. A large simulated QR code image with the label "Demo QR" and a text field for an activation code as an alternative. Buttons: Scan Demo QR Code, Enter Activation Code, and Confirm Start Rental (primary). Show a confirmation state after the rental starts.

## C14. Active Rental
Live rental monitoring screen. Large timer showing elapsed rental duration. Cards for rental ID, PowerBank ID, start time, pricing rule, and estimated cost that updates. Return instructions panel and a list of nearby return stations with a "Find Return Station" button. Buttons: View Rental Details and Start Return Process (primary).

## C15. Return PowerBank
Return station selection screen. Active rental summary at the top. List of return stations with name, address, free slots out of capacity, and a "Select" button for each. Instructions panel. Buttons: Confirm Return Location and Continue to Return Inspection. Show a disabled state for full or inactive stations with a reason label.

## C16. PowerBank Photo Upload
Photo upload screen for return inspection. PowerBank information card at the top. Upload area with drag-and-drop and a file picker, and the note "JPG or PNG, up to 5 MB each, maximum 6 photos". A grid of image previews, each with a remove button. Buttons: Upload Image, Submit Photos (primary), and Continue. Show validation errors for unsupported format, size, and count.

## C17. Return Inspection Result
Result screen. Return reference, the uploaded images in a gallery, and a status badge. Possible statuses to design: Pending Review (amber), Approved (green), Requires Manual Review (blue), Rejected (red). Administrator notes panel. Buttons: View Return Details, Contact Support, and Continue (enabled only when approved).

## C18. Return Confirmation
Completion screen with a success icon and the heading "Return completed". A receipt-style card with return ID, rental ID, returned PowerBank ID, return station, return date and time, final duration, final cost, and return status. Buttons: View Receipt, Go to Rental History, Return to Dashboard.

## C19. Rental History
Table of past and current rentals with columns for rental ID, PowerBank ID, pickup station, return station, start date, end date, duration, final cost, and status badge. Filters: date range picker, status dropdown, station dropdown. Row actions: View Details, View Return Record, View Receipt. Pagination and sortable column headers. Empty state with a "Find a station" button.

## C20. My Profile and Settings
Two-column profile page. Left: profile picture with change button, full name, email, phone, account creation date. Right: editable form fields and a Save Changes button. Settings section with Change Password, language selector, theme selector, and notification preferences toggles. Danger zone at the bottom with a Delete Account button and a confirmation dialog design. Logout button in the header menu.

---

# ADMIN DASHBOARD (21 screens)

Admin screens use a dark-navy sidebar with collapsible sections (Dashboard; Operations: Stations, PowerBanks, Inventory, Transfers; Bookings: Reservations, Rentals, Returns; Business: Pricing, Customers, Reports; Team: Roles and Permissions, Maintenance, Inspection Review, Infrastructure Inspections, Circuit Board Records; System: Audit Logs, Settings, Profile). The top bar has a search field, an account menu, and language and theme switches.

## A1. Admin Login
Centered admin login card with a different visual style from the customer login, labeled "Administrator Access". Fields: email or username, password with show/hide toggle. Primary Login button. Error state for invalid credentials or an account without admin permission.

## A2. Admin Dashboard
Overview page. Top row of stat cards: total stations, active stations, inactive stations, total PowerBanks, available, rented, under maintenance, active reservations, active rentals, completed returns, pending inspections. Charts: rentals over time (line chart), reservations over time (line chart), PowerBank status distribution (donut chart), station utilization (bar chart), and a revenue simulation card. Recent activity panel with tabs for reservations, rentals, returns, and maintenance requests. Quick action buttons: Add Station, Add PowerBank, View Reservations, View Maintenance Requests.

## A3. Analytics and Reports
Reports page with a left filter panel: date range, station, device, customer, and status. Report type tabs: rentals, reservations, returns, station performance, PowerBank utilization, customer activity, maintenance, simulated revenue. Main area: a chart and a data table. Top action buttons: Generate Report, Export CSV, Print Report.

## A4. Stations Management
Table of all stations with columns: station ID, name, address, city, total capacity, available PowerBanks, status badge, and opening hours. Top bar: search field, status filter, and Add Station primary button. Row actions: View, Edit, Deactivate, Delete (with a red icon). Pagination at the bottom.

## A5. Station Details Management
Management page for one station. Header with station name and status. Tabs: Overview, Location, Inventory, Reservations, Rentals, Maintenance History. Overview tab shows station information and a Google Maps-style location card. Inventory tab shows associated PowerBanks in a table. Buttons: Edit Station, Update Location, Change Operating Status.

## A6. PowerBank Management
Table of PowerBank devices with columns: PowerBank ID, serial number, model, current station, device status badge, registration date, last maintenance date. Status badge colors for Available, Reserved, Rented, Returned Pending Review, Under Maintenance, Unavailable, Retired. Top buttons: Add PowerBank and filters by station and status. Row actions: Edit, Assign to Station, Change Status, View History, Retire.

## A7. Inventory Management
Inventory overview. Cards or table per station with total capacity, available, reserved, rented, under maintenance, and remaining capacity. A stacked bar per station showing the status split. Filters: station selector and device search. Link to view inventory changes history.

## A8. Inventory Transfers
Transfer creation screen. Form: source station and destination station dropdowns, a multi-select list of eligible PowerBanks with a note that rented devices cannot be selected, transfer date, and notes. Buttons: Create Transfer, Confirm Transfer, Cancel Pending Transfer. Below: a transfer history table with status badges.

## A9. Reservations Management
Table of reservations with columns: reservation ID, customer, station, PowerBank, creation date, expiration time, and status badge (Pending, Confirmed, Active, Cancelled, Expired). Filters and search at the top. Tabs for all reservations and expired reservations. Row actions: View, Cancel (with a confirmation dialog design).

## A10. Rentals Management
Table of rentals with columns: rental ID, customer, PowerBank, pickup station, start time, current duration, and status. Tabs: Active and Overdue. Row actions: View Details and View Customer. Overdue rows highlighted with a warning color. Filter and search bar.

## A11. Returns Management
Table of returns with columns: return ID, rental ID, customer, PowerBank, return station, return time, inspection status badge, and final cost. Row actions: View Details, Review Photos, Approve, Reject (asks for a reason in a dialog), Request Manual Review. Filters for inspection status.

## A12. Pricing and Rental Policies
Pricing configuration page. Form card for a pricing plan: base fee, price per hour, daily maximum fee, free period in minutes, late return fee, and reservation fee. Side panel with the active plan summary. Buttons: Create Plan, Edit Plan, Activate Plan, Deactivate Plan. A pricing history table below with effective dates and a note that existing rentals keep their original price.

## A13. Customers Management
Table of customers with columns: customer ID, full name, email, phone, registration date, account status badge (Active, Suspended), and total rentals. Search bar and status filter. Row actions: View Profile, View Rental History, Activate, Suspend. Customer profile side drawer design showing account activity summary. Passwords are never shown.

## A14. Roles and Permissions
Two-panel page. Left: list of roles (Super Admin, Admin, Station Manager, Maintenance Staff) with a Create Role button. Right: a permission matrix with checkboxes for View Dashboard, Manage Stations, Manage PowerBanks, Manage Inventory, Manage Reservations, Manage Rentals, Manage Returns, Manage Pricing, Manage Customers, Manage Maintenance, View Reports, Manage Settings. Buttons: Save Role, Disable Role. Section to assign roles to users with a user search.

## A15. Maintenance Management
Maintenance request board. Table or Kanban columns for statuses: Open, Assigned, In Progress, Completed, Cancelled. Each request card shows maintenance ID, device or station, issue description, priority badge (Low, Medium, High), assigned staff, and created date. Buttons: Create Maintenance Request, Assign Staff, Update Status, Add Notes.

## A16. Inspection Review (PowerBank Return Photos)
Manual review screen. Left: return summary (return ID, PowerBank ID, customer, upload date, status). Center: large photo gallery with thumbnails. Right: inspection notes panel with previous notes, a text area for new notes, and buttons: Approve, Reject, Request Additional Information. Label the page "Manual review". Do not show any automated analysis result.

## A17. Infrastructure Inspections
Station infrastructure inspection records. Table with columns: station, inspection date, description, inspection status, and administrator notes. Button: Create Inspection. Detail view with uploaded photo gallery, description field, Approve and Reject buttons, and Create Maintenance Request button.

## A18. Circuit Board Inspection Records
Record management page for maintenance-related circuit board inspections. Table with PowerBank or station ID, circuit board identifier, inspection date, technician, status. Detail form with image upload area, manual inspection notes text area, status dropdown, and a Link to Maintenance Request button.

## A19. Audit Logs
Read-only log table with columns: log ID, user, action, affected entity, entity ID, date and time, IP address, and result (Success or Failed badge). Filters: user, action, entity, date range. Buttons: Export CSV and View Details (side drawer with the full change record). Note at the top that logs are append-only.

## A20. System Settings
Settings page with left-side category menu: General, Branding, Localization, Contact, Notifications, Reservations, File Uploads, Preferences. Forms for application name, logo upload with preview, default language and theme, contact information, reservation expiration policy in minutes, upload limits, and general preferences. Buttons: Save Settings (primary), Restore Defaults (secondary with confirmation).

## A21. Admin Profile and Account Settings
Admin profile page. Profile card with picture, name, email, role badge, and last login time. Edit Profile form, Change Password section, and preferences section (language, theme). Logout button. Show a note that role and permission fields are read-only for the admin account.
