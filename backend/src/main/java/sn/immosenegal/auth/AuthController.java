package sn.immosenegal.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.CookieValue;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.immosenegal.config.OpenApiConfig;
import sn.immosenegal.security.AuthenticatedUser;
import sn.immosenegal.security.JwtService;
import sn.immosenegal.user.UserRepository;
import sn.immosenegal.common.ApiException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription, connexion et profil")
public class AuthController {

    /** En-tête exigé sur refresh/logout : un site tiers ne peut pas l'ajouter sans autorisation CORS explicite. */
    static final String ENTETE_REQUIS = "X-Requested-With";
    static final String VALEUR_ENTETE = "immosenegal";

    private final AuthService service;
    private final UserRepository users;
    private final RefreshTokenService refresh;
    private final JwtService jwt;
    private final CookieRefresh cookies;

    public AuthController(AuthService service, UserRepository users, RefreshTokenService refresh, JwtService jwt,
                          CookieRefresh cookies) {
        this.service = service;
        this.users = users;
        this.refresh = refresh;
        this.jwt = jwt;
        this.cookies = cookies;
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un compte (toujours avec le rôle CLIENT)")
    public ResponseEntity<UserResponse> inscrire(@Valid @RequestBody RegisterRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.inscrire(requete));
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter et obtenir un jeton d'accès (429 après trop d'échecs)")
    public ResponseEntity<AuthResponse> connecter(@Valid @RequestBody LoginRequest requete, HttpServletRequest http) {
        AuthResponse reponse = service.connecter(requete, http.getRemoteAddr());
        String jetonRefresh = refresh.emettre(reponse.user().id());
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookies.creer(jetonRefresh)).body(reponse);
    }

    @PostMapping("/refresh")
    @Operation(summary = "Renouveler la session à partir du cookie HttpOnly (rotation du jeton)")
    public ResponseEntity<AuthResponse> renouveler(
            @CookieValue(name = CookieRefresh.NOM, required = false) String jetonRefresh,
            @RequestHeader(name = ENTETE_REQUIS, required = false) String entete) {
        exigerEntete(entete);
        RefreshTokenService.SessionRenouvelee session = refresh.faireTourner(jetonRefresh);
        AuthResponse reponse = new AuthResponse(jwt.creer(session.utilisateur().getId()), "Bearer",
            jwt.dureeSecondes(), UserResponse.de(session.utilisateur()));
        return ResponseEntity.ok().header(HttpHeaders.SET_COOKIE, cookies.creer(session.refreshToken())).body(reponse);
    }

    @PostMapping("/logout")
    @Operation(summary = "Se déconnecter : le jeton de rafraîchissement est révoqué côté serveur")
    public ResponseEntity<Void> deconnecter(
            @CookieValue(name = CookieRefresh.NOM, required = false) String jetonRefresh,
            @RequestHeader(name = ENTETE_REQUIS, required = false) String entete) {
        exigerEntete(entete);
        refresh.revoquer(jetonRefresh);
        return ResponseEntity.noContent().header(HttpHeaders.SET_COOKIE, cookies.effacer()).build();
    }

    private static void exigerEntete(String entete) {
        if (!VALEUR_ENTETE.equals(entete)) {
            throw new ApiException(HttpStatus.FORBIDDEN, "Requête refusée.");
        }
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecté", security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public UserResponse moi(@AuthenticationPrincipal AuthenticatedUser utilisateur) {
        return users.findById(utilisateur.id())
            .map(UserResponse::de)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide."));
    }
}
