import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';

@Component({
  selector: 'nexo-empty-state',
  standalone: true,
  imports: [CommonModule, MatIconModule],
  template: `
    <div class="empty-state-container">
      <div class="empty-state-icon-wrapper">
        <mat-icon class="empty-state-icon">{{ icon() }}</mat-icon>
      </div>

      <h3 class="empty-state-title">{{ title() }}</h3>
      <p class="empty-state-description">{{ description() }}</p>

      <div class="empty-state-actions">
        <ng-content></ng-content>
      </div>
    </div>
  `,
  styles: [`
    .empty-state-container {
      display: flex;
      flex-direction: column;
      align-items: center;
      justify-content: center;
      text-align: center;
      padding: 3rem 1.5rem;
      border-radius: var(--nexo-radius-lg);
      background-color: var(--nexo-surface-secondary);
      border: 1px dashed var(--nexo-border);
      width: 100%;
    }

    .empty-state-icon-wrapper {
      width: 64px;
      height: 64px;
      border-radius: var(--nexo-radius-full);
      background-color: var(--nexo-surface);
      color: var(--nexo-text-muted);
      border: 1px solid var(--nexo-border);
      display: flex;
      align-items: center;
      justify-content: center;
      margin-bottom: 1.25rem;
      box-shadow: var(--nexo-card-shadow);
    }

    .empty-state-icon {
      font-size: 32px;
      width: 32px;
      height: 32px;
      color: var(--nexo-primary);
    }

    .empty-state-title {
      font-size: 1.125rem;
      font-weight: 700;
      color: var(--nexo-text-primary);
      margin: 0 0 0.5rem 0;
      letter-spacing: -0.01em;
    }

    .empty-state-description {
      font-size: 0.9rem;
      color: var(--nexo-text-secondary);
      margin: 0 0 1.5rem 0;
      max-width: 420px;
      line-height: 1.5;
    }

    .empty-state-actions:not(:empty) {
      display: flex;
      align-items: center;
      gap: 0.75rem;
      flex-wrap: wrap;
      justify-content: center;
    }
  `]
})
export class EmptyStateComponent {
  readonly icon = input<string>('inbox');
  readonly title = input.required<string>();
  readonly description = input.required<string>();
}
