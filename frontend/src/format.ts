/** Formate un montant en FCFA, ex. 350 000 FCFA. */
export function formatFcfa(montant: number): string {
  const nombre = new Intl.NumberFormat('fr-FR').format(Math.round(montant)).replace(/[  ]/g, ' ')
  return `${nombre} FCFA`
}
