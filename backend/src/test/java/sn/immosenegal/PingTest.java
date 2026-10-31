package sn.immosenegal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;

/** Le réveil léger est public, répond UP et n'expose rien. */
class PingTest extends AbstractApiTest {

    @Test
    void pingRepondUpSansConnexion() throws Exception {
        mvc.perform(get("/api/ping"))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("UP"))
            .andExpect(jsonPath("$.baseDeDonnees").doesNotExist());
    }
}
