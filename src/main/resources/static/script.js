const BASE_URL = "";
const ACCESS_TOKEN_KEY = "accessToken";
let refreshPromise = null;

function storeAccessToken(token) {
    sessionStorage.setItem(ACCESS_TOKEN_KEY, token);
}

function clearAccessToken() {
    sessionStorage.removeItem(ACCESS_TOKEN_KEY);
    localStorage.removeItem("jwt");
}

async function refreshAccessToken() {
    if (refreshPromise) {
        return refreshPromise;
    }

    refreshPromise = requestNewAccessToken();
    try {
        return await refreshPromise;
    } finally {
        refreshPromise = null;
    }
}

async function requestNewAccessToken() {
    try {
        const response = await fetch(`${BASE_URL}/auth/refresh`, {
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
    } catch (error) {
        clearAccessToken();
        return false;
    }
}

async function authenticatedFetch(url, options = {}) {
    const requestOptions = { ...options };
    requestOptions.headers = new Headers(options.headers || {});
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

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = document.getElementById("email").value;
            const password = document.getElementById("password").value;

            try {
                const response = await fetch(`${BASE_URL}/auth/login`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify({ email, password })
                });

                if (response.ok) {
                    const data = await response.json();
                    storeAccessToken(data.accessToken);
                    alert("Zalogowano!");
                    window.location.href = "accounts.html";
                } else {
                    alert("Błąd logowania");
                }
            } catch (err) {
                console.error("Błąd:", err);
                alert("Błąd połączenia");
            }
        });
    }
});

async function fetchAccounts() {
    try {
        const userId = prompt("Podaj swoje ID użytkownika:");
        const response = await authenticatedFetch(`${BASE_URL}/accounts/${userId}`);

        if (response.ok) {
            const accounts = await response.json();
            const list = document.getElementById("accounts-list");
            list.innerHTML = "";
            accounts.forEach(acc => {
                const item = document.createElement("li");
                item.textContent = `Konto ID: ${acc.id}, Numer: ${acc.accountNumber}, Saldo: ${acc.balance} ${acc.currency}`;
                list.appendChild(item);
            });
        } else {
            alert("Nie udało się pobrać kont.");
        }
    } catch (err) {
        console.error("Błąd:", err);
        alert("Błąd połączenia");
    }
}

document.addEventListener("DOMContentLoaded", () => {
    const registerForm = document.getElementById("register-form");
    if (registerForm) {
        registerForm.addEventListener("submit", async (e) => {
            e.preventDefault();

            const user = {
                firstName: document.getElementById("firstName").value,
                lastName: document.getElementById("lastName").value,
                email: document.getElementById("email").value,
                password: document.getElementById("password").value,
                pesel: document.getElementById("pesel").value,
                phoneNumber: document.getElementById("phoneNumber").value
            };

            try {
                const response = await fetch(`${BASE_URL}/auth/register`, {
                    method: "POST",
                    headers: { "Content-Type": "application/json" },
                    body: JSON.stringify(user)
                });

                if (response.ok) {
                    alert("Rejestracja zakończona sukcesem! Możesz się teraz zalogować.");
                    window.location.href = "login.html";
                } else {
                    const msg = await response.text();
                    alert("Błąd rejestracji: " + msg);
                }
            } catch (err) {
                console.error("Błąd:", err);
                alert("Błąd połączenia z serwerem");
            }
        });
    }
});

async function logout() {
    try {
        await fetch('/auth/logout', {
            method: 'POST',
            credentials: 'same-origin'
        });
    } finally {
        clearAccessToken();
    }
    window.location.href = '/login.html';
}

function showMessage(title, message) {
    document.getElementById('messageModalTitle').textContent = title;
    document.getElementById('messageModalBody').textContent = message;
    document.getElementById('messageModal').style.display = 'block';
}

async function loadProfile() {
    try {
        const response = await authenticatedFetch('/profile', {
            headers: {
                'Content-Type': 'application/json'
            }
        });

        if (!response.ok) {
            throw new Error('Nie udało się załadować danych profilu');
        }

        const profile = await response.json();
        document.getElementById('firstName').value = profile.firstName || '';
        document.getElementById('lastName').value = profile.lastName || '';
        document.getElementById('email').value = profile.email || '';
        document.getElementById('phoneNumber').value = profile.phoneNumber || '';
    } catch (error) {
        showMessage('Błąd', error.message);
    }
}

document.addEventListener('DOMContentLoaded', function () {
    const profileForm = document.getElementById('profileForm');
    if (!profileForm) {
        return;
    }
    profileForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        const profileData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            phoneNumber: document.getElementById('phoneNumber').value
        };

        try {
            const response = await authenticatedFetch('/profile', {
                method: 'PUT',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(profileData)
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.detail || 'Nie udało się zaktualizować profilu');
            }

            showMessage('Sukces', 'Dane profilu zostały zaktualizowane pomyślnie');
        } catch (error) {
            showMessage('Błąd', error.message);
        }
    });

    const passwordForm = document.getElementById('passwordForm');
    passwordForm.addEventListener('submit', async function (e) {
        e.preventDefault();

        const currentPassword = document.getElementById('currentPassword').value;
        const newPassword = document.getElementById('newPassword').value;

        try {
            const response = await authenticatedFetch('/profile/change-password', {
                method: 'POST',
                headers: {
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify({ currentPassword, newPassword })
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.detail || 'Nie udało się zmienić hasła');
            }

            passwordForm.reset();
            await logout();
        } catch (error) {
            showMessage('Błąd', error.message);
        }
    });

    loadProfile();
});


