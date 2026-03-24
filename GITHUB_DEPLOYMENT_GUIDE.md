# Guide de Déploiement - Push vers GitHub

Date: 23 mars 2026

## 🎯 Objectif
Pousser le code WorkRH sur le repository GitHub : `https://github.com/Hermann2024/workRH.git`

## 📋 Prérequis

### **Outils Nécessaires**
- ✅ **Git** installé (`git --version`)
- ✅ **Node.js** installé (`node --version`)
- ✅ **Angular CLI** installé (`ng version`)
- ✅ **Accès GitHub** avec credentials

### **Vérifications**
```bash
# Vérifier Git
git --version

# Vérifier Node.js
node --version
npm --version

# Vérifier Angular CLI
ng version

# Vérifier le répertoire de travail
pwd
ls -la
```

## 🚀 Étapes de Déploiement

### **Étape 1 : Initialisation du Repository Local**

#### **Si pas encore initialisé :**
```bash
# Initialiser Git dans le répertoire workRH
cd /path/to/workRH
git init

# Créer .gitignore
cat > .gitignore << 'EOF'
# Dependencies
node_modules/
npm-debug.log*
yarn-debug.log*
yarn-error.log*

# Build outputs
dist/
target/
*.jar
*.war

# IDE
.vscode/
.idea/
*.swp
*.swo

# OS
.DS_Store
Thumbs.db

# Environment
.env
.env.local
.env.production

# Logs
logs/
*.log

# Runtime data
pids/
*.pid
*.seed
*.pid.lock

# Coverage directory used by tools like istanbul
coverage/

# nyc test coverage
.nyc_output/

# Dependency directories
jspm_packages/

# Optional npm cache directory
.npm

# Optional REPL history
.node_repl_history

# Output of 'npm pack'
*.tgz

# Yarn Integrity file
.yarn-integrity

# dotenv environment variables file
.env

# IDE files
.vscode/
.idea/

# OS generated files
.DS_Store
.DS_Store?
._*
.Spotlight-V100
.Trashes
ehthumbs.db
Thumbs.db
EOF

# Ajouter tous les fichiers
git add .

# Premier commit
git commit -m "Initial commit - WorkRH SaaS platform

- Angular 19 frontend with Luxembourg theme
- Spring Boot microservices backend
- Stripe payment integration
- Docker containerization
- Kubernetes deployment ready"
```

#### **Si déjà initialisé :**
```bash
# Vérifier l'état
git status

# Ajouter les changements
git add .

# Commit des améliorations récentes
git commit -m "feat: Enhanced UX for checkout process

- Added CheckoutLoadingComponent with progress animation
- Implemented timeout and retry logic for API calls
- Added contextual loading messages per plan
- Improved error handling with toast notifications
- Added cancel functionality during checkout"
```

### **Étape 2 : Configuration du Remote**

```bash
# Ajouter le remote GitHub
git remote add origin https://github.com/Hermann2024/workRH.git

# Vérifier les remotes
git remote -v

# Si remote existe déjà, le mettre à jour
git remote set-url origin https://github.com/Hermann2024/workRH.git
```

### **Étape 3 : Push Initial**

#### **Premier push sur main :**
```bash
# Créer et pousser la branche main
git branch -M main
git push -u origin main
```

#### **Si le repository existe déjà :**
```bash
# Pull d'abord pour éviter les conflits
git pull origin main --allow-unrelated-histories

# Résoudre les conflits si nécessaire
# Puis pousser
git push origin main
```

## 🔐 Gestion des Credentials

### **Méthode 1 : Token GitHub (Recommandé)**
```bash
# Générer un Personal Access Token sur GitHub :
# GitHub → Settings → Developer settings → Personal access tokens → Generate new token
# Scopes : repo, workflow

# Lors du push, utiliser le token comme mot de passe
# Username : Hermann2024
# Password : ghp_xxxxxxxxxxxxxxxxxxxx
```

### **Méthode 2 : SSH (Alternative)**
```bash
# Générer clé SSH
ssh-keygen -t ed25519 -C "your-email@example.com"

# Ajouter au ssh-agent
eval "$(ssh-agent -s)"
ssh-add ~/.ssh/id_ed25519

# Copier la clé publique
cat ~/.ssh/id_ed25519.pub

# L'ajouter sur GitHub : Settings → SSH and GPG keys → New SSH key

# Changer l'URL du remote
git remote set-url origin git@github.com:Hermann2024/workRH.git

# Push
git push origin main
```

## 📁 Structure du Repository

Après push, le repository contiendra :

```
workRH/
├── .gitignore
├── README.md
├── pom.xml                                    # Maven parent
├── docker-compose.yml                         # Docker services
├── docs/                                      # Documentation
│   ├── deployment-strategy.md
│   └── solution-architecture.md
├── frontend/
│   └── angular-app/                          # Angular 19 app
│       ├── angular.json
│       ├── package.json
│       ├── src/
│       │   ├── app/
│       │   │   ├── components/               # Composants réutilisables
│       │   │   ├── pages/                    # Pages principales
│       │   │   ├── services/                 # Services Angular
│       │   │   └── config.ts                 # Configuration
│       │   └── index.html
│       └── tsconfig.json
├── infra/
│   ├── config-repo/                          # Config Spring Cloud
│   ├── docker/                               # Dockerfiles
│   └── k8s/                                  # Kubernetes manifests
├── platform/                                 # Services communs
│   ├── api-gateway/
│   ├── common-lib/
│   ├── config-server/
│   └── discovery-service/
└── services/                                 # Services métier
    ├── leave-service/
    ├── notification-service/
    ├── reporting-service/
    ├── sickness-service/
    ├── subscription-service/
    ├── telework-service/
    └── user-service/
```

## 🏗️ Build et Test Avant Push

### **Frontend - Build de Production**
```bash
cd frontend/angular-app

# Installer les dépendances
npm install

# Build de production
npm run build --prod

# Vérifier le build
ls -la dist/
```

### **Backend - Build Maven**
```bash
# À la racine du projet
mvn clean package -DskipTests

# Vérifier les JARs
find . -name "*.jar" -type f
```

### **Tests**
```bash
# Tests frontend
cd frontend/angular-app
npm test --watch=false

# Tests backend (si configurés)
mvn test
```

## 🚀 Commandes de Push Automatisées

### **Script de déploiement complet :**
```bash
#!/bin/bash
# deploy.sh

echo "🚀 Déploiement WorkRH vers GitHub"

# Vérifications
if ! command -v git &> /dev/null; then
    echo "❌ Git n'est pas installé"
    exit 1
fi

if ! command -v node &> /dev/null; then
    echo "❌ Node.js n'est pas installé"
    exit 1
fi

# Build frontend
echo "📦 Build frontend..."
cd frontend/angular-app
npm install
npm run build --prod
cd ../..

# Build backend
echo "🔨 Build backend..."
mvn clean package -DskipTests

# Git operations
echo "📤 Push vers GitHub..."
git add .
git commit -m "feat: WorkRH SaaS platform deployment

- Angular 19 frontend with Luxembourg theme
- Spring Boot microservices with Docker
- Stripe payment integration
- Enhanced UX with loading animations
- Production builds included"
git push origin main

echo "✅ Déploiement terminé !"
echo "🔗 Repository : https://github.com/Hermann2024/workRH"
```

### **Rendre le script exécutable :**
```bash
chmod +x deploy.sh
./deploy.sh
```

## 🔍 Vérifications Post-Push

### **Sur GitHub :**
```bash
# Vérifier que tous les fichiers sont présents
# Vérifier la structure des dossiers
# Vérifier que les builds sont inclus (dist/, target/)
```

### **Actions GitHub (si configurées) :**
```yaml
# .github/workflows/ci.yml
name: CI/CD Pipeline

on:
  push:
    branches: [ main ]
  pull_request:
    branches: [ main ]

jobs:
  build:
    runs-on: ubuntu-latest
    
    steps:
    - uses: actions/checkout@v3
    
    - name: Setup Node.js
      uses: actions/setup-node@v3
      with:
        node-version: '18'
        cache: 'npm'
        cache-dependency-path: frontend/angular-app/package-lock.json
    
    - name: Setup Java
      uses: actions/setup-java@v3
      with:
        java-version: '17'
        distribution: 'temurin'
        cache: maven
    
    - name: Build Frontend
      run: |
        cd frontend/angular-app
        npm ci
        npm run build --prod
    
    - name: Build Backend
      run: mvn clean package -DskipTests
    
    - name: Run Tests
      run: |
        cd frontend/angular-app
        npm test --watch=false
        cd ../..
        mvn test
```

## 🐛 Résolution des Problèmes

### **Erreur "Repository not found"**
```bash
# Vérifier l'URL
git remote -v

# Vérifier les credentials
git config --global user.name "Your Name"
git config --global user.email "your.email@example.com"
```

### **Erreur "Permission denied"**
```bash
# Utiliser un Personal Access Token
# Générer sur : https://github.com/settings/tokens
# Utiliser comme mot de passe lors du push
```

### **Conflits de merge**
```bash
# Pull avec stratégie
git pull origin main --allow-unrelated-histories

# Résoudre les conflits manuellement
# Puis commit et push
```

### **Fichiers volumineux**
```bash
# Vérifier la taille des fichiers
find . -size +50M

# Ajouter au .gitignore si nécessaire
echo "*.log" >> .gitignore
echo "dist/" >> .gitignore
echo "target/" >> .gitignore
```

## 📊 Métriques de Succès

Après déploiement réussi :
- ✅ **Repository visible** sur GitHub
- ✅ **Structure claire** et organisée
- ✅ **README.md** informatif
- ✅ **CI/CD pipeline** fonctionnelle
- ✅ **Builds automatiques** réussis
- ✅ **Tests passant** automatiquement

---

**🎉 Repository déployé avec succès !**

**URL finale :** `https://github.com/Hermann2024/workRH.git`

**Prochaines étapes :**
1. Configurer GitHub Actions pour CI/CD
2. Ajouter des badges de statut
3. Créer des releases
4. Documenter l'API avec Swagger
5. Préparer le déploiement en production
