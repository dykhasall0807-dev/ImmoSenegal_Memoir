import { BedDouble, MapPin, Ruler } from 'lucide-react'
import type { AnnonceDemo } from '../data/demo'
import { formatFcfa } from '../format'

export default function AnnonceCard({ annonce }: { annonce: AnnonceDemo }) {
  const location = annonce.transaction === 'LOCATION'
  return (
    <article className="overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <div
        aria-hidden
        className="relative flex h-36 items-end bg-gradient-to-br from-nuit-800 to-emeraude-700 p-3"
      >
        <span className="rounded-full bg-dore-500 px-2.5 py-0.5 text-xs font-semibold text-nuit-950">
          {location ? 'À louer' : 'À vendre'}
        </span>
        {annonce.demo && (
          <span className="absolute right-3 top-3 rounded-full bg-white/90 px-2.5 py-0.5 text-xs font-semibold text-slate-700">
            Démonstration
          </span>
        )}
      </div>
      <div className="space-y-2 p-4">
        <h3 className="font-semibold leading-snug">{annonce.titre}</h3>
        <p className="flex items-center gap-1 text-sm text-slate-600">
          <MapPin aria-hidden className="h-4 w-4" />
          {annonce.quartier}, {annonce.ville}
        </p>
        <p className="text-lg font-bold text-emeraude-700">
          {formatFcfa(annonce.prix)}
          {location && <span className="text-sm font-normal text-slate-500"> / mois</span>}
        </p>
        <p className="flex items-center gap-4 text-sm text-slate-600">
          <span className="flex items-center gap-1">
            <Ruler aria-hidden className="h-4 w-4" />
            {annonce.superficie} m²
          </span>
          {annonce.pieces !== null && (
            <span className="flex items-center gap-1">
              <BedDouble aria-hidden className="h-4 w-4" />
              {annonce.pieces} pièce{annonce.pieces > 1 ? 's' : ''}
            </span>
          )}
        </p>
      </div>
    </article>
  )
}
