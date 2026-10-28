import { render, screen } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { MemoryRouter } from 'react-router-dom'
import App from './App'

function afficher(chemin = '/') {
  return render(
    <MemoryRouter initialEntries={[chemin]}>
      <App />
    </MemoryRouter>,
  )
}

describe('Application', () => {
  it("affiche la page d'accueil avec le formulaire de recherche", () => {
    afficher()
    expect(screen.getByRole('heading', { level: 1 })).toHaveTextContent(/Sénégal/)
    expect(screen.getByRole('form', { name: /recherche de logement/i })).toBeInTheDocument()
  })

  it('signale chaque annonce fictive comme « Démonstration »', () => {
    afficher()
    const cartes = screen.getAllByRole('article')
    expect(cartes.length).toBeGreaterThan(0)
    cartes.forEach((c) => expect(c).toHaveTextContent('Démonstration'))
  })

  it('la recherche envoie les critères saisis vers le catalogue', async () => {
    const user = userEvent.setup()
    afficher()
    await user.selectOptions(screen.getByLabelText('Je veux'), 'VENTE')
    await user.selectOptions(screen.getByLabelText('Type de bien'), 'VILLA')
    await user.type(screen.getByLabelText('Quartier'), 'Néma')
    await user.type(screen.getByLabelText(/Budget max/), '50000000')
    await user.click(screen.getByRole('button', { name: /rechercher/i }))

    expect(await screen.findByRole('heading', { name: 'Catalogue' })).toBeInTheDocument()
    expect(screen.getByText('Achat')).toBeInTheDocument()
    expect(screen.getByText('Villa')).toBeInTheDocument()
    expect(screen.getByText('Quartier : Néma')).toBeInTheDocument()
    expect(screen.getByText('Budget max : 50 000 000 FCFA')).toBeInTheDocument()
  })

  it('affiche une page 404 pour une route inconnue', () => {
    afficher('/n-importe-quoi')
    expect(screen.getByText('Page introuvable')).toBeInTheDocument()
  })
})
