# ToveloKno

智能学习资源管理系统，包含 Spring Boot 后端和 Vue/Vite 前端。

## Project Structure

- `backend/`: Spring Boot API, JPA entities, services, controllers, and database schema.
- `frontend/`: Vue 3 client built with Vite.
- `docs/`: Project notes, diagrams, and documentation assets.
- `scripts/`: Local development helper scripts.

## Local Environment

Required tools:

- JDK 21
- Maven 3.9+
- Node.js LTS or newer
- MySQL 8.4+

Default local database settings:

- Database: `tovelokno_db`
- Username: `root`
- Password: `000000`
- Port: `3306`

The backend can be configured with environment variables:

- `TOVELOKNO_DB_URL`
- `TOVELOKNO_DB_USERNAME`
- `TOVELOKNO_DB_PASSWORD`
- `TOVELOKNO_TOKEN_SECRET`
- `TOVELOKNO_TOKEN_EXPIRATION_HOURS`
- `TOVELOKNO_CORS_ALLOWED_ORIGINS`

The frontend API endpoint can be configured with `frontend/.env`:

```ini
VITE_API_BASE_URL=http://localhost:8080/api
```

## Useful Commands

Start MySQL for local development:

```powershell
.\scripts\start-mysql.ps1
```

Initialize the database schema:

```powershell
.\scripts\init-db.ps1
```

Run backend tests:

```powershell
cd backend
.\mvnw.cmd test
```

Run frontend build:

```powershell
cd frontend
npm install
npm run build
```
