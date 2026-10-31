import { Link } from 'react-router-dom'
import SearchForm from '../components/SearchForm'
import ListeAnnonces from '../components/ListeAnnonces'
import QuartierCrepuscule from '../components/QuartierCrepuscule'
import { useAnnonces } from '../components/useAnnonces'
import { useAuth } from '../auth'

/** Fonctions pas encore disponibles : affichées comme telles, jamais comme si elles existaient. */
const aVenir = [
  'Demande de visite en ligne',
  'Messagerie avec le propriétaire',
  'Carte des biens',
  "Badge « annonce vérifiée »",
  'Assistant de recherche',
]

const boutonPlein =
  'inline-flex items-center rounded-full bg-dore-500 px-6 py-3 font-semibold text-nuit-950 transition-colors hover:bg-dore-600'
const boutonContour =
  'inline-flex items-center rounded-full border border-white/50 px-6 py-3 font-semibold text-white transition-colors hover:bg-white/10'

export default function Home() {
  const { etat, reessayer } = useAnnonces('taille=3&tri=recent&statut=DISPONIBLE')
  const { utilisateur } = useAuth()

  return (
    <>
      <section
        aria-labelledby="titre-accueil"
        className="ciel-crepuscule relative isolate overflow-hidden pb-[clamp(13rem,29vw,20rem)] pt-14 text-white sm:pt-20"
      >
        <QuartierCrepuscule className="absolute inset-x-0 bottom-0 -z-10 h-[clamp(18rem,34vw,28rem)] w-full" />
        <div className="mx-auto max-w-6xl px-4">
          <h1
            id="titre-accueil"
            className="font-titre max-w-[11ch] text-5xl font-semibold leading-[1.02] tracking-tight sm:text-7xl"
          >
            Rentrez enfin chez vous.
          </h1>
          <p className="mt-5 max-w-md text-lg text-nuit-100 sm:text-xl">
            Maisons, appartements, studios, villas et terrains à louer ou à acheter, partout au Sénégal.
          </p>
          <div className="mt-8 flex flex-wrap gap-3">
            <Link to="/catalogue" className={boutonPlein}>
              Voir les logements
            </Link>
            {utilisateur ? (
              <Link to="/tableau-de-bord" className={boutonContour}>
                Mon espace
              </Link>
            ) : (
              <Link to="/inscription" className={boutonContour}>
                Créer mon compte
              </Link>
            )}
          </div>
        </div>
      </section>

      <div className="relative z-20 mx-auto -mt-12 max-w-6xl px-4">
        <SearchForm />
      </div>

      <section className="mx-auto max-w-6xl px-4 py-14" aria-labelledby="titre-annonces">
        <div className="flex flex-wrap items-end justify-between gap-2">
          <h2 id="titre-annonces" className="font-titre text-3xl font-semibold">
            Annonces récentes
          </h2>
          <Link to="/catalogue" className="text-sm font-semibold text-emeraude-700 hover:underline">
            Voir tout le catalogue
          </Link>
        </div>
        <div className="mt-6">
          <ListeAnnonces
            etat={etat}
            reessayer={reessayer}
            vide="Aucune annonce n'est encore publiée. Vous avez un bien à proposer ? Créez un compte : la première annonce peut être la vôtre."
          />
        </div>
      </section>

      <section className="mx-auto max-w-6xl px-4 pb-16" aria-labelledby="titre-parcours">
        <h2 id="titre-parcours" className="font-titre text-3xl font-semibold sm:text-4xl">
          Vous cherchez, ou vous proposez ?
        </h2>
        <div className="mt-8 grid gap-6 lg:grid-cols-2">
          <div className="rounded-3xl bg-white p-7 ring-1 ring-slate-200">
            <h3 className="font-titre text-2xl font-semibold">Je cherche un logement</h3>
            <ul className="mt-4 space-y-3 text-slate-700">
              <li>Parcourez toutes les annonces sans créer de compte.</li>
              <li>Filtrez par type de bien, quartier et budget.</li>
              <li>Consultez les photos et le détail de chaque bien.</li>
            </ul>
            <Link
              to="/catalogue"
              className="mt-6 inline-flex rounded-full bg-nuit-900 px-5 py-2.5 font-semibold text-white transition-colors hover:bg-nuit-800"
            >
              Parcourir le catalogue
            </Link>
          </div>

          <div className="rounded-3xl bg-nuit-900 p-7 text-white">
            <h3 className="font-titre text-2xl font-semibold">Je propose un bien</h3>
            <ol className="mt-4 space-y-4">
              {[
                'Créez votre compte : c’est gratuit.',
                'Demandez le rôle de propriétaire. Un administrateur examine votre demande.',
                'Publiez votre annonce avec jusqu’à 8 photos.',
              ].map((texte, i) => (
                <li key={texte} className="flex gap-3">
                  <span
                    aria-hidden
                    className="flex h-7 w-7 shrink-0 items-center justify-center rounded-full bg-dore-500 text-sm font-bold text-nuit-950"
                  >
                    {i + 1}
                  </span>
                  <span className="text-nuit-100">{texte}</span>
                </li>
              ))}
            </ol>
            <Link to={utilisateur ? '/tableau-de-bord' : '/inscription'} className={`${boutonPlein} mt-6`}>
              {utilisateur ? 'Aller à mon espace' : 'Créer mon compte'}
            </Link>
          </div>
        </div>
      </section>

      <section className="border-t border-slate-200 bg-white py-10" aria-labelledby="titre-avenir">
        <div className="mx-auto max-w-6xl px-4">
          <h2 id="titre-avenir" className="font-titre text-2xl font-semibold">
            À venir
          </h2>
          <p className="mt-2 max-w-2xl text-slate-600">Ces fonctions ne sont pas encore disponibles.</p>
          <ul className="mt-4 flex flex-wrap gap-2">
            {aVenir.map((nom) => (
              <li key={nom} className="rounded-full bg-slate-100 px-3.5 py-1.5 text-sm text-slate-700">
                {nom}
              </li>
            ))}
          </ul>
        </div>
      </section>

      {!utilisateur && (
        <section className="bg-gradient-to-br from-nuit-950 via-nuit-900 to-nuit-800 py-16 text-white" aria-labelledby="titre-fin">
          <div className="mx-auto flex max-w-6xl flex-col items-start gap-6 px-4 sm:flex-row sm:items-center sm:justify-between">
            <div>
              <h2 id="titre-fin" className="font-titre text-3xl font-semibold sm:text-4xl">
                Ouvrez la porte.
              </h2>
              <p className="mt-2 max-w-md text-nuit-100">
                Créer un compte est gratuit : c’est la première étape pour publier une annonce.
              </p>
            </div>
            <Link to="/inscription" className={boutonPlein}>
              Créer mon compte
            </Link>
          </div>
        </section>
      )}
    </>
  )
}
