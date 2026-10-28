package sn.immosenegal.auth;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

/** Aucun champ « role » : une inscription publique crée toujours un compte CLIENT. */
public record RegisterRequest(
    @NotBlank @Email @Size(max = 254)
    String email,

    @NotBlank @Size(min = 12, max = 72, message = "Le mot de passe doit contenir entre 12 et 72 caractères.")
    @Schema(accessMode = Schema.AccessMode.WRITE_ONLY)
    String password,

    @NotBlank @Size(max = 120)
    String nomComplet,

    @Pattern(regexp = "^\\+?[0-9 ]{8,15}$", message = "Numéro de téléphone invalide.")
    String telephone
) {
}
