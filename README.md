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

## Licensing integration

Subscription catalog and payment endpoints are compatibility adapters to
`licensing-service` through Eureka. Set `licensing.service.url` for a fixed URL
when discovery is unavailable. The existing Feign interceptor forwards the caller's
JWT; licensing derives payment/trial ownership from that JWT.

Licensing now owns plan initialization, trial grants, M-Pesa calls, and payment
verification. Configure M-Pesa credentials on licensing. Deploy licensing before
this auth version. Both services currently use the existing `pos_core` tables;
auth retains the historical migrations and read-only entitlement queries for tokens
and child-shop access. Subscription operations return 503 when licensing is unavailable.

The legacy paid-subscription update route cannot activate a paid plan: use the
payment initiation and reconciliation endpoints. For a trial, send `freeTrial=true`
or use `/institutions/subscriptions/{subscriptionId}/free-trial`.
