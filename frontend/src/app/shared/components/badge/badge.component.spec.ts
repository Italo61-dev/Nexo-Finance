import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { BadgeComponent } from './badge.component';

@Component({
  standalone: true,
  imports: [BadgeComponent],
  template: `
    <nexo-badge [variant]="variant" [size]="size" [isPill]="isPill" [label]="label" [icon]="icon">
      <span>Conteudo Projetado</span>
    </nexo-badge>
  `
})
class TestHostComponent {
  variant = 'primary';
  size = 'md';
  isPill = false;
  label = 'Ativo';
  icon: string | null = null;
}

describe('BadgeComponent', () => {
  let fixture: ComponentFixture<TestHostComponent>;
  let hostComponent: TestHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve renderizar com a variante primary e tamanho md por padrão', () => {
    const badgeElement = fixture.nativeElement.querySelector('.nexo-badge');
    expect(badgeElement).toBeTruthy();
    expect(badgeElement.classList.contains('variant-primary')).toBeTrue();
    expect(badgeElement.classList.contains('size-medium')).toBeTrue();
    expect(badgeElement.textContent).toContain('Ativo');
    expect(badgeElement.textContent).toContain('Conteudo Projetado');
  });

  it('deve aplicar a classe is-pill quando isPill for verdadeiro', () => {
    hostComponent.isPill = true;
    fixture.detectChanges();

    const badgeElement = fixture.nativeElement.querySelector('.nexo-badge');
    expect(badgeElement.classList.contains('is-pill')).toBeTrue();
  });

  it('deve aplicar variantes danger e tamanho small corretamente', () => {
    hostComponent.variant = 'danger';
    hostComponent.size = 'sm';
    fixture.detectChanges();

    const badgeElement = fixture.nativeElement.querySelector('.nexo-badge');
    expect(badgeElement.classList.contains('variant-danger')).toBeTrue();
    expect(badgeElement.classList.contains('size-small')).toBeTrue();
  });

  it('deve exibir ícone mat-icon quando a propriedade icon for informada', () => {
    hostComponent.icon = 'check_circle';
    fixture.detectChanges();

    const iconElement = fixture.nativeElement.querySelector('.badge-icon');
    expect(iconElement).toBeTruthy();
    expect(iconElement.textContent.trim()).toBe('check_circle');
  });
});
