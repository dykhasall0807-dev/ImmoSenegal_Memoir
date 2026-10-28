import { formatFcfa } from './format'

describe('formatFcfa', () => {
  it('formate un montant avec séparateur de milliers et FCFA', () => {
    expect(formatFcfa(350000)).toBe('350 000 FCFA')
    expect(formatFcfa(45000000)).toBe('45 000 000 FCFA')
  })
  it('arrondit les décimales', () => {
    expect(formatFcfa(1999.6)).toBe('2 000 FCFA')
  })
})
