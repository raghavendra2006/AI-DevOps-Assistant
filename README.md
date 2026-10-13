# AI DevOps Assistant

An intelligent, cloud-native DevOps automation platform featuring AI-assisted observability, CI/CD pipeline management, Kubernetes orchestration, and enterprise-grade security.

---

## Phase 1: Authentication & User Management

Phase 1 provides the security and identity foundation for the platform:

- **Stateless JWT Security**: Short-lived Access Tokens (15 min) + DB-persisted, rotating Refresh Tokens (7 days).
- **Password Security**: BCrypt password hashing with cost factor 12.
- **Role-Based Access Control (RBAC)**: Method-level authorization (`@PreAuthorize`) supporting `ROLE_ADMIN`, `ROLE_ENGINEER`, and `ROLE_VIEWER`.
- **Email Verification & Password Recovery**: Cryptographically secure, expiring one-time tokens for account activation and password resets.
- **Brute-Force Rate Limiting**: In-memory token bucket filter powered by **Bucket4j** (5 attempts/min per IP on authentication endpoints, returning HTTP `429 Too Many Requests`).
- **Security Audit Trails**: Asynchronous, decoupled event publishing capturing all `LOGIN`, `LOGOUT`, `ROLE_CHANGE`, and `PASSWORD_RESET` events with client IP and User-Agent.
- **Global Exception Handling**: RFC-compliant error envelopes with validation error mappings.
- **OpenAPI 3 / Swagger Documentation**: Interactive API documentation with BearerAuth JWT support.
- **Frontend SPA**: React 18 + TypeScript + Vite + Redux Toolkit featuring **Axios silent token refresh interceptor with queued request replay**, protected routes, and role guards.

---

## Architecture Overview

```
                      ┌───────────────────────────────────────────────┐
                      │                 React Frontend                │
                      │   (Redux Toolkit + Axios Silent Interceptor)  │
                      └───────────────────────┬───────────────────────┘
                                              │ Bearer JWT / JSON
                                              ▼
┌─────────────────────────────────────────────────────────────────────────────────────┐
│                             Spring Boot 3.3 Backend                                 │
│                                                                                     │
│  ┌───────────────────────┐   ┌───────────────────────┐   ┌───────────────────────┐  │
│  │ RateLimitingFilter    │──▶│ JwtAuthFilter         │──▶│ SecurityContextHolder │  │
│  │ (Bucket4j / IP-based) │   │ (Signature/Expiry)    │   │ (Method-level RBAC)   │  │
│  └───────────────────────┘   └───────────────────────┘   └───────────┬───────────┘  │
│                                                                      ▼              │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │ Controllers: AuthController | UserController | AuditLogController             │  │
│  └───────────────────────────────────┬───────────────────────────────────────────┘  │
│                                      ▼                                              │
│  ┌───────────────────────────────────────────────────────────────────────────────┐  │
│  │ Services: AuthService | JwtService | UserService | AuditLogService            │  │
│  └───────────────────────┬───────────────────────────────┬───────────────────────┘  │
│                          │                               │                          │
│                          ▼ (Async Event)                 ▼ (Spring Data JPA)        │
│  ┌───────────────────────────────┐               ┌───────────────────────────────┐  │
│  │ Security Audit Event Bus      │               │ PostgreSQL 16 Database        │  │
│  │ (audit_logs table)            │               │ (Flyway V1, V2 Migrations)    │  │
│  └───────────────────────────────┘               └───────────────────────────────┘  │
└─────────────────────────────────────────────────────────────────────────────────────┘
```

---

## Quick Start & Local Execution

### 1. Database (Docker Compose)
Start the PostgreSQL 16 container:
```bash
docker compose up -d
```

### 2. Backend (Spring Boot 3.3 + Java 21)
Using the included Maven Wrapper:
```bash
cd backend
./mvnw clean test
./mvnw spring-boot:run
```
- API Base URL: `http://localhost:8080/api/v1`
- Swagger UI Documentation: `http://localhost:8080/swagger-ui.html`
- OpenAPI Specification: `http://localhost:8080/v3/api-docs`

### 3. Frontend (React + TypeScript + Vite)
```bash
cd frontend
npm install
npm run dev
```
- Frontend Dev Server: `http://localhost:5173`

---

## Default Credentials & Seed Data

The database is pre-seeded via Flyway migration `V2__seed_roles_and_admin.sql`:

| Role | Email | Password | Permissions |
| :--- | :--- | :--- | :--- |
| **Super Admin** | `admin@aidevops.io` | `Admin@DevOps2026!` | Full administrative access, RBAC management, audit log inspection |

---

## REST API Specification

### Authentication (`/api/v1/auth`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `POST` | `/api/v1/auth/register` | Public | Register new user (assigns `ROLE_VIEWER`, generates verification token) |
| `GET` | `/api/v1/auth/verify-email?token={token}` | Public | Verify user email and activate account |
| `POST` | `/api/v1/auth/login` | Public (Rate-limited) | Authenticate user, returns Access Token (15m) + Refresh Token (7d) |
| `POST` | `/api/v1/auth/refresh` | Public | Rotate refresh token and issue new token pair |
| `POST` | `/api/v1/auth/logout` | Authenticated | Revoke refresh tokens and record audit trail |
| `POST` | `/api/v1/auth/forgot-password` | Public (Rate-limited) | Request password reset token |
| `POST` | `/api/v1/auth/reset-password` | Public | Set new password with one-time reset token |
| `GET` | `/api/v1/auth/me` | Authenticated | Retrieve current user profile and active roles |

### User & RBAC Management (`/api/v1/users`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/users` | `ROLE_ADMIN` | Paginated user directory with role assignments |
| `GET` | `/api/v1/users/{id}` | `ROLE_ADMIN` | Retrieve user by ID |
| `PUT` | `/api/v1/users/{id}/roles` | `ROLE_ADMIN` | Update roles for a user (emits `ROLE_CHANGED` audit log) |
| `DELETE`| `/api/v1/users/{id}` | `ROLE_ADMIN` | Delete user |

### Security Audit Trail (`/api/v1/audit-logs`)
| Method | Endpoint | Access | Description |
| :--- | :--- | :--- | :--- |
| `GET` | `/api/v1/audit-logs` | `ROLE_ADMIN` | Paginated security audit trail |
| `GET` | `/api/v1/audit-logs/recent`| `ROLE_ADMIN` | Recent 50 audit log events |

---

## Automated Test Coverage

The test suite includes both Mockito unit tests for service layers and MockMvc integration tests for authentication endpoints:

```bash
cd backend
./mvnw test
```

- **`AuthServiceTest`**: 7 unit tests (registration, duplicate checks, login, password verification, token rotation, revoked tokens).
- **`UserServiceTest`**: 3 unit tests (user pagination, RBAC updates, audit event publishing).
- **`JwtServiceTest`**: 1 unit test (token signing, expiration, subject extraction).
- **`AuthControllerIntegrationTest`**: 3 integration tests (full end-to-end register ➔ verify ➔ login ➔ refresh flow, bad credentials 401 handling, and RBAC 403 authorization checks).
- **Total**: 14 tests, 0 failures, 0 errors.