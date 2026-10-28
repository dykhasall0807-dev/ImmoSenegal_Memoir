package sn.immosenegal.auth;

import java.util.Locale;

import org.springframework.http.HttpStatus;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import sn.immosenegal.audit.AuditService;
import sn.immosenegal.common.ApiException;
import sn.immosenegal.security.JwtService;
import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

@Service
public class AuthService {

    private static final String MESSAGE_IDENTIFIANTS = "Email ou mot de passe incorrect.";

    private final UserRepository users;
    private final PasswordEncoder encoder;
    private final JwtService jwt;
    private final AuditService audit;
    /** Hash factice : permet de comparer quand même un mot de passe si l'email est inconnu (même durée de réponse). */
    private final String hashFactice;

    public AuthService(UserRepository users, PasswordEncoder encoder, JwtService jwt, AuditService audit) {
        this.users = users;
        this.encoder = encoder;
        this.jwt = jwt;
        this.audit = audit;
        this.hashFactice = encoder.encode("mot-de-passe-factice-pour-temps-constant");
    }

    public UserResponse inscrire(RegisterRequest requete) {
        String email = normaliser(requete.email());
        if (users.existsByEmail(email)) {
            throw new ApiException(HttpStatus.CONFLICT, "Un compte existe déjà avec cet email.");
        }
        String telephone = requete.telephone() == null || requete.telephone().isBlank() ? null : requete.telephone().trim();
        // Le rôle est imposé ici, côté serveur : toute inscription publique est un CLIENT.
        User utilisateur = new User(email, encoder.encode(requete.password()), requete.nomComplet().trim(), telephone, Role.CLIENT);
        users.save(utilisateur);
        audit.enregistrer("INSCRIPTION", utilisateur.getId(), "Nouveau compte CLIENT");
        return UserResponse.de(utilisateur);
    }

    public AuthResponse connecter(LoginRequest requete) {
        String email = normaliser(requete.email());
        User utilisateur = users.findByEmail(email).orElse(null);

        String hash = utilisateur != null ? utilisateur.getPasswordHash() : hashFactice;
        boolean motDePasseValide = encoder.matches(requete.password(), hash);

        if (utilisateur == null || !utilisateur.isActif() || !motDePasseValide) {
            audit.enregistrer("CONNEXION_ECHEC", utilisateur != null ? utilisateur.getId() : null, "Identifiants refusés");
            // Même message dans tous les cas : on ne révèle pas si l'email existe.
            throw new ApiException(HttpStatus.UNAUTHORIZED, MESSAGE_IDENTIFIANTS);
        }

        audit.enregistrer("CONNEXION_OK", utilisateur.getId(), null);
        return new AuthResponse(jwt.creer(utilisateur.getId()), "Bearer", jwt.dureeSecondes(), UserResponse.de(utilisateur));
    }

    private static String normaliser(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }
}
