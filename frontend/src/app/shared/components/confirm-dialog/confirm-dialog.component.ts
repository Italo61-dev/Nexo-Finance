import { Component, inject } from '@angular/core';
import { CommonModule } from '@angular/common';
import { MAT_DIALOG_DATA, MatDialogModule, MatDialogRef } from '@angular/material/dialog';
import { MatButtonModule } from '@angular/material/button';
import { MatIconModule } from '@angular/material/icon';

export interface ConfirmDialogData {
  title: string;
  message: string;
  confirmText?: string;
  cancelText?: string;
  variant?: 'primary' | 'danger' | 'warning';
  icon?: string;
}

@Component({
  selector: 'nexo-confirm-dialog',
  standalone: true,
  imports: [CommonModule, MatDialogModule, MatButtonModule, MatIconModule],
  template: `
    <div class="confirm-dialog-wrapper" [class]="'variant-' + (data.variant || 'primary')">
      <div class="dialog-header">
        <div class="icon-circle" [class]="'icon-' + (data.variant || 'primary')">
          <mat-icon>{{ data.icon || getDefaultIcon() }}</mat-icon>
        </div>
        <h2 class="dialog-title">{{ data.title }}</h2>
      </div>

      <div class="dialog-content">
        <p class="dialog-message">{{ data.message }}</p>
      </div>

      <div class="dialog-actions">
        <button
          mat-stroked-button
          type="button"
          class="cancel-btn"
          (click)="onCancel()"
        >
          {{ data.cancelText || 'Cancelar' }}
        </button>

        <button
          mat-flat-button
          type="button"
          class="confirm-btn"
          [class]="'btn-' + (data.variant || 'primary')"
          (click)="onConfirm()"
        >
          {{ data.confirmText || 'Confirmar' }}
        </button>
      </div>
    </div>
  `,
  styles: [`
    .confirm-dialog-wrapper {
      background-color: var(--nexo-surface);
      color: var(--nexo-text-primary);
      padding: 1.5rem;
      border-radius: 20px;
      display: flex;
      flex-direction: column;
      gap: 1.25rem;
      min-width: 320px;
      max-width: 440px;
      font-family: var(--nexo-font-family);
    }

    .dialog-header {
      display: flex;
      flex-direction: column;
      align-items: center;
      text-align: center;
      gap: 0.85rem;
    }

    .icon-circle {
      width: 52px;
      height: 52px;
      border-radius: 50%;
      display: flex;
      align-items: center;
      justify-content: center;
      flex-shrink: 0;

      mat-icon {
        font-size: 26px;
        width: 26px;
        height: 26px;
        display: inline-flex;
        align-items: center;
        justify-content: center;
        line-height: 1;
      }

      &.icon-primary {
        background-color: var(--nexo-primary-light);
        color: var(--nexo-primary);
      }

      &.icon-danger {
        background-color: var(--nexo-danger-bg);
        color: var(--nexo-danger);
      }

      &.icon-warning {
        background-color: var(--nexo-warning-bg);
        color: var(--nexo-warning);
      }
    }

    :host-context([data-theme="dark"]) .icon-circle.icon-primary,
    :host-context(body.dark-theme) .icon-circle.icon-primary {
      background-color: rgba(0, 168, 89, 0.2);
      color: var(--nexo-neon);
    }

    .dialog-title {
      font-size: 1.25rem;
      font-weight: 800;
      letter-spacing: -0.02em;
      margin: 0;
      color: var(--nexo-text-primary);
    }

    .dialog-content {
      text-align: center;
      padding: 0 0.5rem;
    }

    .dialog-message {
      margin: 0;
      font-size: 0.925rem;
      line-height: 1.5;
      color: var(--nexo-text-secondary);
    }

    .dialog-actions {
      display: flex;
      gap: 0.75rem;
      justify-content: center;
      margin-top: 0.5rem;

      button {
        flex: 1;
        height: 42px;
        border-radius: 10px;
        font-weight: 600;
        font-size: 0.875rem;
        display: inline-flex;
        align-items: center;
        justify-content: center;
      }

      .cancel-btn {
        border-color: var(--nexo-border);
        color: var(--nexo-text-secondary);

        &:hover {
          background-color: var(--nexo-surface-secondary);
          color: var(--nexo-text-primary);
        }
      }

      .confirm-btn {
        color: #ffffff;

        &.btn-primary {
          background-color: var(--nexo-primary);
          box-shadow: 0 4px 12px var(--nexo-primary-glow);
          &:hover {
            background-color: var(--nexo-primary-hover);
          }
        }

        &.btn-danger {
          background-color: var(--nexo-danger);
          box-shadow: 0 4px 12px rgba(229, 57, 53, 0.3);
          &:hover {
            background-color: #d32f2f;
          }
        }

        &.btn-warning {
          background-color: var(--nexo-warning);
          box-shadow: 0 4px 12px rgba(245, 158, 11, 0.3);
          &:hover {
            background-color: #d97706;
          }
        }
      }
    }
  `]
})
export class ConfirmDialogComponent {
  readonly data = inject<ConfirmDialogData>(MAT_DIALOG_DATA);
  readonly dialogRef = inject(MatDialogRef<ConfirmDialogComponent>);

  getDefaultIcon(): string {
    switch (this.data.variant) {
      case 'danger': return 'warning';
      case 'warning': return 'error_outline';
      default: return 'help_outline';
    }
  }

  onConfirm(): void {
    this.dialogRef.close(true);
  }

  onCancel(): void {
    this.dialogRef.close(false);
  }
}
