import { useSearchParams } from 'react-router-dom'
import { formatFcfa } from '../format'

const libellesType: Record<string, string> = {
  MAISON: 'Maison', APPARTEMENT: 'Appartement', STUDIO: 'Studio', VILLA: 'Villa', TERRAIN: 'Terrain',
}

/** Page provisoire : affiche les critères reçus. La liste réelle arrive avec l'API du catalogue. */
export default function Catalogue() {
  const [params] = useSearchParams()
  const criteres: string[] = []
  const transaction = params.get('transaction')
  if (transaction) criteres.push(transaction === 'VENTE' ? 'Achat' : 'Location')
  const type = params.get('type')
  if (type) criteres.push(libellesType[type] ?? type)
  const quartier = params.get('quartier')
  if (quartier) criteres.push(`Quartier : ${quartier}`)
  const budget = Number(params.get('budgetMax'))
  if (budget > 0) criteres.push(`Budget max : ${formatFcfa(budget)}`)

  return (
    <div className="mx-auto max-w-6xl px-4 py-12">
      <h1 className="text-2xl font-bold">Catalogue</h1>
      <p className="mt-3 rounded-lg border border-dore-500/50 bg-amber-50 p-4 text-sm text-slate-800">
        Le catalogue n'est pas encore relié à la base de données : aucune annonce n'est affichée ici pour l'instant.
      </p>
      <h2 className="mt-8 font-semibold">Vos critères de recherche</h2>
      {criteres.length === 0 ? (
        <p className="mt-2 text-slate-600">Aucun critère : tous les biens seront affichés.</p>
      ) : (
        <ul className="mt-2 flex flex-wrap gap-2">
          {criteres.map((c) => (
            <li key={c} className="rounded-full bg-emeraude-50 px-3 py-1 text-sm text-emeraude-700">
              {c}
            </li>
          ))}
        </ul>
      )}
    </div>
  )
}
