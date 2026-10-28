package sn.immosenegal;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.Map;
import java.util.UUID;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jayway.jsonpath.JsonPath;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

/**
 * Base commune des tests d'API : application complète + PostgreSQL réel (variables DB_*).
 * Le secret JWT ci-dessous sert uniquement aux tests.
 */
@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = "app.jwt.secret=secret-de-test-uniquement-0123456789abcdef0123456789")
abstract class AbstractApiTest {

    protected static final String MOT_DE_PASSE = "MotDePasse-Solide-123";

    @Autowired protected MockMvc mvc;
    @Autowired protected ObjectMapper json;
    @Autowired protected UserRepository users;
    @Autowired protected PasswordEncoder encoder;

    protected String emailUnique() {
        return "test-" + UUID.randomUUID() + "@example.test";
    }

    protected String corps(Object objet) throws Exception {
        return json.writeValueAsString(objet);
    }

    /** Inscription publique d'un compte (donc CLIENT). */
    protected void inscrire(String email) throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email, "password", MOT_DE_PASSE, "nomComplet", "Utilisateur Test"))))
            .andExpect(status().isCreated());
    }

    protected String connecter(String email) throws Exception {
        MvcResult resultat = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email, "password", MOT_DE_PASSE))))
            .andExpect(status().isOk())
            .andReturn();
        return JsonPath.read(resultat.getResponse().getContentAsString(), "$.accessToken");
    }

    /** Crée un client via l'API publique et retourne son jeton. */
    protected String nouveauClientConnecte(String email) throws Exception {
        inscrire(email);
        return connecter(email);
    }

    /** Crée un ADMIN directement en base (les tests simulent la procédure serveur) et retourne son jeton. */
    protected String nouvelAdminConnecte() throws Exception {
        String email = emailUnique();
        users.save(new User(email, encoder.encode(MOT_DE_PASSE), "Admin Test", null, Role.ADMIN));
        return connecter(email);
    }

    protected static String bearer(String jeton) {
        return "Bearer " + jeton;
    }
}
