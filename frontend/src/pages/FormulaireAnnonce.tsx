import { useEffect, useState, type FormEvent } from 'react'
import { Link, useLocation, useNavigate, useParams } from 'react-router-dom'
import { ApiError, appelApi } from '../api'
import { useAuth } from '../auth'
import Champ from '../components/Champ'
import GestionPhotos from '../components/GestionPhotos'
import { libelleStatut, libelleTypeBien, type Annonce, type StatutAnnonce, type TypeBien, type TypeTransaction } from '../types'

const classeSelect = 'w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-slate-900'

/** Rôles autorisés par le serveur à publier. Le serveur reste seul juge : ceci évite seulement un écran inutile. */
export function peutPublier(role: string | undefined) {
  return role === 'PROPRIETAIRE' || role === 'AGENT' || role === 'ADMIN'
}

export default function FormulaireAnnonce() {
  const { id } = useParams()
  const edition = Boolean(id)
  const { utilisateur, jeton } = useAuth()
  const navigate = useNavigate()
  const nouvelle = (useLocation().state as { nouvelle?: boolean } | null)?.nouvelle === true
  const [photos, setPhotos] = useState<string[]>([])
  const [initial, setInitial] = useState<Annonce | null>(null)
  const [chargement, setChargement] = useState(edition)
  const [erreur, setErreur] = useState<string | null>(null)
  const [champs, setChamps] = useState<Record<string, string>>({})
  const [envoi, setEnvoi] = useState(false)

  useEffect(() => {
    if (!id) return
    let annule = false
    appelApi<Annonce>(`/api/annonces/${id}`)
      .then((a) => {
        if (annule) return
        setInitial(a)
        setPhotos(a.photos)
      })
      .catch((e: unknown) => !annule && setErreur(e instanceof ApiError ? e.message : 'Chargement impossible.'))
      .finally(() => !annule && setChargement(false))
    return () => {
      annule = true
    }
  }, [id])

  if (!peutPublier(utilisateur?.role)) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-12">
        <h1 className="text-2xl font-bold">Publier une annonce</h1>
        <p role="alert" className="mt-4 rounded-lg bg-amber-50 p-4 text-sm text-slate-800">
          Votre compte est un compte Client : il ne peut pas publier d'annonce. Le passage au rôle Propriétaire ou Agent
          doit être validé par un administrateur ; l'écran pour en faire la demande n'est pas encore disponible.
        </p>
      </div>
    )
  }
  if (chargement) return <p role="status" className="p-8 text-center text-slate-500">Chargement…</p>
  if (edition && !initial) {
    return (
      <div className="mx-auto max-w-2xl px-4 py-12">
        <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{erreur ?? 'Annonce introuvable.'}</p>
      </div>
    )
  }

  async function soumettre(e: FormEvent<HTMLFormElement>) {
    e.preventDefault()
    const f = new FormData(e.currentTarget)
    const texte = (k: string) => String(f.get(k) ?? '').trim()
    const nombre = (k: string) => (texte(k) === '' ? null : Number(texte(k)))
    const corps = {
      titre: texte('titre'),
      description: texte('description') || null,
      typeBien: texte('typeBien'),
      transaction: texte('transaction'),
      prix: nombre('prix'),
      superficie: nombre('superficie'),
      pieces: nombre('pieces'),
      quartier: texte('quartier'),
      ville: texte('ville'),
      adresse: texte('adresse') || null,
      equipements: texte('equipements') === '' ? [] : texte('equipements').split(',').map((x) => x.trim()).filter(Boolean),
      ...(edition ? { statut: texte('statut') } : {}),
    }
    setEnvoi(true)
    setErreur(null)
    setChamps({})
    try {
      const reponse = await appelApi<Annonce>(edition ? `/api/annonces/${id}` : '/api/annonces', {
        method: edition ? 'PUT' : 'POST',
        body: corps,
        token: jeton,
      })
      // Après une publication, on ouvre la page de l'annonce pour y ajouter des photos.
      if (edition) navigate('/mes-annonces')
      else navigate(`/annonces/${reponse.id}/modifier`, { replace: true, state: { nouvelle: true } })
    } catch (err) {
      if (err instanceof ApiError) {
        setErreur(err.message)
        setChamps(err.details)
      } else setErreur('Une erreur inattendue est survenue.')
      setEnvoi(false)
    }
  }

  const a = initial
  return (
    <div className="mx-auto max-w-2xl px-4 py-8">
      <h1 className="text-2xl font-bold">{edition ? "Modifier l'annonce" : 'Publier une annonce'}</h1>
      {nouvelle && (
        <p role="status" className="mt-4 rounded-lg bg-emerald-50 p-3 text-sm text-emerald-900">
          Annonce publiée. Ajoutez maintenant des photos, puis cliquez sur « Terminer ».
        </p>
      )}
      <form onSubmit={soumettre} noValidate aria-label="Formulaire d'annonce" className="mt-6 space-y-4 rounded-2xl bg-white p-5 shadow">
        {erreur && <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">{erreur}</p>}
        <Champ id="titre" name="titre" libelle="Titre" required maxLength={150} defaultValue={a?.titre} erreur={champs.titre} />
        <div>
          <label htmlFor="description" className="mb-1 block text-sm font-medium text-slate-700">Description</label>
          <textarea id="description" name="description" rows={4} maxLength={5000} defaultValue={a?.description ?? ''} className={classeSelect} />
          {champs.description && <p className="mt-1 text-sm text-red-700">{champs.description}</p>}
        </div>
        <div className="grid gap-4 sm:grid-cols-2">
          <div>
            <label htmlFor="transaction" className="mb-1 block text-sm font-medium text-slate-700">Transaction</label>
            <select id="transaction" name="transaction" defaultValue={a?.transaction ?? 'LOCATION'} className={classeSelect}>
              {(['LOCATION', 'VENTE'] as TypeTransaction[]).map((t) => (
                <option key={t} value={t}>{t === 'LOCATION' ? 'Location' : 'Vente'}</option>
              ))}
            </select>
          </div>
          <div>
            <label htmlFor="typeBien" className="mb-1 block text-sm font-medium text-slate-700">Type de bien</label>
            <select id="typeBien" name="typeBien" defaultValue={a?.typeBien ?? 'APPARTEMENT'} className={classeSelect}>
              {(Object.keys(libelleTypeBien) as TypeBien[]).map((t) => (
                <option key={t} value={t}>{libelleTypeBien[t]}</option>
              ))}
            </select>
          </div>
          <Champ id="prix" name="prix" type="number" min={1} libelle="Prix (FCFA)" required defaultValue={a?.prix} erreur={champs.prix} />
          <Champ id="superficie" name="superficie" type="number" min={1} step="any" libelle="Superficie (m²)" required defaultValue={a?.superficie} erreur={champs.superficie} />
          <Champ id="pieces" name="pieces" type="number" min={0} libelle="Nombre de pièces" defaultValue={a?.pieces ?? ''} erreur={champs.pieces} />
          <Champ id="quartier" name="quartier" libelle="Quartier" required defaultValue={a?.quartier} erreur={champs.quartier} />
          <Champ id="ville" name="ville" libelle="Ville" required defaultValue={a?.ville ?? 'Ziguinchor'} erreur={champs.ville} />
          <Champ id="adresse" name="adresse" libelle="Adresse (facultatif)" defaultValue={a?.adresse ?? ''} erreur={champs.adresse} />
        </div>
        <Champ id="equipements" name="equipements" libelle="Équipements" aide="Séparés par des virgules : wifi, parking, climatisation" defaultValue={a?.equipements.join(', ')} erreur={champs.equipements} />
        {edition && (
          <div>
            <label htmlFor="statut" className="mb-1 block text-sm font-medium text-slate-700">Statut</label>
            <select id="statut" name="statut" defaultValue={a?.statut} className={classeSelect}>
              {(Object.keys(libelleStatut) as StatutAnnonce[]).map((s) => (
                <option key={s} value={s}>{libelleStatut[s]}</option>
              ))}
            </select>
          </div>
        )}
        <div className="flex flex-wrap gap-3">
          <button type="submit" disabled={envoi} className="rounded-lg bg-emeraude-600 px-4 py-2.5 font-semibold text-white hover:bg-emeraude-700 disabled:opacity-60">
            {envoi ? 'Envoi…' : edition ? 'Enregistrer' : "Publier l'annonce"}
          </button>
          <Link to="/mes-annonces" className="rounded-lg border border-slate-300 px-4 py-2.5 font-medium">
            {nouvelle ? 'Terminer' : 'Annuler'}
          </Link>
        </div>
      </form>
      {edition && id && <GestionPhotos annonceId={id} photos={photos} onChange={setPhotos} />}
    </div>
  )
}
