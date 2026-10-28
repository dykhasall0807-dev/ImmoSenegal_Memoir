package sn.immosenegal.admin;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import sn.immosenegal.config.OpenApiConfig;
import sn.immosenegal.security.AuthenticatedUser;

@RestController
@RequestMapping("/api/auth/role-requests")
@Tag(name = "Authentification")
@SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER)
public class RoleRequestController {

    private final DemandeRoleService service;

    public RoleRequestController(DemandeRoleService service) {
        this.service = service;
    }

    @PostMapping
    @Operation(summary = "Demander le rôle PROPRIETAIRE ou AGENT (à valider par un administrateur)")
    public ResponseEntity<DemandeRoleResponse> demander(@Valid @RequestBody DemandeRoleRequest requete,
                                                        @AuthenticationPrincipal AuthenticatedUser utilisateur) {
        return ResponseEntity.status(HttpStatus.CREATED).body(service.demander(utilisateur.id(), requete.role()));
    }
}
