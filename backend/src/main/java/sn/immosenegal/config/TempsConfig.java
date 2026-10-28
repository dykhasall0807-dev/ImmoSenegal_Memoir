package sn.immosenegal.config;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TempsConfig {

    /** Horloge injectable : permet de simuler le temps dans les tests. */
    @Bean
    Clock horloge() {
        return Clock.systemUTC();
    }
}
