import { createContext, useCallback, useContext, useEffect, useMemo, useState, type ReactNode } from 'react'
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
  /** false tant que la session éventuelle n'a pas été restaurée au chargement de la page. */
  pret: boolean
  connecter: (email: string, password: string) => Promise<void>
  inscrire: (donnees: DonneesInscription) => Promise<void>
  deconnecter: () => void
  /** Met à jour le profil gardé en mémoire avec celui vérifié par le serveur (ex. rôle approuvé entre-temps). */
  majUtilisateur: (u: Utilisateur) => void
}

const Contexte = createContext<ContexteAuth | null>(null)

/** Marge avant l'expiration du jeton d'accès pour le renouveler discrètement. */
const MARGE_SECONDES = 60

// Une seule demande de renouvellement à la fois : le jeton de rafraîchissement est à usage unique.
let renouvellementEnCours: Promise<ReponseConnexion | null> | null = null

function renouveler(): Promise<ReponseConnexion | null> {
  if (!renouvellementEnCours) {
    renouvellementEnCours = appelApi<ReponseConnexion>('/api/auth/refresh', { method: 'POST', cookie: true })
      .catch(() => null)
      .finally(() => {
        renouvellementEnCours = null
      })
  }
  return renouvellementEnCours
}

export function AuthProvider({ children }: { children: ReactNode }) {
  const [session, setSession] = useState<{ utilisateur: Utilisateur; jeton: string; expireDans: number } | null>(null)
  const [pret, setPret] = useState(false)

  const ouvrir = (r: ReponseConnexion) => setSession({ utilisateur: r.user, jeton: r.accessToken, expireDans: r.expiresInSeconds })

  // Au chargement : si le cookie de session est encore valide, on retrouve la connexion (sinon, visiteur).
  useEffect(() => {
    let annule = false
    renouveler().then((r) => {
      if (annule) return
      if (r) ouvrir(r)
      setPret(true)
    })
    return () => {
      annule = true
    }
  }, [])

  // Renouvellement discret avant l'expiration du jeton d'accès ; si le serveur refuse, retour visiteur.
  useEffect(() => {
    if (!session) return
    const delai = Math.max(session.expireDans - MARGE_SECONDES, 30) * 1000
    const minuterie = setTimeout(() => {
      renouveler().then((r) => (r ? ouvrir(r) : setSession(null)))
    }, delai)
    return () => clearTimeout(minuterie)
  }, [session?.jeton]) // eslint-disable-line react-hooks/exhaustive-deps

  const connecter = useCallback(async (email: string, password: string) => {
    const reponse = await appelApi<ReponseConnexion>('/api/auth/login', { method: 'POST', body: { email, password }, cookie: true })
    setSession({ utilisateur: reponse.user, jeton: reponse.accessToken, expireDans: reponse.expiresInSeconds })
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

  const deconnecter = useCallback(() => {
    // Déconnexion locale immédiate ; la révocation côté serveur se fait en parallèle (sans bloquer l'interface).
    setSession(null)
    void appelApi('/api/auth/logout', { method: 'POST', cookie: true }).catch(() => undefined)
  }, [])
  const majUtilisateur = useCallback((u: Utilisateur) => setSession((s) => (s ? { ...s, utilisateur: u } : s)), [])

  const valeur = useMemo<ContexteAuth>(
    () => ({
      utilisateur: session?.utilisateur ?? null,
      jeton: session?.jeton ?? null,
      pret,
      connecter,
      inscrire,
      deconnecter,
      majUtilisateur,
    }),
    [session, pret, connecter, inscrire, deconnecter, majUtilisateur],
  )

  return <Contexte.Provider value={valeur}>{children}</Contexte.Provider>
}

export function useAuth(): ContexteAuth {
  const contexte = useContext(Contexte)
  if (!contexte) throw new Error('useAuth doit être utilisé dans <AuthProvider>.')
  return contexte
}
