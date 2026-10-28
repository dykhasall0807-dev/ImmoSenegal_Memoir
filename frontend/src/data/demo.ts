// DONNÉES DE DÉMONSTRATION : annonces fictives affichées tant que le catalogue n'est pas relié à l'API.
// Elles sont toujours signalées « Démonstration » dans l'interface et seront supprimées à l'étape Catalogue.

export type Transaction = 'LOCATION' | 'VENTE'

export interface AnnonceDemo {
  id: string
  titre: string
  quartier: string
  ville: string
  transaction: Transaction
  prix: number
  superficie: number
  pieces: number | null
  type: string
  demo: true
}

export const annoncesDemo: AnnonceDemo[] = [
  { id: 'demo-1', titre: 'Appartement 3 pièces lumineux', quartier: 'Kandé', ville: 'Ziguinchor', transaction: 'LOCATION', prix: 120000, superficie: 85, pieces: 3, type: 'Appartement', demo: true },
  { id: 'demo-2', titre: 'Villa avec jardin', quartier: 'Néma', ville: 'Ziguinchor', transaction: 'VENTE', prix: 45000000, superficie: 320, pieces: 6, type: 'Villa', demo: true },
  { id: 'demo-3', titre: 'Studio meublé près du campus', quartier: 'Boucotte', ville: 'Ziguinchor', transaction: 'LOCATION', prix: 55000, superficie: 28, pieces: 1, type: 'Studio', demo: true },
  { id: 'demo-4', titre: 'Terrain viabilisé de 300 m²', quartier: 'Kenya', ville: 'Ziguinchor', transaction: 'VENTE', prix: 12000000, superficie: 300, pieces: null, type: 'Terrain', demo: true },
]
