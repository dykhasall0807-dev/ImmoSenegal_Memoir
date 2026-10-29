import { useState, type FormEvent } from 'react'
import { useNavigate } from 'react-router-dom'
import { Search } from 'lucide-react'

const champ =
  'w-full rounded-lg border border-slate-300 bg-white px-3 py-2.5 text-slate-900 placeholder:text-slate-400 focus:border-emeraude-600'

/** Formulaire de recherche : envoie vers /catalogue avec les critères dans l'URL. */
export default function SearchForm() {
  const navigate = useNavigate()
  const [transaction, setTransaction] = useState('LOCATION')
  const [type, setType] = useState('')
  const [quartier, setQuartier] = useState('')
  const [budgetMax, setBudgetMax] = useState('')

  function soumettre(e: FormEvent) {
    e.preventDefault()
    const params = new URLSearchParams({ transaction })
    if (type) params.set('type', type)
    if (quartier.trim()) params.set('quartier', quartier.trim())
    if (budgetMax && Number(budgetMax) > 0) params.set('prixMax', budgetMax)
    navigate(`/catalogue?${params.toString()}`)
  }

  return (
    <form
      onSubmit={soumettre}
      aria-label="Recherche de logement"
      className="grid gap-3 rounded-2xl bg-white p-4 shadow-xl sm:grid-cols-2 lg:grid-cols-5 lg:items-end"
    >
      <div>
        <label htmlFor="transaction" className="mb-1 block text-sm font-medium text-slate-700">
          Je veux
        </label>
        <select id="transaction" value={transaction} onChange={(e) => setTransaction(e.target.value)} className={champ}>
          <option value="LOCATION">Louer</option>
          <option value="VENTE">Acheter</option>
        </select>
      </div>
      <div>
        <label htmlFor="type" className="mb-1 block text-sm font-medium text-slate-700">
          Type de bien
        </label>
        <select id="type" value={type} onChange={(e) => setType(e.target.value)} className={champ}>
          <option value="">Tous les types</option>
          <option value="MAISON">Maison</option>
          <option value="APPARTEMENT">Appartement</option>
          <option value="STUDIO">Studio</option>
          <option value="VILLA">Villa</option>
          <option value="TERRAIN">Terrain</option>
        </select>
      </div>
      <div>
        <label htmlFor="quartier" className="mb-1 block text-sm font-medium text-slate-700">
          Quartier
        </label>
        <input
          id="quartier"
          type="text"
          value={quartier}
          onChange={(e) => setQuartier(e.target.value)}
          placeholder="Ex. Kandé"
          className={champ}
        />
      </div>
      <div>
        <label htmlFor="budgetMax" className="mb-1 block text-sm font-medium text-slate-700">
          Budget max (FCFA)
        </label>
        <input
          id="budgetMax"
          type="number"
          inputMode="numeric"
          min={0}
          step={1000}
          value={budgetMax}
          onChange={(e) => setBudgetMax(e.target.value)}
          placeholder="Ex. 150000"
          className={champ}
        />
      </div>
      <button
        type="submit"
        className="flex items-center justify-center gap-2 rounded-lg bg-emeraude-600 px-4 py-2.5 font-semibold text-white transition-colors hover:bg-emeraude-700 sm:col-span-2 lg:col-span-1"
      >
        <Search aria-hidden className="h-5 w-5" />
        Rechercher
      </button>
    </form>
  )
}
