# GitHub OAuth 2.0 Integration Specification

## 1. OAuth 2.0 Architecture

E-ShoppingZone integrates GitHub as a third-party identity provider using standard OAuth 2.0 Authorization Code Grant via Spring Security's `spring-boot-starter-oauth2-client`.

---

## 2. GitHub OAuth Application Configuration

To enable GitHub login, register an OAuth application on GitHub Developer Settings:
- **Application Name**: `E-ShoppingZone`
- **Homepage URL**: `http://localhost:5173`
- **Authorization Callback URL**: `http://localhost:8080/login/oauth2/code/github`

Configure the corresponding environment variables:
```bash
GITHUB_CLIENT_ID=your_github_client_id
GITHUB_CLIENT_SECRET=your_github_client_secret
```

---

## 3. Account Provisioning & Linking Logic

When GitHub redirects the user back with an authorization code:
1. `Profile Service` exchanges the code for a GitHub access token.
2. `Profile Service` calls the GitHub User API (`https://api.github.com/user`) to retrieve the user's `id`, `login`, `email`, `name`, and `avatar_url`.
3. Account matching workflow:
   - Check if an account already exists with `github_id = :githubId`. If found, log the user in.
   - If not found, check if an account exists with `email = :email`.
     - If yes: Link the GitHub ID to the existing account (`UPDATE users SET github_id = :id`).
     - If no: Auto-provision a new user record with `auth_provider = 'GITHUB'`, role `ROLE_CUSTOMER`, and empty password hash.
4. Auto-generate JWT access token and opaque refresh token cookie.
5. Redirect browser back to React UI callback page:
   `http://localhost:5173/oauth2/redirect?token=<accessToken>`

