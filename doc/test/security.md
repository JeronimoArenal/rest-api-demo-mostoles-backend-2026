# Guía completa de testing en Spring Boot con Spring Security

## Índice

1. Introducción al testing con Spring Security
2. Diferencias entre pruebas unitarias y de integración
3. Dependencias Maven
4. Conceptos de Spring Security que debes conocer
5. Estructura del proyecto
6. Probar servicios con Mockito
7. Probar controladores con `@WebMvcTest`
8. `@WithMockUser`: simular usuarios autenticados
9. Probar roles y autoridades
10. Probar endpoints públicos y protegidos
11. Probar autenticación mediante formulario
12. Probar autenticación Basic
13. Probar CSRF
14. Probar autenticación JWT
15. Probar filtros de seguridad
16. Pruebas de integración con `@SpringBootTest`
17. Probar usuarios y contraseñas
18. Probar acceso denegado y errores de autenticación
19. Probar métodos protegidos con `@PreAuthorize`
20. Problemas frecuentes
21. Ejecutar las pruebas
22. Buenas prácticas
23. Resumen de anotaciones

---

# 1. Introducción al testing con Spring Security

Spring Security es el framework de Spring encargado de gestionar aspectos como:

- Autenticación: comprobar quién es el usuario.
- Autorización: comprobar qué puede hacer ese usuario.
- Roles y autoridades.
- Gestión de sesiones.
- Protección contra CSRF.
- Autenticación mediante usuario y contraseña.
- Autenticación mediante HTTP Basic.
- Autenticación mediante JWT.
- Protección de endpoints y métodos.

Cuando una aplicación utiliza Spring Security, no basta con comprobar que un endpoint devuelve un `200 OK`.

También debemos verificar que:

- Un usuario no autenticado no puede acceder a un endpoint protegido.
- Un usuario autenticado puede acceder a los recursos permitidos.
- Un usuario sin los permisos necesarios recibe una respuesta de acceso denegado.
- Un administrador puede realizar las operaciones administrativas.
- Un token JWT válido permite el acceso correspondiente.
- Un token inválido o caducado no permite acceder.
- Las contraseñas se verifican de forma segura.

**Objetivo:** probar tanto la funcionalidad de la aplicación como las reglas de seguridad que la protegen.

---

# 2. Diferencias entre pruebas unitarias y de integración

## Pruebas unitarias

Comprueban una clase de manera aislada.

Por ejemplo, probar que un servicio no permite eliminar un usuario inexistente.

Normalmente utilizan:

- JUnit 5.
- Mockito.
- `@Mock`.
- `@InjectMocks`.

No necesitan cargar el contexto de Spring ni ejecutar los filtros de seguridad HTTP.

## Pruebas de seguridad web

Comprueban el comportamiento de Spring Security en la capa HTTP.

Por ejemplo:

- Acceder sin autenticación.
- Acceder como usuario autenticado.
- Comprobar los roles.
- Verificar que una petición POST necesita un token CSRF.
- Comprobar el comportamiento de los filtros de seguridad.

Suelen utilizar:

- `@WebMvcTest`.
- `MockMvc`.
- `@WithMockUser`.
- `spring-security-test`.

## Pruebas de integración

Comprueban el funcionamiento conjunto de varias capas:

- Configuración de Spring Security.
- Filtros de seguridad.
- Controladores.
- Servicios.
- Repositorios.
- Base de datos.
- Autenticación real, cuando corresponda.

Pueden utilizar:

- `@SpringBootTest`.
- `@AutoConfigureMockMvc`.
- `@ActiveProfiles("test")`.
- Una base de datos de pruebas.

## Comparación

| Tipo de prueba | ¿Qué comprueba? | ¿Ejecuta Spring Security HTTP? |
|---|---|---|
| Unitaria de servicio | Lógica de negocio | No normalmente |
| `@WebMvcTest` | Controlador y reglas web incluidas en el contexto | Sí |
| `@SpringBootTest` con `MockMvc` | Integración de las capas dentro del contexto | Sí |
| Prueba JWT aislada | Lógica propia de generación o validación del token | Depende de la implementación |
| Prueba de integración JWT | Filtros, validación del token y acceso HTTP | Sí |

Una distinción fundamental: `@WithMockUser` permite simular un usuario autenticado, pero **no demuestra que el inicio de sesión real ni la validación de un JWT funcionen correctamente**.

---

# 3. Dependencias Maven

En una aplicación que utiliza Spring Boot, Spring Security y pruebas MVC, puedes necesitar las siguientes dependencias.

```xml
<dependencies>

    <!-- API REST -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-web</artifactId>
    </dependency>

    <!-- Seguridad -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-security</artifactId>
    </dependency>

    <!-- Testing -->
    <dependency>
        <groupId>org.springframework.boot</groupId>
        <artifactId>spring-boot-starter-test</artifactId>
        <scope>test</scope>
    </dependency>

    <!-- Herramientas de testing de Spring Security -->
    <dependency>
        <groupId>org.springframework.security</groupId>
        <artifactId>spring-security-test</artifactId>
        <scope>test</scope>
    </dependency>

</dependencies>
```

Si la aplicación utiliza JPA:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>
```

Si necesita una base de datos H2 para pruebas:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

Las versiones deben estar gestionadas por el parent o BOM de Spring Boot. Evita declarar versiones independientes de Spring Security que sean incompatibles con la versión de Spring Boot de tu proyecto.

---

# 4. Conceptos de Spring Security que debes conocer

## Autenticación

La autenticación responde a la pregunta:

**¿Quién eres?**

Por ejemplo, un usuario introduce su nombre de usuario y contraseña, y Spring Security comprueba sus credenciales.

## Autorización

La autorización responde a la pregunta:

**¿Qué puedes hacer?**

Por ejemplo, un usuario autenticado puede consultar su perfil, pero solamente un administrador puede eliminar usuarios.

## Roles

Los roles representan categorías de permisos.

Ejemplos:

- `USER`.
- `ADMIN`.
- `MANAGER`.

En las expresiones `hasRole("ADMIN")`, Spring Security normalmente busca la autoridad `ROLE_ADMIN`.

## Authorities

Son las autoridades concretas que tiene un usuario.

Por ejemplo:

- `ROLE_ADMIN`.
- `ROLE_USER`.
- `usuario:leer`.
- `usuario:eliminar`.

`hasAuthority("usuario:leer")` busca esa autoridad exacta.

## CSRF

CSRF es una protección contra determinados ataques en aplicaciones que utilizan credenciales enviadas automáticamente por el navegador, como las sesiones mediante cookies.

En configuraciones web tradicionales, las peticiones que modifican datos pueden necesitar un token CSRF.

No desactives CSRF indiscriminadamente para conseguir que las pruebas pasen. En una API sin estado que utiliza exclusivamente tokens Bearer en la cabecera `Authorization`, puede ser apropiado desactivarlo, según la arquitectura y el modelo de autenticación.

## JWT

JWT es un formato de token que puede transportar información sobre la identidad y las autoridades de un usuario.

Spring Security puede validar tokens JWT y construir el contexto de autenticación a partir de ellos.

Hay que distinguir entre:

1. Simular un usuario autenticado.
2. Crear un JWT de prueba.
3. Validar un JWT real mediante la configuración de seguridad de la aplicación.

Son escenarios diferentes y deben probarse por separado.

---

# 5. Estructura del proyecto

Una estructura posible sería:

```text
src/
├── main/
│   ├── java/com/ejemplo/demo/
│   │   ├── DemoApplication.java
│   │   ├── config/
│   │   │   └── SecurityConfig.java
│   │   ├── controller/
│   │   │   └── UsuarioController.java
│   │   ├── service/
│   │   │   └── UsuarioService.java
│   │   ├── repository/
│   │   │   └── UsuarioRepository.java
│   │   ├── security/
│   │   │   └── JwtService.java
│   │   └── model/
│   │       └── Usuario.java
│   └── resources/
│       └── application.properties
└── test/
    ├── java/com/ejemplo/demo/
    │   ├── service/
    │   │   └── UsuarioServiceTest.java
    │   ├── controller/
    │   │   └── UsuarioControllerSecurityTest.java
    │   ├── security/
    │   │   ├── SecurityConfigTest.java
    │   │   └── JwtAuthenticationTest.java
    │   └── integration/
    │       └── SecurityIntegrationTest.java
    └── resources/
        └── application-test.properties
```

No necesitas crear todos estos archivos obligatoriamente. La estructura depende de la arquitectura de tu aplicación y de si utiliza sesiones, HTTP Basic o JWT.

---

# 6. Probar servicios con Mockito

Las pruebas unitarias de los servicios normalmente no necesitan Spring Security.

Supongamos que tenemos un servicio que consulta un repositorio:

```java
@Service
public class UsuarioService {

    private final UsuarioRepository usuarioRepository;

    public UsuarioService(UsuarioRepository usuarioRepository) {
        this.usuarioRepository = usuarioRepository;
    }

    public Usuario buscarPorId(Long id) {
        return usuarioRepository.findById(id)
                .orElseThrow(() ->
                    new IllegalArgumentException(
                        "Usuario no encontrado"
                    )
                );
    }
}
```

Podemos probarlo de forma aislada:

```java
@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void debeEncontrarUsuario() {
        Usuario usuario =
                new Usuario("Ana", "ana@ejemplo.com");

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        Usuario resultado =
                usuarioService.buscarPorId(1L);

        assertEquals("Ana", resultado.getNombre());
    }

    @Test
    void debeLanzarExcepcionSiNoExiste() {
        when(usuarioRepository.findById(99L))
                .thenReturn(Optional.empty());

        assertThrows(
                IllegalArgumentException.class,
                () -> usuarioService.buscarPorId(99L)
        );
    }
}
```

Esta prueba verifica la lógica del servicio, pero no comprueba si un usuario puede acceder a un endpoint.

Para eso necesitamos pruebas de seguridad web.

---

# 7. Probar controladores con `@WebMvcTest`

`@WebMvcTest` carga principalmente los componentes de la capa web y permite probar los controladores.

Cuando Spring Security está presente, la configuración de seguridad aplicable al contexto MVC también puede participar en las peticiones.

## Controlador de ejemplo

```java
@RestController
@RequestMapping("/api/usuarios")
public class UsuarioController {

    @GetMapping("/perfil")
    public Map<String, String> perfil() {
        return Map.of("mensaje", "Perfil del usuario");
    }

    @GetMapping("/admin")
    public Map<String, String> admin() {
        return Map.of("mensaje", "Zona administrativa");
    }
}
```

## Configuración de seguridad de ejemplo

Esta configuración permite consultar el perfil a usuarios autenticados y reserva la ruta administrativa para administradores.

```java
@Configuration
@EnableWebSecurity
public class SecurityConfig {

    @Bean
    SecurityFilterChain securityFilterChain(
            HttpSecurity http) throws Exception {

        return http
                .authorizeHttpRequests(auth -> auth
                    .requestMatchers("/api/publico/**").permitAll()
                    .requestMatchers("/api/usuarios/admin")
                        .hasRole("ADMIN")
                    .requestMatchers("/api/usuarios/**")
                        .authenticated()
                    .anyRequest().denyAll()
                )
                .httpBasic(Customizer.withDefaults())
                .build();
    }
}
```

Este ejemplo utiliza HTTP Basic para simplificar la demostración. No representa necesariamente la configuración adecuada para una API JWT o una aplicación con sesiones.

En una aplicación real, debes conservar las reglas de seguridad que deseas verificar. No reemplaces la configuración de producción por una configuración permisiva únicamente para hacer pasar las pruebas.

---

# 8. `@WithMockUser`: simular usuarios autenticados

`@WithMockUser` es una de las herramientas más utilizadas para probar Spring Security.

Permite ejecutar una prueba como si hubiera un usuario autenticado, sin tener que realizar un inicio de sesión real.

Import:

```java
import org.springframework.security.test.context.support.WithMockUser;
```

## Ejemplo básico

```java
@WebMvcTest(UsuarioController.class)
class UsuarioControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    @WithMockUser
    void usuarioAutenticadoPuedeConsultarPerfil()
            throws Exception {

        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isOk());
    }
}
```

Por defecto, `@WithMockUser` crea un usuario ficticio llamado `user` con la autoridad `ROLE_USER`.

## Definir nombre y rol

```java
@Test
@WithMockUser(
    username = "ana",
    roles = {"USER"}
)
void usuarioConRolUserPuedeAcceder() throws Exception {
    mockMvc.perform(get("/api/usuarios/perfil"))
            .andExpect(status().isOk());
}
```

## Simular un administrador

```java
@Test
@WithMockUser(
    username = "admin",
    roles = {"ADMIN"}
)
void administradorPuedeAccederAlPanel() throws Exception {
    mockMvc.perform(get("/api/usuarios/admin"))
            .andExpect(status().isOk());
}
```

## Simular un usuario sin rol de administrador

```java
@Test
@WithMockUser(
    username = "ana",
    roles = {"USER"}
)
void usuarioNormalNoPuedeAccederAlPanel()
        throws Exception {

    mockMvc.perform(get("/api/usuarios/admin"))
            .andExpect(status().isForbidden());
}
```

El último caso comprueba que el usuario está autenticado, pero no tiene permisos suficientes.

**Importante:** `@WithMockUser` no comprueba la contraseña almacenada, el proceso real de login ni la firma de un JWT. Simula el contexto de seguridad de la prueba.

---

# 9. Probar roles y autoridades

Es importante distinguir `roles` y `authorities`.

## Utilizar roles

```java
@WithMockUser(roles = {"ADMIN"})
```

Equivale, para esta prueba, a tener la autoridad:

```text
ROLE_ADMIN
```

Por tanto, funciona con:

```java
.hasRole("ADMIN")
```

No escribas `ROLE_` dentro de `roles`, porque Spring añade ese prefijo.

## Utilizar authorities

```java
@WithMockUser(
    username = "ana",
    authorities = {"usuario:leer"}
)
```

Esta configuración simula la autoridad exacta `usuario:leer`.

Puede utilizarse con una regla como:

```java
.requestMatchers("/api/usuarios/lectura")
.hasAuthority("usuario:leer")
```

## Prueba con una autoridad personalizada

```java
@Test
@WithMockUser(authorities = {"usuario:leer"})
void usuarioConAutoridadPuedeLeer() throws Exception {
    mockMvc.perform(get("/api/usuarios/lectura"))
            .andExpect(status().isOk());
}
```

## ¿Cuál debería utilizar?

Utiliza `roles` cuando tu aplicación organiza los permisos en roles como `USER`, `ADMIN` o `MANAGER`.

Utiliza `authorities` cuando necesitas permisos concretos como `pedido:leer`, `pedido:editar` o `usuario:eliminar`.

La elección debe coincidir con las reglas que utiliza tu aplicación.

---

# 10. Probar endpoints públicos y protegidos

Una aplicación normalmente combina endpoints públicos y protegidos.

Por ejemplo:

- `/api/publico/**`: acceso público.
- `/api/usuarios/**`: requiere autenticación.
- `/api/usuarios/admin`: requiere rol `ADMIN`.

## Endpoint público

Si la configuración permite el acceso anónimo a `/api/publico/**`, la prueba puede ser:

```java
@Test
void endpointPublicoNoNecesitaAutenticacion()
        throws Exception {

    mockMvc.perform(get("/api/publico/info"))
            .andExpect(status().isOk());
}
```

Para que funcione, debe existir un controlador asociado a esa ruta y la configuración real debe permitir el acceso.

## Endpoint protegido sin autenticación

```java
@Test
void endpointProtegidoRechazaUsuarioAnonimo()
        throws Exception {

    mockMvc.perform(get("/api/usuarios/perfil"))
            .andExpect(status().isUnauthorized());
}
```

Este resultado es habitual en una API configurada como REST con HTTP Basic. Una aplicación con autenticación basada en formularios y sesiones puede redirigir al usuario al login, devolviendo un `302` en lugar de un `401`.

Por tanto, **el estado esperado depende del mecanismo de autenticación y de la configuración de Spring Security**.

## Endpoint protegido con usuario autenticado

```java
@Test
@WithMockUser(roles = {"USER"})
void endpointProtegidoPermiteUsuarioAutenticado()
        throws Exception {

    mockMvc.perform(get("/api/usuarios/perfil"))
            .andExpect(status().isOk());
}
```

Estas pruebas comprueban el comportamiento de autorización HTTP.

---

# 11. Probar autenticación mediante formulario

En aplicaciones con login mediante formulario, Spring Security normalmente gestiona el proceso de autenticación y una sesión.

La prueba debe comprobar tanto el acceso al formulario como el envío de credenciales y el comportamiento posterior.

Un ejemplo de configuración tradicional sería:

```java
@Bean
SecurityFilterChain securityFilterChain(
        HttpSecurity http) throws Exception {

    return http
            .authorizeHttpRequests(auth -> auth
                .requestMatchers("/login", "/publico/**")
                    .permitAll()
                .anyRequest()
                    .authenticated()
            )
            .formLogin(Customizer.withDefaults())
            .build();
}
```

## Simular un login

`spring-security-test` proporciona un postprocesador para enviar un formulario de usuario y contraseña:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.*;

@Test
void loginCorrectoPermiteAutenticacion()
        throws Exception {

    mockMvc.perform(post("/login")
            .with(csrf())
            .param("username", "ana")
            .param("password", "password-de-prueba"))
        .andExpect(status().is3xxRedirection());
}
```

Este ejemplo requiere una configuración de autenticación real en el contexto de prueba, con un usuario de prueba y un `PasswordEncoder` compatible con la contraseña almacenada.

No basta con escribir cualquier nombre y contraseña: Spring debe tener una fuente de usuarios y credenciales configurada.

La ruta, los parámetros y el resultado también pueden variar si la aplicación personaliza el login.

---

# 12. Probar autenticación Basic

HTTP Basic envía credenciales codificadas en Base64 en la cabecera `Authorization`. Base64 no es cifrado, por lo que HTTP Basic debe utilizarse sobre HTTPS en entornos reales.

Para las pruebas, Spring Security ofrece el postprocesador `httpBasic`.

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.httpBasic;
```

Ejemplo:

```java
@Test
void usuarioBasicValidoPuedeAcceder() throws Exception {
    mockMvc.perform(get("/api/usuarios/perfil")
            .with(httpBasic("ana", "password-de-prueba")))
        .andExpect(status().isOk());
}
```

Esta prueba necesita que `ana` exista en el `UserDetailsService` o en la fuente de usuarios utilizada por la aplicación, y que la contraseña sea válida.

## Credenciales incorrectas

```java
@Test
void usuarioBasicConPasswordIncorrectaNoPuedeAcceder()
        throws Exception {

    mockMvc.perform(get("/api/usuarios/perfil")
            .with(httpBasic("ana", "incorrecta")))
        .andExpect(status().isUnauthorized());
}
```

Estas pruebas sí pueden recorrer el proceso de autenticación Basic, a diferencia de `@WithMockUser`.

---

# 13. Probar CSRF

Spring Security activa normalmente la protección CSRF en las configuraciones web tradicionales.

Una petición POST puede necesitar un token CSRF incluso cuando el usuario está autenticado.

Para las pruebas, se utiliza:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
```

## Petición POST con CSRF

```java
@Test
@WithMockUser
void postConCsrfValidoPuedeContinuar() throws Exception {
    mockMvc.perform(post("/api/usuarios")
            .with(csrf())
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "nombre": "Ana",
                  "email": "ana@ejemplo.com"
                }
                """))
        .andExpect(status().isCreated());
}
```

El endpoint debe existir y aceptar esa petición. La prueba también necesita que el controlador, los servicios y los datos estén configurados adecuadamente.

## Petición POST sin CSRF

```java
@Test
@WithMockUser
void postSinCsrfEsRechazado() throws Exception {
    mockMvc.perform(post("/api/usuarios")
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "nombre": "Ana",
                  "email": "ana@ejemplo.com"
                }
                """))
        .andExpect(status().isForbidden());
}
```

La petición debería ser rechazada por CSRF si la protección está habilitada y no se ha configurado otra excepción para esa ruta.

## ¿Debo desactivar CSRF?

No lo hagas solamente para evitar errores en las pruebas.

En una aplicación que utiliza sesiones y cookies de navegador, CSRF puede ser importante.

En una API que utiliza exclusivamente tokens Bearer enviados explícitamente en la cabecera `Authorization`, el riesgo es diferente y puede ser apropiado desactivar CSRF según el diseño de seguridad.

La configuración debe reflejar la arquitectura real de la aplicación.

---

# 14. Probar autenticación JWT

JWT requiere especial atención porque hay dos maneras muy diferentes de probar una petición.

## Caso A: simular un usuario autenticado

Con:

```java
@WithMockUser(roles = {"USER"})
```

puedes comprobar si un usuario con un rol concreto tiene permiso para acceder.

Pero esto no prueba la autenticación JWT.

## Caso B: simular un JWT para probar la autorización

Si la aplicación utiliza el soporte de Resource Server de Spring Security, puedes utilizar el postprocesador `jwt()`.

Import:

```java
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
```

Ejemplo:

```java
@Test
void jwtSimuladoConAutoridadPuedeAcceder()
        throws Exception {

    mockMvc.perform(get("/api/usuarios/perfil")
            .with(jwt().authorities(
                new SimpleGrantedAuthority("ROLE_USER")
            )))
        .andExpect(status().isOk());
}
```

Necesitarás importar:

```java
import org.springframework.security.core.authority.SimpleGrantedAuthority;
```

Este método crea una autenticación JWT simulada en el contexto de la prueba. **No valida la firma ni la caducidad de un token JWT real.**

Es útil para probar reglas de autorización sin depender de un servidor de autorización.

## Caso C: probar un JWT real

Para comprobar la validación real del token, debes enviar una cabecera:

```text
Authorization: Bearer <token>
```

La aplicación debe tener configurado el mecanismo que valida el token, por ejemplo un `JwtDecoder` con una clave pública o un emisor de confianza.

Conviene probar escenarios como:

- Token válido.
- Token mal formado.
- Firma incorrecta.
- Token caducado.
- Emisor no permitido, si la aplicación valida el emisor.
- Audiencia incorrecta, si la aplicación valida la audiencia.
- Token válido sin la autoridad requerida.

Para no depender de un proveedor externo en cada prueba, puedes configurar claves y tokens de prueba o utilizar un `JwtDecoder` de pruebas cuando el objetivo sea aislar una parte concreta.

Si sustituyes el decodificador por un mock, ya no estás probando toda la validación criptográfica real. Debes distinguir claramente ambos objetivos.

## JWT y roles

Los claims del token no se convierten automáticamente en cualquier autoridad que invente la aplicación. El resultado depende del `JwtAuthenticationConverter` y de la configuración utilizada.

Si tu aplicación convierte un claim `roles` en autoridades, crea pruebas específicas para comprobar esa conversión.

---

# 15. Probar filtros de seguridad

Los filtros de Spring Security forman parte de la cadena que procesa las peticiones HTTP.

Pueden encargarse de:

- Extraer credenciales.
- Validar tokens.
- Crear el contexto de autenticación.
- Gestionar excepciones de seguridad.
- Aplicar reglas de autorización.

## ¿Cómo probarlos?

Si quieres comprobar la cadena de filtros real, utiliza una prueba de integración web que cargue la configuración de seguridad de la aplicación.

```java
@SpringBootTest
@AutoConfigureMockMvc
class SecurityFilterIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void endpointProtegidoRechazaPeticionAnonima()
            throws Exception {

        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isUnauthorized());
    }
}
```

El estado esperado depende de la configuración de autenticación.

No añadas `@WithMockUser` a esta prueba si precisamente quieres verificar qué ocurre cuando una petición llega sin autenticación.

## Probar un filtro personalizado

Si has desarrollado un filtro JWT propio, comprueba:

1. Qué ocurre cuando no existe la cabecera `Authorization`.
2. Qué ocurre cuando la cabecera tiene un formato incorrecto.
3. Qué ocurre cuando el token no es válido.
4. Qué ocurre cuando el token es válido.
5. Qué autoridades se añaden al contexto de seguridad.
6. Que una petición sin credenciales no se convierta accidentalmente en una petición autenticada.

Siempre que sea posible, prueba el filtro dentro de la cadena de seguridad real, además de las pruebas unitarias que pueda necesitar.

---

# 16. Pruebas de integración con `@SpringBootTest`

Para probar la configuración real de seguridad junto con controladores, servicios y repositorios, puedes usar `@SpringBootTest`.

```java
@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class SecurityIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void usuarioAnonimoNoPuedeConsultarPerfil()
            throws Exception {

        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void usuarioAutenticadoPuedeConsultarPerfil()
            throws Exception {

        mockMvc.perform(get("/api/usuarios/perfil"))
                .andExpect(status().isOk());
    }

    @Test
    @WithMockUser(roles = {"USER"})
    void usuarioNormalNoPuedeConsultarZonaAdmin()
            throws Exception {

        mockMvc.perform(get("/api/usuarios/admin"))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void administradorPuedeConsultarZonaAdmin()
            throws Exception {

        mockMvc.perform(get("/api/usuarios/admin"))
                .andExpect(status().isOk());
    }
}
```

Esta clase presupone que:

- Existe el controlador con las rutas utilizadas.
- La configuración de seguridad permite los accesos descritos.
- El contexto de Spring puede arrancar.
- Los endpoints no dependen de datos que falten en la base de datos.

Las pruebas con `@WithMockUser` verifican autorización con usuarios simulados. La prueba anónima comprueba el rechazo de una petición sin usuario.

Para verificar el login real o la validación de JWT, añade pruebas específicas para esos mecanismos.

---

# 17. Probar usuarios y contraseñas

Si la aplicación utiliza un `UserDetailsService` y un `PasswordEncoder`, conviene comprobar que las contraseñas se almacenan y verifican de forma correcta.

Ejemplo de prueba unitaria de un codificador:

```java
@Test
void passwordEncoderDebeVerificarLaPassword() {
    PasswordEncoder encoder = new BCryptPasswordEncoder();

    String password = "password-de-prueba";
    String hash = encoder.encode(password);

    assertNotEquals(password, hash);
    assertTrue(encoder.matches(password, hash));
    assertFalse(encoder.matches("incorrecta", hash));
}
```

Importaciones:

```java
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.*;
```

No compares hashes BCrypt como si tuvieran que ser idénticos: normalmente incluyen una sal aleatoria. Comprueba el resultado mediante `matches()`.

## Probar `UserDetailsService`

Si utilizas un servicio propio para cargar usuarios, prueba:

- Que encuentra un usuario existente.
- Que falla de forma adecuada cuando no existe.
- Que las autoridades son correctas.
- Que la configuración del `PasswordEncoder` es compatible con las contraseñas almacenadas.

Evita utilizar credenciales reales o contraseñas de producción en las pruebas.

---

# 18. Probar acceso denegado y errores de autenticación

Spring Security distingue normalmente entre dos situaciones.

## HTTP 401 Unauthorized

Se utiliza habitualmente cuando una petición necesita autenticación y no se han presentado credenciales válidas.

Ejemplos:

- Petición anónima a un endpoint protegido por HTTP Basic.
- Credenciales Basic incorrectas.
- Token Bearer inválido en una API configurada para responder con `401`.

## HTTP 403 Forbidden

Se utiliza habitualmente cuando la petición no está autorizada.

Ejemplos:

- Usuario autenticado sin el rol necesario.
- Usuario autenticado que no tiene una autoridad concreta.
- Petición rechazada por CSRF.

La respuesta concreta depende de la configuración de Spring Security.

## Ejemplo de autorización

```java
@Test
@WithMockUser(roles = {"USER"})
void usuarioNormalNoPuedeEliminarUsuarios()
        throws Exception {

    mockMvc.perform(delete("/api/usuarios/1"))
            .andExpect(status().isForbidden());
}
```

Este ejemplo presupone que la configuración restringe la eliminación a un rol superior y que el endpoint existe.

## Comprobar que no se ejecuta la operación

Si quieres comprobar que un usuario sin permisos no llega al servicio de eliminación, puedes combinar una prueba de controlador con un servicio simulado.

La prueba debería comprobar:

- Que la respuesta es `403`.
- Que el método de eliminación del servicio no se ejecuta.

Esto ayuda a verificar que la autorización se aplica antes de realizar la operación protegida.

---

# 19. Probar métodos protegidos con `@PreAuthorize`

Spring Security también permite proteger métodos de servicio.

Ejemplo:

```java
@Service
public class AdministracionService {

    @PreAuthorize("hasRole('ADMIN')")
    public String operacionAdministrativa() {
        return "Operación ejecutada";
    }
}
```

Para que `@PreAuthorize` funcione, la aplicación debe tener habilitada la seguridad de métodos, por ejemplo:

```java
@Configuration
@EnableMethodSecurity
public class MethodSecurityConfig {
}
```

## Prueba de método autorizado

Una prueba de integración puede comprobar que un administrador ejecuta el método:

```java
@SpringBootTest
class AdministracionServiceSecurityTest {

    @Autowired
    private AdministracionService administracionService;

    @Test
    @WithMockUser(roles = {"ADMIN"})
    void administradorPuedeEjecutarOperacion() {
        assertEquals(
            "Operación ejecutada",
            administracionService.operacionAdministrativa()
        );
    }
}
```

## Prueba de método no autorizado

```java
@Test
@WithMockUser(roles = {"USER"})
void usuarioNormalNoPuedeEjecutarOperacion() {
    assertThrows(
        AccessDeniedException.class,
        () -> administracionService.operacionAdministrativa()
    );
}
```

Import:

```java
import org.springframework.security.access.AccessDeniedException;
```

Estas pruebas deben ejecutarse a través del bean administrado por Spring para que intervenga la seguridad de métodos. Crear el servicio con `new AdministracionService()` no activa automáticamente los proxies de seguridad.

---

# 20. Problemas frecuentes

## Error: `401 Unauthorized` cuando esperaba `200`

Comprueba:

- Si el endpoint exige autenticación.
- Si has usado `@WithMockUser` cuando deseas simular un usuario.
- Si el mecanismo de autenticación está correctamente configurado.
- Si la prueba pretende verificar el login real y no solamente la autorización.

## Error: `403 Forbidden` cuando esperaba `200`

Comprueba:

- El rol del usuario.
- Si has usado `roles` o `authorities` correctamente.
- Si el endpoint necesita un token CSRF.
- Si existen otras reglas de autorización.
- Si la configuración de seguridad de la prueba coincide con la aplicación real.

Recuerda que `roles = {"ADMIN"}` crea la autoridad `ROLE_ADMIN`, mientras que `authorities = {"ADMIN"}` crea la autoridad exacta `ADMIN`.

## Error: `@WithMockUser` no funciona

Comprueba que has añadido:

```xml
<dependency>
    <groupId>org.springframework.security</groupId>
    <artifactId>spring-security-test</artifactId>
    <scope>test</scope>
</dependency>
```

Y verifica que la prueba utiliza un contexto web que incluye Spring Security.

## Error CSRF en peticiones POST, PUT o DELETE

Si CSRF está habilitado, añade:

```java
.with(csrf())
```

cuando corresponda.

No desactives CSRF sin evaluar antes la arquitectura de autenticación y el riesgo que protege.

## Error de autenticación Basic

Comprueba que:

- El usuario existe en la fuente de usuarios configurada.
- La contraseña es correcta.
- El `PasswordEncoder` coincide con el formato almacenado.
- El contexto de la prueba carga el servicio de usuarios correspondiente.

## Error JWT

Comprueba:

- La cabecera `Authorization`.
- El prefijo `Bearer`.
- La firma del token.
- La caducidad.
- El emisor y la audiencia, cuando se validen.
- El conversor de claims a autoridades.
- La configuración del decodificador.

No confundas una prueba que utiliza `.with(jwt())` con una validación criptográfica del token.

## Error: el test carga una configuración de seguridad diferente

Evita definir configuraciones permisivas en el contexto de pruebas si el objetivo es verificar las reglas reales.

Si utilizas una configuración específica para pruebas, deja claro qué reglas está comprobando y cuáles no.

---

# 21. Ejecutar las pruebas

## Maven

Ejecutar todas las pruebas:

```bash
./mvnw test
```

En Windows:

```powershell
mvnw.cmd test
```

Ejecutar una clase concreta:

```bash
./mvnw -Dtest=UsuarioControllerSecurityTest test
```

Ejecutar una prueba de integración:

```bash
./mvnw -Dtest=SecurityIntegrationTest test
```

Ejecutar la fase de verificación:

```bash
./mvnw verify
```

## Gradle

```bash
./gradlew test
```

Para ejecutar una clase concreta:

```bash
./gradlew test --tests "com.ejemplo.demo.security.SecurityIntegrationTest"
```

---

# 22. Buenas prácticas

## Pruebas unitarias

- Prueba la lógica de negocio de forma aislada.
- Utiliza Mockito para simular dependencias cuando sea necesario.
- No cargues Spring si no aporta valor a la prueba.
- Comprueba resultados y excepciones.
- No utilices contraseñas reales.

## Pruebas web

- Prueba rutas públicas y protegidas.
- Comprueba los casos de usuario anónimo, usuario normal y administrador.
- Verifica tanto `401` como `403` cuando corresponda.
- Prueba CSRF si está habilitado.
- Comprueba los cuerpos JSON y las cabeceras relevantes.

## Pruebas JWT

- Separa las pruebas de autorización de las pruebas de validación del token.
- Comprueba tokens válidos e inválidos.
- Comprueba tokens caducados.
- Comprueba la conversión de roles y autoridades.
- No dependas innecesariamente de un proveedor externo en todas las pruebas.

## Pruebas de integración

- Carga la configuración real de seguridad cuando quieras verificarla.
- Usa una base de datos aislada.
- Evita sustituir por mocks los componentes cuya integración quieras comprobar.
- Mantén las pruebas reproducibles.
- No permitas que las pruebas se conecten a sistemas de producción.

---

# 23. Resumen de anotaciones y herramientas

| Anotación o herramienta | Utilidad |
|---|---|
| `@Test` | Define una prueba de JUnit |
| `@Mock` | Simula una dependencia |
| `@InjectMocks` | Construye la clase que se prueba |
| `@SpringBootTest` | Carga el contexto de Spring Boot |
| `@AutoConfigureMockMvc` | Configura MockMvc para pruebas MVC |
| `@WebMvcTest` | Carga principalmente la capa web |
| `@DataJpaTest` | Prueba JPA y repositorios |
| `@WithMockUser` | Simula un usuario autenticado |
| `@WithAnonymousUser` | Simula un usuario anónimo |
| `@WithUserDetails` | Carga un usuario desde el `UserDetailsService` |
| `@EnableMethodSecurity` | Habilita seguridad de métodos |
| `MockMvc` | Simula peticiones HTTP |
| `spring-security-test` | Añade utilidades de testing de seguridad |
| `with(csrf())` | Añade un token CSRF de prueba |
| `with(httpBasic(...))` | Simula credenciales HTTP Basic |
| `with(jwt())` | Simula autenticación JWT en pruebas compatibles |

---

# Conclusión

Para probar correctamente una aplicación Spring Boot con Spring Security, conviene separar tres objetivos:

1. **Probar la lógica de negocio:** JUnit y Mockito.
2. **Probar las reglas de acceso HTTP:** `@WebMvcTest`, `MockMvc` y `@WithMockUser`.
3. **Probar la integración real de seguridad:** `@SpringBootTest`, `@AutoConfigureMockMvc` y pruebas específicas del mecanismo de autenticación utilizado.

`@WithMockUser` es ideal para comprobar roles y permisos, pero no sustituye las pruebas del login real ni la validación de tokens JWT.

La mejor estrategia combina pruebas rápidas y aisladas con un conjunto de pruebas de integración que verifiquen los recorridos críticos de autenticación y autorización de la aplicación.
