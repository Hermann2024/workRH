# WorkRH OVH Go-Live

Target domain: `www.workrh.lu`

## 1. DNS

In the OVH DNS zone for `workrh.lu`:

- create `A www` pointing to the OVH public IPv4 of the production host or load balancer;
- create `AAAA www` only if the host has stable IPv6;
- keep TTL low during launch, for example 300 seconds;
- after validation, raise TTL if desired.

## 2. TLS and Reverse Proxy

Terminate HTTPS before the API gateway. Recommended simple setup on an OVH VM:

- install Caddy or Nginx;
- issue a Let's Encrypt certificate for `www.workrh.lu`;
- proxy `/` to the Angular static frontend if hosted on the same VM;
- proxy `/api/*` and `/actuator/health` to the gateway on `127.0.0.1:9080`.

Example Caddyfile:

```caddyfile
www.workrh.lu {
  encode zstd gzip

  handle_path /api/* {
    reverse_proxy 127.0.0.1:9080
  }

  handle_path /actuator/* {
    reverse_proxy 127.0.0.1:9080
  }

  root * /var/www/workrh
  try_files {path} /index.html
  file_server
}
```

## 3. Environment

On the OVH host:

```powershell
Copy-Item .env.ovh.template .env.local
notepad .env.local
```

Fill all empty values, then run:

```powershell
.\check-saas-readiness.ps1
```

The script must print `SaaS readiness: OK for production go-live checks`.

## 4. Images

Build and push immutable images to the selected registry.

Set:

```text
WORKRH_IMAGE_REGISTRY=<registry>/<namespace>/workrh
WORKRH_IMAGE_TAG=<release-version>
```

The production compose file expects one image per service:

- `discovery-service`
- `config-server`
- `api-gateway`
- `user-service`
- `leave-service`
- `sickness-service`
- `telework-service`
- `notification-service`
- `reporting-service`
- `subscription-service`

## 5. Databases and Kafka

Use production services, not the local development Compose stack.

Required:

- one PostgreSQL JDBC URL per service database;
- one Kafka bootstrap endpoint;
- database backups enabled outside the application host.

## 6. Deploy

```powershell
.\deploy-workrh-prod.ps1
```

Emergency redeploy of already tested images:

```powershell
.\deploy-workrh-prod.ps1 -SkipBuild
```

## 7. Smoke Test

```powershell
.\smoke-test-workrh.ps1 -BaseUrl https://www.workrh.lu
```

## 8. Stripe

Configure Stripe live webhooks:

- endpoint: `https://www.workrh.lu/api/subscriptions/webhooks/stripe`
- copy the live webhook secret into `STRIPE_WEBHOOK_SECRET`;
- use live price IDs for Starter, Pro and Premium.

## 9. Launch Gate

Before opening sales:

```powershell
.\check-saas-readiness.ps1
.\check-commercial-readiness.ps1
npm --prefix frontend/angular-app run build
mvn test
```

All commands must pass.
