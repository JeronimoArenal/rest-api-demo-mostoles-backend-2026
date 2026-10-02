package com.example.config;

import com.example.dao.PresentationDao;
import com.example.dao.ProductDao;
import com.example.entities.Presentation;
import com.example.entities.Product;
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

import java.math.BigDecimal;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final RoleRepository roleRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    private final PresentationDao presentationDao;
    private final ProductDao productDao;

    @Override
    @Transactional
    public void run(String... args) {

        log.info("========================================");
        log.info("Inicializando datos...");
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

        // =========================================================
        // PRESENTACIONES
        // =========================================================

        Presentation botella = createPresentation(
                "Botella",
                "Producto presentado en botella"
        );

        Presentation caja = createPresentation(
                "Caja",
                "Producto presentado en caja"
        );

        Presentation bolsa = createPresentation(
                "Bolsa",
                "Producto presentado en bolsa"
        );

        // =========================================================
        // PRODUCTOS
        // =========================================================

        createProduct(
                "Agua Mineral",
                "Agua mineral natural de 1 litro",
                100,
                new BigDecimal("1.20"),
                botella
        );

        createProduct(
                "Zumo de Naranja",
                "Zumo de naranja natural",
                50,
                new BigDecimal("2.50"),
                botella
        );

        createProduct(
                "Café Molido",
                "Café molido de tueste natural",
                75,
                new BigDecimal("4.95"),
                caja
        );

        createProduct(
                "Té Verde",
                "Té verde en bolsitas individuales",
                40,
                new BigDecimal("3.25"),
                caja
        );

        createProduct(
                "Arroz",
                "Arroz blanco de grano largo",
                80,
                new BigDecimal("2.10"),
                bolsa
        );

        log.info("========================================");
        log.info("Datos inicializados correctamente.");
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

                    log.info(
                            "Rol '{}' creado.",
                            roleName
                    );

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

            log.info(
                    "El usuario '{}' ya existe.",
                    username
            );

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

    // =============================================================
    // CREAR PRESENTACIÓN
    // =============================================================

    private Presentation createPresentation(
            String name,
            String description) {

        return presentationDao
                .findByName(name)
                .orElseGet(() -> {

                    Presentation presentation = new Presentation();

                    presentation.setName(name);
                    presentation.setDescription(description);

                    Presentation saved =
                            presentationDao.save(presentation);

                    log.info(
                            "Presentación '{}' creada.",
                            name
                    );

                    return saved;
                });
    }

    // =============================================================
    // CREAR PRODUCTO
    // =============================================================

    private Product createProduct(
            String name,
            String description,
            Integer stock,
            BigDecimal price,
            Presentation presentation) {

        if (productDao.findByName(name).isPresent()) {

            log.info(
                    "El producto '{}' ya existe.",
                    name
            );

            return productDao.findByName(name)
                    .orElseThrow();
        }

        Product product = new Product();

        product.setName(name);
        product.setDescription(description);
        product.setStock(stock);
        product.setPrice(price);
        product.setPresentation(presentation);

        Product saved = productDao.save(product);

        log.info(
                "Producto '{}' creado con presentación '{}'.",
                name,
                presentation.getName()
        );

        return saved;
    }
}