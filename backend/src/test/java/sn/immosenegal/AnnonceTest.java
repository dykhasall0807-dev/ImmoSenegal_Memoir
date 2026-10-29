package sn.immosenegal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsInAnyOrder;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import com.jayway.jsonpath.JsonPath;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import sn.immosenegal.user.Role;

/** Tests des annonces : permissions (401/403), propriété, validation, filtres, pagination et tri. */
class AnnonceTest extends AbstractApiTest {

    // ---------- Aides ----------

    private Map<String, Object> annonce(String titre, String quartier) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("titre", titre);
        m.put("description", "Description de test");
        m.put("typeBien", "APPARTEMENT");
        m.put("transaction", "LOCATION");
        m.put("prix", 100000);
        m.put("superficie", 60);
        m.put("pieces", 3);
        m.put("quartier", quartier);
        m.put("ville", "Ziguinchor");
        return m;
    }

    private ResultActions publier(String jeton, Map<String, Object> corps) throws Exception {
        return mvc.perform(post("/api/annonces").header(HttpHeaders.AUTHORIZATION, bearer(jeton))
            .contentType(MediaType.APPLICATION_JSON).content(corps(corps)));
    }

    private String publierEtRecupererId(String jeton, Map<String, Object> corps) throws Exception {
        MvcResult r = publier(jeton, corps).andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private String quartierUnique() {
        return "Quartier" + UUID.randomUUID().toString().substring(0, 8);
    }

    // ---------- Publication : qui a le droit ----------

    @Test
    void publierSansJetonRenvoie401() throws Exception {
        mvc.perform(post("/api/annonces").contentType(MediaType.APPLICATION_JSON)
                .content(corps(annonce("Sans jeton", "Kandé"))))
            .andExpect(status().isUnauthorized());
    }

    @Test
    void unClientNePeutPasPublier() throws Exception {
        String jetonClient = utilisateurConnecte(Role.CLIENT);
        publier(jetonClient, annonce("Interdit", "Kandé")).andExpect(status().isForbidden());
    }

    @Test
    void unProprietairePublie_etLeProprietaireNEstJamaisCeluiDeLaRequete() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        Map<String, Object> corps = annonce("Ma villa", quartierUnique());
        corps.put("statut", "VENDU");                                   // ignoré à la création
        corps.put("proprietaireId", UUID.randomUUID().toString());       // champ inconnu : ignoré
        corps.put("proprietaire", Map.of("id", UUID.randomUUID().toString()));

        MvcResult r = publier(jeton, corps)
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.statut").value("DISPONIBLE"))
            .andReturn();

        String idProprietaire = JsonPath.read(r.getResponse().getContentAsString(), "$.proprietaire.id");
        String moi = JsonPath.read(mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
            .andReturn().getResponse().getContentAsString(), "$.id");
        assertThat(idProprietaire).isEqualTo(moi);
    }

    // ---------- Validation ----------

    @Test
    void lesDonneesInvalidesSontRefusees() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);

        Map<String, Object> prixNegatif = annonce("Prix négatif", "Kandé");
        prixNegatif.put("prix", -5);
        publier(jeton, prixNegatif).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.prix").exists());

        Map<String, Object> sansTitre = annonce("", "Kandé");
        publier(jeton, sansTitre).andExpect(status().isBadRequest()).andExpect(jsonPath("$.details.titre").exists());

        Map<String, Object> latitudeSeule = annonce("Latitude seule", "Kandé");
        latitudeSeule.put("latitude", 12.55);
        publier(jeton, latitudeSeule).andExpect(status().isBadRequest());

        Map<String, Object> latitudeHorsLimites = annonce("Latitude absurde", "Kandé");
        latitudeHorsLimites.put("latitude", 120);
        latitudeHorsLimites.put("longitude", -16.27);
        publier(jeton, latitudeHorsLimites).andExpect(status().isBadRequest());

        Map<String, Object> typeInconnu = annonce("Type inconnu", "Kandé");
        typeInconnu.put("typeBien", "CHATEAU");
        publier(jeton, typeInconnu).andExpect(status().isBadRequest());
    }

    @Test
    void unTerrainNAPasDePieces() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        Map<String, Object> terrain = annonce("Terrain", quartierUnique());
        terrain.put("typeBien", "TERRAIN");
        terrain.put("pieces", 4);
        publier(jeton, terrain).andExpect(status().isCreated()).andExpect(jsonPath("$.pieces").doesNotExist());
    }

    // ---------- Lecture publique ----------

    @Test
    void lAnnoncePeutEtreLueSansJeton_sansExposerLEmailNiLeTelephoneDuProprietaire() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String id = publierEtRecupererId(jeton, annonce("Visible", quartierUnique()));

        mvc.perform(get("/api/annonces/" + id))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Visible"))
            .andExpect(jsonPath("$.proprietaire.nomComplet").exists())
            .andExpect(content().string(not(containsString("@example.test"))))
            .andExpect(content().string(not(containsString("telephone"))));
    }

    @Test
    void uneAnnonceInconnueRenvoie404_etUnIdentifiantMalFormeRenvoie400() throws Exception {
        mvc.perform(get("/api/annonces/" + UUID.randomUUID())).andExpect(status().isNotFound());
        mvc.perform(get("/api/annonces/pas-un-uuid")).andExpect(status().isBadRequest());
    }

    // ---------- Propriété : modifier / supprimer ----------

    @Test
    void unAutreProprietaireNePeutNiModifierNiSupprimerMaisLeProprietaireLePeut() throws Exception {
        String jetonProprio = utilisateurConnecte(Role.PROPRIETAIRE);
        String jetonAutre = utilisateurConnecte(Role.PROPRIETAIRE);
        String id = publierEtRecupererId(jetonProprio, annonce("Titre d'origine", quartierUnique()));

        // L'autre propriétaire : 403 sur la modification ET la suppression, l'annonce reste intacte.
        mvc.perform(put("/api/annonces/" + id).header(HttpHeaders.AUTHORIZATION, bearer(jetonAutre))
                .contentType(MediaType.APPLICATION_JSON).content(corps(annonce("Piratée", "Kandé"))))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/annonces/" + id).header(HttpHeaders.AUTHORIZATION, bearer(jetonAutre)))
            .andExpect(status().isForbidden());
        mvc.perform(get("/api/annonces/" + id)).andExpect(jsonPath("$.titre").value("Titre d'origine"));

        // Un simple client : 403 aussi. Sans jeton : 401.
        String jetonClient = utilisateurConnecte(Role.CLIENT);
        mvc.perform(put("/api/annonces/" + id).header(HttpHeaders.AUTHORIZATION, bearer(jetonClient))
                .contentType(MediaType.APPLICATION_JSON).content(corps(annonce("Piratée", "Kandé"))))
            .andExpect(status().isForbidden());
        mvc.perform(delete("/api/annonces/" + id)).andExpect(status().isUnauthorized());

        // Le propriétaire peut modifier (y compris le statut) puis supprimer.
        Map<String, Object> modif = annonce("Titre modifié", "Kandé");
        modif.put("statut", "RESERVE");
        mvc.perform(put("/api/annonces/" + id).header(HttpHeaders.AUTHORIZATION, bearer(jetonProprio))
                .contentType(MediaType.APPLICATION_JSON).content(corps(modif)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.titre").value("Titre modifié"))
            .andExpect(jsonPath("$.statut").value("RESERVE"));
        mvc.perform(delete("/api/annonces/" + id).header(HttpHeaders.AUTHORIZATION, bearer(jetonProprio)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/annonces/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void unAdminPeutModererEnSupprimantLAnnonceDUnAutre() throws Exception {
        String jetonProprio = utilisateurConnecte(Role.PROPRIETAIRE);
        String jetonAdmin = utilisateurConnecte(Role.ADMIN);
        String id = publierEtRecupererId(jetonProprio, annonce("À modérer", quartierUnique()));

        mvc.perform(delete("/api/annonces/" + id).header(HttpHeaders.AUTHORIZATION, bearer(jetonAdmin)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/annonces/" + id)).andExpect(status().isNotFound());
    }

    @Test
    void mesAnnoncesNeRenvoieQueLesMiennes_etExigeUneSession() throws Exception {
        String jetonA = utilisateurConnecte(Role.PROPRIETAIRE);
        String jetonB = utilisateurConnecte(Role.AGENT);
        publierEtRecupererId(jetonA, annonce("Annonce de A", quartierUnique()));
        publierEtRecupererId(jetonB, annonce("Annonce de B", quartierUnique()));

        mvc.perform(get("/api/annonces/mes-annonces").header(HttpHeaders.AUTHORIZATION, bearer(jetonA)))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.totalElements").value(1))
            .andExpect(jsonPath("$.contenu[0].titre").value("Annonce de A"));
        mvc.perform(get("/api/annonces/mes-annonces")).andExpect(status().isUnauthorized());
    }

    // ---------- Recherche : filtres combinables, pagination, tri ----------

    /** Crée 3 annonces dans un quartier unique (isole les tests des données des autres tests). */
    private String jeuDeRecherche() throws Exception {
        String quartier = quartierUnique();
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);

        Map<String, Object> a = annonce("A studio", quartier);
        a.put("typeBien", "STUDIO");
        a.put("transaction", "LOCATION");
        a.put("prix", 50000);
        a.put("superficie", 28);
        a.put("pieces", 1);
        a.put("equipements", List.of("WiFi", "climatisation"));
        publier(jeton, a).andExpect(status().isCreated());

        Map<String, Object> b = annonce("B villa", quartier);
        b.put("typeBien", "VILLA");
        b.put("transaction", "VENTE");
        b.put("prix", 45000000);
        b.put("superficie", 320);
        b.put("pieces", 6);
        b.put("equipements", List.of("piscine"));
        publier(jeton, b).andExpect(status().isCreated());

        Map<String, Object> c = annonce("C appartement", quartier);
        c.put("typeBien", "APPARTEMENT");
        c.put("transaction", "LOCATION");
        c.put("prix", 120000);
        c.put("superficie", 85);
        c.put("pieces", 3);
        c.put("equipements", List.of("wifi"));
        publier(jeton, c).andExpect(status().isCreated());
        return quartier;
    }

    private ResultActions chercher(String quartier, String parametresSupplementaires) throws Exception {
        return mvc.perform(get("/api/annonces?quartier=" + quartier + parametresSupplementaires));
    }

    @Test
    void lesFiltresSeCombinent() throws Exception {
        String q = jeuDeRecherche();

        chercher(q, "").andExpect(jsonPath("$.totalElements").value(3));
        chercher(q, "&transaction=LOCATION").andExpect(jsonPath("$.contenu[*].titre", containsInAnyOrder("A studio", "C appartement")));
        chercher(q, "&transaction=VENTE").andExpect(jsonPath("$.contenu[*].titre", contains("B villa")));
        chercher(q, "&type=VILLA").andExpect(jsonPath("$.contenu[*].titre", contains("B villa")));
        chercher(q, "&prixMax=100000").andExpect(jsonPath("$.contenu[*].titre", contains("A studio")));
        chercher(q, "&prixMin=100000&prixMax=200000").andExpect(jsonPath("$.contenu[*].titre", contains("C appartement")));
        chercher(q, "&superficieMin=100").andExpect(jsonPath("$.contenu[*].titre", contains("B villa")));
        chercher(q, "&superficieMax=30").andExpect(jsonPath("$.contenu[*].titre", contains("A studio")));
        chercher(q, "&piecesMin=3").andExpect(jsonPath("$.contenu[*].titre", containsInAnyOrder("B villa", "C appartement")));
        chercher(q, "&transaction=LOCATION&piecesMin=2").andExpect(jsonPath("$.contenu[*].titre", contains("C appartement")));
        chercher(q, "&statut=DISPONIBLE").andExpect(jsonPath("$.totalElements").value(3));
        chercher(q, "&statut=VENDU").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void lesEquipementsSontInsensiblesALaCasse_etTousDoiventEtrePresents() throws Exception {
        String q = jeuDeRecherche();

        chercher(q, "&equipements=wifi").andExpect(jsonPath("$.contenu[*].titre", containsInAnyOrder("A studio", "C appartement")));
        chercher(q, "&equipements=WIFI,Climatisation").andExpect(jsonPath("$.contenu[*].titre", contains("A studio")));
        chercher(q, "&equipements=wifi&equipements=piscine").andExpect(jsonPath("$.totalElements").value(0));
    }

    @Test
    void lesCaracteresJokersDeLaRechercheSontTraitesCommeDuTexte() throws Exception {
        String q = jeuDeRecherche();
        // « % » seul ne doit pas correspondre à tous les quartiers : on cherche le texte « % ».
        mvc.perform(get("/api/annonces?quartier=%25")).andExpect(jsonPath("$.contenu[*].titre", not(containsInAnyOrder("A studio"))));
        mvc.perform(get("/api/annonces?quartier=" + q.substring(0, 5) + "_"))
            .andExpect(jsonPath("$.contenu[*].titre", not(containsInAnyOrder("A studio", "B villa", "C appartement"))));
    }

    @Test
    void leTriEtLaPaginationFonctionnent() throws Exception {
        String q = jeuDeRecherche();

        chercher(q, "&tri=prix_asc").andExpect(jsonPath("$.contenu[*].titre", contains("A studio", "C appartement", "B villa")));
        chercher(q, "&tri=prix_desc").andExpect(jsonPath("$.contenu[*].titre", contains("B villa", "C appartement", "A studio")));
        chercher(q, "&tri=superficie_desc").andExpect(jsonPath("$.contenu[0].titre").value("B villa"));

        chercher(q, "&tri=prix_asc&taille=2&page=0")
            .andExpect(jsonPath("$.contenu.length()").value(2))
            .andExpect(jsonPath("$.totalElements").value(3))
            .andExpect(jsonPath("$.totalPages").value(2));
        chercher(q, "&tri=prix_asc&taille=2&page=1")
            .andExpect(jsonPath("$.contenu[*].titre", contains("B villa")));
    }

    @Test
    void laTailleDePageEstPlafonnee_etLesParametresInvalidesSontRefuses() throws Exception {
        mvc.perform(get("/api/annonces?taille=1000")).andExpect(jsonPath("$.taille").value(50));
        mvc.perform(get("/api/annonces?taille=0")).andExpect(jsonPath("$.taille").value(1));
        mvc.perform(get("/api/annonces?tri=prix; DROP TABLE annonces")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/annonces?transaction=TROC")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/annonces?prixMax=beaucoup")).andExpect(status().isBadRequest());
        mvc.perform(get("/api/annonces")).andExpect(status().isOk());   // la table existe toujours
    }
}
