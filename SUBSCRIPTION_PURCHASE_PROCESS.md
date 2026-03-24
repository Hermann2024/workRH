# Processus d'Achat d'Abonnement WorkRH

Date: 23 mars 2026

## 🎯 Vue d'Ensemble

Le processus d'achat d'abonnement WorkRH est entièrement intégré avec **Stripe Checkout** pour une expérience de paiement sécurisée et fluide. Le système supporte les achats initiaux, les upgrades/downgrades, et la gestion complète du cycle de vie des abonnements.

## 📋 Étapes du Processus d'Achat

### **1. Découverte & Sélection (Pricing Page)**

#### **Accès à la Page Tarifs**
- **URL**: `/pricing`
- **Accès**: Public (pas besoin d'être connecté)
- **Contenu**: Catalogue des 4 plans (Starter, Pro, Premium, Enterprise)

#### **Plans Disponibles**
```typescript
interface Plan {
  code: 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';
  name: string;
  monthlyPrice: number;
  minEmployees: number | null;
  maxEmployees: number | null;
  features: string[];
  recommended: boolean;
}
```

#### **Fonctionnalités par Plan**
- **Starter**: Gestion employés + congés
- **Pro**: + Télétravail 34j + Alertes + Exports
- **Premium**: + Reporting avancé + API + SMS
- **Enterprise**: Tout + Custom + Support dédié

### **2. Initiation du Checkout**

#### **Clic "Choisir ce plan"**
```typescript
startCheckout(planCode: string): void {
  // Vérification authentification
  const session = this.authService.session();
  if (!session) {
    this.router.navigateByUrl('/login');
    return;
  }

  // Création session Stripe
  this.checkoutService.createCheckout({
    planCode,
    seatsPurchased: 25, // Défaut
    paymentMethodTypes: ['card', 'sepa_debit'],
    smsOptionEnabled: false,
    advancedAuditOptionEnabled: false,
    advancedExportOptionEnabled: planCode === 'PRO' || planCode === 'PREMIUM',
    customerEmail: session.email,
    successUrl: `${FRONTEND_BASE_URL}/dashboard?checkout=success`,
    cancelUrl: `${FRONTEND_BASE_URL}/pricing?checkout=cancelled`
  });
}
```

#### **Paramètres du Checkout**
- **planCode**: Code du plan sélectionné
- **seatsPurchased**: Nombre d'utilisateurs (défaut 25)
- **paymentMethodTypes**: ['card', 'sepa_debit']
- **Options**: SMS, Audit, Exports (selon plan)
- **URLs**: Success/Cancel avec paramètres

### **3. Redirection vers Stripe Checkout**

#### **API Backend** (`/api/subscriptions/checkout/stripe`)
```java
@PostMapping("/checkout/stripe")
public StripeCheckoutResponse createStripeCheckout(@RequestBody StripeCheckoutRequest request) {
    // 1. Création session Stripe
    SessionCreateParams params = SessionCreateParams.builder()
        .setMode(SessionCreateParams.Mode.SUBSCRIPTION)
        .setCustomerEmail(request.getCustomerEmail())
        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.CARD)
        .addPaymentMethodType(SessionCreateParams.PaymentMethodType.SEPA_DEBIT)
        .setSuccessUrl(request.getSuccessUrl())
        .setCancelUrl(request.getCancelUrl())
        .addLineItem(createLineItem(request))
        .build();

    Session session = Session.create(params);

    // 2. Sauvegarde en base
    saveCheckoutSession(session.getId(), request);

    return new StripeCheckoutResponse(session.getId(), session.getUrl());
}
```

#### **Session Stripe Créée**
- **Mode**: SUBSCRIPTION (récurrent)
- **Paiement**: Carte + SEPA
- **URLs**: Callbacks configurées
- **Line Items**: Plan sélectionné

### **4. Expérience Stripe Checkout**

#### **Page Hébergée par Stripe**
- **Sécurisée**: PCI DSS compliant
- **Responsive**: Mobile-friendly
- **Multi-langues**: Support français/anglais
- **Options**: Carte bancaire ou prélèvement SEPA

#### **Informations Collectées**
- **Paiement**: Carte ou IBAN
- **Facturation**: Adresse, TVA (si applicable)
- **Trial**: 14 jours gratuit automatiquement

#### **Validation**
- **3D Secure**: Authentification forte
- **SCA**: Strong Customer Authentication
- **Anti-fraude**: Stripe Radar

### **5. Confirmation & Activation**

#### **Callback Success** (`/dashboard?checkout=success`)
```typescript
// Dans billing-page.component.ts
ngOnInit() {
  const checkoutParam = this.route.snapshot.queryParamMap.get('checkout');
  if (checkoutParam === 'success' || checkoutParam === 'cancelled') {
    this.checkoutState.set(checkoutParam);
  }
}
```

#### **Webhook Stripe** (Backend)
```java
@PostMapping("/webhooks/stripe")
public void handleStripeWebhook(@RequestBody String payload) {
    Event event = Webhook.constructEvent(payload, signature, endpointSecret);

    switch (event.getType()) {
        case "checkout.session.completed":
            activateSubscription(event.getData().getObject());
            break;
        case "invoice.payment_succeeded":
            handlePaymentSuccess(event.getData().getObject());
            break;
        case "invoice.payment_failed":
            handlePaymentFailure(event.getData().getObject());
            break;
    }
}
```

#### **Activation Abonnement**
- **Statut**: ACTIVE
- **Trial**: 14 jours démarrés
- **Entitlements**: Fonctionnalités débloquées
- **Email**: Confirmation envoyée

### **6. Gestion Post-Achat**

#### **Dashboard avec Bannière**
```html
<div class="checkout-banner success" *ngIf="checkoutState() === 'success'">
  Checkout terminé. Stripe mettra à jour l'abonnement après confirmation du paiement.
</div>
```

#### **Billing Page** (`/billing`)
- **État abonnement**: Actif/Trial/Cancelled
- **Prochaine échéance**: Date renouvellement
- **Historique paiements**: Via Stripe
- **Actions**: Upgrade/Downgrade/Cancel

## 🔄 Gestion du Cycle de Vie

### **Upgrades/Downgrades**
```typescript
upgradeTo(planCode: string): void {
  this.lifecycleService.upgrade({
    targetPlanCode: planCode,
    seatsPurchased: current.seatsPurchased,
    // Options préservées
  }).subscribe(response => {
    this.patchSubscription(response);
  });
}
```

#### **API Backend**
```java
@PatchMapping("/current/upgrade")
public Subscription upgrade(@RequestBody SubscriptionChangeRequest request) {
    // 1. Validation éligibilité
    // 2. Calcul prorata
    // 3. Update Stripe
    // 4. Update base de données
    // 5. Notification
}
```

### **Annulation**
```typescript
cancelSubscription(): void {
  this.lifecycleService.cancel({
    reason: 'Cancelled from billing page'
  });
}
```

#### **Options d'Annulation**
- **Immediate**: Fin immédiate
- **End of Period**: Fin de période en cours
- **Reason**: Motif enregistré

### **Réactivation**
```typescript
reactivateSubscription(): void {
  this.lifecycleService.reactivate();
}
```

## 💳 Méthodes de Paiement

### **Carte Bancaire**
- **Visa, Mastercard, Amex**
- **3D Secure** obligatoire
- **Paiement immédiat** ou **autorisation**

### **Prélèvement SEPA**
- **IBAN** européen
- **Mandat signé** automatiquement
- **Délai**: 3-5 jours ouvrés

### **Facturation**
- **Devise**: EUR
- **TVA**: Selon pays (Luxembourg: 17%)
- **Factures**: PDF via Stripe/email

## 🎯 États d'Abonnement

### **Cycle de Vie**
```
Lead → Trial (14j) → Active → Past Due → Cancelled
    ↓         ↓         ↓         ↓
 Pricing → Checkout → Payment → Retry → Churn
```

### **Statuts Détaillés**
- **TRIAL**: 14 jours gratuit
- **ACTIVE**: Paiement réussi, accès complet
- **PAST_DUE**: Paiement échoué, accès limité
- **CANCELLED**: Annulé, accès révoqué
- **UNPAID**: Non payé, accès suspendu

## 🔐 Sécurité & Conformité

### **Sécurité Stripe**
- **PCI DSS Level 1**
- **Chiffrement end-to-end**
- **Tokenisation** des données sensibles
- **SCA** (Strong Customer Authentication)

### **Conformité RGPD**
- **Données minimales** collectées
- **Consentement** explicite
- **Droit oubli** supporté
- **Portabilité** des données

### **Audit & Logs**
- **Toutes actions** tracées
- **Paiements** loggés
- **Changements** audités
- **Compliance** vérifiable

## 📊 Métriques & Analytics

### **Conversion Funnel**
```
Visitors → Pricing → Checkout → Payment → Activation
   100%    →  20%   →   15%   →   12%   →    10%
```

### **KPIs Clés**
- **Conversion Rate**: Visitors → Paid
- **Trial-to-Paid**: % trials convertis
- **Churn Rate**: % abonnements perdus
- **MRR Growth**: Croissance revenu mensuel
- **LTV**: Lifetime Value par client

## 🚨 Gestion des Erreurs

### **Échecs de Paiement**
- **Retry automatique** (3 tentatives)
- **Email notification** à chaque échec
- **Grace period** 7 jours
- **Account suspension** après échec

### **Annulations**
- **Confirmation** requise
- **Reason tracking** pour analytics
- **Data retention** 30 jours
- **Reactivation** possible

### **Refunds**
- **Politique**: 14 jours trial
- **Partial refunds** pour prorata
- **Stripe dashboard** pour gestion

## 🎉 Expérience Utilisateur

### **Onboarding Fluide**
1. **Inscription** → Création compte
2. **Sélection plan** → Checkout Stripe
3. **Paiement** → Activation immédiate
4. **Welcome email** → Guide démarrage
5. **Dashboard** → Accès complet

### **Support Intégré**
- **Chat en ligne** (Pro+)
- **Email support** (tous plans)
- **Knowledge base** intégrée
- **Webinars** mensuels

---

**Processus d'achat WorkRH** : Sécurisé, fluide et entièrement automatisé avec Stripe Checkout 🇱🇺💳
