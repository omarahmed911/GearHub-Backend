# GearHub Backend

**GearHub** is a REST API for an auto-parts marketplace MVP. It connects **traders** (sellers) with **customers** (buyers), supports an **admin** role for oversight, and uses **JWT** authentication with **MySQL** persistence. Payments are limited to **cash on delivery (COD)**.

---

## Requirements

| Requirement | Notes |
|-------------|--------|
| **Java 21** | Required by `pom.xml` (`java.version`). |
| **Maven** | Use the included wrapper: `./mvnw` (Linux/macOS) or `mvnw.cmd` (Windows). |
| **MySQL** | Server reachable with credentials you configure (local or remote). |

Optional: set `JAVA_HOME` to your JDK 21 installation if the wrapper does not find Java.

---

## Quick start

### 1. Clone and enter the project

```bash
git clone <repository-url>
cd gear-hup-backend
```

### 2. Create environment configuration

Copy the example file and fill in your database credentials:

```bash
cp .env.example .env
```

Edit **`.env`** (this file is gitignored):

| Variable | Description |
|----------|-------------|
| `DB_URL` | JDBC URL, e.g. `jdbc:mysql://localhost:3306/gearhub?createDatabaseIfNotExist=true&useSSL=false&allowPublicKeyRetrieval=true` |
| `DB_USERNAME` | MySQL user (often `root` in development). |
| `DB_PASSWORD` | MySQL password (leave empty only if your user has no password). |

The application loads `.env` via `spring.config.import=optional:file:.env[.properties]` in `application.properties`.

### 3. Configure JWT (recommended before any shared environment)

In `.env` or the shell, set a strong secret (minimum length enforced in code for production-style usage):

```properties
GEARHUB_JWT_SECRET=your-secure-random-string-at-least-32-characters-long
```

If omitted, a development default from `application.properties` is used (not suitable for production).

### 4. Run the application

```bash
./mvnw spring-boot:run
```

Windows:

```powershell
.\mvnw.cmd spring-boot:run
```

Default HTTP port: **8080** (override with `server.port` if needed).

### 5. Verify it is running

- **Root:** `GET http://localhost:8080/` → `{ "status": "UP", "message": "Application is running" }`
- **API health:** `GET http://localhost:8080/api/health`
- **Actuator:** `GET http://localhost:8080/actuator/health`

### 6. Run tests

```bash
./mvnw clean verify
```

Tests use **H2** in memory (`application-test.properties`); they do not require MySQL.

---

## Optional: bootstrap admin user

Admin accounts **cannot** be created through public registration. For a first-time admin, you can enable the bootstrap runner in `.env`:

```properties
GEARHUB_BOOTSTRAP_ADMIN=true
GEARHUB_BOOTSTRAP_ADMIN_USER=Administrator
GEARHUB_BOOTSTRAP_ADMIN_EMAIL=admin@example.com
GEARHUB_BOOTSTRAP_ADMIN_PASSWORD=choose-a-strong-password
```

Disable bootstrap again after the admin exists (`GEARHUB_BOOTSTRAP_ADMIN=false`).

---

## Authentication

- **Register** and **login** are public (`POST /api/auth/register`, `POST /api/auth/login`).
- All other business endpoints expect:

```http
Authorization: Bearer <access_token>
```

- **Logout** (`POST /api/auth/logout`) is documented as client-side token discard; JWTs are stateless (no server-side revocation list in this MVP).

Tokens are issued as **Bearer** JWTs; expiration is controlled by `GEARHUB_JWT_EXPIRATION_MS` (default in `application.properties`).

---

## Response shape

`GlobalResponseAdvice` wraps most controller responses as:

```json
{ "success": true, "data": <payload> }
```

**Not wrapped** (raw body as returned by the controller):

- `GET /`
- `GET /api/health`
- `/api/auth/**` (e.g. register, login, logout)
- `/actuator/**`
- `/error`

Lists returned from controllers are wrapped as `{ "success": true, "data": [ ... ] }`.

Errors from `ApiExceptionHandler` typically return JSON with `success: false`, `path`, and `message` (and sometimes `details` for validation).

---

## Roles

| Role | Purpose |
|------|---------|
| `CUSTOMER` | Cart, checkout, place orders, view own orders. |
| `TRADER` | Manage own product listings; update order status for orders that include their products. |
| `ADMIN` | User/product administration endpoints; broader order/product visibility. |

Public registration accepts **`TRADER`** and **`CUSTOMER`** only.

---

## API reference

Base URL (local): `http://localhost:8080`

Unless noted, send `Content-Type: application/json` for request bodies.

### Health and root

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/` | No | Simple running status. |
| `GET` | `/api/health` | No | JSON health hint + actuator link. |
| `GET` | `/actuator/health` | No | Spring Boot actuator health. |

---

### Authentication (`/api/auth`)

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `POST` | `/api/auth/register` | No | Register a trader or customer. |
| `POST` | `/api/auth/login` | No | Login with email and password; returns JWT. |
| `POST` | `/api/auth/logout` | Yes | Acknowledges logout (client discards JWT). |

**Register — request body**

```json
{
  "username": "john_doe",
  "email": "john@example.com",
  "password": "securepass",
  "role": "CUSTOMER"
}
```

- `role`: `TRADER` or `CUSTOMER` (not `ADMIN`).
- Validation: username length 2–120, valid email, password length 8–128.

**Register — response**

Returns a `UserResponse` object (not wrapped by `GlobalResponseAdvice`):

```json
{
  "id": 1,
  "username": "john_doe",
  "email": "john@example.com",
  "role": "CUSTOMER"
}
```

**Login — request body**

```json
{
  "email": "john@example.com",
  "password": "securepass"
}
```

**Login — response**

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresInMs": 86400000,
  "user": {
    "id": 1,
    "username": "john_doe",
    "email": "john@example.com",
    "role": "CUSTOMER"
  }
}
```

---

### Products (`/api/products`)

| Method | Path | Auth / roles | Description |
|--------|------|----------------|-------------|
| `GET` | `/api/products` | Public | List all products. Optional query: `search` (filters by name or category, case-insensitive). |
| `GET` | `/api/products/{id}` | Public | Product by id. |
| `GET` | `/api/products/mine` | Trader or Admin | List products for the current trader; admin sees all products. |
| `POST` | `/api/products` | Trader or Admin | Create listing. Admin must supply `traderId` in body. |
| `PUT` | `/api/products/{id}` | Authenticated | Update listing; authorization enforced in service (trader owns product, or admin). |
| `DELETE` | `/api/products/{id}` | Authenticated | Delete listing; same ownership rules as update. |

**Create / update — request body (`ProductDto`)**

```json
{
  "name": "Brake Pads",
  "description": "High quality",
  "price": 45.99,
  "stockQuantity": 100,
  "category": "Brakes",
  "traderId": null
}
```

- `traderId`: optional for traders (defaults to self); **required** when an **admin** creates a product for a trader.

---

### Cart (`/api/cart`) — **Customer only**

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/cart` | Customer | Current cart (lines + totals structure per `CartResponse`). |
| `POST` | `/api/cart/items` | Customer | Add or merge line (same `productId` increases quantity). |
| `PUT` | `/api/cart/items/{productId}` | Customer | Set quantity for a line (`{"quantity": 3}`). |
| `DELETE` | `/api/cart/items/{productId}` | Customer | Remove line. |

**Add item — request body**

```json
{
  "productId": 1,
  "quantity": 2
}
```

---

### Orders (`/api/orders`)

| Method | Path | Auth / roles | Description |
|--------|------|----------------|-------------|
| `POST` | `/api/orders` | Customer | Place order from explicit line items. |
| `POST` | `/api/orders/checkout` | Customer | Build order from persisted cart, then clear cart. |
| `GET` | `/api/orders` | Authenticated | List orders (customer: own; trader: orders involving their products; admin: all). |
| `GET` | `/api/orders/{id}` | Authenticated | Order detail if the caller is allowed to view it. |
| `PUT` | `/api/orders/{id}/status` | Trader or Admin | Update order status. |

**Place order — request body (`OrderDto`)**

```json
{
  "items": [
    { "productId": 1, "quantity": 2 }
  ],
  "paymentMethod": "COD"
}
```

- `paymentMethod`: only **COD** is accepted; other values return `400`.

**Update status — request body**

```json
{
  "status": "PROCESSING"
}
```

Allowed values (case-insensitive, stored uppercase): **`PENDING`**, **`PROCESSING`**, **`DELIVERED`**.

---

### Admin (`/api/admin`) — **Admin only**

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| `GET` | `/api/admin/users` | Admin | List all users (`UserResponse` list). |
| `GET` | `/api/admin/products` | Admin | List all products (same semantics as trader “mine” for admin). |
| `DELETE` | `/api/admin/products/{id}` | Admin | Delete any product by id. |

---

## Configuration reference (environment / `.env`)

| Key | Purpose |
|-----|---------|
| `DB_URL`, `DB_USERNAME`, `DB_PASSWORD` | MySQL datasource. |
| `DB_POOL_SIZE`, `DB_POOL_IDLE` | HikariCP pool tuning (optional). |
| `JPA_DDL_AUTO` | Hibernate schema mode (default `update`). |
| `SQL_INIT_MODE` | SQL script init mode (default `always`; see `schema.sql`). |
| `GEARHUB_JWT_SECRET` | JWT signing secret. |
| `GEARHUB_JWT_EXPIRATION_MS` | Access token lifetime. |
| `GEARHUB_CORS_ORIGINS` | Allowed origin patterns (default allows localhost). |
| `GEARHUB_BOOTSTRAP_*` | Optional one-time admin bootstrap (see above). |

---

## Technology stack

- **Java 21**, **Spring Boot 3.5.x**
- **Spring Web**, **Spring Data JPA**, **Hibernate**
- **Spring Security** (stateless JWT filter chain)
- **jjwt** (JSON Web Tokens)
- **MySQL** (runtime driver)
- **Jakarta Validation**, **Lombok**
- **Spring Boot Actuator** (health/info)
- **H2** (tests only)
- **Maven** (build and dependency management)

---

## Project layout (high level)

- `src/main/java/.../controller` — REST endpoints  
- `src/main/java/.../service` — Business logic  
- `src/main/java/.../repository` — Spring Data repositories  
- `src/main/java/.../model` — JPA entities  
- `src/main/java/.../security` — JWT filter, security configuration  
- `src/main/resources/application.properties` — Defaults and placeholders  
- `src/main/resources/schema.sql` — SQL DDL used when SQL init runs  
- `src/test` — Unit and integration tests  

---

## License and contribution

Add your organization’s **LICENSE** and contribution guidelines when you publish the repository.

For questions about behavior not covered here, inspect the controllers under `gearhub.website.gearhub.controller` and `SecurityConfig` for the authoritative rules.
