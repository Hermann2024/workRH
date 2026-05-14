import { CommonModule } from '@angular/common';
import { Component, inject, signal } from '@angular/core';
import { FormsModule } from '@angular/forms';
import { AuthService } from '../auth.service';
import { I18nService } from '../i18n/i18n.service';
import { PlatformWorkspaceResponse, SupportTicketResponse, WorkRhApiService } from '../workrh-api.service';

@Component({
  selector: 'app-platform-admin-page',
  standalone: true,
  imports: [CommonModule, FormsModule],
  templateUrl: './platform-admin-page.component.html',
  styleUrl: './page-styles.css'
})
export class PlatformAdminPageComponent {
  private readonly authService = inject(AuthService);
  private readonly api = inject(WorkRhApiService);
  readonly i18n = inject(I18nService);

  readonly session = this.authService.session;
  readonly tickets = signal<SupportTicketResponse[]>([]);
  readonly workspaces = signal<PlatformWorkspaceResponse[]>([]);
  readonly loadingTickets = signal(true);
  readonly loadingWorkspaces = signal(true);
  readonly ticketError = signal<string | null>(null);
  readonly workspaceError = signal<string | null>(null);
  readonly selectedTicket = signal<SupportTicketResponse | null>(null);
  readonly ticketActionMessage = signal<string | null>(null);
  readonly ticketActionError = signal<string | null>(null);
  readonly workingTicket = signal(false);
  resolutionDraft = '';

  constructor() {
    this.refreshWorkspaces();
    this.refreshTickets();
  }

  refreshWorkspaces(): void {
    this.loadingWorkspaces.set(true);
    this.workspaceError.set(null);
    this.api.getPlatformWorkspaces().subscribe({
      next: (workspaces) => {
        this.workspaces.set(workspaces);
        this.loadingWorkspaces.set(false);
      },
      error: () => {
        this.workspaceError.set(this.i18n.translate('platform.error_workspaces'));
        this.loadingWorkspaces.set(false);
      }
    });
  }

  seatUsageLabel(workspace: PlatformWorkspaceResponse): string {
    return `${workspace.seatsUsed} / ${workspace.seatsPurchased ?? 'illimite'}`;
  }

  refreshTickets(): void {
    this.loadingTickets.set(true);
    this.ticketError.set(null);
    this.api.getPlatformSupportTickets().subscribe({
      next: (tickets) => {
        this.tickets.set(tickets);
        this.loadingTickets.set(false);
      },
      error: () => {
        this.ticketError.set(this.i18n.translate('platform.error_tickets'));
        this.loadingTickets.set(false);
      }
    });
  }

  visibleMessage(ticket: SupportTicketResponse): string {
    return ticket.message.length > 220 ? `${ticket.message.slice(0, 220)}...` : ticket.message;
  }

  openTicket(ticket: SupportTicketResponse): void {
    this.selectedTicket.set(ticket);
    this.resolutionDraft = ticket.resolutionMessage ?? this.i18n.translate('platform.resolve_default');
    this.ticketActionMessage.set(null);
    this.ticketActionError.set(null);
  }

  closeTicketPanel(): void {
    this.selectedTicket.set(null);
    this.resolutionDraft = '';
    this.ticketActionMessage.set(null);
    this.ticketActionError.set(null);
  }

  resolveSelectedTicket(): void {
    const ticket = this.selectedTicket();
    const message = this.resolutionDraft.trim();
    if (!ticket || !message) {
      this.ticketActionError.set(this.i18n.translate('platform.resolve_required'));
      return;
    }

    this.workingTicket.set(true);
    this.ticketActionError.set(null);
    this.api.resolvePlatformSupportTicket(ticket.id, message).subscribe({
      next: (updatedTicket) => {
        this.tickets.update((tickets) => tickets.map((item) => item.id === updatedTicket.id ? updatedTicket : item));
        this.selectedTicket.set(updatedTicket);
        this.ticketActionMessage.set(this.i18n.translate('platform.resolve_success'));
        this.workingTicket.set(false);
      },
      error: () => {
        this.ticketActionError.set(this.i18n.translate('platform.resolve_error'));
        this.workingTicket.set(false);
      }
    });
  }

  deleteSelectedTicket(): void {
    const ticket = this.selectedTicket();
    if (!ticket || !window.confirm(this.i18n.translate('platform.delete_confirm', { id: ticket.id }))) {
      return;
    }

    this.workingTicket.set(true);
    this.ticketActionError.set(null);
    this.api.deletePlatformSupportTicket(ticket.id).subscribe({
      next: () => {
        this.tickets.update((tickets) => tickets.filter((item) => item.id !== ticket.id));
        this.closeTicketPanel();
        this.workingTicket.set(false);
      },
      error: () => {
        this.ticketActionError.set(this.i18n.translate('platform.delete_error'));
        this.workingTicket.set(false);
      }
    });
  }
}
