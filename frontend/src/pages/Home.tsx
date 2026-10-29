import { ShieldCheck, MessageCircle, MapPinned } from 'lucide-react'
import { Link } from 'react-router-dom'
import SearchForm from '../components/SearchForm'
import ListeAnnonces from '../components/ListeAnnonces'
import { useAnnonces } from '../components/useAnnonces'

const atouts = [
  { icone: ShieldCheck, titre: 'Annonces vérifiées', texte: "Un badge n'est attribué qu'après examen des justificatifs par un administrateur." },
  { icone: MapPinned, titre: 'Recherche sur carte', texte: 'Trouvez un logement près de vous avec une carte libre (OpenStreetMap).' },
  { icone: MessageCircle, titre: 'Assistant de recherche', texte: 'Décrivez vos besoins, recevez une sélection issue des annonces réelles.' },
]

export default function Home() {
  const { etat, reessayer } = useAnnonces('taille=3&tri=recent&statut=DISPONIBLE')
  return (
    <>
      <section className="bg-gradient-to-br from-nuit-950 via-nuit-900 to-nuit-800 px-4 pb-16 pt-12 text-white">
        <div className="mx-auto max-w-6xl">
          <h1 className="max-w-2xl text-3xl font-extrabold leading-tight sm:text-5xl">
            Trouvez votre logement au <span className="text-dore-500">Sénégal</span>, en toute confiance
          </h1>
          <p className="mt-4 max-w-xl text-lg text-nuit-100">
            Location et vente de maisons, appartements, studios, villas et terrains.
          </p>
          <div className="mt-8">
            <SearchForm />
          </div>
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 py-12" aria-labelledby="titre-annonces">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <h2 id="titre-annonces" className="text-2xl font-bold">
            Annonces récentes
          </h2>
          <Link to="/catalogue" className="text-sm font-semibold text-emeraude-700 hover:underline">
            Voir tout le catalogue
          </Link>
        </div>
        <div className="mt-6">
          <ListeAnnonces etat={etat} reessayer={reessayer} vide="Aucune annonce n'est encore publiée." />
        </div>
      </section>

      <section className="bg-white px-4 py-12" aria-labelledby="titre-atouts">
        <div className="mx-auto max-w-6xl">
          <h2 id="titre-atouts" className="text-2xl font-bold">
            Ce que propose ImmoSénégal
          </h2>
          <ul className="mt-6 grid gap-6 sm:grid-cols-3">
            {atouts.map(({ icone: Icone, titre, texte }) => (
              <li key={titre} className="flex gap-3">
                <span className="flex h-10 w-10 shrink-0 items-center justify-center rounded-lg bg-emeraude-50 text-emeraude-700">
                  <Icone aria-hidden className="h-5 w-5" />
                </span>
                <div>
                  <h3 className="font-semibold">
                    {titre}{' '}
                    <span className="ml-1 rounded-full bg-slate-200 px-2 py-0.5 align-middle text-xs font-medium text-slate-700">
                      Bientôt
                    </span>
                  </h3>
                  <p className="text-sm text-slate-600">{texte}</p>
                </div>
              </li>
            ))}
          </ul>
        </div>
      </section>
    </>
  )
}
