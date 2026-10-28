package sn.immosenegal.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import sn.immosenegal.audit.AuditService;
import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

/**
 * Création du PREMIER administrateur, uniquement par configuration serveur (variables d'environnement
 * BOOTSTRAP_ADMIN_EMAIL et BOOTSTRAP_ADMIN_PASSWORD). Aucun identifiant par défaut n'existe dans le code.
 * Si un ADMIN existe déjà, rien n'est créé. Il faut retirer ces variables après le premier démarrage.
 */
@Component
public class AdminBootstrap implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrap.class);

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final AuditService audit;
    private final String email;
    private final String motDePasse;

    public AdminBootstrap(UserRepository users, PasswordEncoder encoder, AuditService audit,
                          @Value("${app.bootstrap.admin-email:}") String email,
                          @Value("${app.bootstrap.admin-password:}") String motDePasse) {
        this.users = users;
        this.encoder = encoder;
        this.audit = audit;
        this.email = email;
        this.motDePasse = motDePasse;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() && motDePasse.isBlank()) {
            return;
        }
        if (email.isBlank() || motDePasse.isBlank()) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_EMAIL et BOOTSTRAP_ADMIN_PASSWORD doivent être définis ensemble.");
        }
        if (users.existsByRole(Role.ADMIN)) {
            log.info("Un administrateur existe déjà : les variables BOOTSTRAP_ADMIN_* sont ignorées. Vous pouvez les supprimer.");
            return;
        }
        if (motDePasse.length() < 12) {
            throw new IllegalStateException("BOOTSTRAP_ADMIN_PASSWORD doit contenir au moins 12 caractères.");
        }
        User admin = new User(email.trim().toLowerCase(java.util.Locale.ROOT), encoder.encode(motDePasse),
            "Administrateur", null, Role.ADMIN);
        users.save(admin);
        audit.enregistrer("ADMIN_INITIAL_CREE", admin.getId(), "Premier administrateur créé par configuration serveur");
        log.warn("Premier administrateur créé. Supprimez maintenant BOOTSTRAP_ADMIN_EMAIL et BOOTSTRAP_ADMIN_PASSWORD de l'environnement.");
    }
}
