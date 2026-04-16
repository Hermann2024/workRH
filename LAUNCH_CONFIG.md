# WorkRH - Configuration de lancement

## Ports utilisés

# Infrastructure
- PostgreSQL: 5432 (multiples instances par service)
- Kafka: 9092
- Zookeeper: 2181

# Backend Services
- Config Server: 8888
- Eureka Discovery: 8761
- API Gateway: 8080
- User Service: 8081
- Leave Service: 8082
- Sickness Service: 8083
- Telework Service: 8084
- Reporting Service: 8085
- Notification Service: 8086
- Subscription Service: 8087

# Frontend
- Angular Dev Server: 4200

## Bases de données

Chaque service a sa propre base de données PostgreSQL :
- user_service_db
- leave_service_db
- sickness_service_db
- telework_service_db
- notification_service_db
- reporting_service_db
- subscription_service_db

Identifiants: postgres / postgres

## Configuration Spring Cloud

### Config Server
Charge la configuration depuis : infra/config-repo/

### Eureka Discovery
- URL: http://localhost:8761
- Dashboard visible et consultable

### API Gateway
- Route toutes les requêtes vers les services
- Applique le contrôle d'accès

## Variables d'environnement optionnelles

Crée un fichier .env.local à la racine pour customizer :

```
# Stripe
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

# Autres configurations
FEATURE_FLAG_TELEWORK=true
DEBUG_MODE=false
```

## Commandes utiles

### Vérifier un port utilisé
```
netstat -ano | findstr :8080
```

### Arrêter un processus par PID
```
taskkill /PID 12345 /F
```

### Voir les logs Docker
```
docker-compose logs -f [nom-service]
```

### Lancer un service unique
```
mvn -pl platform/config-server spring-boot:run
```

### Compiler sans tester
```
mvn clean package -DskipTests
```

### Voir l'état de Eureka
```
curl http://localhost:8761/eureka/apps
```

### Health check API Gateway
```
curl http://localhost:8080/actuator/health
```

## Timeout et Performance

- Config Server: 30s de wait au démarrage
- Eureka: 5s min avant de recevoir du trafic
- Base de données: vérifier qu'elle démarre avant les services

## Logs importants à vérifier

1. Config Server boot: "Spring Boot started"
2. Eureka startup: "Registering application with eureka with initial instance info"
3. Services registration: "DiscoveryClient_..."
4. API Gateway routes: "Route..."
5. Frontend compilation: "Application bundle generation complete"

## Notes

- Les services Angular utilisent Hot Module Replacement (HMR)
- Les changements de code backend nécessitent un redémarrage
- Les changements de configuration nécessitent un restart du Config Server
- Les bases de données sont créées automatiquement au premier démarrage
