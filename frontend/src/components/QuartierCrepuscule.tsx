import type { CSSProperties } from 'react'

/**
 * Illustration originale : un quartier au coucher du soleil, avec un baobab et des palmiers.
 * Les fenêtres s'allument une à une au chargement (animation CSS « fenetre », désactivée si l'utilisateur
 * demande moins d'animations). Purement décorative : masquée aux lecteurs d'écran.
 */

const SOL = 440
const PAS_X = 26
const PAS_Y = 34

type Immeuble = { x: number; l: number; h: number; citerne?: boolean }

/** Immeubles du premier plan (silhouettes sombres, avec fenêtres). */
const devant: Immeuble[] = [
  { x: -10, l: 120, h: 92 },
  { x: 108, l: 86, h: 126, citerne: true },
  { x: 190, l: 118, h: 80 },
  { x: 304, l: 92, h: 142 },
  { x: 392, l: 132, h: 108 },
  { x: 520, l: 96, h: 152, citerne: true },
  { x: 612, l: 104, h: 120 },
  { x: 704, l: 84, h: 150 },
  { x: 780, l: 70, h: 96 },
  { x: 930, l: 100, h: 170 },
  { x: 1024, l: 90, h: 220, citerne: true },
  { x: 1108, l: 116, h: 150 },
  { x: 1218, l: 96, h: 260 },
  { x: 1308, l: 84, h: 190, citerne: true },
  { x: 1384, l: 80, h: 130 },
]

/** Immeubles lointains (plus clairs, sans fenêtres) pour donner de la profondeur. */
const loin: Immeuble[] = [
  { x: 60, l: 100, h: 150 },
  { x: 240, l: 120, h: 190 },
  { x: 420, l: 90, h: 170 },
  { x: 560, l: 130, h: 210 },
  { x: 700, l: 110, h: 230 },
  { x: 960, l: 120, h: 250 },
  { x: 1150, l: 100, h: 230 },
  { x: 1290, l: 140, h: 210 },
]

/** Générateur pseudo-aléatoire à graine fixe : le dessin est identique à chaque affichage. */
function hasard(graine: number) {
  let a = graine
  return () => {
    a = (a + 0x6d2b79f5) | 0
    let t = Math.imul(a ^ (a >>> 15), 1 | a)
    t = (t + Math.imul(t ^ (t >>> 7), 61 | t)) ^ t
    return ((t ^ (t >>> 14)) >>> 0) / 4294967296
  }
}

type Fenetre = { x: number; y: number; delai: number; couleur: string }

const fenetres: Fenetre[] = (() => {
  const alea = hasard(2026)
  const liste: Fenetre[] = []
  for (const b of devant) {
    const colonnes = Math.floor((b.l - 18) / PAS_X)
    const lignes = Math.floor((b.h - 30) / PAS_Y)
    const largeurUtile = (colonnes - 1) * PAS_X + 11
    const debutX = b.x + (b.l - largeurUtile) / 2
    for (let r = 0; r < lignes; r++) {
      for (let c = 0; c < colonnes; c++) {
        if (alea() > 0.52) continue
        const x = debutX + c * PAS_X
        liste.push({
          x,
          y: SOL - b.h + 18 + r * PAS_Y,
          delai: Number((0.7 + (x / 1440) * 3.2 + alea() * 1.4).toFixed(2)),
          couleur: alea() > 0.7 ? '#ffd98a' : '#f5b942',
        })
      }
    }
  }
  return liste
})()

const COULEUR_SILHOUETTE = '#07142a'

export default function QuartierCrepuscule({ className }: { className?: string }) {
  return (
    <svg
      className={className}
      viewBox="0 0 1440 460"
      preserveAspectRatio="xMidYMax slice"
      aria-hidden="true"
      focusable="false"
    >
      <defs>
        <radialGradient id="qc-soleil">
          <stop offset="0" stopColor="#fff3cf" />
          <stop offset="0.6" stopColor="#f5b942" />
          <stop offset="1" stopColor="#f5b942" stopOpacity="0" />
        </radialGradient>
        <radialGradient id="qc-halo">
          <stop offset="0" stopColor="#f5b942" stopOpacity="0.55" />
          <stop offset="1" stopColor="#f5b942" stopOpacity="0" />
        </radialGradient>
        <filter id="qc-lueur" x="-50%" y="-50%" width="200%" height="200%">
          <feGaussianBlur stdDeviation="2.6" result="flou" />
          <feMerge>
            <feMergeNode in="flou" />
            <feMergeNode in="SourceGraphic" />
          </feMerge>
        </filter>
      </defs>

      <circle cx="790" cy="330" r="300" fill="url(#qc-halo)" />
      <circle cx="790" cy="326" r="104" fill="url(#qc-soleil)" />

      <g fill="#1b355f" opacity="0.92">
        {loin.map((b) => (
          <rect key={b.x} x={b.x} y={SOL - b.h} width={b.l} height={b.h + 30} />
        ))}
      </g>

      <g fill={COULEUR_SILHOUETTE}>
        {devant.map((b) => (
          <g key={b.x}>
            <rect x={b.x} y={SOL - b.h} width={b.l} height={b.h + 30} />
            {b.citerne && <rect x={b.x + b.l * 0.62} y={SOL - b.h - 13} width="20" height="13" rx="2" />}
          </g>
        ))}
      </g>

      <g filter="url(#qc-lueur)">
        {fenetres.map((f) => (
          <rect
            key={`${f.x}-${f.y}`}
            className="fenetre"
            x={f.x}
            y={f.y}
            width="11"
            height="16"
            rx="1.5"
            fill={f.couleur}
            style={{ '--delai': `${f.delai}s` } as CSSProperties}
          />
        ))}
      </g>

      {/* Baobab */}
      <g fill={COULEUR_SILHOUETTE} stroke={COULEUR_SILHOUETTE} strokeLinecap="round">
        <path
          d="M826 452 C834 400 826 350 838 304 C842 288 850 280 858 280 C866 280 872 288 876 304 C888 350 880 400 890 452 Z"
          stroke="none"
        />
        <g fill="none" strokeWidth="8">
          <path d="M858 290 C836 252 804 236 772 234" />
          <path d="M858 290 C850 244 846 206 830 180" />
          <path d="M858 290 C866 246 886 216 912 198" />
          <path d="M858 290 C886 262 926 254 960 262" />
          <path d="M858 292 C830 284 796 290 770 306" />
        </g>
        {/* Touffes de feuilles au bout des branches (un baobab a un feuillage clairsemé). */}
        <g stroke="none">
          {[
            [772, 234],
            [830, 180],
            [912, 198],
            [960, 262],
            [770, 306],
          ].map(([cx, cy]) => (
            <g key={`${cx}-${cy}`}>
              <circle cx={cx - 15} cy={cy + 3} r="10" />
              <circle cx={cx} cy={cy - 5} r="13" />
              <circle cx={cx + 15} cy={cy + 2} r="10" />
            </g>
          ))}
        </g>
      </g>

      {/* Palmier */}
      <g fill="none" stroke={COULEUR_SILHOUETTE} strokeLinecap="round">
        <path d="M1190 452 C1196 380 1200 320 1214 262" strokeWidth="7" />
        <g strokeWidth="5">
          <path d="M1214 262 C1190 240 1160 238 1136 252" />
          <path d="M1214 262 C1196 226 1170 214 1140 216" />
          <path d="M1214 262 C1220 222 1240 202 1266 196" />
          <path d="M1214 262 C1240 240 1272 240 1296 256" />
          <path d="M1214 262 C1224 236 1214 218 1204 200" />
        </g>
      </g>

      <path d="M0 436 Q360 428 720 438 T1440 434 V460 H0 Z" fill="#050f20" />
    </svg>
  )
}
