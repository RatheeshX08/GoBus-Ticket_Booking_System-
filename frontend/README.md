# Frontend - GoBus Bus Ticket Booking

This folder contains the complete client-side frontend files.

## Files Overview
- `index.html`: Main HTML5 document featuring the search header, bus operator comparison list, lower/upper deck sleeper seat layout, fare summary, checkout modal, and My Bookings drawer.
- `style.css`: Clean responsive CSS stylesheet with CSS custom properties, grid layouts for decks, mobile media queries, and checkout modals.
- `app.js`: Interactive client logic handling route search, bus switching, dual-deck seat selection, instant fare/tax computation, e-ticket receipt generation, and booking cancellation.
- `login.html` and `signup.html`: GoBus account pages. Login and registration are validated by the Java API and stored in MySQL. Demo credentials: `demo@gobus.com` / `password123`; if the API is unavailable, the demo button enters a clearly marked local demo-only session. The browser-only route guard is for demonstration UX, not production access control.
- `auth.js`: Shared API-backed login, registration, session, and page-guard helpers.

## How to Run
The frontend expects the Java API at `http://localhost:8080`. First start the
API and initialize MySQL using the steps in `../backend/README.md`. Then serve
this folder with a static web server:
```bash
cd frontend
python3 -m http.server 3000
```

Open `http://localhost:3000`. Log in or create an account to reach the booking
page. The connection banner reports whether the API
and database are reachable. Opening `index.html` directly with a `file://` URL
is not recommended; use the local server so the API requests work consistently.
