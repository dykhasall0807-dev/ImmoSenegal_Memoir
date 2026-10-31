package sn.immosenegal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.nio.charset.StandardCharsets;
import java.util.Date;
import java.util.Map;

import com.jayway.jsonpath.JsonPath;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;

import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;

/** Tests d'authentification et d'accès : 401 (non authentifié) et 403 (interdit). */
class AuthSecurityTest extends AbstractApiTest {

    // ---------- Inscription ----------

    @Test
    void inscriptionCreeUnClient_memeSiLeRoleAdminEstForceDansLaRequete() throws Exception {
        String email = emailUnique();
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email, "password", MOT_DE_PASSE,
                    "nomComplet", "Pirate", "role", "ADMIN"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.role").value("CLIENT"));

        assertThat(users.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.CLIENT);
    }

    @Test
    void inscriptionNeRenvoieJamaisLeMotDePasseNiSonHash() throws Exception {
        MvcResult r = mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", emailUnique(), "password", MOT_DE_PASSE, "nomComplet", "Test"))))
            .andExpect(status().isCreated()).andReturn();
        String reponse = r.getResponse().getContentAsString().toLowerCase();
        assertThat(reponse).doesNotContain("password").doesNotContain("hash").doesNotContain(MOT_DE_PASSE.toLowerCase());
    }

    @Test
    void motDePasseDeSeptCaracteresEstRefuse() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", emailUnique(), "password", "Abcd123", "nomComplet", "Test"))))
            .andExpect(status().isBadRequest())
            .andExpect(jsonPath("$.details.password").exists());
    }

    @Test
    void motDePasseDeHuitCaracteresEstAccepte() throws Exception {
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", emailUnique(), "password", "Abcd1234", "nomComplet", "Test"))))
            .andExpect(status().isCreated());
    }

    @Test
    void emailDejaUtiliseRenvoie409() throws Exception {
        String email = emailUnique();
        inscrire(email);
        mvc.perform(post("/api/auth/register").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email.toUpperCase(), "password", MOT_DE_PASSE, "nomComplet", "Doublon"))))
            .andExpect(status().isConflict());
    }

    // ---------- Connexion ----------

    @Test
    void connexionReussieRenvoieUnJetonEtLeProfil() throws Exception {
        String email = emailUnique();
        inscrire(email);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email, "password", MOT_DE_PASSE))))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.accessToken").isNotEmpty())
            .andExpect(jsonPath("$.tokenType").value("Bearer"))
            .andExpect(jsonPath("$.user.role").value("CLIENT"));
    }

    @Test
    void mauvaisMotDePasseEtEmailInconnuDonnentLaMemeReponse401() throws Exception {
        String email = emailUnique();
        inscrire(email);
        MvcResult mauvaisMdp = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", email, "password", "Mauvais-Mot-De-Passe-1"))))
            .andExpect(status().isUnauthorized()).andReturn();
        MvcResult inconnu = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content(corps(Map.of("email", emailUnique(), "password", MOT_DE_PASSE))))
            .andExpect(status().isUnauthorized()).andReturn();

        String m1 = JsonPath.read(mauvaisMdp.getResponse().getContentAsString(), "$.message");
        String m2 = JsonPath.read(inconnu.getResponse().getContentAsString(), "$.message");
        assertThat(m1).isEqualTo(m2);
    }

    // ---------- 401 : non authentifié ----------

    @Test
    void profilSansJetonRenvoie401() throws Exception {
        mvc.perform(get("/api/auth/me")).andExpect(status().isUnauthorized());
    }

    @Test
    void jetonInventeRenvoie401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer("pas.un.vrai.jeton")))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void jetonSigneAvecUneAutreCleRenvoie401() throws Exception {
        String email = emailUnique();
        inscrire(email);
        User utilisateur = users.findByEmail(email).orElseThrow();
        String jetonFalsifie = Jwts.builder()
            .issuer("immosenegal")
            .subject(utilisateur.getId().toString())
            .expiration(new Date(System.currentTimeMillis() + 60_000))
            .signWith(Keys.hmacShaKeyFor("une-autre-cle-completement-differente-0123456789".getBytes(StandardCharsets.UTF_8)))
            .compact();

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(jetonFalsifie)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void jetonExpireRenvoie401() throws Exception {
        String email = emailUnique();
        inscrire(email);
        User utilisateur = users.findByEmail(email).orElseThrow();
        String jetonExpire = Jwts.builder()
            .issuer("immosenegal")
            .subject(utilisateur.getId().toString())
            .expiration(new Date(System.currentTimeMillis() - 60_000))
            .signWith(Keys.hmacShaKeyFor("secret-de-test-uniquement-0123456789abcdef0123456789".getBytes(StandardCharsets.UTF_8)))
            .compact();

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(jetonExpire)))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void compteDesactiveNePeutPlusUtiliserSonJeton() throws Exception {
        String email = emailUnique();
        String jeton = nouveauClientConnecte(email);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(jeton))).andExpect(status().isOk());

        User utilisateur = users.findByEmail(email).orElseThrow();
        utilisateur.setActif(false);
        users.save(utilisateur);

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
            .andExpect(status().isUnauthorized());
    }

    // ---------- 403 : accès interdit ----------

    @Test
    void clientNePeutPasAccederAuxRoutesAdmin() throws Exception {
        String jeton = nouveauClientConnecte(emailUnique());
        mvc.perform(get("/api/admin/role-requests").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
            .andExpect(status().isForbidden());
    }

    @Test
    void routesAdminSansJetonRenvoient401() throws Exception {
        mvc.perform(get("/api/admin/role-requests")).andExpect(status().isUnauthorized());
    }

    @Test
    void clientNePeutPasSeDonnerUnRoleParUneRequeteFalsifiee() throws Exception {
        String email = emailUnique();
        String jeton = nouveauClientConnecte(email);

        // 1) Demander ADMIN : refusé (400), et le rôle reste CLIENT.
        mvc.perform(post("/api/auth/role-requests").header(HttpHeaders.AUTHORIZATION, bearer(jeton))
                .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("role", "ADMIN"))))
            .andExpect(status().isBadRequest());

        // 2) Valeur de rôle inventée : refusée (400).
        mvc.perform(post("/api/auth/role-requests").header(HttpHeaders.AUTHORIZATION, bearer(jeton))
                .contentType(MediaType.APPLICATION_JSON).content("{\"role\":\"SUPERMAN\"}"))
            .andExpect(status().isBadRequest());

        // 3) Faire passer un rôle dans la requête de connexion ou d'inscription n'a aucun effet.
        assertThat(users.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.CLIENT);
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
            .andExpect(jsonPath("$.role").value("CLIENT"));
    }

    // ---------- Demandes de rôle validées par un ADMIN ----------

    @Test
    void demandeProprietaireApprouveeParAdmin_puisProprietaireResteInterditSurLesRoutesAdmin() throws Exception {
        String email = emailUnique();
        String jetonClient = nouveauClientConnecte(email);
        String jetonAdmin = nouvelAdminConnecte();

        MvcResult demande = mvc.perform(post("/api/auth/role-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(jetonClient))
                .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("role", "PROPRIETAIRE"))))
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statut").value("EN_ATTENTE"))
            .andReturn();
        String idDemande = JsonPath.read(demande.getResponse().getContentAsString(), "$.id");

        // Tant que l'admin n'a pas validé, le compte reste CLIENT.
        assertThat(users.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.CLIENT);

        // Une 2e demande en attente est refusée.
        mvc.perform(post("/api/auth/role-requests").header(HttpHeaders.AUTHORIZATION, bearer(jetonClient))
                .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("role", "AGENT"))))
            .andExpect(status().isConflict());

        // Le client ne peut pas approuver sa propre demande.
        mvc.perform(post("/api/admin/role-requests/" + idDemande + "/approve")
                .header(HttpHeaders.AUTHORIZATION, bearer(jetonClient)))
            .andExpect(status().isForbidden());
        assertThat(users.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.CLIENT);

        // L'admin approuve.
        mvc.perform(post("/api/admin/role-requests/" + idDemande + "/approve")
                .header(HttpHeaders.AUTHORIZATION, bearer(jetonAdmin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("APPROUVEE"));

        // Le rôle est relu en base : le même jeton reflète immédiatement le nouveau rôle.
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(jetonClient)))
            .andExpect(jsonPath("$.role").value("PROPRIETAIRE"));

        // Un PROPRIETAIRE n'a toujours pas accès aux routes ADMIN.
        mvc.perform(get("/api/admin/role-requests").header(HttpHeaders.AUTHORIZATION, bearer(jetonClient)))
            .andExpect(status().isForbidden());

        // Une demande déjà traitée ne peut pas être retraitée.
        mvc.perform(post("/api/admin/role-requests/" + idDemande + "/reject")
                .header(HttpHeaders.AUTHORIZATION, bearer(jetonAdmin)))
            .andExpect(status().isConflict());
    }

    @Test
    void demandeRefuseeLaisseLeCompteEnClient() throws Exception {
        String email = emailUnique();
        String jetonClient = nouveauClientConnecte(email);
        String jetonAdmin = nouvelAdminConnecte();

        MvcResult demande = mvc.perform(post("/api/auth/role-requests")
                .header(HttpHeaders.AUTHORIZATION, bearer(jetonClient))
                .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("role", "AGENT"))))
            .andExpect(status().isCreated()).andReturn();
        String idDemande = JsonPath.read(demande.getResponse().getContentAsString(), "$.id");

        mvc.perform(post("/api/admin/role-requests/" + idDemande + "/reject")
                .header(HttpHeaders.AUTHORIZATION, bearer(jetonAdmin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.statut").value("REFUSEE"));

        assertThat(users.findByEmail(email).orElseThrow().getRole()).isEqualTo(Role.CLIENT);
    }

    @Test
    void adminPeutListerLesDemandesEnAttente() throws Exception {
        String jetonClient = nouveauClientConnecte(emailUnique());
        String jetonAdmin = nouvelAdminConnecte();
        mvc.perform(post("/api/auth/role-requests").header(HttpHeaders.AUTHORIZATION, bearer(jetonClient))
                .contentType(MediaType.APPLICATION_JSON).content(corps(Map.of("role", "AGENT"))))
            .andExpect(status().isCreated());

        mvc.perform(get("/api/admin/role-requests?statut=EN_ATTENTE").header(HttpHeaders.AUTHORIZATION, bearer(jetonAdmin)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$[0].statut").value("EN_ATTENTE"));
    }
}
