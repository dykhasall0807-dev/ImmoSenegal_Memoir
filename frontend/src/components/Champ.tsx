import type { InputHTMLAttributes } from 'react'

interface Props extends InputHTMLAttributes<HTMLInputElement> {
  id: string
  libelle: string
  erreur?: string
  aide?: string
}

/** Champ de formulaire accessible : libellé lié, erreur annoncée, aide optionnelle. */
export default function Champ({ id, libelle, erreur, aide, ...reste }: Props) {
  const idErreur = `${id}-erreur`
  const idAide = `${id}-aide`
  const decrit = [erreur ? idErreur : null, aide ? idAide : null].filter(Boolean).join(' ') || undefined
  return (
    <div>
      <label htmlFor={id} className="mb-1 block text-sm font-medium text-slate-700">
        {libelle}
      </label>
      <input
        id={id}
        aria-invalid={erreur ? true : undefined}
        aria-describedby={decrit}
        className={`w-full rounded-lg border bg-white px-3 py-2.5 text-slate-900 focus:border-emeraude-600 ${
          erreur ? 'border-red-500' : 'border-slate-300'
        }`}
        {...reste}
      />
      {aide && (
        <p id={idAide} className="mt-1 text-xs text-slate-500">
          {aide}
        </p>
      )}
      {erreur && (
        <p id={idErreur} className="mt-1 text-sm text-red-700">
          {erreur}
        </p>
      )}
    </div>
  )
}
