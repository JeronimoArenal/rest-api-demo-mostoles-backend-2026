# Spring Security JWT --- REST API

Backend REST desarrollado con Spring Boot que permite registrar
usuarios, autenticarlos con usuario y contraseña y proteger recursos
mediante JSON Web Tokens (JWT).

La aplicación utiliza Spring Security en modo **stateless**: el servidor
no mantiene una sesión HTTP para recordar al usuario entre peticiones.
Después del inicio de sesión, el cliente envía el JWT en la cabecera
`Authorization` de las peticiones protegidas.

> Esta documentación se basa en las clases compartidas. Los detalles
> exactos de dependencias, versiones, DTO y controladores de negocio
> deben confirmarse con el resto del proyecto.

## Índice

-   [Características](#características)
-   [Tecnologías](#tecnologías)
-   [Arquitectura y
    responsabilidades](#arquitectura-y-responsabilidades)
-   [Spring Security: cómo encaja
    todo](#spring-security-cómo-encaja-todo)
-   [Flujo completo de registro](#flujo-completo-de-registro)
-   [Flujo completo de login](#flujo-completo-de-login)
-   [Flujo de una petición protegida](#flujo-de-una-petición-protegida)
-   [Configuración de seguridad](#configuración-de-seguridad)
-   [Endpoints de autenticación](#endpoints-de-autenticación)
-   [Modelo de datos y roles](#modelo-de-datos-y-roles)
-   [Configuración de la aplicación](#configuración-de-la-aplicación)
-   [Respuestas y errores](#respuestas-y-errores)
-   [Recomendaciones de seguridad](#recomendaciones-de-seguridad)

## Características

-   Registro de usuarios y comprobación de username y email duplicados.
-   Codificación de contraseñas mediante BCrypt.
-   Asignación de los roles `ROLE_USER` y `ROLE_ADMIN`.
-   Autenticación con `AuthenticationManager`.
-   Generación de JWT firmado con HMAC.
-   Validación de firma y expiración del token.
-   Carga del usuario y sus autoridades desde la base de datos.
-   Protección de endpoints mediante Spring Security.
-   Respuestas JSON para determinados errores de autenticación.

## Tecnologías

Tecnología / componente       Responsabilidad
  ----------------------------- --------------------------------------------------
Spring Boot y Spring Web      Aplicación y endpoints REST.
Spring Security               Autenticación, cadena de filtros y autorización.
JWT / JJWT                    Creación, firma, lectura y validación de tokens.
BCrypt                        Hash de contraseñas.
Spring Data JPA / Hibernate   Persistencia de usuarios y roles.
MySQL                         Base de datos configurada en las propiedades.
Jakarta Validation            Validación declarativa de los datos recibidos.
Lombok                        Reducción de código repetitivo.

## Arquitectura y responsabilidades

``` text
com.example.spring_security_jwt
├── config
│   └── WebSecurityConfig.java
├── controller
│   └── AuthController.java
├── jwt
│   ├── AuthEntryPointJwt.java
│   ├── AuthTokenFilter.java
│   └── JwtUtils.java
├── model
│   ├── User.java
│   ├── Role.java
│   └── ERole.java
├── repository
│   ├── UserRepository.java
│   └── RoleRepository.java
├── service
│   ├── UserDetailsImpl.java
│   └── UserDetailsServiceImpl.java
└── payload
    ├── request
    │   ├── LoginRequest.java
    │   └── SignupRequest.java
    └── response
        ├── JwtResponse.java
        └── MessageResponse.java
```

La estructura refleja las clases compartidas y los nombres de los DTO y
repositorios importados. Ajusta el árbol si los archivos reales están
organizados de otra forma.

  -----------------------------------------------------------------------
Clase                               Función
  ----------------------------------- -----------------------------------
`WebSecurityConfig`                 Define la cadena de filtros, las
rutas públicas/protegidas, el
gestor de autenticación, el
codificador de contraseñas y el
filtro JWT.

`AuthController`                    Expone registro, login y endpoint
de prueba.

`AuthTokenFilter`                   Lee y valida el JWT de cada
petición y establece el contexto de
autenticación si es válido.

`JwtUtils`                          Genera tokens, valida su
firma/validez y extrae el sujeto.

`AuthEntryPointJwt`                 Construye una respuesta JSON
`401 Unauthorized` cuando Spring
Security invoca el punto de entrada
de autenticación.

`UserDetailsServiceImpl`            Busca usuarios por username en
`UserRepository`.

`UserDetailsImpl`                   Adapta `User` a `UserDetails` y
convierte los roles en autoridades.

`User`                              Entidad del usuario.

`Role` / `ERole`                    Entidad y enumeración de roles.

`UserRepository` / `RoleRepository` Acceso a los usuarios y roles
persistidos.

`LoginRequest` / `SignupRequest`    DTO de entrada.

`JwtResponse` / `MessageResponse`   DTO de respuesta.
-----------------------------------------------------------------------

## Spring Security: cómo encaja todo

Spring Security procesa las peticiones HTTP mediante una **cadena de
filtros** (`SecurityFilterChain`). Los filtros realizan tareas antes de
que la petición llegue al controlador: pueden identificar al usuario,
establecer su autenticación y aplicar las reglas de acceso.

En este proyecto, `WebSecurityConfig` registra `AuthTokenFilter` antes
de `UsernamePasswordAuthenticationFilter`. Esto permite que el filtro
JWT examine la cabecera de la petición antes de que continúe el
procesamiento habitual de Spring Security.

### Diagrama didáctico: una petición HTTP

``` mermaid
flowchart TD
    A[Cliente HTTP] --> B[Servlet container]
    B --> C[Spring Security: FilterChainProxy]
    C --> D[AuthTokenFilter]
    D --> E{¿Hay Bearer JWT válido?}
    E -->|Sí| F[JwtUtils valida y extrae username]
    F --> G[UserDetailsServiceImpl carga usuario y roles]
    G --> H[SecurityContextHolder guarda Authentication]
    E -->|No / inválido| I[Continúa sin autenticación creada por el JWT]
    H --> J[Reglas authorizeHttpRequests]
    I --> J
    J --> K{¿Acceso permitido?}
    K -->|Sí| L[Controlador REST]
    K -->|No: requiere login| M[AuthEntryPointJwt: 401]
    K -->|No: faltan permisos| N[403 Forbidden]
```

**Cómo leerlo:**

1.  La petición entra en la cadena de filtros de Spring Security.
2.  `AuthTokenFilter` intenta extraer y validar el JWT.
3.  Si el token es válido, el filtro carga al usuario y guarda un objeto
    `Authentication` en `SecurityContextHolder`.
4.  Spring Security evalúa las reglas de autorización.
5.  Si se permite el acceso, la petición llega al controlador. Si falta
    autenticación, normalmente se responde con `401`; si el usuario está
    autenticado pero carece de permisos, normalmente se responde con
    `403`.

Un token ausente o inválido no significa que el filtro autorice el
acceso: significa que el filtro no establece una autenticación JWT. Las
reglas posteriores deciden si la petición puede continuar.

### Componentes clave de Spring Security

-   **`SecurityFilterChain`**: declara cómo se procesa la seguridad
    HTTP.
-   **`AuthenticationManager`**: coordina la autenticación de las
    credenciales del login.
-   **`PasswordEncoder`**: compara contraseñas mediante un algoritmo de
    hash; aquí se usa BCrypt.
-   **`UserDetailsService`**: proporciona a Spring Security los datos
    del usuario.
-   **`Authentication`**: representa el resultado de una autenticación y
    las autoridades asociadas.
-   **`SecurityContextHolder`**: expone el contexto de seguridad durante
    el procesamiento de la petición actual.
-   **`AuthenticationEntryPoint`**: define cómo responder cuando se
    requiere autenticación y esta falta.
-   **`@EnableMethodSecurity`**: habilita la seguridad a nivel de
    método; las restricciones solo se aplican donde se configuren
    anotaciones como `@PreAuthorize`.

## Flujo completo de registro

**Endpoint:** `POST /api/auth/signup`

El método `AuthController.registerUser()` ejecuta este proceso:

1.  Recibe `SignupRequest`.
2.  Comprueba si ya existe el username mediante `existsByUsername()`. Si
    existe, devuelve `400 Bad Request`.
3.  Comprueba si ya existe el email mediante `existsByEmail()`. Si
    existe, devuelve `400 Bad Request`.
4.  Codifica la contraseña con `PasswordEncoder`, configurado como
    `BCryptPasswordEncoder`.
5.  Construye la entidad `User`.
6.  Resuelve los roles solicitados mediante `RoleRepository`.
    -   Si no se especifica ningún rol, asigna `ROLE_USER`.
    -   Si el cliente envía `"admin"`, el código asigna `ROLE_ADMIN`.
    -   Para cualquier otro valor, asigna `ROLE_USER`.
7.  Guarda el usuario con `userRepository.save()`.
8.  Devuelve un `MessageResponse`.

### Diagrama de registro

``` mermaid
flowchart TD
    A[POST /api/auth/signup] --> B[Validar SignupRequest]
    B --> C{¿Username ya existe?}
    C -->|Sí| X[400 Bad Request]
    C -->|No| D{¿Email ya existe?}
    D -->|Sí| Y[400 Bad Request]
    D -->|No| E[Codificar contraseña con BCrypt]
    E --> F[Resolver rol en RoleRepository]
    F --> G[Guardar User]
    G --> H[200 OK: MessageResponse]
```

### Ejemplo de petición

``` http
POST /api/auth/signup
Content-Type: application/json
```

``` json
{
  "username": "ana",
  "email": "ana@example.com",
  "password": "UnaPasswordSegura123!"
}
```

Este ejemplo presupone que `SignupRequest` utiliza esos nombres de
campo; las validaciones exactas dependen de su implementación.

Respuesta satisfactoria aproximada:

``` json
{
  "message": "Usuario registrado exitosamente."
}
```

La estructura exacta depende de `MessageResponse`.

> **Riesgo importante:** el registro público acepta el valor `"admin"` y
> asigna `ROLE_ADMIN`. Así, un usuario podría solicitar privilegios
> administrativos durante el registro. En producción, el rol
> privilegiado debe asignarse mediante una política controlada por el
> servidor, nunca confiarse al valor enviado por un cliente anónimo.

## Flujo completo de login

**Endpoint:** `POST /api/auth/signin`

`AuthController.authenticateUser()` realiza los siguientes pasos:

1.  Recibe `LoginRequest` con username y contraseña.
2.  Crea un `UsernamePasswordAuthenticationToken` con las credenciales.
3.  Invoca `AuthenticationManager.authenticate()`.
4.  Spring Security valida las credenciales usando el mecanismo
    configurado, que normalmente consulta `UserDetailsServiceImpl` y
    compara la contraseña mediante el `PasswordEncoder`.
5.  Si la autenticación tiene éxito, el controlador guarda el objeto
    `Authentication` en `SecurityContextHolder`.
6.  `JwtUtils.generateJwtToken()` crea y firma el JWT.
7.  El controlador obtiene `UserDetailsImpl` del principal y recopila
    las autoridades.
8.  Devuelve `JwtResponse` con token, ID, username, email y roles.

### Diagrama de secuencia del login

``` mermaid
sequenceDiagram
    participant C as Cliente
    participant AC as AuthController
    participant AM as AuthenticationManager
    participant UDS as UserDetailsServiceImpl
    participant DB as UserRepository
    participant PE as PasswordEncoder
    participant JWT as JwtUtils

    C->>AC: POST /api/auth/signin (username, password)
    AC->>AM: authenticate(credentials)
    AM->>UDS: loadUserByUsername(username)
    UDS->>DB: findByUsername(username)
    DB-->>UDS: User
    UDS-->>AM: UserDetailsImpl
    AM->>PE: Verificar contraseña
    PE-->>AM: Coincide / no coincide
    alt Credenciales correctas
        AM-->>AC: Authentication autenticado
        AC->>JWT: generateJwtToken(authentication)
        JWT-->>AC: JWT firmado
        AC-->>C: JwtResponse(token, usuario, roles)
    else Credenciales incorrectas
        AM-->>AC: Excepción de autenticación
        AC-->>C: Respuesta de error según configuración
    end
```

### Ejemplo de petición

``` http
POST /api/auth/signin
Content-Type: application/json
```

``` json
{
  "username": "ana",
  "password": "UnaPasswordSegura123!"
}
```

Los campos deben coincidir con los definidos en `LoginRequest`.

### Claims del JWT

El método `JwtUtils.generateJwtToken()` establece:

-   `sub`: nombre de usuario.
-   `iat`: fecha de emisión.
-   `exp`: fecha de expiración.
-   Firma HMAC generada a partir de la clave configurada.

El código compartido no añade los roles como claims del JWT. En las
peticiones protegidas, los roles se recuperan de la base de datos.

## Flujo de una petición protegida

Una vez autenticado, el cliente envía el JWT en cada solicitud que
necesite autenticación:

``` http
GET /api/recurso-protegido
Authorization: Bearer <JWT>
```

### Secuencia de validación del token

``` mermaid
sequenceDiagram
    participant C as Cliente
    participant F as AuthTokenFilter
    participant J as JwtUtils
    participant U as UserDetailsServiceImpl
    participant R as UserRepository
    participant S as Spring Security
    participant API as Controlador

    C->>F: Petición con Authorization: Bearer JWT
    F->>J: validateJwtToken(token)
    J-->>F: Válido / inválido

    alt JWT válido
        F->>J: getUsernameFromJwtToken(token)
        J-->>F: username
        F->>U: loadUserByUsername(username)
        U->>R: findByUsername(username)
        R-->>U: User
        U-->>F: UserDetailsImpl + autoridades
        F->>S: Guardar Authentication en SecurityContextHolder
    else JWT ausente o inválido
        F->>F: No establece autenticación JWT
    end

    F->>S: Continuar FilterChain
    S->>S: Evaluar reglas de acceso
    alt Acceso permitido
        S->>API: Ejecutar petición
        API-->>C: Respuesta del recurso
    else Falta autenticación
        S-->>C: 401 Unauthorized
    else Faltan permisos
        S-->>C: 403 Forbidden
    end
```

### Responsabilidad de cada paso

1.  `AuthTokenFilter` intercepta la petición.
2.  `parseJwt()` lee `Authorization` y elimina el prefijo `Bearer`.
3.  `JwtUtils.validateJwtToken()` analiza el JWT y verifica su firma y
    validez temporal.
4.  `JwtUtils.getUsernameFromJwtToken()` extrae el sujeto `sub`.
5.  `UserDetailsServiceImpl.loadUserByUsername()` consulta la base de
    datos.
6.  `UserDetailsImpl.build()` convierte los roles en
    `SimpleGrantedAuthority`.
7.  `AuthTokenFilter` crea un `UsernamePasswordAuthenticationToken` con
    el usuario y sus autoridades.
8.  `SecurityContextHolder` mantiene la autenticación para el
    procesamiento de la petición.
9.  Spring Security aplica las reglas configuradas y permite o rechaza
    el acceso.

`AuthTokenFilter` captura excepciones y continúa la cadena de filtros.
Por eso, un JWT inválido no establece la autenticación, pero la
respuesta final depende de las reglas y del manejo de excepciones
configurado.

## Configuración de seguridad

`WebSecurityConfig` es la configuración central:

-   `@Configuration`: registra la clase como configuración de Spring.
-   `@EnableMethodSecurity`: habilita las comprobaciones de seguridad a
    nivel de método.
-   `@RequiredArgsConstructor`: genera el constructor para las
    dependencias `final`.
-   `SecurityFilterChain`: configura la seguridad HTTP.
-   `AuthenticationManager`: se obtiene de
    `AuthenticationConfiguration`.
-   `BCryptPasswordEncoder`: codifica y verifica contraseñas.
-   `AuthTokenFilter`: se añade antes de
    `UsernamePasswordAuthenticationFilter`.

La política de acceso definida es:

``` java
.authorizeHttpRequests(auth -> auth
    .requestMatchers("/api/auth/**").permitAll()
    .anyRequest().authenticated()
)
```

Esto significa que todas las rutas bajo `/api/auth/**` son públicas a
nivel de reglas HTTP y el resto requiere autenticación. Por tanto,
`/api/auth/test`, `/api/auth/signup` y `/api/auth/signin` no requieren
JWT previo.

`SessionCreationPolicy.STATELESS` indica que Spring Security no debe
crear ni utilizar una sesión HTTP para mantener el contexto de seguridad
entre peticiones.

### CSRF

La configuración deshabilita CSRF. Puede ser apropiado para una API que
utiliza tokens Bearer enviados explícitamente en cabeceras y no depende
de cookies de autenticación enviadas automáticamente por el navegador.
Si se utilizan cookies o sesiones, esta decisión debe revisarse.

## Endpoints de autenticación

  --------------------------------------------------------------------------
Método            Ruta                 Acceso            Función
configurado
  ----------------- -------------------- ----------------- -----------------
`GET`             `/api/auth/test`     Público           Comprueba que el
controlador
responde.

`POST`            `/api/auth/signup`   Público           Registra un
usuario.

`POST`            `/api/auth/signin`   Público           Autentica
credenciales y
devuelve un JWT.
  --------------------------------------------------------------------------

El resto de los endpoints requieren autenticación según la configuración
compartida, salvo que existan otras reglas o configuraciones
adicionales.

## Modelo de datos y roles

### `User`

Se almacena en la tabla `security_users` y contiene:

-   `id`: identificador generado por la base de datos.
-   `username`: obligatorio, único y de entre 3 y 30 caracteres.
-   `email`: obligatorio, único y anotado con `@Email`.
-   `password`: contraseña codificada.
-   `roles`: conjunto de roles asociado mediante una relación
    muchos-a-muchos.

La tabla intermedia de la relación se llama `security_user_roles`.

### `Role` y `ERole`

La entidad `Role` se almacena en `security_roles`. El enum `ERole`
define:

-   `ROLE_USER`
-   `ROLE_ADMIN`

`UserDetailsImpl.build()` convierte cada rol en
`SimpleGrantedAuthority`. Por ejemplo, una autoridad `ROLE_ADMIN` se
puede comprobar con `hasRole("ADMIN")`, mientras que
`hasAuthority("ROLE_ADMIN")` requiere el nombre completo.

La existencia de un rol en el modelo no protege automáticamente una
ruta: deben definirse las reglas correspondientes en
`authorizeHttpRequests` o en anotaciones como `@PreAuthorize`.

## Configuración de la aplicación

### MySQL

Ejemplo de configuración con credenciales externalizadas:

``` properties
spring.datasource.url=jdbc:mysql://<HOST>:3306/<DATABASE>?createDatabaseIfNotExist=true&serverTimezone=UTC
spring.datasource.username=${DB_MYSQL_USERNAME}
spring.datasource.password=${DB_MYSQL_PASSWORD}

spring.jpa.database-platform=org.hibernate.dialect.MySQLDialect
spring.jpa.hibernate.ddl-auto=update
```

Antes de arrancar la aplicación, configura `DB_MYSQL_USERNAME` y
`DB_MYSQL_PASSWORD` en el entorno.

`spring.jpa.hibernate.ddl-auto=update` permite a Hibernate actualizar el
esquema durante el desarrollo, pero para producción suele ser preferible
gestionar los cambios de esquema con migraciones versionadas.

### JWT

``` properties
demo.app.jwtSecret=${JWT_SECRET_BASE64}
demo.app.jwtExpirationMs=86400000
```

`jwtExpirationMs` se expresa en milisegundos; `86400000` equivale a 24
horas.

`JWT_SECRET_BASE64` debe contener una clave Base64 adecuada para el
algoritmo HMAC empleado. No incluyas la clave real en el repositorio. Si
una clave real se ha publicado, debe considerarse comprometida y
rotarse.

### Logging SQL

La configuración compartida activa `spring.jpa.show-sql` y el logging
SQL y de parámetros de Hibernate. Son opciones útiles para desarrollo,
pero pueden generar mucho volumen y revelar datos sensibles. Revísalas
antes de producción.

### Carga de archivos

Las propiedades `spring.servlet.multipart.max-file-size=10MB` y
`spring.servlet.multipart.max-request-size=10MB` limitan las peticiones
multipart. Su presencia no implica por sí sola que existan endpoints de
carga o descarga implementados.

## Respuestas y errores

  -----------------------------------------------------------------------
Situación                           Resultado esperado
  ----------------------------------- -----------------------------------
Registro correcto                   `200 OK` con `MessageResponse`.

Username duplicado                  `400 Bad Request`.

Email duplicado                     `400 Bad Request`.

Credenciales incorrectas            Excepción de autenticación;
confirmar el estado final mediante
pruebas.

Recurso protegido sin autenticación Normalmente `401 Unauthorized`.

Usuario autenticado sin permisos    Normalmente `403 Forbidden`.

JWT inválido o expirado             No se establece autenticación JWT;
el recurso protegido normalmente
será rechazado.
  -----------------------------------------------------------------------

`AuthEntryPointJwt` devuelve JSON con los campos `status`, `error`,
`message` y `path`. Para producción, conviene evitar exponer detalles
internos de excepciones y generar JSON mediante una librería en vez de
concatenar cadenas manualmente.

## Recomendaciones de seguridad

1.  **Restringir el alta de administradores.** No confiar en un rol
    privilegiado enviado por un cliente anónimo.
2.  **Externalizar secretos.** Utilizar variables de entorno o un gestor
    de secretos para la clave JWT y las credenciales de base de datos.
3.  **Usar HTTPS.** Proteger credenciales y tokens durante el
    transporte.
4.  **Revisar la expiración y revocación.** Un JWT válido puede
    reutilizarse hasta su expiración si no hay un mecanismo adicional de
    revocación.
5.  **Mejorar el logging.** Sustituir `System.out.println` por logging
    controlado y no registrar secretos ni tokens.
6.  **Revisar CSRF.** La desactivación depende de cómo se transporten
    las credenciales.
7.  **Validar DTO y errores.** El controlador recibe `BindingResult`,
    pero no lo comprueba explícitamente en el código compartido.
    Verifica cómo se gestionan los errores de validación.
8.  **Gestionar duplicados en la base de datos.** Las comprobaciones
    previas de username/email deben complementarse con restricciones
    únicas y tratamiento de conflictos.
9.  **Aplicar permisos explícitos.** `@EnableMethodSecurity` habilita la
    seguridad de método, pero no impone roles automáticamente.
10. **Evaluar consultas por petición.** El filtro recarga el usuario y
    sus roles en cada petición autenticada. Esto refleja los roles
    actuales, pero añade consultas a la base de datos.
11. **Revisar configuración no utilizada.** `app.security.enabled`
    aparece en las propiedades compartidas, pero no se observa su uso en
    las clases mostradas.
12. **Añadir pruebas de integración.** Cubrir registro, login correcto e
    incorrecto, token expirado, firma inválida, usuario inexistente y
    permisos insuficientes.

## Alcance respecto a OAuth 2.0 y OpenID Connect

Esta implementación utiliza JWT propio y Spring Security para
autenticación y autorización. El código compartido no demuestra que
implemente OAuth 2.0, OpenID Connect ni un servidor de autorización con
Spring Authorization Server.

Si el proyecto necesitara inicio de sesión federado, SSO o emitir tokens
para múltiples clientes conforme a esos estándares, habría que diseñar e
implementar los roles y endpoints de protocolo correspondientes.