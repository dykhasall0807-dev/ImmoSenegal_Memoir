package sn.immosenegal.admin;

import jakarta.validation.constraints.NotNull;

import sn.immosenegal.user.Role;

/** Corps de POST /api/auth/role-requests : seuls PROPRIETAIRE et AGENT sont acceptés (vérifié côté serveur). */
public record DemandeRoleRequest(@NotNull Role role) {
}
