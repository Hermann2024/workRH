import { CommonModule } from '@angular/common';
import { Component, inject } from '@angular/core';
import { AuthService } from '../auth.service';

@Component({
  selector: 'app-platform-admin-page',
  standalone: true,
  imports: [CommonModule],
  templateUrl: './platform-admin-page.component.html',
  styleUrl: './page-styles.css'
})
export class PlatformAdminPageComponent {
  private readonly authService = inject(AuthService);

  readonly session = this.authService.session;
}
