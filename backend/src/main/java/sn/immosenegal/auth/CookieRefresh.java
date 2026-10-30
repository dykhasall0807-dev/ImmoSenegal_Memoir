package sn.immosenegal.auth;

import java.time.Duration;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseCookie;
import org.springframework.stereotype.Component;

/**
 * Cookie du jeton de rafraîchissement : HttpOnly (illisible par JavaScript), limité à /api/auth.
 * Secure et SameSite se règlent par l'environnement (voir .env.example) selon le déploiement.
 */
@Component
public class CookieRefresh {

    public static final String NOM = "immosenegal_refresh";
    private static final String CHEMIN = "/api/auth";

    private final boolean secure;
    private final String sameSite;
    private final Duration duree;

    public CookieRefresh(@Value("${app.security.refresh-cookie-secure:true}") boolean secure,
                         @Value("${app.security.refresh-cookie-samesite:Strict}") String sameSite,
                         @Value("${app.security.refresh-ttl-days:7}") long jours) {
        this.secure = secure;
        this.sameSite = sameSite;
        this.duree = Duration.ofDays(jours);
    }

    public String creer(String valeur) {
        return base(valeur).maxAge(duree).build().toString();
    }

    public String effacer() {
        return base("").maxAge(Duration.ZERO).build().toString();
    }

    private ResponseCookie.ResponseCookieBuilder base(String valeur) {
        return ResponseCookie.from(NOM, valeur).httpOnly(true).secure(secure).sameSite(sameSite).path(CHEMIN);
    }
}
