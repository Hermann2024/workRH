export const PLAN_FEATURE_LABELS: Record<string, string> = {
  EMPLOYEE_MANAGEMENT: 'Gestion des employés',
  LEAVE_MANAGEMENT: 'Gestion des congés',
  TELEWORK_BASIC: 'Suivi simple du télétravail',
  DASHBOARD_BASIC: 'Tableau de bord RH de base',
  EMAIL_SUPPORT: 'Support email',
  SICKNESS_MANAGEMENT: 'Gestion des arrêts maladie',
  TELEWORK_COMPLIANCE_34: 'Conformité télétravail frontalier Luxembourg 34 jours',
  AUTO_EXCLUSION: 'Exclusions automatiques des jours non comptables',
  THRESHOLD_ALERTS: 'Alertes de dépassement de seuil',
  DASHBOARD_ADVANCED: 'Tableau de bord avancé',
  MONTHLY_STATS: 'Statistiques mensuelles',
  EXPORTS: 'Exports de données',
  EMAIL_NOTIFICATIONS: 'Notifications email automatiques',
  PRIORITY_SUPPORT: 'Support prioritaire',
  ADVANCED_RBAC: 'Gestion avancée des rôles et accès',
  FULL_REPORTING: 'Reporting complet',
  DECLARATION_AUDIT: 'Audit des déclarations',
  PUBLIC_API: "Demande d'intégration API",
  SMS_NOTIFICATIONS: 'Notifications SMS',
  COMPANY_BRANDING: 'Personnalisation à votre marque',
  ACCOUNTING_EXPORT: 'Exports comptables',
  SLA_SUPPORT: 'Support avec SLA',
  ONBOARDING_SUPPORT: 'Accompagnement onboarding',
  MULTI_TENANT_ADVANCED: 'Multi-tenant avancé',
  DEDICATED_HOSTING: 'Demande hébergement dédié',
  HARDENED_SECURITY: 'Demande revue sécurité renforcée',
  SSO: 'Demande projet SSO',
  CUSTOM_DEVELOPMENT: 'Demande de développement sur mesure'
};

export function toCommercialFeatureLabel(feature: string): string {
  return PLAN_FEATURE_LABELS[feature] ?? feature.replaceAll('_', ' ').toLowerCase();
}
