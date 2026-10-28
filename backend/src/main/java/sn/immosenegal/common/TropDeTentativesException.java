package sn.immosenegal.common;

import org.springframework.http.HttpStatus;

/** Trop d'échecs de connexion : l'API répond 429 avec l'en-tête Retry-After. */
public class TropDeTentativesException extends ApiException {

    private final long secondes;

    public TropDeTentativesException(long secondes) {
        super(HttpStatus.TOO_MANY_REQUESTS,
            "Trop de tentatives de connexion. Réessayez dans " + Math.max(1, (secondes + 59) / 60) + " minute(s).");
        this.secondes = secondes;
    }

    public long getSecondes() {
        return secondes;
    }
}
