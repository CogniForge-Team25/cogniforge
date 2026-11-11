# CogniForge

Business Assurance and Security Readiness Platform: a web app that helps small and medium
organizations prepare for ISO/IEC 27001 audits.

## Stack
- Frontend: React, TypeScript, Vite, Tailwind CSS
- Backend: Java 21, Spring Boot, Spring Security, Flyway
- Database: PostgreSQL 16
- Infrastructure: Docker Compose, GitHub Actions

## Run it locally
1. Copy `infra/.env.example` to `infra/.env` and set real values.
2. Start the database: `cd infra && docker compose --env-file .env up -d db`
3. Backend: `cd backend && ./mvnw spring-boot:run`
4. Frontend: `cd frontend && npm install && npm run dev`

## Working rules
- Never push to `main`. Create a branch, then open a pull request.
- Commit messages follow Conventional Commits, for example `feat(auth): add login endpoint`.
- Every pull request needs one approving review.
