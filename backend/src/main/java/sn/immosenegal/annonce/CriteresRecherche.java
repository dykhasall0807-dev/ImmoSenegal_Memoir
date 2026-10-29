package sn.immosenegal.annonce;

import java.math.BigDecimal;
import java.util.List;

/** Critères de recherche, tous facultatifs et combinables. */
public record CriteresRecherche(
    TypeTransaction transaction, TypeBien type, Long prixMin, Long prixMax,
    BigDecimal superficieMin, BigDecimal superficieMax, String quartier, String ville,
    Integer piecesMin, List<String> equipements, StatutAnnonce statut) {
}
