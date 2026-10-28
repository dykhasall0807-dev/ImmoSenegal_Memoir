package sn.immosenegal.admin;

import java.util.List;
import java.util.UUID;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import sn.immosenegal.config.OpenApiConfig;
import sn.immosenegal.security.AuthenticatedUser;

/** Toutes les routes /api/admin/** exigent le rôle ADMIN (règle dans SecurityConfig). */
@RestController
@RequestMapping("/api/admin")
@Tag(name = "Administration", description = "Réservé au rôle ADMIN")
@SecurityRequirement(name = OpenApiConfig.SCHEMA_BEARER)
public class AdminController {

    private final DemandeRoleService service;

    public AdminController(DemandeRoleService service) {
        this.service = service;
    }

    @GetMapping("/role-requests")
    @Operation(summary = "Lister les demandes de rôle (filtre optionnel par statut)")
    public List<DemandeRoleResponse> lister(@RequestParam(required = false) StatutDemande statut) {
        return service.lister(statut);
    }

    @PostMapping("/role-requests/{id}/approve")
    @Operation(summary = "Approuver une demande : l'utilisateur obtient le rôle demandé")
    public DemandeRoleResponse approuver(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser admin) {
        return service.approuver(id, admin.id());
    }

    @PostMapping("/role-requests/{id}/reject")
    @Operation(summary = "Refuser une demande")
    public DemandeRoleResponse refuser(@PathVariable UUID id, @AuthenticationPrincipal AuthenticatedUser admin) {
        return service.refuser(id, admin.id());
    }
}
