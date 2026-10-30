import { Route, Routes } from 'react-router-dom'
import Layout from './components/Layout'
import Home from './pages/Home'
import Catalogue from './pages/Catalogue'
import DetailAnnonce from './pages/DetailAnnonce'
import NotFound from './pages/NotFound'
import Connexion from './pages/Connexion'
import Inscription from './pages/Inscription'
import Dashboard from './pages/Dashboard'
import MesAnnonces from './pages/MesAnnonces'
import FormulaireAnnonce from './pages/FormulaireAnnonce'
import RequireAuth from './components/RequireAuth'
import { AuthProvider } from './auth'

export default function App() {
  return (
    <AuthProvider>
      <Routes>
        <Route element={<Layout />}>
          <Route index element={<Home />} />
          <Route path="catalogue" element={<Catalogue />} />
          <Route path="annonces/nouvelle" element={<RequireAuth><FormulaireAnnonce /></RequireAuth>} />
          <Route path="annonces/:id/modifier" element={<RequireAuth><FormulaireAnnonce /></RequireAuth>} />
          <Route path="annonces/:id" element={<DetailAnnonce />} />
          <Route path="mes-annonces" element={<RequireAuth><MesAnnonces /></RequireAuth>} />
          <Route path="connexion" element={<Connexion />} />
          <Route path="inscription" element={<Inscription />} />
          <Route
            path="tableau-de-bord"
            element={
              <RequireAuth>
                <Dashboard />
              </RequireAuth>
            }
          />
          <Route path="*" element={<NotFound />} />
        </Route>
      </Routes>
    </AuthProvider>
  )
}
