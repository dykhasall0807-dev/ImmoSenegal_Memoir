package sn.immosenegal.config;

import java.util.Arrays;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.security.web.authentication.HttpStatusEntryPoint;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import sn.immosenegal.security.JwtAuthenticationFilter;
import sn.immosenegal.security.JwtService;
import sn.immosenegal.user.UserRepository;

/**
 * Configuration de sécurité de base : API sans session (stateless), CORS limité aux origines
 * configurées, tout est protégé sauf la santé de l'API et la documentation Swagger.
 * Le filtre JWT reconstruit l'utilisateur depuis la base ; les routes /api/admin/** exigent le rôle ADMIN.
 */
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, JwtService jwtService, UserRepository users) throws Exception {
        http
            // Pas de cookie de session : l'API lit un jeton Bearer. Le seul cookie (rafraîchissement) est HttpOnly, limité à
            // /api/auth, SameSite, et refresh/logout exigent un en-tête personnalisé : donc CSRF désactivé sans risque ici.
            .csrf(csrf -> csrf.disable())
            .cors(Customizer.withDefaults())
            .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
            .authorizeHttpRequests(auth -> auth
                .requestMatchers(HttpMethod.GET, "/api/health", "/api/ping").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login",
                    "/api/auth/refresh", "/api/auth/logout").permitAll()
                // Catalogue public en lecture ; « mes annonces » exige une session ; publier exige un rôle autorisé.
                .requestMatchers(HttpMethod.GET, "/api/annonces/mes-annonces").authenticated()
                .requestMatchers(HttpMethod.GET, "/api/annonces", "/api/annonces/*", "/api/annonces/*/photos/*").permitAll()
                .requestMatchers(HttpMethod.POST, "/api/annonces").hasAnyRole("PROPRIETAIRE", "AGENT", "ADMIN")
                // Zone réservée aux administrateurs : le rôle vient de la base, jamais du client.
                .requestMatchers("/api/admin/**").hasRole("ADMIN")
                .requestMatchers("/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                .anyRequest().authenticated())
            .addFilterBefore(new JwtAuthenticationFilter(jwtService, users), UsernamePasswordAuthenticationFilter.class)
            // 401 pour une requête non authentifiée (et non une redirection vers un formulaire).
            .exceptionHandling(e -> e.authenticationEntryPoint(new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED)));
        return http.build();
    }

    @Bean
    PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource(@Value("${app.cors.allowed-origins}") String[] allowedOrigins) {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOrigins(Arrays.stream(allowedOrigins).map(String::trim).toList());
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept", "X-Requested-With"));
        // Nécessaire pour que le navigateur accepte le cookie de rafraîchissement entre deux origines.
        config.setAllowCredentials(true);
        config.setExposedHeaders(List.of("Retry-After"));
        config.setMaxAge(3600L);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}
