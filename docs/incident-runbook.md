# WorkRH Incident Runbook

Use this runbook for production incidents affecting customers, billing, authentication, or data integrity.

## Severity

- SEV1: platform unavailable, data loss risk, payment flow broken for all users, or tenant data exposure suspected.
- SEV2: one major feature unavailable, degraded performance, failed webhooks, or one tenant blocked.
- SEV3: limited user impact with a workaround.

## First 10 Minutes

1. Assign an incident owner.
2. Record start time, impacted tenants, and impacted features.
3. Check gateway, service, Postgres, Kafka, and Stripe health.
4. Stop risky changes: pause deploys and background maintenance.
5. If data exposure is suspected, preserve logs and disable affected endpoints before debugging.

## Checks

```powershell
.\check-commercial-readiness.ps1
```

Health endpoints:

```powershell
Invoke-WebRequest http://localhost:9080/actuator/health
Invoke-WebRequest http://localhost:9081/actuator/health
Invoke-WebRequest http://localhost:9086/actuator/health
Invoke-WebRequest http://localhost:9087/actuator/health
```

Recent backups:

```powershell
Get-ChildItem $env:WORKRH_BACKUP_DIR | Sort-Object LastWriteTime -Descending | Select-Object -First 5
```

## Stripe Incident

1. Check webhook delivery in Stripe Dashboard.
2. Confirm `STRIPE_WEBHOOK_SECRET` and live/test mode match the deployed environment.
3. Retry failed webhook events from Stripe once the service is healthy.
4. Compare subscription state in WorkRH with Stripe customer/subscription state.

## Authentication Incident

1. Confirm `WORKRH_JWT_SECRET` has not rotated unexpectedly.
2. Confirm `WORKRH_DEMO_AUTHENTICATION_ENABLED=false`.
3. Check login error rates and tenant mismatch errors.
4. If secret rotation was accidental, restore the previous secret or force re-login for all users.

## Database Incident

1. Stop write traffic if corruption or tenant leakage is suspected.
2. Take a fresh backup before attempting repair.
3. Restore only after choosing the target backup and confirming customer impact.

Restore command:

```powershell
.\restore-workrh.ps1 -BackupDirectory .\backups\YYYYMMDD-HHMMSS -ConfirmRestore RESTORE_WORKRH_DATABASES
```

## Communication

For SEV1 and customer-visible SEV2:

- publish an initial customer update within 30 minutes
- update every 60 minutes until resolved
- publish a short post-incident summary within 2 business days

## Closure

Before closing:

- customer impact is known
- monitoring is green
- delayed jobs/webhooks have caught up
- follow-up issues are created for root cause fixes
