# Processus d'Achat WorkRH - Résumé Visuel

## 🎯 Flow d'Achat Complet

```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Visitor   │ -> │   Pricing   │ -> │  Checkout   │ -> │   Stripe    │
│             │    │   Page      │    │   Session   │    │   Hosted    │
│             │    │  /pricing   │    │   Created   │    │   Page      │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Login     │    │   Select    │    │   API Call  │    │   Payment   │
│   Required  │    │   Plan      │    │   Backend   │    │   Methods   │
│             │    │             │    │             │    │             │
│             │    │  Starter    │    │ POST /api/  │    │  💳 Card    │
│             │    │  Pro ⭐     │    │ subscriptions│    │  🏦 SEPA   │
│             │    │  Premium    │    │ /checkout/  │    │             │
│             │    │  Enterprise │    │ stripe      │    │             │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
                                                       │
                                                       ▼
┌─────────────────────────────────────────────────────────────┐
│                    STRIPE CHECKOUT                          │
│  ┌─────────────────────────────────────────────────────┐    │
│  │ 1. Email pré-rempli (user session)                 │    │
│  │ 2. Plan sélectionné affiché                        │    │
│  │ 3. Prix mensuel + Trial 14j                        │    │
│  │ 4. Options: SMS, Audit, Exports                    │    │
│  │ 5. Paiement: Card/SEPA                             │    │
│  │ 6. 3D Secure + SCA                                 │    │
│  └─────────────────────────────────────────────────────┘    │
└─────────────────────────────────────────────────────────────┘
                               │
                               ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Success   │ -> │   Webhook   │ -> │ Activation  │ -> │   Email     │
│   Callback  │    │   Stripe    │    │   Account   │    │ Confirmation│
│             │    │             │    │             │    │             │
│ /dashboard? │    │ invoice.    │    │ Status:     │    │ Welcome +   │
│ checkout=   │    │ payment_    │    │ ACTIVE      │    │ Guide       │
│ success     │    │ succeeded   │    │             │    │             │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │                   │
       ▼                   ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Banner    │    │   Trial     │    │ Entitle-   │    │   Billing   │
│   Success   │    │   14 Days   │    │ ments      │    │   Page      │
│   Shown     │    │   Started   │    │ Unlocked   │    │   Access     │
└─────────────┘    └─────────────┘    └─────────────┘    └─────────────┘
```

## 💰 Détails Tarifaires

### **Plans & Prix**
| Plan | Prix/Mois | Employés | Fonctionnalités Clés |
|------|-----------|----------|---------------------|
| **Starter** | 49€ | 1-10 | Gestion RH basique |
| **Pro** ⭐ | 99€ | 10-50 | Télétravail 34j + Alertes |
| **Premium** | 199€ | 50+ | Reporting + API + SMS |
| **Enterprise** | Sur devis | Illimité | Tout + Custom |

### **Options Add-on**
- **SMS Notifications**: +€15/mois
- **Advanced Audit**: +€25/mois
- **Advanced Exports**: +€10/mois

## 🔄 Gestion Abonnement

### **Actions Disponibles**
```
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Upgrade   │ -> │   Stripe    │ -> │ Prorata     │
│   Plan      │    │   Update    │    │   Billing   │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│ Downgrade   │ -> │   Stripe    │ -> │ Credit      │
│   Plan      │    │   Update    │    │   Applied   │
└─────────────┘    └─────────────┘    └─────────────┘
       │                   │                   │
       ▼                   ▼                   ▼
┌─────────────┐    ┌─────────────┐    ┌─────────────┐
│   Cancel    │ -> │ End of      │ -> │ Access      │
│ Subscription│    │ Period      │    │ Revoked     │
└─────────────┘    └─────────────┘    └─────────────┘
```

### **États Abonnement**
- **TRIAL**: 14 jours gratuit
- **ACTIVE**: Paiement OK, accès complet
- **PAST_DUE**: Paiement échoué, accès limité
- **CANCELLED**: Annulé, accès révoqué

## 🔐 Sécurité & Paiement

### **Méthodes Acceptées**
- **💳 Cartes**: Visa, Mastercard, Amex
- **🏦 SEPA**: Prélèvement IBAN européen

### **Sécurité Stripe**
- ✅ **PCI DSS Level 1**
- ✅ **3D Secure** obligatoire
- ✅ **SCA** (Strong Customer Auth)
- ✅ **Anti-fraude** Radar

### **Conformité**
- ✅ **RGPD** compliant
- ✅ **Données minimales**
- ✅ **Droit à l'oubli**
- ✅ **Portabilité**

## 📧 Communications

### **Emails Automatiques**
1. **Confirmation achat** → Immédiat
2. **Activation compte** → Après paiement
3. **Guide démarrage** → Avec accès
4. **Rappels échéance** → 7j avant
5. **Échec paiement** → Retry + notification
6. **Annulation** → Confirmation

### **Notifications In-App**
- **Bannière succès** → Checkout terminé
- **Alertes trial** → Jours restants
- **Notifications usage** → Limites approchées

## 🎯 KPIs & Métriques

### **Funnel Conversion**
```
Visitors (100%) → Pricing (20%) → Checkout (15%) → Payment (12%) → Active (10%)
```

### **Métriques Clés**
- **Trial-to-Paid**: 70% (objectif)
- **Churn Rate**: <5%
- **MRR Growth**: +€5k/mois
- **CAC Payback**: <12 mois

## 🚨 Gestion des Cas Limites

### **Échec Paiement**
```
Paiement échoué → Email notification → Retry automatique (3x) → 
Grace period (7j) → Suspension accès → Annulation
```

### **Annulation**
```
Demande annulation → Confirmation email → 
Choix immédiat/fin période → Désactivation → 
Data retention (30j) → Possibilité réactivation
```

### **Upgrade/Downgrade**
```
Sélection nouveau plan → Calcul prorata → 
Update Stripe → Confirmation email → 
Nouvelles fonctionnalités activées
```

---

**Processus d'achat WorkRH** : Sécurisé, automatisé et user-friendly avec Stripe Checkout ! 🇱🇺💳✨
