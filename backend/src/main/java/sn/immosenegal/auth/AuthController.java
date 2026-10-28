package sn.immosenegal.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.immosenegal.config.OpenApiConfig;
import sn.immosenegal.security.AuthenticatedUser;
import sn.immosenegal.user.UserRepository;
import sn.immosenegal.common.ApiException;

@RestController
@RequestMapping("/api/auth")
@Tag(name = "Authentification", description = "Inscription, connexion et profil")
public class AuthController {

    private final AuthService service;
    private final UserRepository users;

    public AuthController(AuthService service, UserRepository users) {
        this.service = service;
        this.users = users;
    }

    @PostMapping("/register")
    @Operation(summary = "Créer un compte (toujours avec le rôle CLIENT)")
    public ResponseEntity<UserResponse> inscrire(@Valid @RequestBody RegisterRequest requete) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.inscrire(requete));
    }

    @PostMapping("/login")
    @Operation(summary = "Se connecter et obtenir un jeton d'accès (429 après trop d'échecs)")
    public AuthResponse connecter(@Valid @RequestBody LoginRequest requete, HttpServletRequest http) {
        return service.connecter(requete, http.getRemoteAddr());
    }

    @GetMapping("/me")
    @Operation(summary = "Profil de l'utilisateur connecté", security = @SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER))
    public UserResponse moi(@AuthenticationPrincipal AuthenticatedUser utilisateur) {
        return users.findById(utilisateur.id())
            .map(UserResponse::de)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide."));
    }
}
