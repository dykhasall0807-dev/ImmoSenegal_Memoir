package sn.immosenegal.annonce;

import java.time.Duration;
import java.util.Map;
import java.util.UUID;

import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import sn.immosenegal.config.OpenApiConfig;
import sn.immosenegal.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/annonces/{annonceId}/photos")
@Tag(name = "Photos", description = "Photos des annonces (lecture publique, écriture réservée au propriétaire)")
public class PhotoController {

    private final PhotoService service;

    public PhotoController(PhotoService service) {
        this.service = service;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "Ajouter une photo (JPEG ou PNG, 5 Mo max, 8 par annonce)",
        security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public ResponseEntity<Map<String, UUID>> ajouter(@PathVariable UUID annonceId,
                                                     @RequestParam("fichier") MultipartFile fichier,
                                                     @AuthenticationPrincipal AuthenticatedUser utilisateur) {
        UUID id = service.ajouter(annonceId, utilisateur, fichier);
        return ResponseEntity.status(HttpStatus.CREATED).body(Map.of("id", id));
    }

    @GetMapping("/{photoId}")
    @Operation(summary = "Afficher une photo (public)")
    public ResponseEntity<byte[]> lire(@PathVariable UUID annonceId, @PathVariable UUID photoId) {
        PhotoAnnonce photo = service.lire(annonceId, photoId);
        return ResponseEntity.ok()
            .contentType(MediaType.parseMediaType(photo.getTypeMime()))
            .cacheControl(CacheControl.maxAge(Duration.ofMinutes(5)).cachePublic())
            // L'image ne peut en aucun cas exécuter du contenu actif si elle était ouverte directement.
            .header("Content-Security-Policy", "default-src 'none'; sandbox")
            .body(photo.getContenu());
    }

    @DeleteMapping("/{photoId}")
    @Operation(summary = "Supprimer une photo (propriétaire de l'annonce ou ADMIN)",
        security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public ResponseEntity<Void> supprimer(@PathVariable UUID annonceId, @PathVariable UUID photoId,
                                          @AuthenticationPrincipal AuthenticatedUser utilisateur) {
        service.supprimer(annonceId, photoId, utilisateur);
        return ResponseEntity.noContent().build();
    }
}
