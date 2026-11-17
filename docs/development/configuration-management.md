# Configuration Management & Environment Variables

## 1. Principles of Configuration

1. **Sensible Local Defaults**: All microservices run locally without required external variables; standard fallback defaults point to `localhost:3306`, user `root`, password `root`, and development JWT keys.
2. **Environment Variable Overrides**: Production and external secrets (database passwords, GitHub OAuth credentials, JWT signing keys) must be supplied via system environment variables.
3. **No Committed Secrets**: Never hardcode client secrets or production database credentials in Git repositories.

---

## 2. Environment Variables Reference

| Variable Name | Default Value | Microservice Affected | Description |
| :--- | :--- | :--- | :--- |
| `DB_HOST` | `localhost` | All core services | MySQL server hostname |
| `DB_PORT` | `3306` | All core services | MySQL server port |
| `DB_USER` | `root` | All core services | MySQL database username |
| `DB_PASSWORD` | `root` | All core services | MySQL database password |
| `JWT_SECRET` | `404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970` | Gateway, Profile | Minimum 256-bit HMAC key |
| `JWT_EXPIRATION_MS` | `900000` (15 min) | Profile Service | Access token lifetime |
| `REFRESH_TOKEN_EXPIRATION_DAYS`| `7` | Profile Service | Refresh token lifetime |
| `GITHUB_CLIENT_ID` | `dummy_github_id` | Profile Service | GitHub OAuth App Client ID |
| `GITHUB_CLIENT_SECRET` | `dummy_github_secret` | Profile Service | GitHub OAuth App Secret |
| `EUREKA_SERVER_URL` | `http://localhost:8761/eureka/` | All clients | Service discovery registry URL |

