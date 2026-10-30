package sn.immosenegal.annonce;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface PhotoRepository extends JpaRepository<PhotoAnnonce, UUID> {

    /** Référence légère (sans le contenu de l'image) pour les listes. */
    interface Reference {
        UUID getId();

        UUID getAnnonceId();
    }

    @Query("select p.id as id, p.annonceId as annonceId from PhotoAnnonce p where p.annonceId in :ids order by p.ordre, p.creeLe, p.id")
    List<Reference> referencesPour(@Param("ids") Collection<UUID> ids);

    long countByAnnonceId(UUID annonceId);

    Optional<PhotoAnnonce> findByIdAndAnnonceId(UUID id, UUID annonceId);

    @Query("select coalesce(max(p.ordre), 0) from PhotoAnnonce p where p.annonceId = :annonceId")
    int ordreMax(@Param("annonceId") UUID annonceId);
}
