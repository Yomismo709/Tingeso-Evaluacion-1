# Keycloak Setup

## Requirements

- Docker Desktop with Docker Compose.
- The backend can run locally with `mvnw.cmd spring-boot:run`; a backend container is not part of the current Compose file.

## Start Keycloak

Copy `.env.example` to `.env`, replace the local admin password, and start Keycloak:

```powershell
docker compose up -d keycloak
```

Open the admin console at `http://localhost:8081`. The `siga-ipt` realm is imported automatically with the `ADMIN`, `TEACHER`, and `STUDENT` realm roles and the public `siga-ipt-frontend` client configured for Authorization Code with PKCE.

## Add Users

Create each user in the `siga-ipt` realm. Set the username to the user's RUN exactly as it is stored for the corresponding student or teacher, enable the account, set a password, and assign the appropriate realm role. For administrators, assign `ADMIN`. Do not reuse the Keycloak bootstrap administrator for application access.

The API reads the user's RUN from the JWT `preferred_username` claim and verifies it against the student or teacher record. Students can access only their own academic records, and teachers can only access grade operations for sections assigned to them. Administrators can manage records across users.

## Backend Configuration

The local defaults are:

- Issuer: `http://localhost:8081/realms/siga-ipt`
- JWKS: `http://localhost:8081/realms/siga-ipt/protocol/openid-connect/certs`

Override these with `KEYCLOAK_ISSUER_URI` and `KEYCLOAK_JWK_SET_URI` when the service runs elsewhere. The issuer must exactly match the `iss` claim in access tokens. If the backend runs in Compose, keep the issuer URL that clients use and set the JWKS URL to the internal Keycloak address, for example `http://keycloak:8080/realms/siga-ipt/protocol/openid-connect/certs`.

The current setup uses Keycloak development mode and is for local development only. Production requires HTTPS, persistent Keycloak storage, secure secrets, and production hostname settings.