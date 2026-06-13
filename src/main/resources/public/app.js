const state = {
  currentView: "home",
  eventScope: "upcoming",
  currentUser: null,
  memberships: [],
  events: [],
  selectedEvent: null,
};

const views = {
  home: document.querySelector("#home-view"),
  events: document.querySelector("#events-view"),
  login: document.querySelector("#login-view"),
  membership: document.querySelector("#membership-view"),
};

const navButtons = [...document.querySelectorAll(".nav-link")];
const logoutButton = document.querySelector("#logout-button");
const sessionSummary = document.querySelector("#session-summary");

const loginForm = document.querySelector("#login-form");
const loginStatus = document.querySelector("#login-status");
const committeeCard = document.querySelector("#committee-card");
const committeeForm = document.querySelector("#committee-form");
const committeeStatus = document.querySelector("#committee-status");

const eventsStatus = document.querySelector("#events-status");
const upcomingTab = document.querySelector("#upcoming-tab");
const pastTab = document.querySelector("#past-tab");
const coachActions = document.querySelector("#coach-actions");
const createSessionForm = document.querySelector("#create-session-form");
const createTripForm = document.querySelector("#create-trip-form");
const eventsGrid = document.querySelector("#events-grid");
const eventDetailPanel = document.querySelector("#event-detail-panel");
const eventDetailBody = document.querySelector("#event-detail-body");
const detailTitle = document.querySelector("#detail-title");
const closeDetailButton = document.querySelector("#close-detail-button");

const membershipViewButton = document.querySelector('[data-view="membership"]');
const membershipStatus = document.querySelector("#membership-status");
const refreshMembershipsButton = document.querySelector("#refresh-memberships-button");
const membershipTableBody = document.querySelector("#membership-table-body");
const createMemberForm = document.querySelector("#create-member-form");
const editMemberForm = document.querySelector("#edit-member-form");
const clearEditButton = document.querySelector("#clear-edit-button");
const editMembershipNumberInput = document.querySelector("#edit-membership-number");
const editNameInput = document.querySelector("#edit-name");
const editRoleInput = document.querySelector("#edit-role");

const api = {
  authSession: "/api/auth/session",
  authLogin: "/api/auth/login",
  authLogout: "/api/auth/logout",
  committeeVerify: "/api/auth/committee/verify",
  memberships: "/api/memberships",
  events: "/api/events",
};

const escapeHtml = (value = "") =>
  String(value)
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

const setStatus = (node, message, tone = "neutral") => {
  node.textContent = message;
  node.dataset.tone = tone;
};

const isCoachOrCommittee = () =>
  state.currentUser &&
  (state.currentUser.role === "coach" || state.currentUser.role === "committee");

const hasCommitteeAccess = () =>
  state.currentUser?.role === "committee" && state.currentUser?.committeeVerified;

const requestJson = async (url, options = {}) => {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
    },
    credentials: "same-origin",
    ...options,
  });

  if (!response.ok) {
    let message = "Request failed.";
    try {
      const payload = await response.json();
      message = payload.error ?? message;
    } catch {}
    throw new Error(message);
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
};

const setView = (viewName) => {
  state.currentView = viewName;
  Object.entries(views).forEach(([name, element]) => {
    element.classList.toggle("active", name === viewName);
  });
  navButtons.forEach((button) => {
    button.classList.toggle("active", button.dataset.view === viewName);
  });
};

const renderSession = () => {
  if (!state.currentUser) {
    sessionSummary.textContent = "Browsing as guest";
    logoutButton.classList.add("hidden");
    committeeCard.classList.add("hidden");
    membershipViewButton.classList.add("hidden");
    if (state.currentView === "membership") {
      setView("login");
    }
    return;
  }

  sessionSummary.textContent = `${state.currentUser.name} (${state.currentUser.role})`;
  logoutButton.classList.remove("hidden");
  committeeCard.classList.toggle(
    "hidden",
    state.currentUser.role !== "committee" || state.currentUser.committeeVerified
  );
  membershipViewButton.classList.toggle("hidden", !hasCommitteeAccess());
};

const renderMemberships = () => {
  if (!hasCommitteeAccess()) {
    membershipTableBody.innerHTML = "";
    setStatus(
      membershipStatus,
      "Committee password verification is required for membership management.",
      "neutral"
    );
    return;
  }

  if (state.memberships.length === 0) {
    membershipTableBody.innerHTML = `
      <tr><td colspan="5" class="empty-state">No memberships found.</td></tr>
    `;
    return;
  }

  membershipTableBody.innerHTML = state.memberships
    .map(
      (membership) => `
        <tr>
          <td>${escapeHtml(membership.membershipNumber)}</td>
          <td>${escapeHtml(membership.name)}</td>
          <td><span class="pill">${escapeHtml(membership.role)}</span></td>
          <td><span class="pill pill-${escapeHtml(membership.status)}">${escapeHtml(membership.status)}</span></td>
          <td class="actions-cell">
            <button class="table-button" type="button" data-membership-action="edit" data-membership-number="${escapeHtml(
              membership.membershipNumber
            )}">Edit</button>
            <button class="table-button" type="button" data-membership-action="${
              membership.status === "active" ? "deactivate" : "activate"
            }" data-membership-number="${escapeHtml(membership.membershipNumber)}">${
              membership.status === "active" ? "Deactivate" : "Activate"
            }</button>
          </td>
        </tr>
      `
    )
    .join("");
};

const renderEvents = () => {
  coachActions.classList.toggle("hidden", !isCoachOrCommittee());
  upcomingTab.classList.toggle("active", state.eventScope === "upcoming");
  pastTab.classList.toggle("active", state.eventScope === "past");

  if (state.events.length === 0) {
    eventsGrid.innerHTML = `<div class="empty-card">No ${state.eventScope} events yet.</div>`;
    return;
  }

  eventsGrid.innerHTML = state.events
    .map(
      (event) => `
        <article class="event-card">
          <p class="eyebrow">${escapeHtml(event.eventType)}</p>
          <h3>${escapeHtml(event.title)}</h3>
          <p class="event-meta">${escapeHtml(event.eventDate)}${
            event.meetTime ? ` • ${escapeHtml(event.meetTime)}` : ""
          }</p>
          <p class="event-meta">${escapeHtml(event.locationText ?? readableLocationType(event.locationType) ?? "Club event")}</p>
          <button class="primary-button" type="button" data-event-id="${event.id}">Open Event</button>
        </article>
      `
    )
    .join("");
};

const renderEventDetail = () => {
  if (!state.selectedEvent) {
    eventDetailPanel.classList.add("hidden");
    return;
  }

  const detail = state.selectedEvent;
  const event = detail.event;
  detailTitle.textContent = event.title;

  const attendeeRows = detail.attendees.length
    ? detail.attendees
        .map(
          (booking) => `
            <tr>
              <td>${escapeHtml(booking.memberName)}</td>
              <td>${escapeHtml(booking.membershipNumber)}</td>
              <td>${escapeHtml(booking.note ?? "-")}</td>
              <td>${booking.paymentSent == null ? "-" : booking.paymentSent ? "Yes" : "No"}</td>
              <td>${booking.paymentReceived == null ? "-" : booking.paymentReceived ? "Yes" : "No"}</td>
              <td>
                ${
                  isCoachOrCommittee() && event.eventType === "trip"
                    ? `<button class="table-button" type="button" data-payment-received-membership-number="${escapeHtml(
                        booking.membershipNumber
                      )}" data-payment-received-value="${booking.paymentReceived ? "false" : "true"}">
                        Mark Received ${booking.paymentReceived ? "Off" : "On"}
                      </button>`
                    : "-"
                }
              </td>
            </tr>
          `
        )
        .join("")
    : `<tr><td colspan="5" class="empty-state">No attendees yet.</td></tr>`;

  const currentBooking = detail.currentUserBooking;

  eventDetailBody.innerHTML = `
    <div class="detail-grid">
      <div class="detail-copy">
        <p><strong>Date:</strong> ${escapeHtml(event.eventDate)}</p>
        <p><strong>Time:</strong> ${escapeHtml(event.meetTime ?? "-")} ${event.endTime ? `to ${escapeHtml(event.endTime)}` : ""}</p>
        <p><strong>Location:</strong> ${escapeHtml(event.locationText ?? readableLocationType(event.locationType) ?? "-")}</p>
        <p><strong>Status:</strong> ${escapeHtml(event.status)}</p>
        <p><strong>Spots Remaining:</strong> ${detail.spotsRemaining}</p>
        <p><strong>Description:</strong> ${escapeHtml(event.description ?? event.extraInfo ?? "No extra information yet.")}</p>
      </div>
      <div class="detail-actions">
        ${
          state.currentUser
            ? `
              <label>Booking Note<textarea id="booking-note">${escapeHtml(currentBooking?.note ?? "")}</textarea></label>
              <div class="button-row">
                <button id="book-event-button" class="primary-button" type="button">${
                  currentBooking ? "Update Booking" : "Book Onto Event"
                }</button>
                ${
                  currentBooking
                    ? `<button id="remove-booking-button" class="secondary-button" type="button">Remove Me</button>`
                    : ""
                }
              </div>
              ${
                event.eventType === "trip" && currentBooking
                  ? `
                    <div class="button-row">
                      <button id="toggle-payment-sent-button" class="secondary-button" type="button">
                        Mark Payment Sent: ${currentBooking.paymentSent ? "On" : "Off"}
                      </button>
                    </div>
                  `
                  : ""
              }
            `
            : `<p class="muted-copy">Log in with your membership number to book onto this event.</p>`
        }
        ${
          isCoachOrCommittee()
            ? `<div class="button-row">
                <button id="toggle-edit-event-button" class="secondary-button" type="button">Edit Event</button>
                <button id="cancel-event-button" class="danger-button" type="button">Cancel Event</button>
              </div>`
            : ""
        }
      </div>
    </div>
    ${
      isCoachOrCommittee()
        ? `
          <div id="edit-event-shell" class="form-card hidden">
            <p class="eyebrow">Edit Event</p>
            <form id="edit-event-form" class="stack-form">
              <label>Title <input name="title" value="${escapeHtml(event.title)}" required /></label>
              <label>Date <input name="eventDate" type="date" value="${escapeHtml(event.eventDate)}" required /></label>
              <label>End Date <input name="endDate" type="date" value="${escapeHtml(event.endDate ?? "")}" /></label>
              <label>Meet Time <input name="meetTime" type="time" value="${escapeHtml(event.meetTime ?? "")}" /></label>
              <label>End Time <input name="endTime" type="time" value="${escapeHtml(event.endTime ?? "")}" /></label>
              <label>
                Location Type
                <select name="locationType">
                  <option value="">None</option>
                  <option value="kew_bridge" ${event.locationType === "kew_bridge" ? "selected" : ""}>Kew Bridge</option>
                  <option value="canal" ${event.locationType === "canal" ? "selected" : ""}>Canal</option>
                  <option value="other" ${event.locationType === "other" ? "selected" : ""}>Other</option>
                </select>
              </label>
              <label>Location Text <input name="locationText" value="${escapeHtml(event.locationText ?? "")}" /></label>
              <label>Destination <input name="destination" value="${escapeHtml(event.destination ?? "")}" /></label>
              <label>Tide Info <input name="tideInfo" value="${escapeHtml(event.tideInfo ?? "")}" /></label>
              <label>Max Participants <input name="maxParticipants" type="number" min="1" value="${event.maxParticipants}" required /></label>
              <label>Description <textarea name="description">${escapeHtml(event.description ?? "")}</textarea></label>
              <label>Extra Info <textarea name="extraInfo">${escapeHtml(event.extraInfo ?? "")}</textarea></label>
              <button class="primary-button" type="submit">Save Event</button>
            </form>
          </div>
        `
        : ""
    }
    <div class="attendee-table">
      <h4>Attendees</h4>
      <table>
        <thead>
          <tr>
            <th>Name</th>
            <th>Number</th>
            <th>Note</th>
            <th>Sent</th>
            <th>Received</th>
            <th>Actions</th>
          </tr>
        </thead>
        <tbody>${attendeeRows}</tbody>
      </table>
    </div>
  `;

  eventDetailPanel.classList.remove("hidden");

  const bookButton = document.querySelector("#book-event-button");
  const removeBookingButton = document.querySelector("#remove-booking-button");
  const togglePaymentSentButton = document.querySelector("#toggle-payment-sent-button");
  const cancelEventButton = document.querySelector("#cancel-event-button");
  const toggleEditEventButton = document.querySelector("#toggle-edit-event-button");
  const editEventForm = document.querySelector("#edit-event-form");

  if (bookButton) {
    bookButton.addEventListener("click", handleBookingSubmit);
  }
  if (removeBookingButton) {
    removeBookingButton.addEventListener("click", handleRemoveBooking);
  }
  if (togglePaymentSentButton) {
    togglePaymentSentButton.addEventListener("click", handleTogglePaymentSent);
  }
  if (cancelEventButton) {
    cancelEventButton.addEventListener("click", handleCancelEvent);
  }
  if (toggleEditEventButton) {
    toggleEditEventButton.addEventListener("click", () => {
      document.querySelector("#edit-event-shell")?.classList.toggle("hidden");
    });
  }
  if (editEventForm) {
    editEventForm.addEventListener("submit", handleEditEvent);
  }
  eventDetailBody.querySelectorAll("[data-payment-received-membership-number]").forEach((button) => {
    button.addEventListener("click", handleTogglePaymentReceived);
  });
};

const readableLocationType = (value) => {
  if (value === "kew_bridge") return "Kew Bridge";
  if (value === "canal") return "Canal";
  if (value === "other") return "Other";
  return null;
};

const loadSession = async () => {
  const payload = await requestJson(api.authSession, { headers: {} });
  state.currentUser = payload.currentUser;
  renderSession();
};

const loadEvents = async () => {
  state.events = await requestJson(`${api.events}?scope=${state.eventScope}`, {
    headers: {},
  });
  renderEvents();
};

const loadMemberships = async () => {
  if (!hasCommitteeAccess()) {
    renderMemberships();
    return;
  }

  state.memberships = await requestJson(api.memberships, { headers: {} });
  renderMemberships();
  setStatus(membershipStatus, "Membership list loaded.", "success");
};

const loadEventDetail = async (eventId) => {
  state.selectedEvent = await requestJson(`${api.events}/${eventId}`, {
    headers: {},
  });
  renderEventDetail();
};

const fillEditForm = (membershipNumber) => {
  const membership = state.memberships.find(
    (candidate) => candidate.membershipNumber === membershipNumber
  );
  if (!membership) {
    setStatus(membershipStatus, `Could not find membership ${membershipNumber}.`, "error");
    return;
  }

  editMembershipNumberInput.value = membership.membershipNumber;
  editNameInput.value = membership.name;
  editRoleInput.value = membership.role;
};

const resetMembershipEdit = () => {
  editMembershipNumberInput.value = "";
  editNameInput.value = "";
  editRoleInput.value = "member";
};

const handleLogin = async (event) => {
  event.preventDefault();

  try {
    const payload = await requestJson(api.authLogin, {
      method: "POST",
      body: JSON.stringify({
        membershipNumber: loginForm.membershipNumber.value,
      }),
    });
    state.currentUser = payload.currentUser;
    renderSession();
    setStatus(loginStatus, `Logged in as ${state.currentUser.name}.`, "success");
    setView("events");
    await loadEvents();
  } catch (error) {
    setStatus(loginStatus, error.message, "error");
  }
};

const handleLogout = async () => {
  await requestJson(api.authLogout, {
    method: "POST",
    headers: {},
  });
  state.currentUser = null;
  state.memberships = [];
  state.selectedEvent = null;
  renderSession();
  renderMemberships();
  renderEventDetail();
  setView("home");
};

const handleCommitteeVerify = async (event) => {
  event.preventDefault();

  try {
    const payload = await requestJson(api.committeeVerify, {
      method: "POST",
      body: JSON.stringify({ password: committeeForm.password.value }),
    });
    state.currentUser = payload.currentUser;
    renderSession();
    setStatus(committeeStatus, "Committee area unlocked.", "success");
    setView("membership");
    await loadMemberships();
  } catch (error) {
    setStatus(committeeStatus, error.message, "error");
  }
};

const handleCreateSession = async (event) => {
  event.preventDefault();

  try {
    await requestJson(`${api.events}/sessions`, {
      method: "POST",
      body: JSON.stringify({
        eventDate: createSessionForm.eventDate.value,
        meetTime: createSessionForm.meetTime.value,
        endTime: createSessionForm.endTime.value,
        locationType: createSessionForm.locationType.value,
        locationText: createSessionForm.locationText.value || null,
        destination: createSessionForm.destination.value || null,
        tideInfo: createSessionForm.tideInfo.value || null,
        maxParticipants: Number(createSessionForm.maxParticipants.value),
        extraInfo: createSessionForm.extraInfo.value || null,
      }),
    });
    createSessionForm.reset();
    setStatus(eventsStatus, "Session created.", "success");
    await loadEvents();
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleCreateTrip = async (event) => {
  event.preventDefault();

  try {
    await requestJson(`${api.events}/trips`, {
      method: "POST",
      body: JSON.stringify({
        title: createTripForm.title.value,
        eventDate: createTripForm.eventDate.value,
        endDate: createTripForm.endDate.value || null,
        maxParticipants: Number(createTripForm.maxParticipants.value),
        description: createTripForm.description.value,
      }),
    });
    createTripForm.reset();
    setStatus(eventsStatus, "Trip created.", "success");
    await loadEvents();
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleBookingSubmit = async () => {
  if (!state.selectedEvent) return;

  try {
    await requestJson(`${api.events}/${state.selectedEvent.event.id}/bookings`, {
      method: "POST",
      body: JSON.stringify({
        note: document.querySelector("#booking-note")?.value || null,
      }),
    });
    await loadEventDetail(state.selectedEvent.event.id);
    await loadEvents();
    setStatus(eventsStatus, "Booking saved.", "success");
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleRemoveBooking = async () => {
  if (!state.selectedEvent || !state.currentUser) return;

  try {
    await requestJson(
      `${api.events}/${state.selectedEvent.event.id}/bookings/${state.currentUser.membershipNumber}`,
      { method: "DELETE" }
    );
    await loadEventDetail(state.selectedEvent.event.id);
    await loadEvents();
    setStatus(eventsStatus, "Booking removed.", "success");
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleTogglePaymentSent = async () => {
  if (!state.selectedEvent || !state.currentUser) return;

  const currentBooking = state.selectedEvent.currentUserBooking;
  if (!currentBooking) return;

  try {
    await requestJson(
      `${api.events}/${state.selectedEvent.event.id}/bookings/${state.currentUser.membershipNumber}/payment-sent`,
      {
        method: "PATCH",
        body: JSON.stringify({ sent: !currentBooking.paymentSent }),
      }
    );
    await loadEventDetail(state.selectedEvent.event.id);
    setStatus(eventsStatus, "Trip payment state updated.", "success");
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleCancelEvent = async () => {
  if (!state.selectedEvent) return;

  try {
    await requestJson(`${api.events}/${state.selectedEvent.event.id}/cancel`, {
      method: "PATCH",
      headers: {},
    });
    await loadEventDetail(state.selectedEvent.event.id);
    await loadEvents();
    setStatus(eventsStatus, "Event cancelled.", "success");
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleEditEvent = async (event) => {
  event.preventDefault();
  if (!state.selectedEvent) return;

  const form = event.currentTarget;

  try {
    await requestJson(`${api.events}/${state.selectedEvent.event.id}`, {
      method: "PUT",
      body: JSON.stringify({
        title: form.title.value,
        eventDate: form.eventDate.value,
        endDate: form.endDate.value || null,
        meetTime: form.meetTime.value || null,
        endTime: form.endTime.value || null,
        locationType: form.locationType.value || null,
        locationText: form.locationText.value || null,
        destination: form.destination.value || null,
        tideInfo: form.tideInfo.value || null,
        maxParticipants: Number(form.maxParticipants.value),
        description: form.description.value || null,
        extraInfo: form.extraInfo.value || null,
      }),
    });
    await loadEventDetail(state.selectedEvent.event.id);
    await loadEvents();
    setStatus(eventsStatus, "Event updated.", "success");
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleTogglePaymentReceived = async (event) => {
  const button = event.currentTarget;
  const membershipNumber = button.dataset.paymentReceivedMembershipNumber;
  const received = button.dataset.paymentReceivedValue === "true";
  if (!state.selectedEvent || !membershipNumber) return;

  try {
    await requestJson(
      `${api.events}/${state.selectedEvent.event.id}/bookings/${membershipNumber}/payment-received`,
      {
        method: "PATCH",
        body: JSON.stringify({ received }),
      }
    );
    await loadEventDetail(state.selectedEvent.event.id);
    setStatus(eventsStatus, "Trip payment receipt updated.", "success");
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

const handleCreateMember = async (event) => {
  event.preventDefault();

  try {
    await requestJson(api.memberships, {
      method: "POST",
      body: JSON.stringify({
        name: createMemberForm.name.value,
        role: createMemberForm.role.value,
        status: createMemberForm.status.value,
      }),
    });
    createMemberForm.reset();
    createMemberForm.role.value = "member";
    createMemberForm.status.value = "active";
    await loadMemberships();
    setStatus(membershipStatus, "Member created.", "success");
  } catch (error) {
    setStatus(membershipStatus, error.message, "error");
  }
};

const handleEditMember = async (event) => {
  event.preventDefault();
  if (!editMembershipNumberInput.value) {
    setStatus(membershipStatus, "Select a member first.", "error");
    return;
  }

  try {
    await requestJson(`${api.memberships}/${editMembershipNumberInput.value}`, {
      method: "PUT",
      body: JSON.stringify({
        name: editNameInput.value,
        role: editRoleInput.value,
      }),
    });
    await loadMemberships();
    setStatus(membershipStatus, "Member updated.", "success");
  } catch (error) {
    setStatus(membershipStatus, error.message, "error");
  }
};

const handleMembershipTableClick = async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) return;

  const membershipNumber = target.dataset.membershipNumber;
  const action = target.dataset.membershipAction;
  if (!membershipNumber || !action) return;

  if (action === "edit") {
    fillEditForm(membershipNumber);
    return;
  }

  try {
    await requestJson(`${api.memberships}/${membershipNumber}/${action}`, {
      method: "PATCH",
      headers: {},
    });
    await loadMemberships();
    setStatus(membershipStatus, `Membership ${action}d.`, "success");
  } catch (error) {
    setStatus(membershipStatus, error.message, "error");
  }
};

navButtons.forEach((button) => {
  button.addEventListener("click", async () => {
    const viewName = button.dataset.view;
    if (viewName === "membership" && !hasCommitteeAccess()) {
      setView("login");
      return;
    }
    setView(viewName);
    if (viewName === "events") {
      await loadEvents();
    }
    if (viewName === "membership") {
      await loadMemberships();
    }
  });
});

loginForm.addEventListener("submit", handleLogin);
committeeForm.addEventListener("submit", handleCommitteeVerify);
logoutButton.addEventListener("click", handleLogout);
upcomingTab.addEventListener("click", async () => {
  state.eventScope = "upcoming";
  await loadEvents();
});
pastTab.addEventListener("click", async () => {
  state.eventScope = "past";
  await loadEvents();
});
createSessionForm.addEventListener("submit", handleCreateSession);
createTripForm.addEventListener("submit", handleCreateTrip);
eventsGrid.addEventListener("click", async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) return;
  const eventId = target.dataset.eventId;
  if (!eventId) return;
  await loadEventDetail(eventId);
});
closeDetailButton.addEventListener("click", () => {
  state.selectedEvent = null;
  renderEventDetail();
});
refreshMembershipsButton.addEventListener("click", loadMemberships);
createMemberForm.addEventListener("submit", handleCreateMember);
editMemberForm.addEventListener("submit", handleEditMember);
clearEditButton.addEventListener("click", resetMembershipEdit);
membershipTableBody.addEventListener("click", handleMembershipTableClick);

const initialise = async () => {
  try {
    await loadSession();
    await loadEvents();
    renderMemberships();
    setStatus(eventsStatus, "Events loaded.", "success");
    setStatus(loginStatus, "Use your membership number to log in.", "neutral");
    setStatus(
      committeeStatus,
      "Committee members can unlock the membership area here.",
      "neutral"
    );
  } catch (error) {
    setStatus(eventsStatus, error.message, "error");
  }
};

initialise();
