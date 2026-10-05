package com.example.spring_security_jwt.jwt;

import com.example.spring_security_jwt.service.UserDetailsServiceImpl;
import io.jsonwebtoken.JwtException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;

/**
 * RESOURCE SERVER
 *
 * Intercepta las peticiones -> Es un filtro HTTP:
 * En cada petición, busca el JWT, pide a JwtUtils que lo valide y, si es correcto,
 * le dice a Spring Security quién es el usuario.
 */
@RequiredArgsConstructor
public class AuthTokenFilter extends OncePerRequestFilter {

    private final JwtUtils jwtUtils;
    private final UserDetailsServiceImpl userDetailsServiceImpl;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {

        try {
            // Intentamos extraer el token JWT de la cabecera 'Authorization' de la petición
            String jwt = parseJwt(request);
            // Si el token existe y es sintácticamente válido (firma, expiración, etc.)
            if (jwt != null && jwtUtils.validateJwtToken(jwt)) {
                // Extraemos el nombre de usuario embebido dentro del propio token
                String username = jwtUtils.getUsernameFromJwtToken(jwt);

                // PROBLEMA: ¡Consulta a la BD en cada clic del usuario!
                UserDetails userDetails = userDetailsServiceImpl.loadUserByUsername(username);

                // Creamos el objeto principal de autenticación de Spring Security con los datos del usuario.
                // Pasamos las credenciales como 'null' porque el JWT ya demostró su identidad.
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                                userDetails,
                                null,
                                userDetails.getAuthorities()
                        );

                // Guardamos el usuario autenticado en el contexto de seguridad de la aplicación.
                // A partir de esta línea, Spring Security sabe quién es el usuario durante toda la petición.
                SecurityContextHolder.getContext().setAuthentication(authentication);
            }

        } catch (JwtException e) {
            System.out.println("JWT inválido: " + e.getMessage());

        } catch (Exception e) {
            System.out.println("Error procesando JWT: " + e.getMessage());
        }

        // MUY IMPORTANTE: continuar con el resto de filtros y llegar al Controller
        filterChain.doFilter(request, response);
    }

    /**
     * Método privado auxiliar para extraer el token limpio de la cabecera HTTP.
     */
    private String parseJwt(HttpServletRequest request) {

        // Obtenemos el valor de la cabecera llamada "Autorization"
        String headerAuth = request.getHeader("Authorization");

        // Verificamos que la cabecera no este vacia y que empiece por "Bearer"
        if (StringUtils.hasText(headerAuth) && headerAuth.startsWith("Bearer ")) {
            //Recortamos los 7 primeros caracteres, correspondentes a la palabra"Bearer"
            return headerAuth.substring(7);
        }

        // Si no hay cabecera o no usamos el formato Bearer, retornamos null (el usuario no envía token)
        return null;
    }
}