# Guía de testing en Java con Spring Boot y Arquitectura Hexagonal

## 1. ¿Qué tipos de test existen?

En una aplicación Spring Boot no todos los tests deben arrancar el servidor ni conectarse a una base de datos. Cada tipo de test tiene una responsabilidad diferente.

| Tipo de test | ¿Qué comprueba? | ¿Arranca Spring? | ¿Usa base de datos? |
|---|---|---|---|
| Unitario | Una clase o método aislado | No | No |
| De integración | La colaboración entre varias partes | Normalmente sí | Puede |
| De controlador | Endpoints HTTP, validaciones y respuestas | Solo la capa web | Normalmente no |
| De repositorio | Consultas y persistencia | Sí, parcialmente | Sí |
| End-to-end (E2E) | Un flujo completo desde el exterior | Sí | Habitualmente sí |
| De arquitectura | Reglas de diseño y dependencias entre capas | No necesariamente | No |

**Regla general:** utiliza muchos tests unitarios, tests de integración para las conexiones entre componentes y unos pocos E2E para verificar los flujos críticos.

---

## 2. Dependencias de testing

En un proyecto Maven con Spring Boot, empieza por comprobar si tienes esta dependencia en tu `pom.xml`:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Incluye las herramientas habituales, como JUnit, Mockito, AssertJ y el soporte de testing de Spring.

Si quieres probar una base de datos MySQL real en tests de integración, puedes utilizar Testcontainers. La configuración exacta depende de la versión de Spring Boot y de las dependencias que ya tenga tu proyecto.

Para comprobar que Maven ejecuta los tests:

```bash
mvn test
```

Para ejecutar una clase concreta:

```bash
mvn -Dtest=UserServiceTest test
```

Los tests deben estar normalmente en:

```text
src/
├── main/
│   └── java/
└── test/
    └── java/
```

Usa en `src/test/java` el mismo paquete base que en producción para facilitar la organización y el acceso a las clases.

---

# 3. Tests unitarios

## ¿Qué son?

Prueban una clase de forma aislada, sin levantar el contexto de Spring ni utilizar servicios externos reales.

Son ideales para probar:

- Reglas de negocio.
- Validaciones del dominio.
- Servicios de aplicación.
- Cálculos y transformaciones.
- Casos de éxito y de error.

En arquitectura hexagonal, los tests unitarios son especialmente útiles para el dominio y la capa de aplicación.

## Ejemplo: probar el dominio

Imagina que tienes este modelo:

```java
package com.example.user.domain.model;

public class User {

    private final Long id;
    private final String username;

    public User(Long id, String username) {
        if (username == null || username.isBlank()) {
            throw new IllegalArgumentException(
                "El nombre de usuario es obligatorio"
            );
        }

        this.id = id;
        this.username = username;
    }

    public Long getId() {
        return id;
    }

    public String getUsername() {
        return username;
    }
}
```

El test podría ser:

```java
package com.example.user.domain.model;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;

class UserTest {

    @Test
    void shouldCreateUserWithValidUsername() {
        User user = new User(1L, "ana");

        assertEquals(1L, user.getId());
        assertEquals("ana", user.getUsername());
    }

    @Test
    void shouldRejectBlankUsername() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new User(1L, " ")
        );
    }

    @Test
    void shouldRejectNullUsername() {
        assertThrows(
            IllegalArgumentException.class,
            () -> new User(1L, null)
        );
    }
}
```

Aquí no necesitamos `@SpringBootTest`, Mockito ni MySQL. Solo queremos verificar el comportamiento de `User`.

## ¿Qué deberías probar?

Por cada regla de negocio importante, contempla:

1. El caso válido.
2. Los valores límite.
3. Los datos inválidos.
4. Las excepciones esperadas.

No es necesario crear un test por cada getter trivial. Prioriza el comportamiento que podría fallar y causar un problema real.

---

# 4. Tests unitarios de servicios con Mockito

En arquitectura hexagonal, el servicio de aplicación coordina los casos de uso y utiliza puertos de salida para acceder a otras capacidades, como la persistencia.

Por ejemplo, imagina que tienes este puerto:

```java
package com.example.user.domain.port.out;

import com.example.user.domain.model.User;

import java.util.Optional;

public interface UserRepositoryPort {

    User save(User user);

    Optional<User> findById(Long id);
}
```

Y este servicio de aplicación:

```java
package com.example.user.application.service;

import com.example.user.domain.model.User;
import com.example.user.domain.port.out.UserRepositoryPort;

public class UserService {

    private final UserRepositoryPort userRepository;

    public UserService(UserRepositoryPort userRepository) {
        this.userRepository = userRepository;
    }

    public User createUser(String username) {
        User user = new User(null, username);
        return userRepository.save(user);
    }
}
```

Para probar el servicio, simulamos el puerto con Mockito.

```java
package com.example.user.application.service;

import com.example.user.domain.model.User;
import com.example.user.domain.port.out.UserRepositoryPort;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class UserServiceTest {

    private UserRepositoryPort userRepository;
    private UserService userService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepositoryPort.class);
        userService = new UserService(userRepository);
    }

    @Test
    void shouldCreateUser() {
        User savedUser = new User(1L, "ana");

        when(userRepository.save(any(User.class)))
            .thenReturn(savedUser);

        User result = userService.createUser("ana");

        assertEquals(1L, result.getId());
        assertEquals("ana", result.getUsername());

        verify(userRepository).save(any(User.class));
    }
}
```

### ¿Qué estamos haciendo?

- `mock(...)`: crea un doble de prueba del puerto.
- `when(...).thenReturn(...)`: define lo que devuelve el doble.
- `verify(...)`: comprueba que el servicio invoca al puerto.
- `assertEquals(...)`: comprueba el resultado.

Este test no necesita una base de datos. Estamos comprobando el comportamiento del servicio, no si JPA guarda correctamente.

**Importante:** el ejemplo supone que `UserService` no tiene `@Service` ni necesita el contexto de Spring. Si decides anotarlo con `@Service`, puedes seguir construyéndolo directamente en el test mediante su constructor, siempre que sus dependencias lo permitan.

---

# 5. Tests de integración de la aplicación

## ¿Qué son?

Comprueban que varios componentes funcionan correctamente juntos. Por ejemplo:

- Spring crea los beans.
- La inyección de dependencias funciona.
- El servicio utiliza el adaptador correcto.
- La configuración de la aplicación es válida.

La anotación habitual es `@SpringBootTest`.

```java
package com.example.user;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class ApplicationIntegrationTest {

    @Test
    void contextLoads() {
    }
}
```

Este test comprueba que el contexto de Spring puede arrancar.

### ¿Es suficiente?

No. Es una primera comprobación útil, pero no verifica que el sistema guarde usuarios, responda correctamente a las peticiones o gestione los errores esperados.

Añade tests de integración con un propósito concreto y comprueba resultados observables.

Si el contexto requiere MySQL, tendrás que proporcionar una base de datos de test o configurar un entorno de pruebas adecuado. No ejecutes estos tests contra tu base de datos de producción.

---

# 6. Tests de controladores REST con MockMvc

## ¿Qué comprueban?

Prueban la capa HTTP:

- Las rutas.
- Los códigos de estado.
- Los cuerpos JSON.
- La validación de los datos recibidos.
- La transformación de excepciones en respuestas HTTP.

Para esto suele ser suficiente `@WebMvcTest`, que carga una parte de Spring centrada en MVC, en vez de levantar toda la aplicación.

Supongamos que tienes un controlador como este:

```java
@RestController
@RequestMapping("/users")
public class UserController {

    private final CreateUserUseCase createUserUseCase;

    public UserController(CreateUserUseCase createUserUseCase) {
        this.createUserUseCase = createUserUseCase;
    }

    @PostMapping
    public ResponseEntity<UserResponse> create(
            @Valid @RequestBody UserRequest request) {

        User user = createUserUseCase.createUser(
            request.username()
        );

        UserResponse response = new UserResponse(
            user.getId(),
            user.getUsername()
        );

        return ResponseEntity
            .status(HttpStatus.CREATED)
            .body(response);
    }
}
```

El ejemplo presupone que `UserRequest` y `UserResponse` son records, y que `CreateUserUseCase` expone el método `createUser(String)`.

Un test de controlador podría ser:

```java
@WebMvcTest(UserController.class)
class UserControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private CreateUserUseCase createUserUseCase;

    @Test
    void shouldCreateUser() throws Exception {
        when(createUserUseCase.createUser("ana"))
            .thenReturn(new User(1L, "ana"));

        mockMvc.perform(
                post("/users")
                    .contentType(MediaType.APPLICATION_JSON)
                    .content("""
                        {
                          "username": "ana"
                        }
                        """)
            )
            .andExpect(status().isCreated())
            .andExpect(jsonPath("$.id").value(1))
            .andExpect(jsonPath("$.username").value("ana"));
    }
}
```

Imports estáticos necesarios:

```java
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
```

Otros imports habituales:

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
```

`@MockitoBean` está disponible en versiones recientes de Spring Framework. En proyectos anteriores puedes encontrar ejemplos que utilizan `@MockBean`; comprueba cuál corresponde a tu versión.

Este test simula el caso de uso: **no comprueba la base de datos ni la lógica interna del servicio**.

También conviene probar:

- Un JSON inválido.
- Campos obligatorios ausentes.
- Respuestas `400 Bad Request`.
- Recursos inexistentes y respuestas `404 Not Found`.
- Respuestas `401` o `403` si la aplicación utiliza Spring Security.

La respuesta concreta depende de cómo hayas configurado validaciones, seguridad y manejo de excepciones.

---

# 7. Tests de repositorios JPA

## ¿Qué comprueban?

Verifican la persistencia real:

- Que una entidad se guarde.
- Que se recupere correctamente.
- Que las consultas personalizadas funcionen.
- Que las relaciones y restricciones estén bien configuradas.

La anotación habitual es `@DataJpaTest`. Carga los componentes relacionados con JPA y, por defecto, suele configurar una base de datos embebida si existe una disponible.

Ejemplo con un repositorio Spring Data:

```java
public interface SpringDataUserRepository
        extends JpaRepository<UserJpaEntity, Long> {

    Optional<UserJpaEntity> findByUsername(String username);
}
```

Test:

```java
@DataJpaTest
class SpringDataUserRepositoryTest {

    @Autowired
    private SpringDataUserRepository repository;

    @Test
    void shouldSaveAndFindUser() {
        UserJpaEntity entity = new UserJpaEntity();
        entity.setUsername("ana");

        UserJpaEntity saved = repository.save(entity);

        Optional<UserJpaEntity> result =
            repository.findById(saved.getId());

        assertTrue(result.isPresent());
        assertEquals(
            "ana",
            result.orElseThrow().getUsername()
        );
    }
}
```

Adapta el constructor o los setters a la implementación real de `UserJpaEntity`.

### ¿Y si utilizo MySQL?

Una base de datos embebida no siempre se comporta igual que MySQL. Si tus consultas dependen de características específicas de MySQL, es recomendable utilizar una base de datos de test del mismo motor.

Puedes hacerlo mediante Testcontainers, que ejecuta una instancia temporal de MySQL para las pruebas. Requiere Docker y las dependencias de test adecuadas.

No confundas un test que funciona con H2 con una garantía de que todas las consultas funcionarán en MySQL.

---

# 8. Tests del adaptador de persistencia en arquitectura hexagonal

En una arquitectura hexagonal, el servicio de aplicación no debería depender directamente de `JpaRepository`. Depende de un puerto, por ejemplo `UserRepositoryPort`, y un adaptador implementa ese puerto utilizando Spring Data JPA.

```text
UserService
    |
    v
UserRepositoryPort
    |
    v
UserPersistenceAdapter
    |
    v
SpringDataUserRepository
    |
    v
Base de datos
```

Hay dos pruebas diferentes que conviene realizar.

### A. Test unitario del servicio

Simula `UserRepositoryPort`, como en el apartado 4.

Comprueba que el servicio ejecuta correctamente las reglas de negocio y llama al puerto cuando corresponde.

### B. Test de integración del adaptador

Utiliza el adaptador real y una base de datos de prueba. Comprueba que:

1. El modelo de dominio se convierte en una entidad JPA.
2. La entidad se guarda.
3. El identificador generado se devuelve al dominio.
4. La búsqueda transforma la entidad recuperada en el modelo de dominio.

Puedes utilizar `@DataJpaTest` y configurar explícitamente el adaptador y el mapper, o utilizar `@SpringBootTest` si necesitas el cableado completo de la aplicación.

No simules el repositorio en esta segunda prueba: precisamente quieres verificar la integración real entre el adaptador, MapStruct, JPA y la base de datos.

---

# 9. Tests de MapStruct

MapStruct genera las implementaciones de los mappers durante la compilación. Si utilizas `componentModel = "spring"`, puedes probar el mapper como un bean de Spring o, si no depende de otros beans, instanciar su implementación generada directamente.

Por ejemplo:

```java
@Mapper(componentModel = "spring")
public interface UserMapper {

    @Mapping(source = "username", target = "name")
    UserResponse toResponse(User user);
}
```

Un test con JUnit y Spring podría ser:

```java
@SpringBootTest
class UserMapperTest {

    @Autowired
    private UserMapper userMapper;

    @Test
    void shouldMapUserToResponse() {
        User user = new User(1L, "ana");

        UserResponse response = userMapper.toResponse(user);

        assertEquals(1L, response.id());
        assertEquals("ana", response.name());
    }
}
```

Si el mapper no necesita dependencias de Spring, este test también puede escribirse como un test unitario cargando `UserMapperImpl`, la clase generada por MapStruct. Esto exige que el procesamiento de anotaciones haya generado esa implementación.

Prueba especialmente:

- Campos con nombres distintos.
- Formatos de fecha.
- Valores nulos.
- Campos ignorados.
- Métodos personalizados con `@Named`.
- Enmascaramiento de teléfonos u otros datos.

Recuerda que un test de MapStruct verifica la conversión de datos, no que el servicio aplique correctamente las reglas de negocio.

---

# 10. Tests end-to-end (E2E)

## ¿Qué son?

Verifican un flujo completo desde el punto de vista de un cliente. Por ejemplo:

1. Enviar `POST /users`.
2. Comprobar que la API responde `201 Created`.
3. Extraer el identificador devuelto.
4. Consultar `GET /users/{id}`.
5. Verificar que el usuario recuperado contiene los datos esperados.

Un E2E debe atravesar las capas que quieres validar de verdad, incluidos el controlador, el servicio, el adaptador y la base de datos si forman parte del flujo.

Puedes arrancar la aplicación en un puerto aleatorio con `@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)` y enviar peticiones HTTP reales desde el test.

Estos tests son más lentos y requieren más configuración. Reserva su uso para los flujos críticos y no para probar cada pequeña regla de negocio.

---

# 11. Tests de arquitectura

Estos tests comprueban que el código respeta las reglas de diseño.

Por ejemplo, en arquitectura hexagonal puedes querer garantizar que:

- El dominio no depende de Spring.
- El dominio no importa clases JPA.
- Los controladores no acceden directamente a los repositorios de infraestructura.
- La aplicación depende de puertos y no de adaptadores concretos.

Se pueden escribir comprobaciones con herramientas de análisis de arquitectura, como ArchUnit, añadiendo su dependencia de test compatible con tu proyecto.

Una regla conceptual sería:

```text
domain
  NO debe depender de
application, infrastructure o web
```

Esto es importante porque una aplicación puede tener todos sus tests funcionales en verde y, aun así, terminar con dependencias que rompen su arquitectura.

---

# 12. ¿Qué debería probar en cada capa hexagonal?

| Capa | Test recomendado | ¿Qué verificar? |
|---|---|---|
| Dominio | Unitario | Reglas, invariantes y excepciones |
| Aplicación | Unitario con Mockito | Casos de uso y colaboración con puertos |
| Web / REST | `@WebMvcTest` | JSON, validaciones, códigos HTTP |
| Persistencia | `@DataJpaTest` | Consultas y comportamiento de JPA |
| Adaptadores | Integración | Traducción entre puertos, mappers y tecnología |
| Aplicación completa | `@SpringBootTest` | Configuración e integración de beans |
| Flujo crítico | E2E | Petición real hasta el resultado final |
| Arquitectura | ArchUnit u otra herramienta | Dependencias permitidas entre paquetes |

---

# 13. Cómo organizar los tests

Puedes mantener una estructura paralela a la aplicación:

```text
src/
├── main/
│   └── java/com/example/
│       ├── user/
│       │   ├── domain/
│       │   ├── application/
│       │   ├── adapter/
│       │   └── web/
│       └── Application.java
└── test/
    └── java/com/example/
        ├── user/
        │   ├── domain/
        │   │   └── UserTest.java
        │   ├── application/
        │   │   └── UserServiceTest.java
        │   ├── adapter/
        │   │   └── UserPersistenceAdapterTest.java
        │   └── web/
        │       └── UserControllerTest.java
        └── ApplicationIntegrationTest.java
```

No es obligatorio reproducir exactamente esta estructura. Lo importante es que resulte fácil localizar los tests de cada clase y que el nombre del test deje claro qué comportamiento verifica.

---

# 14. Cómo ejecutar los tests

Desde la raíz del proyecto:

```bash
# Ejecutar los tests habituales
mvn test

# Ejecutar una clase concreta
mvn -Dtest=UserTest test

# Ejecutar varias clases por patrón
mvn -Dtest='User*Test' test

# Ejecutar el ciclo de verificación del proyecto
mvn verify
```

`mvn verify` ejecuta las fases anteriores del ciclo Maven y puede ejecutar también verificaciones adicionales configuradas en el proyecto, como tests de integración o análisis de calidad. El comportamiento exacto depende de los plugins y perfiles de tu `pom.xml`.

---

# 15. Orden recomendado para empezar

No intentes construir todos los tipos de tests el primer día. Sigue este orden:

1. **Dominio:** prueba las reglas y validaciones sin Spring.
2. **Aplicación:** prueba los casos de uso con Mockito y puertos simulados.
3. **MapStruct:** verifica los mapeos importantes.
4. **Controladores:** prueba las rutas y respuestas HTTP.
5. **Persistencia:** comprueba los repositorios y adaptadores con una base de datos de test.
6. **Integración:** verifica que Spring arranca y conecta los componentes.
7. **E2E:** automatiza los flujos más importantes.
8. **Arquitectura:** añade reglas que eviten dependencias indebidas entre capas.

## Conclusión

La idea fundamental es que cada test responda a una pregunta concreta:

- **Unitario:** ¿funciona esta regla o clase por sí sola?
- **Controlador:** ¿responde correctamente mi API?
- **Repositorio:** ¿se persisten y recuperan bien los datos?
- **Integración:** ¿funcionan juntos los componentes?
- **E2E:** ¿puede un usuario completar el flujo real?
- **Arquitectura:** ¿sigue el código respetando el diseño previsto?

Si mantienes esta separación, tendrás tests más rápidos, fáciles de entender y menos frágiles. En una arquitectura hexagonal, prioriza los tests unitarios del dominio y de la aplicación; utiliza pruebas de integración para comprobar los adaptadores y reserva los E2E para los flujos de mayor valor.