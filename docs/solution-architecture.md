# Architecture détaillée

## Vue d'ensemble

- `Frontend Angular`: portail admin/RH/employé.
- `API Gateway`: point d’entrée unique, propagation du JWT et du `X-Tenant-Id`.
- `Config Server`: centralise les propriétés d’exécution.
- `Discovery Service`: annuaire des microservices.
- `Kafka`: backbone événementiel.
- `PostgreSQL`: une base dédiée par microservice.

## Découpage métier

### `user-service`

- gère employés, rôles, onboarding.
- expose l’authentification et génère les JWT.
- prépare la future extension SSO/SCIM.

### `leave-service`

- enregistre les demandes de congés.
- gère la validation RH.
- publie les événements `leave.status.changed`.

### `sickness-service`

- enregistre les arrêts maladie.
- publie les événements `sickness.declared`.

### `telework-service`

- calcule le télétravail éligible.
- maintient l’historique des déclarations.
- exclut automatiquement:
  - week-ends
  - jours fériés Luxembourg
  - congés approuvés
  - arrêts maladie
- publie:
  - `telework.declared`
  - `telework.threshold.exceeded`

### `notification-service`

- consomme les événements d’alerte et de validation.
- journalise les notifications envoyées.
- point d’extension pour SMTP, Twilio, providers transactionnels.

### `reporting-service`

- fournit le dashboard RH.
- prépare les exports.
- pourra évoluer vers un entrepôt analytique ou ClickHouse/BigQuery.

## Flux principaux

### Déclaration télétravail

1. L’employé appelle `POST /api/telework`.
2. Le gateway relaie le JWT et `X-Tenant-Id`.
3. `telework-service` contrôle la date.
4. `telework-service` consulte son référentiel local d’exclusions.
5. Si la journée est valide, la déclaration est persistée.
6. Un événement Kafka est publié.
7. Si le plafond est dépassé, une alerte Kafka est émise.
8. `notification-service` et `reporting-service` consomment l’événement.

### Validation d’un congé

1. RH approuve la demande via `leave-service`.
2. Le service publie `leave.status.changed`.
3. `telework-service` matérialise l’exclusion dans sa base locale.
4. Les futures déclarations sur cette période sont bloquées.

## Multi-tenant

- stratégie retenue: `shared database, shared schema, tenant discriminator`.
- tous les agrégats portent `tenant_id`.
- le tenant est propagé par `X-Tenant-Id`.
- recommandation production:
  - contrôle systématique au niveau service.
  - audit logs par tenant.
  - chiffrement des secrets par tenant si connecteurs externes.

## Sécurité

- authentification par JWT signé.
- rôles `ADMIN`, `HR`, `EMPLOYEE`.
- sécurité method-level via `@PreAuthorize`.
- recommandation production:
  - rotation de clé JWT.
  - refresh tokens.
  - MFA pour rôles RH/Admin.
  - rate limiting au niveau gateway.

## Évolutions recommandées avant go-live

- ajouter Flyway/Liquibase.
- remplacer le placeholder PDF par une génération réelle.
- brancher notification email/SMS sur des providers réels.
- enrichir le calendrier des fériés mobiles luxembourgeois.
- ajouter observabilité: Prometheus, Grafana, OpenTelemetry.
