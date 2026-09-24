# 💰 SmartSpender

> 💡 A personal finance dashboard that answers two questions: *Where is my money going?* and *How much is safe to spend today?*

SmartSpender is a full-stack expense and budget tracker with a **Vue 3** dashboard, a **Spring Boot 4 REST API**, and **PostgreSQL** storage. It focuses on speed and clarity — log a transaction in under 5 seconds, see categorized spending at a glance, and track envelope budgets for every category.

The project was built using **Test-Driven Development** and **object-oriented design** principles, with a strong emphasis on testable domain logic, clean layering, and production-shaped infrastructure.

---

## ✨ Features

### 💸 Transactions
- **Quick Add modal** — log a transaction in seconds
- **Mandatory categories** — every transaction belongs to a category
- **Per-period browsing** — filter by month or year
- **Cross-user isolation** — you can only see and edit your own data

### 🎯 Budgets (Envelopes)
- Set a monthly soft limit per category
- Utilization computed in the backend: `OK` / `WARNING` (≥80%) / `EXCEEDED` (≥100%)
- Days remaining in the current period
- One-click carry-over to copy last month's budgets

### 📊 Dashboard
- **Row 1** — Total Income, Total Expense, Net Flow, Average per Day/Month
- **Row 2** — Expense by Category (donut), Budget Envelopes (list), Income by Category (donut)
- **Period switcher** — jump between months and years, or navigate back/forward

### 🔐 Auth
- Email + password registration with BCrypt hashing
- JWT-based stateless sessions
- **Email verification on signup** (console-based in dev)

---

## 🚀 Tech Stack

| Layer | Technology |
|-------|-----------|
| **Frontend** | Vue 3, Vite, TypeScript, PrimeVue 4, Pinia, Chart.js, Axios |
| **Backend** | Spring Boot 4, Spring Web, Spring Data JPA, Spring Security, Java 25 |
| **Persistence** | Hibernate 7, Flyway migrations, JJWT for tokens, BCrypt for password hashing |
| **Database** | PostgreSQL 16 |
| **Build & Test** | Maven, JUnit 5, Mockito, AssertJ, Spring MockMvc |
| **Infrastructure** | Docker, Docker Compose, Nginx (multi-stage builds) |
| **Tooling** | Git, AI-assisted development (Claude, ChatGPT) |

---

## 🧠 Engineering Practices

### 🔴🟢♻️ Test-Driven Development

The backend was developed using TDD, following the **red → green → refactor** cycle for every feature:

- **Tests written first.** Each new behavior starts as a failing test that describes the intended API and edge cases.
- **Minimum code to pass.** Implementations are written only to satisfy the current failing test, which prevents speculative over-engineering.
- **Refactor with confidence.** Once green, the code is cleaned up while the test suite protects against regressions.
- **Fail fast, fail loud.** Mockito's strict stubbing caught design smells early — for example, an awkward service signature was refactored *because the test made it obvious*, not because a bug surfaced.

**110+ automated tests** cover:
- Service-layer business rules (uniqueness, cross-user isolation, period math)
- REST controller contracts (status codes, JSON shape, error envelopes)
- Domain value objects (`PeriodSelection`, `SpendingSummary`, `EnvelopeStatus`)

```bash
cd backend
mvn test
```

### 🏛️ Object-Oriented Design

The codebase applies classic OOP principles throughout:

| Principle | Where it shows up |
|-----------|-------------------|
| **Encapsulation** | Entities validate their own invariants in constructors — `Transaction`, `Category`, `Budget` cannot be put into an invalid state from the outside. |
| **Single Responsibility** | Controller binds HTTP, service enforces use-case rules, repository handles persistence, entity enforces invariants. Each layer knows only its job. |
| **Tell, Don't Ask** | `EnvelopeStatus.of(budget, spent)` computes its own derived metrics — callers never re-derive utilization on the frontend. |
| **Value Objects** | `SpendingSummary`, `CategorySpending`, `EnvelopeStatus`, `PeriodSelection` are immutable records that encapsulate derived behavior. |
| **Dependency Inversion** | Services depend on repository *interfaces*, not concrete implementations — enables straightforward mocking and future swapping. |
| **Composition over inheritance** | Vue frontend uses composables (`usePeriod`, `useToast`) rather than an inheritance hierarchy. |
| **Fail-fast validation** | `IllegalArgumentException` from an entity constructor fires the moment invalid data enters the domain, not three layers later. |

### 🧱 Layered Architecture

```
HTTP  →  Controller  →  Service  →  Repository  →  PostgreSQL
         (bind)        (rules)     (persistence)
```

- **DTO-at-boundary pattern.** Services convert entities to DTOs *inside* the `@Transactional` boundary, so lazy-loaded relations never leak past the transaction and blow up during JSON serialization. This is the fix for the classic `LazyInitializationException` in JPA + REST apps.
- **Uniform error contract.** Every endpoint returns an `ApiResponse<T>` envelope with a machine-readable error code — the frontend maps codes to user-friendly messages in one place.

### 📈 Continuous Improvement

- Flyway migrations are **immutable** once applied; changes always land as new migrations.
- Strict mode in Maven Surefire and TypeScript (`noUnusedLocals`, `erasableSyntaxOnly`) keeps dead code out.
- Refactors are always preceded by tests; the git history reads as a red → green → refactor narrative.

---

## 🗂️ Project Structure

```
smartspender/
├── backend/                    Spring Boot API
│   ├── src/main/java/com/smartspender/
│   │   ├── auth/               JWT, login, verification
│   │   ├── user/               User entity + repository
│   │   ├── category/           Category CRUD + validation
│   │   ├── transaction/        Core transaction logic + aggregations
│   │   ├── budget/             Envelope budgeting
│   │   ├── common/             Shared utilities, exceptions, period model
│   │   ├── config/             Security, CORS, argument resolvers
│   │   └── mail/               Mail sender (console for dev)
│   ├── src/main/resources/
│   │   ├── application.yml     Configuration
│   │   └── db/migration/       Flyway SQL migrations
│   └── src/test/               110+ tests
│
├── frontend/                   Vue 3 SPA
│   ├── src/
│   │   ├── api/                Typed HTTP clients
│   │   ├── stores/             Pinia stores
│   │   ├── views/              Page components
│   │   ├── components/         Reusable UI
│   │   ├── layouts/            Layout shell
│   │   ├── router/             Vue Router config
│   │   ├── types/              TypeScript interfaces
│   │   ├── utils/              Formatters, error messages
│   │   └── assets/styles/      Design system
│   └── nginx.conf              SPA + API proxy config
│
├── docker-compose.yml          Full-stack orchestration
└── README.md
```

---

## 🚦 Getting Started

### 📋 Prerequisites

- **Docker Desktop** (recommended) — for the containerized setup
- **Or** for local development: Java 25, Maven 3.9+, Node 20+, PostgreSQL 16

### 🐳 Quick Start — Docker (recommended)

```bash
git clone <your-repo-url> smartspender
cd smartspender

# Bring up the entire stack (Postgres + backend + frontend)
docker compose up -d

# Wait ~30 seconds for the backend to start
# Then open http://localhost
```

The database is seeded with a test user and ~1,400 transactions spanning 2022–2026:

| Email | Password |
|-------|----------|
| `smartspender@test.local` | `hunter2hunter2` |

### 💻 Local Development

If you want to modify code with hot reload, run the services directly.

#### 1️⃣ Start PostgreSQL

```bash
docker compose up -d postgres
```

Postgres runs on `localhost:5432`, database `smartspender`, credentials `smartspender` / `smartspender`.

#### 2️⃣ Start the backend

```bash
cd backend
mvn spring-boot:run
```

Backend runs on **http://localhost:8080**. Flyway applies migrations on startup.

#### 3️⃣ Start the frontend

```bash
cd frontend
npm install
npm run dev
```

Frontend runs on **http://localhost:5173**. The Vite dev server proxies `/api/*` to the backend.

---

## 🔌 API Overview

All endpoints are prefixed with `/api` and (except for `/auth/**`) require a `Bearer <token>` header.

### 🔐 Auth

| Method | Endpoint | Description |
|--------|----------|-------------|
| POST | `/api/auth/register` | Create an account (sends verification email) |
| POST | `/api/auth/login` | Authenticate and receive a JWT |
| GET | `/api/auth/verify?token=...` | Verify email, returns a JWT |
| POST | `/api/auth/resend-verification` | Resend the verification email |
| GET | `/api/auth/me` | Current user info |

### 🏷️ Categories

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/categories` | List all categories for the current user |
| POST | `/api/categories` | Create a category |
| PUT | `/api/categories/{id}` | Rename / recolor / change icon |
| DELETE | `/api/categories/{id}` | Delete (refuses if transactions reference it) |

### 💸 Transactions

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/transactions?period=MONTH&year=2026&month=9` | List transactions in a period |
| POST | `/api/transactions` | Create a transaction |
| GET | `/api/transactions/summary?period=...` | Aggregated totals for a period |
| GET | `/api/transactions/by-category?period=...&type=EXPENSE` | Category breakdown |
| GET | `/api/transactions/largest?period=...` | Largest expense in the period |

### 🎯 Budgets

| Method | Endpoint | Description |
|--------|----------|-------------|
| GET | `/api/budgets/envelopes?year=2026&month=9` | Envelopes with computed utilization |
| PUT | `/api/budgets` | Create or update a budget |
| DELETE | `/api/budgets/{id}` | Delete a budget |
| POST | `/api/budgets/copy-from?fromYear=...&toYear=...` | Duplicate budgets between periods |

### 📦 Response Format

Every endpoint returns a uniform envelope:

```json
{
  "data": { "...": "..." },
  "timestamp": "2026-09-23T18:00:00.000Z"
}
```

Errors include a machine-readable code:

```json
{
  "error": {
    "code": "CATEGORY_COLOR_TAKEN",
    "message": "Color #22c55e is already used by another category"
  },
  "timestamp": "..."
}
```

---

## 🧪 Testing

```bash
cd backend
mvn test
```

The suite is organized by domain and follows the same layering as the production code:

| Test class | Coverage |
|------------|----------|
| `TransactionServiceTest` | Business rules for transactions (validation, ownership, aggregations) |
| `TransactionControllerTest` | HTTP contract (status codes, JSON shape, validation errors) |
| `CategoryServiceTest` | Category uniqueness, color conflicts, delete guards |
| `BudgetServiceTest` | Envelope upsert, copy-from, cross-period logic |
| `PeriodSelectionTest` | Value-object validation, leap years, date ranges |
| `JwtServiceTest` | Token generation, expiry, signature rejection |
| `AuthServiceTest` | Registration, login, email verification |
| `GlobalExceptionHandlerTest` | Error mapping to `ApiResponse` |

**100+ tests total.** Every service method and every controller endpoint has coverage for the happy path, edge cases, and cross-user security boundaries.

---

## 🤖 AI-Assisted Development

AI tools (GitHub Copilot, Gemini, and ChatGPT) were used throughout development to accelerate mechanical work and support learning. Effective uses included:

- **Scaffolding and boilerplate** — generating initial project structure, Maven dependencies, and TypeScript configs.
- **Test-first drafts** — producing initial failing tests from feature descriptions, which were then refined to match domain intent.
- **Refactoring support** — proposing cleaner service signatures and surfacing code smells (e.g. `createCategory(User, ...)` → `createCategory(Long userId, ...)`).
- **Seed data generation** — designing and generating ~1,400 realistic transactions across 4 years, including edge cases like very long strings, unicode, and boundary dates.
- **Docker and infrastructure setup** — scaffolding Dockerfiles, Nginx config, and `docker-compose.yml`.
- **Error contract design** — generating the machine-code → user-friendly-message lookup table used on the frontend.
- **Documentation** — drafting the README, API overview, and inline comments.

---

## 📄 License

MIT — see `LICENSE`