package sn.immosenegal.security;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

/**
 * Compte les échecs récents par clé (email ou adresse IP) sur une fenêtre glissante.
 * Une clé est « bloquée » dès qu'elle atteint le maximum d'échecs dans la fenêtre.
 * Stockage en mémoire : suffisant pour un seul serveur (voir README pour les limites).
 */
public class LimiteurEchecs {

    private static final int TAILLE_MAX_AVANT_NETTOYAGE = 10_000;

    private final int maxEchecs;
    private final Duration fenetre;
    private final Clock horloge;
    private final Map<String, Deque<Instant>> echecs = new HashMap<>();

    public LimiteurEchecs(int maxEchecs, Duration fenetre, Clock horloge) {
        if (maxEchecs < 1) {
            throw new IllegalArgumentException("maxEchecs doit être >= 1");
        }
        this.maxEchecs = maxEchecs;
        this.fenetre = fenetre;
        this.horloge = horloge;
    }

    public synchronized boolean estBloque(String cle) {
        purger(cle);
        Deque<Instant> liste = echecs.get(cle);
        return liste != null && liste.size() >= maxEchecs;
    }

    public synchronized void enregistrerEchec(String cle) {
        purger(cle);
        echecs.computeIfAbsent(cle, k -> new ArrayDeque<>()).addLast(horloge.instant());
        if (echecs.size() > TAILLE_MAX_AVANT_NETTOYAGE) {
            new java.util.ArrayList<>(echecs.keySet()).forEach(this::purger);
        }
    }

    public synchronized void reinitialiser(String cle) {
        echecs.remove(cle);
    }

    /** Secondes à attendre avant que la clé soit débloquée (0 si elle ne l'est pas). */
    public synchronized long secondesRestantes(String cle) {
        purger(cle);
        Deque<Instant> liste = echecs.get(cle);
        if (liste == null || liste.size() < maxEchecs) {
            return 0;
        }
        long millis = Duration.between(horloge.instant(), liste.peekFirst().plus(fenetre)).toMillis();
        return Math.max(1, (long) Math.ceil(millis / 1000.0));
    }

    private void purger(String cle) {
        Deque<Instant> liste = echecs.get(cle);
        if (liste == null) {
            return;
        }
        Instant limite = horloge.instant().minus(fenetre);
        while (!liste.isEmpty() && !liste.peekFirst().isAfter(limite)) {
            liste.removeFirst();
        }
        if (liste.isEmpty()) {
            echecs.remove(cle);
        }
    }
}
