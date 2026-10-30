import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { json, reponseConnexion, simulerApi, utilisateurTest } from '../test/apiSimulee'

async function connecter(role: 'CLIENT' | 'ADMIN', routes: Parameters<typeof simulerApi>[0]) {
  const u = { ...utilisateurTest, role }
  const api = simulerApi({
    'POST /api/auth/login': () => reponseConnexion(u),
    'GET /api/auth/me': () => json(200, u),
    ...routes,
  })
  const user = userEvent.setup()
  render(<MemoryRouter initialEntries={['/connexion']}><App /></MemoryRouter>)
  await user.type(await screen.findByLabelText(/email/i), 'khady@example.test')
  await user.type(screen.getByLabelText(/mot de passe/i), 'MotDePasse123!')
  await user.click(screen.getByRole('button', { name: /se connecter/i }))
  await screen.findByRole('heading', { name: /Bonjour/ })
  return { api, user }
}

afterEach(() => vi.unstubAllGlobals())

describe('Demande de rôle', () => {
  it('un Client envoie sa demande et voit qu\'elle est en attente de validation', async () => {
    const { api, user } = await connecter('CLIENT', { 'POST /api/auth/role-requests': () => json(201, { id: 'd1' }) })
    await user.selectOptions(screen.getByLabelText('Rôle souhaité'), 'AGENT')
    await user.click(screen.getByRole('button', { name: /envoyer la demande/i }))
    expect(await screen.findByRole('status')).toHaveTextContent(/en attente de validation/)
    const appel = api.mock.calls.find(([u]) => u === '/api/auth/role-requests')!
    expect(JSON.parse(String(appel[1]!.body))).toEqual({ role: 'AGENT' })
  })

  it('affiche le refus du serveur si une demande est déjà en attente', async () => {
    const { user } = await connecter('CLIENT', {
      'POST /api/auth/role-requests': () => json(409, { message: 'Une demande est déjà en attente de validation.' }),
    })
    await user.click(screen.getByRole('button', { name: /envoyer la demande/i }))
    expect(await screen.findByRole('alert')).toHaveTextContent('déjà en attente')
  })

  it("un administrateur approuve une demande ; un Client ne voit pas le lien d'administration", async () => {
    let liste = [{ id: 'd1', email: 'awa@example.test', nomComplet: 'Awa Diallo', roleDemande: 'PROPRIETAIRE', statut: 'EN_ATTENTE', creeLe: '2026-10-29T10:00:00Z' }]
    const { api, user } = await connecter('ADMIN', {
      'GET /api/admin/role-requests?statut=EN_ATTENTE': () => json(200, liste),
      'POST /api/admin/role-requests/d1/approve': () => {
        liste = []
        return json(200, {})
      },
    })
    expect(screen.queryByRole('button', { name: /envoyer la demande/i })).not.toBeInTheDocument()
    await user.click(screen.getByRole('link', { name: /demandes de rôle/i }))
    expect(await screen.findByText('Awa Diallo')).toBeInTheDocument()
    await user.click(screen.getByRole('button', { name: 'Approuver' }))
    expect(await screen.findByText('Aucune demande en attente.')).toBeInTheDocument()
    expect(api.mock.calls.some(([u, i]) => u === '/api/admin/role-requests/d1/approve' && i?.method === 'POST')).toBe(true)
  })
})
