import { toLocalIsoDate } from './format';

describe('toLocalIsoDate', () => {
  it('devuelve la fecha local en yyyy-MM-dd, sin pasar por UTC', () => {
    // 23:30 local: con toISOString() podría pasar al día siguiente según la zona del navegador.
    expect(toLocalIsoDate(new Date(2026, 10, 26, 23, 30))).toBe('2026-11-26');
    expect(toLocalIsoDate(new Date(2026, 0, 5, 0, 0))).toBe('2026-01-05');
  });
});
