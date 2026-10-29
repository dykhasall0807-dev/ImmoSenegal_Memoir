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

// ---------- Annonces ----------

export type TypeBien = 'MAISON' | 'APPARTEMENT' | 'STUDIO' | 'VILLA' | 'TERRAIN'
export type TypeTransaction = 'LOCATION' | 'VENTE'
export type StatutAnnonce = 'DISPONIBLE' | 'RESERVE' | 'VENDU' | 'LOUE'

export interface Annonce {
  id: string
  titre: string
  description: string | null
  typeBien: TypeBien
  transaction: TypeTransaction
  prix: number
  superficie: number
  pieces: number | null
  quartier: string
  ville: string
  adresse: string | null
  latitude: number | null
  longitude: number | null
  equipements: string[]
  statut: StatutAnnonce
  proprietaire: { id: string; nomComplet: string }
  creeLe: string
  modifieLe: string
}

export interface PagePaginee<T> {
  contenu: T[]
  page: number
  taille: number
  totalElements: number
  totalPages: number
}

export const libelleTypeBien: Record<TypeBien, string> = {
  MAISON: 'Maison',
  APPARTEMENT: 'Appartement',
  STUDIO: 'Studio',
  VILLA: 'Villa',
  TERRAIN: 'Terrain',
}

export const libelleStatut: Record<StatutAnnonce, string> = {
  DISPONIBLE: 'Disponible',
  RESERVE: 'Réservé',
  VENDU: 'Vendu',
  LOUE: 'Loué',
}
