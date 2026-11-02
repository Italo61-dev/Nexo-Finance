import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type BadgeVariant = 'primary' | 'success' | 'danger' | 'warning' | 'info' | 'neutral';
export type BadgeSize = 'sm' | 'md' | 'lg';

@Component({
  selector: 'nexo-badge',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <span
      class="nexo-badge"
      [class.variant-primary]="variant() === 'primary'"
      [class.variant-success]="variant() === 'success'"
      [class.variant-danger]="variant() === 'danger'"
      [class.variant-warning]="variant() === 'warning'"
      [class.variant-info]="variant() === 'info'"
      [class.variant-neutral]="variant() === 'neutral'"
      [class.size-small]="size() === 'sm'"
      [class.size-medium]="size() === 'md'"
      [class.size-large]="size() === 'lg'"
      [class.is-pill]="isPill()"
    >
      @if (icon()) {
        <mat-icon class="badge-icon">{{ icon() }}</mat-icon>
      }
      <span class="badge-content">
        <ng-content></ng-content>
        @if (label()) {
          {{ label() }}
        }
      </span>
    </span>
  `,
  styles: [`
    .nexo-badge {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.35rem;
      font-family: var(--nexo-font-family);
      font-weight: 600;
      line-height: 1;
      border-radius: var(--nexo-radius-sm);
      transition: background-color var(--nexo-transition-fast), color var(--nexo-transition-fast);
      user-select: none;
    }

    .is-pill {
      border-radius: var(--nexo-radius-full);
    }

    /* Tamanhos */
    .size-small {
      padding: 0.2rem 0.5rem;
      font-size: 0.725rem;
      letter-spacing: 0.02em;
    }

    .size-medium {
      padding: 0.3rem 0.65rem;
      font-size: 0.8rem;
    }

    .size-large {
      padding: 0.4rem 0.85rem;
      font-size: 0.875rem;
    }

    .badge-icon {
      font-size: 0.95em;
      width: 0.95em;
      height: 0.95em;
      line-height: 0.95em;
    }

    /* Variantes de Cor Fintech */
    .variant-primary {
      background-color: var(--nexo-primary-light);
      color: var(--nexo-primary-dark);
      border: 1px solid rgba(0, 168, 89, 0.25);
    }

    :host-context([data-theme="dark"]) .variant-primary,
    :host-context(body.dark-theme) .variant-primary {
      background-color: rgba(0, 168, 89, 0.16);
      color: var(--nexo-neon);
      border: 1px solid rgba(27, 240, 137, 0.3);
    }

    .variant-success {
      background-color: var(--nexo-success-bg);
      color: var(--nexo-success);
      border: 1px solid rgba(0, 168, 89, 0.2);
    }

    .variant-danger {
      background-color: var(--nexo-danger-bg);
      color: var(--nexo-danger);
      border: 1px solid rgba(229, 57, 53, 0.25);
    }

    .variant-warning {
      background-color: var(--nexo-warning-bg);
      color: #B45309;
      border: 1px solid rgba(245, 158, 11, 0.25);
    }

    :host-context([data-theme="dark"]) .variant-warning,
    :host-context(body.dark-theme) .variant-warning {
      color: #FBBF24;
    }

    .variant-info {
      background-color: var(--nexo-info-bg);
      color: var(--nexo-info);
      border: 1px solid rgba(59, 130, 246, 0.25);
    }

    .variant-neutral {
      background-color: var(--nexo-surface-secondary);
      color: var(--nexo-text-secondary);
      border: 1px solid var(--nexo-border);
    }
  `]
})
export class BadgeComponent {
  readonly variant = input<BadgeVariant>('neutral');
  readonly size = input<BadgeSize>('md');
  readonly isPill = input<boolean>(false);
  readonly icon = input<string | null>(null);
  readonly label = input<string | null>(null);
}
