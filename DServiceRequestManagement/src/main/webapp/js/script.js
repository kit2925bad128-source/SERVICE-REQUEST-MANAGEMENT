"use strict";

const loginForm = document.querySelector("#login-form");

if (loginForm) {
    const submitButton = document.querySelector("#login-submit");
    const buttonLabel = submitButton.querySelector(".button-label");
    const message = document.querySelector("#login-message");
    const passwordInput = document.querySelector("#password");
    const passwordToggle = document.querySelector(".password-toggle");

    passwordToggle.addEventListener("click", () => {
        const isVisible = passwordInput.type === "text";
        passwordInput.type = isVisible ? "password" : "text";
        passwordToggle.setAttribute("aria-pressed", String(!isVisible));
        passwordToggle.setAttribute("aria-label", isVisible ? "Show password" : "Hide password");
        passwordToggle.title = isVisible ? "Show password" : "Hide password";
    });

    loginForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        message.hidden = true;
        message.textContent = "";

        if (!loginForm.reportValidity()) {
            return;
        }

        submitButton.disabled = true;
        buttonLabel.textContent = "Signing in...";

        try {
            const response = await fetch(new URL("login", window.location.href), {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                    "Accept": "application/json"
                },
                body: new URLSearchParams(new FormData(loginForm)),
                credentials: "same-origin"
            });
            const result = await response.json();

            if (!response.ok || !result.success) {
                throw new Error(result.message || "Unable to sign in. Check your details and try again.");
            }

            const destination = new URL(result.redirectUrl || "dashboard.html", window.location.href);
            if (destination.origin !== window.location.origin) {
                throw new Error("The server returned an invalid redirect destination.");
            }
            window.location.assign(destination.href);
        } catch (error) {
            message.textContent = error instanceof TypeError
                ? "We could not reach the service. Check your connection and try again."
                : error.message;
            message.dataset.kind = "error";
            message.hidden = false;
            submitButton.disabled = false;
            buttonLabel.textContent = "Sign in";
        }
    });
}

if (document.body.dataset.page === "dashboard") {
    const dashboardMessage = document.querySelector("#dashboard-message");
    const recentRequestsBody = document.querySelector("#recent-requests");
    const dashboardNumber = new Intl.NumberFormat();
    const dateFormatter = new Intl.DateTimeFormat(undefined, {
        year: "numeric",
        month: "short",
        day: "numeric"
    });

    document.querySelector("#today-label").textContent = new Intl.DateTimeFormat(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric"
    }).format(new Date());

    loadDashboard();

    async function loadDashboard() {
        try {
            const response = await fetch(new URL("dashboard", window.location.href), {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });

            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }

            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || "Dashboard data is temporarily unavailable.");
            }

            setText("#welcome-name", result.name || "there");
            setText("#profile-name", result.name || "Account");
            setText("#profile-role", result.role === "ADMIN" ? "ADMINISTRATOR" : "REQUESTER");
            const initials = (result.name || "SD").trim().split(/\s+/).slice(0, 2)
                .map((part) => part.charAt(0).toUpperCase()).join("");
            setText("#profile-avatar", initials || "SD");
            setText("#topbar-avatar", initials || "SD");
            document.querySelector(".admin-nav").hidden = result.role !== "ADMIN";

            setText("#total-count", dashboardNumber.format(result.totalRequests || 0));
            setText("#new-count", dashboardNumber.format(result.newRequests || 0));
            setText("#assigned-count", dashboardNumber.format(result.assignedRequests || 0));
            setText("#in-progress-count", dashboardNumber.format(result.inProgressRequests || 0));
            setText("#resolved-count", dashboardNumber.format(result.resolvedRequests || 0));
            renderRecentRequests(Array.isArray(result.recentRequests) ? result.recentRequests : []);
        } catch (error) {
            dashboardMessage.textContent = error instanceof TypeError
                ? "We could not reach the service. Refresh the page to try again."
                : error.message;
            dashboardMessage.hidden = false;
            recentRequestsBody.replaceChildren(makeMessageRow("Request activity could not be loaded."));
        }
    }

    function renderRecentRequests(requests) {
        if (requests.length === 0) {
            recentRequestsBody.replaceChildren(makeMessageRow("No requests yet. Create one to get started."));
            return;
        }

        const rows = requests.map((request) => {
            const row = document.createElement("tr");
            const requestCell = document.createElement("td");
            const title = document.createElement("span");
            const reference = document.createElement("span");
            title.className = "request-title";
            title.textContent = request.title || "Untitled request";
            reference.className = "request-ref";
            reference.textContent = `SR-${String(request.id).padStart(4, "0")}`;
            requestCell.append(title, reference);
            row.append(requestCell);
            row.append(makeTextCell(request.category || "General"));
            row.append(makeBadgeCell(request.priority, "priority"));
            row.append(makeTextCell(request.assignedEmployee || "Unassigned"));
            row.append(makeBadgeCell(request.status, "status"));
            row.append(makeTextCell(formatRequestDate(request.createdAt)));
            return row;
        });
        recentRequestsBody.replaceChildren(...rows);
    }

    function makeTextCell(value) {
        const cell = document.createElement("td");
        cell.textContent = value;
        return cell;
    }

    function makeBadgeCell(value, kind) {
        const cell = document.createElement("td");
        const badge = document.createElement("span");
        const normalizedValue = String(value || "UNKNOWN").toUpperCase();
        const validValues = kind === "priority"
            ? ["LOW", "MEDIUM", "HIGH"]
            : ["NEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"];
        const safeValue = validValues.includes(normalizedValue) ? normalizedValue : "UNKNOWN";
        badge.className = `data-badge ${kind}-${safeValue.toLowerCase().replaceAll("_", "-")}`;
        badge.textContent = safeValue.replaceAll("_", " ");
        cell.append(badge);
        return cell;
    }

    function makeMessageRow(text) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.className = "table-loading";
        cell.colSpan = 6;
        cell.textContent = text;
        row.append(cell);
        return row;
    }

    function formatRequestDate(value) {
        if (!value) {
            return "—";
        }
        const date = new Date(value);
        return Number.isNaN(date.getTime()) ? "—" : dateFormatter.format(date);
    }

    function setText(selector, value) {
        document.querySelector(selector).textContent = value;
    }
}

if (document.body.dataset.page === "create-request") {
    const createForm = document.querySelector("#create-request-form");
    const createButton = document.querySelector("#create-request-submit");
    const createButtonLabel = createButton.querySelector(".button-label");
    const feedback = document.querySelector("#request-feedback");
    const feedbackText = document.querySelector("#request-feedback-text");
    const resultLink = document.querySelector("#request-result-link");
    const localDate = new Date();
    const dateOffset = localDate.getTimezoneOffset() * 60_000;
    document.querySelector("#request-date").value = new Date(localDate.getTime() - dateOffset)
        .toISOString().slice(0, 10);
    document.querySelector("#today-label").textContent = new Intl.DateTimeFormat(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric"
    }).format(localDate);

    loadProfile();

    createForm.addEventListener("submit", async (event) => {
        event.preventDefault();
        feedback.hidden = true;
        resultLink.hidden = true;

        if (!createForm.reportValidity()) {
            return;
        }

        createButton.disabled = true;
        createButtonLabel.textContent = "Submitting...";

        try {
            const response = await fetch(new URL("create-request", window.location.href), {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                    "Accept": "application/json"
                },
                body: new URLSearchParams(new FormData(createForm)),
                credentials: "same-origin"
            });

            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }

            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || "The request could not be submitted.");
            }

            feedback.classList.remove("feedback-error");
            feedbackText.textContent = `Request SR-${String(result.requestId).padStart(4, "0")} created successfully.`;
            feedback.hidden = false;
            resultLink.hidden = false;
            createForm.reset();
            document.querySelector("#request-date").value = new Date(localDate.getTime() - dateOffset)
                .toISOString().slice(0, 10);
        } catch (error) {
            feedback.classList.add("feedback-error");
            feedbackText.textContent = error instanceof TypeError
                ? "We could not reach the service. Check your connection and try again."
                : error.message;
            feedback.hidden = false;
        } finally {
            createButton.disabled = false;
            createButtonLabel.textContent = "Submit request";
        }
    });

    async function loadProfile() {
        try {
            const response = await fetch(new URL("dashboard", window.location.href), {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });
            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }
            if (!response.ok) {
                return;
            }

            const result = await response.json();
            const name = result.name || "Account";
            const initials = name.trim().split(/\s+/).slice(0, 2)
                .map((part) => part.charAt(0).toUpperCase()).join("") || "SD";
            document.querySelector("#profile-name").textContent = name;
            document.querySelector("#profile-role").textContent = result.role === "ADMIN"
                ? "ADMINISTRATOR"
                : "REQUESTER";
            document.querySelector("#profile-avatar").textContent = initials;
            document.querySelector("#topbar-avatar").textContent = initials;
            document.querySelector(".admin-nav").hidden = result.role !== "ADMIN";
        } catch {
            document.querySelector("#profile-name").textContent = "Service Desk";
        }
    }
}

if (document.body.dataset.page === "requests") {
    const tableBody = document.querySelector("#requests-table-body");
    const message = document.querySelector("#requests-message");
    const searchInput = document.querySelector("#request-search");
    const statusFilter = document.querySelector("#status-filter");
    const priorityFilter = document.querySelector("#priority-filter");
    const resultCount = document.querySelector("#request-result-count");
    let requestItems = [];

    document.querySelector("#today-label").textContent = new Intl.DateTimeFormat(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric"
    }).format(new Date());

    searchInput.addEventListener("input", renderRequests);
    statusFilter.addEventListener("change", renderRequests);
    priorityFilter.addEventListener("change", renderRequests);
    loadRequests();

    async function loadRequests() {
        try {
            const response = await fetch(new URL("requests", window.location.href), {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });

            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }

            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || "Requests are temporarily unavailable.");
            }

            requestItems = Array.isArray(result.requests) ? result.requests : [];
            if (result.role === "ADMIN") {
                document.querySelector("#requests-title").textContent = "Request management";
                document.querySelector("#requests-subtitle").textContent = "Review every service request across the organization.";
            }
            renderRequests();
            loadRequestProfile();
        } catch (error) {
            message.textContent = error instanceof TypeError
                ? "We could not reach the service. Refresh the page to try again."
                : error.message;
            message.hidden = false;
            tableBody.replaceChildren(makeRequestMessage("Request data could not be loaded."));
            resultCount.textContent = "Unavailable";
        }
    }

    function renderRequests() {
        const query = searchInput.value.trim().toLocaleLowerCase();
        const selectedStatus = statusFilter.value;
        const selectedPriority = priorityFilter.value;
        const filteredRequests = requestItems.filter((request) => {
            const searchableText = [
                request.id,
                request.title,
                request.description,
                request.category,
                request.assignedEmployee,
                request.status,
                request.priority,
                request.resolution
            ].join(" ").toLocaleLowerCase();
            return (!query || searchableText.includes(query))
                && (!selectedStatus || request.status === selectedStatus)
                && (!selectedPriority || request.priority === selectedPriority);
        });

        resultCount.textContent = `${filteredRequests.length} of ${requestItems.length} requests`;
        if (filteredRequests.length === 0) {
            tableBody.replaceChildren(makeRequestMessage(requestItems.length
                ? "No requests match these filters."
                : "No service requests have been created yet."));
            return;
        }

        tableBody.replaceChildren(...filteredRequests.map(makeRequestRow));
    }

    function makeRequestRow(request) {
        const row = document.createElement("tr");
        const titleCell = document.createElement("td");
        const title = document.createElement("span");
        const reference = document.createElement("span");
        title.className = "request-title";
        title.textContent = request.title || "Untitled request";
        reference.className = "request-ref";
        reference.textContent = `SR-${String(request.id).padStart(4, "0")}`;
        titleCell.append(title, reference);
        row.append(titleCell);
        row.append(makeRequestTextCell(request.description || "No description provided.", "request-description"));
        row.append(makeRequestTextCell(request.category || "General"));
        row.append(makeRequestBadgeCell(request.priority, "priority"));
        row.append(makeRequestTextCell(request.assignedEmployee || "Unassigned"));
        row.append(makeRequestBadgeCell(request.status, "status"));
        row.append(makeRequestTextCell(request.createdAt || "—"));
        row.append(makeRequestTextCell(request.resolvedAt || "—"));
        row.append(makeRequestTextCell(request.resolution || "—", "request-resolution"));
        return row;
    }

    function makeRequestTextCell(value, className = "") {
        const cell = document.createElement("td");
        if (className) {
            cell.className = className;
        }
        cell.textContent = value;
        return cell;
    }

    function makeRequestBadgeCell(value, kind) {
        const cell = document.createElement("td");
        const badge = document.createElement("span");
        const normalizedValue = String(value || "UNKNOWN").toUpperCase();
        const allowedValues = kind === "priority"
            ? ["LOW", "MEDIUM", "HIGH"]
            : ["NEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"];
        const safeValue = allowedValues.includes(normalizedValue) ? normalizedValue : "UNKNOWN";
        badge.className = `data-badge ${kind}-${safeValue.toLowerCase().replaceAll("_", "-")}`;
        badge.textContent = safeValue.replaceAll("_", " ");
        cell.append(badge);
        return cell;
    }

    function makeRequestMessage(text) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.className = "table-loading";
        cell.colSpan = 9;
        cell.textContent = text;
        row.append(cell);
        return row;
    }

    async function loadRequestProfile() {
        try {
            const response = await fetch(new URL("dashboard", window.location.href), {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });
            if (!response.ok) {
                return;
            }

            const profile = await response.json();
            const name = profile.name || "Account";
            const initials = name.trim().split(/\s+/).slice(0, 2)
                .map((part) => part.charAt(0).toUpperCase()).join("") || "SD";
            document.querySelector("#profile-name").textContent = name;
            document.querySelector("#profile-role").textContent = profile.role === "ADMIN"
                ? "ADMINISTRATOR"
                : "REQUESTER";
            document.querySelector("#profile-avatar").textContent = initials;
            document.querySelector("#topbar-avatar").textContent = initials;
            document.querySelector(".admin-nav").hidden = profile.role !== "ADMIN";
        } catch {
            document.querySelector("#profile-name").textContent = "Service Desk";
        }
    }
}

if (document.body.dataset.page === "admin") {
    const managementMessage = document.querySelector("#management-message");
    const assignmentFeedback = document.querySelector("#assignment-feedback");
    const allocationBody = document.querySelector("#allocation-table-body");
    const employeeBody = document.querySelector("#employee-table-body");
    const resolutionBody = document.querySelector("#resolution-table-body");

    document.querySelector("#today-label").textContent = new Intl.DateTimeFormat(undefined, {
        weekday: "short",
        month: "short",
        day: "numeric"
    }).format(new Date());

    allocationBody.addEventListener("submit", submitAssignment);
    resolutionBody.addEventListener("submit", submitResolutionUpdate);
    resolutionBody.addEventListener("change", handleResolutionStatusChange);
    loadManagementData();
    loadResolutionData();

    async function loadManagementData() {
        try {
            const response = await fetch(new URL("assign-request", window.location.href), {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });
            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }
            if (response.status === 403) {
                window.location.replace(new URL("dashboard.html", window.location.href));
                return;
            }

            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || "Management data is temporarily unavailable.");
            }

            renderEmployees(Array.isArray(result.employees) ? result.employees : []);
            renderAllocations(
                Array.isArray(result.requests) ? result.requests : [],
                Array.isArray(result.employees) ? result.employees : []
            );
            loadManagementProfile();
        } catch (error) {
            managementMessage.textContent = error instanceof TypeError
                ? "We could not reach the service. Refresh the page to try again."
                : error.message;
            managementMessage.hidden = false;
            allocationBody.replaceChildren(makeManagementMessage("Assignment data could not be loaded.", 5));
            employeeBody.replaceChildren(makeManagementMessage("Employee data could not be loaded.", 3));
            document.querySelector("#open-request-count").textContent = "Unavailable";
            document.querySelector("#employee-count").textContent = "Unavailable";
        }
    }

    function renderEmployees(employees) {
        document.querySelector("#employee-count").textContent = `${employees.length} team members`;
        if (employees.length === 0) {
            employeeBody.replaceChildren(makeManagementMessage("No employees are available for assignment.", 3));
            return;
        }

        employeeBody.replaceChildren(...employees.map((employee) => {
            const row = document.createElement("tr");
            const identity = document.createElement("td");
            const name = document.createElement("span");
            const id = document.createElement("span");
            name.className = "request-title";
            name.textContent = employee.name || "Unnamed employee";
            id.className = "request-ref";
            id.textContent = `EMP-${String(employee.id).padStart(3, "0")}`;
            identity.append(name, id);
            row.append(identity, makeManagementCell(employee.email || "—"),
                makeManagementCell(employee.department || "—"));
            return row;
        }));
    }

    function renderAllocations(requests, employees) {
        document.querySelector("#open-request-count").textContent = `${requests.length} open requests`;
        if (requests.length === 0) {
            allocationBody.replaceChildren(makeManagementMessage("There are no open requests to assign.", 5));
            return;
        }

        allocationBody.replaceChildren(...requests.map((request) => {
            const row = document.createElement("tr");
            const requestCell = document.createElement("td");
            const title = document.createElement("span");
            const reference = document.createElement("span");
            title.className = "request-title";
            title.textContent = request.title || "Untitled request";
            reference.className = "request-ref";
            reference.textContent = `SR-${String(request.id).padStart(4, "0")}`;
            requestCell.append(title, reference);
            row.append(requestCell, makeManagementBadgeCell(request.priority, "priority"),
                makeManagementBadgeCell(request.status, "status"));

            const assignmentCell = document.createElement("td");
            const form = document.createElement("form");
            form.className = "assignment-form";
            form.dataset.requestId = String(request.id);
            const select = document.createElement("select");
            select.name = "employeeId";
            select.setAttribute("aria-label", `Employee for request SR-${String(request.id).padStart(4, "0")}`);
            select.required = true;
            const placeholder = document.createElement("option");
            placeholder.value = "";
            placeholder.textContent = "Select employee";
            placeholder.disabled = true;
            placeholder.selected = !request.assignedTo;
            select.append(placeholder);
            for (const employee of employees) {
                const option = document.createElement("option");
                option.value = String(employee.id);
                option.textContent = `${employee.name}${employee.department ? ` · ${employee.department}` : ""}`;
                option.selected = Number(request.assignedTo) === Number(employee.id);
                select.append(option);
            }
            form.append(select);
            assignmentCell.append(form);
            row.append(assignmentCell);

            const actionCell = document.createElement("td");
            const button = document.createElement("button");
            button.className = "assignment-submit";
            button.type = "button";
            button.textContent = request.assignedTo ? "Reassign" : "Assign";
            button.addEventListener("click", () => {
                form.requestSubmit();
            });
            actionCell.append(button);
            row.append(actionCell);
            return row;
        }));
    }

    async function submitAssignment(event) {
        if (!(event.target instanceof HTMLFormElement) || !event.target.matches(".assignment-form")) {
            return;
        }
        event.preventDefault();
        const form = event.target;
        if (!form.reportValidity()) {
            return;
        }

        const submitButton = form.closest("tr").querySelector(".assignment-submit");
        submitButton.disabled = true;
        submitButton.textContent = "Saving...";
        assignmentFeedback.hidden = true;

        try {
            const formData = new URLSearchParams(new FormData(form));
            formData.set("requestId", form.dataset.requestId);
            const response = await fetch(new URL("assign-request", window.location.href), {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                    "Accept": "application/json"
                },
                body: formData,
                credentials: "same-origin"
            });
            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }

            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || "Assignment could not be saved.");
            }

            assignmentFeedback.classList.remove("feedback-error");
            assignmentFeedback.textContent = `Request SR-${String(result.requestId).padStart(4, "0")} assigned successfully.`;
            assignmentFeedback.hidden = false;
            await loadManagementData();
        } catch (error) {
            assignmentFeedback.classList.add("feedback-error");
            assignmentFeedback.textContent = error instanceof TypeError
                ? "We could not reach the service. Try again."
                : error.message;
            assignmentFeedback.hidden = false;
            submitButton.disabled = false;
            submitButton.textContent = "Assign";
        }
    }

    async function loadResolutionData() {
        try {
            const response = await fetch(new URL("update-request", window.location.href), {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });
            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }
            if (response.status === 403) {
                window.location.replace(new URL("dashboard.html", window.location.href));
                return;
            }

            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || "Resolution data is temporarily unavailable.");
            }
            renderResolutionRequests(Array.isArray(result.requests) ? result.requests : []);
        } catch (error) {
            managementMessage.textContent = error instanceof TypeError
                ? "We could not reach the service. Refresh the page to try again."
                : error.message;
            managementMessage.hidden = false;
            resolutionBody.replaceChildren(makeManagementMessage("Resolution data could not be loaded.", 5));
            document.querySelector("#resolution-request-count").textContent = "Unavailable";
        }
    }

    function renderResolutionRequests(requests) {
        document.querySelector("#resolution-request-count").textContent = `${requests.length} requests`;
        if (requests.length === 0) {
            resolutionBody.replaceChildren(makeManagementMessage("No requests are available for status tracking.", 5));
            return;
        }

        resolutionBody.replaceChildren(...requests.map((request) => {
            const row = document.createElement("tr");
            const requestCell = document.createElement("td");
            const title = document.createElement("span");
            const reference = document.createElement("span");
            const requestNumber = String(request.id).padStart(4, "0");
            title.className = "request-title";
            title.textContent = request.title || "Untitled request";
            reference.className = "request-ref";
            reference.textContent = `SR-${requestNumber}`;
            requestCell.append(title, reference);
            row.append(requestCell);

            const statusCell = document.createElement("td");
            const form = document.createElement("form");
            form.className = "resolution-form";
            form.id = `resolution-form-${request.id}`;
            form.dataset.requestId = String(request.id);
            const hiddenId = document.createElement("input");
            hiddenId.type = "hidden";
            hiddenId.name = "requestId";
            hiddenId.value = String(request.id);
            const statusSelect = document.createElement("select");
            statusSelect.name = "status";
            statusSelect.className = "resolution-status-select";
            statusSelect.setAttribute("aria-label", `Status for request SR-${requestNumber}`);
            for (const [value, label] of [
                ["NEW", "New"],
                ["ASSIGNED", "Assigned"],
                ["IN_PROGRESS", "In progress"],
                ["RESOLVED", "Resolved"],
                ["CLOSED", "Closed"]
            ]) {
                const option = document.createElement("option");
                option.value = value;
                option.textContent = label;
                option.selected = request.status === value;
                statusSelect.append(option);
            }
            form.append(hiddenId, statusSelect);
            statusCell.append(form);
            row.append(statusCell);

            const resolutionCell = document.createElement("td");
            const resolutionInput = document.createElement("textarea");
            resolutionInput.name = "resolution";
            resolutionInput.className = "resolution-input";
            resolutionInput.rows = 2;
            resolutionInput.maxLength = 60000;
            resolutionInput.placeholder = "Add outcome or resolution";
            resolutionInput.setAttribute("aria-label", `Resolution for request SR-${requestNumber}`);
            resolutionInput.setAttribute("form", form.id);
            resolutionInput.value = request.resolution || "";
            resolutionInput.required = request.status === "RESOLVED" || request.status === "CLOSED";
            resolutionCell.append(resolutionInput);
            row.append(resolutionCell);
            row.append(makeManagementCell(request.resolvedAt || "—"));

            const actionCell = document.createElement("td");
            const button = document.createElement("button");
            button.className = "assignment-submit resolution-submit";
            button.type = "submit";
            button.setAttribute("form", form.id);
            button.textContent = "Save";
            actionCell.append(button);
            row.append(actionCell);
            return row;
        }));
    }

    function handleResolutionStatusChange(event) {
        if (!(event.target instanceof HTMLSelectElement)
                || !event.target.matches(".resolution-status-select")) {
            return;
        }
        const resolutionInput = event.target.closest("tr").querySelector(".resolution-input");
        resolutionInput.required = event.target.value === "RESOLVED" || event.target.value === "CLOSED";
    }

    async function submitResolutionUpdate(event) {
        if (!(event.target instanceof HTMLFormElement) || !event.target.matches(".resolution-form")) {
            return;
        }
        event.preventDefault();
        const form = event.target;
        if (!form.reportValidity()) {
            return;
        }

        const submitButton = document.querySelector(`[form="${form.id}"].resolution-submit`);
        submitButton.disabled = true;
        submitButton.textContent = "Saving...";
        assignmentFeedback.hidden = true;

        try {
            const response = await fetch(new URL("update-request", window.location.href), {
                method: "POST",
                headers: {
                    "Content-Type": "application/x-www-form-urlencoded;charset=UTF-8",
                    "Accept": "application/json"
                },
                body: new URLSearchParams(new FormData(form)),
                credentials: "same-origin"
            });
            if (response.status === 401) {
                window.location.replace(new URL("login.html", window.location.href));
                return;
            }

            const result = await response.json();
            if (!response.ok || !result.success) {
                throw new Error(result.message || "The request update could not be saved.");
            }

            assignmentFeedback.classList.remove("feedback-error");
            assignmentFeedback.textContent = `Request SR-${String(result.requestId).padStart(4, "0")} updated to ${result.status.replaceAll("_", " ")}.`;
            assignmentFeedback.hidden = false;
            await loadResolutionData();
        } catch (error) {
            assignmentFeedback.classList.add("feedback-error");
            assignmentFeedback.textContent = error instanceof TypeError
                ? "We could not reach the service. Try again."
                : error.message;
            assignmentFeedback.hidden = false;
            submitButton.disabled = false;
            submitButton.textContent = "Save";
        }
    }

    function makeManagementCell(value) {
        const cell = document.createElement("td");
        cell.textContent = value;
        return cell;
    }

    function makeManagementBadgeCell(value, kind) {
        const cell = document.createElement("td");
        const badge = document.createElement("span");
        const normalizedValue = String(value || "UNKNOWN").toUpperCase();
        const values = kind === "priority"
            ? ["LOW", "MEDIUM", "HIGH"]
            : ["NEW", "ASSIGNED", "IN_PROGRESS", "RESOLVED", "CLOSED"];
        const safeValue = values.includes(normalizedValue) ? normalizedValue : "UNKNOWN";
        badge.className = `data-badge ${kind}-${safeValue.toLowerCase().replaceAll("_", "-")}`;
        badge.textContent = safeValue.replaceAll("_", " ");
        cell.append(badge);
        return cell;
    }

    function makeManagementMessage(text, columnCount) {
        const row = document.createElement("tr");
        const cell = document.createElement("td");
        cell.className = "table-loading";
        cell.colSpan = columnCount;
        cell.textContent = text;
        row.append(cell);
        return row;
    }

    async function loadManagementProfile() {
        try {
            const response = await fetch(new URL("dashboard", window.location.href), {
                headers: { "Accept": "application/json" },
                credentials: "same-origin"
            });
            if (!response.ok) {
                return;
            }
            const profile = await response.json();
            const name = profile.name || "Administrator";
            const initials = name.trim().split(/\s+/).slice(0, 2)
                .map((part) => part.charAt(0).toUpperCase()).join("") || "AD";
            document.querySelector("#profile-name").textContent = name;
            document.querySelector("#profile-role").textContent = "ADMINISTRATOR";
            document.querySelector("#profile-avatar").textContent = initials;
            document.querySelector("#topbar-avatar").textContent = initials;
        } catch {
            document.querySelector("#profile-name").textContent = "Administrator";
        }
    }
}