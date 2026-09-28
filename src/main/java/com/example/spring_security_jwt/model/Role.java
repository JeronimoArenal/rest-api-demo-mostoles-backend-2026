package com.example.spring_security_jwt.model;

import jakarta.persistence.*;
import lombok.*;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

/**
 * El perfil/grupo: Actúa como un contenedor. Agrupa un conjunto de permisos bajo un nombre
 * (ej. ROLE_ADMIN, ROLE_OPERATOR).
 */
@Getter @Setter
@Entity
@Table(name = "security_roles")
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Enumerated(EnumType.STRING)
    private ERole name;

}