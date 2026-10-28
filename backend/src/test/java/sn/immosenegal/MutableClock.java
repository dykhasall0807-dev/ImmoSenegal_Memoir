package sn.immosenegal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZoneOffset;

/** Horloge de test dont on peut faire avancer l'heure. */
class MutableClock extends Clock {

    private Instant maintenant = Instant.parse("2026-01-01T00:00:00Z");

    void avancer(Duration duree) {
        maintenant = maintenant.plus(duree);
    }

    @Override
    public ZoneId getZone() {
        return ZoneOffset.UTC;
    }

    @Override
    public Clock withZone(ZoneId zone) {
        return this;
    }

    @Override
    public Instant instant() {
        return maintenant;
    }
}
