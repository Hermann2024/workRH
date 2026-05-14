# WorkRH Pilot Readiness

Use this mode when you do not have paid hosting yet.

The goal is to sell demos, run assisted pilots, and validate demand without pretending the public SaaS infrastructure is live.

## What This Mode Allows

- live demo from your machine
- guided prospect walkthrough
- paid or free pilot with manual onboarding
- video demo and screenshots
- validation of pricing, workflows, and objections

## What This Mode Does Not Allow

- public self-service signup without your involvement
- production SLA
- unattended customer usage
- real customer data without a written pilot agreement
- claim that the SaaS is fully hosted and production-operated

## Required Checks

Run:

```powershell
.\check-pilot-readiness.ps1
```

Expected result:

```text
Pilot readiness: OK for demo / assisted pilot without paid hosting
```

## Demo Start

```powershell
.\launch-workrh.ps1
```

Local URLs:

- frontend: `http://localhost:4200`
- API gateway: `http://localhost:9080`
- Eureka: `http://localhost:9761`

## Before Each Prospect Demo

```powershell
.\check-pilot-readiness.ps1
mvn -pl services/reporting-service test
npm --prefix frontend/angular-app run build
```

If the demo needs billing:

- use Stripe test mode
- verify Starter, Pro, and Premium test price IDs
- do not enter real card or customer billing data unless you intentionally use live Stripe

## Pilot Offer

Suggested positioning:

- "Assisted pilot"
- fixed duration: 14 to 30 days
- one company workspace
- anonymized or test employee data by default
- manual onboarding by you
- no production SLA until hosted production is funded

## Pilot Exit Criteria

Convert to paid hosted SaaS only when at least one of these is true:

- a customer signs a paid pilot
- a prospect agrees to pay for setup or first month
- you can fund one month of hosting from committed revenue

## Production Upgrade

When hosting is available, move to:

```powershell
.\check-commercial-readiness.ps1
.\deploy-workrh-prod.ps1
```

Production requires managed Postgres, Kafka, image registry, public URLs, SSL, backups, and incident monitoring.
