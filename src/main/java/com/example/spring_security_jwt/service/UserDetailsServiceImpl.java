package com.example.spring_security_jwt.service;

import com.example.spring_security_jwt.model.User;
import com.example.spring_security_jwt.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Servicio encargado de buscar usuarios y proporcionar a Spring Security
 * la información necesaria para identificarlos y comprobar sus permisos.
 *
 * Implementa UserDetailsService, una interfaz de Spring Security que permite
 * cargar un usuario a partir de su nombre de usuario.
 */
@Service
@RequiredArgsConstructor
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Este método puede utilizarse durante el login y también cuando
     * AuthTokenFilter valida un JWT y necesita recuperar al usuario.
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {

        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // IMPORTANTE:
        // Aquí no se valida el JWT ni se genera uno. Se prepara la información del usuario
        // para que Spring Security pueda autenticarlo y comprobar sus autoridades.
        return UserDetailsImpl.build(user);
    }
}