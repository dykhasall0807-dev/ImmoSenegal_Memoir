package sn.immosenegal.admin;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface DemandeRoleRepository extends JpaRepository<DemandeRole, UUID> {

    boolean existsByUserIdAndStatut(UUID userId, StatutDemande statut);

    List<DemandeRole> findByStatutOrderByCreeLeAsc(StatutDemande statut);

    List<DemandeRole> findAllByOrderByCreeLeDesc();
}
