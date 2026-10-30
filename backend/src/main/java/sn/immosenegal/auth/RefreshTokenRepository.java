package sn.immosenegal.auth;

import java.time.Instant;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface RefreshTokenRepository extends JpaRepository<RefreshToken, UUID> {

    Optional<RefreshToken> findByTokenHash(String tokenHash);

    /** Révoque toutes les sessions encore actives d'un utilisateur. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("update RefreshToken t set t.revoqueLe = :maintenant where t.userId = :userId and t.revoqueLe is null")
    int revoquerTous(@Param("userId") UUID userId, @Param("maintenant") Instant maintenant);
}
