import { useState } from 'react'
import type { Annonce } from '../types'
import { urlPhoto } from './photos'

/** Photo principale + vignettes cliquables. Sans photo : bandeau coloré et mention claire. */
export default function Galerie({ annonce }: { annonce: Annonce }) {
  const [courante, setCourante] = useState(0)
  const badge = (
    <span className="relative rounded-full bg-dore-500 px-3 py-1 text-sm font-semibold text-nuit-950">
      {annonce.transaction === 'LOCATION' ? 'À louer' : 'À vendre'}
    </span>
  )

  if (annonce.photos.length === 0) {
    return (
      <>
        <div aria-hidden className="flex h-48 items-end rounded-2xl bg-gradient-to-br from-nuit-800 to-emeraude-700 p-4 sm:h-64">
          {badge}
        </div>
        <p className="mt-2 text-xs text-slate-500">Aucune photo pour cette annonce.</p>
      </>
    )
  }

  const index = Math.min(courante, annonce.photos.length - 1)
  return (
    <section aria-label="Photos de l'annonce">
      <div className="relative flex h-64 items-end overflow-hidden rounded-2xl bg-slate-200 p-4 sm:h-96">
        <img
          src={urlPhoto(annonce.id, annonce.photos[index])}
          alt={`Photo ${index + 1} sur ${annonce.photos.length} : ${annonce.titre}`}
          className="absolute inset-0 h-full w-full object-cover"
        />
        {badge}
      </div>
      {annonce.photos.length > 1 && (
        <ul className="mt-2 flex gap-2 overflow-x-auto pb-1">
          {annonce.photos.map((photo, i) => (
            <li key={photo} className="shrink-0">
              <button
                type="button"
                onClick={() => setCourante(i)}
                aria-label={`Voir la photo ${i + 1}`}
                aria-current={i === index ? 'true' : undefined}
                className={`block overflow-hidden rounded-lg border-2 ${i === index ? 'border-emeraude-600' : 'border-transparent'}`}
              >
                <img src={urlPhoto(annonce.id, photo)} alt="" loading="lazy" className="h-16 w-20 object-cover" />
              </button>
            </li>
          ))}
        </ul>
      )}
    </section>
  )
}
