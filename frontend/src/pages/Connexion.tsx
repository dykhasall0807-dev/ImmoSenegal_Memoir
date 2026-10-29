import { useState, type FormEvent } from 'react'
import { Link, Navigate, useLocation, useNavigate } from 'react-router-dom'
import { LogIn } from 'lucide-react'
import { useAuth } from '../auth'
import { ApiError } from '../api'
import Champ from '../components/Champ'

function messageErreur(e: unknown): string {
  if (e instanceof ApiError) {
    if (e.status === 429 && e.retryAfter) {
      const minutes = Math.max(1, Math.ceil(e.retryAfter / 60))
      return `Trop de tentatives. Réessayez dans ${minutes} minute${minutes > 1 ? 's' : ''}.`
    }
    return e.message
  }
  return 'Une erreur inattendue est survenue.'
}

export default function Connexion() {
  const { utilisateur, connecter } = useAuth()
  const navigate = useNavigate()
  const location = useLocation()
  const destination = (location.state as { from?: string } | null)?.from ?? '/tableau-de-bord'

  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [erreur, setErreur] = useState<string | null>(null)
  const [enCours, setEnCours] = useState(false)

  if (utilisateur) return <Navigate to={destination} replace />

  async function soumettre(e: FormEvent) {
    e.preventDefault()
    setErreur(null)
    setEnCours(true)
    try {
      await connecter(email.trim(), password)
      navigate(destination, { replace: true })
    } catch (err) {
      setErreur(messageErreur(err))
    } finally {
      setEnCours(false)
    }
  }

  return (
    <div className="mx-auto max-w-md px-4 py-12">
      <h1 className="text-2xl font-bold">Connexion</h1>
      <p className="mt-1 text-sm text-slate-600">Accédez à votre espace ImmoSénégal.</p>

      <form onSubmit={soumettre} className="mt-6 space-y-4 rounded-2xl bg-white p-5 shadow" noValidate>
        {erreur && (
          <p role="alert" className="rounded-lg bg-red-50 p-3 text-sm text-red-800">
            {erreur}
          </p>
        )}
        <Champ
          id="email"
          libelle="Adresse email"
          type="email"
          autoComplete="email"
          required
          value={email}
          onChange={(e) => setEmail(e.target.value)}
        />
        <Champ
          id="password"
          libelle="Mot de passe"
          type="password"
          autoComplete="current-password"
          required
          value={password}
          onChange={(e) => setPassword(e.target.value)}
        />
        <button
          type="submit"
          disabled={enCours || email.trim() === '' || password === ''}
          className="flex w-full items-center justify-center gap-2 rounded-lg bg-emeraude-600 px-4 py-2.5 font-semibold text-white transition-colors hover:bg-emeraude-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          <LogIn aria-hidden className="h-5 w-5" />
          {enCours ? 'Connexion…' : 'Se connecter'}
        </button>
      </form>

      <p className="mt-4 text-center text-sm text-slate-600">
        Pas encore de compte ?{' '}
        <Link to="/inscription" className="font-semibold text-emeraude-700 underline">
          Créer un compte
        </Link>
      </p>
    </div>
  )
}
