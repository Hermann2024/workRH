# WorkRH Production Readiness

This checklist defines the minimum bar before opening WorkRH as a self-service SaaS.

If paid hosting is not available yet, use `docs/pilot-readiness.md` and `check-pilot-readiness.ps1` instead. Production readiness intentionally requires managed infrastructure and should remain blocked until those values exist.

## Required Environment

- `WORKRH_JWT_SECRET`: base64-encoded 256-bit signing secret.
- `WORKRH_CONNECTOR_SECRET_KEY`: base64-encoded 256-bit key for HR connector secrets.
- `WORKRH_DEMO_AUTHENTICATION_ENABLED=false`.
- `WORKRH_HIBERNATE_DDL_AUTO=validate`.
- `WORKRH_FRONTEND_ORIGIN`: public frontend origin, for example `https://app.workrh.com`.
- Stripe live keys and price IDs for Starter, Pro, and Premium.
- `SMTP_HOST`, `SMTP_USERNAME`, and `SMTP_PASSWORD` configured for support and transactional emails.
- `WORKRH_BACKUP_DIR`: durable backup destination.
- `WORKRH_INCIDENT_CONTACT`: operational owner reachable during incidents.
- `WORKRH_PUBLIC_BASE_URL`: public API gateway URL used by smoke tests.
- `WORKRH_IMAGE_REGISTRY` and `WORKRH_IMAGE_TAG`: immutable production image reference.
- `WORKRH_KAFKA_BOOTSTRAP_SERVERS`: managed Kafka endpoint.
- one managed Postgres JDBC URL per service database.

## Deployment Rules

- Do not deploy with demo authentication enabled.
- Do not deploy with Hibernate schema update/create mode.
- Do not store production secrets in Git or in Docker Compose files.
- Do not run production Postgres or Kafka from the local development Compose file.
- Run database migrations before serving traffic.
- Keep `/actuator/health` and `/actuator/info` reachable by infrastructure health checks only.
- Keep gateway CORS restricted to the production frontend origin.

## Database

Reporting schema is managed by Flyway in `services/reporting-service/src/main/resources/db/migration`.

Before every release:

```powershell
mvn test
npm --prefix frontend/angular-app run build
.\check-saas-readiness.ps1
```

Backups:

```powershell
.\backup-workrh.ps1
```

Restore requires an explicit destructive confirmation:

```powershell
.\restore-workrh.ps1 -BackupDirectory .\backups\YYYYMMDD-HHMMSS -ConfirmRestore RESTORE_WORKRH_DATABASES
```

## Monitoring Minimum

Track these signals per service:

- process/container up
- `/actuator/health` status
- HTTP 5xx rate
- p95 API latency
- failed login spikes
- Stripe webhook failures
- Kafka consumer lag
- Postgres disk usage and connection count
- backup success age

Alert immediately when:

- a service health check fails for more than 3 minutes
- Stripe webhooks fail for more than 5 minutes
- backup age exceeds 24 hours
- database disk usage exceeds 80 percent
- API 5xx rate exceeds 2 percent for 10 minutes

## Commercial Gate

Run:

```powershell
.\check-saas-readiness.ps1
.\check-commercial-readiness.ps1
```

Both commands must pass. `check-saas-readiness.ps1` is the hard production gate; `check-commercial-readiness.ps1` remains the commercial packaging gate.

Deploy with:

```powershell
.\deploy-workrh-prod.ps1
```

Smoke test with:

```powershell
.\smoke-test-workrh.ps1 -BaseUrl $env:WORKRH_PUBLIC_BASE_URL
```
