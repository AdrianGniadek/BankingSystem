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

- Docker Desktop z Docker Compose.

Java 21 jest potrzebna tylko wtedy, gdy aplikacja ma być uruchamiana poza
kontenerem. Nie trzeba instalować Mavena, ponieważ repozytorium zawiera Maven
Wrapper.

## Uruchomienie lokalne

### 1. Pobierz projekt

```powershell
git clone https://github.com/AdrianGniadek/BankingSystem.git
cd BankingSystem
```

### 2. Przygotuj konfigurację

Utwórz lokalny plik `.env` na podstawie przykładu:

```powershell
Copy-Item .env.example .env
```

Na Linux lub macOS:

```bash
cp .env.example .env
```

Przed uruchomieniem zmień wartości `DB_PASSWORD`, `MYSQL_ROOT_PASSWORD` oraz
`JWT_SECRET` w pliku `.env`. Sekret JWT musi zawierać co najmniej 32 znaki.

### 3. Uruchom aplikację

```powershell
docker compose up --build
```

Po uruchomieniu aplikacja będzie dostępna pod adresem:

```text
http://localhost:8080
```

Zatrzymanie kontenerów:

```powershell
docker compose down
```

Wyczyszczenie kontenerów razem z lokalnymi danymi MySQL:

```powershell
docker compose down --volumes
```

Profil `local` jest domyślny. Automatycznie uruchamia migracje Flyway i włącza
demonstracyjne zasilanie rachunku.

### Uruchomienie z IntelliJ IDEA

Jeśli aplikacja ma działać bez kontenera, uruchom sam MySQL:

```powershell
docker compose up -d mysql
```

Następnie ustaw `DB_USERNAME`, `DB_PASSWORD` i `JWT_SECRET` w konfiguracji
uruchomieniowej IntelliJ IDEA i uruchom klasę `BankingSystemApplication`.

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
