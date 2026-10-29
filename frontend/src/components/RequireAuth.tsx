import { Navigate, useLocation } from 'react-router-dom'
import type { ReactNode } from 'react'
import { useAuth } from '../auth'

/** Redirige vers /connexion si personne n'est connecté. (La vraie protection reste côté serveur.) */
export default function RequireAuth({ children }: { children: ReactNode }) {
  const { utilisateur } = useAuth()
  const location = useLocation()
  if (!utilisateur) {
    return <Navigate to="/connexion" replace state={{ from: location.pathname }} />
  }
  return <>{children}</>
}
