import { useEffect, useState } from 'react'
import { Link, useNavigate } from 'react-router-dom'
import { appelApi, ApiError } from '../api'
import { useAuth } from '../auth'
import DemandeRole from '../components/DemandeRole'
import { libelleRole, type Utilisateur } from '../types'

/** Tableau de bord de base : affiche le profil tel que VÉRIFIÉ par le serveur (GET /api/auth/me). */
export default function Dashboard() {
  const { utilisateur: depuisConnexion, jeton, deconnecter, majUtilisateur } = useAuth()
  const navigate = useNavigate()
  const [profil, setProfil] = useState<Utilisateur | null>(null)
  const [erreur, setErreur] = useState<string | null>(null)

  useEffect(() => {
    let annule = false
    appelApi<Utilisateur>('/api/auth/me', { token: jeton })
      .then((p) => {
        if (annule) return
        setProfil(p)
        // Le rôle a pu changer côté serveur (demande approuvée) : on aligne l'interface.
        if (p.role !== depuisConnexion?.role) majUtilisateur(p)
      })
      .catch((e: unknown) => {
        if (annule) return
        if (e instanceof ApiError && e.status === 401) {
          // Jeton expiré ou compte désactivé : retour à la connexion.
          deconnecter()
          navigate('/connexion', { replace: true })
        } else {
          setErreur(e instanceof ApiError ? e.message : 'Impossible de charger votre profil.')
        }
      })
    return () => {
      annule = true
    }
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [jeton, deconnecter, navigate, majUtilisateur])

  const affiche = profil ?? depuisConnexion
  if (!affiche) return null

  return (
    <div className="mx-auto max-w-3xl px-4 py-12">
      <h1 className="text-2xl font-bold">Bonjour, {affiche.nomComplet}</h1>

      {erreur && (
        <p role="alert" className="mt-4 rounded-lg bg-red-50 p-3 text-sm text-red-800">
          {erreur}
        </p>
      )}
      {!profil && !erreur && (
        <p role="status" className="mt-2 text-sm text-slate-500">
          Vérification de votre session…
        </p>
      )}

      <dl className="mt-6 grid gap-4 rounded-2xl bg-white p-5 shadow sm:grid-cols-2">
        <div>
          <dt className="text-sm text-slate-500">Email</dt>
          <dd className="font-medium">{affiche.email}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500">Rôle</dt>
          <dd className="font-medium">{libelleRole[affiche.role]}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500">Téléphone</dt>
          <dd className="font-medium">{affiche.telephone ?? 'Non renseigné'}</dd>
        </div>
        <div>
          <dt className="text-sm text-slate-500">Membre depuis</dt>
          <dd className="font-medium">{new Date(affiche.creeLe).toLocaleDateString('fr-FR')}</dd>
        </div>
      </dl>

      {affiche.role === 'CLIENT' && <DemandeRole />}

      {affiche.role === 'ADMIN' && (
        <Link to="/admin/demandes" className="mt-6 mr-3 inline-block rounded-lg border border-slate-300 bg-white px-4 py-2.5 font-semibold">
          Demandes de rôle
        </Link>
      )}

      {['PROPRIETAIRE', 'AGENT', 'ADMIN'].includes(affiche.role) && (
        <Link to="/mes-annonces" className="mt-6 inline-block rounded-lg bg-emeraude-600 px-4 py-2.5 font-semibold text-white hover:bg-emeraude-700">
          Gérer mes annonces
        </Link>
      )}

      <p className="mt-6 rounded-lg border border-dore-500/50 bg-amber-50 p-4 text-sm text-slate-800">
        Les visites, offres, messages et paiements ne sont pas encore disponibles : ils arrivent aux
        prochaines étapes.
      </p>
    </div>
  )
}
