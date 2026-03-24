#!/bin/bash
# WorkRH Deployment Script
# Push to GitHub repository: https://github.com/Hermann2024/workRH.git

set -e  # Exit on any error

echo "🚀 WorkRH - Déploiement vers GitHub"
echo "=================================="

# Colors for output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m' # No Color

# Function to print colored output
print_status() {
    echo -e "${BLUE}[INFO]${NC} $1"
}

print_success() {
    echo -e "${GREEN}[SUCCESS]${NC} $1"
}

print_warning() {
    echo -e "${YELLOW}[WARNING]${NC} $1"
}

print_error() {
    echo -e "${RED}[ERROR]${NC} $1"
}

# Check prerequisites
check_prerequisites() {
    print_status "Vérification des prérequis..."

    # Check Git
    if ! command -v git &> /dev/null; then
        print_error "Git n'est pas installé. Veuillez installer Git."
        exit 1
    fi
    print_success "Git trouvé: $(git --version)"

    # Check Node.js
    if ! command -v node &> /dev/null; then
        print_error "Node.js n'est pas installé. Veuillez installer Node.js."
        exit 1
    fi
    print_success "Node.js trouvé: $(node --version)"

    # Check npm
    if ! command -v npm &> /dev/null; then
        print_error "npm n'est pas installé. Veuillez installer npm."
        exit 1
    fi
    print_success "npm trouvé: $(npm --version)"

    # Check Angular CLI
    if ! command -v ng &> /dev/null; then
        print_warning "Angular CLI n'est pas installé globalement. Installation..."
        npm install -g @angular/cli
    fi
    print_success "Angular CLI trouvé: $(ng version | head -n 2 | tail -n 1)"
}

# Initialize Git repository if needed
init_git_repo() {
    if [ ! -d ".git" ]; then
        print_status "Initialisation du repository Git..."
        git init

        # Create .gitignore
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

        print_success "Repository Git initialisé avec .gitignore"
    else
        print_success "Repository Git déjà initialisé"
    fi
}

# Build frontend
build_frontend() {
    print_status "Construction du frontend Angular..."

    cd frontend/angular-app

    # Install dependencies
    print_status "Installation des dépendances npm..."
    npm ci

    # Build production
    print_status "Build de production..."
    npm run build --prod

    # Check build output
    if [ -d "dist" ]; then
        print_success "Frontend build réussi - $(du -sh dist | cut -f1) généré"
    else
        print_error "Échec du build frontend"
        exit 1
    fi

    cd ../..
}

# Build backend
build_backend() {
    print_status "Construction du backend Spring Boot..."

    # Check if Maven wrapper exists
    if [ -f "mvnw" ]; then
        print_status "Utilisation de Maven wrapper..."
        ./mvnw clean package -DskipTests
    elif command -v mvn &> /dev/null; then
        print_status "Utilisation de Maven système..."
        mvn clean package -DskipTests
    else
        print_warning "Maven non trouvé - skip du build backend"
        return
    fi

    # Check JAR files
    JAR_COUNT=$(find . -name "*.jar" -type f | wc -l)
    if [ "$JAR_COUNT" -gt 0 ]; then
        print_success "Backend build réussi - $JAR_COUNT JAR(s) généré(s)"
    else
        print_warning "Aucun JAR trouvé - vérifiez la configuration Maven"
    fi
}

# Setup Git remote
setup_git_remote() {
    print_status "Configuration du remote GitHub..."

    # Check if remote already exists
    if git remote get-url origin &> /dev/null; then
        CURRENT_REMOTE=$(git remote get-url origin)
        if [ "$CURRENT_REMOTE" != "https://github.com/Hermann2024/workRH.git" ]; then
            print_warning "Remote existant différent. Mise à jour..."
            git remote set-url origin https://github.com/Hermann2024/workRH.git
        fi
    else
        git remote add origin https://github.com/Hermann2024/workRH.git
    fi

    print_success "Remote GitHub configuré: $(git remote get-url origin)"
}

# Commit and push
commit_and_push() {
    print_status "Commit et push vers GitHub..."

    # Add all files
    git add .

    # Check if there are changes to commit
    if git diff --cached --quiet; then
        print_warning "Aucun changement à commiter"
        return
    fi

    # Create commit with detailed message
    COMMIT_MESSAGE="feat: WorkRH SaaS platform deployment

🎯 Core Features:
- Angular 19 frontend with Luxembourg theme & Poppins font
- Spring Boot microservices with Docker containerization
- Stripe payment integration with enhanced UX
- Multi-tenant architecture with JWT authentication
- Telework compliance 34j for Luxembourg

🛠️ Technical Stack:
- Frontend: Angular 19, TypeScript, RxJS, Signals
- Backend: Java 17, Spring Boot 3.3.2, Spring Cloud
- Database: PostgreSQL per service
- Infrastructure: Docker, Kubernetes, Kafka
- Security: JWT, Spring Security, PCI DSS compliance

🎨 UX Enhancements:
- CheckoutLoadingComponent with progress animation
- Contextual loading messages per subscription plan
- Timeout & retry logic for API resilience
- Toast notifications for error handling
- Harmonized design system with CSS variables

📊 Business Features:
- 4 subscription plans (Starter €49 → Enterprise custom)
- Stripe Checkout with trial periods
- Billing management with upgrade/downgrade
- Multi-language support (FR/EN)
- Responsive design for all devices

🚀 Deployment Ready:
- Docker Compose for local development
- Kubernetes manifests for production
- CI/CD pipeline with GitHub Actions
- Environment configuration with Spring Cloud Config
- Monitoring setup with Spring Actuator"

    git commit -m "$COMMIT_MESSAGE"

    # Push to main branch
    print_status "Push vers GitHub..."
    if git push -u origin main; then
        print_success "Push réussi !"
        print_success "Repository: https://github.com/Hermann2024/workRH"
    else
        print_error "Échec du push. Vérifiez vos credentials GitHub."
        print_status "Astuces:"
        print_status "1. Créez un Personal Access Token: https://github.com/settings/tokens"
        print_status "2. Utilisez votre username GitHub comme nom d'utilisateur"
        print_status "3. Utilisez le token comme mot de passe"
        exit 1
    fi
}

# Main execution
main() {
    echo ""
    print_status "Démarrage du déploiement WorkRH vers GitHub"
    echo ""

    check_prerequisites
    echo ""

    init_git_repo
    echo ""

    build_frontend
    echo ""

    build_backend
    echo ""

    setup_git_remote
    echo ""

    commit_and_push
    echo ""

    print_success "🎉 Déploiement terminé avec succès !"
    print_success "🔗 Repository: https://github.com/Hermann2024/workRH"
    print_success "📊 Métriques:"
    print_success "   - Frontend: $(du -sh frontend/angular-app/dist 2>/dev/null | cut -f1 || echo 'N/A')"
    print_success "   - Backend JARs: $(find . -name "*.jar" -type f 2>/dev/null | wc -l || echo '0') fichier(s)"
    print_success "   - Commits: $(git rev-list --count HEAD 2>/dev/null || echo '0')"
    echo ""
}

# Run main function
main "$@"