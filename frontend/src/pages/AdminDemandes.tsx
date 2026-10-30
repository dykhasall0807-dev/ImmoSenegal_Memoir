import { useCallback, useEffect, useState } from 'react'
import { ApiError, appelApi } from '../api'
import { useAuth } from '../auth'
import { libelleRole, type Role } from '../types'

interface Demande {
  id: string
  email: string
  nomComplet: string
  roleDemande: Role
  statut: 'EN_ATTENTE' | 'APPROUVEE' | 'REFUSEE'
  creeLe: string
}

export default function AdminDemandes() {
  const { utilisateur, jeton } = useAuth()
  const [demandes, setDemandes] = useState<Demande[] | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  const charger = useCallback(async () => {
    try {
      setDemandes(await appelApi<Demande[]>('/api/admin/role-requests?statut=EN_ATTENTE', { token: jeton }))
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Chargement impossible.')
    }
  }, [jeton])

  useEffect(() => {
    if (utilisateur?.role === 'ADMIN') void charger()
  }, [charger, utilisateur?.role])

  async function decider(id: string, action: 'approve' | 'reject') {
    setErreur(null)
    try {
      await appelApi(`/api/admin/role-requests/${id}/${action}`, { method: 'POST', token: jeton })
      await charger()
    } catch (e) {
      setErreur(e instanceof ApiError ? e.message : 'Action impossible.')
    }
  }

  if (utilisateur?.role !== 'ADMIN') {
    return (
      <div className="mx-auto max-w-2xl px-4 py-12">
        <p role="alert" className="rounded-lg bg-amber-50 p-4 text-sm">Cette page est réservée aux administrateurs.</p>
      </div>
    )
  }

  return (
    <div className="mx-auto max-w-3xl px-4 py-8">
      <h1 className="text-2xl font-bold">Demandes de rôle en attente</h1>
      {erreur && <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">{erreur}</p>}
      {!demandes && !erreur && <p role="status" className="mt-6 text-slate-500">Chargement…</p>}
      {demandes && demandes.length === 0 && <p className="mt-6 text-slate-600">Aucune demande en attente.</p>}
      <ul className="mt-6 space-y-3">
        {demandes?.map((d) => (
          <li key={d.id} className="rounded-2xl bg-white p-4 shadow">
            <p className="break-words font-semibold">{d.nomComplet}</p>
            <p className="break-all text-sm text-slate-600">{d.email}</p>
            <p className="mt-1 text-sm">Rôle demandé : <strong>{libelleRole[d.roleDemande]}</strong></p>
            <div className="mt-3 flex flex-wrap gap-2">
              <button onClick={() => decider(d.id, 'approve')} className="rounded-lg bg-emeraude-600 px-3 py-1.5 text-sm font-semibold text-white">
                Approuver
              </button>
              <button onClick={() => decider(d.id, 'reject')} className="rounded-lg border border-red-300 px-3 py-1.5 text-sm font-medium text-red-700">
                Refuser
              </button>
            </div>
          </li>
        ))}
      </ul>
    </div>
  )
}
