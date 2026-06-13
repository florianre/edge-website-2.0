# Edge Progressive Paddling Booking System

## Concept Brief

## 1. Purpose

A members-only booking system embedded in the Edge website, allowing members to sign up to club sessions and trips.

- Coaches can create and manage sessions and trips.
- Committee members have full administrative control.
- All user roles can book onto sessions and trips as participants.

## 2. User Roles

| Role | Who | Permissions |
| --- | --- | --- |
| Committee | Committee members | All coach permissions, plus access to membership management |
| Coach | 13 coaches | Create, edit, and cancel sessions and trips; view attendee lists; mark trip payments as sent or received; view membership list |
| Member | Standard members | Browse and book sessions/trips; cancel own bookings; mark own trip payment as sent |

All three roles can sign up to sessions and trips as standard participants.

## 3. Authentication

- Users identify themselves by entering a unique membership number in the event modal.
- No separate login, cookies, or account creation is required for standard use.
- The membership number is checked against a JSON membership file.
- If the membership is found and active, the user proceeds with permissions based on their role.
- The membership management area requires both:
  - Membership number
  - Shared committee password
- Membership numbers are treated as low-security credentials.
- The stated risk assumption is that misuse is limited to signing someone up under another member's name.

## 4. Membership File

The membership file is stored as JSON or an equivalent structured format.

Each member record contains:

- Membership number
- Name
- Role: `member`, `coach`, or `committee`
- Status: `active` or `inactive`

### Access by Role

| Role | Access |
| --- | --- |
| Committee | Full read/write access through the admin area |
| Coach | Read-only access, including active/inactive status |
| Member | No access |

### Committee Actions

- Add members
- Edit member names
- Change member roles
- Activate members
- Deactivate members

## 5. Member-Facing Interface

The main page includes two tabs:

- `Upcoming`: all future sessions and trips, ordered by date
- `Past`: all previous sessions and trips, ordered by date

Each event displays basic information:

- Date
- Time
- Location
- Type

Selecting an event opens a modal where the user enters a membership number. Once identified, the modal shows full event details and role-appropriate actions.

### Actions Available in Event Modal

- All users can:
  - Sign up or remove themselves
  - View the names of signed-up members
  - See remaining spaces
- Coaches and committee can also:
  - Edit the event
  - Cancel the event

Additional interface requirements:

- Coaches see a `Create Event` button on the main events list.
- Event creation does not require a separate login.
- The committee admin area is a separate login section using membership number plus committee password.

## 6. Sessions

### Session Creation Fields

- Meet time
- Meet location
  - Options: `Canal`, `Kew Bridge` (default), `Other`
- Destination or direction (optional free text)
- End time
- High or low tide (optional)
  - Hidden when `Canal` is selected
- Maximum number of participants
- Extra info (optional free text)

### Session Display for Members

Members see:

- All coach-entered session fields
- A standard footer message shown on every session

Example footer text from the brief:

> Please wear shoes you don't mind getting wet…

### Session Booking

- A member can mark themselves as attending.
- Their name is populated automatically from the membership file.
- The booking flow allows an optional short note.
- A member can remove themselves from a session.

## 7. Trips

### Trip Creation Fields

- Title
- Date or dates
- Maximum number of participants
- Description (large free text field)

### Trip Booking

- A member can mark themselves as `Going`.
- Their name is populated automatically from the membership file.

### Trip Payment Tracking

Payment status is tracked per participant:

- `Payment sent`
  - Can be marked by the member, coach, or committee
- `Payment received`
  - Can be marked only by coach or committee

## 8. Capacity and Full Events

- Each session and trip has a participant cap defined at creation time.
- When an event is full, new members see a message directing them to the club WhatsApp community for updates.
- If a place becomes available more than one hour before the session start time, an automated notification is sent to the club WhatsApp community.
- The notification fires every time a place becomes available, subject to the one-hour rule.
- The brief notes that this behavior should be reviewed later if it becomes too noisy.

## 9. WhatsApp and Facebook Integration

Sessions and trips are automatically posted to the club WhatsApp community and Facebook page.

- Both integrations should post to existing club accounts.
- API access for both services will need to be obtained by the developer.

### Trigger Rules

| Event | Trigger |
| --- | --- |
| Session or trip created | Post immediately |
| Session or trip edited | Post only if meet time or meet location changed |
| Session or trip cancelled | Always post |

### Optional Coach Note

When editing an event due to a time/location change, or when cancelling an event:

- Coaches can optionally enter free text
- Any entered text is appended to the automated post for context

Example uses from the brief:

- `moved to 10am due to tides`
- `cancelled due to high winds`

### Link Requirement

- Every automated post must include a direct link back to the relevant session or trip in the booking system.

## 10. Admin Controls

- Manage the membership file (committee only, behind additional password)
- Create, edit, and cancel sessions and trips
- View attendee lists for sessions and trips
- Mark trip payments as sent or received
