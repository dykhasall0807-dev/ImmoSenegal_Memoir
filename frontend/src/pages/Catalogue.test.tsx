import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { annonceTest, json, pageTest, parametresDe, simulerApi } from '../test/apiSimulee'

function afficher(chemin: string) {
  return render(
    <MemoryRouter initialEntries={[chemin]}>
      <App />
    </MemoryRouter>,
  )
}

afterEach(() => vi.unstubAllGlobals())

describe('Catalogue', () => {
  it("lit les filtres dans l'URL, les envoie à l'API et affiche les résultats", async () => {
    const api = simulerApi({
      'GET /api/annonces?*': () => json(200, pageTest([annonceTest(), annonceTest({ id: 'b2', titre: 'Studio meublé', prix: 55000 })])),
    })
    afficher('/catalogue?transaction=LOCATION&prixMax=150000&quartier=Kand%C3%A9&tri=prix_asc')

    expect(await screen.findByText('2 annonces')).toBeInTheDocument()
    expect(screen.getByRole('link', { name: 'Studio meublé' })).toBeInTheDocument()
    expect(screen.getByLabelText('Prix maximum (FCFA)')).toHaveValue(150000)
    expect(screen.getByLabelText('Trier par')).toHaveValue('prix_asc')

    const p = parametresDe(api.mock.calls[0][0])
    expect(p.get('transaction')).toBe('LOCATION')
    expect(p.get('prixMax')).toBe('150000')
    expect(p.get('quartier')).toBe('Kandé')
    expect(p.get('tri')).toBe('prix_asc')
    expect(p.get('taille')).toBe('12')
    expect(p.get('page')).toBe('0')
  })

  it('ne transmet pas de paramètre inconnu de l\'URL au serveur', async () => {
    const api = simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([])) })
    afficher('/catalogue?transaction=LOCATION&proprietaireId=pirate&statut=VENDU')
    await screen.findByText('Aucune annonce')
    const p = parametresDe(api.mock.calls[0][0])
    expect(p.has('proprietaireId')).toBe(false)
    expect(p.has('statut')).toBe(false)
  })

  it('affiche un message quand aucune annonce ne correspond', async () => {
    simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([])) })
    afficher('/catalogue?quartier=Introuvable')
    expect(await screen.findByText(/aucune annonce ne correspond/i)).toBeInTheDocument()
  })

  it("affiche l'erreur du serveur sans planter", async () => {
    simulerApi({ 'GET /api/annonces?*': () => json(500, { message: 'Le serveur a rencontré un problème.' }) })
    afficher('/catalogue')
    expect(await screen.findByRole('alert')).toHaveTextContent('Le serveur a rencontré un problème.')
  })

  it('signale les annonces déjà réservées, vendues ou louées', async () => {
    simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([annonceTest({ statut: 'LOUE' })])) })
    afficher('/catalogue')
    const carte = await screen.findByRole('article')
    expect(within(carte).getByText('Loué')).toBeInTheDocument()
  })

  it('appliquer les filtres met à jour l\'URL et relance la recherche', async () => {
    const api = simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([])) })
    const user = userEvent.setup()
    afficher('/catalogue')
    await screen.findByText('Aucune annonce')

    await user.selectOptions(screen.getByLabelText('Type de bien'), 'STUDIO')
    await user.type(screen.getByLabelText('Pièces (minimum)'), '2')
    await user.click(screen.getByRole('button', { name: 'Appliquer' }))

    await waitFor(() => {
      const p = parametresDe(api.mock.calls.at(-1)![0])
      expect(p.get('type')).toBe('STUDIO')
      expect(p.get('piecesMin')).toBe('2')
    })
  })

  it('réinitialiser efface tous les filtres', async () => {
    const api = simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([])) })
    const user = userEvent.setup()
    afficher('/catalogue?type=VILLA&quartier=Néma')
    await screen.findByText('Aucune annonce')

    await user.click(screen.getByRole('button', { name: 'Réinitialiser' }))
    await waitFor(() => expect(screen.getByLabelText('Quartier')).toHaveValue(''))
    const p = parametresDe(api.mock.calls.at(-1)![0])
    expect(p.has('type')).toBe(false)
    expect(p.has('quartier')).toBe(false)
  })

  it('la pagination demande la page suivante', async () => {
    const api = simulerApi({
      'GET /api/annonces?*': () => json(200, pageTest([annonceTest()], { totalElements: 25, totalPages: 3 })),
    })
    const user = userEvent.setup()
    afficher('/catalogue')
    expect(await screen.findByText('Page 1 sur 3')).toBeInTheDocument()
    expect(screen.getByRole('button', { name: /précédent/i })).toBeDisabled()

    await user.click(screen.getByRole('button', { name: /suivant/i }))
    await waitFor(() => expect(parametresDe(api.mock.calls.at(-1)![0]).get('page')).toBe('1'))
  })
})

describe("Détail d'une annonce", () => {
  const id = 'a1111111-1111-1111-1111-111111111111'

  it('affiche le détail sans exposer de contact du propriétaire', async () => {
    simulerApi({ [`GET /api/annonces/${id}`]: () => json(200, annonceTest()) })
    afficher(`/annonces/${id}`)

    expect(await screen.findByRole('heading', { level: 1, name: 'Appartement lumineux' })).toBeInTheDocument()
    expect(screen.getByText('120 000 FCFA')).toBeInTheDocument()
    expect(screen.getByText('Awa Diallo')).toBeInTheDocument()
    expect(screen.getByText('wifi')).toBeInTheDocument()
    expect(screen.queryByRole('button', { name: /contacter|visite/i })).not.toBeInTheDocument()
  })

  it('affiche « Annonce introuvable » quand le serveur répond 404', async () => {
    simulerApi({ [`GET /api/annonces/${id}`]: () => json(404, { message: 'Annonce introuvable.' }) })
    afficher(`/annonces/${id}`)
    expect(await screen.findByRole('heading', { name: 'Annonce introuvable' })).toBeInTheDocument()
  })

  it('affiche une erreur claire quand le serveur est injoignable', async () => {
    vi.stubGlobal('fetch', vi.fn(async () => { throw new TypeError('Failed to fetch') }))
    afficher(`/annonces/${id}`)
    expect(await screen.findByRole('alert')).toHaveTextContent(/impossible de joindre le serveur/i)
  })
})
