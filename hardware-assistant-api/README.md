# HardwareAI — AI-Powered Hardware Business Assistant

> AI-generated business intelligence for hardware merchants in Lagos, Nigeria.

[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.0-brightgreen?style=flat-square&logo=springboot)](https://spring.io/projects/spring-boot)
[![React](https://img.shields.io/badge/React-18-blue?style=flat-square&logo=react)](https://reactjs.org)
[![PostgreSQL](https://img.shields.io/badge/PostgreSQL-17-blue?style=flat-square&logo=postgresql)](https://postgresql.org)
[![Groq AI](https://img.shields.io/badge/Groq-AI-orange?style=flat-square)](https://groq.com)
[![Deployed on Railway](https://img.shields.io/badge/Backend-Railway-purple?style=flat-square&logo=railway)](https://railway.app)
[![Deployed on Vercel](https://img.shields.io/badge/Frontend-Vercel-black?style=flat-square&logo=vercel)](https://vercel.com)

---

##  Overview

HardwareAI is a full-stack SaaS platform that delivers AI-powered business intelligence to hardware merchants. It analyses a merchant's business profile — products, location, customer type, price range — and generates actionable insights, revenue forecasts, strengths, weaknesses, and personalised recommendations using the Groq AI language model.

Built for the Lagos hardware trade, HardwareAI bridges the gap between enterprise-grade business intelligence tools and the merchants who need them most but have never had access.

---

## Live URLs

| Environment | URL |
|-------------|-----|
| Frontend | https://hardware-assistant-frontend.vercel.app |
| Backend API | https://hardware-assistant-backend-production.up.railway.app/api |
| API Health | https://hardware-assistant-backend-production.up.railway.app/actuator/health |

---

## Features

### Core
-  **AI Business Analysis** — Groq-powered analysis with strengths, weaknesses, recommendations, and revenue forecasting
-  **JWT Authentication** — Stateless, secure, role-based
-  **Email Verification** — Resend integration with verified domain (hardwareai.org)
-  **SMS Delivery** — Top recommendations sent via Africa's Talking
-  **Subscription & Quota Management** — FREE / BASIC / PRO tiers with monthly limits

### Security
- ️ **Prompt Injection Protection** — Sanitisation layer before every AI call
-  **Role-Based Access Control** — ADMIN and MERCHANT roles
-  **Rate Limiting** — 20 AI analyses per hour per user
-  **Input Validation** — Jakarta Validation on all DTOs with clean error responses

### Admin
-  **AI Usage Dashboard** — Platform-wide cost, token usage, monthly trends
-  **Merchant Management** — Search, view, and manage all merchants
-  **Per-User Cost Tracking** — Cost breakdown per merchant

### Frontend
-  **Dark Mode** — Full dark/light toggle
-  **Framer Motion Animations** — Smooth transitions and state feedback
-  **Loading Skeletons** — No blank screens during data fetch
-  **Toast Notifications** — Non-blocking success/error/info feedback
-  **Fully Responsive** — Mobile-first, works on all screen sizes

---

## ️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Backend | Spring Boot 4, Java 21, Spring Security, Spring Data JPA |
| Database | PostgreSQL 17, Flyway migrations |
| AI | Groq API (`llama-3.3-70b-versatile`) |
| Email | Resend (domain: hardwareai.org) |
| SMS | Africa's Talking |
| Payments | Paystack |
| Frontend | React 18, Vite, Tailwind CSS, Framer Motion |
| Charts | Recharts |
| Backend Deploy | Railway |
| Frontend Deploy | Vercel |
| CI/CD | GitHub Actions |

---

## ️Architecture

```
┌─────────────────────────────────────────────┐
│                  Frontend                    │
│  React 18 + Vite + Tailwind + Framer Motion │
│  Pages / Components / API Clients / Context  │
└─────────────────┬───────────────────────────┘
                  │ HTTPS / JWT
┌─────────────────▼───────────────────────────┐
│               Spring Boot API                │
│  Controller → Service → Repository → Model   │
│  JWT Filter → Rate Limit → Security Chain    │
└──────┬──────────┬───────────────┬────────────┘
       │          │               │
┌──────▼──┐  ┌────▼────┐  ┌──────▼──────┐
│PostgreSQL│  │ Groq AI │  │  Resend /   │
│    17    │  │  API    │  │ Africa's    │
│ Flyway   │  │         │  │  Talking    │
└──────────┘  └─────────┘  └─────────────┘
```

### Backend Layers
- **Controller** — HTTP handling, input validation trigger, auth checks
- **Service** — Business logic, transaction boundaries, orchestration
- **Repository** — Spring Data JPA with custom JPQL queries
- **DTO** — Request/response objects, never expose entities directly
- **Security** — JWT filter chain, rate limiting, CORS, RBAC

---

## ️Database Schema

| Entity | Purpose |
|--------|---------|
| `users` | Auth identity — email, hashed password, role, verification status |
| `merchant_profiles` | Business context for AI — products, location, customer type |
| `analyses` | Persisted AI results — JSON blob + metadata |
| `ai_usage` | Token counts and cost per AI call |
| `sms_logs` | Record of every SMS sent |
| `subscriptions` | Tier, quota, renewal date |

---

## ️ Local Setup

### Prerequisites
- Java 21+
- Maven 3.9+
- PostgreSQL 17
- Node 20+

### Backend

```bash
# Clone the repo
git clone https://github.com/yourusername/hardware-assistant.git
cd hardware-assistant

# Configure environment
cp src/main/resources/application-local.properties.example \
   src/main/resources/application-local.properties

# Edit application-local.properties with your values (see Environment Variables below)

# Run
mvn spring-boot:run -Dspring-boot.run.profiles=local
# API available at http://localhost:8080/api
```

### Frontend

```bash
cd frontend

# Configure environment
cp .env.example .env.local
# Set VITE_API_URL=http://localhost:8080/api

# Install and run
npm install
npm run dev
# App available at http://localhost:5173
```

---

##  Environment Variables

### Backend (Railway Variables)

| Variable | Description |
|----------|-------------|
| `DATABASE_URL` | PostgreSQL JDBC connection string |
| `JWT_SECRET` | JWT signing secret (min 256-bit) |
| `GROQ_API_KEY` | Groq AI API key |
| `APP_RESEND_API-KEY` | Resend email API key |
| `APP_RESEND_FROM-EMAIL` | Verified sender email (`noreply@hardwareai.org`) |
| `APP_RESEND_FRONTEND-URL` | Frontend base URL for email links |
| `AT_USERNAME` | Africa's Talking SMS username |
| `AT_API_KEY` | Africa's Talking SMS API key |
| `PAYSTACK_SECRET_KEY` | Paystack secret key |
| `PAYSTACK_WEBHOOK_SECRET` | Paystack webhook verification secret |

### Frontend (Vercel / `.env.local`)

| Variable | Description |
|----------|-------------|
| `VITE_API_URL` | Backend API base URL |

---

## Security

HardwareAI implements defence-in-depth across every layer:

| Layer | Implementation |
|-------|----------------|
| Authentication | JWT Bearer tokens, 24-hour expiry |
| Authorisation | Spring Security RBAC, route + method level |
| Input validation | Jakarta Validation on all DTOs |
| SQL injection | JPA parameterised queries — no raw SQL |
| Prompt injection | PromptSanitiser strips injection patterns before AI calls |
| Rate limiting | 20 analyses/hour per user via ConcurrentHashMap filter |
| Password storage | BCrypt hashing — plaintext never stored |
| Secrets | Railway Variables + GitHub Secrets — never in code |
| AI output | JSON schema validation before storage |
| Monitoring | Suspicious inputs logged at WARN with user identity |

---

## AI Pipeline

```
Merchant Profile
      ↓
PromptSanitiser (injection check + strip)
      ↓
PromptBuilderService (construct structured prompt)
      ↓
Groq API (llama-3.3-70b-versatile)
      ↓
ResponseValidatorService (JSON schema check)
      ↓
CostCalculatorService (token cost calculation)
      ↓
AiUsageRepository (persist usage record)
      ↓
AnalysisRepository (persist result)
      ↓
Return to frontend
```

---

## ️ Roadmap

- [ ] **Phase 2** — Multi-period analysis comparison, peer benchmarking
- [ ] **Phase 3** — Seasonal demand forecasting, inventory risk alerts
- [ ] **Phase 4** — WhatsApp Business API delivery
- [ ] **Phase 5** — Embedded finance indicators, Paystack Terminal integration
- [ ] **Phase 6** — Redis caching, WebSocket real-time updates, microservices extraction

---

## Contributing

1. Fork the repository
2. Create a feature branch: `git checkout -b feature/your-feature`
3. Follow architecture conventions:
    - Controllers delegate to services — no business logic in controllers
    - Services return DTOs — never entities
    - All new endpoints must be authorised in `SecurityConfig`
    - All schema changes require a Flyway migration script
    - All AI prompt changes must be tested against injection patterns
4. Commit: `git commit -m "feat: your feature description"`
5. Push and open a Pull Request against `main`
