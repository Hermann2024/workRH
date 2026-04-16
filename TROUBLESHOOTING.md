# 🔧 WorkRH - Guide de dépannage

## ❌ Erreurs courantes et solutions

### 1. "Docker n'est pas accessible"

**Symptômes:**
```
Cannot connect to Docker daemon
Error response from daemon
```

**Solutions:**
```powershell
# Vérifie que Docker Desktop est lancé
# Redémarre Docker Desktop

# Ou teste la connexion
docker ps
```

---

### 2. "Maven : Command not found"

**Symptômes:**
```
'mvn' n'est pas reconnu
mvn command not found
```

**Solutions:**

Option 1 : Ajouter Maven au PATH
```cmd
set PATH=%PATH%;C:\Program Files\Maven\bin
```

Option 2 : Utiliser le Maven wrapper du projet
```cmd
mvnw clean package -DskipTests
```

Option 3 : Réinstaller Maven
- Télécharger depuis https://maven.apache.org/
- Ajouter aux variables d'environnement

---

### 3. "Port déjà utilisé"

**Symptômes:**
```
Address already in use
Port 8080 already in use
```

**Solutions:**

```cmd
REM Trouver le processus utilisant le port
netstat -ano | findstr :8080

REM Arrêter le processus (remplacer 12345 par le PID)
taskkill /PID 12345 /F

REM Ou changer le port dans application.yml
```

---

### 4. "npm install échoue"

**Symptômes:**
```
npm ERR! ERESOLVE unable to resolve dependency tree
npm ERR! code EUTARGET
```

**Solutions:**

```cmd
cd frontend\angular-app

REM Nettoyer le cache
npm cache clean --force

REM Supprimer node_modules
rmdir /s node_modules
del package-lock.json

REM Réinstaller
npm install
```

---

### 5. "PostgreSQL ne démarre pas"

**Symptômes:**
```
database "postgres" does not exist
connection refused
```

**Solutions:**

```cmd
REM Vérifier que Docker fonctionne
docker ps

REM Vérifier les logs
docker-compose logs postgres

REM Nettoyer et redémarrer
docker-compose down
docker-compose up -d
```

---

### 6. "API Gateway n'accède pas aux services"

**Symptômes:**
```
503 Service Unavailable
Cannot find service
```

**Solutions:**

```cmd
REM Vérifier que les services sont enregistrés dans Eureka
curl http://localhost:8761/eureka/apps

REM Attendre quelques secondes (les services prennent du temps à s'enregistrer)

REM Vérifier les logs du service
REM Chercher "Registering with Eureka"
```

---

### 7. "Frontend ne compile pas"

**Symptômes:**
```
ng: command not found
ERROR in src/app/...
```

**Solutions:**

```cmd
REM Installer Angular CLI globalement
npm install -g @angular/cli

REM Ou utiliser le CLI local
npx ng serve

REM Nettoyer
cd frontend\angular-app
npm cache clean --force
rm -r node_modules
npm install
```

---

### 8. "Timeout sur la compilation Maven"

**Symptômes:**
```
BUILD FAILURE
Timeout waiting for...
```

**Solutions:**

```cmd
REM Compiler avec plus de temps
mvn clean package -DskipTests -T1C

REM Augmenter la mémoire Java
set MAVEN_OPTS=-Xmx2048m
mvn clean package -DskipTests

REM Nettoyer le cache Maven
mvn clean -U
```

---

### 9. "JWT/Authentification échoue"

**Symptômes:**
```
401 Unauthorized
Invalid token
```

**Solutions:**

```cmd
REM Vérifier le header Authorization
REM Format: "Authorization: Bearer <token>"

REM Le token par défaut en mode demo est généré automatiquement

REM Vérifier dans les logs du service
REM Chercher "JWT validation"
```

---

### 10. "Stripe checkout échoue"

**Symptômes:**
```
402 Payment Required
Stripe API key missing
```

**Solutions:**

```cmd
REM Vérifier les variables d'environnement
set STRIPE_SECRET_KEY=sk_test_...

REM Ou créer un fichier .env.local
# .env.local
STRIPE_SECRET_KEY=sk_test_...
STRIPE_WEBHOOK_SECRET=whsec_...

REM Redémarrer le service
```

---

## 🔍 Commandes de diagnostic

### Voir tous les conteneurs Docker
```cmd
docker ps -a
```

### Voir les logs d'un conteneur
```cmd
docker-compose logs -f [service-name]
docker logs [container-id]
```

### Vérifier un port
```cmd
netstat -ano | findstr :[port]
```

### Faire un health check API
```cmd
curl http://localhost:8080/actuator/health
curl http://localhost:8761/eureka/apps
curl http://localhost:8888/health
```

### Voir les processus Java
```cmd
jps -l
```

### Arrêter un service proprement
```cmd
REM Terminal où le service s'exécute
Ctrl+C
```

---

## 📊 Vérifier l'état du système

### Dashboard complet
```
Frontend:         http://localhost:4200
Eureka:           http://localhost:8761
Config Server:    http://localhost:8888
API Gateway:      http://localhost:8080
```

### Logs en temps réel
```cmd
REM Terminal 1 - Docker
docker-compose logs -f

REM Terminal 2 - Services (vérifier les titres des fenêtres)
REM Terminal 3 - Frontend
REM Ctrl+C pour arrêter les logs
```

---

## 💾 Backup et reset

### Réinitialiser les bases de données
```cmd
REM Arrêter Docker
docker-compose down

REM Supprimer les volumes
docker volume rm workrh_postgres_users_data workrh_postgres_leaves_data ...

REM Redémarrer
docker-compose up -d
```

### Nettoyer complètement
```cmd
REM Arrêter tout
.\stop-workrh.bat

REM Nettoyer Docker
docker system prune -a

REM Nettoyer Maven
rmdir /s %USERPROFILE%\.m2\repository

REM Redémarrer
.\launch-workrh.bat
```

---

## 📝 Logs à consulter

### Config Server
- Vérifier la chargemet des configs
- Chercher "Spring Cloud Config" dans les logs

### Eureka
- http://localhost:8761 pour voir les services enregistrés

### API Gateway
- Chercher "Route defined" dans les logs
- Vérifier les filtres actifs

### Services
- Consulter les logs de chaque service pour les erreurs métier

### Frontend
- Vérifier la console du navigateur (F12)
- Chercher les erreurs CORS

---

## ⚡ Tips de performance

### Accélérer Maven
```cmd
mvn clean package -T1C -DskipTests
```

### Réduire la mémoire Docker
```cmd
docker-compose down
REM Réduire les limites dans docker-compose.yml
docker-compose up -d
```

### Vider les caches
```cmd
npm cache clean --force
mvn clean
docker system prune -a
```

---

## 🆘 Besoin d'aide ?

### Recueillir les informations de diagnostic
```cmd
echo "=== System Info ===" > diagnostic.log
docker ps -a >> diagnostic.log
docker-compose logs >> diagnostic.log
netstat -ano >> diagnostic.log
java -version >> diagnostic.log
mvn --version >> diagnostic.log
node --version >> diagnostic.log
```

### Partager les logs
```cmd
REM Copier les logs pertinents
type [filename].log > debug_info.txt
```

### Ressources
- GitHub Issues: https://github.com/Hermann2024/workRH/issues
- Email: support@workrh.com
- Documentation: README.md, docs/

---

**Dernière mise à jour:** 28 mars 2026
