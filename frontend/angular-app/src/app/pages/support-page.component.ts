import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../auth.service';
import { SupportTicketResponse, WorkRhApiService } from '../workrh-api.service';

type SupportImpact = 'question' | 'bug' | 'incident' | 'login' | 'billing';
type SupportPriority = 'standard' | 'blocking' | 'critical';

@Component({
  selector: 'app-support-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './support-page.component.html',
  styleUrl: './page-styles.css'
})
export class SupportPageComponent {
  private readonly api = inject(WorkRhApiService);
  private readonly authService = inject(AuthService);

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly successTicket = signal<SupportTicketResponse | null>(null);
  readonly prioritySupportAvailable = signal(false);
  readonly ticketHistory = signal<SupportTicketResponse[]>([]);

  readonly supportDraft = {
    requesterName: '',
    requesterEmail: this.authService.session()?.email ?? '',
    phoneNumber: '',
    impact: 'bug' as SupportImpact,
    priority: 'standard' as SupportPriority,
    subject: '',
    message: ''
  };

  readonly impactOptions: Array<{ value: SupportImpact; label: string }> = [
    { value: 'bug', label: 'Bug applicatif' },
    { value: 'incident', label: 'Panne ou indisponibilité' },
    { value: 'login', label: 'Connexion ou accès' },
    { value: 'billing', label: 'Facturation' },
    { value: 'question', label: 'Question ou aide' }
  ];

  readonly priorityOptions: Array<{ value: SupportPriority; label: string }> = [
    { value: 'standard', label: 'Normal' },
    { value: 'blocking', label: 'Bloquant' },
    { value: 'critical', label: 'Critique' }
  ];

  constructor() {
    this.api.checkFeature('PRIORITY_SUPPORT').subscribe({
      next: (response) => this.prioritySupportAvailable.set(response.allowed),
      error: () => this.prioritySupportAvailable.set(false)
    });

    if (this.authService.hasRole('ADMIN') || this.authService.hasRole('HR')) {
      this.api.getSupportTickets().subscribe({
        next: (tickets) => this.ticketHistory.set(tickets.slice(0, 8)),
        error: () => this.ticketHistory.set([])
      });
    }
  }

  submitSupport(): void {
    const requesterEmail = this.supportDraft.requesterEmail.trim();
    const subject = this.supportDraft.subject.trim();
    const message = this.supportDraft.message.trim();
    if (!requesterEmail || !subject || !message) {
      this.loadError.set('Email, sujet et description sont obligatoires.');
      return;
    }

    this.loading.set(true);
    this.loadError.set(null);
    this.successTicket.set(null);

    this.api.createSupportTicket(this.resolveSupportMode(), {
      requesterName: this.supportDraft.requesterName.trim(),
      requesterEmail,
      phoneNumber: this.supportDraft.phoneNumber.trim() || null,
      subject: this.buildSubject(subject),
      message: this.buildMessage(message)
    }).subscribe({
      next: (ticket) => {
        this.loading.set(false);
        this.successTicket.set(ticket);
        this.ticketHistory.update((tickets) => [ticket, ...tickets].slice(0, 8));
        this.supportDraft.subject = '';
        this.supportDraft.message = '';
      },
      error: (error: HttpErrorResponse) => {
        this.loading.set(false);
        this.loadError.set(this.readBackendMessage(error, 'Impossible de creer le ticket de support.'));
      }
    });
  }

  responseTimeLabel(): string {
    if (this.supportDraft.priority === 'critical') {
      return this.prioritySupportAvailable() ? 'Traitement prioritaire avec objectif 4 h ouvrées.' : 'Ticket transmis en priorité normale selon le plan actif.';
    }
    if (this.supportDraft.priority === 'blocking') {
      return this.prioritySupportAvailable() ? 'Traitement prioritaire selon le support actif.' : 'Ticket transmis au support standard.';
    }
    return 'Accusé de réception et suivi dans le centre support.';
  }

  private resolveSupportMode(): 'standard' | 'priority' {
    if (this.prioritySupportAvailable() && (this.supportDraft.priority === 'blocking' || this.supportDraft.priority === 'critical')) {
      return 'priority';
    }
    return 'standard';
  }

  private buildSubject(subject: string): string {
    const impact = this.impactOptions.find((option) => option.value === this.supportDraft.impact)?.label ?? this.supportDraft.impact;
    return `[${impact}] ${subject}`;
  }

  private buildMessage(message: string): string {
    const session = this.authService.session();
    return [
      message,
      '',
      '--- Contexte automatique ---',
      `Priorité: ${this.supportDraft.priority}`,
      `Type: ${this.supportDraft.impact}`,
      `Page: ${window.location.href}`,
      `Tenant: ${session?.tenantId ?? 'non renseigné'}`,
      `Utilisateur: ${session?.email ?? this.supportDraft.requesterEmail}`,
      `Navigateur: ${navigator.userAgent}`
    ].join('\n');
  }

  private readBackendMessage(error: HttpErrorResponse, fallback: string): string {
    if (typeof error.error?.message === 'string' && error.error.message.trim()) {
      return error.error.message;
    }
    if (error.status === 403) {
      return "Le support n'est pas inclus dans le plan actif.";
    }
    if (error.status === 401) {
      return 'Votre session a expiré. Reconnectez-vous puis réessayez.';
    }
    return fallback;
  }
}
