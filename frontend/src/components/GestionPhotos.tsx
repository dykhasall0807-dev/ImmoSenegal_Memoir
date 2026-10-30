import { useRef, useState } from 'react'
import { ApiError, appelApi } from '../api'
import { useAuth } from '../auth'
import { PHOTOS_MAX, TAILLE_MAX_OCTETS, urlPhoto } from './photos'

interface Props {
  annonceId: string
  photos: string[]
  onChange: (photos: string[]) => void
}

/** Ajout et suppression des photos d'une annonce. Le serveur revérifie tout (droits, format réel, taille, nombre). */
export default function GestionPhotos({ annonceId, photos, onChange }: Props) {
  const { jeton } = useAuth()
  const champ = useRef<HTMLInputElement>(null)
  const [erreurs, setErreurs] = useState<string[]>([])
  const [enCours, setEnCours] = useState(false)

  async function ajouter(fichiers: FileList | null) {
    if (!fichiers || fichiers.length === 0) return
    setEnCours(true)
    const problemes: string[] = []
    let courantes = photos
    for (const fichier of Array.from(fichiers)) {
      if (courantes.length >= PHOTOS_MAX) {
        problemes.push(`« ${fichier.name} » : maximum ${PHOTOS_MAX} photos par annonce.`)
        continue
      }
      if (fichier.size > TAILLE_MAX_OCTETS) {
        problemes.push(`« ${fichier.name} » : plus de 5 Mo.`)
        continue
      }
      const donnees = new FormData()
      donnees.append('fichier', fichier)
      try {
        const r = await appelApi<{ id: string }>(`/api/annonces/${annonceId}/photos`, { method: 'POST', body: donnees, token: jeton })
        courantes = [...courantes, r.id]
        onChange(courantes)
      } catch (e) {
        problemes.push(`« ${fichier.name} » : ${e instanceof ApiError ? e.message : 'envoi impossible.'}`)
      }
    }
    setErreurs(problemes)
    setEnCours(false)
    if (champ.current) champ.current.value = ''
  }

  async function supprimer(photo: string) {
    setErreurs([])
    try {
      await appelApi<void>(`/api/annonces/${annonceId}/photos/${photo}`, { method: 'DELETE', token: jeton })
      onChange(photos.filter((p) => p !== photo))
    } catch (e) {
      setErreurs([e instanceof ApiError ? e.message : 'Suppression impossible.'])
    }
  }

  return (
    <section aria-labelledby="titre-photos" className="mt-6 rounded-2xl bg-white p-5 shadow">
      <h2 id="titre-photos" className="text-lg font-semibold">Photos ({photos.length}/{PHOTOS_MAX})</h2>
      <p className="mt-1 text-sm text-slate-600">
        JPEG ou PNG, 5 Mo maximum chacune. La première photo sert de couverture. Les informations cachées dans les photos
        (position GPS, appareil) sont supprimées à l'envoi.
      </p>

      {photos.length > 0 && (
        <ul className="mt-4 grid grid-cols-2 gap-3 sm:grid-cols-4">
          {photos.map((photo, i) => (
            <li key={photo} className="space-y-1">
              <img src={urlPhoto(annonceId, photo)} alt={`Photo ${i + 1}`} className="h-24 w-full rounded-lg object-cover" />
              <button
                type="button"
                onClick={() => supprimer(photo)}
                aria-label={`Supprimer la photo ${i + 1}`}
                className="w-full rounded-lg border border-red-300 px-2 py-1 text-xs font-medium text-red-700"
              >
                Supprimer
              </button>
            </li>
          ))}
        </ul>
      )}

      {photos.length < PHOTOS_MAX && (
        <div className="mt-4">
          <label htmlFor="ajout-photos" className="mb-1 block text-sm font-medium text-slate-700">Ajouter des photos</label>
          <input
            ref={champ}
            id="ajout-photos"
            type="file"
            accept="image/jpeg,image/png"
            multiple
            disabled={enCours}
            onChange={(e) => void ajouter(e.target.files)}
            className="block w-full text-sm"
          />
          {enCours && <p role="status" className="mt-2 text-sm text-slate-500">Envoi en cours…</p>}
        </div>
      )}

      {erreurs.length > 0 && (
        <ul role="alert" className="mt-3 space-y-1 rounded-lg bg-red-50 p-3 text-sm text-red-800">
          {erreurs.map((e) => (
            <li key={e}>{e}</li>
          ))}
        </ul>
      )}
    </section>
  )
}
