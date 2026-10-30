package sn.immosenegal.annonce;

import java.io.IOException;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import sn.immosenegal.audit.AuditService;
import sn.immosenegal.common.ApiException;
import sn.immosenegal.security.AuthenticatedUser;

/**
 * Photos d'annonces. Règles : seul le propriétaire de l'annonce (ou un ADMIN) ajoute/supprime ; le type réel du fichier
 * est reconnu par son contenu (jamais par son nom ni par l'en-tête envoyé), l'image est ré-encodée (métadonnées supprimées) ;
 * 5 Mo et 8 photos au maximum.
 */
@Service
public class PhotoService {

    static final int TAILLE_MAX_OCTETS = 5 * 1024 * 1024;
    static final int PHOTOS_MAX = 8;

    private final PhotoRepository photos;
    private final AnnonceService annonces;
    private final AuditService audit;

    public PhotoService(PhotoRepository photos, AnnonceService annonces, AuditService audit) {
        this.photos = photos;
        this.annonces = annonces;
        this.audit = audit;
    }

    @Transactional
    public UUID ajouter(UUID annonceId, AuthenticatedUser auteur, MultipartFile fichier) {
        annonces.annonceModifiable(annonceId, auteur);
        if (fichier == null || fichier.isEmpty()) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Aucun fichier reçu.");
        }
        if (fichier.getSize() > TAILLE_MAX_OCTETS) {
            throw new ApiException(HttpStatus.PAYLOAD_TOO_LARGE, "La photo dépasse 5 Mo.");
        }
        byte[] contenu;
        try {
            contenu = fichier.getBytes();
        } catch (IOException e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Fichier illisible.");
        }
        String type = reconnaitreType(contenu);
        if (type == null) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Format refusé : seuls les JPEG et PNG sont acceptés.");
        }
        if (photos.countByAnnonceId(annonceId) >= PHOTOS_MAX) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Une annonce ne peut pas avoir plus de " + PHOTOS_MAX + " photos.");
        }
        TraitementImage.Resultat propre;
        try {
            // Ré-encodage : vraie image obligatoire, métadonnées (GPS...) supprimées, taille réduite.
            propre = TraitementImage.nettoyer(contenu, type);
        } catch (TraitementImage.ImageRefusee e) {
            throw new ApiException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
        PhotoAnnonce photo = photos.save(new PhotoAnnonce(annonceId, propre.octets(), propre.typeMime(), photos.ordreMax(annonceId) + 1));
        audit.enregistrer("PHOTO_AJOUTEE", auteur.id(), "Annonce " + annonceId);
        return photo.getId();
    }

    @Transactional(readOnly = true)
    public PhotoAnnonce lire(UUID annonceId, UUID photoId) {
        return photos.findByIdAndAnnonceId(photoId, annonceId)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Photo introuvable."));
    }

    @Transactional
    public void supprimer(UUID annonceId, UUID photoId, AuthenticatedUser auteur) {
        annonces.annonceModifiable(annonceId, auteur);
        PhotoAnnonce photo = lire(annonceId, photoId);
        photos.delete(photo);
        audit.enregistrer("PHOTO_SUPPRIMEE", auteur.id(), "Annonce " + annonceId);
    }

    /** Reconnaît JPEG ou PNG d'après les premiers octets. Tout le reste (dont SVG, WebP, HTML, PDF) est refusé. */
    static String reconnaitreType(byte[] c) {
        if (c.length >= 3 && (c[0] & 0xFF) == 0xFF && (c[1] & 0xFF) == 0xD8 && (c[2] & 0xFF) == 0xFF) {
            return "image/jpeg";
        }
        if (c.length >= 8 && (c[0] & 0xFF) == 0x89 && c[1] == 'P' && c[2] == 'N' && c[3] == 'G'
            && c[4] == 0x0D && c[5] == 0x0A && c[6] == 0x1A && c[7] == 0x0A) {
            return "image/png";
        }
        return null;
    }
}
