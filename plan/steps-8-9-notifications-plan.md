# Steps 8-9 Notifications Plan

## Scope

This plan covers:

- Step 8: full-capacity messaging and vacancy notifications
- Step 9: WhatsApp and Facebook posting for create, edit, and cancel flows

It also defines where the notification hooks should sit in the current `routes -> service -> repository` backend structure.

## External API Findings

These findings were checked against current official Meta documentation on June 13, 2026.

### WhatsApp

- The supported API is the WhatsApp Business Platform Cloud API.
- Cloud API can send messages from a registered business phone number to WhatsApp users.
- Meta documents that users must opt in before receiving WhatsApp messages.
- Meta also documents that free-form non-template messages are only allowed inside the 24-hour customer service window.
- For business-initiated notifications outside that window, approved templates are required.
- The current pricing documentation was updated on May 21, 2026.
- Meta also now has a WhatsApp Groups API, introduced in the WhatsApp changelog on October 3, 2025.
- The Groups API supports programmatic group creation and messaging, but it needs a proper product spike before assuming it can post into an existing club-run community exactly as-is.

### Facebook

- The official Facebook Groups API is not a viable target for this requirement.
- Meta deprecated the Groups API in Graph API v19.0, including `publish_to_groups`, and the changelog states that this deprecation applied to all versions on April 22, 2024.
- That means posting directly to the provided Facebook group URL via the current official API should be treated as blocked.
- The supported official alternative is the Facebook Pages API, which can create Page posts with Page permissions such as `pages_manage_posts`.

## Consequence For This Project

### WhatsApp

We can plan two levels:

1. Immediate implementation target
   Use WhatsApp Cloud API to send notification messages to `07846696819` for now, assuming that number is reachable on WhatsApp and has opted in.

2. Later club-community target
   Run a short spike to confirm whether the WhatsApp Groups API can be used for the club's existing community/group topology, or whether the club needs a new API-managed WhatsApp group flow.

### Facebook

There are two realistic options:

1. Supported option
   Post to a Facebook Page instead of the provided Facebook group.

2. If the group link is a hard requirement
   Keep Facebook group posting out of the first implementation and record it as blocked by the current official Meta platform.

Recommendation:

- implement WhatsApp now
- implement Facebook behind an abstraction, but target a Page, not a Group
- document the group target as an external blocker unless the club accepts a non-official workaround, which I would not recommend

## Architectural Shape

Use the user-suggested split, but make it more type-safe and extensible.

### Core Model

Add a notification domain package, for example:

- `notification/NotificationEvent.scala`
- `notification/NotificationChannel.scala`
- `notification/NotificationMessage.scala`
- `notification/NotificationContext.scala`

Suggested shape:

```scala
sealed trait NotificationEvent
object NotificationEvent:
  final case class EventCreated(event: Event, deepLink: Uri) extends NotificationEvent
  final case class EventEdited(
      before: Event,
      after: Event,
      coachNote: Option[String],
      deepLink: Uri
  ) extends NotificationEvent
  final case class EventCancelled(
      event: Event,
      coachNote: Option[String],
      deepLink: Uri
  ) extends NotificationEvent
  final case class SpotReleased(
      event: Event,
      deepLink: Uri,
      releasedAt: Instant
  ) extends NotificationEvent
```

```scala
sealed trait NotificationChannel
object NotificationChannel:
  case object WhatsApp extends NotificationChannel
  case object Facebook extends NotificationChannel
```

```scala
final case class NotificationMessage(
    channel: NotificationChannel,
    title: String,
    body: String
)
```

### Rendering Layer

Use a type class for channel-specific rendering:

```scala
trait NotificationGenerator[A]:
  def generate(event: NotificationEvent): Either[NotificationSkipReason, A]
```

Examples:

- `WhatsAppNotificationGenerator` produces a `WhatsAppPayload`
- `FacebookNotificationGenerator` produces a `FacebookPayload`

This keeps formatting and trigger rules out of the delivery layer.

### Delivery Layer

Use effect-polymorphic publishers:

```scala
trait NotificationPublisher[F[_], A]:
  def publish(payload: A): F[Unit]
```

Examples:

- `WhatsAppNotificationPublisher[F[_]]`
- `FacebookNotificationPublisher[F[_]]`

### Orchestration Layer

Use one service to coordinate all notifications:

```scala
trait NotificationService[F[_]]:
  def notify(event: NotificationEvent): F[Unit]
```

Concrete service:

- builds channel payloads
- skips channels that do not apply
- dispatches each channel in fire-and-forget mode
- logs success/failure without failing the user-facing request

This is where the extendability lives:

- new channels add a new payload type, generator, and publisher
- event service code emits domain events only
- notification rendering and delivery stay separate

## Fire-And-Forget Semantics

This should not block normal booking/event workflows.

Recommended behavior:

- event create/edit/cancel succeeds if the core DB write succeeds
- notification sending is kicked off asynchronously after the core write
- notification failures are logged and counted, but do not fail the user request

Suggested implementation:

```scala
notificationService
  .notify(event)
  .handleErrorWith(error => logger.warn(error)("Notification dispatch failed"))
  .start
  .void
```

If we want stronger control later, replace raw `.start` with a small internal queue:

- `Queue[F, NotificationEvent]`
- one or more background workers

That is still fire-and-forget from the request path, but more operationally stable.

Recommendation:

- phase 1: background fiber with logging
- phase 2: queue-based dispatcher if message volume grows

## Where The APIs Should Be Called

Do not call WhatsApp or Facebook from routes.

### Correct Call Site

Call notification orchestration from `EventService`, after the state mutation is committed.

Hooks:

- `createSession` -> always emit `EventCreated`
- `createTrip` -> always emit `EventCreated`
- `updateEvent` -> emit `EventEdited` only if meet time or location changed
- `cancelEvent` -> always emit `EventCancelled`
- `removeBooking` or equivalent seat-release path -> emit `SpotReleased` when:
  - the event had been full before the removal
  - the event now has at least one free spot
  - the event starts more than one hour from `now`

This keeps the trigger rules aligned with the requirements and avoids duplication in routes.

## Trigger Logic

### Step 8

- If an event is full, show a WhatsApp message in the UI telling members to use the club WhatsApp community for updates.
- When a place becomes available:
  - compute whether the event was full before the change
  - compute whether it is no longer full after the change
  - compute whether start time is more than one hour away
  - if all are true, emit `SpotReleased`

### Step 9

- `EventCreated` -> always notify WhatsApp and Facebook
- `EventEdited` -> notify only when meet time or meet location changed
- `EventCancelled` -> always notify
- if a coach note is supplied on edit/cancel, append it to the generated post body
- every message must include a direct booking-system link back to the specific event

## Required Config

Extend `application.yaml`.

Suggested shape:

```yaml
app:
  publicBaseUrl: "http://localhost:8080"

notifications:
  enabled: true
  whatsapp:
    enabled: true
    apiBaseUrl: "https://graph.facebook.com/v23.0"
    phoneNumberId: "REPLACE_ME"
    accessToken: "${WHATSAPP_ACCESS_TOKEN}"
    defaultRecipient: "447846696819"
    useTemplatesOutsideCustomerWindow: true
    createdTemplateName: "edge_event_created"
    updatedTemplateName: "edge_event_updated"
    cancelledTemplateName: "edge_event_cancelled"
    spotReleasedTemplateName: "edge_spot_released"
  facebook:
    enabled: false
    apiBaseUrl: "https://graph.facebook.com/v23.0"
    pageId: "REPLACE_ME"
    pageAccessToken: "${FACEBOOK_PAGE_ACCESS_TOKEN}"
    targetMode: "page"
    targetGroupUrl: "https://www.facebook.com/groups/1306205915010292"
```

Config notes:

- store secrets in env vars, not committed YAML
- keep the group URL in config so the blocker stays visible
- include a target mode so the code can explicitly reject unsupported `group` posting for now

## External Credentials And Setup

### WhatsApp Cloud API

Needed:

- Meta developer app
- WhatsApp product added to the app
- business verification if required for production usage
- WhatsApp Business Account
- registered business sending number
- `phone_number_id`
- permanent or system-user access token
- recipient opt-in from `07846696819`
- approved message templates for business-initiated notifications outside the 24-hour window

Primary API call:

- `POST /{phone-number-id}/messages`

Expected payload classes:

- text payload for in-window service messages
- template payload for business-initiated notifications

### WhatsApp Groups API

Needed before we can plan a real community implementation:

- confirm access eligibility
- confirm whether an existing club community/group can be targeted
- confirm whether the group must be created and managed by the API
- confirm pricing and membership-management implications

This should be treated as a discovery spike, not assumed implementation work.

### Facebook

If the club accepts Page posting:

- Meta app
- Facebook Login / Business auth flow
- Page access token
- `pages_manage_posts`
- likely `pages_read_engagement` and `pages_show_list` during setup and validation

Primary API call:

- `POST /{page-id}/feed`

If the club insists on group posting:

- record as blocked by official Meta platform support

## Pricing Notes

Pricing changes often, so this should stay configuration and operations-driven rather than hard-coded.

Current official findings:

- WhatsApp Business Platform pricing page was updated on May 21, 2026
- Groups API pricing references the Cloud API per-message pricing model
- Facebook Page posting does not have a per-post Meta API fee in the same way WhatsApp messaging does, but it still carries app-review and token-management overhead

Implementation consequence:

- do not encode pricing logic in the app
- document pricing ownership in README/ops notes
- assume WhatsApp is the main billable integration

## Delivery Strategy By Phase

### Phase 1

- Add notification domain model
- Add generator abstractions
- Add publishers
- Add `NotificationService`
- Wire event-service triggers
- Implement WhatsApp direct-recipient delivery to `07846696819`
- Implement UI full-event hint text
- Add deep-link generation

### Phase 2

- Add coach note fields to edit/cancel forms
- Add template selection logic for WhatsApp
- Add queue-backed dispatcher
- Add delivery telemetry and retry counters

### Phase 3

- If club accepts a Page target, implement Facebook Page posting
- If club does not accept a Page target, leave Facebook integration as blocked and documented
- Independently spike WhatsApp Groups API suitability for the real community target

## Concrete File Plan

Likely new files:

- `src/main/scala/.../notification/NotificationEvent.scala`
- `src/main/scala/.../notification/NotificationChannel.scala`
- `src/main/scala/.../notification/NotificationGenerator.scala`
- `src/main/scala/.../notification/NotificationPublisher.scala`
- `src/main/scala/.../notification/NotificationService.scala`
- `src/main/scala/.../notification/WhatsAppNotificationGenerator.scala`
- `src/main/scala/.../notification/WhatsAppNotificationPublisher.scala`
- `src/main/scala/.../notification/FacebookNotificationGenerator.scala`
- `src/main/scala/.../notification/FacebookNotificationPublisher.scala`
- `src/main/scala/.../http/MetaHttpClient.scala`

Likely touched files:

- `AppConfig.scala`
- `application.yaml`
- `Main.scala`
- `EventService.scala`
- `EventRoutes.scala`
- frontend event detail and event list UI
- `README.md`
- `openapi.yaml`

## Open Questions

- Is the club willing to use a Facebook Page instead of the provided Facebook Group?
- Is `07846696819` already opted in on WhatsApp?
- Does the club already have a verified WhatsApp Business Account and business sending number?
- Are we allowed to create new WhatsApp templates under the club's Meta business account?
- Does the club want delivery retries, or is log-only failure handling enough for the first cut?

## Recommendation

Proceed with implementation only under this split:

- supported now: WhatsApp direct-recipient integration plus notification architecture
- supported with official API: Facebook Page posting
- blocked pending product decision: direct posting to the specified Facebook Group
- blocked pending spike: posting into the club's existing WhatsApp community/group structure

## Sources

- Meta WhatsApp pricing: https://developers.facebook.com/documentation/business-messaging/whatsapp/pricing
- Meta WhatsApp send messages: https://developers.facebook.com/documentation/business-messaging/whatsapp/messages/send-messages
- Meta WhatsApp get started: https://developers.facebook.com/documentation/business-messaging/whatsapp/get-started
- Meta WhatsApp opt-in: https://developers.facebook.com/documentation/business-messaging/whatsapp/getting-opt-in
- Meta WhatsApp Groups API overview: https://developers.facebook.com/documentation/business-messaging/whatsapp/groups
- Meta WhatsApp Groups API get started: https://developers.facebook.com/documentation/business-messaging/whatsapp/groups/get-started/
- Meta WhatsApp group messaging: https://developers.facebook.com/documentation/business-messaging/whatsapp/groups/groups-messaging/
- Meta Graph API v19 changelog: https://developers.facebook.com/docs/graph-api/changelog/version19.0/
- Meta Facebook Pages API overview: https://developers.facebook.com/docs/pages-api/
- Meta Facebook Pages API posts: https://developers.facebook.com/docs/pages-api/posts/
- Meta permissions reference: https://developers.facebook.com/docs/permissions/
