import { expect, test, type Page } from '@playwright/test'

// Les identifiants de l'administrateur viennent de l'environnement (jamais écrits dans le code ni dans Git).
const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? ''
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? ''
const suffixe = Date.now()
const CLIENT_EMAIL = `e2e.${suffixe}@example.test`
const CLIENT_PASSWORD = `Mdp-E2E-${suffixe}-Solide!`
const TITRE = `Villa E2E ${suffixe}`

async function connecter(page: Page, email: string, motDePasse: string) {
  await page.goto('/connexion')
  await page.getByLabel('Adresse email').fill(email)
  await page.getByLabel('Mot de passe').fill(motDePasse)
  await page.getByRole('button', { name: /se connecter/i }).click()
  await expect(page.getByRole('heading', { name: /Bonjour/ })).toBeVisible()
}

async function deconnecter(page: Page) {
  await page.getByRole('button', { name: /déconnexion/i }).click()
  await expect(page.getByRole('link', { name: 'Connexion' })).toBeVisible()
}

test.describe.configure({ mode: 'serial' })

test('parcours complet : inscription, demande de rôle, validation admin, publication, catalogue, suppression', async ({ page, request }) => {
  expect(ADMIN_EMAIL, 'E2E_ADMIN_EMAIL doit être défini').not.toBe('')

  // 0. Sécurité : sans connexion, le serveur refuse la publication.
  const anonyme = await request.post('/api/annonces', { data: { titre: 'x' } })
  expect(anonyme.status()).toBe(401)

  // 1. Inscription publique : le compte est un Client.
  await page.goto('/inscription')
  await page.getByLabel('Nom complet').fill('Cliente E2E')
  await page.getByLabel('Adresse email').fill(CLIENT_EMAIL)
  await page.getByLabel('Mot de passe', { exact: true }).fill(CLIENT_PASSWORD)
  await page.getByLabel('Confirmer le mot de passe').fill(CLIENT_PASSWORD)
  await page.getByRole('button', { name: 'Créer mon compte' }).click()
  await expect(page.getByRole('heading', { name: /Bonjour, Cliente E2E/ })).toBeVisible()
  await expect(page.getByText('Client', { exact: true })).toBeVisible()

  // 2. Un Client ne peut pas publier. (Le rechargement de page efface la session en mémoire : limite connue.)
  await page.goto('/annonces/nouvelle')
  await expect(page).toHaveURL(/connexion/)
  await page.getByLabel('Adresse email').fill(CLIENT_EMAIL)
  await page.getByLabel('Mot de passe').fill(CLIENT_PASSWORD)
  await page.getByRole('button', { name: /se connecter/i }).click()
  await expect(page.getByRole('alert')).toContainText('compte Client')
  await page.getByRole('link', { name: 'Mon espace' }).click()
  await expect(page.getByRole('heading', { name: /Bonjour/ })).toBeVisible()

  // 3. Demande du rôle Propriétaire.
  await page.getByLabel('Rôle souhaité').selectOption('PROPRIETAIRE')
  await page.getByRole('button', { name: /envoyer la demande/i }).click()
  await expect(page.getByRole('status')).toContainText('en attente de validation')
  await deconnecter(page)

  // 4. L'administrateur approuve.
  await connecter(page, ADMIN_EMAIL, ADMIN_PASSWORD)
  await page.getByRole('link', { name: /demandes de rôle/i }).click()
  const carte = page.getByRole('listitem').filter({ hasText: CLIENT_EMAIL })
  await expect(carte).toBeVisible()
  await carte.getByRole('button', { name: 'Approuver' }).click()
  await expect(page.getByRole('listitem').filter({ hasText: CLIENT_EMAIL })).toHaveCount(0)
  await deconnecter(page)

  // 5. Le nouveau Propriétaire publie une annonce.
  await connecter(page, CLIENT_EMAIL, CLIENT_PASSWORD)
  await page.getByRole('link', { name: /gérer mes annonces/i }).click()
  await page.getByRole('link', { name: /publier une annonce/i }).click()
  await page.getByLabel('Titre').fill(TITRE)
  await page.getByLabel('Transaction').selectOption('VENTE')
  await page.getByLabel('Type de bien').selectOption('VILLA')
  await page.getByLabel('Prix (FCFA)').fill('45000000')
  await page.getByLabel('Superficie (m²)').fill('300')
  await page.getByLabel('Quartier').fill('Néma')
  await page.getByLabel('Équipements').fill('wifi, parking')
  await page.getByRole('button', { name: /publier l'annonce/i }).click()
  await expect(page.getByRole('link', { name: TITRE })).toBeVisible()

  // 6. L'annonce apparaît dans le catalogue public (sans être connecté) avec ses filtres.
  const visiteur = await page.context().browser()!.newPage({ baseURL: page.url().split('/').slice(0, 3).join('/') })
  await visiteur.goto('/catalogue?type=VILLA&transaction=VENTE')
  await expect(visiteur.getByRole('link', { name: TITRE })).toBeVisible()
  await visiteur.getByRole('link', { name: TITRE }).click()
  await expect(visiteur.getByRole('heading', { level: 1, name: TITRE })).toBeVisible()
  await expect(visiteur.getByText('45 000 000 FCFA')).toBeVisible()
  await visiteur.close()

  // 7. Modification du statut puis suppression avec confirmation.
  const ligne = page.getByRole('listitem').filter({ hasText: TITRE })
  await ligne.getByRole('link', { name: 'Modifier' }).click()
  await page.getByLabel('Statut').selectOption('RESERVE')
  await page.getByRole('button', { name: 'Enregistrer' }).click()
  await expect(page.getByRole('listitem').filter({ hasText: TITRE })).toContainText('Réservé')
  await page.getByRole('listitem').filter({ hasText: TITRE }).getByRole('button', { name: 'Supprimer' }).click()
  await page.getByRole('button', { name: /confirmer la suppression/i }).click()
  await expect(page.getByRole('link', { name: TITRE })).toHaveCount(0)
})
