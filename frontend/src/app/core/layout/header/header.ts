import { Component, inject } from '@angular/core';
import { MatButton, MatIconButton } from '@angular/material/button';
import { MatIcon } from '@angular/material/icon';
import { MatMenuModule } from '@angular/material/menu';
import { MatToolbar } from '@angular/material/toolbar';
import { RouterLink } from '@angular/router';
import { ThemeService } from '@core/services/theme.service';
import { LogoComponent } from '@core/logo/logo.component';

@Component({
  imports: [
    MatToolbar,
    MatIcon,
    MatButton,
    RouterLink,
    MatMenuModule,
    LogoComponent,
    MatIconButton,
  ],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  readonly themeService = inject(ThemeService);
}
