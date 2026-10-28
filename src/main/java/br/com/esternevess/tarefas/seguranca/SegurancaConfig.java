package br.com.esternevess.tarefas.seguranca;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.InMemoryUserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;

import br.com.esternevess.tarefas.dominio.Perfil;

/**
 * Autenticação HTTP Basic com usuários fixos em memória: serve para demonstrar perfis e
 * permissões, não para produção. Em um sistema real os usuários viriam do banco e o login
 * seria por token (JWT/OAuth2).
 */
@Configuration
public class SegurancaConfig {

    @Bean
    public SecurityFilterChain filtros(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(a -> a
                        .requestMatchers("/actuator/health/**").permitAll()
                        .requestMatchers(HttpMethod.POST, "/tarefas").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.PUT, "/tarefas/*").hasAnyRole("ADMIN", "SUPERVISOR")
                        .requestMatchers(HttpMethod.DELETE, "/tarefas/*").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .httpBasic(Customizer.withDefaults());
        return http.build();
    }

    @Bean
    public PasswordEncoder codificadorDeSenha() {
        // Custo 4 (o padrão é 10) para a demonstração e os testes não ficarem lentos.
        return new BCryptPasswordEncoder(4);
    }

    @Bean
    public UserDetailsService usuarios(PasswordEncoder codificador, @Value("${app.senha-demo}") String senha) {
        String senhaCodificada = codificador.encode(senha);
        return new InMemoryUserDetailsManager(
                usuario("admin", senhaCodificada, Perfil.ADMIN),
                usuario("supervisor", senhaCodificada, Perfil.SUPERVISOR),
                usuario("ana", senhaCodificada, Perfil.COLABORADOR),
                usuario("bruno", senhaCodificada, Perfil.COLABORADOR));
    }

    private static UserDetails usuario(
            String nome, String senhaCodificada, Perfil perfil) {
        return User.withUsername(nome).password(senhaCodificada).roles(perfil.name()).build();
    }
}
