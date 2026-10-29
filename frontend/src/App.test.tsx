import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach } from 'vitest'
import App from './App'
import { annonceTest, json, pageTest, parametresDe, simulerApi } from './test/apiSimulee'

function afficher(chemin = '/') {
  return render(
    <MemoryRouter initialEntries={[chemin]}>
      <App />
    </MemoryRouter>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Accueil', () => {
  it("affiche le formulaire de recherche et les annonces récentes venant de l'API", async () => {
    simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([annonceTest({ titre: 'Villa des Pins' })])) })
    afficher()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Sénégal/)
    expect(screen.getByRole('form', { name: /recherche de logement/i })).toBeInTheDocument()
    expect(await screen.findByRole('link', { name: 'Villa des Pins' })).toHaveAttribute('href', '/annonces/a1111111-1111-1111-1111-111111111111')
  })

  it("n'affiche aucune annonce fictive quand la base est vide", async () => {
    simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([])) })
    afficher()
    expect(await screen.findByText("Aucune annonce n'est encore publiée.")).toBeInTheDocument()
    expect(screen.queryByText('Démonstration')).not.toBeInTheDocument()
  })

  it("affiche une erreur claire quand l'API est injoignable", async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    afficher()
    expect(await screen.findByRole('alert')).toHaveTextContent(/impossible de joindre le serveur/i)
  })

  it('la recherche envoie les critères saisis vers le catalogue et vers l\'API', async () => {
    const api = simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([])) })
    const user = userEvent.setup()
    afficher()
    await user.selectOptions(screen.getByLabelText('Je veux'), 'VENTE')
    await user.selectOptions(screen.getByLabelText('Type de bien'), 'VILLA')
    await user.type(screen.getByLabelText('Quartier'), 'Néma')
    await user.type(screen.getByLabelText(/Budget max/), '50000000')
    await user.click(screen.getByRole('button', { name: /rechercher/i }))

    expect(await screen.findByRole('heading', { name: 'Catalogue des annonces' })).toBeInTheDocument()
    expect(screen.getByLabelText('Transaction')).toHaveValue('VENTE')
    expect(screen.getByLabelText('Type de bien')).toHaveValue('VILLA')
    expect(screen.getByLabelText('Quartier')).toHaveValue('Néma')
    expect(screen.getByLabelText('Prix maximum (FCFA)')).toHaveValue(50000000)

    const dernier = api.mock.calls.at(-1)![0]
    const p = parametresDe(dernier)
    expect(p.get('transaction')).toBe('VENTE')
    expect(p.get('type')).toBe('VILLA')
    expect(p.get('quartier')).toBe('Néma')
    expect(p.get('prixMax')).toBe('50000000')
  })
})

describe('Application', () => {
  it('affiche une page 404 pour une route inconnue', () => {
    simulerApi({})
    afficher('/n-importe-quoi')
    expect(screen.getByText('Page introuvable')).toBeInTheDocument()
  })
})
