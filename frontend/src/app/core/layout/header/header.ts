import { Component, inject } from '@angular/core';
import { MatIcon } from '@angular/material/icon';
import { MatToolbar } from '@angular/material/toolbar';
import { ThemeService } from '@core/services/theme.service';

@Component({
  imports: [MatToolbar, MatIcon],
  selector: 'app-header',
  styleUrl: './header.css',
  templateUrl: './header.html',
})
export class Header {
  readonly themeService = inject(ThemeService);
}
