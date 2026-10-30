import { render, screen, waitFor } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { json, reponseConnexion, simulerApi, utilisateurTest } from '../test/apiSimulee'

function afficher(chemin: string) {
  return render(
    <MemoryRouter initialEntries={[chemin]}>
      <App />
    </MemoryRouter>,
  )
}

async function seConnecter(user: ReturnType<typeof userEvent.setup>) {
  await user.type(screen.getByLabelText('Adresse email'), 'khady@example.test')
  await user.type(screen.getByLabelText('Mot de passe'), 'MotDePasse-Solide-123')
  await user.click(screen.getByRole('button', { name: /se connecter/i }))
}

beforeEach(() => {
  localStorage.clear()
  sessionStorage.clear()
})
afterEach(() => vi.unstubAllGlobals())

describe('Connexion', () => {
  it('connecte l\'utilisateur et affiche son tableau de bord vérifié par /me', async () => {
    const api = simulerApi({
      'POST /api/auth/login': () => reponseConnexion(),
      'GET /api/auth/me': () => json(200, utilisateurTest),
    })
    const user = userEvent.setup()
    afficher('/connexion')
    await seConnecter(user)

    expect(await screen.findByRole('heading', { name: /bonjour, khady test/i })).toBeInTheDocument()
    expect(await screen.findByText('Client')).toBeInTheDocument()

    // Le jeton est envoyé à /me dans l'en-tête Authorization.
    const appelMe = api.mock.calls.find(([url]) => url === '/api/auth/me')
    expect((appelMe?.[1]?.headers as Record<string, string>).Authorization).toBe('Bearer jeton-de-test')
  })

  it('ne stocke jamais le jeton dans localStorage ni sessionStorage', async () => {
    simulerApi({
      'POST /api/auth/login': () => reponseConnexion(),
      'GET /api/auth/me': () => json(200, utilisateurTest),
    })
    const user = userEvent.setup()
    afficher('/connexion')
    await seConnecter(user)
    await screen.findByRole('heading', { name: /bonjour/i })

    expect(localStorage.length).toBe(0)
    expect(sessionStorage.length).toBe(0)
  })

  it('affiche le message du serveur quand les identifiants sont refusés', async () => {
    simulerApi({ 'POST /api/auth/login': () => json(401, { message: 'Email ou mot de passe incorrect.' }) })
    const user = userEvent.setup()
    afficher('/connexion')
    await seConnecter(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('Email ou mot de passe incorrect.')
    expect(screen.getByRole('heading', { name: 'Connexion' })).toBeInTheDocument()
  })

  it('indique le temps d\'attente après un blocage 429', async () => {
    simulerApi({
      'POST /api/auth/login': () => json(429, { message: 'Trop de tentatives.' }, { 'Retry-After': '600' }),
    })
    const user = userEvent.setup()
    afficher('/connexion')
    await seConnecter(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('Réessayez dans 10 minutes')
  })

  it('signale clairement un serveur injoignable', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    const user = userEvent.setup()
    afficher('/connexion')
    await seConnecter(user)

    expect(await screen.findByRole('alert')).toHaveTextContent(/impossible de joindre le serveur/i)
  })
})

describe('Accès protégé et déconnexion', () => {
  it('redirige un visiteur anonyme du tableau de bord vers la connexion', async () => {
    simulerApi({})
    afficher('/tableau-de-bord')
    expect(await screen.findByRole('heading', { name: 'Connexion' })).toBeInTheDocument()
  })

  it('la déconnexion efface la session et ferme l\'espace personnel', async () => {
    simulerApi({
      'POST /api/auth/login': () => reponseConnexion(),
      'GET /api/auth/me': () => json(200, utilisateurTest),
    })
    const user = userEvent.setup()
    afficher('/connexion')
    await seConnecter(user)
    await screen.findByRole('heading', { name: /bonjour/i })

    await user.click(screen.getByRole('button', { name: /déconnexion/i }))
    expect(screen.queryByRole('button', { name: /déconnexion/i })).not.toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Connexion' })).toBeInTheDocument()
  })

  it('renvoie à la connexion si le serveur répond 401 sur /me (jeton expiré)', async () => {
    simulerApi({
      'POST /api/auth/login': () => reponseConnexion(),
      'GET /api/auth/me': () => json(401, { message: 'Session invalide.' }),
    })
    const user = userEvent.setup()
    afficher('/connexion')
    await seConnecter(user)

    await waitFor(() => expect(screen.getByRole('heading', { name: 'Connexion' })).toBeInTheDocument())
    expect(screen.getByRole('link', { name: 'Inscription' })).toBeInTheDocument()
  })
})

describe('Inscription', () => {
  async function remplir(user: ReturnType<typeof userEvent.setup>, confirmation = 'MotDePasse-Solide-123') {
    await user.type(screen.getByLabelText('Nom complet'), 'Khady Test')
    await user.type(screen.getByLabelText('Adresse email'), 'khady@example.test')
    await user.type(screen.getByLabelText('Mot de passe'), 'MotDePasse-Solide-123')
    await user.type(screen.getByLabelText('Confirmer le mot de passe'), confirmation)
    await user.click(screen.getByRole('button', { name: /créer mon compte/i }))
  }

  it('refuse sans appeler le serveur quand les mots de passe diffèrent', async () => {
    const api = simulerApi({})
    const user = userEvent.setup()
    afficher('/inscription')
    await remplir(user, 'Autre-Mot-De-Passe-1')

    expect(await screen.findByText('Les deux mots de passe ne correspondent pas.')).toBeInTheDocument()
    expect(api).not.toHaveBeenCalled()
  })

  it('refuse un mot de passe de moins de 12 caractères', async () => {
    const api = simulerApi({})
    const user = userEvent.setup()
    afficher('/inscription')
    await user.type(screen.getByLabelText('Nom complet'), 'Khady')
    await user.type(screen.getByLabelText('Adresse email'), 'khady@example.test')
    await user.type(screen.getByLabelText('Mot de passe'), 'court')
    await user.type(screen.getByLabelText('Confirmer le mot de passe'), 'court')
    await user.click(screen.getByRole('button', { name: /créer mon compte/i }))

    expect(await screen.findByText(/au moins 12 caractères/i)).toBeInTheDocument()
    expect(api).not.toHaveBeenCalled()
  })

  it('n\'envoie aucun champ « role » au serveur et connecte après l\'inscription', async () => {
    const api = simulerApi({
      'POST /api/auth/register': () => json(201, utilisateurTest),
      'POST /api/auth/login': () => reponseConnexion(),
      'GET /api/auth/me': () => json(200, utilisateurTest),
    })
    const user = userEvent.setup()
    afficher('/inscription')
    await remplir(user)

    expect(await screen.findByRole('heading', { name: /bonjour, khady test/i })).toBeInTheDocument()
    const appel = api.mock.calls.find(([url]) => url === '/api/auth/register')
    const corps = JSON.parse(String(appel?.[1]?.body))
    expect(corps).not.toHaveProperty('role')
    expect(corps).toMatchObject({ email: 'khady@example.test', nomComplet: 'Khady Test', telephone: null })
  })

  it('affiche l\'erreur du serveur quand l\'email existe déjà', async () => {
    simulerApi({ 'POST /api/auth/register': () => json(409, { message: 'Un compte existe déjà avec cet email.' }) })
    const user = userEvent.setup()
    afficher('/inscription')
    await remplir(user)

    expect(await screen.findByRole('alert')).toHaveTextContent('Un compte existe déjà avec cet email.')
  })

  it('affiche les erreurs champ par champ renvoyées par le serveur', async () => {
    simulerApi({
      'POST /api/auth/register': () => json(400, { message: 'Certains champs sont invalides.', details: { email: 'Email refusé par le serveur.' } }),
    })
    const user = userEvent.setup()
    afficher('/inscription')
    await remplir(user)

    expect(await screen.findByText('Email refusé par le serveur.')).toBeInTheDocument()
  })
})
