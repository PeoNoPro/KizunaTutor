# KizunaTutor VPS Docker Deployment Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Package KizunaTutor (Node.js frontend + Spring Boot backend) into production Docker containers with environment-driven configuration and a step-by-step VPS + Cloudflare Tunnel deployment guide.

**Architecture:** Frontend (`server.js`, port 3000) serves static pages and reverse-proxies `/api/`, `/oauth2/`, `/login/oauth2/`, and `/memory/` to the Spring Boot backend (`8085`) over a Docker bridge network using `BACKEND_HOST`. Both services run via `docker-compose.prod.yml` against the existing cloud Supabase PostgreSQL database, with a persistent Docker volume for `/root/kizuna-data` and optional `cloudflared` tunnel for public HTTPS.

**Tech Stack:** Docker, Docker Compose, Node.js 20 Alpine, Java 17 (Eclipse Temurin), Maven Wrapper (`mvnw`), Spring Boot 3.3.4, Cloudflare Tunnel (`cloudflared`).

**Spec:** [2026-11-27-vps-docker-deploy-design.md](file:///c:/Users/Vuh26/Desktop/dev/KizunaTutor/docs/superpowers/specs/2026-11-27-vps-docker-deploy-design.md)

## Global Constraints

- Keep `localhost` dev workflow (`127.0.0.1:8085`, `node server.js`) working unchanged when no Docker env vars are set.
- Do not commit any real secrets, database passwords, JWT secrets, or API keys into `.env.example`, `docker-compose.prod.yml`, or `docs/DEPLOY_VPS.md`.
- Do not deploy separate `VOICEVOX` or `agent-memory` containers; rely on Web Speech API fallback and Spring Boot `AgentMemoryController`.
- Persist `/root/kizuna-data` (avatars and verification docs) on a named Docker volume.

## Review Focus

- Accessing the app over HTTPS on standard port 443 (`window.location.port === ''`) must resolve API calls to `/api` and `/memory/append`, never `http://localhost:8085`.
- Accessing the app locally via `file://` or non-3000 dev server (e.g. Live Server `5500` on `localhost`) must still fall back to `http://localhost:8085/api`.
- Google OAuth2 login redirects (`/oauth2/authorization/google`, `/login/oauth2/code/google`) must preserve `Host` / `X-Forwarded-*` headers or use `GOOGLE_REDIRECT_URI` from `.env` so OAuth2 does not redirect users to `localhost:3000` or `backend:8085`.
- Backend container must stay healthy and not crash if `VNPAY_TMN_CODE` or `R2_ENABLED=false` are unset.
- Uploaded avatars and verification documents must survive `docker compose restart`.

---

### Task 1: Backend Multi-Stage Dockerfile

**Files:**
- Create: `backend/Dockerfile`
- Create: `.dockerignore`

**Interfaces:**
- Consumes: Root `pom.xml`, `mvnw`, `.mvn/`, `backend/pom.xml`, `backend/src/`
- Produces: Docker image exposing port `8085`, running Spring Boot JAR with `SPRING_PROFILES_ACTIVE` and env overrides, storing uploads under `/root/kizuna-data`.

- [ ] **Step 1: Create `.dockerignore` at repo root**

Exclude `.git`, `frontend/node_modules`, `backend/target`, `services/`, `scratch/`, `docs/`, and `.env` so Docker build contexts stay small and never bake local secrets.

- [ ] **Step 2: Create `backend/Dockerfile`**

Use multi-stage build:
- Stage 1 (`eclipse-temurin:17-jdk-alpine`): copy `mvnw`, `.mvn`, `pom.xml`, `backend/pom.xml`, `backend/src`, run `./mvnw -pl backend -am package -DskipTests` (with `chmod +x mvnw` and `dos2unix`/line-ending safety via `sed -i 's/\r$//' mvnw`).
- Stage 2 (`eclipse-temurin:17-jre-alpine`): copy `/app/backend/target/*.jar` to `/app/app.jar`, create `/root/kizuna-data/avatars` and `/root/kizuna-data/verification-docs`, `EXPOSE 8085`, `ENTRYPOINT ["sh", "-c", "java ${JAVA_OPTS:--Xmx512m} -jar /app/app.jar"]`.

- [ ] **Step 3: Verify Maven build succeeds**

Run: `.\mvnw.cmd -pl backend -am package -DskipTests` (or `docker build -f backend/Dockerfile -t kizuna-backend:test .` if Docker is available).
Expected: `BUILD SUCCESS` and JAR produced in `backend/target/`.

- [ ] **Step 4: Commit**

```bash
git add .dockerignore backend/Dockerfile
git commit -m "chore(deploy): add backend multi-stage Dockerfile and .dockerignore"
```

---

### Task 2: Frontend Proxy Configuration, API URL Resolution & Dockerfile

**Files:**
- Modify: `frontend/server.js:5-8,202-247`
- Modify: `frontend/js/api/apiClient.js:6-13`
- Modify: `frontend/js/api/agentMemory.js:7-10`
- Modify: `frontend/js/config/api.config.js:6-8`
- Create: `frontend/js/config/urlResolver.js` (or shared helper / inline consistent check)
- Create: `frontend/tests/url-resolution.test.mjs`
- Create: `frontend/Dockerfile`

**Interfaces:**
- Consumes: `process.env.BACKEND_HOST` (default `'127.0.0.1'`), `process.env.BACKEND_PORT` (default `8085`).
- Produces: `resolveApiBaseUrl(loc)` / `resolveMemoryUrl(loc)` behavior across `apiClient.js`, `agentMemory.js`, `api.config.js`, plus `frontend/Dockerfile` exposing port `3000`.

- [ ] **Step 1: Write failing unit test `frontend/tests/url-resolution.test.mjs`**

Test `resolveApiBaseUrl(locationLike)` and `resolveMemoryUrl(locationLike)` for:
1. `{ protocol: 'https:', hostname: 'kizuna.example.com', port: '' }` -> `'/api'`, `'/memory'`
2. `{ protocol: 'http:', hostname: 'localhost', port: '3000' }` -> `'/api'`, `'/memory'`
3. `{ protocol: 'http:', hostname: 'localhost', port: '5500' }` -> `'http://localhost:8085/api'`, `'http://localhost:8081/memory'`
4. `{ protocol: 'file:', hostname: '', port: '' }` -> `'http://localhost:8085/api'`, `'http://localhost:8081/memory'`

- [ ] **Step 2: Run test to verify it fails**

Run: `node --test frontend/tests/url-resolution.test.mjs`
Expected: FAIL (`urlResolver.js` / helper not found).

- [ ] **Step 3: Implement shared URL resolution & update `apiClient.js`, `agentMemory.js`, `api.config.js`, and `server.js`**

- Add `BACKEND_HOST = process.env.BACKEND_HOST || '127.0.0.1'` in `frontend/server.js` and use `BACKEND_HOST` in both the `/memory/` proxy and `/api/`, `/oauth2/`, `/login/oauth2/` proxy (removing the hardcoded `'127.0.0.1'` hostname; also forward `X-Forwarded-Proto` / `X-Forwarded-Host` or keep original `Host` appropriately).
- Update `apiClient.js`, `agentMemory.js`, `api.config.js` to use `/api` and `/memory` whenever `window.location.protocol.startsWith('http') && (window.location.port === '3000' || window.location.port === '' || (window.location.hostname !== 'localhost' && window.location.hostname !== '127.0.0.1'))`.

- [ ] **Step 4: Run unit test to verify it passes**

Run: `node --test frontend/tests/url-resolution.test.mjs`
Expected: PASS (all 4 cases).

- [ ] **Step 5: Create `frontend/Dockerfile`**

Use `node:20-alpine`, `WORKDIR /app`, copy `frontend/package*.json`, run `npm ci --omit=dev`, copy `frontend/`, `EXPOSE 3000`, `CMD ["node", "server.js"]`.

- [ ] **Step 6: Commit**

```bash
git add frontend/server.js frontend/js/api/apiClient.js frontend/js/api/agentMemory.js frontend/js/config/api.config.js frontend/tests/url-resolution.test.mjs frontend/Dockerfile
git commit -m "feat(deploy): support BACKEND_HOST proxy, relative prod URLs, and frontend Dockerfile"
```

---

### Task 3: Production Docker Compose & Environment Template

**Files:**
- Create: `docker-compose.prod.yml`
- Create: `.env.example`

**Interfaces:**
- Consumes: `backend/Dockerfile`, `frontend/Dockerfile`, `.env` variables (`SPRING_DATASOURCE_*`, `APP_JWT_SECRET`, `MAIL_*`, `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI`, `VNPAY_*`, `CLOUDFLARE_TUNNEL_TOKEN`).
- Produces: `docker-compose.prod.yml` with services `backend`, `frontend`, and optional `cloudflared` (under `profiles: ["tunnel"]`).

- [ ] **Step 1: Create `.env.example`**

Document all required and optional environment variables with safe placeholder values (no real passwords/keys):
- `SPRING_DATASOURCE_URL`, `SPRING_DATASOURCE_USERNAME`, `SPRING_DATASOURCE_PASSWORD`
- `APP_JWT_SECRET`
- `MAIL_HOST`, `MAIL_PORT`, `MAIL_USERNAME`, `MAIL_PASSWORD`, `MAIL_SENDER_EMAIL`
- `GOOGLE_CLIENT_ID`, `GOOGLE_CLIENT_SECRET`, `GOOGLE_REDIRECT_URI` (e.g. `https://your-domain.com/login/oauth2/code/google`)
- `VNPAY_TMN_CODE`, `VNPAY_HASH_SECRET`, `VNPAY_RETURN_URL` (e.g. `https://your-domain.com/tutor/membership/result`)
- `R2_ENABLED`, `R2_ENDPOINT`, `R2_BUCKET`, `R2_ACCESS_KEY_ID`, `R2_SECRET_ACCESS_KEY`
- `JAVA_OPTS=-Xmx512m -Xms256m`
- `CLOUDFLARE_TUNNEL_TOKEN=`

- [ ] **Step 2: Create `docker-compose.prod.yml`**

Define:
- `backend`: builds from `.` with `dockerfile: backend/Dockerfile`, `env_file: .env`, `restart: unless-stopped`, mounts `kizuna_data:/root/kizuna-data`, sets `VERIFICATION_STORAGE_DIR=/root/kizuna-data/verification-docs` and `AVATAR_STORAGE_DIR=/root/kizuna-data/avatars`.
- `frontend`: builds from `./frontend` (or `.` with `dockerfile: frontend/Dockerfile`), `ports: ["3000:3000"]`, `environment: BACKEND_HOST=backend, BACKEND_PORT=8085`, `depends_on: [backend]`, `restart: unless-stopped`.
- `cloudflared`: image `cloudflare/cloudflared:latest`, `profiles: ["tunnel"]`, `command: tunnel --no-autoupdate run --token ${CLOUDFLARE_TUNNEL_TOKEN}`, `depends_on: [frontend]`, `restart: unless-stopped`.
- `volumes: kizuna_data:`.

- [ ] **Step 3: Validate Compose syntax (if Docker is installed) or inspect YAML**

Run: `docker compose -f docker-compose.prod.yml config --quiet` (if Docker CLI present; otherwise verify YAML syntax via Node/Python).
Expected: Exit code 0 with no errors.

- [ ] **Step 4: Commit**

```bash
git add docker-compose.prod.yml .env.example
git commit -m "chore(deploy): add production docker-compose and .env.example"
```

---

### Task 4: Step-by-Step VPS & Cloudflare Tunnel Deployment Guide

**Files:**
- Create: `docs/DEPLOY_VPS.md`

**Interfaces:**
- Consumes: `docker-compose.prod.yml`, `.env.example`
- Produces: Clear Vietnamese runbook for thesis defense deployment.

- [ ] **Step 1: Write `docs/DEPLOY_VPS.md`**

Include concrete commands for:
1. Chuẩn bị VPS (Ubuntu 22.04/24.04, 1–2GB RAM) & cài Docker + Docker Compose plugin.
2. Clone repo, tạo file `.env` từ `.env.example` và điền thông tin Supabase/Brevo/JWT/Google OAuth2.
3. Chạy `docker compose -f docker-compose.prod.yml up -d --build` và kiểm tra log (`docker compose -f docker-compose.prod.yml logs -f`).
4. Mở HTTPS công khai bằng Cloudflare Tunnel (cách dùng Tunnel có token + domain miễn phí/rẻ `.id.vn` hoặc `trycloudflare.com` nhanh).
5. Cập nhật `Authorized redirect URIs` trên Google Cloud Console (`https://<domain>/login/oauth2/code/google`) và `VNPAY_RETURN_URL`.

- [ ] **Step 2: Commit**

```bash
git add docs/DEPLOY_VPS.md
git commit -m "docs(deploy): add Vietnamese VPS and Cloudflare Tunnel deployment guide"
```
