import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

export type CardVariant = 'default' | 'elevated' | 'outlined' | 'glass';
export type CardPadding = 'none' | 'sm' | 'md' | 'lg';

@Component({
  selector: 'nexo-card',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div
      class="nexo-card"
      [class.variant-default]="variant() === 'default'"
      [class.variant-elevated]="variant() === 'elevated'"
      [class.variant-outlined]="variant() === 'outlined'"
      [class.variant-glass]="variant() === 'glass'"
      [class.padding-none]="padding() === 'none'"
      [class.padding-small]="padding() === 'sm'"
      [class.padding-medium]="padding() === 'md'"
      [class.padding-large]="padding() === 'lg'"
    >
      @if (title() || subtitle() || icon()) {
        <div class="card-header">
          <div class="header-main">
            @if (icon()) {
              <div class="header-icon-container">
                <mat-icon class="header-icon">{{ icon() }}</mat-icon>
              </div>
            }
            <div class="header-titles">
              @if (title()) {
                <h3 class="card-title">{{ title() }}</h3>
              }
              @if (subtitle()) {
                <p class="card-subtitle">{{ subtitle() }}</p>
              }
            </div>
          </div>
          <div class="header-actions">
            <ng-content select="[card-actions]"></ng-content>
          </div>
        </div>
      }

      <div class="card-content">
        <ng-content></ng-content>
      </div>

      <div class="card-footer">
        <ng-content select="[card-footer]"></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .nexo-card {
      background-color: var(--nexo-surface);
      color: var(--nexo-text-primary);
      border-radius: var(--nexo-radius-lg);
      border: 1px solid var(--nexo-border);
      box-shadow: var(--nexo-card-shadow);
      transition: box-shadow var(--nexo-transition-normal), border-color var(--nexo-transition-normal), transform var(--nexo-transition-fast);
      display: flex;
      flex-direction: column;
      position: relative;
      overflow: hidden;
    }

    /* Padding Variations */
    .padding-none {
      padding: 0;
    }

    .padding-small {
      padding: 1rem;
    }

    .padding-medium {
      padding: 1.5rem;
    }

    .padding-large {
      padding: 2rem;
    }

    /* Variants */
    .variant-elevated {
      box-shadow: var(--nexo-card-shadow-hover);
    }

    .variant-elevated:hover {
      transform: translateY(-2px);
    }

    .variant-outlined {
      box-shadow: none;
      border: 1px solid var(--nexo-border);
    }

    .variant-glass {
      backdrop-filter: blur(12px);
      -webkit-backdrop-filter: blur(12px);
      background-color: rgba(255, 255, 255, 0.7);
    }

    :host-context([data-theme="dark"]) .variant-glass,
    :host-context(body.dark-theme) .variant-glass {
      background-color: rgba(19, 26, 23, 0.7);
    }

    /* Header */
    .card-header {
      display: flex;
      align-items: center;
      justify-content: space-between;
      gap: 1rem;
      margin-bottom: 1.25rem;
    }

    .header-main {
      display: flex;
      align-items: center;
      gap: 0.75rem;
    }

    .header-icon-container {
      width: 40px;
      height: 40px;
      border-radius: var(--nexo-radius-md);
      background-color: var(--nexo-surface-secondary);
      color: var(--nexo-primary);
      display: flex;
      align-items: center;
      justify-content: center;
    }

    .header-icon {
      font-size: 22px;
      width: 22px;
      height: 22px;
    }

    .card-title {
      font-size: 1.125rem;
      font-weight: 700;
      margin: 0;
      color: var(--nexo-text-primary);
      line-height: 1.25;
    }

    .card-subtitle {
      font-size: 0.85rem;
      color: var(--nexo-text-secondary);
      margin: 0.2rem 0 0 0;
    }

    .header-actions {
      display: flex;
      align-items: center;
      gap: 0.5rem;
    }

    /* Content & Footer */
    .card-content {
      flex: 1;
    }

    .card-footer:empty {
      display: none;
    }

    .card-footer:not(:empty) {
      margin-top: 1.25rem;
      padding-top: 1rem;
      border-top: 1px solid var(--nexo-border-subtle);
    }
  `]
})
export class CardComponent {
  readonly title = input<string | null>(null);
  readonly subtitle = input<string | null>(null);
  readonly icon = input<string | null>(null);
  readonly variant = input<CardVariant>('default');
  readonly padding = input<CardPadding>('md');
}
