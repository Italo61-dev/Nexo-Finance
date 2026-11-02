import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { CardComponent } from './card.component';

@Component({
  standalone: true,
  imports: [CardComponent],
  template: `
    <nexo-card [title]="title" [subtitle]="subtitle" [icon]="icon" [variant]="variant" [padding]="padding">
      <div card-actions>
        <button id="test-action-button">Acao</button>
      </div>
      <p id="test-content">Conteudo Principal do Card</p>
      <div card-footer>
        <span id="test-footer">Rodape do Card</span>
      </div>
    </nexo-card>
  `
})
class TestCardHostComponent {
  title: string | null = 'Resumo Mensal';
  subtitle: string | null = 'Visualização de gastos';
  icon: string | null = 'account_balance_wallet';
  variant = 'default';
  padding = 'md';
}

describe('CardComponent', () => {
  let fixture: ComponentFixture<TestCardHostComponent>;
  let hostComponent: TestCardHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestCardHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestCardHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve renderizar o card com título, subtítulo, ícone e conteúdo projetado', () => {
    const cardElement = fixture.nativeElement.querySelector('.nexo-card');
    expect(cardElement).toBeTruthy();
    expect(fixture.nativeElement.querySelector('.card-title').textContent).toContain('Resumo Mensal');
    expect(fixture.nativeElement.querySelector('.card-subtitle').textContent).toContain('Visualização de gastos');
    expect(fixture.nativeElement.querySelector('.header-icon').textContent.trim()).toBe('account_balance_wallet');
    expect(fixture.nativeElement.querySelector('#test-action-button')).toBeTruthy();
    expect(fixture.nativeElement.querySelector('#test-content').textContent).toContain('Conteudo Principal do Card');
    expect(fixture.nativeElement.querySelector('#test-footer').textContent).toContain('Rodape do Card');
  });

  it('deve aplicar as classes corretas de variante e padding', () => {
    hostComponent.variant = 'elevated';
    hostComponent.padding = 'lg';
    fixture.detectChanges();

    const cardElement = fixture.nativeElement.querySelector('.nexo-card');
    expect(cardElement.classList.contains('variant-elevated')).toBeTrue();
    expect(cardElement.classList.contains('padding-large')).toBeTrue();
  });

  it('não deve renderizar o cabeçalho se title, subtitle e icon forem nulos', () => {
    hostComponent.title = null;
    hostComponent.subtitle = null;
    hostComponent.icon = null;
    fixture.detectChanges();

    const headerElement = fixture.nativeElement.querySelector('.card-header');
    expect(headerElement).toBeNull();
  });
});
