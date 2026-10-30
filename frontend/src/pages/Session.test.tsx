import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { json, reponseConnexion, simulerApi, utilisateurTest } from '../test/apiSimulee'

function afficher(chemin: string) {
  return render(
    <MemoryRouter initialEntries={[chemin]}>
      <App />
    </MemoryRouter>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Session longue (cookie HttpOnly)', () => {
  it('retrouve la connexion au chargement quand le cookie est encore valide', async () => {
    const api = simulerApi({
      'POST /api/auth/refresh': () => reponseConnexion(),
      'GET /api/auth/me': () => json(200, utilisateurTest),
    })
    afficher('/tableau-de-bord')

    expect(await screen.findByRole('heading', { name: /Bonjour, Khady Test/ })).toBeInTheDocument()
    const [url, init] = api.mock.calls.find(([u]) => u === '/api/auth/refresh')!
    expect(url).toBe('/api/auth/refresh')
    expect(init?.method).toBe('POST')
    expect(init?.credentials).toBe('include')
    expect((init?.headers as Record<string, string>)['X-Requested-With']).toBe('immosenegal')
  })

  it("n'affiche pas la page de connexion pendant la restauration (pas de clignotement)", async () => {
    let terminer: (r: Response) => void = () => undefined
    simulerApi({
      'POST /api/auth/refresh': () => new Promise<Response>((r) => (terminer = r)),
      'GET /api/auth/me': () => json(200, utilisateurTest),
    })
    afficher('/tableau-de-bord')
    expect(await screen.findByRole('status')).toHaveTextContent('Chargement')
    expect(screen.queryByRole('heading', { name: 'Connexion' })).not.toBeInTheDocument()
    terminer(reponseConnexion())
    expect(await screen.findByRole('heading', { name: /Bonjour/ })).toBeInTheDocument()
  })

  it('un visiteur sans cookie valide arrive sur la connexion', async () => {
    simulerApi({ 'POST /api/auth/refresh': () => json(401, { message: 'Session expirée. Veuillez vous reconnecter.' }) })
    afficher('/tableau-de-bord')
    expect(await screen.findByRole('heading', { name: 'Connexion' })).toBeInTheDocument()
  })

  it('la connexion demande au navigateur de garder le cookie ; le jeton reste hors du stockage du navigateur', async () => {
    const api = simulerApi({
      'POST /api/auth/login': () => reponseConnexion(),
      'GET /api/auth/me': () => json(200, utilisateurTest),
    })
    const user = userEvent.setup()
    afficher('/connexion')
    await user.type(await screen.findByLabelText('Adresse email'), 'khady@example.test')
    await user.type(screen.getByLabelText('Mot de passe'), 'MotDePasse-Solide-123')
    await user.click(screen.getByRole('button', { name: /se connecter/i }))
    await screen.findByRole('heading', { name: /Bonjour/ })

    const login = api.mock.calls.find(([u]) => u === '/api/auth/login')!
    expect(login[1]?.credentials).toBe('include')
    expect(JSON.stringify({ ...localStorage })).not.toContain('jeton-de-test')
    expect(JSON.stringify({ ...sessionStorage })).not.toContain('jeton-de-test')
  })

  it('la déconnexion révoque la session côté serveur', async () => {
    const api = simulerApi({
      'POST /api/auth/refresh': () => reponseConnexion(),
      'GET /api/auth/me': () => json(200, utilisateurTest),
      'POST /api/auth/logout': () => new Response(null, { status: 204 }),
    })
    const user = userEvent.setup()
    afficher('/tableau-de-bord')
    await screen.findByRole('heading', { name: /Bonjour/ })
    await user.click(screen.getByRole('button', { name: /déconnexion/i }))

    expect(await screen.findByRole('link', { name: 'Connexion' })).toBeInTheDocument()
    const appel = api.mock.calls.find(([u]) => u === '/api/auth/logout')!
    expect(appel[1]?.method).toBe('POST')
    expect(appel[1]?.credentials).toBe('include')
  })
})
