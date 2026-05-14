import { CommonModule } from '@angular/common';
import { Component, computed, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { HttpErrorResponse } from '@angular/common/http';
import { AuthService } from '../auth.service';
import { I18nService } from '../i18n/i18n.service';
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
  readonly i18n = inject(I18nService);

  readonly loading = signal(false);
  readonly loadError = signal<string | null>(null);
  readonly successTicket = signal<SupportTicketResponse | null>(null);
  readonly prioritySupportAvailable = signal(false);
  readonly ticketHistory = signal<SupportTicketResponse[]>([]);
  readonly selectedHistoryTicket = signal<SupportTicketResponse | null>(null);
  readonly resolvingTicket = signal(false);
  readonly ticketActionMessage = signal<string | null>(null);
  readonly ticketActionError = signal<string | null>(null);
  readonly canCreateSupportTicket = computed(() => this.authService.hasRole('HR') || this.authService.hasRole('EMPLOYEE'));
  readonly canViewTenantTickets = computed(() => this.authService.hasRole('ADMIN') || this.authService.hasRole('HR'));
  readonly canResolveSupportTicket = computed(() => this.authService.hasRole('ADMIN'));
  resolutionDraft = '';

  readonly supportDraft = {
    requesterName: '',
    requesterEmail: this.authService.session()?.email ?? '',
    phoneNumber: '',
    impact: 'bug' as SupportImpact,
    priority: 'standard' as SupportPriority,
    subject: '',
    message: ''
  };

  readonly impactOptions = computed(() => {
    this.i18n.locale();
    const t = (k: string) => this.i18n.translate(k);
    return [
      { value: 'bug' as SupportImpact, label: t('support.impact.bug') },
      { value: 'incident' as SupportImpact, label: t('support.impact.incident') },
      { value: 'login' as SupportImpact, label: t('support.impact.login') },
      { value: 'billing' as SupportImpact, label: t('support.impact.billing') },
      { value: 'question' as SupportImpact, label: t('support.impact.question') }
    ];
  });

  readonly priorityOptions = computed(() => {
    this.i18n.locale();
    const t = (k: string) => this.i18n.translate(k);
    return [
      { value: 'standard' as SupportPriority, label: t('support.prio.standard') },
      { value: 'blocking' as SupportPriority, label: t('support.prio.blocking') },
      { value: 'critical' as SupportPriority, label: t('support.prio.critical') }
    ];
  });

  constructor() {
    this.api.checkFeature('PRIORITY_SUPPORT').subscribe({
      next: (response) => this.prioritySupportAvailable.set(response.allowed),
      error: () => this.prioritySupportAvailable.set(false)
    });

    if (this.canViewTenantTickets()) {
      this.api.getSupportTickets().subscribe({
        next: (tickets) => this.ticketHistory.set(tickets.slice(0, 8)),
        error: () => this.ticketHistory.set([])
      });
    }
  }

  responseTimeLabel(): string {
    this.i18n.locale();
    const t = (k: string) => this.i18n.translate(k);
    if (this.supportDraft.priority === 'critical') {
      return this.prioritySupportAvailable() ? t('support.response.critical_yes') : t('support.response.critical_no');
    }
    if (this.supportDraft.priority === 'blocking') {
      return this.prioritySupportAvailable() ? t('support.response.blocking_yes') : t('support.response.blocking_no');
    }
    return t('support.response.default');
  }

  submitSupport(): void {
    if (!this.canCreateSupportTicket()) {
      this.loadError.set('Action non autorisee.');
      return;
    }

    const requesterEmail = this.supportDraft.requesterEmail.trim();
    const subject = this.supportDraft.subject.trim();
    const message = this.supportDraft.message.trim();
    if (!requesterEmail || !subject || !message) {
      this.loadError.set(this.i18n.translate('support.err_required'));
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
        this.loadError.set(this.readBackendMessage(error, this.i18n.translate('support.err_generic')));
      }
    });
  }

  openHistoryTicket(ticket: SupportTicketResponse): void {
    this.selectedHistoryTicket.set(ticket);
    this.resolutionDraft = ticket.resolutionMessage ?? this.i18n.translate('support.resolve_default');
    this.ticketActionMessage.set(null);
    this.ticketActionError.set(null);
  }

  closeHistoryTicket(): void {
    this.selectedHistoryTicket.set(null);
    this.resolutionDraft = '';
    this.ticketActionMessage.set(null);
    this.ticketActionError.set(null);
  }

  resolveHistoryTicket(): void {
    const ticket = this.selectedHistoryTicket();
    const message = this.resolutionDraft.trim();
    if (!ticket || !message) {
      this.ticketActionError.set(this.i18n.translate('support.resolve_required'));
      return;
    }

    this.resolvingTicket.set(true);
    this.ticketActionError.set(null);
    this.api.resolveTenantSupportTicket(ticket.id, message).subscribe({
      next: (updatedTicket) => {
        this.ticketHistory.update((tickets) => tickets.map((item) => item.id === updatedTicket.id ? updatedTicket : item));
        this.selectedHistoryTicket.set(updatedTicket);
        this.ticketActionMessage.set(this.i18n.translate('support.resolve_success'));
        this.resolvingTicket.set(false);
      },
      error: (error: HttpErrorResponse) => {
        this.ticketActionError.set(this.readBackendMessage(error, this.i18n.translate('support.resolve_error')));
        this.resolvingTicket.set(false);
      }
    });
  }

  private resolveSupportMode(): 'standard' | 'priority' {
    if (this.prioritySupportAvailable() && (this.supportDraft.priority === 'blocking' || this.supportDraft.priority === 'critical')) {
      return 'priority';
    }
    return 'standard';
  }

  private buildSubject(subject: string): string {
    const impact = this.impactOptions().find((option) => option.value === this.supportDraft.impact)?.label ?? this.supportDraft.impact;
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
      return this.i18n.translate('support.err_403');
    }
    if (error.status === 401) {
      return this.i18n.translate('support.err_401');
    }
    return fallback;
  }
}
