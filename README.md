# Edge Website 2.0

This repository now contains the first working slice of the new club website backend and a dummy frontend for manual validation.

## What the app currently does

The app currently implements membership management only.

- Loads application config from `src/main/resources/application.yaml`
- Uses `sbt` and Scala 3 for the backend
- Serves an `http4s` API for membership CRUD operations
- Persists membership state to `data/membership.json`
- Serves a lightweight browser UI for viewing and mutating that membership state

Implemented membership operations:

- View all memberships
- View a single membership by membership number
- Create a membership
- Update a member's name and role
- Activate a membership
- Deactivate a membership
- Delete a membership

This is intentionally a first slice only. It does not yet implement sessions, trips, authentication, committee password protection, or social integrations.

## Tech stack

- Scala 3
- `sbt`
- `cats-effect`
- `http4s`
- `circe`
- `circe-yaml`
- Plain HTML, CSS, and JavaScript for the dummy UI

## Project structure

Key paths:

- `build.sbt`: backend build definition
- `src/main/resources/application.yaml`: application config
- `data/membership.json`: file-backed membership store
- `src/main/scala/com/edgeprogressivepaddling/booking/domain`: membership domain models
- `src/main/scala/com/edgeprogressivepaddling/booking/service`: service trait and JSON-backed implementation
- `src/main/scala/com/edgeprogressivepaddling/booking/routes`: `http4s` routes
- `src/main/resources/public`: dummy frontend assets

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

- Open `http://localhost:8080` in a browser to use the dummy membership UI
- Use the create form to add a member
- Use the table actions to edit, activate, deactivate, or delete members
- Refreshing the page will reflect the current contents of `data/membership.json`

## API endpoints

- `GET /api/memberships`
- `GET /api/memberships/{membershipNumber}`
- `POST /api/memberships`
- `PUT /api/memberships/{membershipNumber}`
- `DELETE /api/memberships/{membershipNumber}`
- `PATCH /api/memberships/{membershipNumber}/activate`
- `PATCH /api/memberships/{membershipNumber}/deactivate`

## Configuration

Current config lives in `src/main/resources/application.yaml`:

```yaml
server:
  host: "0.0.0.0"
  port: 8080
membership:
  file: "data/membership.json"
```

The membership file path is currently relative to the repo root when the app is started via `sbt run`.
