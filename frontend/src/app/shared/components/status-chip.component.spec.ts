import { ComponentFixture, TestBed } from '@angular/core/testing';

import { StatusChipComponent } from './status-chip.component';

describe('StatusChipComponent', () => {
  let fixture: ComponentFixture<StatusChipComponent>;

  beforeEach(() => {
    fixture = TestBed.createComponent(StatusChipComponent);
  });

  async function render(kind: 'quote' | 'policy', status: string): Promise<HTMLElement> {
    fixture.componentRef.setInput('kind', kind);
    fixture.componentRef.setInput('status', status);
    await fixture.whenStable();
    return (fixture.nativeElement as HTMLElement).querySelector<HTMLElement>('mat-chip')!;
  }

  it.each([
    ['quote', 'CALCULATED', 'Calculada', 'status-chip--primary'],
    ['quote', 'EMITTED', 'Emitida', 'status-chip--success'],
    ['quote', 'EXPIRED', 'Vencida', 'status-chip--muted'],
    ['policy', 'ACTIVE', 'Activa', 'status-chip--primary'],
    ['policy', 'EXPIRED', 'Vencida', 'status-chip--muted'],
  ] as const)('%s %s se muestra como "%s" con su color', async (kind, status, label, color) => {
    const chip = await render(kind, status);

    expect(chip.textContent?.trim()).toBe(label);
    expect(chip.classList).toContain(color);
  });
});
