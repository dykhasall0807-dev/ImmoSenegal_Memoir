import { useEffect, useState } from 'react'
import { ApiError, appelApi } from './api'

interface Etat<T> {
  chemin: string
  data: T | null
  erreur: ApiError | null
}

/** Charge une ressource publique. `chargement` vaut true tant que la réponse correspond à un ancien chemin. */
export function useApi<T>(chemin: string) {
  const [etat, setEtat] = useState<Etat<T>>({ chemin: '', data: null, erreur: null })

  useEffect(() => {
    let annule = false
    appelApi<T>(chemin)
      .then((data) => {
        if (!annule) setEtat({ chemin, data, erreur: null })
      })
      .catch((e: unknown) => {
        if (!annule) setEtat({ chemin, data: null, erreur: e instanceof ApiError ? e : new ApiError(0, 'Erreur inattendue.') })
      })
    return () => {
      annule = true
    }
  }, [chemin])

  const chargement = etat.chemin !== chemin
  return {
    data: chargement ? null : etat.data,
    erreur: chargement ? null : etat.erreur,
    chargement,
  }
}
