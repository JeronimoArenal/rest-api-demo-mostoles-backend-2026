# AGENTS.md

## Stack

- Spring Boot 4.1.0, Java 25, Maven wrapper `./mvnw`. Paquete raíz `com.example`.
- Spring MVC, Data JPA, Security (JWT stateless), HATEOAS, MapStruct (configurado pero SIN usar), Lombok, MySQL, JJWT, Commons Text.

## Comandos

- Compilar: `./mvnw -DskipTests compile` (verificado; funciona offline con el `.m2` local)
- Todos los tests: `./mvnw test`
- Test único: `./mvnw test -Dtest=ProductDaoTest`
- Empaquetar: `./mvnw -DskipTests package`
- Ejecutar: `./mvnw spring-boot:run`

## Tests: requieren MySQL real (importante)

- No hay H2 ni `src/test/resources`. Todos los `@DataJpaTest` y `@SpringBootTest` usan `@AutoConfigureTestDatabase(replace = Replace.NONE)`, o sea MySQL real del `application.properties` (por defecto `192.168.122.43:3306/comercio_mostoles`, credenciales `root`/`Kose9suf`, sobreescribibles con `DB_MYSQL_USERNAME`/`DB_MYSQL_PASSWORD`).
- `ProductControllerTest` levanta el contexto completo y se loguea de verdad; depende de que `CreateSampleData` haya creado `admin`/`123456` en el arranque.
- Por tanto `./mvnw test` falla si MySQL no está accesible.

## Gotchas de Spring Boot 4

- Imports de test con paquetes nuevos: `org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest`, `org.springframework.boot.webmvc.test.autoconfigure.{AutoConfigureMockMvc,WebMvcTest}`, `org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase`, `org.springframework.test.context.bean.override.mockito.MockitoBean`.
- Jackson 3 en tests: `tools.jackson.databind.ObjectMapper`, no `com.fasterxml.jackson`.
- No "corregir" estos imports a la nomenclatura de Boot 3: el proyecto es Boot 4.

## Arquitectura real (no confundir con DDD/Hexagonal)

- Estructura por capas clásica: `controllers` → `services` → `dao`, más `com.example.spring_security_jwt` para auth JWT. No existe paquete `domain`.
- `com.example.dao` contiene repositorios Spring Data (`JpaRepository`), no DAOs manuales. `ProductDao` define queries JPQL con fetch-join que sobreescriben `findAll(Pageable)`, `findAll(Sort)` y `findById(int)`.
- `ProductService`/`PresentationService` son pasantes y su interfaz filtra tipos de Spring Data (`Page`, `Pageable`, `Sort`) hacia arriba.
- Los controladores acumulan lógica e infraestructura: `@Transactional` en métodos de controlador y captura de `DataAccessException` para respuestas HTTP. Las entidades JPA se reciben directo en el request (`@RequestPart Product`).
- `ProductController` expone los endpoints CRUD devolviendo `EntityModel<ProductoDto>` (individual) y `CollectionModel<EntityModel<ProductoDto>>` (colección). Los enlaces `_links` están centralizados en `ProductoModelAssembler` (`com.example.dto`), un `@Component` que extiende `RepresentationModelAssemblerSupport<Product, EntityModel<ProductoDto>>` (patrón de la guía oficial de Spring, "eliminating boilerplate code"): cada recurso lleva `self` y `all-products`. En HAL la colección serializa como `_embedded.productoDtoList`. Las entidades NO extienden `RepresentationModel`.
- `ProductoDto` (record en `com.example.dto`) es el DTO de presentación de `Product`. MapStruct (`ProductoMapper`, `componentModel = "spring"`) mapea `Product` ↔ `ProductoDto`; las respuestas CRUD devuelven el DTO (mapeado por el assembler) pero los requests (multipart con `@RequestPart Product`) siguen recibiendo la entidad JPA directo.
- Seguridad: stateless; `/api/auth/**` público; el resto exige autenticación; `@PreAuthorize` por método en `/products` (ADMIN para escrituras/descarga, ADMIN o USER para lecturas). `app.security.enabled=true` existe en properties pero NO se lee en el código.
- `CreateSampleData` (`CommandLineRunner`) siembra roles, `admin`/`123456`, `user`/`123456`, presentaciones y productos al arrancar.
- `doc/readme.md` documenta únicamente la parte JWT y está desactualizada respecto a la capa de productos: no es un mapa completo del proyecto.

## Estado del "dominio" respecto a DDD/Hexagonal

- Los equivalentes de dominio son las entidades JPA `Product`/`Presentation` (`com.example.entities`) y `User`/`Role`/`ERole` (`com.example.spring_security_jwt.model`). No cumplen reglas estrictas por este acoplamiento verificado:
  - Persistencia (`jakarta.persistence.*`) directamente sobre las clases de dominio; no hay modelo de persistencia separado ni puertos/adaptadores.
  - Jackson (`@JsonIgnore`, `@JsonIgnoreProperties`) y Bean Validation (`@NotNull`, `@NotBlank`, `@Size`, `@Min`, `@Email`, mensajes en español) también sobre las entidades: el mismo objeto es dominio + persistencia + contrato HTTP.
  - Modelo anémico y mutable (Lombok `@Setter`/`@Builder` en todo); invariantes delegadas a anotaciones; sin value objects (el precio es `BigDecimal`, no `Money`); sin excepciones de dominio (se lanzan `RuntimeException` a secas).
  - Spring Data atraviesa los puertos: `ProductService` declara `Page`/`Pageable`/`Sort`; los DAO extienden `JpaRepository`; `UserDetailsImpl` implementa el `UserDetails` de Spring Security.
  - Ejemplo de pista: el nombre de paquete `spring_security_jwt.model` muestra tecnología dentro del nombre del "modelo".
- Si un cambio rompe algo de esto (p. ej. introducir un paquete `domain`), respetar las dependencias unidireccionales: el dominio no debe importar Spring/Jakarta.

## Convenciones

- Código, comentarios y mensajes de validación en español.
- Lombok para reducir boilerplate; records para DTOs (`com.example.dto`, `com.example.models`).