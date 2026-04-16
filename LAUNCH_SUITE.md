# 🚀 WorkRH - Suite de scripts de lancement

**Date:** 28 mars 2026  
**Version:** 1.0  
**Projet:** WorkRH - SaaS Platform for HR Management

## 📋 Fichiers créés

### 1. Scripts de lancement principal

#### `launch-workrh.bat` (Recommandé)
- **Type:** Batch (Windows)
- **Utilisation:** Double-clic ou `launch-workrh.bat`
- **Fonction:** Lance l'application complète en 1 clic
- **Avantages:** Simple, intuitif, idéal pour développeurs
- **Lancement des services:**
  - Infrastructure Docker (PostgreSQL, Kafka, Zookeeper)
  - Config Server (port 8888)
  - Eureka Discovery (port 8761)
  - API Gateway (port 8080)
  - 7 microservices (ports 8081-8087)
  - Frontend Angular (port 4200)

#### `launch-workrh.ps1`
- **Type:** PowerShell
- **Utilisation:** `.\launch-workrh.ps1`
- **Fonction:** Alternative plus robuste au script batch
- **Avantages:** Meilleure gestion d'erreurs, couleurs, plus moderne

### 2. Scripts d'arrêt

#### `stop-workrh.bat`
- **Type:** Batch
- **Utilisation:** Double-clic ou `stop-workrh.bat`
- **Fonction:** Arrête proprement tous les services

#### `stop-workrh.ps1`
- **Type:** PowerShell
- **Utilisation:** `.\stop-workrh.ps1`
- **Fonction:** Alternative PowerShell pour arrêter les services

### 3. Scripts de vérification

#### `check-requirements.bat`
- **Type:** Batch
- **Utilisation:** Double-clic ou `check-requirements.bat`
- **Fonction:** Vérifie tous les prérequis avant le lancement
- **Contrôles:**
  - Docker et Docker Compose
  - Java et Maven
  - Node.js et npm
  - Fichiers de configuration

#### `check-requirements.ps1`
- **Type:** PowerShell
- **Utilisation:** `.\check-requirements.ps1`
- **Fonction:** Alternative PowerShell pour vérifier les prérequis

### 4. Documentation

#### `LAUNCH_GUIDE.md`
- **Contenu:**
  - Guide de démarrage rapide
  - Instructions d'utilisation des scripts
  - URLs d'accès aux services
  - Identifiants de connexion par défaut
  - Troubleshooting basique
  - Prérequis détaillés

#### `LAUNCH_CONFIG.md`
- **Contenu:**
  - Configuration complète des ports
  - Bases de données par service
  - Variables d'environnement
  - Commandes utiles
  - Logs importants à vérifier
  - Notes de performance

#### `TROUBLESHOOTING.md`
- **Contenu:**
  - 10+ erreurs courantes et solutions
  - Commandes de diagnostic
  - Tips de performance
  - Procédures de backup/reset
  - Instructions pour recueillir les diagnostics

### 5. Fichiers originaux conservés

Les fichiers originaux ont été améliorés :
- `run-all.ps1` - Script PowerShell original (inchangé)
- `deploy.sh` - Script de déploiement GitHub (inchangé)

## 🎯 Flux d'utilisation recommandé

### Pour la première fois

```
1. Ouvrir: check-requirements.bat
   → Vérifier que tout est installé
   
2. Ouvrir: launch-workrh.bat
   → Lancer toute l'application
   
3. Attendre 30-60 secondes
   → Services en démarrage
   
4. Aller sur: http://localhost:4200
   → Frontend WorkRH
   
5. Connexion avec:
   Email: rh@company.com
   Password: secret
   Tenant: demo-lu
```

### Pour les accès ultérieurs

```
1. Ouvrir: launch-workrh.bat
   → Tout se lance automatiquement
   
2. Vérifier: http://localhost:8761 (Eureka)
   → Attendre que tous les services se montrent
   
3. Accéder: http://localhost:4200
```

### Pour arrêter proprement

```
1. Ouvrir: stop-workrh.bat
   → Tous les services s'arrêtent
   
2. Attendre la fin de l'exécution
   
3. Fermer les fenêtres CMD/PowerShell restantes
```

## 🌐 Ports et accès

| Service | Port | URL |
|---------|------|-----|
| Frontend | 4200 | http://localhost:4200 |
| API Gateway | 8080 | http://localhost:8080 |
| Eureka Dashboard | 8761 | http://localhost:8761 |
| Config Server | 8888 | http://localhost:8888 |
| User Service | 8081 | (interne) |
| Leave Service | 8082 | (interne) |
| Sickness Service | 8083 | (interne) |
| Telework Service | 8084 | (interne) |
| Reporting Service | 8085 | (interne) |
| Notification Service | 8086 | (interne) |
| Subscription Service | 8087 | (interne) |
| PostgreSQL | 5432 | (interne) |
| Kafka | 9092 | (interne) |
| Zookeeper | 2181 | (interne) |

## 🔐 Credentials par défaut

### Mode démo (par défaut)
```
Email:    rh@company.com
Password: secret
Tenant:   demo-lu
Role:     HR (accès complet)
```

### Autres utilisateurs de test
```
Email:    admin@company.com → Rôle ADMIN
Email:    employee@company.com → Rôle EMPLOYEE
Password: secret (pour tous)
```

## 📊 Architecture lancée

```
┌─────────────────────────────────────────────┐
│            Frontend Angular (4200)           │
└─────────────────┬───────────────────────────┘
                  │ HTTP/REST
┌─────────────────▼───────────────────────────┐
│         API Gateway (8080)                  │
│    ┌─────────────────────────────────┐      │
│    │   Config Server (8888)          │      │
│    │   Eureka Discovery (8761)       │      │
└────┼─────────────────────────────────┼──────┘
     │
     ├── User Service (8081)
     ├── Leave Service (8082)
     ├── Sickness Service (8083)
     ├── Telework Service (8084)
     ├── Reporting Service (8085)
     ├── Notification Service (8086)
     ├── Subscription Service (8087)
     │
     └─► PostgreSQL x7 (5432)
         Kafka (9092)
         Zookeeper (2181)
```

## ✅ Améliorations apportées

### Scripts existants
- ✓ Créé `launch-workrh.bat` (plus simple que `run-all.ps1`)
- ✓ Amélioré avec vérification des prérequis intégrée
- ✓ Ajouté un script d'arrêt `stop-workrh.bat`
- ✓ Corrigé l'erreur TypeScript dans `pricing-page.component.ts`
- ✓ Corrigé le composant `checkout-loading.component.ts`

### Code frontend
- ✓ Erreur `TS2367` corrigée (comparaison HttpErrorResponse/TimeoutError)
- ✓ Erreur `@Output()` corrigée (EmitEvent manquait)
- ✓ Tous les composants vérifiés et validés

### Documentation
- ✓ Guide de lancement complet
- ✓ Guide de dépannage avec 10+ solutions
- ✓ Configuration détaillée de tous les services
- ✓ Commandes utiles et diagnostics

## 🎓 Instructions d'utilisation

### Avant de lancer pour la 1ère fois

```powershell
# Vérifier les prérequis
.\check-requirements.bat

# Ou en PowerShell
.\check-requirements.ps1
```

### Lancer l'application

```cmd
# Option 1 : Batch (recommandé)
launch-workrh.bat

# Option 2 : PowerShell
.\launch-workrh.ps1
```

### Arrêter proprement

```cmd
# Option 1 : Batch
stop-workrh.bat

# Option 2 : PowerShell
.\stop-workrh.ps1
```

## 🔄 Workflow de développement

```
1. Lancer: launch-workrh.bat
2. Vérifier: http://localhost:4200
3. Modifier le code
4. Frontend: HMR (Hot Module Replacement) rechargement auto
5. Backend: Redémarrer le service concerné
6. Arrêter: stop-workrh.bat quand terminé
```

## 📝 Notes importantes

- **Ports:** Assurez-vous que les ports 4200, 8080, 8888, 8761 et 8081-8087 ne sont pas utilisés
- **Docker:** Docker Desktop doit être lancé avant d'utiliser les scripts
- **Mémoire:** Assurez-vous d'avoir au moins 4GB de RAM disponible
- **Temps de démarrage:** ~2-3 minutes pour tous les services
- **Première compilation:** Maven prend ~5-10 minutes la première fois

## 🆘 Support

Si vous rencontrez des problèmes :

1. Consultez `TROUBLESHOOTING.md`
2. Vérifiez les logs (voir `LAUNCH_CONFIG.md`)
3. Créez un issue sur GitHub
4. Contactez le support

## 📚 Documentation complète

Pour plus d'informations :
- `README.md` - Vue d'ensemble du projet
- `LAUNCH_GUIDE.md` - Guide de démarrage
- `LAUNCH_CONFIG.md` - Configuration des services
- `TROUBLESHOOTING.md` - Dépannage
- `docs/` - Documentation technique

---

**Création:** 28 mars 2026  
**Auteur:** GitHub Copilot  
**Version:** WorkRH 1.0.0
