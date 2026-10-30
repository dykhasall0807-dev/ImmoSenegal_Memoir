import { useState, type FormEvent } from 'react'
import { ApiError, appelApi } from '../api'
import { useAuth } from '../auth'

/** Un Client demande à devenir Propriétaire ou Agent. Rien n'est accordé ici : un administrateur doit valider. */
export default function DemandeRole() {
  const { jeton } = useAuth()
  const [role, setRole] = useState('PROPRIETAIRE')
  const [envoi, setEnvoi] = useState(false)
  const [succes, setSucces] = useState(false)
  const [erreur, setErreur] = useState<string | null>(null)

  async function envoyer(e: FormEvent) {
    e.preventDefault()
    setEnvoi(true)
    setErreur(null)
    try {
      await appelApi('/api/auth/role-requests', { method: 'POST', body: { role }, token: jeton })
      setSucces(true)
    } catch (err) {
      setErreur(err instanceof ApiError ? err.message : 'Une erreur inattendue est survenue.')
    } finally {
      setEnvoi(false)
    }
  }

  return (
    <section aria-labelledby="demande-role" className="mt-6 rounded-2xl bg-white p-5 shadow">
      <h2 id="demande-role" className="text-lg font-semibold">Publier des annonces</h2>
      <p className="mt-1 text-sm text-slate-600">
        Votre compte est un compte Client. Pour publier des biens, demandez un rôle : un administrateur doit le valider.
      </p>
      {succes ? (
        <p role="status" className="mt-3 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">
          Demande envoyée. Elle est en attente de validation ; reconnectez-vous ou rechargez « Mon espace » après son approbation.
        </p>
      ) : (
        <form onSubmit={envoyer} className="mt-3 flex flex-wrap items-end gap-3">
          <div>
            <label htmlFor="role-demande" className="mb-1 block text-sm font-medium text-slate-700">Rôle souhaité</label>
            <select id="role-demande" value={role} onChange={(e) => setRole(e.target.value)} className="rounded-lg border border-slate-300 bg-white px-3 py-2.5">
              <option value="PROPRIETAIRE">Propriétaire</option>
              <option value="AGENT">Agent immobilier</option>
            </select>
          </div>
          <button type="submit" disabled={envoi} className="rounded-lg bg-emeraude-600 px-4 py-2.5 font-semibold text-white hover:bg-emeraude-700 disabled:opacity-60">
            {envoi ? 'Envoi…' : 'Envoyer la demande'}
          </button>
        </form>
      )}
      {erreur && <p role="alert" className="mt-3 rounded-lg bg-red-50 p-3 text-sm text-red-800">{erreur}</p>}
    </section>
  )
}
