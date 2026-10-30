package sn.immosenegal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MvcResult;

import com.jayway.jsonpath.JsonPath;

import jakarta.servlet.http.Cookie;

/** Session longue : cookie HttpOnly, rotation du jeton, détection de réutilisation, déconnexion côté serveur. */
class SessionRefreshTest extends AbstractApiTest {

    private static final String COOKIE = "immosenegal_refresh";
    private static final String ENTETE = "X-Requested-With";
    private static final String VALEUR = "immosenegal";

    @Autowired JdbcTemplate jdbc;

    private MvcResult connexion(String email) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email, "password", MOT_DE_PASSE))))
            .andExpect(status().isOk()).andReturn();
    }

    private String cookieDe(MvcResult resultat) {
        Cookie c = resultat.getResponse().getCookie(COOKIE);
        assertThat(c).as("cookie de rafraîchissement présent").isNotNull();
        return c.getValue();
    }

    private MvcResult renouveler(String valeurCookie) throws Exception {
        return mvc.perform(post("/api/auth/refresh").header(ENTETE, VALEUR).cookie(new Cookie(COOKIE, valeurCookie))).andReturn();
    }

    @Test
    void connexionPoseUnCookieHttpOnlyEtNeMetPasLeJetonDansLeCorps() throws Exception {
        String email = emailUnique();
        inscrire(email);
        MvcResult r = connexion(email);

        String entete = r.getResponse().getHeader(HttpHeaders.SET_COOKIE);
        assertThat(entete).contains(COOKIE + "=").contains("HttpOnly").contains("Secure")
            .contains("SameSite=Strict").contains("Path=/api/auth");
        assertThat(r.getResponse().getContentAsString()).doesNotContain(cookieDe(r));
    }

    @Test
    void lEmpreinteSeuleEstStockeeEnBase() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String brut = cookieDe(connexion(email));

        var id = users.findByEmail(email).orElseThrow().getId();
        var empreintes = jdbc.queryForList("SELECT token_hash FROM refresh_tokens WHERE user_id = ?", String.class, id);
        assertThat(empreintes).hasSize(1);
        assertThat(empreintes.get(0)).hasSize(64).isNotEqualTo(brut);
    }

    @Test
    void renouvellementDonneUnNouveauJetonDAccesUtilisableEtUnNouveauCookie() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String ancien = cookieDe(connexion(email));

        MvcResult r = renouveler(ancien);
        assertThat(r.getResponse().getStatus()).isEqualTo(200);
        String nouveau = cookieDe(r);
        assertThat(nouveau).isNotEqualTo(ancien);

        String acces = JsonPath.read(r.getResponse().getContentAsString(), "$.accessToken");
        mvc.perform(get("/api/auth/me").header("Authorization", bearer(acces)))
            .andExpect(status().isOk());
    }

    @Test
    void renouvellementSansEnteteRequisEstRefuse() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String brut = cookieDe(connexion(email));
        mvc.perform(post("/api/auth/refresh").cookie(new Cookie(COOKIE, brut)))
            .andExpect(status().isForbidden());
    }

    @Test
    void renouvellementSansCookieOuAvecCookieInconnuRenvoie401() throws Exception {
        mvc.perform(post("/api/auth/refresh").header(ENTETE, VALEUR)).andExpect(status().isUnauthorized());
        assertThat(renouveler("jeton-qui-n-existe-pas").getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void unJetonDejaUtiliseRevoqueToutesLesSessionsDuCompte() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String ancien = cookieDe(connexion(email));
        String courant = cookieDe(renouveler(ancien));

        // Un voleur (ou un rejeu) présente l'ancien jeton : refus, ET le jeton courant devient inutilisable.
        assertThat(renouveler(ancien).getResponse().getStatus()).isEqualTo(401);
        assertThat(renouveler(courant).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void jetonExpireEstRefuse() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String brut = cookieDe(connexion(email));
        var id = users.findByEmail(email).orElseThrow().getId();
        jdbc.update("UPDATE refresh_tokens SET expire_le = now() - interval '1 minute' WHERE user_id = ?", id);

        assertThat(renouveler(brut).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void compteDesactiveNePeutPlusRenouveler() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String brut = cookieDe(connexion(email));
        var utilisateur = users.findByEmail(email).orElseThrow();
        utilisateur.setActif(false);
        users.save(utilisateur);

        assertThat(renouveler(brut).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void deconnexionRevoqueLeJetonCoteServeurEtEffaceLeCookie() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String brut = cookieDe(connexion(email));

        MvcResult r = mvc.perform(post("/api/auth/logout").header(ENTETE, VALEUR).cookie(new Cookie(COOKIE, brut)))
            .andExpect(status().isNoContent()).andReturn();
        assertThat(r.getResponse().getHeader(HttpHeaders.SET_COOKIE)).contains("Max-Age=0");

        assertThat(renouveler(brut).getResponse().getStatus()).isEqualTo(401);
    }

    @Test
    void deconnexionSansEnteteRequisEstRefusee() throws Exception {
        mvc.perform(post("/api/auth/logout")).andExpect(status().isForbidden());
    }

    @Test
    void leRoleApprouveEntreTempsEstVisibleApresRenouvellement() throws Exception {
        String email = emailUnique();
        inscrire(email);
        String brut = cookieDe(connexion(email));
        var utilisateur = users.findByEmail(email).orElseThrow();
        utilisateur.setRole(sn.immosenegal.user.Role.PROPRIETAIRE);
        users.save(utilisateur);

        MvcResult r = renouveler(brut);
        assertThat((String) JsonPath.read(r.getResponse().getContentAsString(), "$.user.role")).isEqualTo("PROPRIETAIRE");
    }
}
