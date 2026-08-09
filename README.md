# TeamForge

An internal workforce intelligence platform for HR and engineering/project managers. TeamForge replaces spreadsheets and tribal knowledge with a live, searchable picture of who can do what, who's free right now, and what skills the organization should be hiring for next.

## Why

Most internal staffing decisions run on memory and outdated spreadsheets: "I think Priya knows Kubernetes?" TeamForge makes that answerable in one click — and, just as importantly, surfaces the skills a company is systematically short on *before* a project stalls waiting for them.

## Core features

- **Skill profiles** — every employee logs their own skills and proficiency level.
- **Smart team matching** — attach a required tech stack to a project and TeamForge finds every employee who qualifies, including people wrapping up other active work within the next 7 days.
- **Skill gap forecasting** — the moment a project needs a skill nobody has, it's flagged as a market/skill gap, driving the next hiring round.
- **Dashboard analytics** — active project progress vs. deadline, skill usage across projects, and skill supply vs. demand, all as interactive charts.
- **Role-based access** — Employees manage their own profile; Managers/HR create projects, assemble teams, and manage the skill catalog.

## Tech stack

- **Backend:** Java 21, Spring Boot 3, Spring Security (JWT), Spring Data JPA, PostgreSQL
- **Frontend:** Vanilla HTML/CSS/JS, Chart.js — served as static resources from the Spring Boot app (no separate frontend build/deploy)
- **Infra:** Docker & Docker Compose

## Running it locally

Requires Docker.

```bash
docker compose up --build -d
```

Then open **http://localhost:8080**.

The database is seeded automatically on first run with demo accounts (all use password `password123`):

| Role    | Email                     |
|---------|---------------------------|
| HR/Admin | hr@teamforge.com          |
| Manager  | manager1@teamforge.com    |
| Manager  | manager2@teamforge.com    |

To reset the database and reseed from scratch:

```bash
docker compose down -v
docker compose up --build -d
```

## Configuration

Local defaults live in `application.yml` and just work out of the box. For any real deployment, override these via environment variables instead of relying on the defaults:

- `DB_HOST`, `DB_NAME`, `DB_USER`, `DB_PASSWORD`
- `JWT_SECRET`, `JWT_EXPIRATION_MS`

## Project structure

```
src/main/java/com/teamforge/
  auth/          JWT auth, login/register
  users/         User entity, profile
  skills/        Skill catalog, user-skill profiles, talent search
  projects/      Projects, required skills, team membership
  dashboard/     Aggregated metrics for the dashboard + detail views
  security/      JWT filter, security config
src/main/resources/static/   Frontend (index.html, app.js, style.css)
```
