import { expect, test, type Page } from '@playwright/test'

// Les identifiants de l'administrateur viennent de l'environnement (jamais écrits dans le code ni dans Git).
const ADMIN_EMAIL = process.env.E2E_ADMIN_EMAIL ?? ''
const ADMIN_PASSWORD = process.env.E2E_ADMIN_PASSWORD ?? ''
const suffixe = Date.now()
const CLIENT_EMAIL = `e2e.${suffixe}@example.test`
const CLIENT_PASSWORD = `Mdp-E2E-${suffixe}-Solide!`
const TITRE = `Villa E2E ${suffixe}`
// Plus petite image PNG valide (1 pixel) : suffit pour vérifier le chemin complet d'envoi, de nettoyage et d'affichage.
const PNG_1PX = Buffer.from('iVBORw0KGgoAAAANSUhEUgAAAAEAAAABCAYAAAAfFcSJAAAADUlEQVR42mNkYPhfDwAChwGA60e6kgAAAABJRU5ErkJggg==', 'base64')

async function connecter(page: Page, email: string, motDePasse: string) {
  await page.goto('/connexion')
  await page.getByLabel('Adresse email').fill(email)
  await page.getByLabel('Mot de passe').fill(motDePasse)
  await page.getByRole('button', { name: /se connecter/i }).click()
  // Après connexion, le site peut renvoyer vers la dernière page demandée : on passe par « Mon espace ».
  await page.getByRole('link', { name: 'Mon espace' }).click()
  await expect(page.getByRole('heading', { name: /Bonjour/ })).toBeVisible()
}

async function deconnecter(page: Page) {
  // On attend la réponse du serveur : c'est elle qui efface le cookie de session.
  const reponse = page.waitForResponse((r) => r.url().includes('/api/auth/logout'))
  await page.getByRole('button', { name: /déconnexion/i }).click()
  expect((await reponse).status()).toBe(204)
  await expect(page.getByRole('link', { name: 'Connexion' })).toBeVisible()
}

test.describe.configure({ mode: 'serial' })

// En cas d'échec, on joint le texte visible de la page et l'URL pour comprendre sans avoir les journaux complets.
test.afterEach(async ({ page }, info) => {
  if (info.status !== info.expectedStatus) {
    const texte = (await page.locator('body').innerText().catch(() => '')).replace(/\s+/g, ' ').slice(0, 600)
    console.log(`PAGE-URL: ${page.url()}\nPAGE-TEXTE: ${texte}`)
  }
})

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

  // 2. La session survit à un rechargement de page (cookie HttpOnly), et le cookie n'est pas lisible par JavaScript.
  await page.reload()
  await expect(page.getByRole('heading', { name: /Bonjour, Cliente E2E/ })).toBeVisible()
  expect(await page.evaluate(() => document.cookie)).not.toContain('immosenegal_refresh')
  expect(await page.evaluate(() => JSON.stringify({ ...localStorage, ...sessionStorage }))).not.toContain('eyJ')

  // 2b. Un Client ne peut pas publier (message clair dans l'interface ; le serveur refuserait aussi).
  await page.goto('/annonces/nouvelle')
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

  // 4b. Après déconnexion, un rechargement ne ramène pas la session.
  await page.reload()
  await expect(page.getByRole('link', { name: 'Connexion' })).toBeVisible()

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

  // 5b. Photos : un faux fichier est refusé par le serveur, une vraie image est acceptée et s'affiche.
  await expect(page.getByRole('heading', { name: "Modifier l'annonce" })).toBeVisible()
  await page.getByLabel('Ajouter des photos').setInputFiles({ name: 'faux.jpg', mimeType: 'image/jpeg', buffer: Buffer.from('<html>pas une image</html>') })
  await expect(page.getByRole('alert')).toContainText('faux.jpg')
  await page.getByLabel('Ajouter des photos').setInputFiles({ name: 'salon.png', mimeType: 'image/png', buffer: PNG_1PX })
  const miniature = page.getByAltText('Photo 1')
  await expect(miniature).toBeVisible()
  await expect.poll(() => miniature.evaluate((i: HTMLImageElement) => i.naturalWidth)).toBeGreaterThan(0)
  await page.getByRole('link', { name: 'Terminer' }).click()
  await expect(page.getByRole('link', { name: TITRE })).toBeVisible()

  // 6. L'annonce apparaît dans le catalogue public (sans être connecté) avec ses filtres.
  const visiteur = await page.context().browser()!.newPage({ baseURL: page.url().split('/').slice(0, 3).join('/') })
  await visiteur.goto('/catalogue?type=VILLA&transaction=VENTE')
  await expect(visiteur.getByRole('link', { name: TITRE })).toBeVisible()
  // La photo est lisible SANS être connecté (lecture publique) : l'image est réellement chargée par le navigateur.
  const couverture = visiteur.getByRole('article').filter({ hasText: TITRE }).getByRole('img')
  await expect.poll(() => couverture.evaluate((i: HTMLImageElement) => i.naturalWidth)).toBeGreaterThan(0)
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
