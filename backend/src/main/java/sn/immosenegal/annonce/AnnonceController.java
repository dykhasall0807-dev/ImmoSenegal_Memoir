package sn.immosenegal.annonce;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.immosenegal.config.OpenApiConfig;
import sn.immosenegal.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/annonces")
@Tag(name = "Annonces", description = "Catalogue public ; publication réservée aux propriétaires, agents et administrateurs")
public class AnnonceController {

    private final AnnonceService service;

    public AnnonceController(AnnonceService service) {
        this.service = service;
    }

    @GetMapping
    @Operation(summary = "Rechercher des annonces (public) : filtres combinables, pagination, tri",
        description = "tri = recent (défaut), prix_asc, prix_desc, superficie_desc. taille max : 50. "
            + "equipements : liste séparée par des virgules, tous doivent être présents.")
    public PageResponse<AnnonceResponse> rechercher(
            @RequestParam(required = false) TypeTransaction transaction,
            @RequestParam(required = false) TypeBien type,
            @RequestParam(required = false) Long prixMin,
            @RequestParam(required = false) Long prixMax,
            @RequestParam(required = false) BigDecimal superficieMin,
            @RequestParam(required = false) BigDecimal superficieMax,
            @RequestParam(required = false) String quartier,
            @RequestParam(required = false) String ville,
            @RequestParam(required = false) Integer piecesMin,
            @RequestParam(required = false) List<String> equipements,
            @RequestParam(required = false) StatutAnnonce statut,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer taille,
            @RequestParam(required = false) String tri) {
        CriteresRecherche criteres = new CriteresRecherche(transaction, type, prixMin, prixMax, superficieMin,
            superficieMax, quartier, ville, piecesMin, equipements, statut);
        return service.rechercher(criteres, page, taille, tri);
    }

    @GetMapping("/{id}")
    @Operation(summary = "Détail d'une annonce (public)")
    public AnnonceResponse obtenir(@PathVariable UUID id) {
        return service.obtenir(id);
    }

    @GetMapping("/mes-annonces")
    @Operation(summary = "Mes annonces", security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public PageResponse<AnnonceResponse> mesAnnonces(@AuthenticationPrincipal AuthenticatedUser utilisateur,
                                                     @RequestParam(required = false) Integer page,
                                                     @RequestParam(required = false) Integer taille) {
        return service.mesAnnonces(utilisateur, page, taille);
    }

    @PostMapping
    @Operation(summary = "Publier une annonce (PROPRIETAIRE, AGENT ou ADMIN)",
        security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public ResponseEntity<AnnonceResponse> creer(@Valid @RequestBody AnnonceRequest requete,
                                                 @AuthenticationPrincipal AuthenticatedUser utilisateur) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.creer(utilisateur, requete));
    }

    @PutMapping("/{id}")
    @Operation(summary = "Modifier une annonce (son propriétaire ou un ADMIN)",
        security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public AnnonceResponse modifier(@PathVariable UUID id, @Valid @RequestBody AnnonceRequest requete,
                                    @AuthenticationPrincipal AuthenticatedUser utilisateur) {
        return service.modifier(id, utilisateur, requete);
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Supprimer une annonce (son propriétaire ou un ADMIN)",
        security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public ResponseEntity<Void> supprimer(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser utilisateur) {
        service.supprimer(id, utilisateur);
        return ResponseEntity.noContent().build();
    }
}
