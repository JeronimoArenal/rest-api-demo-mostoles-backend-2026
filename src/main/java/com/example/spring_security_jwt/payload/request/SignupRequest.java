package com.example.spring_security_jwt.payload.request;

import com.example.spring_security_jwt.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.util.Set;

@Getter @Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SignupRequest {

    @NotBlank(message = "El nombre de usuario no puede estar vacío")
    @Size(min = 3, max = 20)
    private String username;

    @NotBlank(message = "El email no puede estar vacío")
    @Email(message = "El formato del email no es válido")
    @Size(max = 50)
    private String email;

//    private Set<Role> roles;

    private Set<String> role;

    @NotBlank(message = "La contraseña no puede estar vacía")
    @Size(min = 6, max = 40)
    private String password;
}
