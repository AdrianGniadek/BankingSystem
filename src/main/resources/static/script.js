const BASE_URL = "";

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
                    localStorage.setItem("jwt", data.token);
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
    const token = localStorage.getItem("jwt");
    if (!token) {
        alert("Nie jesteś zalogowany");
        window.location.href = "login.html";
        return;
    }

    try {
        const userId = prompt("Podaj swoje ID użytkownika:");
        const response = await fetch(`${BASE_URL}/accounts/${userId}`, {
            headers: {
                "Authorization": `Bearer ${token}`
            }
        });

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

const token = localStorage.getItem('token');

if (!token) {
    window.location.href = '/login.html';
}

function logout() {
    localStorage.removeItem('token');
    window.location.href = '/login.html';
}

function showMessage(title, message) {
    document.getElementById('messageModalTitle').textContent = title;
    document.getElementById('messageModalBody').textContent = message;
    document.getElementById('messageModal').style.display = 'block';
}

async function loadProfile() {
    try {
        const response = await fetch('/api/profile', {
            headers: {
                'Authorization': `Bearer ${token}`,
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
    profileForm.addEventListener('submit', async function (e) {
        e.preventDefault();
        const profileData = {
            firstName: document.getElementById('firstName').value,
            lastName: document.getElementById('lastName').value,
            email: document.getElementById('email').value,
            phoneNumber: document.getElementById('phoneNumber').value
        };

        try {
            const response = await fetch('/api/profile', {
                method: 'PUT',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/json'
                },
                body: JSON.stringify(profileData)
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Nie udało się zaktualizować profilu');
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
            const response = await fetch('/api/profile/change-password', {
                method: 'POST',
                headers: {
                    'Authorization': `Bearer ${token}`,
                    'Content-Type': 'application/x-www-form-urlencoded',
                },
                body: `currentPassword=${encodeURIComponent(currentPassword)}&newPassword=${encodeURIComponent(newPassword)}`
            });

            if (!response.ok) {
                const error = await response.json();
                throw new Error(error.message || 'Nie udało się zmienić hasła');
            }

            passwordForm.reset();
            showMessage('Sukces', 'Hasło zostało zmienione pomyślnie');
        } catch (error) {
            showMessage('Błąd', error.message);
        }
    });

    loadProfile();
});


