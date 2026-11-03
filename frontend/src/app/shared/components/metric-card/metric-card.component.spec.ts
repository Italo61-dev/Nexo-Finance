import { ComponentFixture, TestBed } from '@angular/core/testing';
import { Component } from '@angular/core';
import { MetricCardComponent, TrendDirection } from './metric-card.component';

@Component({
  standalone: true,
  imports: [MetricCardComponent],
  template: `
    <nexo-metric-card
      [title]="title"
      [value]="value"
      [icon]="icon"
      [trend]="trend"
      [trendLabel]="trendLabel"
      [subtitle]="subtitle"
      [variant]="variant"
    ></nexo-metric-card>
  `
})
class TestMetricCardHostComponent {
  title = 'Saldo Total';
  value = 'R$ 15.420,50';
  icon = 'wallet';
  trend: TrendDirection | null = 'up';
  trendLabel: string | null = '+12.5%';
  subtitle: string | null = 'vs. mês anterior';
  variant = 'primary';
}

describe('MetricCardComponent', () => {
  let fixture: ComponentFixture<TestMetricCardHostComponent>;
  let hostComponent: TestMetricCardHostComponent;

  beforeEach(async () => {
    await TestBed.configureTestingModule({
      imports: [TestMetricCardHostComponent]
    }).compileComponents();

    fixture = TestBed.createComponent(TestMetricCardHostComponent);
    hostComponent = fixture.componentInstance;
    fixture.detectChanges();
  });

  it('deve renderizar o título, valor, ícone e footer com trend badge', () => {
    const titleElement = fixture.nativeElement.querySelector('.metric-title');
    const valueElement = fixture.nativeElement.querySelector('.metric-value');
    const iconElement = fixture.nativeElement.querySelector('.metric-icon');
    const subtitleElement = fixture.nativeElement.querySelector('.metric-subtitle');

    expect(titleElement.textContent).toContain('Saldo Total');
    expect(valueElement.textContent).toContain('R$ 15.420,50');
    expect(iconElement.textContent.trim()).toBe('wallet');
    expect(subtitleElement.textContent).toContain('vs. mês anterior');
  });

  it('deve calcular corretamente a variante e o ícone de tendência para "down"', () => {
    hostComponent.trend = 'down';
    hostComponent.trendLabel = '-5.2%';
    fixture.detectChanges();

    const metricCardDebug = fixture.debugElement.children[0].componentInstance as MetricCardComponent;
    expect(metricCardDebug.getTrendBadgeVariant()).toBe('danger');
    expect(metricCardDebug.getTrendIcon()).toBe('trending_down');
  });

  it('não deve renderizar footer se trendLabel e subtitle forem nulos', () => {
    hostComponent.trendLabel = null;
    hostComponent.subtitle = null;
    fixture.detectChanges();

    const footerElement = fixture.nativeElement.querySelector('.metric-footer');
    expect(footerElement).toBeNull();
  });
});
