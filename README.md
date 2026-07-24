# BankingSystem

[![Build](https://github.com/AdrianGniadek/BankingSystem/actions/workflows/build.yml/badge.svg)](https://github.com/AdrianGniadek/BankingSystem/actions/workflows/build.yml)
![Java](https://img.shields.io/badge/Java-21-ED8B00)
![Spring Boot](https://img.shields.io/badge/Spring_Boot-3.5-6DB33F)

BankingSystem to edukacyjna aplikacja bankowa typu full stack. Pozwala użytkownikowi
założyć konto, zarządzać rachunkami, wykonywać przelewy i przeglądać historię
operacji. Backend udostępnia REST API, a responsywny frontend jest serwowany
bezpośrednio przez Spring Boot.

## Funkcje

- rejestracja, logowanie, odświeżanie sesji i wylogowanie,
- dostęp chroniony tokenami JWT i rotowanymi refresh tokenami,
- tworzenie rachunków osobistych i oszczędnościowych,
- obsługa rachunków w walutach PLN, EUR i USD,
- przelewy wykonywane na 20-cyfrowy numer rachunku,
- historia operacji oraz zestawienia salda,
- demonstracyjne zasilanie rachunku w środowisku lokalnym,
- edycja profilu i zmiana hasła,
- operacje administracyjne na kontach użytkowników,
- responsywny panel klienta na komputerze i telefonie.

## Technologie

| Obszar | Technologie |
| --- | --- |
| Backend | Java 21, Spring Boot 3.5, Spring Web |
| Bezpieczeństwo | Spring Security, JWT, BCrypt |
| Dane | Spring Data JPA, Hibernate, MySQL 8 |
| Migracje | Flyway |
| Frontend | HTML, CSS, JavaScript, Lucide Icons |
| Testy | JUnit 5, Mockito, MockMvc, H2, Testcontainers |
| CI | GitHub Actions, Maven Wrapper |

## Architektura

Kod backendu jest podzielony na warstwy:

```text
controller -> service -> repository -> database
                 |
              mapper / DTO
```

- `controller` definiuje endpointy REST i waliduje dane wejściowe,
- `service` zawiera reguły biznesowe i granice transakcji,
- `repository` odpowiada za komunikację z bazą danych,
- `DTO` określają kontrakty API bez ujawniania encji,
- `mapper` przekształca encje na obiekty odpowiedzi,
- `security` obsługuje uwierzytelnianie i autoryzację,
- migracje Flyway wersjonują schemat bazy danych.

Operacje zmieniające saldo korzystają z blokad pesymistycznych, wpisów księgowych
i kluczy idempotencji. Ogranicza to ryzyko podwójnego wykonania żądania oraz
problemów przy równoczesnych operacjach.

## Wymagania

- Java 21,
- MySQL 8,
- Docker Desktop, jeśli baza lub testy integracyjne mają działać w kontenerze.

Nie trzeba instalować Mavena. Repozytorium zawiera Maven Wrapper.

## Uruchomienie lokalne

### 1. Pobierz projekt

```powershell
git clone https://github.com/AdrianGniadek/BankingSystem.git
cd BankingSystem
```

### 2. Uruchom MySQL

Możesz wykorzystać istniejącą lokalną instancję MySQL albo uruchomić bazę
w Dockerze:

```powershell
docker run --name banking-mysql `
  -e MYSQL_ROOT_PASSWORD=banking_password `
  -e MYSQL_DATABASE=banking_system `
  -p 3306:3306 `
  -d mysql:8.0
```

Przy kolejnych uruchomieniach wystarczy:

```powershell
docker start banking-mysql
```

### 3. Ustaw zmienne środowiskowe

W PowerShell:

```powershell
$env:DB_PASSWORD = "banking_password"
$env:JWT_SECRET = "local-development-secret-change-me-123456"
```

Sekret JWT musi zawierać co najmniej 32 znaki. Powyższa wartość jest wyłącznie
przykładem lokalnym i nie powinna być używana w środowisku publicznym.

### 4. Uruchom aplikację

Na Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Na Linux lub macOS:

```bash
export DB_PASSWORD="banking_password"
export JWT_SECRET="local-development-secret-change-me-123456"
./mvnw spring-boot:run
```

Aplikacja będzie dostępna pod adresem:

```text
http://localhost:8080
```

Profil `local` jest domyślny. Automatycznie uruchamia migracje Flyway i włącza
demonstracyjne zasilanie rachunku.

## Konfiguracja

Najważniejsze zmienne środowiskowe:

| Zmienna | Wymagana | Wartość domyślna | Znaczenie |
| --- | --- | --- | --- |
| `JWT_SECRET` | tak | brak | Sekret podpisujący JWT, minimum 32 znaki |
| `DB_PASSWORD` | tak | brak | Hasło użytkownika MySQL |
| `DB_URL` | nie | lokalny MySQL | Adres bazy danych |
| `DB_USERNAME` | nie | `root` | Użytkownik bazy |
| `JWT_ACCESS_TOKEN_EXPIRATION` | nie | `15m` | Czas życia access tokenu |
| `JWT_REFRESH_TOKEN_EXPIRATION` | nie | `30d` | Czas życia refresh tokenu |
| `REFRESH_COOKIE_SECURE` | nie | `false` lokalnie | Cookie tylko przez HTTPS |
| `DEMO_FUNDING_ENABLED` | nie | `true` lokalnie | Zasilanie demonstracyjne |
| `JPA_SHOW_SQL` | nie | `false` | Logowanie zapytań SQL |

## Najważniejsze endpointy

| Metoda | Endpoint | Opis |
| --- | --- | --- |
| `POST` | `/auth/register` | Rejestracja użytkownika |
| `POST` | `/auth/login` | Logowanie i utworzenie sesji |
| `POST` | `/auth/refresh` | Odświeżenie sesji |
| `POST` | `/auth/logout` | Unieważnienie sesji |
| `GET` | `/accounts` | Rachunki zalogowanego użytkownika |
| `POST` | `/accounts` | Utworzenie rachunku |
| `GET` | `/accounts/{id}/statement` | Zestawienie operacji rachunku |
| `POST` | `/transfers` | Wykonanie przelewu |
| `GET` | `/transfers/{accountId}` | Stronicowana lista przelewów |
| `GET`, `PUT` | `/profile` | Odczyt i aktualizacja profilu |

API zwraca błędy w standardzie
[Problem Details for HTTP APIs](https://www.rfc-editor.org/info/rfc9457).

## Testy

Testy jednostkowe, warstwy webowej i bezpieczeństwa:

```powershell
.\mvnw.cmd clean test
```

Pełna weryfikacja z testami integracyjnymi MySQL:

```powershell
.\mvnw.cmd clean verify -Pintegration-tests
```

Testy integracyjne używają Testcontainers, dlatego wymagają uruchomionego
Dockera. Obejmują również scenariusze współbieżnych przelewów i zmian statusu
rachunku.

Każdy push i pull request do `main` lub `develop` uruchamia ten sam proces
weryfikacji w GitHub Actions.
