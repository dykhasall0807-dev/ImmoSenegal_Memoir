package sn.immosenegal.user;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "users")
public class User {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @Column(nullable = false, unique = true, length = 254)
    private String email;

    @Column(name = "password_hash", nullable = false, length = 100)
    private String passwordHash;

    @Column(name = "nom_complet", nullable = false, length = 120)
    private String nomComplet;

    @Column(length = 20)
    private String telephone;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private Role role;

    @Column(nullable = false)
    private boolean actif = true;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe = Instant.now();

    protected User() {
        // requis par JPA
    }

    public User(String email, String passwordHash, String nomComplet, String telephone, Role role) {
        this.email = email;
        this.passwordHash = passwordHash;
        this.nomComplet = nomComplet;
        this.telephone = telephone;
        this.role = role;
    }

    public UUID getId() { return id; }
    public String getEmail() { return email; }
    public String getPasswordHash() { return passwordHash; }
    public String getNomComplet() { return nomComplet; }
    public String getTelephone() { return telephone; }
    public Role getRole() { return role; }
    public boolean isActif() { return actif; }
    public Instant getCreeLe() { return creeLe; }

    public void setRole(Role role) { this.role = role; }
    public void setActif(boolean actif) { this.actif = actif; }
}
