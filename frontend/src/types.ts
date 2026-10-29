export type Role = 'CLIENT' | 'PROPRIETAIRE' | 'AGENT' | 'ADMIN'

export interface Utilisateur {
  id: string
  email: string
  nomComplet: string
  telephone: string | null
  role: Role
  creeLe: string
}

export interface ReponseConnexion {
  accessToken: string
  tokenType: string
  expiresInSeconds: number
  user: Utilisateur
}

export const libelleRole: Record<Role, string> = {
  CLIENT: 'Client',
  PROPRIETAIRE: 'Propriétaire',
  AGENT: 'Agent immobilier',
  ADMIN: 'Administrateur',
}
