import { act, render, screen } from '@testing-library/react'
import { MemoryRouter } from 'react-router-dom'
import { afterEach, beforeEach, describe, expect, it, vi } from 'vitest'
import App from '../App'
import { annonceTest, json, pageTest, simulerApi } from '../test/apiSimulee'

beforeEach(() => vi.useFakeTimers({ shouldAdvanceTime: true }))
afterEach(() => {
  vi.useRealTimers()
  vi.unstubAllGlobals()
})

describe('Serveur lent (réveil de l\'hébergement)', () => {
  it("affiche un message d'attente après 4 secondes, puis le retire quand le serveur répond", async () => {
    let repondre: (r: Response) => void = () => undefined
    simulerApi({ 'GET /api/annonces?*': () => new Promise<Response>((r) => (repondre = r)) })
    render(
      <MemoryRouter initialEntries={['/catalogue']}>
        <App />
      </MemoryRouter>,
    )
    expect(screen.queryByText(/le serveur se réveille/i)).not.toBeInTheDocument()

    await act(async () => {
      await vi.advanceTimersByTimeAsync(4100)
    })
    expect(screen.getByText(/le serveur se réveille/i)).toBeInTheDocument()

    await act(async () => {
      repondre(json(200, pageTest([annonceTest({ titre: 'Villa des Pins' })])))
      await vi.advanceTimersByTimeAsync(10)
    })
    expect(await screen.findByRole('link', { name: 'Villa des Pins' })).toBeInTheDocument()
    expect(screen.queryByText(/le serveur se réveille/i)).not.toBeInTheDocument()
  })

  it("n'affiche rien quand le serveur répond vite", async () => {
    simulerApi({ 'GET /api/annonces?*': () => json(200, pageTest([annonceTest()])) })
    render(
      <MemoryRouter initialEntries={['/catalogue']}>
        <App />
      </MemoryRouter>,
    )
    await screen.findByRole('link', { name: 'Appartement lumineux' })
    await act(async () => {
      await vi.advanceTimersByTimeAsync(5000)
    })
    expect(screen.queryByText(/le serveur se réveille/i)).not.toBeInTheDocument()
  })
})
