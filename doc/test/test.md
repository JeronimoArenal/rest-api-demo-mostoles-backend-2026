# Guía completa de testing en Spring Boot: pruebas unitarias y de integración

## Índice

1. Introducción al testing
2. Diferencias entre pruebas unitarias y de integración
3. Herramientas de testing en Spring Boot
4. Dependencias Maven
5. Estructura del proyecto
6. JUnit 5
7. Mockito
8. Pruebas unitarias de servicios
9. Pruebas de repositorios con `@DataJpaTest`
10. Pruebas de controladores con `@WebMvcTest`
11. Pruebas de integración con `@SpringBootTest`
12. `@AutoConfigureMockMvc` y `MockMvc`
13. Pruebas HTTP: GET, POST, PUT y DELETE
14. Pruebas de validaciones y excepciones
15. Configuración de la base de datos de pruebas
16. Diferencias entre las principales anotaciones
17. Errores frecuentes
18. Ejecución de las pruebas
19. Buenas prácticas
20. Resumen final

---

# 1. Introducción al testing

El testing consiste en comprobar mediante pruebas que una aplicación funciona como esperamos.

En Spring Boot, las pruebas permiten verificar que:

- Los servicios ejecutan correctamente la lógica de negocio.
- Los repositorios consultan y guardan datos correctamente.
- Los controladores devuelven las respuestas HTTP esperadas.
- Las validaciones detectan datos incorrectos.
- Las excepciones se gestionan adecuadamente.
- Los diferentes componentes funcionan correctamente cuando se conectan entre sí.

El objetivo no es únicamente comprobar que la aplicación arranca, sino verificar que se comporta correctamente ante diferentes situaciones.

## Tipos principales de pruebas

### Pruebas unitarias

Comprueban una clase o unidad de código de manera aislada.

Por ejemplo, probar un servicio simulando el repositorio con Mockito.

### Pruebas de integración

Comprueban que varios componentes funcionan conjuntamente.

Por ejemplo, enviar una petición HTTP a un controlador, ejecutar el servicio real y consultar una base de datos de pruebas.

### Pruebas de extremo a extremo

Comprueban un flujo completo de la aplicación desde el punto de vista del usuario o cliente.

Por ejemplo, realizar una petición HTTP contra una aplicación desplegada y comprobar el resultado final.

---

# 2. Diferencias entre pruebas unitarias y de integración

| Característica | Prueba unitaria | Prueba de integración |
|---|---|---|
| Objetivo | Comprobar una unidad aislada | Comprobar varios componentes conectados |
| Contexto Spring | Normalmente no se carga | Habitualmente se carga |
| Mockito | Muy habitual | Opcional |
| Base de datos real | Normalmente no | Puede utilizarse |
| Velocidad | Muy rápida | Generalmente más lenta |
| Comprueba lógica de negocio | Sí | Sí |
| Comprueba configuración Spring | No normalmente | Sí |
| Comprueba persistencia real | No si se simula el repositorio | Sí, si usa una base real |
| Ejemplo | Probar `UsuarioService` | Probar controlador, servicio y repositorio juntos |

## ¿Cuándo utilizar cada una?

Utiliza pruebas unitarias para comprobar reglas de negocio:

- Calcular importes.
- Comprobar condiciones.
- Validar reglas de negocio.
- Gestionar excepciones.
- Verificar comportamientos ante dependencias simuladas.

Utiliza pruebas de integración para comprobar:

- La inyección de dependencias de Spring.
- La comunicación entre controlador y servicio.
- Las consultas de JPA.
- La persistencia en la base de datos.
- La conversión entre objetos Java y JSON.
- El funcionamiento conjunto de las capas.

Una estrategia habitual consiste en escribir muchas pruebas unitarias y complementar estas con pruebas de integración para los recorridos más importantes.

---

# 3. Herramientas de testing en Spring Boot

## JUnit 5

Es el framework utilizado para escribir y ejecutar pruebas.

Anotaciones principales:

- `@Test`: identifica un método de prueba.
- `@BeforeEach`: ejecuta una preparación antes de cada prueba.
- `@AfterEach`: ejecuta una acción después de cada prueba.
- `@DisplayName`: asigna un nombre descriptivo.
- `@ParameterizedTest`: ejecuta una prueba con diferentes datos.

Aserciones habituales:

```java
assertEquals(10, resultado);
assertNotNull(resultado);
assertNull(resultado);
assertTrue(condicion);
assertFalse(condicion);
assertThrows(MiExcepcion.class, () -> ejecutarMetodo());
```

## Mockito

Permite simular dependencias sin utilizar sus implementaciones reales.

Anotaciones y métodos habituales:

- `@Mock`: crea un objeto simulado.
- `@InjectMocks`: crea la clase que queremos probar e intenta inyectarle los mocks.
- `when(...).thenReturn(...)`: define qué devuelve una llamada.
- `thenThrow(...)`: simula una excepción.
- `verify(...)`: comprueba que se realizó una llamada.
- `never()`: comprueba que una llamada no ocurrió.
- `ArgumentCaptor`: captura los argumentos enviados a una dependencia.

## Spring Boot Test

Proporciona herramientas para probar aplicaciones Spring Boot.

Anotaciones principales:

- `@SpringBootTest`
- `@AutoConfigureMockMvc`
- `@WebMvcTest`
- `@DataJpaTest`
- `@ActiveProfiles`

## MockMvc

Permite simular peticiones HTTP a la capa MVC sin arrancar un servidor web real.

Permite comprobar:

- El código de estado HTTP.
- Las cabeceras.
- El contenido JSON.
- Las validaciones.
- Las respuestas del controlador.

---

# 4. Dependencias Maven

En un proyecto Spring Boot que utilice Maven, normalmente se incluye el starter de pruebas.

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Esta dependencia incluye herramientas habituales como JUnit Jupiter, Mockito, Spring Test y AssertJ.

Si la aplicación utiliza Spring MVC y JPA, las dependencias principales podrían ser:

```xml
<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-web</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-data-jpa</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-validation</artifactId>
</dependency>

<dependency>
    <groupId>org.springframework.boot</groupId>
    <artifactId>spring-boot-starter-test</artifactId>
    <scope>test</scope>
</dependency>
```

Para utilizar H2 como base de datos de pruebas:

```xml
<dependency>
    <groupId>com.h2database</groupId>
    <artifactId>h2</artifactId>
    <scope>test</scope>
</dependency>
```

Estas dependencias deben utilizarse con una versión de Spring Boot correctamente configurada mediante el parent o BOM del proyecto.

---

# 5. Estructura recomendada del proyecto

```text
src/
├── main/
│   ├── java/com/ejemplo/demo/
│   │   ├── DemoApplication.java
│   │   ├── controller/
│   │   │   └── UsuarioController.java
│   │   ├── service/
│   │   │   └── UsuarioService.java
│   │   ├── repository/
│   │   │   └── UsuarioRepository.java
│   │   ├── model/
│   │   │   └── Usuario.java
│   │   └── dto/
│   │       └── CrearUsuarioRequest.java
│   └── resources/
│       └── application.properties
└── test/
    ├── java/com/ejemplo/demo/
    │   ├── service/
    │   │   └── UsuarioServiceTest.java
    │   ├── controller/
    │   │   └── UsuarioControllerTest.java
    │   ├── repository/
    │   │   └── UsuarioRepositoryTest.java
    │   └── integration/
    │       └── UsuarioIntegrationTest.java
    └── resources/
        └── application-test.properties
```

Las pruebas se guardan en `src/test/java`.

Es recomendable mantener los mismos paquetes que en `src/main/java` para facilitar la organización.

---

# 6. JUnit 5

JUnit permite definir escenarios de prueba y comprobar sus resultados.

Ejemplo:

```java
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class CalculadoraTest {

    @Test
    void debeSumarDosNumeros() {
        // Arrange: preparar datos
        int a = 5;
        int b = 3;

        // Act: ejecutar la operación
        int resultado = a + b;

        // Assert: comprobar el resultado
        assertEquals(8, resultado);
    }
}
```

## Patrón Arrange, Act, Assert

Una buena prueba suele tener tres fases:

1. **Arrange:** preparar datos y dependencias.
2. **Act:** ejecutar el método que queremos probar.
3. **Assert:** verificar el resultado.

Este patrón facilita la lectura y el mantenimiento de las pruebas.

## Comprobar excepciones

```java
@Test
void debeLanzarExcepcionCuandoElIdEsInvalido() {
    assertThrows(
        IllegalArgumentException.class,
        () -> servicio.buscarPorId(-1L)
    );
}
```

La prueba pasa si se lanza la excepción esperada.

---

# 7. Mockito

Mockito permite probar una clase sin depender de la implementación real de sus colaboradores.

Por ejemplo, un servicio utiliza un repositorio. En una prueba unitaria podemos simular el repositorio y controlar sus respuestas.

## Ejemplo de mock

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EjemploMockitoTest {

    @Mock
    private MiRepositorio repositorio;

    @Test
    void ejemploDeMock() {
        when(repositorio.buscarNombre()).thenReturn("Ana");

        String resultado = repositorio.buscarNombre();

        assertEquals("Ana", resultado);
        verify(repositorio).buscarNombre();
    }
}
```

Este ejemplo presupone que existe un método `buscarNombre()` en `MiRepositorio`. En un proyecto real, utiliza los métodos que existan en tu clase.

## Diferencia entre `@Mock` y `@Autowired`

`@Mock` crea un objeto simulado de Mockito.

`@Autowired` solicita a Spring que inyecte un bean existente en el contexto.

Por tanto:

- En pruebas unitarias normales, es habitual usar `@Mock`.
- En pruebas con contexto Spring, es habitual usar `@Autowired`.
- En pruebas web aisladas, puede ser necesario registrar un mock como bean del contexto.

## `@InjectMocks`

Crea la instancia que queremos probar e intenta inyectar en ella los mocks declarados.

```java
@Mock
private UsuarioRepository usuarioRepository;

@InjectMocks
private UsuarioService usuarioService;
```

No arranca Spring ni necesita `@SpringBootTest`.

---

# 8. Pruebas unitarias de servicios

Supongamos que tenemos un servicio que consulta usuarios.

## Servicio de ejemplo

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
                    new UsuarioNoEncontradoException(
                        "No existe el usuario con id " + id
                    )
                );
    }
}
```

Excepción personalizada:

```java
public class UsuarioNoEncontradoException
        extends RuntimeException {

    public UsuarioNoEncontradoException(String mensaje) {
        super(mensaje);
    }
}
```

## Prueba unitaria completa

```java
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UsuarioServiceTest {

    @Mock
    private UsuarioRepository usuarioRepository;

    @InjectMocks
    private UsuarioService usuarioService;

    @Test
    void debeEncontrarUsuarioCuandoExiste() {
        // Arrange
        Usuario usuario = new Usuario(
            "Ana",
            "ana@ejemplo.com"
        );

        when(usuarioRepository.findById(1L))
                .thenReturn(Optional.of(usuario));

        // Act
        Usuario resultado = usuarioService.buscarPorId(1L);

        // Assert
        assertNotNull(resultado);
        assertEquals("Ana", resultado.getNombre());
        assertEquals(
            "ana@ejemplo.com",
            resultado.getEmail()
        );

        verify(usuarioRepository).findById(1L);
    }

    @Test
    void debeLanzarExcepcionCuandoNoExiste() {
        // Arrange
        when(usuarioRepository.findById(99L))
                .thenReturn(Optional.empty());

        // Act + Assert
        assertThrows(
            UsuarioNoEncontradoException.class,
            () -> usuarioService.buscarPorId(99L)
        );

        verify(usuarioRepository).findById(99L);
    }
}
```

## ¿Qué estamos comprobando?

En la primera prueba:

1. Simulamos que el repositorio encuentra un usuario.
2. Ejecutamos el servicio.
3. Comprobamos los datos devueltos.
4. Verificamos que el repositorio fue consultado.

En la segunda:

1. Simulamos que el usuario no existe.
2. Ejecutamos el servicio.
3. Comprobamos que se lanza la excepción esperada.

No se utiliza una base de datos. La prueba se centra en el comportamiento de `UsuarioService`.

---

# 9. Pruebas de repositorios con `@DataJpaTest`

`@DataJpaTest` permite probar repositorios y componentes JPA con un contexto reducido.

Normalmente configura una base de datos embebida si está disponible y revierte la transacción de cada prueba al terminar.

## Ejemplo de repositorio

```java
public interface UsuarioRepository
        extends JpaRepository<Usuario, Long> {

    Optional<Usuario> findByEmail(String email);
}
```

## Prueba del repositorio

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;

@DataJpaTest
class UsuarioRepositoryTest {

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Test
    void debeBuscarUsuarioPorEmail() {
        // Arrange
        Usuario usuario = new Usuario(
            "Ana",
            "ana.jpa@ejemplo.com"
        );

        usuarioRepository.saveAndFlush(usuario);

        // Act
        Optional<Usuario> resultado =
            usuarioRepository.findByEmail(
                "ana.jpa@ejemplo.com"
            );

        // Assert
        assertTrue(resultado.isPresent());
        assertEquals(
            "Ana",
            resultado.get().getNombre()
        );
    }
}
```

## ¿Qué comprueba `@DataJpaTest`?

- Que las entidades se pueden persistir.
- Que las consultas del repositorio funcionan.
- Que las consultas derivadas por nombre funcionan.
- Que se pueden recuperar los datos guardados.
- Que los mapeos y restricciones JPA se comportan como se espera.

## ¿Por qué `saveAndFlush()`?

`saveAndFlush()` fuerza el envío de los cambios pendientes a la base de datos. Puede ayudar a detectar errores de persistencia antes de finalizar la prueba.

No es necesario utilizarlo en todos los casos.

**Importante:** H2 no se comporta exactamente igual que PostgreSQL o MySQL. Para consultas o migraciones específicas del motor de producción, conviene realizar pruebas con la misma base de datos.

---

# 10. Pruebas de controladores con `@WebMvcTest`

`@WebMvcTest` crea un contexto centrado en la capa web.

Es útil para comprobar:

- Las rutas del controlador.
- Los códigos HTTP.
- La conversión de JSON.
- Las validaciones.
- Las respuestas y errores HTTP.

Normalmente no carga toda la aplicación. Por eso, los servicios utilizados por el controlador suelen simularse.

## Controlador de ejemplo

```java
@RestController
@RequestMapping("/usuarios")
public class UsuarioController {

    private final UsuarioService usuarioService;

    public UsuarioController(UsuarioService usuarioService) {
        this.usuarioService = usuarioService;
    }

    @GetMapping("/{id}")
    public Usuario buscarPorId(@PathVariable Long id) {
        return usuarioService.buscarPorId(id);
    }
}
```

## Prueba con MockMvc

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.Optional;

import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(UsuarioController.class)
class UsuarioControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private UsuarioService usuarioService;

    @Test
    void debeDevolverUsuario() throws Exception {
        Usuario usuario = new Usuario(
            "Ana",
            "ana@ejemplo.com"
        );

        when(usuarioService.buscarPorId(1L))
                .thenReturn(usuario);

        mockMvc.perform(get("/usuarios/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.nombre").value("Ana"))
                .andExpect(jsonPath("$.email")
                    .value("ana@ejemplo.com"));
    }
}
```

El ejemplo usa `@MockitoBean`, disponible en versiones modernas de Spring Framework. En proyectos anteriores puede ser necesario usar `@MockBean`, de Spring Boot, teniendo en cuenta su estado de obsolescencia en las versiones recientes.

## ¿Qué hace `MockMvc`?

La expresión:

```java
mockMvc.perform(get("/usuarios/1"))
```

simula una petición GET a `/usuarios/1`.

Después podemos comprobar la respuesta:

```java
.andExpect(status().isOk())
```

comprueba que el estado HTTP es 200.

```java
.andExpect(jsonPath("$.nombre").value("Ana"))
```

comprueba que el JSON contiene el campo `nombre` con valor `Ana`.

Estas pruebas verifican la capa web con el servicio simulado. No comprueban que la base de datos funcione.

---

# 11. Pruebas de integración con `@SpringBootTest`

`@SpringBootTest` carga el contexto de Spring Boot y permite probar componentes reales conectados.

Es útil para comprobar:

- La inyección de dependencias.
- La configuración de la aplicación.
- La comunicación entre servicios y repositorios.
- La integración de distintas capas.
- Los flujos completos de una aplicación.

## Ejemplo mínimo

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.assertNotNull;

@SpringBootTest
class UsuarioServiceIntegrationTest {

    @Autowired
    private UsuarioService usuarioService;

    @Test
    void debeCargarElServicio() {
        assertNotNull(usuarioService);
    }
}
```

Esta prueba comprueba que el contexto puede arrancar y que el servicio se puede inyectar. No demuestra por sí sola que todas las funcionalidades del servicio sean correctas.

## Integración HTTP con contexto completo

Para probar peticiones HTTP con el contexto completo, podemos combinar `@SpringBootTest` y `@AutoConfigureMockMvc`.

```java
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class UsuarioApiIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void debeResponderConEstadoOk() throws Exception {
        mockMvc.perform(get("/usuarios"))
                .andExpect(status().isOk());
    }
}
```

Este ejemplo presupone que existe la ruta `/usuarios` y que el contexto puede arrancar. Si el controlador accede a una base de datos, debe existir una configuración de base de datos de pruebas válida.

Cuando no sustituyes el servicio por un mock, la petición puede recorrer el controlador, el servicio y el repositorio reales.

---

# 12. `@AutoConfigureMockMvc` y `MockMvc`

## ¿Para qué sirve `@AutoConfigureMockMvc`?

Configura automáticamente `MockMvc` dentro del contexto de pruebas de Spring.

Se utiliza habitualmente así:

```java
@SpringBootTest
@AutoConfigureMockMvc
class MiControladorIntegrationTest {

    @Autowired
    private MockMvc mockMvc;
}
```

### ¿Qué aporta cada anotación?

`@SpringBootTest`:

- Carga el contexto de Spring Boot.
- Crea los beans de la aplicación según la configuración.
- Permite comprobar que las capas se integran.

`@AutoConfigureMockMvc`:

- Configura el objeto `MockMvc`.
- Permite simular peticiones HTTP a través de la infraestructura MVC.
- No necesita arrancar un servidor HTTP real.

`@Autowired`:

- Inyecta el `MockMvc` configurado por Spring.

### ¿Qué significa simular una petición?

Por ejemplo:

```java
mockMvc.perform(get("/usuarios"))
        .andExpect(status().isOk());
```

Spring procesa la petición simulada mediante su infraestructura MVC. Según el contexto configurado, la llamada puede alcanzar los controladores y servicios reales.

### ¿MockMvc es un servidor real?

No. `MockMvc` no abre un puerto HTTP real.

Si necesitas arrancar un servidor real en un puerto aleatorio, puedes utilizar:

```java
@SpringBootTest(
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class UsuarioApiRealHttpTest {
    // Usar un cliente HTTP apropiado para realizar peticiones.
}
```

Para muchas pruebas de integración MVC, `MockMvc` es suficiente y resulta más sencillo.

---

# 13. Pruebas HTTP: GET, POST, PUT y DELETE

Los ejemplos siguientes muestran cómo simular diferentes métodos HTTP. Debes adaptar las rutas y las respuestas a los contratos reales de tu API.

## GET

```java
mockMvc.perform(get("/usuarios"))
        .andExpect(status().isOk());
```

Comprueba que el endpoint devuelve un estado HTTP 200.

## GET por ID

```java
mockMvc.perform(get("/usuarios/{id}", 1L))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.nombre").value("Ana"));
```

El usuario debe existir, o el servicio simulado debe estar configurado para devolverlo.

## POST

```java
String json = """
    {
      "nombre": "Ana",
      "email": "ana.post@ejemplo.com"
    }
    """;

mockMvc.perform(post("/usuarios")
        .contentType(MediaType.APPLICATION_JSON)
        .content(json))
    .andExpect(status().isCreated());
```

Esta prueba comprueba una petición POST que debería crear un recurso y devolver `201 Created`.

## DELETE

```java
mockMvc.perform(delete("/usuarios/{id}", 1L))
        .andExpect(status().isNoContent());
```

Presupone que el contrato de la API devuelve `204 No Content` cuando elimina correctamente el recurso.

## PUT

Para probar PUT, el controlador debe tener un endpoint de actualización implementado.

Un contrato posible sería:

- Ruta: `PUT /usuarios/{id}`.
- Entrada: JSON con los datos que se van a actualizar.
- Respuesta: `200 OK` con el recurso actualizado.
- Si no existe: `404 Not Found`.

Una vez definido el contrato, la prueba debe comprobar tanto el código HTTP como los datos devueltos.

## ¿Qué debería comprobar una prueba HTTP?

Además del código de estado, conviene comprobar:

- El cuerpo JSON.
- Las cabeceras relevantes.
- El tipo de contenido.
- Los errores de validación.
- Los mensajes de error, si forman parte del contrato de la API.
- Los cambios persistidos cuando se trata de una prueba de integración completa.

---

# 14. Pruebas de validaciones y excepciones

## Validación de datos

Supongamos que el DTO utiliza:

```java
public class CrearUsuarioRequest {

    @NotBlank
    private String nombre;

    @NotBlank
    @Email
    private String email;

    // Getters y setters
}
```

El controlador debe utilizar `@Valid`:

```java
@PostMapping
@ResponseStatus(HttpStatus.CREATED)
public Usuario crear(
        @Valid @RequestBody CrearUsuarioRequest request) {
    return usuarioService.crear(request);
}
```

Y la aplicación debe tener disponible `spring-boot-starter-validation`.

## Probar un email inválido

```java
@Test
void debeRechazarEmailInvalido() throws Exception {
    String json = """
        {
          "nombre": "Ana",
          "email": "esto-no-es-un-email"
        }
        """;

    mockMvc.perform(post("/usuarios")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest());
}
```

La validación debe fallar antes de ejecutar la lógica del servicio.

## Probar un nombre vacío

```java
@Test
void debeRechazarNombreVacio() throws Exception {
    String json = """
        {
          "nombre": " ",
          "email": "ana@ejemplo.com"
        }
        """;

    mockMvc.perform(post("/usuarios")
            .contentType(MediaType.APPLICATION_JSON)
            .content(json))
        .andExpect(status().isBadRequest());
}
```

## Convertir una excepción en HTTP 404

Puedes crear un manejador global de excepciones:

```java
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(UsuarioNoEncontradoException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ErrorResponse manejarUsuarioNoEncontrado(
            UsuarioNoEncontradoException exception) {

        return new ErrorResponse(exception.getMessage());
    }

    public record ErrorResponse(String mensaje) {}
}
```

Ahora, cuando el servicio lance `UsuarioNoEncontradoException`, la API responderá con `404 Not Found`.

## Probar el error HTTP

En una prueba de controlador con el servicio simulado:

```java
when(usuarioService.buscarPorId(999L))
    .thenThrow(
        new UsuarioNoEncontradoException(
            "Usuario no encontrado"
        )
    );

mockMvc.perform(get("/usuarios/999"))
    .andExpect(status().isNotFound())
    .andExpect(jsonPath("$.mensaje")
        .value("Usuario no encontrado"));
```

Este ejemplo corresponde a una prueba de la capa web. En una prueba de integración con servicio y repositorio reales, debes preparar un estado de base de datos en el que el usuario no exista.

---

# 15. Configuración de la base de datos de pruebas

Las pruebas de integración no deben depender de la base de datos de producción.

Una opción para empezar es utilizar H2 en memoria.

## Archivo `application-test.properties`

Ubicación:

`src/test/resources/application-test.properties`

Contenido:

```properties
spring.datasource.url=jdbc:h2:mem:testdb;DB_CLOSE_DELAY=-1
spring.datasource.driver-class-name=org.h2.Driver
spring.datasource.username=sa
spring.datasource.password=

spring.jpa.hibernate.ddl-auto=create-drop
spring.jpa.show-sql=false
```

## Activar el perfil de pruebas

```java
@SpringBootTest
@ActiveProfiles("test")
class UsuarioIntegrationTest {
}
```

`@ActiveProfiles("test")` hace que Spring utilice el perfil de pruebas.

Asegúrate de que H2 está presente en las dependencias de test y de que no hay otra configuración que sobrescriba estas propiedades.

## ¿Qué significa `create-drop`?

Hibernate crea el esquema al iniciar el contexto y lo elimina al cerrarlo.

Es cómodo para ejemplos y pruebas aisladas, pero no necesariamente reproduce la estrategia de migraciones de producción.

## ¿Se deben limpiar los datos?

Sí, o al menos debes garantizar que cada prueba tenga datos independientes.

Opciones:

- Utilizar transacciones que se revierten al finalizar.
- Preparar y eliminar datos explícitamente.
- Crear una base de datos nueva para las pruebas cuando sea posible.
- Utilizar emails y claves únicas en cada escenario.

No presupongas que una petición HTTP simulada revertirá automáticamente los cambios. El comportamiento depende de los límites transaccionales y de cómo se ejecute la prueba.

## ¿Y si la aplicación usa PostgreSQL o MySQL?

H2 no reproduce todas las particularidades de esos motores.

Para probar SQL específico, migraciones o restricciones propias de producción, es recomendable realizar pruebas contra el mismo motor. Testcontainers permite iniciar una base de datos en un contenedor durante la ejecución de las pruebas.

---

# 16. Diferencias entre las principales anotaciones

## `@Test`

Marca un método como prueba de JUnit.

```java
@Test
void debeEjecutarUnaPrueba() {
    assertEquals(4, 2 + 2);
}
```

## `@Mock`

Crea una dependencia simulada de Mockito. No crea un bean de Spring.

## `@InjectMocks`

Crea la clase que queremos probar e intenta inyectar los mocks en sus dependencias.

## `@ExtendWith(MockitoExtension.class)`

Activa la extensión de Mockito en JUnit 5.

## `@SpringBootTest`

Carga el contexto de Spring Boot. Se utiliza cuando se necesita probar la aplicación con su configuración y beans.

No implica necesariamente que se inicie un servidor HTTP real.

## `@AutoConfigureMockMvc`

Configura `MockMvc` dentro del contexto de pruebas para simular peticiones HTTP a la capa MVC.

## `@WebMvcTest`

Carga un contexto centrado en la capa web. Es útil para probar controladores sin cargar toda la aplicación.

## `@DataJpaTest`

Carga componentes de persistencia JPA y repositorios en un contexto reducido. Normalmente utiliza una base de datos embebida disponible y revierte la transacción de cada prueba.

## `@ActiveProfiles("test")`

Activa el perfil de configuración `test`.

## `@BeforeEach`

Ejecuta un método antes de cada prueba.

## Tabla resumen

| Anotación | Uso principal |
|---|---|
| `@Test` | Definir una prueba |
| `@Mock` | Simular una dependencia |
| `@InjectMocks` | Construir la clase que se prueba |
| `@ExtendWith(MockitoExtension.class)` | Activar Mockito con JUnit 5 |
| `@SpringBootTest` | Cargar el contexto completo |
| `@AutoConfigureMockMvc` | Configurar pruebas MVC con MockMvc |
| `@WebMvcTest` | Probar la capa web |
| `@DataJpaTest` | Probar JPA y repositorios |
| `@ActiveProfiles("test")` | Activar configuración de pruebas |
| `@BeforeEach` | Preparar cada prueba |

---

# 17. Errores frecuentes

## Error: `No qualifying bean of type ...`

Spring no encuentra un bean de la clase requerida.

Comprueba:

- Que la clase tenga la anotación correspondiente, como `@Service`, cuando proceda.
- Que esté en un paquete escaneado por Spring.
- Que el contexto de prueba incluya el componente.
- Que una prueba `@WebMvcTest` tenga los mocks de las dependencias que necesita el controlador.

## Error: `Failed to load ApplicationContext`

El contexto no puede arrancar.

Posibles causas:

- Base de datos mal configurada.
- Variables de entorno ausentes.
- Dependencias no resueltas.
- Propiedades incompatibles.
- Migraciones fallidas.
- Configuración de producción aplicada durante las pruebas.

Lee la causa raíz del stack trace; los mensajes posteriores pueden ser consecuencias del primer error.

## Error: `NullPointerException` en una prueba unitaria

Puede ocurrir si los mocks no se han inicializado o si la clase se ha creado sin pasarle las dependencias necesarias.

Comprueba que utilizas:

```java
@ExtendWith(MockitoExtension.class)
```

y que los mocks están correctamente declarados.

## Error: `MockMvc` no está disponible

Si utilizas `@SpringBootTest`, añade:

```java
@AutoConfigureMockMvc
```

Si solo quieres probar la capa web, considera `@WebMvcTest`.

## Error HTTP 400 inesperado

Comprueba:

- Que el JSON esté bien formado.
- Que `Content-Type` sea `application/json`.
- Que los campos coincidan con el DTO.
- Que las validaciones se cumplan.
- Que la dependencia de validación esté presente.

## Error HTTP 404 inesperado

Comprueba:

- La URL.
- El método HTTP.
- El mapeo del controlador.
- Que el controlador se encuentre en el contexto.
- Que el recurso exista, si el endpoint depende de él.

## Error de email duplicado

La base de datos puede contener datos de una ejecución anterior. Aísla los datos de prueba y define una estrategia de limpieza.

## Las pruebas pasan individualmente, pero fallan en conjunto

Esto suele indicar:

- Datos compartidos.
- Estado mutable compartido.
- Dependencia del orden de ejecución.
- Limpieza insuficiente.

Cada prueba debería ser independiente y reproducible.

---

# 18. Cobertura de código

La cobertura mide qué partes del código se ejecutan durante las pruebas.

Una herramienta habitual es JaCoCo, disponible para Maven y Gradle.

La cobertura ayuda a localizar código que no se prueba, pero una cobertura alta no garantiza que las pruebas sean buenas.

Por ejemplo, una prueba puede ejecutar un método sin comprobar que devuelve el resultado correcto.

Prioriza comprobar:

- Reglas de negocio.
- Casos límite.
- Excepciones.
- Validaciones.
- Ramas condicionales.
- Consultas de persistencia.
- Flujos HTTP importantes.

No escribas pruebas únicamente para aumentar un porcentaje.

---

# 19. Ejecutar las pruebas

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
./mvnw -Dtest=UsuarioServiceTest test
```

Ejecutar varias clases:

```bash
./mvnw -Dtest=UsuarioServiceTest,UsuarioRepositoryTest test
```

Ejecutar la fase de verificación:

```bash
./mvnw verify
```

## Gradle

Ejecutar todas las pruebas:

```bash
./gradlew test
```

Ejecutar una clase:

```bash
./gradlew test --tests "com.ejemplo.demo.service.UsuarioServiceTest"
```

## Desde el IDE

En IntelliJ IDEA, Eclipse o VS Code puedes ejecutar una clase o un método de prueba directamente desde el editor.

También es importante ejecutar las pruebas mediante Maven o Gradle, porque son los comandos que suelen utilizarse en integración continua.

---

# 20. Buenas prácticas

## Para pruebas unitarias

- Mantén las pruebas rápidas.
- No cargues Spring si no es necesario.
- Simula únicamente las dependencias que quieras aislar.
- Comprueba resultados y comportamientos relevantes.
- Prueba escenarios válidos e inválidos.
- Utiliza nombres descriptivos.
- Evita compartir estado entre pruebas.

## Para pruebas de integración

- Utiliza una base de datos exclusiva de pruebas.
- No conectes accidentalmente con producción.
- Prepara los datos necesarios para cada escenario.
- Define una estrategia de limpieza.
- Evita simular las capas cuya integración quieres comprobar.
- Comprueba los recorridos críticos de la aplicación.
- Utiliza el motor real de base de datos cuando la compatibilidad sea importante.

## Estrategia recomendada

Para una API REST típica de Spring Boot sin Spring Security:

1. Escribe pruebas unitarias de servicios con JUnit 5 y Mockito.
2. Prueba los repositorios con `@DataJpaTest`.
3. Prueba los controladores con `@WebMvcTest` y `MockMvc`.
4. Añade pruebas de integración con `@SpringBootTest` y `@AutoConfigureMockMvc`.
5. Prueba los flujos más importantes con la base de datos real o con Testcontainers cuando sea necesario.

---

# Resumen final

- `@Test`: identifica un método de prueba.
- `@Mock`: crea una dependencia simulada.
- `@InjectMocks`: crea la clase que se quiere probar e intenta inyectarle los mocks.
- `@ExtendWith(MockitoExtension.class)`: activa Mockito con JUnit 5.
- `@WebMvcTest`: se centra en la capa web.
- `@DataJpaTest`: se centra en JPA y los repositorios.
- `@SpringBootTest`: carga el contexto de Spring Boot.
- `@AutoConfigureMockMvc`: configura `MockMvc` para simular peticiones HTTP.
- `MockMvc`: permite probar endpoints sin iniciar un servidor HTTP real.

**Idea clave:** utiliza pruebas unitarias para verificar la lógica de cada clase de forma aislada y pruebas de integración para comprobar que las distintas partes de tu aplicación funcionan correctamente juntas.
