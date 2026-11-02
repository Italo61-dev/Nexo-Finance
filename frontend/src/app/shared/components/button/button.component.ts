import { Component, input, output } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MatIconModule } from '@angular/material/icon';
import { MatProgressSpinnerModule } from '@angular/material/progress-spinner';

export type ButtonVariant = 'primary' | 'secondary' | 'outline' | 'danger' | 'ghost';
export type ButtonSize = 'sm' | 'md' | 'lg';
export type ButtonType = 'button' | 'submit' | 'reset';

@Component({
  selector: 'nexo-button',
  standalone: true,
  imports: [CommonModule, MatIconModule, MatProgressSpinnerModule],
  template: `
    <button
      [type]="type()"
      [disabled]="disabled() || isLoading()"
      class="nexo-button"
      [class.variant-primary]="variant() === 'primary'"
      [class.variant-secondary]="variant() === 'secondary'"
      [class.variant-outline]="variant() === 'outline'"
      [class.variant-danger]="variant() === 'danger'"
      [class.variant-ghost]="variant() === 'ghost'"
      [class.size-small]="size() === 'sm'"
      [class.size-medium]="size() === 'md'"
      [class.size-large]="size() === 'lg'"
      [class.full-width]="fullWidth()"
      [class.is-loading]="isLoading()"
      (click)="handleButtonClick($event)"
    >
      @if (isLoading()) {
        <mat-spinner [diameter]="getSpinnerDiameter()" class="button-spinner"></mat-spinner>
      } @else {
        @if (icon() && iconPosition() === 'left') {
          <mat-icon class="button-icon">{{ icon() }}</mat-icon>
        }
      }

      <span class="button-label" [class.invisible]="isLoading()">
        <ng-content></ng-content>
      </span>

      @if (!isLoading() && icon() && iconPosition() === 'right') {
        <mat-icon class="button-icon">{{ icon() }}</mat-icon>
      }
    </button>
  `,
  styles: [`
    .nexo-button {
      display: inline-flex;
      align-items: center;
      justify-content: center;
      gap: 0.5rem;
      font-family: var(--nexo-font-family);
      font-weight: 600;
      border-radius: var(--nexo-radius-md);
      border: 1px solid transparent;
      cursor: pointer;
      user-select: none;
      transition: background-color var(--nexo-transition-fast), 
                  border-color var(--nexo-transition-fast), 
                  box-shadow var(--nexo-transition-fast), 
                  transform var(--nexo-transition-fast);
      outline: none;
      position: relative;
    }

    .nexo-button:focus-visible {
      box-shadow: 0 0 0 3px var(--nexo-primary-glow);
    }

    .nexo-button:active:not(:disabled) {
      transform: scale(0.98);
    }

    .nexo-button:disabled {
      opacity: 0.55;
      cursor: not-allowed;
      transform: none;
    }

    .full-width {
      width: 100%;
    }

    /* Tamanhos */
    .size-small {
      padding: 0.45rem 0.85rem;
      font-size: 0.8125rem;
      min-height: 36px;
    }

    .size-medium {
      padding: 0.65rem 1.25rem;
      font-size: 0.9375rem;
      min-height: 44px;
    }

    .size-large {
      padding: 0.85rem 1.75rem;
      font-size: 1.05rem;
      min-height: 52px;
    }

    .button-icon {
      font-size: 1.2em;
      width: 1.2em;
      height: 1.2em;
      line-height: 1.2em;
    }

    .button-spinner {
      position: absolute;
    }

    .invisible {
      visibility: hidden;
    }

    /* Variantes */
    .variant-primary {
      background-color: var(--nexo-primary);
      color: #FFFFFF;
      box-shadow: 0 4px 14px var(--nexo-primary-glow);
    }

    .variant-primary:hover:not(:disabled) {
      background-color: var(--nexo-primary-hover);
      box-shadow: 0 6px 20px rgba(0, 168, 89, 0.35);
    }

    .variant-secondary {
      background-color: var(--nexo-surface-secondary);
      color: var(--nexo-text-primary);
      border-color: var(--nexo-border);
    }

    .variant-secondary:hover:not(:disabled) {
      background-color: var(--nexo-surface-hover);
      border-color: var(--nexo-text-muted);
    }

    .variant-outline {
      background-color: transparent;
      color: var(--nexo-primary);
      border-color: var(--nexo-primary);
    }

    .variant-outline:hover:not(:disabled) {
      background-color: var(--nexo-primary-light);
    }

    :host-context([data-theme="dark"]) .variant-outline,
    :host-context(body.dark-theme) .variant-outline {
      color: var(--nexo-neon);
      border-color: var(--nexo-neon);
    }

    :host-context([data-theme="dark"]) .variant-outline:hover:not(:disabled),
    :host-context(body.dark-theme) .variant-outline:hover:not(:disabled) {
      background-color: rgba(27, 240, 137, 0.12);
    }

    .variant-danger {
      background-color: var(--nexo-danger);
      color: #FFFFFF;
    }

    .variant-danger:hover:not(:disabled) {
      background-color: #D32F2F;
    }

    .variant-ghost {
      background-color: transparent;
      color: var(--nexo-text-secondary);
    }

    .variant-ghost:hover:not(:disabled) {
      background-color: var(--nexo-surface-secondary);
      color: var(--nexo-text-primary);
    }
  `]
})
export class ButtonComponent {
  readonly variant = input<ButtonVariant>('primary');
  readonly size = input<ButtonSize>('md');
  readonly type = input<ButtonType>('button');
  readonly disabled = input<boolean>(false);
  readonly isLoading = input<boolean>(false);
  readonly fullWidth = input<boolean>(false);
  readonly icon = input<string | null>(null);
  readonly iconPosition = input<'left' | 'right'>('left');

  readonly buttonClick = output<MouseEvent>();

  handleButtonClick(event: MouseEvent): void {
    if (!this.disabled() && !this.isLoading()) {
      this.buttonClick.emit(event);
    }
  }

  getSpinnerDiameter(): number {
    if (this.size() === 'sm') return 16;
    if (this.size() === 'lg') return 24;
    return 20;
  }
}
