package sn.immosenegal.annonce;

import java.util.UUID;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface AnnonceRepository extends JpaRepository<Annonce, UUID>, JpaSpecificationExecutor<Annonce> {

    Page<Annonce> findByProprietaireId(UUID proprietaireId, Pageable pageable);
}
