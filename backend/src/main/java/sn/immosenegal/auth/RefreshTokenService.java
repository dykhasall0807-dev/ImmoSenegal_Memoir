package sn.immosenegal.auth;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.HexFormat;
import java.util.UUID;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.immosenegal.audit.AuditService;
import sn.immosenegal.common.ApiException;
import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

/**
 * Sessions longues par jeton de rafraîchissement à usage unique (rotation).
 * Si un jeton déjà utilisé est présenté de nouveau (vol probable), toutes les sessions du compte sont révoquées.
 */
@Service
public class RefreshTokenService {

    private static final String MESSAGE = "Session expirée. Veuillez vous reconnecter.";

    /** Résultat d'un renouvellement : l'utilisateur et le NOUVEAU jeton de rafraîchissement. */
    public record SessionRenouvelee(User utilisateur, String refreshToken) {
    }

    private final RefreshTokenRepository tokens;
    private final UserRepository users;
    private final AuditService audit;
    private final Clock horloge;
    private final Duration duree;
    private final SecureRandom aleatoire = new SecureRandom();

    public RefreshTokenService(RefreshTokenRepository tokens, UserRepository users, AuditService audit, Clock horloge,
                               @Value("${app.security.refresh-ttl-days:7}") long jours) {
        this.tokens = tokens;
        this.users = users;
        this.audit = audit;
        this.horloge = horloge;
        this.duree = Duration.ofDays(jours);
    }

    /** Crée un jeton pour cet utilisateur et retourne sa valeur en clair (à mettre dans le cookie, jamais en base). */
    @Transactional
    public String emettre(UUID userId) {
        byte[] octets = new byte[32];
        aleatoire.nextBytes(octets);
        String brut = Base64.getUrlEncoder().withoutPadding().encodeToString(octets);
        Instant maintenant = horloge.instant();
        tokens.save(new RefreshToken(userId, empreinte(brut), maintenant, maintenant.plus(duree)));
        return brut;
    }

    /** noRollbackFor : la révocation en cas de réutilisation doit être enregistrée malgré l'erreur 401. */
    @Transactional(noRollbackFor = ApiException.class)
    public SessionRenouvelee faireTourner(String brut) {
        if (brut == null || brut.isBlank()) {
            throw nonAuthentifie();
        }
        RefreshToken jeton = tokens.findByTokenHash(empreinte(brut)).orElseThrow(RefreshTokenService::nonAuthentifie);
        Instant maintenant = horloge.instant();

        if (jeton.estRevoque()) {
            tokens.revoquerTous(jeton.getUserId(), maintenant);
            audit.enregistrer("REFRESH_REUTILISE", jeton.getUserId(), "Jeton déjà utilisé : toutes les sessions révoquées");
            throw nonAuthentifie();
        }
        if (jeton.getExpireLe().isBefore(maintenant)) {
            throw nonAuthentifie();
        }
        User utilisateur = users.findById(jeton.getUserId()).filter(User::isActif)
            .orElseThrow(RefreshTokenService::nonAuthentifie);

        jeton.revoquer(maintenant);
        tokens.save(jeton);
        return new SessionRenouvelee(utilisateur, emettre(utilisateur.getId()));
    }

    /** Déconnexion côté serveur : le jeton ne pourra plus jamais servir. Sans effet si le jeton est inconnu. */
    @Transactional
    public void revoquer(String brut) {
        if (brut == null || brut.isBlank()) {
            return;
        }
        tokens.findByTokenHash(empreinte(brut)).ifPresent(j -> {
            if (!j.estRevoque()) {
                j.revoquer(horloge.instant());
                tokens.save(j);
                audit.enregistrer("DECONNEXION", j.getUserId(), null);
            }
        });
    }

    private static ApiException nonAuthentifie() {
        return new ApiException(HttpStatus.UNAUTHORIZED, MESSAGE);
    }

    static String empreinte(String brut) {
        try {
            byte[] hash = MessageDigest.getInstance("SHA-256").digest(brut.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
