import { useSearchParams } from 'react-router-dom'
import type { FormEvent } from 'react'
import { Search } from 'lucide-react'
import { parametresApi } from '../annonces'
import ListeAnnonces from '../components/ListeAnnonces'
import { useAnnonces } from '../components/useAnnonces'

const champ = 'w-full rounded-lg border border-slate-300 bg-white px-3 py-2 text-slate-900 focus:border-emeraude-600'
const etiquette = 'mb-1 block text-sm font-medium text-slate-700'

export default function Catalogue() {
  const [params, setParams] = useSearchParams()
  const requete = parametresApi(params).toString()
  const { etat, reessayer } = useAnnonces(requete)
  const page = Number(params.get('page') ?? 0) || 0

  function appliquer(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const donnees = new FormData(e.currentTarget)
    const suivants = new URLSearchParams()
    for (const nom of ['transaction', 'type', 'quartier', 'ville', 'prixMin', 'prixMax', 'superficieMin', 'piecesMin', 'equipements', 'tri']) {
      const valeur = String(donnees.get(nom) ?? '').trim()
      if (valeur) suivants.set(nom, valeur)
    }
    setParams(suivants) // une nouvelle recherche repart de la page 0
  }

  function allerPage(numero: number) {
    const suivants = new URLSearchParams(params)
    suivants.set('page', String(numero))
    setParams(suivants)
  }

  return (
    <div className="mx-auto max-w-6xl px-4 py-8">
      <h1 className="text-2xl font-bold">Catalogue des annonces</h1>

      {/* key : le formulaire se réinitialise quand l'URL change (ex. arrivée depuis la recherche de l'accueil). */}
      <form key={params.toString()} onSubmit={appliquer} aria-label="Filtres du catalogue" className="mt-4 rounded-2xl bg-white p-4 shadow-sm">
        <details open>
          <summary className="cursor-pointer font-semibold">Filtres</summary>
          <div className="mt-4 grid gap-3 sm:grid-cols-2 lg:grid-cols-4">
            <div>
              <label htmlFor="f-transaction" className={etiquette}>Transaction</label>
              <select id="f-transaction" name="transaction" defaultValue={params.get('transaction') ?? ''} className={champ}>
                <option value="">Location et vente</option>
                <option value="LOCATION">Location</option>
                <option value="VENTE">Vente</option>
              </select>
            </div>
            <div>
              <label htmlFor="f-type" className={etiquette}>Type de bien</label>
              <select id="f-type" name="type" defaultValue={params.get('type') ?? ''} className={champ}>
                <option value="">Tous les types</option>
                <option value="MAISON">Maison</option>
                <option value="APPARTEMENT">Appartement</option>
                <option value="STUDIO">Studio</option>
                <option value="VILLA">Villa</option>
                <option value="TERRAIN">Terrain</option>
              </select>
            </div>
            <div>
              <label htmlFor="f-quartier" className={etiquette}>Quartier</label>
              <input id="f-quartier" name="quartier" defaultValue={params.get('quartier') ?? ''} className={champ} />
            </div>
            <div>
              <label htmlFor="f-ville" className={etiquette}>Ville</label>
              <input id="f-ville" name="ville" defaultValue={params.get('ville') ?? ''} className={champ} />
            </div>
            <div>
              <label htmlFor="f-prixMin" className={etiquette}>Prix minimum (FCFA)</label>
              <input id="f-prixMin" name="prixMin" type="number" min={0} inputMode="numeric" defaultValue={params.get('prixMin') ?? ''} className={champ} />
            </div>
            <div>
              <label htmlFor="f-prixMax" className={etiquette}>Prix maximum (FCFA)</label>
              <input id="f-prixMax" name="prixMax" type="number" min={0} inputMode="numeric" defaultValue={params.get('prixMax') ?? ''} className={champ} />
            </div>
            <div>
              <label htmlFor="f-superficieMin" className={etiquette}>Superficie minimum (m²)</label>
              <input id="f-superficieMin" name="superficieMin" type="number" min={0} inputMode="numeric" defaultValue={params.get('superficieMin') ?? ''} className={champ} />
            </div>
            <div>
              <label htmlFor="f-piecesMin" className={etiquette}>Pièces (minimum)</label>
              <input id="f-piecesMin" name="piecesMin" type="number" min={0} inputMode="numeric" defaultValue={params.get('piecesMin') ?? ''} className={champ} />
            </div>
            <div className="sm:col-span-2">
              <label htmlFor="f-equipements" className={etiquette}>Équipements (séparés par des virgules)</label>
              <input id="f-equipements" name="equipements" placeholder="wifi, climatisation" defaultValue={params.get('equipements') ?? ''} className={champ} />
            </div>
            <div>
              <label htmlFor="f-tri" className={etiquette}>Trier par</label>
              <select id="f-tri" name="tri" defaultValue={params.get('tri') ?? ''} className={champ}>
                <option value="">Plus récentes</option>
                <option value="prix_asc">Prix croissant</option>
                <option value="prix_desc">Prix décroissant</option>
                <option value="superficie_desc">Plus grande surface</option>
              </select>
            </div>
          </div>
          <div className="mt-4 flex flex-wrap gap-3">
            <button type="submit" className="flex items-center gap-2 rounded-lg bg-emeraude-600 px-4 py-2.5 font-semibold text-white hover:bg-emeraude-700">
              <Search aria-hidden className="h-5 w-5" />
              Appliquer
            </button>
            <button
              type="button"
              onClick={() => setParams(new URLSearchParams())}
              className="rounded-lg border border-slate-300 px-4 py-2.5 font-medium text-slate-700 hover:bg-slate-50"
            >
              Réinitialiser
            </button>
          </div>
        </details>
      </form>

      <div className="mt-6" aria-live="polite">
        {etat.statut === 'ok' && (
          <p className="mb-3 text-sm text-slate-600">
            {etat.page.totalElements === 0
              ? 'Aucune annonce'
              : `${etat.page.totalElements} annonce${etat.page.totalElements > 1 ? 's' : ''}`}
          </p>
        )}
        <ListeAnnonces etat={etat} reessayer={reessayer} vide="Aucune annonce ne correspond à ces critères. Essayez d'élargir votre recherche." />
      </div>

      {etat.statut === 'ok' && etat.page.totalPages > 1 && (
        <nav aria-label="Pagination" className="mt-8 flex items-center justify-center gap-4">
          <button
            type="button"
            onClick={() => allerPage(page - 1)}
            disabled={page <= 0}
            className="rounded-lg border border-slate-300 px-4 py-2 font-medium disabled:cursor-not-allowed disabled:opacity-50"
          >
            Précédent
          </button>
          <span className="text-sm text-slate-600">
            Page {etat.page.page + 1} sur {etat.page.totalPages}
          </span>
          <button
            type="button"
            onClick={() => allerPage(page + 1)}
            disabled={page + 1 >= etat.page.totalPages}
            className="rounded-lg border border-slate-300 px-4 py-2 font-medium disabled:cursor-not-allowed disabled:opacity-50"
          >
            Suivant
          </button>
        </nav>
      )}
    </div>
  )
}
