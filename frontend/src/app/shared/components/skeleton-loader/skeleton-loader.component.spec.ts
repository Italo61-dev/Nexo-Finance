import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { SkeletonLoaderComponent, SkeletonVariant } from './skeleton-loader.component';

@Component({
  standalone: true,
  imports: [SkeletonLoaderComponent],
  template: `
    <nexo-skeleton-loader
      [variant]="variant"
      [width]="width"
      [height]="height"
      [count]="count"
    ></nexo-skeleton-loader>
  `
})
class TestSkeletonHostComponent {
  variant: SkeletonVariant = 'text';
  width = '100%';
  height: string | null = null;
  count = 1;
}

describe('SkeletonLoaderComponent', () => {
  let fixture: ComponentFixture<TestSkeletonHostComponent>;
  let hostComponent: TestSkeletonHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestSkeletonHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestSkeletonHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve renderizar um item de skeleton text por padrão', () => {
    const items = fixture.nativeElement.querySelectorAll('.skeleton-item');
    expect(items.length).toBe(1);
    expect(items[0].classList.contains('variant-text')).toBeTrue();
  });

  it('deve renderizar múltiplos itens quando count for maior que 1', () => {
    hostComponent.count = 3;
    fixture.detectChanges();

    const items = fixture.nativeElement.querySelectorAll('.skeleton-item');
    expect(items.length).toBe(3);
  });

  it('deve aplicar altura e variante circular corretamente', () => {
    hostComponent.variant = 'circular';
    hostComponent.width = '48px';
    fixture.detectChanges();

    const item = fixture.nativeElement.querySelector('.skeleton-item');
    expect(item.classList.contains('variant-circular')).toBeTrue();
    expect(item.style.width).toBe('48px');
    expect(item.style.borderRadius).toBe('50%');
  });
});
