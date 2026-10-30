package sn.immosenegal.annonce;

import java.time.Instant;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;
import java.util.Map;
import java.util.List;
import java.util.Collection;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.immosenegal.audit.AuditService;
import sn.immosenegal.common.ApiException;
import sn.immosenegal.security.AuthenticatedUser;
import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

@Service
public class AnnonceService {

    private static final Set<Role> ROLES_PUBLICATION = Set.of(Role.PROPRIETAIRE, Role.AGENT, Role.ADMIN);
    private static final int TAILLE_MAX = 50;
    private static final int TAILLE_PAR_DEFAUT = 12;

    private final AnnonceRepository annonces;
    private final UserRepository users;
    private final AuditService audit;
    private final PhotoRepository photos;

    public AnnonceService(AnnonceRepository annonces, UserRepository users, AuditService audit, PhotoRepository photos) {
        this.annonces = annonces;
        this.users = users;
        this.audit = audit;
        this.photos = photos;
    }

    @Transactional
    public AnnonceResponse creer(AuthenticatedUser auteur, AnnonceRequest requete) {
        if (!ROLES_PUBLICATION.contains(auteur.role())) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Seuls les propriétaires et agents peuvent publier une annonce.");
        }
        // Le propriétaire est TOUJOURS l'utilisateur connecté, jamais une valeur envoyée par le client.
        User proprietaire = users.findById(auteur.id())
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide."));
        Annonce annonce = new Annonce(proprietaire);
        appliquer(annonce, requete);
        annonce.setStatut(StatutAnnonce.DISPONIBLE);
        annonces.save(annonce);
        audit.enregistrer("ANNONCE_CREEE", auteur.id(), "Annonce " + annonce.getId());
        return AnnonceResponse.de(annonce, List.of());
    }

    @Transactional
    public AnnonceResponse modifier(UUID id, AuthenticatedUser auteur, AnnonceRequest requete) {
        Annonce annonce = trouver(id);
        verifierDroits(annonce, auteur);
        appliquer(annonce, requete);
        if (requete.statut() != null) {
            annonce.setStatut(requete.statut());
        }
        audit.enregistrer("ANNONCE_MODIFIEE", auteur.id(), "Annonce " + id);
        return AnnonceResponse.de(annonce, photosDe(List.of(id)).getOrDefault(id, List.of()));
    }

    @Transactional
    public void supprimer(UUID id, AuthenticatedUser auteur) {
        Annonce annonce = trouver(id);
        verifierDroits(annonce, auteur);
        annonces.delete(annonce);
        boolean parAdminSurAutrui = !annonce.getProprietaire().getId().equals(auteur.id());
        audit.enregistrer(parAdminSurAutrui ? "ANNONCE_SUPPRIMEE_PAR_ADMIN" : "ANNONCE_SUPPRIMEE", auteur.id(), "Annonce " + id);
    }

    @Transactional(readOnly = true)
    public AnnonceResponse obtenir(UUID id) {
        Annonce annonce = trouver(id);
        return AnnonceResponse.de(annonce, photosDe(List.of(id)).getOrDefault(id, List.of()));
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnonceResponse> rechercher(CriteresRecherche criteres, Integer page, Integer taille, String tri) {
        return enPage(annonces.findAll(AnnonceSpecifications.depuis(criteres), pageable(page, taille, tri)));
    }

    @Transactional(readOnly = true)
    public PageResponse<AnnonceResponse> mesAnnonces(AuthenticatedUser utilisateur, Integer page, Integer taille) {
        return enPage(annonces.findByProprietaireId(utilisateur.id(), pageable(page, taille, "recent")));
    }

    // ---------- Règles internes ----------

    /** Vérifie que l'utilisateur peut modifier cette annonce (propriétaire ou ADMIN) et la retourne. */
    Annonce annonceModifiable(UUID id, AuthenticatedUser utilisateur) {
        Annonce annonce = trouver(id);
        verifierDroits(annonce, utilisateur);
        return annonce;
    }

    private PageResponse<AnnonceResponse> enPage(Page<Annonce> page) {
        Map<UUID, List<UUID>> parAnnonce = photosDe(page.getContent().stream().map(Annonce::getId).toList());
        return PageResponse.de(page.map(a -> AnnonceResponse.de(a, parAnnonce.getOrDefault(a.getId(), List.of()))));
    }

    /** Identifiants de photos par annonce, en une seule requête (sans charger les images). */
    private Map<UUID, List<UUID>> photosDe(Collection<UUID> ids) {
        if (ids.isEmpty()) {
            return Map.of();
        }
        return photos.referencesPour(ids).stream().collect(
            Collectors.groupingBy(PhotoRepository.Reference::getAnnonceId,
                Collectors.mapping(PhotoRepository.Reference::getId, Collectors.toList())));
    }

    private Annonce trouver(UUID id) {
        return annonces.findById(id).orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Annonce introuvable."));
    }

    /** Seul le propriétaire de l'annonce, ou un ADMIN (modération), peut la modifier ou la supprimer. */
    private void verifierDroits(Annonce annonce, AuthenticatedUser utilisateur) {
        boolean proprietaire = annonce.getProprietaire().getId().equals(utilisateur.id());
        if (!proprietaire && utilisateur.role() != Role.ADMIN) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Cette annonce ne vous appartient pas.");
        }
    }

    private void appliquer(Annonce annonce, AnnonceRequest r) {
        if ((r.latitude() == null) != (r.longitude() == null)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "La latitude et la longitude doivent être renseignées ensemble.");
        }
        annonce.setTitre(r.titre().trim());
        annonce.setDescription(r.description() == null || r.description().isBlank() ? null : r.description().trim());
        annonce.setTypeBien(r.typeBien());
        annonce.setTypeTransaction(r.transaction());
        annonce.setPrix(r.prix());
        annonce.setSuperficie(r.superficie());
        // Un terrain n'a pas de pièces.
        annonce.setPieces(r.typeBien() == TypeBien.TERRAIN ? null : r.pieces());
        annonce.setQuartier(r.quartier().trim());
        annonce.setVille(r.ville().trim());
        annonce.setAdresse(r.adresse() == null || r.adresse().isBlank() ? null : r.adresse().trim());
        annonce.setLatitude(r.latitude());
        annonce.setLongitude(r.longitude());
        annonce.remplacerEquipements(normaliser(r.equipements()));
        annonce.setModifieLe(Instant.now());
    }

    private static Set<String> normaliser(Set<String> equipements) {
        Set<String> resultat = new HashSet<>();
        if (equipements != null) {
            for (String e : equipements) {
                String propre = e.trim().toLowerCase(Locale.ROOT);
                if (!propre.isEmpty()) {
                    resultat.add(propre);
                }
            }
        }
        return resultat;
    }

    private static Pageable pageable(Integer page, Integer taille, String tri) {
        int p = page == null ? 0 : Math.max(0, page);
        int t = taille == null ? TAILLE_PAR_DEFAUT : Math.min(TAILLE_MAX, Math.max(1, taille));
        return PageRequest.of(p, t, trier(tri));
    }

    /** Tri en liste blanche (jamais un nom de colonne fourni par l'utilisateur). Le tri par id rend la pagination stable. */
    private static Sort trier(String tri) {
        Sort base = switch (tri == null || tri.isBlank() ? "recent" : tri) {
            case "recent" -> Sort.by(Sort.Direction.DESC, "creeLe");
            case "prix_asc" -> Sort.by(Sort.Direction.ASC, "prix");
            case "prix_desc" -> Sort.by(Sort.Direction.DESC, "prix");
            case "superficie_desc" -> Sort.by(Sort.Direction.DESC, "superficie");
            default -> throw new ApiException(HttpStatus.BAD_REQUEST,
                "Tri inconnu. Valeurs possibles : recent, prix_asc, prix_desc, superficie_desc.");
        };
        return base.and(Sort.by("id"));
    }
}
