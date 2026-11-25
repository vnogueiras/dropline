import { ChangeDetectionStrategy, Component, input } from '@angular/core';
import { RouterLink } from '@angular/router';

/**
 * Dropline toolbar logo: a drop that doubles as a ticket stub
 * (side notches + perforation line).
 *
 * The SVG is inline and uses `currentColor`, so it follows the Material
 * theme (via the Tailwind `text-primary` token mapping) and switches
 * automatically with light/dark `color-scheme`.
 */
@Component({
  selector: 'app-logo',
  imports: [RouterLink],
  changeDetection: ChangeDetectionStrategy.OnPush,
  template: `
    <a
      routerLink="/"
      class="flex items-center gap-2 text-primary no-underline"
      aria-label="Dropline home"
    >
      <svg viewBox="0 0 32 32" class="size-7 shrink-0" aria-hidden="true">
        <defs>
          <mask id="dl-cut">
            <rect width="32" height="32" fill="#fff" />
            <circle cx="7" cy="20" r="2.4" fill="#000" />
            <circle cx="25" cy="20" r="2.4" fill="#000" />
            <line
              x1="10.5"
              y1="20"
              x2="22"
              y2="20"
              stroke="#000"
              stroke-width="1.6"
              stroke-linecap="round"
              stroke-dasharray="0.1 3.6"
            />
          </mask>
        </defs>
        <path
          mask="url(#dl-cut)"
          fill="currentColor"
          d="M16 2.5C16 2.5 7 13 7 20a9 9 0 0 0 18 0C25 13 16 2.5 16 2.5Z"
        />
      </svg>
      @if (showWordmark()) {
        <span class="text-xl font-bold tracking-tight">Dropline</span>
      }
    </a>
  `,
})
export class LogoComponent {
  /** Hide the text on narrow screens, e.g. `[showWordmark]="!isHandset()"`. */
  readonly showWordmark = input(true);
}
