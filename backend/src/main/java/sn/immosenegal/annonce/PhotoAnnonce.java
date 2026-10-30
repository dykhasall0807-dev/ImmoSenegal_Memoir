package sn.immosenegal.annonce;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "annonce_photos")
public class PhotoAnnonce {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "annonce_id", nullable = false)
    private UUID annonceId;

    @Column(nullable = false)
    private byte[] contenu;

    @Column(name = "type_mime", nullable = false, length = 20)
    private String typeMime;

    @Column(nullable = false)
    private int ordre;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe = Instant.now();

    protected PhotoAnnonce() {
        // requis par JPA
    }

    public PhotoAnnonce(UUID annonceId, byte[] contenu, String typeMime, int ordre) {
        this.annonceId = annonceId;
        this.contenu = contenu;
        this.typeMime = typeMime;
        this.ordre = ordre;
    }

    public UUID getId() { return id; }
    public byte[] getContenu() { return contenu; }
    public String getTypeMime() { return typeMime; }
}
