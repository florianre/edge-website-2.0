const membershipTableBody = document.querySelector("#membership-table-body");
const statusMessage = document.querySelector("#status-message");
const createForm = document.querySelector("#create-form");
const editForm = document.querySelector("#edit-form");
const clearEditButton = document.querySelector("#clear-edit-button");
const refreshButton = document.querySelector("#refresh-button");

const editMembershipNumberInput = document.querySelector("#edit-membership-number");
const editNameInput = document.querySelector("#edit-name");
const editRoleInput = document.querySelector("#edit-role");

const apiBase = "/api/memberships";

let memberships = [];

const setStatus = (message, tone = "neutral") => {
  statusMessage.textContent = message;
  statusMessage.dataset.tone = tone;
};

const escapeHtml = (value) =>
  value
    .replaceAll("&", "&amp;")
    .replaceAll("<", "&lt;")
    .replaceAll(">", "&gt;")
    .replaceAll('"', "&quot;")
    .replaceAll("'", "&#39;");

const renderMemberships = () => {
  if (!Array.isArray(memberships)) {
    setStatus("Membership state could not be rendered.", "error");
    membershipTableBody.innerHTML = "";
    return;
  }

  if (memberships.length === 0) {
    membershipTableBody.innerHTML = `
      <tr>
        <td colspan="5" class="empty-state">No memberships found.</td>
      </tr>
    `;
    return;
  }

  membershipTableBody.innerHTML = memberships
    .map(
      (membership) => `
        <tr>
          <td>${escapeHtml(membership.membershipNumber)}</td>
          <td>${escapeHtml(membership.name)}</td>
          <td><span class="pill">${escapeHtml(membership.role)}</span></td>
          <td><span class="pill pill-${escapeHtml(membership.status)}">${escapeHtml(membership.status)}</span></td>
          <td class="actions-cell">
            <button type="button" class="table-button" data-action="edit" data-membership-number="${escapeHtml(
              membership.membershipNumber
            )}">Edit</button>
            <button type="button" class="table-button" data-action="${
              membership.status === "active" ? "deactivate" : "activate"
            }" data-membership-number="${escapeHtml(membership.membershipNumber)}">${
              membership.status === "active" ? "Deactivate" : "Activate"
            }</button>
            <button type="button" class="table-button danger-button" data-action="delete" data-membership-number="${escapeHtml(
              membership.membershipNumber
            )}">Delete</button>
          </td>
        </tr>
      `
    )
    .join("");
};

const fetchMemberships = async () => {
  const response = await fetch(apiBase);
  if (!response.ok) {
    throw new Error("Failed to load memberships.");
  }

  const payload = await response.json();
  if (!Array.isArray(payload)) {
    throw new Error("Membership list response was not an array.");
  }

  memberships = payload;
  renderMemberships();
};

const readErrorMessage = async (response) => {
  try {
    const payload = await response.json();
    return payload.error ?? "Request failed.";
  } catch {
    return "Request failed.";
  }
};

const requestJson = async (url, options = {}) => {
  const response = await fetch(url, {
    headers: {
      "Content-Type": "application/json",
    },
    ...options,
  });

  if (!response.ok) {
    throw new Error(await readErrorMessage(response));
  }

  if (response.status === 204) {
    return null;
  }

  return response.json();
};

const resetEditForm = () => {
  editMembershipNumberInput.value = "";
  editNameInput.value = "";
  editRoleInput.value = "member";
};

const upsertMembership = (membership) => {
  const index = memberships.findIndex(
    (candidate) => candidate.membershipNumber === membership.membershipNumber
  );

  if (index === -1) {
    memberships = [...memberships, membership].sort((left, right) =>
      left.membershipNumber.localeCompare(right.membershipNumber, undefined, {
        numeric: true,
      })
    );
  } else {
    memberships = memberships.map((candidate) =>
      candidate.membershipNumber === membership.membershipNumber
        ? membership
        : candidate
    );
  }

  renderMemberships();
};

const removeMembership = (membershipNumber) => {
  memberships = memberships.filter(
    (candidate) => candidate.membershipNumber !== membershipNumber
  );
  renderMemberships();
};

const fillEditForm = (membershipNumber) => {
  const membership = memberships.find(
    (candidate) => candidate.membershipNumber === membershipNumber
  );

  if (!membership) {
    setStatus(`Could not find membership ${membershipNumber}.`, "error");
    return;
  }

  editMembershipNumberInput.value = membership.membershipNumber;
  editNameInput.value = membership.name;
  editRoleInput.value = membership.role;
  editNameInput.focus();
  setStatus(`Editing ${membership.name}.`, "neutral");
};

const handleCreate = async (event) => {
  event.preventDefault();

  const payload = {
    name: createForm.name.value,
    role: createForm.role.value,
    status: createForm.status.value,
  };

  try {
    const createdMembership = await requestJson(apiBase, {
      method: "POST",
      body: JSON.stringify(payload),
    });
    upsertMembership(createdMembership);
    createForm.reset();
    createForm.role.value = "member";
    createForm.status.value = "active";
    setStatus(
      `Created membership ${createdMembership.membershipNumber}.`,
      "success"
    );
  } catch (error) {
    setStatus(error.message, "error");
  }
};

const handleEdit = async (event) => {
  event.preventDefault();

  const membershipNumber = editMembershipNumberInput.value;
  if (!membershipNumber) {
    setStatus("Select a member to edit first.", "error");
    return;
  }

  const payload = {
    name: editNameInput.value,
    role: editRoleInput.value,
  };

  try {
    const updatedMembership = await requestJson(`${apiBase}/${membershipNumber}`, {
      method: "PUT",
      body: JSON.stringify(payload),
    });
    upsertMembership(updatedMembership);
    setStatus(`Updated membership ${membershipNumber}.`, "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
};

const handleTableAction = async (event) => {
  const target = event.target;
  if (!(target instanceof HTMLButtonElement)) {
    return;
  }

  const action = target.dataset.action;
  const membershipNumber = target.dataset.membershipNumber;

  if (!action || !membershipNumber) {
    return;
  }

  if (action === "edit") {
    fillEditForm(membershipNumber);
    return;
  }

  try {
    if (action === "delete") {
      const confirmed = window.confirm(
        `Delete membership ${membershipNumber}?`
      );
      if (!confirmed) {
        return;
      }

      await requestJson(`${apiBase}/${membershipNumber}`, {
        method: "DELETE",
      });
      if (editMembershipNumberInput.value === membershipNumber) {
        resetEditForm();
      }
      removeMembership(membershipNumber);
      setStatus(`Deleted membership ${membershipNumber}.`, "success");
      return;
    }

    const endpoint =
      action === "activate"
        ? `${apiBase}/${membershipNumber}/activate`
        : `${apiBase}/${membershipNumber}/deactivate`;

    const updatedMembership = await requestJson(endpoint, {
      method: "PATCH",
    });
    upsertMembership(updatedMembership);
    setStatus(
      `${action === "activate" ? "Activated" : "Deactivated"} membership ${membershipNumber}.`,
      "success"
    );
  } catch (error) {
    setStatus(error.message, "error");
  }
};

const initialise = async () => {
  try {
    await fetchMemberships();
    setStatus("Loaded membership state from membership.json.", "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
};

createForm.addEventListener("submit", handleCreate);
editForm.addEventListener("submit", handleEdit);
clearEditButton.addEventListener("click", () => {
  resetEditForm();
  setStatus("Edit form cleared.", "neutral");
});
refreshButton.addEventListener("click", async () => {
  try {
    await fetchMemberships();
    setStatus("Membership list refreshed.", "success");
  } catch (error) {
    setStatus(error.message, "error");
  }
});
membershipTableBody.addEventListener("click", handleTableAction);

resetEditForm();
initialise();
