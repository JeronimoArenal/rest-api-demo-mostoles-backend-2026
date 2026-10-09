package com.example.spring_security_jwt.config;

import com.example.spring_security_jwt.jwt.AuthEntryPointJwt;
import com.example.spring_security_jwt.jwt.AuthTokenFilter;
import com.example.spring_security_jwt.jwt.JwtUtils;
import com.example.spring_security_jwt.service.UserDetailsServiceImpl;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * EL PANEL DE CONTROL DEL MONOLITO:
 * Aquí se orquestan y configuran los dos mundos (Authorization y Resource Server)
 */
@Configuration
@EnableMethodSecurity
@RequiredArgsConstructor
public class WebSecurityConfig {

    private final UserDetailsServiceImpl userDetailsService;
    private final AuthEntryPointJwt authEntryPointJwt;
    private final JwtUtils jwtUtils;    /* Genera/valida los JWT.*/

    /* recibe el JWT de las peticiones y lo valida. */
    /**
     * PIEZA DEL RESOURCE SERVER:
     * Instancia el filtro que interceptará las peticiones HTTP normales de las APIs
     * para buscar el token y validarlo en cada request.
     */
    @Bean
    public AuthTokenFilter authenticationJwtTokenFilter() {

        return new AuthTokenFilter(jwtUtils, userDetailsService);
    }

    /**
     * PIEZA DEL AUTHORIZATION SERVER:
     * El AuthenticationManager es el motor que procesa el login de las usuarios.
     * Lo inyecta aquí para poder usarlo en el controlador de login (AuthController)
     * cuando alguien introduce su usuario y contraseña por primera vez.
     */
    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration authConfig) throws Exception {
        return authConfig.getAuthenticationManager();
    }

    /**
     * PIEZA DEL AUTHORIZATION SERVER:
     * Verifica criptográficamente que la contraseña que envía el usuario coincide con el hash guardado en la DB.
     */
    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }


    /**
     * EL CONTRATO GENERAL DE SEGURIDAD (Une ambos roles)
     */
    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http.csrf(csrf -> csrf.disable()) // Desactivado para APIs REST (JWT)
                .exceptionHandling(exception ->
                        exception.authenticationEntryPoint(authEntryPointJwt))
                .sessionManagement(session ->
                        session.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/api/auth/**").permitAll() // Permite registrarse y loguearse sin token
                        .requestMatchers("/error").permitAll()
                        .requestMatchers("/v3/api-docs/**", "/swagger-ui.html", "/swagger-ui/**",
                                "/scalar", "/scalar/**").permitAll()
                        .anyRequest().authenticated()               // Todo lo demás requiere estar autenticado
                );

        /**
         * ENLACE CLAVE DEL RESOURCE SERVER:
         * Filtro manual (authenticationJwtTokenFilter) justo ANTES del filtro estándar de Spring.
         * Esto garantiza que si la petición lleva un token, se procese, valide y registre al usuario
         * en el SecurityContext antes de evaluar si tiene permisos para entrar.
         */
        http.addFilterBefore(authenticationJwtTokenFilter(),
                UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }
}
