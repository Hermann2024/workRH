# Stratégie de déploiement

## Local

1. démarrer `docker compose -f infra/docker/docker-compose.yml up -d` pour Kafka et PostgreSQL.
2. lancer les services Spring via Maven ou images Docker.
3. lancer le frontend Angular séparément.

## Préproduction / Production

- conteneuriser chaque service.
- pousser les images vers un registre privé.
- déployer sur Kubernetes.
- config externe via Config Server ou ConfigMaps/Secrets.
- PostgreSQL managé par service ou cluster avec bases séparées.

## Pipeline cible

1. lint + tests unitaires.
2. build Maven/Angular.
3. scan SCA/SAST.
4. build images Docker.
5. publication registre.
6. déploiement progressif `staging`.
7. smoke tests.
8. promotion `production`.

## Stratégie de release

- `blue/green` ou `rolling update` pour services stateless.
- schémas DB versionnés par migration.
- compatibilité backward sur les événements Kafka.
- versionnement des APIs REST si rupture de contrat.

## SLA / exploitation

- health checks Actuator.
- alerting sur erreurs Kafka, saturation DB, dépassements réglementaires.
- sauvegardes PostgreSQL quotidiennes.
- rétention d’historique déclaratif selon exigences RH et fiscales.
