# Exemples d'appels API

## Authentification

```bash
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -H "X-Tenant-Id: demo-lu" \
  -d '{"email":"hr@company.com","password":"secret"}'
```

## Création employé

```bash
curl -X POST http://localhost:8080/api/users \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"email":"jane@company.com","password":"secret","firstName":"Jane","lastName":"Doe","countryOfResidence":"FR","phoneNumber":"0600000000","department":"Operations","jobTitle":"Consultant","crossBorderWorker":true,"hireDate":"2025-01-01","roles":["EMPLOYEE"]}'
```

## Profil courant

```bash
curl http://localhost:8080/api/users/me \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Demande de congé

```bash
curl -X POST http://localhost:8080/api/leaves \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"type":"PAID","startDate":"2026-04-06","endDate":"2026-04-08","comment":"Vacances de printemps"}'
```

## Validation d'un congé

```bash
curl -X POST http://localhost:8080/api/leaves/1/approve \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"comment":"Validé par RH"}'
```

## Déclaration arrêt maladie

```bash
curl -X POST http://localhost:8080/api/sickness \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"startDate":"2026-03-10","endDate":"2026-03-12","comment":"Arrêt médical"}'
```

## Déclaration télétravail

```bash
curl -X POST http://localhost:8080/api/telework \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"employeeId":1,"workDate":"2026-03-23","countryCode":"FR"}'
```

## Synthèse télétravail employé

```bash
curl "http://localhost:8080/api/telework/summary/1?year=2026&month=3&countryCode=FR" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Synthèse télétravail entreprise

```bash
curl "http://localhost:8080/api/telework/company-summary?year=2026&month=3&countryCode=FR" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Politique télétravail effective

```bash
curl "http://localhost:8080/api/telework/policies/effective?countryCode=FR" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Création politique télétravail pays

```bash
curl -X POST http://localhost:8080/api/telework/policies \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"countryCode":"FR","annualFiscalLimitDays":34,"weeklyCompanyLimitDays":2,"weeklyLimitEnabled":true,"active":true}'
```

## Mise à jour politique télétravail pays

```bash
curl -X PUT http://localhost:8080/api/telework/policies/1 \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"countryCode":"BE","annualFiscalLimitDays":34,"weeklyCompanyLimitDays":2,"weeklyLimitEnabled":true,"active":true}'
```

## Liste des politiques télétravail

```bash
curl http://localhost:8080/api/telework/policies \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Dashboard reporting

```bash
curl "http://localhost:8080/api/reports/dashboard?year=2026&month=3" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Liste des offres d'abonnement

```bash
curl http://localhost:8080/api/subscriptions/plans \
  -H "X-Tenant-Id: demo-lu"
```

## Souscription courante du tenant

```bash
curl http://localhost:8080/api/subscriptions/current \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Création / mise à jour de la souscription du tenant

```bash
curl -X POST http://localhost:8080/api/subscriptions/current \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"planCode":"PRO","status":"ACTIVE","seatsPurchased":25,"smsOptionEnabled":false,"advancedAuditOptionEnabled":false,"advancedExportOptionEnabled":true,"startsAt":"2026-03-01","renewsAt":"2026-04-01"}'
```

## Vérification d'accès à une feature

```bash
curl "http://localhost:8080/api/subscriptions/features/check?feature=TELEWORK_COMPLIANCE_34" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```

## Création d'une session Stripe Checkout

```bash
curl -X POST http://localhost:8080/api/subscriptions/checkout/stripe \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  -H "Content-Type: application/json" \
  -d '{"planCode":"PRO","seatsPurchased":25,"smsOptionEnabled":false,"advancedAuditOptionEnabled":false,"advancedExportOptionEnabled":true,"customerEmail":"admin@company.com","successUrl":"http://localhost:4200/dashboard?checkout=success","cancelUrl":"http://localhost:4200/pricing?checkout=cancelled"}'
```

## Webhook Stripe

```bash
curl -X POST http://localhost:8080/api/subscriptions/webhooks/stripe \
  -H "Stripe-Signature: <STRIPE_SIGNATURE>" \
  -H "Content-Type: application/json" \
  -d '<STRIPE_EVENT_JSON>'
```

## Export PDF réel

```bash
curl "http://localhost:8080/api/reports/dashboard/export/pdf?year=2026&month=3" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu" \
  --output dashboard.pdf
```

## Export placeholder

```bash
curl "http://localhost:8080/api/reports/dashboard/export/pdf-placeholder?year=2026&month=3" \
  -H "Authorization: Bearer <JWT>" \
  -H "X-Tenant-Id: demo-lu"
```
