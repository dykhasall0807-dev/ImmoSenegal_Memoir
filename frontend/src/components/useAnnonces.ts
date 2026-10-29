import { useEffect, useState } from 'react'
import { appelApi, ApiError } from '../api'
import type { Annonce, PagePaginee } from '../types'

export type EtatListe =
  | { statut: 'chargement' }
  | { statut: 'erreur'; message: string }
  | { statut: 'ok'; page: PagePaginee<Annonce> }

/** Charge une page d'annonces. `reessayer` relance la requête après une erreur. */
export function useAnnonces(requete: string) {
  const [etat, setEtat] = useState<EtatListe>({ statut: 'chargement' })
  const [essai, setEssai] = useState(0)

  useEffect(() => {
    let annule = false
    setEtat({ statut: 'chargement' })
    appelApi<PagePaginee<Annonce>>(`/api/annonces?${requete}`)
      .then((page) => {
        if (!annule) setEtat({ statut: 'ok', page })
      })
      .catch((e: unknown) => {
        if (!annule) setEtat({ statut: 'erreur', message: e instanceof ApiError ? e.message : 'Une erreur est survenue.' })
      })
    return () => {
      annule = true
    }
  }, [requete, essai])

  return { etat, reessayer: () => setEssai((n) => n + 1) }
}
