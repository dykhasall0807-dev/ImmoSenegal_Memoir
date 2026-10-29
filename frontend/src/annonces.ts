/**
 * Passage des paramètres de l'URL du catalogue (partageable) aux paramètres de l'API.
 * Seuls les paramètres connus sont transmis, et les nombres invalides sont ignorés.
 */
const TEXTES = ['transaction', 'type', 'quartier', 'ville', 'equipements', 'tri'] as const
const NOMBRES = ['prixMin', 'prixMax', 'superficieMin', 'piecesMin'] as const

export function parametresApi(url: URLSearchParams): URLSearchParams {
  const api = new URLSearchParams()
  for (const nom of TEXTES) {
    const valeur = url.get(nom)?.trim()
    if (valeur) api.set(nom, valeur)
  }
  for (const nom of NOMBRES) {
    const valeur = url.get(nom)
    if (valeur !== null && valeur !== '' && Number.isFinite(Number(valeur)) && Number(valeur) >= 0) {
      api.set(nom, String(Number(valeur)))
    }
  }
  const page = Number(url.get('page'))
  api.set('page', String(Number.isInteger(page) && page > 0 ? page : 0))
  api.set('taille', '12')
  return api
}
