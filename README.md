# TeamForge

A B2B SaaS Internal Talent Intelligence Platform built with **Spring Boot**, **PostgreSQL**, and vanilla **JavaScript**.

## Live Demo

🔗 **[Try TeamForge Live](https://teamforge.onrender.com)**

### Demo Credentials

| Role | Email | Password |
|------|-------|----------|
| **HR Director** | `hr@teamforge.com` | `password123` |
| **Engineering Manager** | `manager1@teamforge.com` | `password123` |
| **Design Lead** | `manager2@teamforge.com` | `password123` |

> **Note:** The free tier spins down after 15 minutes of inactivity. The first request may take ~30 seconds to cold-start.

## Features

- 🧠 **Skill Intelligence** — Employees log their own skills and proficiency levels
- 🎯 **Smart Team Matching** — Match employees to projects by required skills and availability
- 📊 **Skill Gap Forecasting** — Identify trending skills your workforce lacks
- 📈 **Interactive Dashboards** — Chart.js-powered analytics for project progress, skill distribution, and gaps
- 🔔 **Real-time Notifications** — Alert HR/Managers when talent is matched to projects
- 🔒 **Role-Based Access Control** — HR, Manager, and Employee permission levels

## Tech Stack

- **Backend:** Java 21, Spring Boot 3.5, Spring Security (JWT), Spring Data JPA
- **Database:** PostgreSQL 16
- **Frontend:** Vanilla JavaScript, HTML5, CSS3, Chart.js
- **Deployment:** Docker, Render.com

## Local Development

```bash
# Clone the repository
git clone https://github.com/somvanshi7/TeamForge.git
cd TeamForge/teamforge

# Start with Docker Compose
docker compose up --build -d

# Open in browser
open http://localhost:8080
```

## Architecture

```
┌─────────────────────────────────────────┐
│           Landing Page (Public)         │
│    Hero • Features • Demo Credentials   │
└──────────────────┬──────────────────────┘
                   │ Login (JWT)
┌──────────────────▼──────────────────────┐
│          Dashboard (Authenticated)      │
│  Charts • Stats • Quick Search          │
├─────────┬─────────┬─────────┬───────────┤
│ Skills  │ Catalog │ Talent  │ Projects  │
│ Profile │ Browser │ Search  │ Manager   │
└─────────┴─────────┴─────────┴───────────┘
                   │
┌──────────────────▼──────────────────────┐
│        Spring Boot REST API             │
│  Auth • Users • Skills • Projects       │
│  Dashboard • Notifications              │
├─────────────────────────────────────────┤
│        PostgreSQL Database              │
└─────────────────────────────────────────┘
```
