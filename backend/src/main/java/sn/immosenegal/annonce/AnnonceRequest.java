package sn.immosenegal.annonce;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;

/**
 * Corps de création/modification. Volontairement SANS propriétaire : il est toujours déduit de la session.
 * Le statut est ignoré à la création (toute annonce commence DISPONIBLE).
 */
public record AnnonceRequest(
    @NotBlank @Size(max = 150) String titre,
    @Size(max = 5000) String description,
    @NotNull TypeBien typeBien,
    @NotNull TypeTransaction transaction,
    @NotNull @Positive @Max(1_000_000_000_000L) Long prix,
    @NotNull @Positive @DecimalMax("10000000") BigDecimal superficie,
    @Min(0) @Max(100) Integer pieces,
    @NotBlank @Size(max = 100) String quartier,
    @NotBlank @Size(max = 100) String ville,
    @Size(max = 255) String adresse,
    @DecimalMin("-90") @DecimalMax("90") BigDecimal latitude,
    @DecimalMin("-180") @DecimalMax("180") BigDecimal longitude,
    @Size(max = 30) Set<@NotBlank @Size(max = 50) String> equipements,
    StatutAnnonce statut
) {
}
