# Edge Website 2.0

This repository now contains the first working slice of the new club website backend and frontend for manual validation.

## What the app currently does

The app currently implements:

- Loads application config from `src/main/resources/application.yaml`
- Uses `sbt` and Scala 3 for the backend
- Serves an `http4s` API for authentication, membership management, and event management
- Persists membership state to `data/membership.json`
- Persists event and booking state to a local H2 database through Slick
- Serves a lightweight browser UI with Home, Events, Login, and committee-only Membership Management areas

Implemented user flows:

- Browse a static kayaking-themed homepage with placeholder club and contact content
- Log in with a membership number
- Unlock the committee area with an additional committee password
- View upcoming and past events
- Create sessions and trips as a coach or committee member
- Edit and cancel events as a coach or committee member
- Book onto events as a logged-in member
- Track trip payment sent and payment received flags
- View, create, edit, activate, and deactivate memberships from the committee-only admin page

The current implementation is still intentionally lightweight. It uses simple cookie-backed login, a file-backed membership store, and a local database for events so the club can validate the workflow before harder production choices are made.

## Tech stack

- Scala 3
- `sbt`
- `cats-effect`
- `http4s`
- `circe`
- `circe-yaml`
- `slick`
- `h2`
- Plain HTML, CSS, and JavaScript for the dummy UI

## Project structure

Key paths:

- `build.sbt`: backend build definition
- `src/main/resources/application.yaml`: application config
- `data/membership.json`: file-backed membership store
- `data/edge-events.mv.db`: local H2 database file created at runtime
- `src/main/scala/com/edgeprogressivepaddling/booking/domain`: membership domain models
- `src/main/scala/com/edgeprogressivepaddling/booking/service`: membership, auth, and event services
- `src/main/scala/com/edgeprogressivepaddling/booking/repository`: Slick-backed event persistence
- `src/main/scala/com/edgeprogressivepaddling/booking/routes`: `http4s` routes
- `src/main/resources/public`: browser UI assets

## How to run the app

Prerequisites:

- Java installed
- `sbt` installed

From the repository root:

```bash
sbt run
```

By default the app starts on:

- `http://localhost:8080`

## How to use it

Once the app is running:

- Open `http://localhost:8080` in a browser to use the site
- Use the Home tab for placeholder club content
- Use the Events tab to browse upcoming and past events
- Use the Login tab to sign in with a membership number from `data/membership.json`
- After logging in as a committee member, enter the extra committee password to unlock Membership Management
- Coaches and committee members can create sessions and trips from the Events page
- Logged-in members can book onto events and manage their own booking state
- Open `http://localhost:8080/docs` to browse the Swagger UI
- Open `http://localhost:8080/openapi.yaml` to view the raw OpenAPI spec

## API endpoints

- `POST /api/auth/login`
- `GET /api/auth/session`
- `POST /api/auth/logout`
- `POST /api/auth/committee/verify`
- `GET /api/memberships`
- `GET /api/memberships?role=coach&status=active`
- `GET /api/memberships?name=alex`
- `GET /api/memberships/{membershipNumber}`
- `POST /api/memberships`
- `PUT /api/memberships/{membershipNumber}`
- `DELETE /api/memberships/{membershipNumber}`
- `PATCH /api/memberships/{membershipNumber}/activate`
- `PATCH /api/memberships/{membershipNumber}/deactivate`
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

## Configuration

Current config lives in `src/main/resources/application.yaml`:

```yaml
server:
  host: "0.0.0.0"
  port: 8080
membership:
  file: "data/membership.json"
committee:
  password: "1234"
database:
  url: "jdbc:h2:file:./data/edge-events;MODE=MySQL;DATABASE_TO_LOWER=TRUE"
  driver: "org.h2.Driver"
  user: "sa"
  password: ""
```

Notes:

- The membership file path is relative to the repo root when the app is started via `sbt run`
- The committee password is deliberately hard-coded in config for now because this is still a prototype
- H2 was chosen to keep local development and review friction low while the event schema is still moving; switching the Slick repository to MySQL later is straightforward once the model settles
