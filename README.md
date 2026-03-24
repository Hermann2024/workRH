# WorkRH - SaaS Platform for HR Management

[![License: MIT](https://img.shields.io/badge/License-MIT-yellow.svg)](https://opensource.org/licenses/MIT)
[![Angular](https://img.shields.io/badge/Angular-19-red.svg)](https://angular.io/)
[![Spring Boot](https://img.shields.io/badge/Spring%20Boot-3.3.2-brightgreen.svg)](https://spring.io/projects/spring-boot)
[![Docker](https://img.shields.io/badge/Docker-Ready-blue.svg)](https://www.docker.com/)
[![Kubernetes](https://img.shields.io/badge/Kubernetes-Ready-326ce5.svg)](https://kubernetes.io/)

> **WorkRH** is a comprehensive SaaS platform designed for HR management with specialized focus on telework compliance in Luxembourg. Built with modern technologies and enterprise-grade architecture.

## 🎯 Overview

WorkRH addresses the complex challenges of HR management for companies employing workers in cross-border situations, particularly in Luxembourg. The platform combines regulatory compliance, HR automation, and modern user experience.

### ✨ Key Features

- **🏛️ Luxembourg Compliance**: Specialized telework regulation (34-day rule)
- **💳 Stripe Integration**: Secure payment processing with trial periods
- **🏢 Multi-tenant**: Isolated company environments
- **🔐 Enterprise Security**: JWT authentication, PCI DSS compliance
- **🎨 Modern UX**: Angular 19 with harmonized design system
- **🐳 Container Ready**: Docker + Kubernetes deployment
- **📊 Real-time Analytics**: Dashboard with HR metrics
- **🌐 Multi-language**: French/English support

## 🏗️ Architecture

### Microservices Architecture
```
┌─────────────────┐    ┌─────────────────┐    ┌─────────────────┐
│   API Gateway   │    │   Config Server │    │  Discovery      │
│   (Spring GW)   │    │   (Spring Cloud)│    │  (Eureka)       │
└─────────────────┘    └─────────────────┘    └─────────────────┘
         │                       │                       │
         └───────────────────────┼───────────────────────┘
                                 │
                    ┌─────────────────────┐
                    │   Business Services │
                    │                     │
                    │  ┌────────────────┐ │
                    │  │ user-service   │ │
                    │  │ leave-service  │ │
                    │  │ sickness-svc   │ │
                    │  │ telework-svc   │ │
                    │  │ reporting-svc  │ │
                    │  │ notification-svc│ │
                    │  │ subscription-svc│ │
                    │  └────────────────┘ │
                    └─────────────────────┘
```

### Technology Stack

| Component | Technology | Version |
|-----------|------------|---------|
| **Frontend** | Angular | 19.0 |
| **Backend** | Spring Boot | 3.3.2 |
| **Language** | Java | 17 |
| **Database** | PostgreSQL | 15+ |
| **Message Queue** | Apache Kafka | 3.6+ |
| **Container** | Docker | 24+ |
| **Orchestration** | Kubernetes | 1.28+ |
| **API Gateway** | Spring Cloud Gateway | 4.1.3 |
| **Service Discovery** | Netflix Eureka | 4.0.3 |
| **Config Management** | Spring Cloud Config | 4.1.2 |
| **Security** | Spring Security | 6.3+ |
| **Payment** | Stripe | Latest |

## 🚀 Quick Start

### Prerequisites
- **Java**: 17+ (`java -version`)
- **Node.js**: 18+ (`node -version`)
- **Docker**: 24+ (`docker --version`)
- **Docker Compose**: (`docker-compose --version`)
- **Git**: (`git --version`)

### Local Development Setup

1. **Clone the repository**
   ```bash
   git clone https://github.com/Hermann2024/workRH.git
   cd workRH
   ```

2. **Start infrastructure with Docker Compose**
   ```bash
   docker-compose up -d postgres kafka zookeeper
   ```

3. **Configure environment**
   ```bash
   # Copy environment files
   cp infra/config-repo/application-local.yml infra/config-repo/application.yml
   ```

4. **Build and run backend services**
   ```bash
   # Build all services
   mvn clean package -DskipTests

   # Run services (in separate terminals)
   java -jar platform/config-server/target/config-server-*.jar
   java -jar platform/discovery-service/target/discovery-service-*.jar
   java -jar platform/api-gateway/target/api-gateway-*.jar
   # ... run other services
   ```

5. **Build and run frontend**
   ```bash
   cd frontend/angular-app
   npm install
   npm start
   ```

6. **Access the application**
   - **Frontend**: http://localhost:4200
   - **API Gateway**: http://localhost:8080
   - **Config Server**: http://localhost:8888
   - **Eureka Dashboard**: http://localhost:8761

## 💰 Subscription Plans

| Plan | Monthly | Annual | Features |
|------|---------|--------|----------|
| **Starter** | €49 | €490 | Basic HR management |
| **Pro** ⭐ | €99 | €990 | + Telework compliance 34j |
| **Premium** | €199 | €1,990 | + Advanced reporting + API |
| **Enterprise** | Custom | Custom | + Custom features + SLA |

- ✅ 14-day free trial
- ✅ No setup fees
- ✅ Cancel anytime
- ✅ Secure Stripe payments

## 🎨 Design System

### Color Palette (Luxembourg Flag)
```css
--blue: #002395;      /* Primary */
--accent: #EF3340;     /* Secondary */
--white: #ffffff;     /* Background */
--ink: #0d1117;       /* Text */
```

### Typography
- **Font Family**: Poppins (Google Fonts)
- **Weights**: 400, 500, 600, 700
- **Line Heights**: 1.4, 1.6, 1.8

### Components
- **Harmonized spacing**: 8px base grid
- **Smooth animations**: 0.15s - 0.3s transitions
- **Responsive design**: Mobile-first approach
- **Accessibility**: WCAG AA compliant

## 📊 Business Features

### HR Management
- ✅ **Employee CRUD**: Complete lifecycle management
- ✅ **Leave Management**: Automated approval workflows
- ✅ **Sickness Tracking**: Medical leave monitoring
- ✅ **Telework Compliance**: 34-day rule enforcement

### Analytics & Reporting
- ✅ **Real-time Dashboard**: HR metrics visualization
- ✅ **Custom Reports**: PDF/Excel/CSV exports
- ✅ **Compliance Monitoring**: Regulatory adherence tracking
- ✅ **Usage Analytics**: Platform utilization insights

### Integration & API
- ✅ **RESTful API**: Complete platform API
- ✅ **Webhook Support**: Real-time event notifications
- ✅ **Third-party Integration**: HR systems compatibility
- ✅ **API Documentation**: Swagger/OpenAPI specs

## 🔒 Security & Compliance

### Authentication & Authorization
- **JWT Tokens**: Secure stateless authentication
- **Role-based Access**: ADMIN, HR, EMPLOYEE roles
- **Multi-tenant Isolation**: Company data separation
- **Session Management**: Secure token lifecycle

### Data Protection
- **Encryption**: Data at rest and in transit
- **GDPR Compliance**: Data portability and deletion
- **Audit Logging**: Complete action traceability
- **Backup & Recovery**: Automated data protection

### Payment Security
- **Stripe PCI DSS**: Level 1 compliance
- **3D Secure**: Enhanced payment authentication
- **Fraud Detection**: Advanced risk assessment
- **SCA Compliance**: Strong Customer Authentication

## 🚀 Deployment

### Automated Deployment
```bash
# Run the deployment script
chmod +x deploy.sh
./deploy.sh
```

### Manual Deployment Steps
1. **Build frontend**: `npm run build --prod`
2. **Build backend**: `mvn clean package`
3. **Create Docker images**: `docker build`
4. **Deploy to Kubernetes**: `kubectl apply -f k8s/`
5. **Configure ingress**: Load balancer setup

### Environment Configuration
```yaml
# application-prod.yml
spring:
  profiles:
    active: prod
  datasource:
    url: jdbc:postgresql://prod-db:5432/workrh
stripe:
  secret-key: ${STRIPE_SECRET_KEY}
  webhook-secret: ${STRIPE_WEBHOOK_SECRET}
```

## 🧪 Testing

### Frontend Tests
```bash
cd frontend/angular-app
npm test                    # Unit tests
npm run test:e2e          # End-to-end tests
npm run lint              # Code linting
```

### Backend Tests
```bash
mvn test                   # Unit tests
mvn integration-test      # Integration tests
mvn verify               # Full test suite
```

### Performance Testing
```bash
# Load testing with JMeter
jmeter -n -t workrh-performance-test.jmx

# API testing with Postman/Newman
newman run workrh-api-tests.postman_collection.json
```

## 📈 Monitoring & Observability

### Application Metrics
- **Spring Actuator**: Health checks and metrics
- **Micrometer**: Performance monitoring
- **Prometheus**: Metrics collection
- **Grafana**: Visualization dashboards

### Infrastructure Monitoring
- **Kubernetes**: Pod health and resource usage
- **Docker**: Container performance
- **ELK Stack**: Log aggregation and analysis
- **Alert Manager**: Automated alerting

## 🤝 Contributing

### Development Workflow
1. **Fork** the repository
2. **Create** a feature branch (`git checkout -b feature/amazing-feature`)
3. **Commit** your changes (`git commit -m 'Add amazing feature'`)
4. **Push** to the branch (`git push origin feature/amazing-feature`)
5. **Open** a Pull Request

### Code Standards
- **Frontend**: ESLint + Prettier configuration
- **Backend**: Spring Boot coding standards
- **Testing**: 80%+ code coverage required
- **Documentation**: JSDoc/JavaDoc for public APIs

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 📞 Support

- **Documentation**: [Wiki](https://github.com/Hermann2024/workRH/wiki)
- **Issues**: [GitHub Issues](https://github.com/Hermann2024/workRH/issues)
- **Discussions**: [GitHub Discussions](https://github.com/Hermann2024/workRH/discussions)
- **Email**: support@workrh.com

## 🙏 Acknowledgments

- **Angular Team** for the amazing framework
- **Spring Team** for the robust backend ecosystem
- **Stripe** for secure payment processing
- **Docker & Kubernetes** communities
- **Open source contributors**

---

**🇱🇺 Built with ❤️ for Luxembourg's HR community 🇱🇺**

**🌟 Star this repository if you find it useful!**

[🚀 Get Started](#quick-start) | [📚 Documentation](docs/) | [🔧 API Reference](api/)
