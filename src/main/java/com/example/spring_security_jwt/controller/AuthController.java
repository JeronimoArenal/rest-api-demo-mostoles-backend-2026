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
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirements;
import io.swagger.v3.oas.annotations.tags.Tag;
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

/**
 * Autenticación JWT: registro, login y comprobación. Estos endpoints son
 * públicos (no requieren token), por eso se anotan con
 * {@code @SecurityRequirements({})} para excluirlos del requisito global
 * {@code bearerAuth} que declara {@code OpenApiConfig}.
 */
@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Tag(name = "Autenticación", description = "Registro, login y comprobación JWT (endpoints públicos)")
public class AuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtils jwtUtils;

    private static final Logger LOGGER = LoggerFactory.getLogger(AuthController.class);


    @GetMapping("/test")
    @Operation(summary = "Comprueba que el controlador de autenticación responde")
    @SecurityRequirements({})
    public ResponseEntity<String> test() {
        LOGGER.info("========== ENTRE EN AUTH CONTROLLER ==========");
        return ResponseEntity.ok("AUTH CONTROLLER FUNCIONA");
    }

    //....................... registerUser .......................................
    @PostMapping("/signup")
    @Operation(summary = "Registra un nuevo usuario en la aplicación")
    @SecurityRequirements({})
    public ResponseEntity<?> registerUser(
            @Valid @RequestBody SignupRequest signupRequest,
            BindingResult validationResults) {

        LOGGER.info("========== ENTRO EN /api/auth/signup ==========");

        LOGGER.info("Username recibido: {}", signupRequest.getUsername());
        if(userRepository.existsByUsername(signupRequest.getUsername())){
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error. el username ya existe:"));
        }
        if(userRepository.existsByEmail(signupRequest.getEmail())){
            return ResponseEntity.badRequest()
                    .body(new MessageResponse("Error. el email ya existe:"));
        }

        User user = User.builder()
                        .username(signupRequest.getUsername())
                        .email(signupRequest.getEmail())
                        .password(passwordEncoder.encode(signupRequest.getPassword()))
                        .build();

        Set<String> strRoles = signupRequest.getRole();
        Set<Role> roles = new HashSet<>();

        if(strRoles == null){
            Role userRole = roleRepository.findByName(ERole.ROLE_USER)
                    .orElseThrow(() -> new RuntimeException("Error, role not found"));

            roles.add(userRole);
        } else {
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
    @Operation(summary = "Inicia sesión y devuelve el token JWT (Bearer)")
    @SecurityRequirements({})
    public ResponseEntity<?> authenticateUser(@Valid @RequestBody LoginRequest loginRequest, BindingResult
            result) {
        Authentication authentication = authenticationManager
                .authenticate(new UsernamePasswordAuthenticationToken(loginRequest.getUsername(),
                        loginRequest.getPassword()));

        SecurityContextHolder.getContext().setAuthentication(authentication);
        String token = jwtUtils.generateJwtToken(authentication);
        UserDetailsImpl userDetails = (UserDetailsImpl) authentication.getPrincipal();

        Set<String> roles = userDetails.getAuthorities()
                .stream()
                .map(item -> item.getAuthority())
                .collect(Collectors.toSet());

        LOGGER.info("Roles del usuario: {}", roles);
        return ResponseEntity.ok(new JwtResponse(token, userDetails.getId(), userDetails.getUsername(),
                userDetails.getEmail(), roles));
    }

}
