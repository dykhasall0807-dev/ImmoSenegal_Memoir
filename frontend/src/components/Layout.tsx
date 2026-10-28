import { NavLink, Outlet, Link } from 'react-router-dom'
import { Building2 } from 'lucide-react'

const lienClasse = ({ isActive }: { isActive: boolean }) =>
  `rounded-md px-3 py-2 text-sm font-medium transition-colors ${
    isActive ? 'bg-white/15 text-white' : 'text-nuit-100 hover:bg-white/10 hover:text-white'
  }`

export default function Layout() {
  return (
    <div className="flex min-h-screen flex-col">
      <a
        href="#contenu"
        className="sr-only focus:not-sr-only focus:absolute focus:left-4 focus:top-4 focus:z-50 focus:rounded-md focus:bg-dore-500 focus:px-4 focus:py-2 focus:text-nuit-950"
      >
        Aller au contenu
      </a>

      <header className="bg-nuit-900 text-white shadow">
        <div className="mx-auto flex max-w-6xl items-center justify-between gap-4 px-4 py-3">
          <Link to="/" className="flex items-center gap-2 text-lg font-bold">
            <Building2 aria-hidden className="h-6 w-6 text-emeraude-500" />
            <span>
              Immo<span className="text-dore-500">Sénégal</span>
            </span>
          </Link>
          <nav aria-label="Navigation principale" className="flex items-center gap-1">
            <NavLink to="/" end className={lienClasse}>
              Accueil
            </NavLink>
            <NavLink to="/catalogue" className={lienClasse}>
              Catalogue
            </NavLink>
          </nav>
        </div>
      </header>

      <main id="contenu" className="flex-1">
        <Outlet />
      </main>

      <footer className="bg-nuit-950 py-6 text-center text-sm text-nuit-100/80">
        <p>© {new Date().getFullYear()} ImmoSénégal – projet de mémoire L3 Ingénierie Informatique, UASZ</p>
      </footer>
    </div>
  )
}
