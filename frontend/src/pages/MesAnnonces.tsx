import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { ApiError, appelApi } from '../api'
import { useAuth } from '../auth'
import { formatFcfa } from '../format'
import { libelleStatut, type Annonce, type PagePaginee } from '../types'
import { peutPublier } from './FormulaireAnnonce'

export default function MesAnnonces() {
  const { utilisateur, jeton } = useAuth()
  const [annonces, setAnnonces] = useState<Annonce[] | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)
  const [aSupprimer, setASupprimer] = useState<string | null>(null)

  const charger = useCallback(async () => {
    try {
      const p = await appelApi<PagePaginee<Annonce>>('/api/annonces/mes-annonces?taille=50', { token: jeton })
      setAnnonces(p.contenu)
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Chargement impossible.')
    }
  }, [jeton])

  useEffect(() => {
    void charger()
  }, [charger])

  async function supprimer(id: string) {
    setErreur(null)
    try {
      await appelApi<void>(`/api/annonces/${id}`, { method: 'DELETE', token: jeton })
      setASupprimer(null)
      await charger()
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Suppression impossible.')
    }
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <div className="flex flex-wrap items-center justify-between gap-3">
        <h1 className="text-2xl font-bold">Mes annonces</h1>
        {peutPublier(utilisateur?.role) && (
          <Link to="/annonces/nouvelle" className="rounded-lg bg-emeraude-600 px-4 py-2.5 font-semibold text-white hover:bg-emeraude-700">
            Publier une annonce
          </Link>
        )}
      </div>
      {erreur && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{erreur}</p>}
      {!annonces && !erreur && <p role="status" className="mt-6 text-slate-500">Chargement…</p>}
      {annonces && annonces.length === 0 && <p className="mt-6 text-slate-600">Vous n'avez publié aucune annonce.</p>}
      <ul className="mt-6 space-y-3">
        {annonces?.map((a) => (
          <li key={a.id} className="rounded-2xl bg-white p-4 shadow">
            <div className="flex flex-wrap items-start justify-between gap-2">
              <div className="min-w-0">
                <Link to={`/annonces/${a.id}`} className="break-words font-semibold hover:underline">{a.titre}</Link>
                <p className="text-sm text-slate-600">{a.quartier}, {a.ville} · {formatFcfa(a.prix)}</p>
              </div>
              <span className="rounded-full bg-slate-100 px-2.5 py-1 text-xs font-medium">{libelleStatut[a.statut]}</span>
            </div>
            <div className="mt-3 flex flex-wrap gap-2">
              <Link to={`/annonces/${a.id}/modifier`} className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm font-medium">Modifier</Link>
              {aSupprimer === a.id ? (
                <>
                  <button onClick={() => supprimer(a.id)} className="rounded-lg bg-red-700 px-3 py-1.5 text-sm font-semibold text-white">Confirmer la suppression</button>
                  <button onClick={() => setASupprimer(null)} className="rounded-lg border border-slate-300 px-3 py-1.5 text-sm">Annuler</button>
                </>
              ) : (
                <button onClick={() => setASupprimer(a.id)} className="rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700">Supprimer</button>
              )}
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
