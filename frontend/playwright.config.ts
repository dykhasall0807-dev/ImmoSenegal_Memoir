import { defineConfig } from '@playwright/test'

// Test de bout en bout : vrai navigateur + vrai backend + vraie base PostgreSQL (lancés par la CI, voir ci.yml).
export default defineConfig({
  testDir: './e2e',
  testMatch: '**/*.e2e.ts',
  timeout: 90_000,
  retries: 0,
  reporter: [['list']],
  use: { baseURL: process.env.E2E_BASE_URL ?? 'http://localhost:4173', locale: 'fr-FR' },
})
