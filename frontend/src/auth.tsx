import { createContext, useCallback, useContext, useMemo, useState, type ReactNode } from 'react'
import { appelApi } from './api'
import type { ReponseConnexion, Utilisateur } from './types'

interface DonneesInscription {
  nomComplet: string
  email: string
  telephone: string
  password: string
}

interface ContexteAuth {
  utilisateur: Utilisateur | null
  /** Jeton d'accès, gardé UNIQUEMENT en mémoire (jamais dans localStorage/sessionStorage). */
  jeton: string | null
  connecter: (email: string, password: string) => Promise<void>
  inscrire: (donnees: DonneesInscription) => Promise<void>
  deconnecter: () => void
}

const Contexte = createContext<ContexteAuth | null>(null)

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<{ utilisateur: Utilisateur; jeton: string } | null>(null)

  const connecter = useCallback(async (email: string, password: string) => {
    const reponse = await appelApi<ReponseConnexion>('/api/auth/login', { method: 'POST', body: { email, password } })
    setSession({ utilisateur: reponse.user, jeton: reponse.accessToken })
  }, [])

  const inscrire = useCallback(
    async (donnees: DonneesInscription) => {
      await appelApi<Utilisateur>('/api/auth/register', {
        method: 'POST',
        body: {
          email: donnees.email,
          password: donnees.password,
          nomComplet: donnees.nomComplet,
          telephone: donnees.telephone.trim() === '' ? null : donnees.telephone,
        },
      })
      await connecter(donnees.email, donnees.password)
    },
    [connecter],
  )

  const deconnecter = useCallback(() => setSession(null), [])

  const valeur = useMemo<ContexteAuth>(
    () => ({
      utilisateur: session?.utilisateur ?? null,
      jeton: session?.jeton ?? null,
      connecter,
      inscrire,
      deconnecter,
    }),
    [session, connecter, inscrire, deconnecter],
  )

  return <Contexte.Provider value={valeur}>{children}</Contexte.Provider>
}

export function useAuth(): ContexteAuth {
  const contexte = useContext(Contexte)
  if (!contexte) throw new Error('useAuth doit être utilisé dans <AuthProvider>.')
  return contexte
}
