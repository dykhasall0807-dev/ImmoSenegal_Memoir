package sn.immosenegal.health;

import java.util.Map;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

/**
 * Réveil léger : répond sans toucher à la base de données.
 * Sert aux visites automatiques qui empêchent l'hébergement gratuit de s'endormir. Utiliser /api/health pour cela
 * maintiendrait aussi la base éveillée en permanence et épuiserait son quota gratuit.
 */
@RestController
@RequestMapping("/api/ping")
@Tag(name = "Santé")
public class PingController {

    @GetMapping
    @Operation(summary = "Le serveur répond (sans interroger la base de données)")
    public Map<String, String> ping() {
        return Map.of("statut", "UP");
    }
}
