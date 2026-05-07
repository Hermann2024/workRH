export type CommercialPlanCode = 'STARTER' | 'PRO' | 'PREMIUM' | 'ENTERPRISE';

export interface PlanCommercialContent {
  headline: string;
  idealFor: string;
  businessValue: string[];
  serviceBundle: string[];
  addOns: string[];
}

export const PLAN_COMMERCIAL_CONTENT: Record<CommercialPlanCode, PlanCommercialContent> = {
  STARTER: {
    headline: 'Le socle RH pour lancer votre organisation sans complexité inutile.',
    idealFor: 'TPE, cabinets et petites équipes qui veulent centraliser les demandes RH et tracer le télétravail simplement.',
    businessValue: [
      'Dossier salarié, congés et premiers workflows dans un même outil.',
      "Suivi du télétravail utile pour démarrer avant d'activer la couche conformité avancée.",
      'Coût maîtrisé pour une équipe de 1 à 15 collaborateurs.'
    ],
    serviceBundle: [
      'Mise en route rapide sans paramétrage lourd.',
      "Support email pour les questions d'usage du quotidien.",
      'Base idéale avant montée en gamme vers la conformité frontalière.'
    ],
    addOns: ['Passage ultérieur vers Pro pour la conformité 34 jours et les alertes.']
  },
  PRO: {
    headline: 'Le plan opérationnel pour piloter le télétravail frontalier Luxembourg au quotidien.',
    idealFor: 'Entreprises de 16 à 100 salariés avec frontaliers FR, BE ou DE et besoin de supervision RH active.',
    businessValue: [
      'Contrôle du seuil fiscal de 34 jours avec alertes et vue consolidée.',
      'Exclusions automatiques des jours non comptables liés aux congés et arrêts maladie.',
      'Statistiques mensuelles et exports pour le pilotage RH et la communication management.'
    ],
    serviceBundle: [
      'Couverture des arrêts maladie et des notifications email.',
      'Dashboard avancé orienté suivi de consommation et risques de dépassement.',
      "Plan recommandé pour l'usage télétravail frontalier réellement exploité."
    ],
    addOns: ['Export avancé déjà activable', 'SMS si connecté au provider', 'Audit déclaratif selon montée en gamme']
  },
  PREMIUM: {
    headline: "Le niveau gouvernance pour industrialiser la conformité, l'audit et le reporting.",
    idealFor: "Structures plus larges, groupes multi-équipes ou organisations souhaitant des traces d'audit et des exports comptables.",
    businessValue: [
      'Historique détaillé des déclarations et meilleure préparation des contrôles internes.',
      'Exports comptables et notifications SMS pour industrialiser les opérations quand le provider est configuré.',
      'Accompagnement onboarding et niveau de service renforcé.'
    ],
    serviceBundle: [
      'Convient aux organisations de 101 à 300 collaborateurs.',
      'Plus robuste pour les besoins finance, contrôle interne et pilotage transverse.',
      'Réduit les traitements manuels liés aux audits et aux extractions.'
    ],
    addOns: ['Support SLA', 'Accompagnement de déploiement', "Capacité d'industrialisation renforcée"]
  },
  ENTERPRISE: {
    headline: 'Une offre projet sur cadrage pour intégration, gouvernance et hébergement dédié.',
    idealFor: "Groupes et contextes sensibles qui ont besoin d'un accompagnement commercial et technique dédié plutôt qu'un package standard.",
    businessValue: [
      'Cadre de prix et de déploiement personnalisé.',
      'Instruction des demandes SSO, sécurité, hébergement et intégration au cas par cas.',
      'Convient aux besoins spécifiques non couverts par le catalogue standard.'
    ],
    serviceBundle: [
      "Construction d'un périmètre personnalisé avec l'équipe commerciale.",
      'Activation des demandes via tickets projet et cadrage technique.',
      "Approche projet plutôt qu'abonnement standard."
    ],
    addOns: ['Ateliers de cadrage', 'Intégrations spécifiques', 'Développements sur mesure']
  }
};
