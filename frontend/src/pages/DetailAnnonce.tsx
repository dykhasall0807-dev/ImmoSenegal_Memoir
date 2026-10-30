import { Link, useParams } from 'react-router-dom'
import { ArrowLeft, BedDouble, MapPin, Ruler, UserRound } from 'lucide-react'
import { useApi } from '../useApi'
import { formatFcfa } from '../format'
import { libelleStatut, libelleTypeBien, type Annonce } from '../types'
import { Chargement, MessageErreur } from '../components/ListeAnnonces'
import Galerie from '../components/Galerie'

export default function DetailAnnonce() {
  const { id = '' } = useParams()
  const { data: a, erreur, chargement } = useApi<Annonce>(`/api/annonces/${encodeURIComponent(id)}`)

  return (
    <div className="mx-auto max-w-4xl px-4 py-10">
      <Link to="/catalogue" className="inline-flex items-center gap-1 text-sm font-medium text-emeraude-700 hover:underline">
        <ArrowLeft aria-hidden className="h-4 w-4" />
        Retour au catalogue
      </Link>

      {chargement && <Chargement texte="Chargement de l'annonce…" />}

      {erreur && (
        <div className="mt-6">
          {erreur.status === 404 || erreur.status === 400 ? (
            <div className="rounded-lg border border-slate-200 bg-white p-8 text-center">
              <h1 className="text-xl font-bold">Annonce introuvable</h1>
              <p className="mt-2 text-slate-600">Cette annonce n'existe pas ou a été retirée.</p>
            </div>
          ) : (
            <MessageErreur message={erreur.message} />
          )}
        </div>
      )}

      {a && (
        <article className="mt-6">
          <Galerie annonce={a} />

          <div className="mt-4 flex flex-wrap items-start justify-between gap-3">
            <div>
              <p className="text-xs font-medium uppercase tracking-wide text-slate-500">
                {libelleTypeBien[a.typeBien]} · {libelleStatut[a.statut]}
              </p>
              <h1 className="mt-1 text-2xl font-bold">{a.titre}</h1>
              <p className="mt-1 flex items-center gap-1 text-slate-600">
                <MapPin aria-hidden className="h-4 w-4" />
                {a.quartier}, {a.ville}
                {a.adresse ? ` — ${a.adresse}` : ''}
              </p>
            </div>
            <p className="text-2xl font-extrabold text-emeraude-700">
              {formatFcfa(a.prix)}
              {a.transaction === 'LOCATION' && <span className="text-base font-normal text-slate-500"> / mois</span>}
            </p>
          </div>

          <dl className="mt-6 grid gap-4 rounded-2xl bg-white p-5 shadow sm:grid-cols-3">
            <div className="flex items-center gap-2">
              <Ruler aria-hidden className="h-5 w-5 text-emeraude-700" />
              <div>
                <dt className="text-sm text-slate-500">Superficie</dt>
                <dd className="font-medium">{a.superficie} m²</dd>
              </div>
            </div>
            {a.pieces !== null && (
              <div className="flex items-center gap-2">
                <BedDouble aria-hidden className="h-5 w-5 text-emeraude-700" />
                <div>
                  <dt className="text-sm text-slate-500">Pièces</dt>
                  <dd className="font-medium">{a.pieces}</dd>
                </div>
              </div>
            )}
            <div className="flex items-center gap-2">
              <UserRound aria-hidden className="h-5 w-5 text-emeraude-700" />
              <div>
                <dt className="text-sm text-slate-500">Publié par</dt>
                <dd className="font-medium">{a.proprietaire.nomComplet}</dd>
              </div>
            </div>
          </dl>

          {a.description && (
            <section className="mt-6" aria-labelledby="titre-description">
              <h2 id="titre-description" className="text-lg font-semibold">Description</h2>
              <p className="mt-2 whitespace-pre-line text-slate-700">{a.description}</p>
            </section>
          )}

          {a.equipements.length > 0 && (
            <section className="mt-6" aria-labelledby="titre-equipements">
              <h2 id="titre-equipements" className="text-lg font-semibold">Équipements</h2>
              <ul className="mt-2 flex flex-wrap gap-2">
                {a.equipements.map((e) => (
                  <li key={e} className="rounded-full bg-emeraude-50 px-3 py-1 text-sm text-emeraude-700">{e}</li>
                ))}
              </ul>
            </section>
          )}

          <p className="mt-8 rounded-lg border border-dore-500/50 bg-amber-50 p-4 text-sm text-slate-800">
            La carte, la demande de visite et la messagerie ne sont pas encore disponibles : elles arrivent aux
            prochaines étapes.
          </p>
        </article>
      )}
    </div>
  )
}
