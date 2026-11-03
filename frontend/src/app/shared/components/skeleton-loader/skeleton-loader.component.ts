import { Component, input } from '@angular/core';
import { CommonModule } from '@angular/common';

export type SkeletonVariant = 'text' | 'circular' | 'rectangular' | 'card';

@Component({
  selector: 'nexo-skeleton-loader',
  standalone: true,
  imports: [CommonModule],
  template: `
    <div class="skeleton-wrapper" [class.is-list]="count() > 1">
      @for (item of getItemsArray(); track $index) {
        <div
          class="skeleton-item"
          [class.variant-text]="variant() === 'text'"
          [class.variant-circular]="variant() === 'circular'"
          [class.variant-rectangular]="variant() === 'rectangular'"
          [class.variant-card]="variant() === 'card'"
          [style.width]="width()"
          [style.height]="getHeight()"
          [style.border-radius]="getBorderRadius()"
        ></div>
      }
    </div>
  `,
  styles: [`
    .skeleton-wrapper {
      display: flex;
      flex-direction: column;
      gap: 0.5rem;
      width: 100%;
    }

    .skeleton-item {
      background: linear-gradient(
        90deg,
        var(--nexo-surface-secondary) 25%,
        var(--nexo-border) 37%,
        var(--nexo-surface-secondary) 63%
      );
      background-size: 400% 100%;
      animation: skeleton-shimmer 1.4s ease infinite;
      position: relative;
      overflow: hidden;
    }

    @keyframes skeleton-shimmer {
      0% {
        background-position: 100% 50%;
      }
      100% {
        background-position: 0 50%;
      }
    }

    .variant-text {
      height: 1rem;
      border-radius: var(--nexo-radius-xs);
    }

    .variant-circular {
      border-radius: 50%;
    }

    .variant-rectangular {
      border-radius: var(--nexo-radius-sm);
    }

    .variant-card {
      border-radius: var(--nexo-radius-lg);
      height: 120px;
    }
  `]
})
export class SkeletonLoaderComponent {
  readonly variant = input<SkeletonVariant>('text');
  readonly width = input<string>('100%');
  readonly height = input<string | null>(null);
  readonly count = input<number>(1);
  readonly borderRadius = input<string | null>(null);

  getItemsArray(): number[] {
    return Array.from({ length: Math.max(1, this.count()) }, (_, index) => index);
  }

  getHeight(): string {
    if (this.height()) {
      return this.height()!;
    }
    if (this.variant() === 'circular') return '40px';
    if (this.variant() === 'card') return '120px';
    if (this.variant() === 'rectangular') return '48px';
    return '1rem';
  }

  getBorderRadius(): string | null {
    if (this.borderRadius()) {
      return this.borderRadius();
    }
    if (this.variant() === 'circular') return '50%';
    return null;
  }
}
