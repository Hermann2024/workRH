# WorkRH Production Deployment

This guide describes the OVH-oriented Docker deployment path for `www.workrh.lu`. Use managed Postgres, managed Kafka, a real image registry, TLS at the load balancer or reverse proxy, and external backups.

## Required Inputs

Set these in `.env.local` or in the deployment secret manager:

- `WORKRH_IMAGE_REGISTRY`
- `WORKRH_IMAGE_TAG`
- `WORKRH_PUBLIC_BASE_URL`
- `WORKRH_FRONTEND_ORIGIN`
- `WORKRH_KAFKA_BOOTSTRAP_SERVERS`
- `WORKRH_DATABASE_USERNAME`
- `WORKRH_DATABASE_PASSWORD`
- one JDBC URL per service database:
  - `WORKRH_USERS_DATABASE_URL`
  - `WORKRH_LEAVES_DATABASE_URL`
  - `WORKRH_SICKNESS_DATABASE_URL`
  - `WORKRH_TELEWORK_DATABASE_URL`
  - `WORKRH_NOTIFICATIONS_DATABASE_URL`
  - `WORKRH_REPORTING_DATABASE_URL`
  - `WORKRH_SUBSCRIPTIONS_DATABASE_URL`
- `WORKRH_JWT_SECRET`
- `WORKRH_CONNECTOR_SECRET_KEY`
- `WORKRH_SUBSCRIPTION_BOOTSTRAP_KEY`
- `WORKRH_WORKSPACE_INTERNAL_KEY`
- `WORKRH_NOTIFICATION_INTERNAL_KEY`
- Stripe live keys and price IDs
- SMTP credentials

## OVH Target

Production public domain:

- frontend: `https://www.workrh.lu`
- API gateway: `https://www.workrh.lu`

Recommended OVH setup:

- OVH DNS zone for `workrh.lu`
- `A` or `AAAA` record for `www.workrh.lu` pointing to the OVH public instance or load balancer
- TLS certificate for `www.workrh.lu`, ideally Let's Encrypt through the reverse proxy
- OVH Public Cloud instance or Kubernetes node pool for the Docker stack
- managed PostgreSQL databases or OVH database service equivalent
- managed Kafka-compatible endpoint or a production Kafka service outside the app Compose stack
- private Docker image registry such as GitLab Container Registry, GitHub Container Registry, Docker Hub, or OVH registry

## Gate

Run:

```powershell
.\check-saas-readiness.ps1
.\check-commercial-readiness.ps1
```

Both commands must pass before deployment.

## Deploy

```powershell
.\deploy-workrh-prod.ps1
```

The script:

1. loads `.env.local`
2. runs the commercial readiness gate
3. runs backend tests
4. builds the frontend
5. pulls production images
6. starts `infra/docker/docker-compose.prod.yml`
7. runs smoke tests against `WORKRH_PUBLIC_BASE_URL`

For emergency redeploys where CI has already tested the exact image:

```powershell
.\deploy-workrh-prod.ps1 -SkipBuild
```

## Smoke Test Only

```powershell
.\smoke-test-workrh.ps1 -BaseUrl https://www.workrh.lu
```

## Rollback

1. Set `WORKRH_IMAGE_TAG` to the previous known-good image tag.
2. Re-run `.\deploy-workrh-prod.ps1 -SkipBuild`.
3. Run `.\smoke-test-workrh.ps1`.
4. Check Stripe webhooks and delayed jobs after rollback.

## Notes

- `docker-compose.prod.yml` intentionally does not run Postgres or Kafka containers.
- Only the API gateway is exposed publicly.
- Service-to-service traffic stays inside the Compose network.
- TLS should terminate at the cloud load balancer or reverse proxy.
