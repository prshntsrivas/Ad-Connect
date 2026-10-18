# Ad-Connect

Marketplace that matches **brands** with **influencers**. Rewritten as a Java 21 / Spring Boot 3 microservices backend and a React (Vite) SPA.

## Architecture

```
React :5173  →  Spring Cloud Gateway :8080  →  Eureka :8761
                                  │
        ┌────────────┬────────────┼────────────┬────────────┐
        ▼            ▼            ▼            ▼            ▼
   auth :8081   campaign     search :8083   chat :8084   payment :8085
   gRPC :9091    :8082                                    notification :8086
        │            │            │            │            │
      MySQL        MySQL+Redis    ES         Mongo+Redis   MySQL / Mongo
                    Kafka ◄──────────────────────────────── Kafka + Mailpit
```

| Service | Port | Store | Notes |
|---|---|---|---|
| discovery-server | 8761 | — | Netflix Eureka |
| api-gateway | 8080 | Redis | JWT filter, rate limit, CORS, `lb://` routing |
| auth-service | 8081 / gRPC 9091 | MySQL `adconnect_auth` | Register/login, rotating refresh tokens, manager verification, optional Google OAuth2 |
| campaign-service | 8082 | MySQL + Redis cache | Profiles, campaigns, deal state machine, reviews, dashboards; **gRPC client** to auth-service |
| search-service | 8083 | Elasticsearch | Kafka-driven CQRS index, faceted fuzzy search, autocomplete |
| chat-service | 8084 | MongoDB + Redis | Idempotent messages (`clientMessageId` + unique index + Redis), STOMP WebSocket, Redis fan-out |
| payment-service | 8085 | MySQL | Stripe test **or** mock, idempotent PaymentIntents, webhook, OpenFeign → campaign-service |
| notification-service | 8086 | MongoDB | Kafka `@RetryableTopic` + DLT, Thymeleaf email via Mailpit, in-app inbox |

## Run locally

**Prereqs:** Java 21, Maven 3.9+, Node 20+, Docker Desktop.

```powershell
# 1. Infrastructure
docker compose up -d

# 2. Backend (builds then starts jars under .run/)
./scripts/start-backend.ps1 -Build

# 3. Frontend
cd frontend; npm install; npm run dev
```

Open http://localhost:5173

Seed accounts (password in `.env.example`):

| Role | Email | Password |
|---|---|---|
| Admin | admin@adconnect.local | Admin@123 |
| Manager | manager@adconnect.local | Manager@123 |

New brand/influencer accounts start **PENDING**. Log in as the manager → **Manager** → **Verify**.

### Stripe

Leave `STRIPE_SECRET_KEY` empty to use **mock** payments (`POST /api/payments/mock/confirm`). For Stripe test mode, copy `.env.example` to `.env` and set `sk_test_…` / `pk_test_…`. Webhook: `http://localhost:8080/api/payments/webhook/stripe`.

### Google OAuth2

Set `GOOGLE_CLIENT_ID` / `GOOGLE_CLIENT_SECRET` and start auth-service with `SPRING_PROFILES_ACTIVE=google`. Redirect URI: `http://localhost:8080/login/oauth2/code/google`.

## Tests

```powershell
cd e2e
npm install
npx playwright install chromium
npm test
```

Playwright walks register → manager verify → profiles → campaign → apply → accept → mock pay → search → chat → inbox.

## Useful UIs

- Eureka: http://localhost:8761
- Kafka UI: http://localhost:8090
- Mailpit: http://localhost:8025
- Swagger (each service): `http://localhost:808x/swagger-ui.html`

## Docker / Kubernetes (optional)

```powershell
docker compose --profile apps up -d --build
kubectl apply -f k8s/
```

## Deal lifecycle

`APPLIED` / `OFFERED` → `ACCEPTED` → `FUNDED` (escrow) → `DELIVERED` → `COMPLETED` (payout released). `REJECTED` / `CANCELLED` are terminal; a brand can send a funded deal back to `FUNDED` via request-revision.
