import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../auth'

/** Redirige vers /connexion si personne n'est connecté. (La vraie protection reste côté serveur.) */
export default function RequireAuth({ children }: { children: ReactNode }) {
  const { utilisateur, pret } = useAuth()
  const location = useLocation()
  if (!pret) return <p role="status" className="p-8 text-center text-slate-500">Chargement…</p>
  if (!utilisateur) {
    return <Navigate to="/connexion" replace state={{ from: location.pathname }} />
  }
  return <>{children}</>
}
