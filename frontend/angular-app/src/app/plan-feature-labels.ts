export const PLAN_FEATURE_LABELS: Record<string, string> = {
  EMPLOYEE_MANAGEMENT: 'Gestion des employes',
  LEAVE_MANAGEMENT: 'Gestion des conges',
  TELEWORK_BASIC: 'Suivi simple du teletravail',
  DASHBOARD_BASIC: 'Tableau de bord RH de base',
  EMAIL_SUPPORT: 'Support email',
  SICKNESS_MANAGEMENT: 'Gestion des arrets maladie',
  TELEWORK_COMPLIANCE_34: 'Conformite teletravail frontalier Luxembourg 34 jours',
  AUTO_EXCLUSION: 'Exclusions automatiques des jours non comptables',
  THRESHOLD_ALERTS: 'Alertes de depassement de seuil',
  DASHBOARD_ADVANCED: 'Tableau de bord avance',
  MONTHLY_STATS: 'Statistiques mensuelles',
  EXPORTS: 'Exports de données',
  EMAIL_NOTIFICATIONS: 'Notifications email automatiques',
  PRIORITY_SUPPORT: 'Support prioritaire',
  ADVANCED_RBAC: 'Gestion avancée des rôles et accès',
  FULL_REPORTING: 'Reporting complet',
  DECLARATION_AUDIT: 'Audit des declarations',
  PUBLIC_API: "Demande d'integration API",
  SMS_NOTIFICATIONS: 'Notifications SMS',
  COMPANY_BRANDING: 'Personnalisation a votre marque',
  ACCOUNTING_EXPORT: 'Exports comptables',
  SLA_SUPPORT: 'Support avec SLA',
  ONBOARDING_SUPPORT: 'Accompagnement onboarding',
  MULTI_TENANT_ADVANCED: 'Multi-tenant avance',
  DEDICATED_HOSTING: 'Demande hebergement dedie',
  HARDENED_SECURITY: 'Demande revue securite renforcee',
  SSO: 'Demande projet SSO',
  CUSTOM_DEVELOPMENT: 'Demande de developpement sur mesure'
};

export function toCommercialFeatureLabel(feature: string): string {
  return PLAN_FEATURE_LABELS[feature] ?? feature.replaceAll('_', ' ').toLowerCase();
}
