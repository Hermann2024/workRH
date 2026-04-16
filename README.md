# WorkRH - HR Platform for Luxembourg Cross-Border Teams

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Angular](https://img.shields.io/badge/Angular-19-red.svg)](https://angular.io/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)

> WorkRH is a SaaS-oriented HR platform focused on telework compliance for Luxembourg cross-border teams.

## Overview

WorkRH combines operational HR workflows with telework compliance logic tailored to companies employing cross-border workers around Luxembourg.

The repository currently provides:

- a working Angular frontend
- a Spring Boot microservices backend
- tenant-aware authentication and role separation
- telework, leave, sickness, reporting, notification, and subscription modules
- Stripe-based subscription flows

## Product Scope

WorkRH is designed around three concrete use cases:

1. Employee self-service
   - declare telework outside Luxembourg
   - request leave
   - declare sickness periods
   - review personal compliance indicators
2. HR operations
   - monitor annual and weekly thresholds
   - identify fiscal and policy alerts
   - review monthly statistics and exports
   - manage telework policies by country
3. Billing and subscription management
   - self-service signup
   - plan selection and Stripe checkout
   - subscription upgrades and downgrades
   - accounting export and support workflows

## Core Capabilities

- Luxembourg telework compliance centered on the 34-day fiscal rule
- Multi-tenant workspace isolation
- JWT-based authentication with ADMIN, HR, and EMPLOYEE roles
- Dashboard views for HR supervision and employee self-service
- Country-based telework policy management
- Stripe checkout and subscription lifecycle handling
- Notification and support flows for higher plans

## Architecture

The platform is split into the following modules:

- `platform/config-server`
- `platform/discovery-service`
- `platform/api-gateway`
- `services/user-service`
- `services/leave-service`
- `services/sickness-service`
- `services/telework-service`
- `services/reporting-service`
- `services/notification-service`
- `services/subscription-service`
- `frontend/angular-app`

## Subscription Plans

| Plan | Monthly price | Positioning |
|------|---------------|-------------|
| Starter | EUR 199 | Core HR flows for small teams |
| Pro | EUR 299 | Telework compliance, alerts, exports, monthly stats |
| Premium | EUR 399 | Audit, SMS, accounting export, SLA, onboarding support |
| Enterprise | Custom | SSO, dedicated hosting, public API, custom development |

Current subscription logic is monthly. Trial initialization is supported in the codebase.

## Local Start

### Prerequisites

- Java 17+
- Maven 3.9+
- Node.js 18+
- npm
- Docker Desktop or Docker Engine with Compose

### Windows fast start

From the repository root:

```powershell
Copy-Item .env.example .env.local
.\launch-workrh.ps1
```

This script:

- starts the local infrastructure from `infra/docker/docker-compose.yml`
- builds the Maven reactor with tests skipped for launch speed
- opens one terminal per backend service
- starts Angular on port `4200`

### Manual build

```powershell
mvn clean install "-Dmaven.test.skip=true" -T1C
cd frontend/angular-app
npm install
npm run build
```

## Local URLs

- Frontend: `http://localhost:4200`
- API Gateway: `http://localhost:9080`
- Config Server: `http://localhost:9888`
- Eureka Dashboard: `http://localhost:9761`

## Runtime Frontend Configuration

Frontend runtime values are loaded from:

- `frontend/angular-app/src/assets/workrh-config.js`

This allows you to change:

- API base URL
- frontend base URL
- default tenant hint
- demo hint visibility

without recompiling the Angular source.

## Security and Billing Notes

What is already present:

- JWT authentication
- role-based access control
- tenant-based request routing
- Stripe checkout integration
- Stripe webhook handling

What still depends on deployment hardening:

- production secret management
- legal and privacy validation
- backup policy
- operational monitoring and alerting
- final Stripe price alignment in each target environment

## API Reality

The application exposes REST endpoints used by the Angular frontend and by service-to-service flows.

Important current note:

- public HTTP API documentation is not yet packaged as a polished developer portal

## Testing

Useful commands in the current repository:

```powershell
mvn test
mvn -pl services/user-service -Dtest=EmployeeServiceTest test
mvn -pl services/subscription-service -Dtest=SubscriptionServiceTest,SubscriptionInvoiceExportServiceTest test
cd frontend/angular-app
npm run build
```

Before broad commercial rollout, keep smoke checks on:

- authentication
- plan display and prices
- Stripe checkout configuration
- policy editing by country
- subscription upgrade and downgrade flows

## Current Product Status

The codebase is in a good state for:

- product demo
- assisted pilot
- early commercial validation

Broad self-serve commercialization still requires hardening around operations, documentation, monitoring, and deployment discipline.

## License

This project is licensed under the MIT License. See `LICENSE` if present in your distribution context.

## Support

- Repository docs: `docs/`
- Troubleshooting guides: `TROUBLESHOOTING.md`, `LAUNCH_GUIDE.md`, `README-SCRIPTS.txt`
- Email placeholder: `support@workrh.com`
