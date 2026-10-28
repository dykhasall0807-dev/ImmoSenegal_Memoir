package sn.immosenegal.admin;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import sn.immosenegal.audit.AuditService;
import sn.immosenegal.common.ApiException;
import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

@Service
public class DemandeRoleService {

    /** Seuls ces rôles peuvent être demandés. ADMIN n'est JAMAIS demandable : il se crée uniquement par le serveur. */
    private static final Set<Role> ROLES_DEMANDABLES = Set.of(Role.PROPRIETAIRE, Role.AGENT);

    private final DemandeRoleRepository demandes;
    private final UserRepository users;
    private final AuditService audit;

    public DemandeRoleService(DemandeRoleRepository demandes, UserRepository users, AuditService audit) {
        this.demandes = demandes;
        this.users = users;
        this.audit = audit;
    }

    @Transactional
    public DemandeRoleResponse demander(UUID userId, Role roleDemande) {
        if (!ROLES_DEMANDABLES.contains(roleDemande)) {
            throw new ApiException(HttpStatus.BAD_REQUEST, "Ce rôle ne peut pas être demandé.");
        }
        User utilisateur = users.findById(userId)
            .orElseThrow(() -> new ApiException(HttpStatus.UNAUTHORIZED, "Session invalide."));
        if (utilisateur.getRole() != Role.CLIENT) {
            throw new ApiException(HttpStatus.CONFLICT, "Votre compte a déjà un rôle supplémentaire.");
        }
        if (demandes.existsByUserIdAndStatut(userId, StatutDemande.EN_ATTENTE)) {
            throw new ApiException(HttpStatus.CONFLICT, "Une demande est déjà en attente de validation.");
        }
        DemandeRole demande = demandes.save(new DemandeRole(utilisateur, roleDemande));
        audit.enregistrer("DEMANDE_ROLE", userId, "Rôle demandé : " + roleDemande);
        return DemandeRoleResponse.de(demande);
    }

    @Transactional(readOnly = true)
    public List<DemandeRoleResponse> lister(StatutDemande statut) {
        List<DemandeRole> liste = statut == null ? demandes.findAllByOrderByCreeLeDesc()
            : demandes.findByStatutOrderByCreeLeAsc(statut);
        return liste.stream().map(DemandeRoleResponse::de).toList();
    }

    @Transactional
    public DemandeRoleResponse approuver(UUID demandeId, UUID administrateurId) {
        DemandeRole demande = demandeEnAttente(demandeId);
        demande.getUser().setRole(demande.getRoleDemande());
        demande.decider(StatutDemande.APPROUVEE, administrateurId);
        audit.enregistrer("ROLE_APPROUVE", administrateurId,
            "Utilisateur " + demande.getUser().getId() + " devient " + demande.getRoleDemande());
        return DemandeRoleResponse.de(demande);
    }

    @Transactional
    public DemandeRoleResponse refuser(UUID demandeId, UUID administrateurId) {
        DemandeRole demande = demandeEnAttente(demandeId);
        demande.decider(StatutDemande.REFUSEE, administrateurId);
        audit.enregistrer("ROLE_REFUSE", administrateurId, "Demande " + demande.getId() + " refusée");
        return DemandeRoleResponse.de(demande);
    }

    private DemandeRole demandeEnAttente(UUID id) {
        DemandeRole demande = demandes.findById(id)
            .orElseThrow(() -> new ApiException(HttpStatus.NOT_FOUND, "Demande introuvable."));
        if (demande.getStatut() != StatutDemande.EN_ATTENTE) {
            throw new ApiException(HttpStatus.CONFLICT, "Cette demande a déjà été traitée.");
        }
        return demande;
    }
}
