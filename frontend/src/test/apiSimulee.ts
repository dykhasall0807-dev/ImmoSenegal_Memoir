import { vi } from 'vitest'
import type { Annonce, PagePaginee, Utilisateur } from '../types'

export const utilisateurTest: Utilisateur = {
  id: '11111111-1111-1111-1111-111111111111',
  email: 'khady@example.test',
  nomComplet: 'Khady Test',
  telephone: null,
  role: 'CLIENT',
  creeLe: '2026-10-28T10:00:00Z',
}

type Gestionnaire = (init?: RequestInit) => Response | Promise<Response>

export function json(statut: number, corps: unknown, entetes: Record<string, string> = {}): Response {
  return new Response(JSON.stringify(corps), {
    status: statut,
    headers: { 'Content-Type': 'application/json', ...entetes },
  })
}

/** Remplace fetch par une API simulée. Clé = « METHODE /chemin ». Retourne le mock pour inspecter les appels. */
export function simulerApi(routes: Record<string, Gestionnaire>) {
  const mock = vi.fn(async (url: string, init?: RequestInit) => {
    const cle = `${init?.method ?? 'GET'} ${url}`
    // Une clé peut se terminer par « * » pour accepter n'importe quelle suite (ex. paramètres de recherche).
    const gestionnaire = routes[cle] ?? Object.entries(routes).find(([k]) => k.endsWith('*') && cle.startsWith(k.slice(0, -1)))?.[1]
    if (!gestionnaire) throw new Error(`Appel non prévu dans le test : ${cle}`)
    return gestionnaire(init)
  })
  // Au chargement, l'application tente de restaurer la session (POST /api/auth/refresh). Sans route dédiée, la réponse
  // par défaut est « pas de session » et l'appel n'est pas compté, pour ne pas fausser les assertions des tests.
  const sansSession = json(401, { message: 'Session expirée. Veuillez vous reconnecter.' })
  vi.stubGlobal('fetch', (url: string, init?: RequestInit) =>
    url === '/api/auth/refresh' && !routes['POST /api/auth/refresh'] ? Promise.resolve(sansSession.clone()) : mock(url, init),
  )
  return mock
}

export const reponseConnexion = (u: Utilisateur = utilisateurTest) =>
  json(200, { accessToken: 'jeton-de-test', tokenType: 'Bearer', expiresInSeconds: 900, user: u })

export function annonceTest(surcharge: Partial<Annonce> = {}): Annonce {
  return {
    id: 'a1111111-1111-1111-1111-111111111111',
    titre: 'Appartement lumineux',
    description: 'Un bel appartement.\nProche du marché.',
    typeBien: 'APPARTEMENT',
    transaction: 'LOCATION',
    prix: 120000,
    superficie: 85,
    pieces: 3,
    quartier: 'Kandé',
    ville: 'Ziguinchor',
    adresse: 'Rue 12',
    latitude: null,
    longitude: null,
    equipements: ['climatisation', 'wifi'],
    statut: 'DISPONIBLE',
    proprietaire: { id: 'p1', nomComplet: 'Awa Diallo' },
    creeLe: '2026-10-28T10:00:00Z',
    modifieLe: '2026-10-28T10:00:00Z',
    ...surcharge,
  }
}

export function pageTest(contenu: Annonce[], extra: Partial<PagePaginee<Annonce>> = {}): PagePaginee<Annonce> {
  return { contenu, page: 0, taille: 12, totalElements: contenu.length, totalPages: 1, ...extra }
}

/** Paramètres de recherche réellement envoyés au serveur dans un appel donné. */
export function parametresDe(url: string): URLSearchParams {
  return new URL(url, 'http://localhost').searchParams
}
