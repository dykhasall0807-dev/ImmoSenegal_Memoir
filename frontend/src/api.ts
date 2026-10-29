// URL de l'API : vide en local (le proxy Vite redirige /api vers le backend), renseignée en production.
const BASE = String(import.meta.env.VITE_API_URL ?? '').replace(/\/$/, '')

export class ApiError extends Error {
  readonly status: number
  readonly details: Record<string, string>
  readonly retryAfter?: number

  constructor(status: number, message: string, details: Record<string, string> = {}, retryAfter?: number) {
    super(message)
    this.name = 'ApiError'
    this.status = status
    this.details = details
    this.retryAfter = retryAfter
  }
}

interface Options {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  token?: string | null
}

function messageParDefaut(status: number): string {
  if (status === 401) return 'Session expirée ou identifiants incorrects.'
  if (status === 403) return "Vous n'avez pas le droit d'effectuer cette action."
  if (status === 404) return 'Élément introuvable.'
  if (status === 429) return 'Trop de tentatives. Réessayez plus tard.'
  if (status >= 500) return 'Le serveur a rencontré un problème. Réessayez dans un instant.'
  return 'La requête a échoué.'
}

/** Appelle l'API et transforme toute erreur en ApiError avec un message lisible. */
export async function appelApi<T>(chemin: string, { method = 'GET', body, token }: Options = {}): Promise<T> {
  const entetes: Record<string, string> = { Accept: 'application/json' }
  if (body !== undefined) entetes['Content-Type'] = 'application/json'
  if (token) entetes.Authorization = `Bearer ${token}`

  let reponse: Response
  try {
    reponse = await fetch(`${BASE}${chemin}`, {
      method,
      headers: entetes,
      body: body === undefined ? undefined : JSON.stringify(body),
    })
  } catch {
    throw new ApiError(0, 'Impossible de joindre le serveur. Vérifiez votre connexion internet.')
  }

  if (!reponse.ok) {
    let donnees: { message?: string; details?: Record<string, string> } | null = null
    try {
      donnees = await reponse.json()
    } catch {
      donnees = null
    }
    const retry = reponse.headers.get('Retry-After')
    throw new ApiError(
      reponse.status,
      donnees?.message ?? messageParDefaut(reponse.status),
      donnees?.details ?? {},
      retry ? Number(retry) : undefined,
    )
  }
  return (await reponse.json()) as T
}
