import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { ButtonComponent } from './button.component';

@Component({
  standalone: true,
  imports: [ButtonComponent],
  template: `
    <nexo-button
      [variant]="variant"
      [size]="size"
      [disabled]="disabled"
      [isLoading]="isLoading"
      [icon]="icon"
      (buttonClick)="onButtonClicked($event)"
    >
      Confirmar Ação
    </nexo-button>
  `
})
class TestButtonHostComponent {
  variant = 'primary';
  size = 'md';
  disabled = false;
  isLoading = false;
  icon: string | null = null;
  clicked = false;

  onButtonClicked(event: MouseEvent): void {
    this.clicked = true;
  }
}

describe('ButtonComponent', () => {
  let fixture: ComponentFixture<TestButtonHostComponent>;
  let hostComponent: TestButtonHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestButtonHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestButtonHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve renderizar o botão com o rótulo projetado', () => {
    const buttonElement = fixture.nativeElement.querySelector('button');
    expect(buttonElement).toBeTruthy();
    expect(buttonElement.textContent).toContain('Confirmar Ação');
    expect(buttonElement.classList.contains('variant-primary')).toBeTrue();
    expect(buttonElement.classList.contains('size-medium')).toBeTrue();
  });

  it('deve emitir o evento buttonClick quando clicado', () => {
    const buttonElement = fixture.nativeElement.querySelector('button');
    buttonElement.click();
    expect(hostComponent.clicked).toBeTrue();
  });

  it('não deve emitir o evento buttonClick quando desabilitado', () => {
    hostComponent.disabled = true;
    fixture.detectChanges();

    const buttonElement = fixture.nativeElement.querySelector('button');
    expect(buttonElement.disabled).toBeTrue();

    buttonElement.click();
    expect(hostComponent.clicked).toBeFalse();
  });

  it('deve exibir spinner de carregamento e ocultar o ícone quando isLoading for verdadeiro', () => {
    hostComponent.icon = 'add';
    hostComponent.isLoading = true;
    fixture.detectChanges();

    const spinner = fixture.nativeElement.querySelector('mat-spinner');
    const icon = fixture.nativeElement.querySelector('.button-icon');

    expect(spinner).toBeTruthy();
    expect(icon).toBeNull();
  });
});
