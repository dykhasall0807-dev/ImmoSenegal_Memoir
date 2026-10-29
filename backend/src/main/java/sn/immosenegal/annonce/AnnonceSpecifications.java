package sn.immosenegal.annonce;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

import jakarta.persistence.criteria.Predicate;

import org.springframework.data.jpa.domain.Specification;

/** Construit la requête SQL à partir des critères. Toutes les valeurs sont passées en paramètres liés. */
final class AnnonceSpecifications {

    private AnnonceSpecifications() {
    }

    static Specification<Annonce> depuis(CriteresRecherche c) {
        return (root, query, cb) -> {
            List<Predicate> predicats = new ArrayList<>();

            if (c.transaction() != null) {
                predicats.add(cb.equal(root.get("typeTransaction"), c.transaction()));
            }
            if (c.type() != null) {
                predicats.add(cb.equal(root.get("typeBien"), c.type()));
            }
            if (c.statut() != null) {
                predicats.add(cb.equal(root.get("statut"), c.statut()));
            }
            if (c.prixMin() != null) {
                predicats.add(cb.greaterThanOrEqualTo(root.<Long>get("prix"), c.prixMin()));
            }
            if (c.prixMax() != null) {
                predicats.add(cb.lessThanOrEqualTo(root.<Long>get("prix"), c.prixMax()));
            }
            if (c.superficieMin() != null) {
                predicats.add(cb.greaterThanOrEqualTo(root.<java.math.BigDecimal>get("superficie"), c.superficieMin()));
            }
            if (c.superficieMax() != null) {
                predicats.add(cb.lessThanOrEqualTo(root.<java.math.BigDecimal>get("superficie"), c.superficieMax()));
            }
            if (c.piecesMin() != null) {
                predicats.add(cb.greaterThanOrEqualTo(root.<Integer>get("pieces"), c.piecesMin()));
            }
            if (c.quartier() != null && !c.quartier().isBlank()) {
                predicats.add(contient(cb, root.get("quartier"), c.quartier()));
            }
            if (c.ville() != null && !c.ville().isBlank()) {
                predicats.add(contient(cb, root.get("ville"), c.ville()));
            }
            if (c.equipements() != null) {
                // Tous les équipements demandés doivent être présents.
                for (String equipement : c.equipements()) {
                    String propre = equipement.trim().toLowerCase(Locale.ROOT);
                    if (!propre.isEmpty()) {
                        predicats.add(cb.isMember(propre, root.<Set<String>>get("equipements")));
                    }
                }
            }
            return cb.and(predicats.toArray(new Predicate[0]));
        };
    }

    private static Predicate contient(jakarta.persistence.criteria.CriteriaBuilder cb,
                                      jakarta.persistence.criteria.Expression<String> champ, String valeur) {
        String motif = "%" + echapper(valeur.trim().toLowerCase(Locale.ROOT)) + "%";
        return cb.like(cb.lower(champ), motif, '\\');
    }

    /** Les caractères % et _ saisis par l'utilisateur sont traités comme du texte, pas comme des jokers. */
    static String echapper(String texte) {
        return texte.replace("\\", "\\\\").replace("%", "\\%").replace("_", "\\_");
    }
}
