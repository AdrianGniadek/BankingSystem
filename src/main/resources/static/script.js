const BASE_URL = "";

document.addEventListener("DOMContentLoaded", () => {
    const loginForm = document.getElementById("login-form");
    if (loginForm) {
        loginForm.addEventListener("submit", async (e) => {
            e.preventDefault();
            const email = document.getElementById("email").value;
            const password = document.getElementById("password").value;

            try {
                const response = await fetch(`${BASE_URL}/login`, {
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
