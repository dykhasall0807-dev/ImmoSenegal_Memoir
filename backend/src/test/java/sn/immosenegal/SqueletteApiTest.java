package sn.immosenegal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/**
 * Tests du squelette. Ils démarrent l'application complète : une base PostgreSQL doit être
 * joignable avec les variables DB_URL, DB_USER et DB_PASSWORD (voir README).
 */
class SqueletteApiTest extends AbstractApiTest {

    @Test
    void healthEstPublicEtConfirmeQueFlywayAAppliqueLaMigration() throws Exception {
        mvc.perform(get("/api/health"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("UP"))
            .andExpect(jsonPath("$.baseDeDonnees").value("OK"))
            .andExpect(jsonPath("$.schemaVersion").value("1"));
    }

    @Test
    void uneRouteProtegeeSansJetonRenvoie401() throws Exception {
        mvc.perform(get("/api/une-route-protegee"))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void laDocumentationOpenApiEstDisponible() throws Exception {
        mvc.perform(get("/v3/api-docs"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.info.title").value("ImmoSénégal API"));
    }
}
