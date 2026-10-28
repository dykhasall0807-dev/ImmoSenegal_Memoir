package sn.immosenegal.auth;

import java.time.Instant;
import java.util.UUID;

import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;

/** Vue publique d'un utilisateur : jamais de hash de mot de passe. */
public record UserResponse(UUID id, String email, String nomComplet, String telephone, Role role, Instant creeLe) {

    public static UserResponse de(User u) {
        return new UserResponse(u.getId(), u.getEmail(), u.getNomComplet(), u.getTelephone(), u.getRole(), u.getCreeLe());
    }
}
