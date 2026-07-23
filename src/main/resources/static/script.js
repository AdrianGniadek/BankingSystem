"use strict";

const ACCESS_TOKEN_KEY = "accessToken";
const HISTORY_DAYS = 90;
const state = {
    accounts: [],
    profile: null,
    statements: new Map(),
    activeView: "overview"
};

let refreshPromise = null;

class ApiError extends Error {
    constructor(message, status, fieldErrors = {}) {
        super(message);
        this.name = "ApiError";
        this.status = status;
        this.fieldErrors = fieldErrors;
    }
}

function initializeIcons() {
    if (window.lucide) {
        window.lucide.createIcons({
            attrs: {
                "aria-hidden": "true"
            }
        });
    }
}

function storeAccessToken(token) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

function clearAccessToken() {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem("jwt");
}

async function requestNewAccessToken() {
    try {
        const response = await fetch("/auth/refresh", {
            method: "POST",
            credentials: "same-origin"
        });

        if (!response.ok) {
            clearAccessToken();
            return false;
        }

        const data = await response.json();
        storeAccessToken(data.accessToken);
        return true;
    } catch {
        clearAccessToken();
        return false;
    }
}

async function refreshAccessToken() {
    if (!refreshPromise) {
        refreshPromise = requestNewAccessToken().finally(() => {
            refreshPromise = null;
        });
    }
    return refreshPromise;
}

async function authenticatedFetch(url, options = {}) {
    const requestOptions = {
        ...options,
        credentials: "same-origin",
        headers: new Headers(options.headers || {})
    };
    const token = sessionStorage.getItem(ACCESS_TOKEN_KEY);
    if (token) {
        requestOptions.headers.set("Authorization", `Bearer ${token}`);
    }

    let response = await fetch(url, requestOptions);
    if (response.status !== 401 || !(await refreshAccessToken())) {
        return response;
    }

    requestOptions.headers.set(
        "Authorization",
        `Bearer ${sessionStorage.getItem(ACCESS_TOKEN_KEY)}`
    );
    response = await fetch(url, requestOptions);
    return response;
}

async function apiRequest(url, options = {}, authenticated = true) {
    const requestOptions = { ...options };
    requestOptions.headers = new Headers(options.headers || {});
    if (options.body && !requestOptions.headers.has("Content-Type")) {
        requestOptions.headers.set("Content-Type", "application/json");
    }

    const response = authenticated
        ? await authenticatedFetch(url, requestOptions)
        : await fetch(url, { ...requestOptions, credentials: "same-origin" });

    if (!response.ok) {
        const problem = await readProblem(response);
        throw new ApiError(
            translateApiMessage(problem.detail || problem.title, response.status),
            response.status,
            problem.errors || {}
        );
    }

    const contentType = response.headers.get("content-type") || "";
    if (response.status === 204 || !contentType.includes("json")) {
        return null;
    }
    return response.json();
}

async function readProblem(response) {
    const contentType = response.headers.get("content-type") || "";
    if (!contentType.includes("json")) {
        return { detail: "Nie udało się wykonać operacji." };
    }

    try {
        return await response.json();
    } catch {
        return { detail: "Nie udało się odczytać odpowiedzi serwera." };
    }
}

function translateApiMessage(message, status) {
    const translations = {
        "Invalid email or password": "Nieprawidłowy adres e-mail lub hasło.",
        "Access token is missing or invalid": "Sesja wygasła. Zaloguj się ponownie.",
        "You do not have permission to access this resource": "Nie masz uprawnień do tej operacji.",
        "Target account not found": "Nie znaleziono rachunku odbiorcy.",
        "Source and target accounts cannot be the same": "Rachunek nadawcy i odbiorcy nie mogą być takie same.",
        "Insufficient funds": "Brak wystarczających środków na rachunku.",
        "Account is not active": "Wybrany rachunek nie jest aktywny.",
        "The request conflicts with existing data": "Te dane są już wykorzystywane."
    };

    if (translations[message]) {
        return translations[message];
    }
    if (status === 404) {
        return "Nie znaleziono żądanego zasobu.";
    }
    if (status >= 500) {
        return "Serwer nie mógł wykonać operacji. Spróbuj ponownie.";
    }
    return message || "Nie udało się wykonać operacji.";
}

function setButtonLoading(button, loading) {
    if (!button) {
        return;
    }

    if (loading) {
        button.dataset.originalContent = button.innerHTML;
        button.disabled = true;
        button.innerHTML = '<span class="spinner"></span><span>Proszę czekać</span>';
        return;
    }

    button.disabled = false;
    if (button.dataset.originalContent) {
        button.innerHTML = button.dataset.originalContent;
        delete button.dataset.originalContent;
        initializeIcons();
    }
}

function clearFormErrors(form) {
    form.querySelectorAll(".field").forEach(field => field.classList.remove("has-error"));
    form.querySelectorAll(".field-error").forEach(error => {
        error.textContent = "";
    });
}

function showFormErrors(form, errors) {
    Object.entries(errors).forEach(([fieldName, message]) => {
        const normalizedName = fieldName.split(".").pop();
        const error = form.querySelector(`[data-error-for="${normalizedName}"]`);
        if (!error) {
            return;
        }
        error.textContent = translateValidationMessage(message);
        error.closest(".field")?.classList.add("has-error");
    });
}

function translateValidationMessage(message) {
    const translations = {
        "First name is required": "Imię jest wymagane.",
        "Last name is required": "Nazwisko jest wymagane.",
        "Email is required": "Adres e-mail jest wymagany.",
        "Invalid email format": "Wprowadź poprawny adres e-mail.",
        "Email should be valid": "Wprowadź poprawny adres e-mail.",
        "Password is required": "Hasło jest wymagane.",
        "PESEL must be exactly 11 digits": "PESEL musi składać się z 11 cyfr.",
        "Phone number must be exactly 9 digits": "Numer telefonu musi składać się z 9 cyfr.",
        "Target account number must contain exactly 20 digits and cannot start with zero":
            "Numer rachunku musi mieć 20 cyfr i nie może zaczynać się od zera.",
        "Amount must be at least 0.01": "Kwota musi wynosić co najmniej 0,01.",
        "Demo deposit must not exceed 10000.00": "Zasilenie nie może przekroczyć 10 000,00."
    };
    return translations[message] || message;
}

function showAlert(element, message, success = false) {
    if (!element) {
        return;
    }
    element.textContent = message;
    element.classList.toggle("alert--success", success);
    element.hidden = false;
}

function hideAlert(element) {
    if (element) {
        element.hidden = true;
        element.classList.remove("alert--success");
    }
}

function showToast(title, message, type = "success") {
    const region = document.getElementById("toast-region");
    if (!region) {
        return;
    }

    const toast = document.createElement("div");
    toast.className = `toast${type === "error" ? " toast--error" : ""}`;
    toast.innerHTML = `
        <span class="toast__icon">
            <i data-lucide="${type === "error" ? "circle-alert" : "circle-check"}"></i>
        </span>
        <span>
            <strong>${escapeHtml(title)}</strong>
            <p>${escapeHtml(message)}</p>
        </span>
        <button type="button" aria-label="Zamknij powiadomienie">
            <i data-lucide="x"></i>
        </button>
    `;
    toast.querySelector("button").addEventListener("click", () => toast.remove());
    region.appendChild(toast);
    initializeIcons();
    window.setTimeout(() => toast.remove(), 5500);
}

function setupPasswordToggles() {
    document.querySelectorAll("[data-password-toggle]").forEach(button => {
        button.addEventListener("click", () => {
            const input = document.getElementById(button.dataset.passwordToggle);
            const showing = input.type === "text";
            input.type = showing ? "password" : "text";
            button.setAttribute("aria-label", showing ? "Pokaż hasło" : "Ukryj hasło");
            button.setAttribute("title", showing ? "Pokaż hasło" : "Ukryj hasło");
            button.innerHTML = `<i data-lucide="${showing ? "eye" : "eye-off"}"></i>`;
            initializeIcons();
        });
    });
}

function formDataObject(form) {
    return Object.fromEntries(new FormData(form).entries());
}

async function initializeLoginPage() {
    setupPasswordToggles();
    if (sessionStorage.getItem(ACCESS_TOKEN_KEY) || await refreshAccessToken()) {
        window.location.replace("/accounts.html");
        return;
    }

    const form = document.getElementById("login-form");
    const alert = document.getElementById("auth-alert");
    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearFormErrors(form);
        hideAlert(alert);

        if (!form.reportValidity()) {
            return;
        }

        const button = form.querySelector('[type="submit"]');
        setButtonLoading(button, true);
        try {
            const values = formDataObject(form);
            const session = await apiRequest("/auth/login", {
                method: "POST",
                body: JSON.stringify({
                    email: values.email.trim(),
                    password: values.password
                })
            }, false);
            storeAccessToken(session.accessToken);
            window.location.replace("/accounts.html");
        } catch (error) {
            showFormErrors(form, error.fieldErrors || {});
            showAlert(alert, error.message);
        } finally {
            setButtonLoading(button, false);
        }
    });
}

function initializeRegisterPage() {
    setupPasswordToggles();
    const form = document.getElementById("register-form");
    const alert = document.getElementById("auth-alert");

    form.addEventListener("submit", async event => {
        event.preventDefault();
        clearFormErrors(form);
        hideAlert(alert);

        if (!form.reportValidity()) {
            return;
        }

        const button = form.querySelector('[type="submit"]');
        setButtonLoading(button, true);
        try {
            const values = formDataObject(form);
            await apiRequest("/auth/register", {
                method: "POST",
                body: JSON.stringify({
                    firstName: values.firstName.trim(),
                    lastName: values.lastName.trim(),
                    email: values.email.trim(),
                    password: values.password,
                    pesel: values.pesel.trim(),
                    phoneNumber: values.phoneNumber.trim()
                })
            }, false);
            sessionStorage.setItem("registrationComplete", "true");
            window.location.replace("/login.html");
        } catch (error) {
            showFormErrors(form, error.fieldErrors || {});
            showAlert(alert, error.message);
        } finally {
            setButtonLoading(button, false);
        }
    });
}

async function initializeDashboard() {
    setupDashboardNavigation();
    setupDialogs();
    setupDashboardForms();

    try {
        const [profile, accounts] = await Promise.all([
            apiRequest("/profile"),
            apiRequest("/accounts")
        ]);
        state.profile = profile;
        state.accounts = accounts;
        renderProfile();
        renderAccounts();
        renderAccountSelectors();
        showDashboard();
        await loadAllStatements();
    } catch (error) {
        if (error.status === 401) {
            clearAccessToken();
            window.location.replace("/login.html");
            return;
        }
        showDashboard();
        showAlert(document.getElementById("dashboard-alert"), error.message);
    }
}

function showDashboard() {
    document.getElementById("app-loading").hidden = true;
    document.getElementById("app-shell").hidden = false;
    const initialView = window.location.hash.replace("#", "");
    navigateToView(["overview", "transfer", "history", "profile"].includes(initialView)
        ? initialView
        : "overview");
    initializeIcons();
}

function setupDashboardNavigation() {
    document.querySelectorAll("[data-view-target]").forEach(button => {
        button.addEventListener("click", () => navigateToView(button.dataset.viewTarget));
    });

    window.addEventListener("hashchange", () => {
        const view = window.location.hash.replace("#", "");
        if (["overview", "transfer", "history", "profile"].includes(view)) {
            navigateToView(view, false);
        }
    });

    const sidebar = document.querySelector(".sidebar");
    document.getElementById("mobile-menu-button").addEventListener("click", () => {
        sidebar.classList.toggle("is-open");
    });

    document.getElementById("logout-button").addEventListener("click", logout);
}

function navigateToView(view, updateHash = true) {
    const titles = {
        overview: ["Panel klienta", greeting()],
        transfer: ["Płatności", "Wykonaj przelew"],
        history: ["Twoje finanse", "Historia operacji"],
        profile: ["Ustawienia", "Profil i bezpieczeństwo"]
    };
    state.activeView = view;

    document.querySelectorAll(".app-view").forEach(section => {
        section.classList.toggle("is-active", section.dataset.view === view);
    });
    document.querySelectorAll(".nav-item").forEach(item => {
        item.classList.toggle("is-active", item.dataset.viewTarget === view);
    });

    document.getElementById("page-eyebrow").textContent = titles[view][0];
    document.getElementById("page-title").textContent = titles[view][1];
    document.querySelector(".sidebar").classList.remove("is-open");
    if (updateHash) {
        history.replaceState(null, "", `#${view}`);
    }
    window.scrollTo({ top: 0, behavior: "smooth" });
}

function greeting() {
    const hour = new Date().getHours();
    const prefix = hour < 12 ? "Dzień dobry" : hour < 18 ? "Dzień dobry" : "Dobry wieczór";
    return state.profile?.firstName ? `${prefix}, ${state.profile.firstName}` : prefix;
}

function setupDialogs() {
    document.getElementById("open-account-dialog").addEventListener("click", () => {
        document.getElementById("account-dialog").showModal();
    });

    document.querySelectorAll("[data-close-dialog]").forEach(button => {
        button.addEventListener("click", () => {
            document.getElementById(button.dataset.closeDialog).close();
        });
    });

    document.querySelectorAll("dialog").forEach(dialog => {
        dialog.addEventListener("click", event => {
            if (event.target === dialog) {
                dialog.close();
            }
        });
    });
}

function setupDashboardForms() {
    document.getElementById("account-form").addEventListener("submit", createAccount);
    document.getElementById("funding-form").addEventListener("submit", fundAccount);
    document.getElementById("transfer-form").addEventListener("submit", createTransfer);
    document.getElementById("profile-form").addEventListener("submit", updateProfile);
    document.getElementById("password-form").addEventListener("submit", changePassword);
    document.getElementById("transfer-source").addEventListener("change", updateTransferSummary);
    document.getElementById("history-account").addEventListener("change", renderSelectedHistory);
    document.getElementById("refresh-history").addEventListener("click", refreshSelectedHistory);

    document.getElementById("accounts-grid").addEventListener("click", handleAccountAction);
}

function renderProfile() {
    const profile = state.profile;
    const fullName = `${profile.firstName} ${profile.lastName}`;
    const initials = `${profile.firstName?.[0] || ""}${profile.lastName?.[0] || ""}`.toUpperCase();

    document.getElementById("sidebar-user-name").textContent = fullName;
    document.getElementById("sidebar-user-email").textContent = profile.email;
    document.getElementById("sidebar-avatar").textContent = initials || "U";
    document.getElementById("profile-first-name").value = profile.firstName || "";
    document.getElementById("profile-last-name").value = profile.lastName || "";
    document.getElementById("profile-email").value = profile.email || "";
    document.getElementById("profile-phone").value = profile.phoneNumber || "";
}

function renderAccounts() {
    const grid = document.getElementById("accounts-grid");
    const activeAccounts = state.accounts.filter(account => account.status === "ACTIVE");
    const plnBalance = activeAccounts
        .filter(account => account.currency === "PLN")
        .reduce((sum, account) => sum + Number(account.balance), 0);

    document.getElementById("total-balance").textContent = formatMoney(plnBalance, "PLN");
    document.getElementById("accounts-count").textContent = state.accounts.length;
    document.getElementById("active-accounts-caption").textContent =
        activeAccountsLabel(activeAccounts.length);

    if (!state.accounts.length) {
        grid.innerHTML = `
            <div class="empty-state">
                <i data-lucide="landmark"></i>
                <strong>Nie masz jeszcze rachunku</strong>
                <p>Otwórz pierwsze konto, aby wykonywać przelewy i przeglądać historię.</p>
                <button class="button button--primary" type="button" data-empty-create-account>
                    <i data-lucide="plus"></i>
                    <span>Otwórz konto</span>
                </button>
            </div>
        `;
        grid.querySelector("[data-empty-create-account]").addEventListener("click", () => {
            document.getElementById("account-dialog").showModal();
        });
        initializeIcons();
        return;
    }

    grid.innerHTML = state.accounts.map(account => `
        <article class="account-card">
            <div class="account-card__top">
                <div class="account-card__type">
                    <span><i data-lucide="${account.accountType === "SAVINGS" ? "piggy-bank" : "credit-card"}"></i></span>
                    <span>${accountTypeLabel(account.accountType)}</span>
                </div>
                <span class="account-status ${accountStatusClass(account.status)}">
                    ${accountStatusLabel(account.status)}
                </span>
            </div>
            <div class="account-card__balance">
                <span>Dostępne środki</span>
                <strong>${formatMoney(account.balance, account.currency)}</strong>
            </div>
            <div class="account-card__footer">
                <span class="account-card__number">${formatAccountNumber(account.accountNumber)}</span>
                <div class="account-card__actions">
                    <button class="icon-button" type="button" data-copy-account="${account.id}"
                            aria-label="Kopiuj numer rachunku" title="Kopiuj numer rachunku">
                        <i data-lucide="copy"></i>
                    </button>
                    ${account.status === "ACTIVE" ? `
                        <button class="icon-button" type="button" data-fund-account="${account.id}"
                                aria-label="Zasil konto demonstracyjne" title="Zasil konto demonstracyjne">
                            <i data-lucide="circle-dollar-sign"></i>
                        </button>
                        <button class="icon-button" type="button" data-transfer-account="${account.id}"
                                aria-label="Wykonaj przelew" title="Wykonaj przelew">
                            <i data-lucide="send"></i>
                        </button>
                    ` : ""}
                </div>
            </div>
        </article>
    `).join("");
    initializeIcons();
}

function renderAccountSelectors() {
    const activeAccounts = state.accounts.filter(account => account.status === "ACTIVE");
    const transferSelect = document.getElementById("transfer-source");
    const historySelect = document.getElementById("history-account");

    transferSelect.innerHTML = activeAccounts.length
        ? activeAccounts.map(account => accountOption(account)).join("")
        : '<option value="">Brak aktywnych rachunków</option>';
    transferSelect.disabled = !activeAccounts.length;

    historySelect.innerHTML = state.accounts.length
        ? state.accounts.map(account => accountOption(account)).join("")
        : '<option value="">Brak rachunków</option>';
    historySelect.disabled = !state.accounts.length;
    document.getElementById("refresh-history").disabled = !state.accounts.length;
    updateTransferSummary();
}

function accountOption(account) {
    return `<option value="${account.id}">${accountTypeLabel(account.accountType)} · ${maskAccountNumber(account.accountNumber)} · ${account.currency}</option>`;
}

function updateTransferSummary() {
    const account = selectedAccount("transfer-source");
    document.getElementById("transfer-currency").textContent = account?.currency || "---";
    document.getElementById("transfer-account-balance").textContent = account
        ? formatMoney(account.balance, account.currency)
        : "0,00";
    document.getElementById("transfer-available-balance").textContent = account
        ? formatMoney(account.balance, account.currency)
        : "0,00";
    document.getElementById("transfer-account-number").textContent = account
        ? formatAccountNumber(account.accountNumber)
        : "Nie wybrano rachunku";
}

function selectedAccount(selectId) {
    const id = Number(document.getElementById(selectId).value);
    return state.accounts.find(account => account.id === id);
}

async function createAccount(event) {
    event.preventDefault();
    const form = event.currentTarget;
    clearFormErrors(form);
    if (!form.reportValidity()) {
        return;
    }

    const button = form.querySelector('[type="submit"]');
    setButtonLoading(button, true);
    try {
        const values = formDataObject(form);
        await apiRequest("/accounts", {
            method: "POST",
            body: JSON.stringify({
                accountType: values.accountType,
                currency: values.currency
            })
        });
        document.getElementById("account-dialog").close();
        showToast("Konto otwarte", "Nowy rachunek jest już dostępny w panelu.");
        await reloadDashboardData();
    } catch (error) {
        showFormErrors(form, error.fieldErrors || {});
        showToast("Nie udało się otworzyć konta", error.message, "error");
    } finally {
        setButtonLoading(button, false);
    }
}

function handleAccountAction(event) {
    const copyButton = event.target.closest("[data-copy-account]");
    const fundButton = event.target.closest("[data-fund-account]");
    const transferButton = event.target.closest("[data-transfer-account]");

    if (copyButton) {
        const account = accountById(copyButton.dataset.copyAccount);
        copyText(account.accountNumber);
    }
    if (fundButton) {
        openFundingDialog(accountById(fundButton.dataset.fundAccount));
    }
    if (transferButton) {
        document.getElementById("transfer-source").value = transferButton.dataset.transferAccount;
        updateTransferSummary();
        navigateToView("transfer");
    }
}

function openFundingDialog(account) {
    document.getElementById("funding-account-id").value = account.id;
    document.getElementById("funding-currency").textContent = account.currency;
    document.getElementById("funding-account-label").textContent =
        `${accountTypeLabel(account.accountType)} · ${formatAccountNumber(account.accountNumber)}`;
    document.getElementById("funding-dialog").showModal();
}

async function fundAccount(event) {
    event.preventDefault();
    const form = event.currentTarget;
    clearFormErrors(form);
    if (!form.reportValidity()) {
        return;
    }

    const values = formDataObject(form);
    const account = accountById(values.accountId);
    const button = form.querySelector('[type="submit"]');
    setButtonLoading(button, true);
    try {
        await apiRequest(`/demo/accounts/${account.id}/deposits`, {
            method: "POST",
            body: JSON.stringify({
                idempotencyKey: crypto.randomUUID(),
                amount: Number(values.amount),
                currency: account.currency
            })
        });
        form.reset();
        document.getElementById("funding-dialog").close();
        showToast("Konto zasilone", "Saldo rachunku zostało zaktualizowane.");
        await reloadDashboardData();
    } catch (error) {
        showFormErrors(form, error.fieldErrors || {});
        const message = error.status === 404
            ? "Zasilanie demonstracyjne nie jest aktywne w tym środowisku."
            : error.message;
        showToast("Nie udało się zasilić konta", message, "error");
    } finally {
        setButtonLoading(button, false);
    }
}

async function createTransfer(event) {
    event.preventDefault();
    const form = event.currentTarget;
    clearFormErrors(form);
    if (!form.reportValidity()) {
        return;
    }

    const values = formDataObject(form);
    const source = accountById(values.sourceAccountId);
    const button = form.querySelector('[type="submit"]');
    setButtonLoading(button, true);
    try {
        await apiRequest("/transfers", {
            method: "POST",
            body: JSON.stringify({
                idempotencyKey: crypto.randomUUID(),
                sourceAccountId: source.id,
                targetAccountNumber: values.targetAccountNumber.trim(),
                amount: Number(values.amount),
                currency: source.currency,
                description: values.description.trim()
            })
        });
        form.reset();
        renderAccountSelectors();
        showToast("Przelew wykonany", "Środki zostały zaksięgowane.");
        await reloadDashboardData();
        navigateToView("overview");
    } catch (error) {
        showFormErrors(form, error.fieldErrors || {});
        showToast("Przelew nie został wykonany", error.message, "error");
    } finally {
        setButtonLoading(button, false);
    }
}

async function updateProfile(event) {
    event.preventDefault();
    const form = event.currentTarget;
    clearFormErrors(form);
    if (!form.reportValidity()) {
        return;
    }

    const values = formDataObject(form);
    const button = form.querySelector('[type="submit"]');
    setButtonLoading(button, true);
    try {
        state.profile = await apiRequest("/profile", {
            method: "PUT",
            body: JSON.stringify({
                firstName: values.firstName.trim(),
                lastName: values.lastName.trim(),
                phoneNumber: values.phoneNumber.trim()
            })
        });
        renderProfile();
        showToast("Profil zaktualizowany", "Twoje dane zostały zapisane.");
    } catch (error) {
        showFormErrors(form, error.fieldErrors || {});
        showToast("Nie udało się zapisać profilu", error.message, "error");
    } finally {
        setButtonLoading(button, false);
    }
}

async function changePassword(event) {
    event.preventDefault();
    const form = event.currentTarget;
    clearFormErrors(form);
    if (!form.reportValidity()) {
        return;
    }

    const values = formDataObject(form);
    const button = form.querySelector('[type="submit"]');
    setButtonLoading(button, true);
    try {
        await apiRequest("/profile/change-password", {
            method: "POST",
            body: JSON.stringify({
                currentPassword: values.currentPassword,
                newPassword: values.newPassword
            })
        });
        showToast("Hasło zmienione", "Zaloguj się ponownie przy użyciu nowego hasła.");
        window.setTimeout(logout, 1200);
    } catch (error) {
        showFormErrors(form, error.fieldErrors || {});
        showToast("Nie udało się zmienić hasła", error.message, "error");
        setButtonLoading(button, false);
    }
}

async function reloadDashboardData() {
    state.accounts = await apiRequest("/accounts");
    state.statements.clear();
    renderAccounts();
    renderAccountSelectors();
    await loadAllStatements();
}

async function loadAllStatements() {
    if (!state.accounts.length) {
        renderActivity([]);
        renderSelectedHistory();
        return;
    }

    const results = await Promise.allSettled(state.accounts.map(loadStatement));
    const entries = [];
    results.forEach((result, index) => {
        if (result.status !== "fulfilled") {
            return;
        }
        const account = state.accounts[index];
        result.value.entries.forEach(entry => entries.push({ ...entry, account }));
    });
    entries.sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt));
    renderActivity(entries);
    renderSelectedHistory();
}

async function loadStatement(account) {
    // Include operations committed during the current second.
    const endDate = new Date(Date.now() + 1000);
    const startDate = new Date();
    startDate.setDate(startDate.getDate() - HISTORY_DAYS);
    const query = new URLSearchParams({
        startDate: toLocalIsoDateTime(startDate),
        endDate: toLocalIsoDateTime(endDate)
    });
    const statement = await apiRequest(`/accounts/${account.id}/statement?${query}`);
    state.statements.set(account.id, statement);
    return statement;
}

function renderActivity(entries) {
    document.getElementById("activity-count").textContent = entries.length;
    renderEntryRows(document.getElementById("recent-activity"), entries.slice(0, 5), false);
}

function renderSelectedHistory() {
    const container = document.getElementById("history-content");
    const account = selectedAccount("history-account");
    if (!account) {
        renderEmptyState(
            container,
            "list-ordered",
            "Brak historii",
            "Otwórz rachunek, aby zobaczyć wykonane operacje."
        );
        return;
    }

    const statement = state.statements.get(account.id);
    if (!statement) {
        container.innerHTML = '<div class="loading-row"><span class="spinner"></span></div>';
        return;
    }

    const entries = [...statement.entries]
        .sort((left, right) => new Date(right.createdAt) - new Date(left.createdAt))
        .map(entry => ({ ...entry, account }));

    container.innerHTML = `
        <div class="history-summary">
            <span>Rachunek<strong>${maskAccountNumber(account.accountNumber)}</strong></span>
            <span>Saldo początkowe<strong>${formatMoney(statement.openingBalance, account.currency)}</strong></span>
            <span>Saldo końcowe<strong>${formatMoney(statement.closingBalance, account.currency)}</strong></span>
            <span>Zakres<strong>Ostatnie ${HISTORY_DAYS} dni</strong></span>
        </div>
        <div data-history-rows></div>
    `;
    renderEntryRows(container.querySelector("[data-history-rows]"), entries, true);
}

async function refreshSelectedHistory() {
    const account = selectedAccount("history-account");
    if (!account) {
        return;
    }
    const button = document.getElementById("refresh-history");
    button.disabled = true;
    button.querySelector("svg")?.classList.add("spin");
    try {
        await loadStatement(account);
        renderSelectedHistory();
        showToast("Historia odświeżona", "Wyświetlasz najnowsze operacje.");
    } catch (error) {
        showToast("Nie udało się odświeżyć historii", error.message, "error");
    } finally {
        button.disabled = false;
    }
}

function renderEntryRows(container, entries, showAccount) {
    if (!entries.length) {
        renderEmptyState(
            container,
            "receipt-text",
            "Brak operacji",
            `Na tym rachunku nie odnotowano operacji z ostatnich ${HISTORY_DAYS} dni.`
        );
        return;
    }

    container.innerHTML = entries.map(entry => {
        const incoming = entry.type === "DEPOSIT" || entry.type === "TRANSFER_IN";
        const sign = incoming ? "+" : "-";
        return `
            <article class="activity-row">
                <span class="activity-icon ${incoming ? "activity-icon--in" : "activity-icon--out"}">
                    <i data-lucide="${entryIcon(entry.type)}"></i>
                </span>
                <span class="activity-description">
                    <strong>${escapeHtml(entryDescription(entry))}</strong>
                    <small>${formatDateTime(entry.createdAt)}</small>
                </span>
                <span class="activity-account"${showAccount ? "" : ""}>
                    <span>${accountTypeLabel(entry.account.accountType)}</span>
                    <small>${maskAccountNumber(entry.account.accountNumber)}</small>
                </span>
                <span class="activity-amount ${incoming ? "activity-amount--in" : "activity-amount--out"}">
                    <strong>${sign}${formatMoney(entry.amount, entry.currency)}</strong>
                    <small>${entryTypeLabel(entry.type)}</small>
                </span>
            </article>
        `;
    }).join("");
    initializeIcons();
}

function renderEmptyState(container, icon, title, message) {
    container.innerHTML = `
        <div class="empty-state">
            <i data-lucide="${icon}"></i>
            <strong>${escapeHtml(title)}</strong>
            <p>${escapeHtml(message)}</p>
        </div>
    `;
    initializeIcons();
}

async function logout() {
    try {
        await fetch("/auth/logout", {
            method: "POST",
            credentials: "same-origin"
        });
    } finally {
        clearAccessToken();
        window.location.replace("/login.html");
    }
}

function accountById(id) {
    return state.accounts.find(account => account.id === Number(id));
}

async function copyText(value) {
    try {
        await navigator.clipboard.writeText(value);
        showToast("Numer skopiowany", "Numer rachunku znajduje się w schowku.");
    } catch {
        showToast("Nie udało się skopiować", "Zaznacz numer rachunku i skopiuj go ręcznie.", "error");
    }
}

function formatMoney(value, currency) {
    return new Intl.NumberFormat("pl-PL", {
        style: "currency",
        currency,
        minimumFractionDigits: 2
    }).format(Number(value));
}

function formatAccountNumber(number) {
    return String(number).replace(/(\d{4})(?=\d)/g, "$1 ");
}

function maskAccountNumber(number) {
    const value = String(number);
    return `•••• ${value.slice(-4)}`;
}

function formatDateTime(value) {
    return new Intl.DateTimeFormat("pl-PL", {
        day: "2-digit",
        month: "short",
        year: "numeric",
        hour: "2-digit",
        minute: "2-digit"
    }).format(new Date(value));
}

function toLocalIsoDateTime(date) {
    const offset = date.getTimezoneOffset() * 60_000;
    return new Date(date.getTime() - offset).toISOString().slice(0, 23);
}

function accountTypeLabel(type) {
    return type === "SAVINGS" ? "Konto oszczędnościowe" : "Konto osobiste";
}

function accountStatusLabel(status) {
    return {
        ACTIVE: "Aktywne",
        BLOCKED: "Zablokowane",
        CLOSED: "Zamknięte"
    }[status] || status;
}

function accountStatusClass(status) {
    return {
        BLOCKED: "account-status--blocked",
        CLOSED: "account-status--closed"
    }[status] || "";
}

function activeAccountsLabel(count) {
    if (count === 1) {
        return "1 aktywny rachunek";
    }
    if (count >= 2 && count <= 4) {
        return `${count} aktywne rachunki`;
    }
    return `${count} aktywnych rachunków`;
}

function entryDescription(entry) {
    if (entry.description === "Demo account funding") {
        return "Zasilenie demonstracyjne";
    }
    return entry.description || entryTypeLabel(entry.type);
}

function entryTypeLabel(type) {
    return {
        DEPOSIT: "Zasilenie",
        TRANSFER_IN: "Przelew przychodzący",
        TRANSFER_OUT: "Przelew wychodzący"
    }[type] || type;
}

function entryIcon(type) {
    return {
        DEPOSIT: "circle-dollar-sign",
        TRANSFER_IN: "arrow-down-left",
        TRANSFER_OUT: "arrow-up-right"
    }[type] || "receipt-text";
}

function escapeHtml(value) {
    return String(value)
        .replaceAll("&", "&amp;")
        .replaceAll("<", "&lt;")
        .replaceAll(">", "&gt;")
        .replaceAll('"', "&quot;")
        .replaceAll("'", "&#039;");
}

document.addEventListener("DOMContentLoaded", async () => {
    initializeIcons();

    if (sessionStorage.getItem("registrationComplete") === "true") {
        sessionStorage.removeItem("registrationComplete");
        showAlert(
            document.getElementById("auth-alert"),
            "Konto zostało utworzone. Możesz się teraz zalogować.",
            true
        );
    }

    const page = document.body.dataset.page;
    if (page === "login") {
        await initializeLoginPage();
    } else if (page === "register") {
        initializeRegisterPage();
    } else if (page === "dashboard") {
        await initializeDashboard();
    }
});
