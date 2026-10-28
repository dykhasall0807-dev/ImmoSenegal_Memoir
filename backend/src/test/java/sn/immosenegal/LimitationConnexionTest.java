package sn.immosenegal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

/** Tests d'intégration de la limitation des tentatives de connexion (5 échecs par email par défaut). */
class LimitationConnexionTest extends AbstractApiTest {

    private ResultActions tenter(String email, String motDePasse) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
            .content(corps(Map.of("email", email, "password", motDePasse))));
    }

    private void echouer(String email, int fois) throws Exception {
        for (int i = 0; i < fois; i++) {
            tenter(email, "Mauvais-Mot-De-Passe-" + i).andExpect(status().isUnauthorized());
        }
    }

    @Test
    void apresCinqEchecsLeBonMotDePasseEstRefuseAvec429EtRetryAfter() throws Exception {
        String email = emailUnique();
        inscrire(email);
        echouer(email, 5);

        MvcResult bloque = tenter(email, MOT_DE_PASSE).andExpect(status().is(429)).andReturn();
        String retryAfter = bloque.getResponse().getHeader("Retry-After");
        assertThat(retryAfter).isNotNull();
        assertThat(Long.parseLong(retryAfter)).isBetween(1L, 15L * 60);
    }

    @Test
    void unSuccesRemetLeCompteurAZero() throws Exception {
        String email = emailUnique();
        inscrire(email);

        echouer(email, 4);
        tenter(email, MOT_DE_PASSE).andExpect(status().isOk());   // succès : compteur remis à zéro
        echouer(email, 4);
        tenter(email, MOT_DE_PASSE).andExpect(status().isOk());   // 8 échecs au total, mais jamais 5 de suite
    }

    @Test
    void leBlocageD_unCompteNAffectePasLesAutres() throws Exception {
        String victime = emailUnique();
        String autre = emailUnique();
        inscrire(victime);
        inscrire(autre);

        echouer(victime, 5);
        tenter(victime, MOT_DE_PASSE).andExpect(status().is(429));
        tenter(autre, MOT_DE_PASSE).andExpect(status().isOk());
    }

    @Test
    void lesEmailsInconnusSontAussiLimitesSansRevelerLeurExistence() throws Exception {
        String inconnu = emailUnique();
        echouer(inconnu, 5);
        tenter(inconnu, MOT_DE_PASSE).andExpect(status().is(429));
    }
}
