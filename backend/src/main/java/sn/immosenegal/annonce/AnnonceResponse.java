package sn.immosenegal.annonce;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

/**
 * Vue publique d'une annonce. Le propriétaire n'est identifié que par son nom : aucun email ni téléphone
 * (le contact passera par la messagerie interne).
 */
public record AnnonceResponse(
    UUID id, String titre, String description, TypeBien typeBien, TypeTransaction transaction,
    long prix, BigDecimal superficie, Integer pieces, String quartier, String ville, String adresse,
    BigDecimal latitude, BigDecimal longitude, List<String> equipements, StatutAnnonce statut,
    Proprietaire proprietaire, Instant creeLe, Instant modifieLe) {

    public record Proprietaire(UUID id, String nomComplet) {
    }

    public static AnnonceResponse de(Annonce a) {
        return new AnnonceResponse(a.getId(), a.getTitre(), a.getDescription(), a.getTypeBien(), a.getTypeTransaction(),
            a.getPrix(), a.getSuperficie(), a.getPieces(), a.getQuartier(), a.getVille(), a.getAdresse(),
            a.getLatitude(), a.getLongitude(), a.getEquipements().stream().sorted().toList(), a.getStatut(),
            new Proprietaire(a.getProprietaire().getId(), a.getProprietaire().getNomComplet()),
            a.getCreeLe(), a.getModifieLe());
    }
}
