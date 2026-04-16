# 🚀 WorkRH - Guide de lancement rapide

## ⚡ Démarrage en 1 clic

### Option 1 : Script Batch (Recommandé - Plus simple)

```bash
double-clic sur : launch-workrh.bat
```

ou en ligne de commande :

```cmd
cd c:\Users\phare\OneDrive\Documents\workRH
launch-workrh.bat
```

### Option 2 : Script PowerShell (Plus robuste)

Ouvre PowerShell et exécute :

```powershell
cd c:\Users\phare\OneDrive\Documents\workRH
.\launch-workrh.ps1
```

## ✅ Qu'est-ce qui va se passer ?

Le script va automatiquement :

1. ✓ Vérifier que Docker, Maven, Node.js sont installés
2. ✓ Lancer l'infrastructure (PostgreSQL, Kafka, Zookeeper)
3. ✓ Compiler le projet Maven
4. ✓ Lancer les services backend dans des fenêtres séparées :
   - Config Server (port 8888)
   - Eureka Discovery (port 8761)
   - API Gateway (port 8080)
   - 7 microservices (ports 8081-8087)
5. ✓ Lancer le frontend Angular (port 4200)

## 🌐 Accès à l'application

Une fois tout lancé, tu peux accéder à :

| Service | URL | Description |
|---------|-----|-------------|
| **Frontend** | http://localhost:4200 | Application WorkRH |
| **API Gateway** | http://localhost:8080 | API REST |
| **Eureka Dashboard** | http://localhost:8761 | Services enregistrés |
| **Config Server** | http://localhost:8888 | Configuration centralisée |

## 🔐 Identifiants de connexion

```
Email:    rh@company.com
Password: secret
Tenant:   demo-lu
```

Ou essaye aussi :
- `admin@company.com` (rôle ADMIN)
- `employee@company.com` (rôle EMPLOYEE)

## 📊 Vérification du statut

### Vérifier si les services démarrent correctement

**Config Server:**
```
http://localhost:8888/health
```

**API Gateway:**
```
http://localhost:8080/actuator/health
```

**Eureka:**
```
http://localhost:8761
```

## 🛑 Arrêter l'application

### Arrêter tous les services :

1. Ferme les fenêtres PowerShell/CMD une par une
2. Arrête le frontend (Ctrl+C dans la fenêtre)
3. Arrête Docker : 
   ```cmd
   docker-compose -f infra\docker\docker-compose.yml down
   ```

## ⚙️ Prérequis

Assure-toi que les éléments suivants sont installés et accessibles dans le PATH :

- **Docker Desktop** (24+) : https://www.docker.com/products/docker-desktop
- **Java** (17+) : `java -version`
- **Maven** (3.6+) : `mvn --version`
- **Node.js** (18+) : `node --version`
- **npm** (9+) : `npm --version`

## 🐛 Troubleshooting

### Docker n'est pas lancé
```cmd
REM Démarre Docker Desktop manuellement
```

### Port déjà utilisé
```cmd
REM Chercher quel processus utilise le port (ex: 4200)
netstat -ano | findstr :4200

REM Arrêter le processus
taskkill /PID <PID> /F
```

### Maven ne compile pas
```cmd
REM Nettoyer le cache Maven
mvn clean
mvn package -DskipTests
```

### Frontend ne démarre pas
```cmd
cd frontend\angular-app
npm install
npm start
```

## 📝 Fichiers importants

- `launch-workrh.bat` - Script batch (Windows)
- `launch-workrh.ps1` - Script PowerShell
- `run-all.ps1` - Script PowerShell original
- `deploy.sh` - Script de déploiement GitHub
- `infra/docker/docker-compose.yml` - Configuration Docker

## 🔗 Documentation complète

Voir `README.md` pour plus de détails sur :
- Architecture du système
- Développement local
- Testing
- Déploiement en production
- API Documentation

---

**WorkRH** - SaaS Platform for HR Management with Luxembourg Telework Compliance
