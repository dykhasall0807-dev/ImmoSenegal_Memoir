import { vi } from 'vitest'
import type { Utilisateur } from '../types'

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
    const gestionnaire = routes[cle]
    if (!gestionnaire) throw new Error(`Appel non prévu dans le test : ${cle}`)
    return gestionnaire(init)
  })
  vi.stubGlobal('fetch', mock)
  return mock
}

export const reponseConnexion = (u: Utilisateur = utilisateurTest) =>
  json(200, { accessToken: 'jeton-de-test', tokenType: 'Bearer', expiresInSeconds: 900, user: u })
