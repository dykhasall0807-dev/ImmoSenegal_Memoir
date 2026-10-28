package sn.immosenegal.security;

import java.util.UUID;

import sn.immosenegal.user.Role;

/** Identité de l'utilisateur connecté, reconstruite depuis la base à chaque requête (jamais depuis le jeton). */
public record AuthenticatedUser(UUID id, String email, Role role) {
}
