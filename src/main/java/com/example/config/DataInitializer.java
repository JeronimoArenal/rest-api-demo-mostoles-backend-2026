package com.example.config;

import com.example.spring_security_jwt.model.ERole;
import com.example.spring_security_jwt.model.Role;
import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.repository.RoleRepository;
import com.example.spring_security_jwt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("========================================");
        log.info("Inicializando seguridad...");
        log.info("========================================");

        // =========================================================
        // ROLES
        // =========================================================

        Role adminRole = createRole(ERole.ROLE_ADMIN);
        Role userRole = createRole(ERole.ROLE_USER);

        // =========================================================
        // USUARIOS
        // =========================================================

        createUserIfNotExists(
                "admin",
                "admin@test.com",
                "123456",
                adminRole
        );

        createUserIfNotExists(
                "user",
                "user@test.com",
                "123456",
                userRole
        );

        log.info("========================================");
        log.info("Seguridad inicializada correctamente.");
        log.info("========================================");
    }

    // =============================================================
    // CREAR ROL
    // =============================================================

    private Role createRole(ERole roleName) {

        return roleRepository.findByName(roleName)
                .orElseGet(() -> {

                    Role role = Role.builder()
                            .name(roleName)
                            .build();

                    Role savedRole = roleRepository.save(role);

                    log.info("Rol '{}' creado.", roleName);

                    return savedRole;
                });
    }

    // =============================================================
    // CREAR USUARIO
    // =============================================================

    private void createUserIfNotExists(
            String username,
            String email,
            String password,
            Role role) {

        if (userRepository.findByUsername(username).isPresent()) {

            log.info("El usuario '{}' ya existe.", username);

            return;
        }

        User user = User.builder()
                .username(username)
                .email(email)
                .password(passwordEncoder.encode(password))
                .build();

        user.getRoles().add(role);

        User savedUser = userRepository.save(user);

        log.info(
                "Usuario '{}' creado con ID {} y rol '{}'.",
                savedUser.getUsername(),
                savedUser.getId(),
                role.getName()
        );
    }
}