import { Link } from 'react-router-dom'

export default function NotFound() {
  return (
    <div className="mx-auto max-w-xl px-4 py-20 text-center">
      <p className="text-6xl font-extrabold text-emeraude-600">404</p>
      <h1 className="mt-4 text-2xl font-bold">Page introuvable</h1>
      <p className="mt-2 text-slate-600">Cette page n'existe pas ou a été déplacée.</p>
      <Link to="/" className="mt-6 inline-block rounded-lg bg-nuit-900 px-5 py-2.5 font-semibold text-white hover:bg-nuit-800">
        Retour à l'accueil
      </Link>
    </div>
  )
}
