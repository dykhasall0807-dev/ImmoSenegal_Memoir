package sn.immosenegal.auth;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

/** Jeton de rafraîchissement : seule l'empreinte SHA-256 est conservée en base. */
@Entity
@Table(name = "refresh_tokens")
public class RefreshToken {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "token_hash", nullable = false, unique = true, length = 64)
    private String tokenHash;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe;

    @Column(name = "expire_le", nullable = false)
    private Instant expireLe;

    @Column(name = "revoque_le")
    private Instant revoqueLe;

    protected RefreshToken() {
        // requis par JPA
    }

    public RefreshToken(UUID userId, String tokenHash, Instant creeLe, Instant expireLe) {
        this.userId = userId;
        this.tokenHash = tokenHash;
        this.creeLe = creeLe;
        this.expireLe = expireLe;
    }

    public UUID getUserId() { return userId; }
    public Instant getExpireLe() { return expireLe; }
    public boolean estRevoque() { return revoqueLe != null; }

    public void revoquer(Instant quand) {
        if (revoqueLe == null) {
            revoqueLe = quand;
        }
    }
}
