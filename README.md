# SubsTrack

**SubsTrack** is a REST API for tracking recurring subscriptions, managing multi-email user profiles, and delivering automated payment reminders.

Users register and authenticate with JWT, maintain subscriptions (billing cycle, price, currency, optional trial), configure reminder lead times per subscription, and receive scheduled emails before the next billing date.

Built as a **modular monolith** on **Java 25** and **Spring Boot 4**, with a clear package-by-feature boundary, Command–Query Separation, and facade-based inter-module communication.

---

## Table of Contents

- [Features](#features)
- [Tech Stack](#tech-stack)
- [Architecture](#architecture)
- [Domain Modules](#domain-modules)
- [API Overview](#api-overview)
- [Security](#security)
- [Payment Reminders](#payment-reminders)
- [Database](#database)
- [Getting Started](#getting-started)
- [Testing](#testing)
- [Project Structure](#project-structure)

---

## Features

- **JWT authentication** — register and login issue a Bearer token; all business endpoints require authentication
- **User account management** — profile updates, password change, account deletion
- **Multi-email profiles** — additional emails linked to an account, used as subscription notification targets
- **Subscription tracking** — name, price, currency, start/end dates, recurring period (`DAY` / `WEEK` / `MONTH` / `YEAR`), active flag, optional trial period
- **Configurable reminders** — one notification config per subscription with multiple lead times (`Duration`)
- **Scheduled email delivery** — daily cron job computes the next billing date (including trial handling), sends Thymeleaf-based reminders, and records sends for idempotency
- **Ownership enforcement** — resources are scoped to the authenticated user; cross-user access returns `403`
- **OpenAPI / Swagger UI** — interactive documentation with JWT bearer scheme
- **Centralized error handling** — uniform `ErrorResponse` for validation, business, and security failures
- **Schema migrations** — Liquibase changelogs with Hibernate `ddl-auto: validate`

---

## Tech Stack

| Area | Technology |
|------|------------|
| Language | Java 25 |
| Framework | Spring Boot 4.1 (Web MVC, Security, Data JPA, Mail, Validation, Liquibase, Thymeleaf) |
| Database | PostgreSQL 18 |
| Migrations | Liquibase (SQL changelogs) |
| Security | Spring Security + JJWT (HMAC) |
| Mapping | MapStruct, Lombok |
| API docs | springdoc-openapi |
| Email | Spring Mail + Thymeleaf text templates |
| Scheduling | Spring `@Scheduled` |
| Testing | JUnit 5, Mockito, MockMvc, Testcontainers (PostgreSQL) |
| Local infra | Docker Compose |

---

## Architecture

The application follows a **modular monolith (package-by-feature)** design combined with **CQS (Command–Query Separation)**.

Each domain module is split into public contracts (`api/`) and package-private implementation (`internal/`). Example for `subscription`:

```
subscription/
├── api/                          # Only surface other modules may use
│   ├── SubscriptionFacade.java
│   └── dto/
│       ├── command/              # Write intents
│       ├── query/                # Read intents
│       ├── request/              # HTTP inbound DTOs
│       └── response/             # Outbound DTOs
└── internal/                     # Package-private implementation
    ├── SubscriptionController
    ├── SubscriptionService
    ├── SubscriptionRepository
    ├── SubscriptionMapper
    └── Subscription
```

**Cross-module access** goes exclusively through facades (`UserFacade`, `SubscriptionFacade`, `EmailFacade`). Controllers, entities, and repositories stay package-private inside `internal/`.

**Request flow:** HTTP request → Controller → Command/Query → Service → Repository (or another module’s Facade) → Response DTO.

This keeps HTTP concerns at the edge, business rules in services, and module boundaries enforceable by package visibility.

---

## Domain Modules

| Module | Responsibility |
|--------|----------------|
| **auth** | Registration, login, JWT issuance/validation, Spring Security filter chain, BCrypt password encoding |
| **user** | Accounts, profile/password updates, multi-email management; exposes `UserFacade` |
| **subscription** | CRUD for recurring subscriptions; ownership checks; exposes `SubscriptionFacade` |
| **notification** | Reminder configuration per subscription; daily cron that triggers payment emails |
| **email** | Outbound mail via `JavaMailSender` and Thymeleaf templates; exposes `EmailFacade` |
| **common** | Shared OpenAPI config, exception hierarchy, and global exception handlers |
---

## API Overview

Base URL: `http://localhost:8080`  
Authenticated endpoints require: `Authorization: Bearer <jwtToken>`

### Auth — `/api/v1/auth`

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/register` | Public | Create account; returns JWT |
| `POST` | `/login` | Public | Authenticate; returns JWT |

### Users — `/api/v1/users`

| Method | Path | Description |
|--------|------|-------------|
| `GET` | `/me` | Current user profile |
| `DELETE` | `/me` | Delete account |
| `PUT` | `/me/profile` | Update username |
| `PUT` | `/me/password` | Change password |
| `POST` | `/me/emails` | Add email |
| `PUT` | `/me/emails` | Update email |
| `DELETE` | `/me/emails` | Delete email (primary email cannot be removed) |

### Subscriptions — `/api/v1/subscriptions`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/` | Create subscription |
| `GET` | `/` | List current user’s subscriptions |
| `GET` | `/{id}` | Get subscription by id |
| `PUT` | `/{id}` | Update subscription |
| `DELETE` | `/{id}` | Delete subscription |

### Notifications — `/api/v1/subscriptions/{subscriptionId}/notifications`

| Method | Path | Description |
|--------|------|-------------|
| `POST` | `/` | Create reminder config (one per subscription) |
| `GET` | `/` | Get reminder config |
| `PUT` | `/` | Update active flag and lead times |
| `DELETE` | `/` | Delete reminder config |

Interactive docs: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)  
OpenAPI JSON: [http://localhost:8080/v3/api-docs](http://localhost:8080/v3/api-docs)

---

## Security

- **Stateless JWT** — `SessionCreationPolicy.STATELESS`, CSRF disabled
- **Public endpoints** — `/api/v1/auth/register`, `/api/v1/auth/login`, Swagger / OpenAPI paths
- **All other endpoints** — authenticated
- **`JwtAuthenticationFilter`** — validates `Bearer` token and sets the security principal to the user’s `UUID`
- **Password hashing** — BCrypt
- **Uniform error responses** — `SecurityExceptionHandler` and `GlobalExceptionHandler`

JWT secret and expiration are configured via environment variables (`JWT_SECRET`, `JWT_EXPIRATION`).

---

## Payment Reminders

The scheduled job lives in the notification module. It loads subscription and recipient data through `SubscriptionFacade` / `UserFacade`, then sends mail via `EmailFacade` — without reaching into those modules’ internals.

1. User attaches a notification to a subscription with one or more lead times (e.g. 7 days, 1 day before billing).
2. A daily job runs at **08:00** (`0 0 8 * * *`).
3. For each active notification, the service resolves the next billing date (respecting trial length when enabled).
4. When a lead time matches today, an email is sent to the subscription’s linked address.
5. Sends are recorded in `notification_sent` with a unique constraint on `(notification_id, billing_date, days_before)` so the same reminder is not delivered twice. Failed sends roll back the claim so the job can retry.

---

## Database

PostgreSQL with Liquibase migrations and Hibernate validation (`ddl-auto: validate`).

| Table | Purpose |
|-------|---------|
| `users` | User accounts |
| `user_emails` | Emails linked to a user (globally unique) |
| `subscriptions` | Subscription records |
| `notifications` | Reminder config per subscription |
| `notification_durations` | Lead times (`Duration`) for a notification |
| `notification_sent` | Idempotency log for sent reminders |

**Relationships (logical):**

```
User 1──* UserEmail
User 1──* Subscription
UserEmail 1──* Subscription
Subscription 1──0..1 Notification
Notification 1──* Duration
Notification 1──* NotificationSent
```

---

## Getting Started

### Prerequisites

- JDK 25
- Maven 3.9+
- Docker & Docker Compose

### 1. Clone and configure environment

```bash
cp .env.example .env
```

Edit `.env` with your database, JWT, and SMTP credentials:

| Variable | Description |
|----------|-------------|
| `DB_USERNAME` / `DB_PASSWORD` / `DB_NAME` / `DB_URL` | PostgreSQL connection |
| `JWT_SECRET` | HMAC secret (at least 32 characters) |
| `JWT_EXPIRATION` | Token lifetime in milliseconds (e.g. `86400000` = 24h) |
| `MAIL_HOST` / `MAIL_PORT` / `MAIL_USERNAME` / `MAIL_PASSWORD` | SMTP settings |
| `MAIL_FROM` | Sender address for reminder emails |

For local email testing, a sandbox such as [Mailtrap](https://mailtrap.io) works well with the defaults in `.env.example`.

### 2. Start PostgreSQL

```bash
docker compose up -d
```

This starts PostgreSQL 18 on port `5432` with a persistent volume.

### 3. Run the application

```bash
mvn spring-boot:run
```

Liquibase applies migrations on startup. The API listens on **port 8080**.

### 4. Explore the API

Open Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html)

1. `POST /api/v1/auth/register`
2. Authorize with the returned `jwtToken`
3. Create subscriptions and notification configs

---

## Testing

```bash
mvn test
```

| Layer | Approach |
|-------|----------|
| **Unit** | JUnit 5 + Mockito (`AuthService`, `JwtService`, `UserService`, `SubscriptionService`, `NotificationService`, `EmailService`) |
| **Integration** | `@SpringBootTest` + MockMvc + Testcontainers PostgreSQL; `JavaMailSender` mocked |
| **Coverage areas** | Auth flows, user/email CRUD, subscription ownership, notification CRUD, reminder send path |

Integration tests share `AbstractIntegrationTest`, which wires a real Postgres instance via `@ServiceConnection` and application test configuration under `src/test/resources`.

---

## Project Structure

```
src/main/java/org/wilczewski/substrack/
├── auth/            # JWT auth & Spring Security
├── user/            # Accounts & multi-email profiles
├── subscription/    # Recurring subscription management
├── notification/    # Reminder config & scheduled delivery
├── email/           # SMTP + Thymeleaf templates
└── common/          # Exceptions, OpenAPI, shared handlers

src/main/resources/
├── application.yaml
├── db/changelog/    # Liquibase migrations
└── templates/email/ # Payment reminder template

docker-compose.yaml  # Local PostgreSQL
.env.example         # Environment template
```

---

## Highlights for Reviewers

- **Modular monolith** with explicit `api` / `internal` boundaries and facade-based coupling
- **CQS** at the application layer (commands/queries separate from HTTP request DTOs)
- **Stateless JWT security** with ownership checks on every resource
- **Idempotent scheduled reminders** backed by a unique constraint and retry-safe claim/rollback
- **Integration tests on real PostgreSQL** via Testcontainers, not an in-memory substitute
- **Production-oriented schema management** with Liquibase + Hibernate validate
