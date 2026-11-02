import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { EmptyStateComponent } from './empty-state.component';

@Component({
  standalone: true,
  imports: [EmptyStateComponent],
  template: `
    <nexo-empty-state
      [icon]="icon"
      [title]="title"
      [description]="description"
    >
      <button id="test-empty-action">Adicionar Registro</button>
    </nexo-empty-state>
  `
})
class TestEmptyStateHostComponent {
  icon = 'receipt_long';
  title = 'Nenhuma transação encontrada';
  description = 'Você ainda não possui transações cadastradas neste período.';
}

describe('EmptyStateComponent', () => {
  let fixture: ComponentFixture<TestEmptyStateHostComponent>;
  let hostComponent: TestEmptyStateHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestEmptyStateHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestEmptyStateHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve renderizar o ícone, título, descrição e botão projetado', () => {
    const iconElement = fixture.nativeElement.querySelector('.empty-state-icon');
    const titleElement = fixture.nativeElement.querySelector('.empty-state-title');
    const descElement = fixture.nativeElement.querySelector('.empty-state-description');
    const buttonElement = fixture.nativeElement.querySelector('#test-empty-action');

    expect(iconElement.textContent.trim()).toBe('receipt_long');
    expect(titleElement.textContent).toContain('Nenhuma transação encontrada');
    expect(descElement.textContent).toContain('Você ainda não possui transações');
    expect(buttonElement).toBeTruthy();
  });
});
