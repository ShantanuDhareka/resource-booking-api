# Resource Booking API

Secure REST API for booking rooms, vehicles, and equipment. Users authenticate with JWT, then create and manage reservations. Administrators have full control over resources and all bookings.

## Tech stack

- Java 17+
- Spring Boot 4
- Spring Security with JWT (HMAC SHA-256)
- Role-based access control (`ADMIN`, `USER`)
- Spring Data JPA / Hibernate
- PostgreSQL
- Bean Validation
- springdoc OpenAPI / Swagger UI

## Requirements implemented

| Area | Behavior |
| --- | --- |
| Auth | `POST /auth/login` issues a JWT |
| RBAC | `ADMIN` has full CRUD on resources and reservations |
| Users | `USER` can read resources, create reservations, and see only their own |
| Identity | Reservation owner is taken from the JWT `userId` claim, never from the request body |
| Status | `PENDING`, `CONFIRMED`, `CANCELLED` |
| Price | Stored as `DECIMAL(12,2)` (`BigDecimal`) |
| Listing | Filter by `status`, `minPrice`, `maxPrice`; paginate with `page` and `size`; optional `sort` |
| Errors | Consistent JSON error payloads and validation messages |
| Docs | Swagger UI and a Postman collection |

## Quick start

### 1. Start PostgreSQL

```bash
docker compose up -d
```

Or create a database named `resource_booking` on an existing PostgreSQL instance.

### 2. Configure the application

Copy `.env.example` values into your environment, or export them in the shell:

```bash
set DATABASE_URL=jdbc:postgresql://localhost:5432/resource_booking
set DATABASE_USERNAME=postgres
set DATABASE_PASSWORD=postgres
set JWT_SECRET=change-this-to-a-32-byte-minimum-secret-key
```

On macOS/Linux use `export` instead of `set`.

`JWT_SECRET` must be at least 32 characters (256 bits) for HS256.

### 3. Run the API

```bash
./mvnw spring-boot:run
```

Windows:

```bat
mvnw.cmd spring-boot:run
```

The API listens on `http://localhost:8080`.

### 4. Open API docs

- Swagger UI: http://localhost:8080/swagger-ui.html
- OpenAPI JSON: http://localhost:8080/v3/api-docs
- Postman: import `docs/Resource-Booking-API.postman_collection.json`

## Environment variables

| Variable | Default | Description |
| --- | --- | --- |
| `SERVER_PORT` | `8080` | HTTP port |
| `DATABASE_URL` | `jdbc:postgresql://localhost:5432/resource_booking` | JDBC URL |
| `DATABASE_USERNAME` | `postgres` | Database user |
| `DATABASE_PASSWORD` | `postgres` | Database password |
| `JPA_DDL_AUTO` | `update` | Hibernate schema mode |
| `JPA_SHOW_SQL` | `false` | Log SQL |
| `JWT_SECRET` | development placeholder | HMAC signing key (min 32 bytes) |
| `JWT_EXPIRATION_MS` | `86400000` | Access token lifetime (24h) |

Hibernate `ddl-auto=update` creates tables on startup. Use a migration tool in production.

### MySQL instead of PostgreSQL

1. Add the MySQL driver to `pom.xml`.
2. Set `DATABASE_URL=jdbc:mysql://localhost:3306/resource_booking`.
3. Create the `resource_booking` schema.

## Seed users

Created automatically on first startup:

| Username | Password | Role |
| --- | --- | --- |
| `admin` | `Admin@123` | `ADMIN` |
| `user` | `User@123` | `USER` |

Sample resources (conference room, van, projector) are also inserted when the `resources` table is empty.

## Authentication

```http
POST /auth/login
Content-Type: application/json

{
  "username": "user",
  "password": "User@123"
}
```

Response:

```json
{
  "accessToken": "<jwt>",
  "tokenType": "Bearer",
  "expiresIn": 86400,
  "userId": 2,
  "username": "user",
  "role": "USER"
}
```

Send the token on later requests:

```http
Authorization: Bearer <jwt>
```

## Endpoints

### Resources

| Method | Path | USER | ADMIN |
| --- | --- | --- | --- |
| GET | `/api/resources` | yes | yes |
| GET | `/api/resources/{id}` | yes | yes |
| POST | `/api/resources` | no | yes |
| PUT | `/api/resources/{id}` | no | yes |
| DELETE | `/api/resources/{id}` | no | yes |

### Reservations

| Method | Path | USER | ADMIN |
| --- | --- | --- | --- |
| GET | `/api/reservations` | own rows only | all rows |
| GET | `/api/reservations/{id}` | own row only | any row |
| POST | `/api/reservations` | yes (owner = JWT) | yes (owner = JWT) |
| PUT | `/api/reservations/{id}` | reschedule or cancel own | full update |
| DELETE | `/api/reservations/{id}` | no | yes |

Query parameters for `GET /api/reservations`:

- `status` — `PENDING` \| `CONFIRMED` \| `CANCELLED`
- `minPrice` / `maxPrice` — decimal filters
- `page` — zero-based page index (default `0`)
- `size` — page size, 1–100 (default `10`)
- `sort` — `field,dir` such as `price,desc`. Allowed fields: `id`, `price`, `status`, `startTime`, `endTime`, `createdAt`, `updatedAt`

Example:

```http
GET /api/reservations?status=PENDING&minPrice=10&maxPrice=200&page=0&size=10&sort=price,desc
Authorization: Bearer <token>
```

Create a reservation (do **not** send `userId`):

```json
{
  "resourceId": 1,
  "startTime": "2026-10-01T10:00:00Z",
  "endTime": "2026-10-01T12:00:00Z",
  "notes": "Project kickoff"
}
```

New reservations start as `PENDING`. Price is `hourlyRate * duration` rounded to two decimals. Overlapping `PENDING` or `CONFIRMED` bookings on the same resource are rejected.

Users may set `status` to `CANCELLED` on their own reservation. Only admins can confirm bookings or override price.

## Error format

```json
{
  "timestamp": "2026-09-27T00:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/auth/login",
  "details": ["username: Username is required"]
}
```

Typical status codes: `400` validation, `401` missing/invalid JWT or bad login, `403` RBAC denial, `404` missing entity, `409` booking conflict.

## Tests

Tests use an in-memory H2 database:

```bash
./mvnw test
```

Windows:

```bat
mvnw.cmd test
```

## Project layout

```
src/main/java/com/example/booking
  config/       OpenAPI + data seeder
  domain/       JPA entities and enums
  dto/          request/response records
  exception/    API errors
  repository/   Spring Data JPA
  security/     JWT, RBAC, current user
  service/      business rules
  web/          REST controllers
```
# resource-booking-api
