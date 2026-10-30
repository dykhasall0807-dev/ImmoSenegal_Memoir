import { useSyncExternalStore } from 'react'
import { abonnerServeurLent, serveurLent } from '../api'

/** Prévient le visiteur quand le serveur met du temps à répondre (réveil de l'hébergement gratuit). */
export default function BandeauServeurLent() {
  const lent = useSyncExternalStore(abonnerServeurLent, serveurLent)
  if (!lent) return null
  return (
    <p role="status" className="bg-amber-100 px-4 py-2 text-center text-sm text-amber-900">
      Le serveur se réveille : l'hébergement gratuit s'endort après un moment sans visite. Patientez environ une minute,
      la page se mettra à jour toute seule.
    </p>
  )
}
