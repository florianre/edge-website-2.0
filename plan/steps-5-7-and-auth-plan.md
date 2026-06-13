# Steps 5 to 7 Plus Step 3 Revisit Plan

## Scope

This plan covers:

- Step 3 revisit: lightweight login and role-aware access
- Step 5: member-facing interface and navigation
- Step 6: sessions
- Step 7: trips

This plan does not include:

- Step 8 capacity notifications
- Step 9 WhatsApp/Facebook integration
- Production authentication hardening
- Automated tests

## Current Baseline

The repo already contains:

- Scala 3 backend built with `sbt`
- `http4s` routing
- YAML-based app config
- JSON-backed membership management
- A dummy UI for membership CRUD

That means this next slice should extend the existing app rather than replace it.

## Product Decisions

### Navigation Pattern

Use **visible top navigation tabs** as the primary pattern, not a hamburger menu.

Reasoning:

- The product has only a small number of top-level destinations
- The main destinations are high-priority and should stay discoverable
- Current UX guidance still favors visible primary navigation over hiding it behind a hamburger when the destination count is low

Planned behavior:

- Desktop: visible top navigation
- Mobile: visible compact nav first; collapse to a hamburger only if the layout becomes too tight

Top-level nav for this slice:

- `Home`
- `Events`
- `Login`
- `Membership Management` (only appears after committee login)

## Architecture Direction

### Keep Memberships File-Backed

The existing membership system should remain on `membership.json` for now.

Reasoning:

- It already exists and works
- This slice is mainly about events and access flow
- There is no need to migrate membership data to a DB yet just to unlock steps 5 to 7

### Add Slick for Event Persistence

Use Slick for the event-related persistence layer only.

Planned layering:

- `routes/`
- `service/`
- `repository/`

This keeps the event slice aligned with the existing pattern while introducing DB access in a dedicated layer.

## Authentication Revisit

## Goal

Reconcile the original low-friction membership-number identification model with the new requirement for a visible `Login` page and role-aware navigation.

## Proposed Flow

### Login

- Add a dedicated `Login` page
- User enters membership number
- Backend checks `membership.json`
- If the member exists and is active, create a lightweight logged-in session
- The session stores:
  - membership number
  - member name
  - role

### Committee-Only Membership Management

- If the logged-in member has role `committee`, show `Membership Management` in navigation
- Accessing `Membership Management` requires an additional committee password
- For now, configure that password as `1234` in `application.yaml`

### Session Model

For this slice, add simple session support using cookies.

Reasoning:

- The app now has navigation and gated pages, which is awkward without server-recognized login state
- A cookie-backed session is enough for a low-security first version
- This is a pragmatic extension of step 3, not a production auth system

### Step 3 Reconciliation

The original brief says there is no separate login for normal event interaction. This slice should reinterpret that as:

- The product still uses membership number as the core identity mechanism
- The site now also supports an optional lightweight login flow for better navigation and role-aware UI
- Event participation should still be possible using the logged-in identity rather than forcing separate account credentials

## Frontend Plan

## 1. Home Page

Create a static themed landing page.

Content:

- Club overview
- Dummy contact details
- Themed visuals/text around sea kayaking, white-water kayaking, paddling, and club trips

Design direction:

- Keep the current custom look and avoid generic admin styling
- Make the home page feel like a real club website, not just an internal tool

## 2. Shared Layout

Introduce a shared layout/navigation shell for:

- Home
- Events
- Login
- Membership Management

State-aware behavior:

- Logged out: show `Home`, `Events`, `Login`
- Logged in as member/coach: show `Home`, `Events`, `Login` plus logged-in identity status
- Logged in as committee: show `Home`, `Events`, `Login`, `Membership Management`

## 3. Events Page

Build the events page around the step 5 requirements.

Planned UI:

- `Upcoming` tab
- `Past` tab
- Event cards or rows showing:
  - date
  - time
  - location
  - type
- Event detail modal/panel

Role-aware actions:

- All users:
  - sign up
  - remove themselves
  - view signed-up names
  - view remaining spaces
- Coach and committee:
  - create event
  - edit event
  - cancel event

Committee-specific membership management should stay on its own page, not inside events.

## Backend Plan

## 1. Config Changes

Extend `application.yaml` with:

- DB config for Slick
- committee password

Example shape:

```yaml
server:
  host: "0.0.0.0"
  port: 8080
membership:
  file: "data/membership.json"
committee:
  password: "1234"
database:
  url: "jdbc:h2:file:./data/edge-events"
  driver: "org.h2.Driver"
  user: "sa"
  password: ""
```

Use H2 with Slick for the first DB-backed slice.

Reasoning:

- Lowest setup friction
- Works well locally
- Fine for planning and early implementation

## 2. Event Domain Model

To keep the DB minimal while still covering steps 6 and 7, use a single `events` table with an event type discriminator.

### Events Table

Proposed minimum columns:

- `id`
- `event_type` (`session` or `trip`)
- `title`
- `event_date`
- `end_date` nullable
- `meet_time` nullable
- `end_time` nullable
- `location_type` nullable
- `location_text` nullable
- `destination` nullable
- `tide_info` nullable
- `max_participants`
- `description` nullable
- `extra_info` nullable
- `status` (`active` / `cancelled`)
- `created_by_membership_number`
- `created_at`
- `updated_at`

Rationale:

- Sessions and trips overlap enough to justify one table initially
- Nullable fields are acceptable at this stage
- This avoids premature schema splitting while still mapping to the spec

### Bookings Table

Steps 5 to 7 require signups and trip payment state, so a second table is needed.

Proposed minimum columns:

- `id`
- `event_id`
- `membership_number`
- `booking_status` (`going` / `cancelled`)
- `note` nullable
- `payment_sent` nullable boolean
- `payment_received` nullable boolean
- `created_at`
- `updated_at`

Rationale:

- One bookings table supports both sessions and trips
- Session rows can simply leave payment fields empty
- Trip rows can use payment fields

## 3. Repository Layer

Add a dedicated repository package for DB access.

Suggested structure:

```text
repository/
  EventRepository.scala
  SlickEventRepository.scala
  BookingRepository.scala
  SlickBookingRepository.scala
```

Repository responsibilities:

- Create/read/update/cancel events
- Fetch upcoming and past events
- Create/remove bookings
- Fetch attendee lists
- Update trip payment flags

## 4. Service Layer

Suggested structure:

```text
service/
  AuthService.scala
  EventService.scala
  EventAuthorization.scala
```

Service responsibilities:

### AuthService

- login by membership number
- restore current session
- validate committee password

### EventService

- list upcoming events
- list past events
- fetch event details
- create session
- create trip
- edit event
- cancel event
- sign up member
- remove member from event
- mark trip payment sent
- mark trip payment received

### EventAuthorization

Centralize role rules:

- members can book/unbook and mark own payment sent
- coaches can create/edit/cancel events and update trip payment states
- committee can do everything coaches can, plus access membership management after second-factor password

## 5. Routes Layer

Add separate route groups:

```text
routes/
  HomeRoutes.scala
  AuthRoutes.scala
  EventRoutes.scala
  MembershipRoutes.scala
```

### AuthRoutes

Planned endpoints:

- `POST /api/auth/login`
- `POST /api/auth/logout`
- `GET /api/auth/session`
- `POST /api/auth/committee/verify`

### EventRoutes

Planned endpoints:

- `GET /api/events?scope=upcoming`
- `GET /api/events?scope=past`
- `GET /api/events/{id}`
- `POST /api/events/sessions`
- `POST /api/events/trips`
- `PUT /api/events/{id}`
- `PATCH /api/events/{id}/cancel`
- `POST /api/events/{id}/bookings`
- `DELETE /api/events/{id}/bookings/{membershipNumber}`
- `PATCH /api/events/{id}/bookings/{membershipNumber}/payment-sent`
- `PATCH /api/events/{id}/bookings/{membershipNumber}/payment-received`

## Access and UI Rules to Enforce

### Logged-Out User

- Can view home page
- Can open events page
- Cannot perform authenticated event actions
- Can use login page

### Logged-In Member

- Can view events
- Can book/unbook themselves
- Can mark own trip payment as sent
- Cannot create/edit/cancel events
- Cannot access membership management

### Logged-In Coach

- All member permissions
- Can create/edit/cancel events
- Can view attendee lists
- Can mark trip payment sent/received
- Cannot access membership management

### Logged-In Committee

- All coach permissions
- Can unlock membership management with additional password

## Delivery Sequence

### Phase 1: Shared Shell and Navigation

- Add shared layout
- Add home page
- Add nav and role-aware UI shell

### Phase 2: Auth Revisit

- Add login/logout/session endpoints
- Add cookie-backed session handling
- Add committee second-factor verification
- Hide/show navigation by role

### Phase 3: Slick Persistence

- Add Slick and H2 dependencies
- Add DB config
- Add schema creation/migration bootstrap
- Create `events` and `bookings` tables

### Phase 4: Event Backend

- Add repositories
- Add services
- Add event routes
- Add authorization checks

### Phase 5: Events UI

- Build upcoming/past tabs
- Build event detail modal/panel
- Add create/edit/cancel flow for coach/committee
- Add booking/payment UI

### Phase 6: Membership Management Integration

- Move existing membership management UI under the new shared shell
- Gate it behind committee role plus extra password

## Risks and Open Questions

### 1. Step 3 Interpretation

The biggest design tension is the original “no separate login” statement versus the new explicit `Login` page requirement.

Recommended resolution:

- Introduce lightweight session login using membership number only
- Keep the spirit of low friction
- Avoid pretending this is strong authentication

### 2. Session vs Pure Modal Identification

Pure modal-based membership-number entry is consistent with the brief but awkward once the site has:

- role-aware navigation
- gated membership management
- reusable logged-in actions

For this reason, session-backed login is the better implementation direction now.

### 3. Single Events Table vs Split Tables

One `events` table is fine for the next slice.

If later complexity grows around:

- tides
- multi-date trips
- cancellation workflows
- richer trip finance state

then sessions and trips can be split later without invalidating the service shape.

## Recommendation

Proceed with:

- visible top nav as the primary navigation pattern
- lightweight membership-number session login
- committee second-factor password from config
- Slick + H2 for events and bookings
- one `events` table plus one `bookings` table
- route/service/repository layering consistent with the current codebase
