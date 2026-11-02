import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { BadgeComponent } from '../badge/badge.component';

export type MetricVariant = 'primary' | 'success' | 'danger' | 'warning' | 'info' | 'neutral';
export type TrendDirection = 'up' | 'down' | 'neutral';

@Component({
  selector: 'nexo-metric-card',
  standalone: true,
  imports: [CommonModule, MatIconModule, BadgeComponent],
  template: `
    <div
      class="metric-card"
      [class.variant-primary]="variant() === 'primary'"
      [class.variant-success]="variant() === 'success'"
      [class.variant-danger]="variant() === 'danger'"
      [class.variant-warning]="variant() === 'warning'"
      [class.variant-info]="variant() === 'info'"
      [class.variant-neutral]="variant() === 'neutral'"
    >
      <div class="metric-header">
        <span class="metric-title">{{ title() }}</span>
        <div class="metric-icon-badge">
          <mat-icon class="metric-icon">{{ icon() }}</mat-icon>
        </div>
      </div>

      <div class="metric-body">
        <h2 class="metric-value">{{ value() }}</h2>
      </div>

      @if (trendLabel() || subtitle()) {
        <div class="metric-footer">
          @if (trendLabel()) {
            <nexo-badge
              [variant]="getTrendBadgeVariant()"
              size="sm"
              [isPill]="true"
              [icon]="getTrendIcon()"
            >
              {{ trendLabel() }}
            </nexo-badge>
          }
          @if (subtitle()) {
            <span class="metric-subtitle">{{ subtitle() }}</span>
          }
        </div>
      }
    </div>
  `,
  styles: [`
    .metric-card {
      background-color: var(--nexo-surface);
      border: 1px solid var(--nexo-border);
      border-radius: var(--nexo-radius-lg);
      padding: 1.25rem 1.5rem;
      box-shadow: var(--nexo-card-shadow);
      display: flex;
      flex-direction: column;
      gap: 0.75rem;
      position: relative;
      overflow: hidden;
      transition: transform var(--nexo-transition-fast), box-shadow var(--nexo-transition-fast);
    }

    .metric-card:hover {
      transform: translateY(-2px);
      box-shadow: var(--nexo-card-shadow-hover);
    }

    .metric-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 0.5rem;
    }

    .metric-title {
      font-size: 0.875rem;
      font-weight: 600;
      color: var(--nexo-text-secondary);
      letter-spacing: -0.01em;
    }

    .metric-icon-badge {
      width: 36px;
      height: 36px;
      border-radius: var(--nexo-radius-md);
      display: flex;
      align-items: center;
      justify-content: center;
      background-color: var(--nexo-surface-secondary);
      color: var(--nexo-primary);
      transition: background-color var(--nexo-transition-fast), color var(--nexo-transition-fast);
    }

    .metric-icon {
      font-size: 20px;
      width: 20px;
      height: 20px;
    }

    .metric-body {
      margin: 0.2rem 0;
    }

    .metric-value {
      font-size: 1.75rem;
      font-weight: 800;
      color: var(--nexo-text-primary);
      margin: 0;
      letter-spacing: -0.03em;
      line-height: 1.2;
    }

    .metric-footer {
      display: flex;
      align-items: center;
      gap: 0.5rem;
      font-size: 0.8rem;
      color: var(--nexo-text-muted);
    }

    .metric-subtitle {
      font-size: 0.8rem;
      color: var(--nexo-text-muted);
    }

    /* Variantes Visuais de Destaque */
    .variant-primary .metric-icon-badge {
      background-color: var(--nexo-primary-light);
      color: var(--nexo-primary);
    }

    :host-context([data-theme="dark"]) .variant-primary .metric-icon-badge,
    :host-context(body.dark-theme) .variant-primary .metric-icon-badge {
      background-color: rgba(0, 168, 89, 0.18);
      color: var(--nexo-neon);
    }

    .variant-success .metric-icon-badge {
      background-color: var(--nexo-success-bg);
      color: var(--nexo-success);
    }

    .variant-danger .metric-icon-badge {
      background-color: var(--nexo-danger-bg);
      color: var(--nexo-danger);
    }

    .variant-warning .metric-icon-badge {
      background-color: var(--nexo-warning-bg);
      color: var(--nexo-warning);
    }

    .variant-info .metric-icon-badge {
      background-color: var(--nexo-info-bg);
      color: var(--nexo-info);
    }
  `]
})
export class MetricCardComponent {
  readonly title = input.required<string>();
  readonly value = input.required<string | number>();
  readonly icon = input.required<string>();
  readonly trend = input<TrendDirection | null>(null);
  readonly trendLabel = input<string | null>(null);
  readonly subtitle = input<string | null>(null);
  readonly variant = input<MetricVariant>('primary');

  getTrendBadgeVariant(): 'success' | 'danger' | 'neutral' {
    if (this.trend() === 'up') return 'success';
    if (this.trend() === 'down') return 'danger';
    return 'neutral';
  }

  getTrendIcon(): string | null {
    if (this.trend() === 'up') return 'trending_up';
    if (this.trend() === 'down') return 'trending_down';
    if (this.trend() === 'neutral') return 'trending_flat';
    return null;
  }
}
