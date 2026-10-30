package sn.immosenegal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;

import javax.imageio.ImageIO;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.ResultActions;

import com.jayway.jsonpath.JsonPath;

import sn.immosenegal.user.Role;

/** Photos d'annonces : droits, formats, nettoyage des métadonnées, limites, lecture publique. */
class PhotoTest extends AbstractApiTest {

    // ---------- Aides ----------

    private static byte[] image(String format, int largeur, int hauteur) throws Exception {
        ByteArrayOutputStream sortie = new ByteArrayOutputStream();
        ImageIO.write(new BufferedImage(largeur, hauteur, BufferedImage.TYPE_INT_RGB), format, sortie);
        return sortie.toByteArray();
    }

    private String annonceDe(String jeton) throws Exception {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("titre", "Annonce photo");
        m.put("typeBien", "VILLA");
        m.put("transaction", "VENTE");
        m.put("prix", 1000000);
        m.put("superficie", 100);
        m.put("pieces", 4);
        m.put("quartier", "Quartier" + UUID.randomUUID().toString().substring(0, 6));
        m.put("ville", "Ziguinchor");
        MvcResult r = mvc.perform(post("/api/annonces").header(HttpHeaders.AUTHORIZATION, bearer(jeton))
                .contentType(MediaType.APPLICATION_JSON).content(corps(m)))
            .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    private ResultActions envoyer(String annonceId, String jeton, String nom, String type, byte[] octets) throws Exception {
        var requete = multipart("/api/annonces/{id}/photos", annonceId)
            .file(new MockMultipartFile("fichier", nom, type, octets));
        if (jeton != null) {
            requete.header(HttpHeaders.AUTHORIZATION, bearer(jeton));
        }
        return mvc.perform(requete);
    }

    private String ajouterPhoto(String annonceId, String jeton) throws Exception {
        MvcResult r = envoyer(annonceId, jeton, "p.jpg", "image/jpeg", image("jpg", 40, 30))
            .andExpect(status().isCreated()).andReturn();
        return JsonPath.read(r.getResponse().getContentAsString(), "$.id");
    }

    // ---------- Droits ----------

    @Test
    void sansConnexionOnNePeutPasAjouterDePhoto() throws Exception {
        String annonce = annonceDe(utilisateurConnecte(Role.PROPRIETAIRE));
        envoyer(annonce, null, "p.jpg", "image/jpeg", image("jpg", 10, 10)).andExpect(status().isUnauthorized());
    }

    @Test
    void unAutreProprietaireNePeutNiAjouterNiSupprimer() throws Exception {
        String proprietaire = utilisateurConnecte(Role.PROPRIETAIRE);
        String intrus = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(proprietaire);
        String photo = ajouterPhoto(annonce, proprietaire);

        envoyer(annonce, intrus, "p.jpg", "image/jpeg", image("jpg", 10, 10)).andExpect(status().isForbidden());
        mvc.perform(delete("/api/annonces/{a}/photos/{p}", annonce, photo).header(HttpHeaders.AUTHORIZATION, bearer(intrus)))
            .andExpect(status().isForbidden());
        // La photo est toujours là.
        mvc.perform(get("/api/annonces/{a}/photos/{p}", annonce, photo)).andExpect(status().isOk());
    }

    @Test
    void unClientNePeutPasAjouterDePhotoSurUneAnnonceQuiNeLuiAppartientPas() throws Exception {
        String annonce = annonceDe(utilisateurConnecte(Role.PROPRIETAIRE));
        String client = nouveauClientConnecte(emailUnique());
        envoyer(annonce, client, "p.jpg", "image/jpeg", image("jpg", 10, 10)).andExpect(status().isForbidden());
    }

    @Test
    void unAdminPeutModererLesPhotos() throws Exception {
        String proprietaire = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(proprietaire);
        String photo = ajouterPhoto(annonce, proprietaire);
        String admin = nouvelAdminConnecte();
        mvc.perform(delete("/api/annonces/{a}/photos/{p}", annonce, photo).header(HttpHeaders.AUTHORIZATION, bearer(admin)))
            .andExpect(status().isNoContent());
    }

    // ---------- Formats et contenu ----------

    @Test
    void lePropreitaireAjouteUnePhotoQuiApparaitDansLAnnonceEtSeLitSansConnexion() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        String photo = ajouterPhoto(annonce, jeton);

        mvc.perform(get("/api/annonces/{id}", annonce))
            .andExpect(status().isOk())
            .andExpect(jsonPath("$.photos[0]").value(photo));

        MvcResult r = mvc.perform(get("/api/annonces/{a}/photos/{p}", annonce, photo)).andExpect(status().isOk()).andReturn();
        assertThat(r.getResponse().getContentType()).isEqualTo("image/jpeg");
        assertThat(r.getResponse().getHeader("X-Content-Type-Options")).isEqualTo("nosniff");
        assertThat(ImageIO.read(new ByteArrayInputStream(r.getResponse().getContentAsByteArray()))).isNotNull();
    }

    @Test
    void laListeDesAnnoncesContientLesIdentifiantsDePhotosMaisPasLesImages() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        String photo = ajouterPhoto(annonce, jeton);
        MvcResult r = mvc.perform(get("/api/annonces/mes-annonces").header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
            .andExpect(status().isOk()).andReturn();
        String corps = r.getResponse().getContentAsString();
        assertThat(corps).contains(photo);
        assertThat(corps.length()).isLessThan(5_000);
    }

    @Test
    void lesMetadonneesDeLaPhotoSontSupprimees() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        // JPEG avec un segment commentaire (même emplacement que les EXIF) contenant un marqueur.
        byte[] jpg = image("jpg", 60, 40);
        byte[] marqueur = "SECRET-GPS-14.34N".getBytes(StandardCharsets.ISO_8859_1);
        ByteArrayOutputStream avecMeta = new ByteArrayOutputStream();
        avecMeta.write(jpg, 0, 2);
        avecMeta.write(0xFF);
        avecMeta.write(0xFE);
        avecMeta.write((marqueur.length + 2) >> 8);
        avecMeta.write((marqueur.length + 2) & 0xFF);
        avecMeta.write(marqueur);
        avecMeta.write(jpg, 2, jpg.length - 2);
        assertThat(new String(avecMeta.toByteArray(), StandardCharsets.ISO_8859_1)).contains("SECRET-GPS");

        MvcResult ajout = envoyer(annonce, jeton, "gps.jpg", "image/jpeg", avecMeta.toByteArray())
            .andExpect(status().isCreated()).andReturn();
        String photo = JsonPath.read(ajout.getResponse().getContentAsString(), "$.id");
        byte[] servi = mvc.perform(get("/api/annonces/{a}/photos/{p}", annonce, photo)).andReturn()
            .getResponse().getContentAsByteArray();
        assertThat(new String(servi, StandardCharsets.ISO_8859_1)).doesNotContain("SECRET-GPS");
    }

    @Test
    void uneGrandeImageEstReduite() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        MvcResult ajout = envoyer(annonce, jeton, "grande.png", "image/png", image("png", 3200, 2000))
            .andExpect(status().isCreated()).andReturn();
        String photo = JsonPath.read(ajout.getResponse().getContentAsString(), "$.id");
        byte[] servi = mvc.perform(get("/api/annonces/{a}/photos/{p}", annonce, photo)).andReturn()
            .getResponse().getContentAsByteArray();
        BufferedImage lue = ImageIO.read(new ByteArrayInputStream(servi));
        assertThat(lue.getWidth()).isEqualTo(1600);
        assertThat(lue.getHeight()).isEqualTo(1000);
    }

    @Test
    void leTypeEstReconnuParLeContenuEtNonParLeNomOuLEnTete() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        // Du HTML déguisé en image.
        byte[] html = "<html><script>alert(1)</script></html>".getBytes(StandardCharsets.UTF_8);
        envoyer(annonce, jeton, "photo.jpg", "image/jpeg", html).andExpect(status().isBadRequest());
        // Un SVG (peut contenir du script) est refusé.
        byte[] svg = "<svg xmlns=\"http://www.w3.org/2000/svg\"><script>alert(1)</script></svg>".getBytes(StandardCharsets.UTF_8);
        envoyer(annonce, jeton, "dessin.png", "image/png", svg).andExpect(status().isBadRequest());
        // Un en-tête JPEG suivi de n'importe quoi n'est pas une image lisible.
        byte[] faux = new byte[] {(byte) 0xFF, (byte) 0xD8, (byte) 0xFF, 1, 2, 3, 4, 5, 6, 7, 8};
        envoyer(annonce, jeton, "faux.jpg", "image/jpeg", faux).andExpect(status().isBadRequest());
        mvc.perform(get("/api/annonces/{id}", annonce)).andExpect(jsonPath("$.photos").isEmpty());
    }

    @Test
    void unFichierVideEstRefuse() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        envoyer(annonce, jeton, "vide.jpg", "image/jpeg", new byte[0]).andExpect(status().isBadRequest());
    }

    // ---------- Limites ----------

    @Test
    void unFichierDePlusDe5MoEstRefuse() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        byte[] enorme = new byte[5 * 1024 * 1024 + 1];
        enorme[0] = (byte) 0xFF;
        enorme[1] = (byte) 0xD8;
        enorme[2] = (byte) 0xFF;
        envoyer(annonce, jeton, "gros.jpg", "image/jpeg", enorme).andExpect(status().isPayloadTooLarge());
    }

    @Test
    void uneAnnonceNePeutPasAvoirPlusDeHuitPhotos() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        for (int i = 0; i < 8; i++) {
            ajouterPhoto(annonce, jeton);
        }
        envoyer(annonce, jeton, "p.jpg", "image/jpeg", image("jpg", 10, 10)).andExpect(status().isBadRequest());
    }

    // ---------- Suppression et cohérence ----------

    @Test
    void supprimerUnePhotoLaRetireDeLAnnonceEtSupprimerLAnnonceSupprimeSesPhotos() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonce = annonceDe(jeton);
        String photo1 = ajouterPhoto(annonce, jeton);
        String photo2 = ajouterPhoto(annonce, jeton);

        mvc.perform(delete("/api/annonces/{a}/photos/{p}", annonce, photo1).header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/annonces/{a}/photos/{p}", annonce, photo1)).andExpect(status().isNotFound());
        mvc.perform(get("/api/annonces/{id}", annonce)).andExpect(jsonPath("$.photos.length()").value(1));

        mvc.perform(delete("/api/annonces/{id}", annonce).header(HttpHeaders.AUTHORIZATION, bearer(jeton)))
            .andExpect(status().isNoContent());
        mvc.perform(get("/api/annonces/{a}/photos/{p}", annonce, photo2)).andExpect(status().isNotFound());
    }

    @Test
    void unePhotoNeSeLitPasViaUneAutreAnnonce() throws Exception {
        String jeton = utilisateurConnecte(Role.PROPRIETAIRE);
        String annonceA = annonceDe(jeton);
        String annonceB = annonceDe(jeton);
        String photo = ajouterPhoto(annonceA, jeton);
        mvc.perform(get("/api/annonces/{a}/photos/{p}", annonceB, photo)).andExpect(status().isNotFound());
    }
}
