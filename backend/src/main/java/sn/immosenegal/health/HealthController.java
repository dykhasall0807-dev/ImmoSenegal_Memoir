package sn.immosenegal.health;

import java.util.LinkedHashMap;
import java.util.Map;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;

import org.springframework.dao.DataAccessException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/health")
@Tag(name = "Santé", description = "Vérification que l'API et la base de données répondent")
public class HealthController {

    private final JdbcTemplate jdbc;

    public HealthController(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    @GetMapping
    @Operation(summary = "État de l'API et de la base de données (public)")
    public ResponseEntity<Map<String, Object>> health() {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("application", "ImmoSénégal");
        try {
            String version = jdbc.queryForObject(
                "SELECT valeur FROM app_meta WHERE cle = 'schema_version'", String.class);
            body.put("statut", "UP");
            body.put("baseDeDonnees", "OK");
            body.put("schemaVersion", version);
            return ResponseEntity.ok(body);
        } catch (DataAccessException e) {
            // On n'expose aucun détail technique (URL, identifiants, message SQL).
            body.put("statut", "DOWN");
            body.put("baseDeDonnees", "INDISPONIBLE");
            return ResponseEntity.status(HttpStatus.SERVICE_UNAVAILABLE).body(body);
        }
    }
}
