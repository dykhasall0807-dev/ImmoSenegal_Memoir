import AnnonceCard from './AnnonceCard'
import type { EtatListe } from './useAnnonces'

export function Chargement({ texte = 'Chargement…' }: { texte?: string }) {
  return (
    <p role="status" className="py-10 text-center text-slate-500">
      {texte}
    </p>
  )
}

export function MessageErreur({ message, reessayer }: { message: string; reessayer?: () => void }) {
  return (
    <div role="alert" className="rounded-lg bg-red-50 p-4 text-red-800">
      <p>{message}</p>
      {reessayer && (
        <button
          type="button"
          onClick={reessayer}
          className="mt-3 rounded-lg bg-red-700 px-4 py-2 text-sm font-semibold text-white hover:bg-red-800"
        >
          Réessayer
        </button>
      )}
    </div>
  )
}

/** Affiche les états d'une liste d'annonces : chargement, erreur (avec « Réessayer »), vide, résultats. */
export default function ListeAnnonces({ etat, reessayer, vide }: { etat: EtatListe; reessayer: () => void; vide: string }) {
  if (etat.statut === 'chargement') return <Chargement texte="Chargement des annonces…" />
  if (etat.statut === 'erreur') return <MessageErreur message={etat.message} reessayer={reessayer} />
  if (etat.page.contenu.length === 0) {
    return <p className="rounded-lg bg-white p-6 text-center text-slate-600 shadow-sm">{vide}</p>
  }
  return (
    <div className="grid gap-5 sm:grid-cols-2 lg:grid-cols-3">
      {etat.page.contenu.map((a) => (
        <AnnonceCard key={a.id} annonce={a} />
      ))}
    </div>
  )
}
