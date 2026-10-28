package sn.immosenegal.admin;

import java.time.Instant;
import java.util.UUID;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;

@Entity
@Table(name = "demandes_role")
public class DemandeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Enumerated(EnumType.STRING)
    @Column(name = "role_demande", nullable = false, length = 20)
    private Role roleDemande;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private StatutDemande statut = StatutDemande.EN_ATTENTE;

    @Column(name = "cree_le", nullable = false)
    private Instant creeLe = Instant.now();

    @Column(name = "decide_le")
    private Instant decideLe;

    @Column(name = "decide_par")
    private UUID decidePar;

    protected DemandeRole() {
        // requis par JPA
    }

    public DemandeRole(User user, Role roleDemande) {
        this.user = user;
        this.roleDemande = roleDemande;
    }

    public UUID getId() { return id; }
    public User getUser() { return user; }
    public Role getRoleDemande() { return roleDemande; }
    public StatutDemande getStatut() { return statut; }
    public Instant getCreeLe() { return creeLe; }
    public Instant getDecideLe() { return decideLe; }
    public UUID getDecidePar() { return decidePar; }

    public void decider(StatutDemande nouveauStatut, UUID administrateur) {
        this.statut = nouveauStatut;
        this.decideLe = Instant.now();
        this.decidePar = administrateur;
    }
}
