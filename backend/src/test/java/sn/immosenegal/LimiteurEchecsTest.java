package sn.immosenegal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;

import org.junit.jupiter.api.Test;

import sn.immosenegal.security.LimiteurEchecs;

/** Tests unitaires du limiteur, avec une horloge simulée (aucune attente réelle). */
class LimiteurEchecsTest {

    private final MutableClock horloge = new MutableClock();
    private final LimiteurEchecs limiteur = new LimiteurEchecs(3, Duration.ofMinutes(15), horloge);

    @Test
    void neBloquePasAvantLeMaximum() {
        limiteur.enregistrerEchec("a");
        limiteur.enregistrerEchec("a");
        assertThat(limiteur.estBloque("a")).isFalse();
        assertThat(limiteur.secondesRestantes("a")).isZero();
    }

    @Test
    void bloqueAuMaximumEtIndiqueLeTempsRestant() {
        for (int i = 0; i < 3; i++) {
            limiteur.enregistrerEchec("a");
        }
        assertThat(limiteur.estBloque("a")).isTrue();
        assertThat(limiteur.secondesRestantes("a")).isEqualTo(15 * 60);

        horloge.avancer(Duration.ofMinutes(5));
        assertThat(limiteur.secondesRestantes("a")).isEqualTo(10 * 60);
    }

    @Test
    void debloqueApresLaFenetre() {
        for (int i = 0; i < 3; i++) {
            limiteur.enregistrerEchec("a");
        }
        horloge.avancer(Duration.ofMinutes(15));
        assertThat(limiteur.estBloque("a")).isFalse();
    }

    @Test
    void lesClesSontIndependantes() {
        for (int i = 0; i < 3; i++) {
            limiteur.enregistrerEchec("a");
        }
        assertThat(limiteur.estBloque("a")).isTrue();
        assertThat(limiteur.estBloque("b")).isFalse();
    }

    @Test
    void reinitialiserRemetLeCompteurAZero() {
        limiteur.enregistrerEchec("a");
        limiteur.enregistrerEchec("a");
        limiteur.reinitialiser("a");
        limiteur.enregistrerEchec("a");
        assertThat(limiteur.estBloque("a")).isFalse();
    }

    @Test
    void lesVieuxEchecsSortentDeLaFenetreGlissante() {
        limiteur.enregistrerEchec("a");
        limiteur.enregistrerEchec("a");
        horloge.avancer(Duration.ofMinutes(16));
        limiteur.enregistrerEchec("a");
        assertThat(limiteur.estBloque("a")).isFalse();
    }

    @Test
    void refuseUnMaximumInvalide() {
        assertThatThrownBy(() -> new LimiteurEchecs(0, Duration.ofMinutes(1), horloge))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
