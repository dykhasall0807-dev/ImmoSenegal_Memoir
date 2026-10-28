package sn.immosenegal.audit;

import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

/**
 * Journal des événements sensibles (inscription, connexion, changement de rôle...).
 * RÈGLE : ne jamais passer de mot de passe ni de jeton dans {@code details}.
 */
@Service
public class AuditService {

    private static final Logger log = LoggerFactory.getLogger(AuditService.class);

    private final JdbcTemplate jdbc;

    public AuditService(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void enregistrer(String type, UUID userId, String details) {
        String texte = details == null ? null : (details.length() > 500 ? details.substring(0, 500) : details);
        log.info("audit type={} user={} details={}", type, userId, texte);
        jdbc.update("INSERT INTO audit_events (type, user_id, details) VALUES (?, ?, ?)", type, userId, texte);
    }
}
