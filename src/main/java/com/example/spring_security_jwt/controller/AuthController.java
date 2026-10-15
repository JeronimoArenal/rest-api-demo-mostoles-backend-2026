package com.example.spring_security_jwt.controller;

import com.example.spring_security_jwt.jwt.JwtUtils;
import com.example.spring_security_jwt.model.ERole;
import com.example.spring_security_jwt.model.Role;
import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.payload.request.LoginRequest;
import com.example.spring_security_jwt.payload.request.SignupRequest;
import com.example.spring_security_jwt.payload.response.JwtResponse;
import com.example.spring_security_jwt.payload.response.MessageResponse;
import com.example.spring_security_jwt.repository.RoleRepository;
import com.example.spring_security_jwt.repository.UserRepository;
import com.example.spring_security_jwt.service.UserDetailsImpl;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;

import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);


    @GetMapping("/test")
    public ResponseEntity<String> test() {
        LOGGER.info("========== ENTRE EN AUTH CONTROLLER ==========");
        return ResponseEntity.ok("AUTH CONTROLLER FUNCIONA");
    }

    //....................... registerUser .......................................
    @PostMapping("/signup")
    public ResponseEntity<?> registerUser(@Valid @RequestBody SignupRequest signupRequest) {

        LOGGER.info("========== ENTRO EN /api/auth/signup ==========");

        LOGGER.info("Username recibido: {}", signupRequest.getUsername());
        // 1. Comprueba si el nombre de usuario ya está registrado en la base de datos.
        if(userRepository.existsByUsername(signupRequest.getUsername())){
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error. el username ya existe:"));
        }

        // 2.Comprueba si el correo electrónico ya está registrado.
        if(userRepository.existsByEmail(signupRequest.getEmail())){
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error. el email ya existe:"));
        }

        // 3. Creación de la entidad Usuario utilizando el patrón Builder.
        // La contraseña se encripta de forma segura con BCrypt (passwordEncoder) antes de guardarla.
        User user = User.builder()
                        .username(signupRequest.getUsername())
                        .email(signupRequest.getEmail())
                        .password(passwordEncoder.encode(signupRequest.getPassword()))
                        .build();

        // 4. Procesamiento y asignación de roles.
        Set<String> strRoles = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();

        // Si el cliente no envía roles en la petición, se le asigna el rol por defecto (ROLE_USER).
        if(strRoles == null){
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error, role not found"));
            roles.add(userRole);
        } else {
            // Si el cliente envía roles, se mapean las cadenas de texto a las entidades Role de la BD.
            strRoles.forEach(role -> {
                switch (role) {
                    case "admin" : Role adminRole = roleRepository.findByName(ERole.ROLE_ADMIN)
                            .orElseThrow(() -> new RuntimeException("Error: role is not found"));
                    roles.add(adminRole);
                    break;

                    default: Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                            .orElseThrow(() -> new RuntimeException("Error: role is not found"));
                    roles.add(userRole);
                }
                    } );
        }

        user.setRoles(roles);
        userRepository.save(user);
        LOGGER.info("Usuario registrado con éxito: {}", user.getUsername());

        // Respondemos al cliente
        return ResponseEntity.ok(new MessageResponse("Usuario registrado exitosamente."));
    }

    //....................... login .......................................
    @PostMapping("/signin")
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest) {

        // 1. Valida las credenciales (usuario y contraseña) usando el AuthenticationManager.
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(
                        loginRequest.getUsername(),
                        loginRequest.getPassword()
                ));

        // 2. Si las credenciales son validas, guarda el objeto de autenticación en el contexto de seguridad
        // de Spring. Esto le indica a la aplicación que el usuario actual ya está autenticado con éxito.
        SecurityContextHolder.getContext().setAuthentication(authentication);

        // 3. Genera el token JWT firmado a partir de los datos y claims del usuario autenticado.
        String token = jwtUtils.generateJwtToken(authentication);

        // 4. Obtiene el objeto principal (la identidad del usuario) y lo castea a tu implementación UserDetailsImpl.
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        // 5. Extrae los permisos/autoridades del usuario y los transforma en una colección de Strings (Roles).
        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toSet());

        // 7. Devuelve una respuesta 200 OK con un cuerpo JSON (JwtResponse) que incluye el token y los datos del perfil.
        return ResponseEntity.ok(new JwtResponse(
                token,
                userDetails.getId(),
                userDetails.getUsername(),
                userDetails.getEmail(),
                roles
        ));
    }

}
