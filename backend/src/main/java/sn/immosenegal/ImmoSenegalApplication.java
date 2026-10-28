package sn.immosenegal;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.autoconfigure.security.servlet.UserDetailsServiceAutoConfiguration;

// L'utilisateur par défaut généré par Spring Security est désactivé : aucun compte n'existe
// tant que nous ne l'avons pas créé nous-mêmes (étape authentification).
@SpringBootApplication(exclude = UserDetailsServiceAutoConfiguration.class)
public class ImmoSenegalApplication {

    public static void main(String[] args) {
        SpringApplication.run(ImmoSenegalApplication.class, args);
    }
}
