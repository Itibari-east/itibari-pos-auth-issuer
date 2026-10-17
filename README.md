# itibari-pos-auth-issuer

POS authentication and user-management service. Issues JWTs and owns user CRUD.

Eureka name: `pos-auth-issuer`  
Default port: `8082`

## Phase 1 APIs

| Method | Path | Auth | Description |
|--------|------|------|-------------|
| POST | `/login` | public | Password login → JWT |
| POST | `/users` | public | Create user |
| GET | `/users` | JWT | List users |
| GET | `/users/{publicId}` | JWT | Get user |
| PATCH | `/users/{publicId}` | JWT | Update user |
| DELETE | `/users/{publicId}` | JWT | Soft-delete user |
| GET | `/users/me` | JWT | Current user from token |

Gateway routes (StripPrefix=3):

- `/api/v1/pos/login/**` → this service `/login/**`
- `/api/v1/pos/users/**` → this service `/users/**`

## Run

```bash
export DATABASE_HOST=localhost DATABASE_PORT=5432 DATABASE_NAME=itibari_db
export DATABASE_USER=postgres DATABASE_PASSWORD=...
export EUREKA_HOST=localhost EUREKA_PORT=8761
./mvnw spring-boot:run
```
