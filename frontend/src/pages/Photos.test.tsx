import { render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { annonceTest, json, pageTest, reponseConnexion, simulerApi, utilisateurTest } from '../test/apiSimulee'

const proprio = { ...utilisateurTest, role: 'PROPRIETAIRE' as const }
const ID = 'a1111111-1111-1111-1111-111111111111'
const P1 = 'b0000000-0000-0000-0000-000000000001'
const P2 = 'b0000000-0000-0000-0000-000000000002'

function afficher(chemin: string) {
  return render(
    <MemoryRouter initialEntries={[chemin]}>
      <App />
    </MemoryRouter>,
  )
}

/** Ouvre la page de modification d'une annonce en étant déjà connecté (session restaurée par le cookie). */
async function pageModification(routes: Parameters<typeof simulerApi>[0], photos: string[] = []) {
  const api = simulerApi({
    'POST /api/auth/refresh': () => reponseConnexion(proprio),
    [`GET /api/annonces/${ID}`]: () => json(200, annonceTest({ photos })),
    ...routes,
  })
  const user = userEvent.setup()
  afficher(`/annonces/${ID}/modifier`)
  await screen.findByLabelText('Ajouter des photos')
  return { api, user }
}

const fichier = (nom: string, octets = 10, type = 'image/jpeg') => new File([new Uint8Array(octets)], nom, { type })

afterEach(() => vi.unstubAllGlobals())

describe('Gestion des photos (propriétaire)', () => {
  it("envoie la photo en multipart (champ « fichier ») avec le jeton, puis l'affiche", async () => {
    const { api, user } = await pageModification({ [`POST /api/annonces/${ID}/photos`]: () => json(201, { id: P1 }) })
    await user.upload(screen.getByLabelText('Ajouter des photos'), fichier('salon.jpg'))

    expect(await screen.findByAltText('Photo 1')).toHaveAttribute('src', `/api/annonces/${ID}/photos/${P1}`)
    expect(screen.getByRole('heading', { name: 'Photos (1/8)' })).toBeInTheDocument()

    const [, init] = api.mock.calls.find(([u, i]) => u === `/api/annonces/${ID}/photos` && i?.method === 'POST')!
    expect(init!.body).toBeInstanceOf(FormData)
    expect((init!.body as FormData).get('fichier')).toBeInstanceOf(File)
    const entetes = init!.headers as Record<string, string>
    expect(entetes.Authorization).toBe('Bearer jeton-de-test')
    expect(entetes['Content-Type']).toBeUndefined() // le navigateur ajoute la frontière multipart
  })

  it("refuse une photo de plus de 5 Mo sans l'envoyer", async () => {
    const { api, user } = await pageModification({})
    await user.upload(screen.getByLabelText('Ajouter des photos'), fichier('enorme.jpg', 5 * 1024 * 1024 + 1))
    expect(await screen.findByRole('alert')).toHaveTextContent('« enorme.jpg » : plus de 5 Mo.')
    expect(api.mock.calls.some(([, i]) => i?.method === 'POST' && i.body instanceof FormData)).toBe(false)
  })

  it('affiche le refus du serveur pour un fichier qui ne serait pas une vraie image', async () => {
    const { user } = await pageModification({
      [`POST /api/annonces/${ID}/photos`]: () => json(400, { message: "Ce fichier n'est pas une image lisible." }),
    })
    await user.upload(screen.getByLabelText('Ajouter des photos'), fichier('faux.jpg'))
    expect(await screen.findByRole('alert')).toHaveTextContent("« faux.jpg » : Ce fichier n'est pas une image lisible.")
    expect(screen.queryByAltText('Photo 1')).not.toBeInTheDocument()
  })

  it('supprime une photo après appel au serveur', async () => {
    const { api, user } = await pageModification(
      { [`DELETE /api/annonces/${ID}/photos/${P1}`]: () => new Response(null, { status: 204 }) },
      [P1, P2],
    )
    expect(screen.getAllByRole('img')).toHaveLength(2)
    await user.click(screen.getByRole('button', { name: 'Supprimer la photo 1' }))
    await waitFor(() => expect(screen.getAllByRole('img')).toHaveLength(1))
    expect(api.mock.calls.some(([u, i]) => u === `/api/annonces/${ID}/photos/${P1}` && i?.method === 'DELETE')).toBe(true)
  })

  it('masque le champ d\'envoi quand la limite de 8 photos est atteinte', async () => {
    const huit = Array.from({ length: 8 }, (_, i) => `b000000${i}-0000-0000-0000-000000000000`)
    simulerApi({
      'POST /api/auth/refresh': () => reponseConnexion(proprio),
      [`GET /api/annonces/${ID}`]: () => json(200, annonceTest({ photos: huit })),
    })
    afficher(`/annonces/${ID}/modifier`)
    expect(await screen.findByRole('heading', { name: 'Photos (8/8)' })).toBeInTheDocument()
    expect(screen.queryByLabelText('Ajouter des photos')).not.toBeInTheDocument()
  })
})

describe('Affichage public des photos', () => {
  it('le catalogue montre la photo de couverture, et rien de fictif sans photo', async () => {
    simulerApi({
      'GET /api/annonces?*': () =>
        json(200, pageTest([annonceTest({ photos: [P1, P2] }), annonceTest({ id: 'c2', titre: 'Sans photo' })])),
    })
    afficher('/catalogue')
    const cartes = await screen.findAllByRole('article')
    expect(within(cartes[0]).getByRole('img')).toHaveAttribute('src', `/api/annonces/${ID}/photos/${P1}`)
    expect(within(cartes[1]).queryByRole('img')).not.toBeInTheDocument()
  })

  it('le détail affiche la galerie et change de photo au clic sur une vignette', async () => {
    simulerApi({ [`GET /api/annonces/${ID}`]: () => json(200, annonceTest({ photos: [P1, P2] })) })
    const user = userEvent.setup()
    afficher(`/annonces/${ID}`)
    const principale = await screen.findByAltText(/Photo 1 sur 2/)
    expect(principale).toHaveAttribute('src', `/api/annonces/${ID}/photos/${P1}`)
    await user.click(screen.getByRole('button', { name: 'Voir la photo 2' }))
    expect(screen.getByAltText(/Photo 2 sur 2/)).toHaveAttribute('src', `/api/annonces/${ID}/photos/${P2}`)
  })

  it('le détail dit clairement quand une annonce n\'a pas de photo', async () => {
    simulerApi({ [`GET /api/annonces/${ID}`]: () => json(200, annonceTest()) })
    afficher(`/annonces/${ID}`)
    expect(await screen.findByText('Aucune photo pour cette annonce.')).toBeInTheDocument()
    expect(screen.queryByRole('img')).not.toBeInTheDocument()
  })
})
