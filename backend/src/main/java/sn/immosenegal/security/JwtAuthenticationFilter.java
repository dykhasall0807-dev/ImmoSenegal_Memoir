package sn.immosenegal.security;

import java.io.IOException;
import java.util.List;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.filter.OncePerRequestFilter;

import sn.immosenegal.user.User;
import sn.immosenegal.user.UserRepository;

/**
 * Lit le jeton « Authorization: Bearer ... ». Si le jeton est valide ET que le compte existe et est actif,
 * l'utilisateur (avec son rôle ACTUEL en base) est placé dans le contexte de sécurité.
 * Sinon la requête reste anonyme et sera refusée (401) par les règles d'autorisation.
 * Cette classe n'est volontairement pas un @Component : elle est ajoutée uniquement à la chaîne Spring Security.
 */
public class JwtAuthenticationFilter extends OncePerRequestFilter {

    private static final String PREFIXE = "Bearer ";

    private final JwtService jwtService;
    private final UserRepository users;

    public JwtAuthenticationFilter(JwtService jwtService, UserRepository users) {
        this.jwtService = jwtService;
        this.users = users;
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain)
            throws ServletException, IOException {
        String entete = request.getHeader("Authorization");
        if (entete != null && entete.startsWith(PREFIXE)) {
            jwtService.lireIdentifiant(entete.substring(PREFIXE.length()).trim())
                .flatMap(users::findById)
                .filter(User::isActif)
                .ifPresent(u -> {
                    AuthenticatedUser principal = new AuthenticatedUser(u.getId(), u.getEmail(), u.getRole());
                    var authentification = new UsernamePasswordAuthenticationToken(
                        principal, null, List.of(new SimpleGrantedAuthority("ROLE_" + u.getRole().name())));
                    SecurityContextHolder.getContext().setAuthentication(authentification);
                });
        }
        chain.doFilter(request, response);
    }
}
