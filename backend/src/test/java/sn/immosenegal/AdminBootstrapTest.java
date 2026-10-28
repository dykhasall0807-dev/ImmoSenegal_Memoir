package sn.immosenegal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import sn.immosenegal.audit.AuditService;
import sn.immosenegal.config.AdminBootstrap;
import sn.immosenegal.user.Role;
import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

/** Tests unitaires (sans base de données) de la création du premier administrateur. */
class AdminBootstrapTest {

    private final UserRepository users = mock(UserRepository.class);
    private final AuditService audit = mock(AuditService.class);
    private final BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
    private final ApplicationArguments args = mock(ApplicationArguments.class);

    private AdminBootstrap bootstrap(String email, String motDePasse) {
        return new AdminBootstrap(users, encoder, audit, email, motDePasse);
    }

    @Test
    void sansVariablesAucunAdminNEstCree() {
        bootstrap("", "").run(args);
        verify(users, never()).save(any());
    }

    @Test
    void creeLePremierAdminAvecUnMotDePasseHache() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false);

        bootstrap("Admin@Example.test", "Un-Mot-De-Passe-Long-12").run(args);

        ArgumentCaptor<User> capture = ArgumentCaptor.forClass(User.class);
        verify(users).save(capture.capture());
        User admin = capture.getValue();
        assertThat(admin.getRole()).isEqualTo(Role.ADMIN);
        assertThat(admin.getEmail()).isEqualTo("admin@example.test");
        assertThat(admin.getPasswordHash()).isNotEqualTo("Un-Mot-De-Passe-Long-12");
        assertThat(encoder.matches("Un-Mot-De-Passe-Long-12", admin.getPasswordHash())).isTrue();
    }

    @Test
    void neCreeRienSiUnAdminExisteDeja() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(true);
        bootstrap("admin@example.test", "Un-Mot-De-Passe-Long-12").run(args);
        verify(users, never()).save(any());
    }

    @Test
    void refuseUnMotDePasseTropCourt() {
        when(users.existsByRole(Role.ADMIN)).thenReturn(false);
        assertThatThrownBy(() -> bootstrap("admin@example.test", "court").run(args))
            .isInstanceOf(IllegalStateException.class);
        verify(users, never()).save(any());
    }

    @Test
    void refuseUneConfigurationIncomplete() {
        assertThatThrownBy(() -> bootstrap("admin@example.test", "").run(args))
            .isInstanceOf(IllegalStateException.class);
    }
}
