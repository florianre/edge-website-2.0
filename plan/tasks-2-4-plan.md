# Tasks 2 to 4 Implementation Plan

## Scope

This plan treats the website brief as product context and focuses only on the first deliverable needed to start implementation work:

- Task 2: membership data management
- Task 3: HTTP routes exposing membership operations
- Task 4: a dummy UI that exercises those operations and reflects the current JSON state

The immediate goal is not a full production website. The goal is to establish a clean Scala backend foundation for the membership file, prove the CRUD flow end to end, and create a minimal UI that is good enough for manual validation.

## Assumptions

- The project will be built from scratch in Scala.
- `http4s` will be used for the HTTP layer.
- The membership store will initially be a local JSON file named `membership.json`.
- We are intentionally skipping unit tests for now.
- The dummy UI is for internal validation, not for final design or production usage.

## Recommended Technical Direction

### Backend

- Scala 3
- `sbt`
- `http4s` for routes and server
- `circe` for JSON encoding, decoding, and file serialization
- A small effect stack based on `cats-effect`

### Frontend

For this phase, I recommend **not** using React.

Reasoning:

- The current need is a thin test harness for backend CRUD, not a large interactive application.
- React adds project structure, build tooling, component conventions, and client-state complexity that does not buy much at this stage.
- A minimal static frontend served by the Scala app is faster to build and easier to change while the API shape is still moving.

Recommended option for this phase:

- Plain HTML, CSS, and small vanilla JavaScript module

This keeps the feedback loop short:

- Read current membership state from the backend
- Render the member list
- Trigger CRUD operations with buttons/forms
- Re-fetch and re-render after each operation

If the project later grows into a richer application, the backend can stay as-is and the frontend can be replaced with React, Laminar, or another client later.

## Proposed Initial Architecture

### Data

`membership.json`

- Stores an array of membership records
- Seeded with dummy data for development

Suggested record shape:

```json
{
  "membershipNumber": "1001",
  "name": "Alex Carter",
  "role": "member",
  "status": "active"
}
```

### Scala Structure

Suggested package layout:

```text
src/main/scala/.../
  domain/
    Membership.scala
    MembershipRole.scala
    MembershipStatus.scala
  service/
    MembershipService.scala
    JsonMembershipService.scala
  routes/
    MembershipRoutes.scala
  config/
    AppConfig.scala
  Main.scala

src/main/resources/
  public/
    index.html
    app.js
    styles.css

data/
  membership.json
```

### Core Backend Components

`Membership`

- Domain model matching the JSON schema in the requirements

`MembershipService`

- A trait defining CRUD operations and read operations needed by the UI and later auth flow

Initial methods:

- `getAll`
- `getByMembershipNumber`
- `create`
- `update`
- `delete`
- `activate`
- `deactivate`

Potentially useful early additions:

- `updateRole`
- `updateName`
- `exists`

`JsonMembershipService`

- Concrete implementation backed by `membership.json`
- Responsible for:
  - Loading the file
  - Decoding JSON
  - Applying updates
  - Writing JSON back safely

Important implementation constraint:

- File operations should be serialized in-process to avoid concurrent write corruption

The simplest safe approach for this phase is:

- Keep an in-memory `Ref` of the current membership state
- Load once at startup
- Persist to disk after each successful mutation

This is cleaner than reading and writing the whole file on every request with no coordination.

`MembershipRoutes`

- `http4s` routes exposing the service over HTTP
- JSON request/response bodies

## API Plan

Suggested initial endpoints:

- `GET /api/memberships`
- `GET /api/memberships/{membershipNumber}`
- `POST /api/memberships`
- `PUT /api/memberships/{membershipNumber}`
- `DELETE /api/memberships/{membershipNumber}`
- `PATCH /api/memberships/{membershipNumber}/activate`
- `PATCH /api/memberships/{membershipNumber}/deactivate`

Optional extra route if useful for the UI:

- `PATCH /api/memberships/{membershipNumber}/role`

Response behavior should be straightforward:

- `200` for successful reads and updates
- `201` for create
- `204` for delete
- `404` when a member does not exist
- `409` when creating a duplicate membership number
- `400` for malformed request bodies

## Dummy UI Plan

The dummy UI should prove all required membership operations from the brief:

- View current membership list
- Add member
- Edit member name
- Change member role
- Activate member
- Deactivate member
- Delete member if we want full CRUD coverage, even if the product brief later prefers soft state changes

### UI Features

- A table showing the current JSON-backed membership state
- A create-member form
- Inline action buttons per row
- A small edit form or modal for changing name/role
- Visible success/error messages
- A manual refresh button is optional, but the UI should also refresh automatically after mutations

### UI Constraints

- Keep styling intentionally light
- Do not optimize for final design yet
- Make each membership operation obvious and testable

## Delivery Sequence

### Phase 1: Project Skeleton

- Create `sbt` project
- Add Scala, `http4s`, `cats-effect`, and `circe` dependencies
- Add app entry point and basic server wiring
- Add static asset serving for the dummy UI

### Phase 2: Membership Domain and File Store

- Define membership model and enums
- Create `membership.json` with dummy seed data
- Add `MembershipService` trait
- Implement `JsonMembershipService`
- Ensure startup load and mutation persistence work correctly

### Phase 3: Membership HTTP Routes

- Implement `MembershipRoutes`
- Add request models for create/update operations
- Wire routes into the server
- Manually validate endpoints with curl or browser tooling

### Phase 4: Dummy UI

- Build static page and minimal JavaScript client
- Fetch and render memberships
- Add forms/buttons for all specified operations
- Re-fetch state after each mutation

### Phase 5: Manual Validation

- Create, update, activate, deactivate, and delete members through the UI
- Confirm file contents change in `membership.json`
- Confirm UI always reflects persisted state

## Non-Goals for This Slice

- Sessions and trips
- Authentication by membership number
- Committee password protection
- WhatsApp or Facebook integration
- Production-grade styling
- Automated tests
- Deployment setup

## Risks and Design Notes

### JSON File as the Source of Truth

This is acceptable for the first slice, but there are limitations:

- No multi-instance safety
- Weak auditability
- Full-file rewrite on mutation
- Future concurrency pressure if many admins edit data

That is fine for this phase because the goal is to validate the domain and flows before introducing a proper database.

### Delete vs Deactivate

The brief explicitly mentions activate/deactivate, not removal. For internal CRUD completeness we can support delete at the service/API level, but the UI can de-emphasize it or hide it if you want the product behavior to align more closely with the brief.

### Frontend Evolution Path

If this grows into a real application, the most likely next step is:

- Keep the backend API
- Replace the dummy UI with a proper frontend

At that point the best frontend choice can be revisited based on team preference:

- React if you want the broadest ecosystem
- Laminar if you want to stay more Scala-centric
- HTMX if you want minimal client-side complexity

For now, plain HTML plus JavaScript is the fastest and lowest-risk choice.

## Proposed Next Implementation Step

Start with the backend membership slice only:

1. Scaffold the Scala project and dependencies
2. Add the domain model and `membership.json`
3. Implement `MembershipService` and `JsonMembershipService`
4. Add `MembershipRoutes`
5. Add the dummy UI once the API shape is stable

This ordering keeps the core logic stable before any frontend work is layered on top.
