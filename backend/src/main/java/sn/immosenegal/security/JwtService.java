package sn.immosenegal.security;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.time.Instant;
import java.util.Date;
import java.util.Optional;
import java.util.UUID;

import javax.crypto.SecretKey;

import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

/**
 * Création et lecture des jetons d'accès. Le jeton ne contient QUE l'identifiant de l'utilisateur :
 * le rôle est toujours relu en base, donc un jeton ne peut jamais « porter » un rôle falsifié.
 */
@Service
public class JwtService {

    static final String ISSUER = "immosenegal";

    private final SecretKey key;
    private final Duration duree;

    public JwtService(@Value("${app.jwt.secret}") String secret,
                      @Value("${app.jwt.access-ttl-minutes:15}") long dureeMinutes) {
        byte[] octets = secret.getBytes(StandardCharsets.UTF_8);
        if (octets.length < 32) {
            throw new IllegalStateException("JWT_SECRET doit contenir au moins 32 caractères.");
        }
        this.key = Keys.hmacShaKeyFor(octets);
        this.duree = Duration.ofMinutes(dureeMinutes);
    }

    public String creer(UUID userId) {
        Instant maintenant = Instant.now();
        return Jwts.builder()
            .issuer(ISSUER)
            .subject(userId.toString())
            .issuedAt(Date.from(maintenant))
            .expiration(Date.from(maintenant.plus(duree)))
            .signWith(key)
            .compact();
    }

    /** Retourne l'identifiant si le jeton est valide (signature, émetteur, expiration), sinon vide. */
    public Optional<UUID> lireIdentifiant(String jeton) {
        try {
            String sujet = Jwts.parser()
                .verifyWith(key)
                .requireIssuer(ISSUER)
                .build()
                .parseSignedClaims(jeton)
                .getPayload()
                .getSubject();
            return Optional.of(UUID.fromString(sujet));
        } catch (JwtException | IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public long dureeSecondes() {
        return duree.toSeconds();
    }
}
