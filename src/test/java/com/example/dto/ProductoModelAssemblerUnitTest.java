package com.example.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.mediatype.MessageResolver;
import org.springframework.hateoas.mediatype.hal.CurieProvider;
import org.springframework.hateoas.mediatype.hal.HalJacksonModule;
import org.springframework.hateoas.mediatype.hal.HalJacksonModule.HalHandlerInstantiator;
import org.springframework.hateoas.server.core.DefaultLinkRelationProvider;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;

import com.example.entities.Presentation;
import com.example.entities.Product;
import com.example.spring_security_jwt.model.ERole;

import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

/**
 * Prueba unitaria del {@link ProductoModelAssembler}: verifica los enlaces
 * dinámicos (self, all-products), los affordances de las operaciones
 * (PUT/DELETE sobre cada producto y POST sobre la colección), su
 * condicionamiento por rol (solo {@code ROLE_ADMIN} los ve) y la paginación
 * (PagedModel con metadata "page" y enlaces first/prev/self/next/last).
 *
 * <p>
 * No levanta el contexto de Spring ni necesita MySQL: solo el assembler, el
 * mapper MapStruct generado (ProductoMapperImpl) y un SecurityContext simulado
 * (SecurityContextHolder) para probar los dos roles.
 * </p>
 */
class ProductoModelAssemblerUnitTest {

	private final ProductoModelAssembler assembler = new ProductoModelAssembler(new ProductoMapperImpl());

	@AfterEach
	void limpiarSeguridad() {
		SecurityContextHolder.clearContext();
	}

	private Product producto(int id, String nombre) {
		Presentation presentacion = Presentation.builder().name("decenas").build();
		return Product.builder()
				.id(id)
				.name(nombre)
				.description("Descripción del producto")
				.stock(10)
				.price(new BigDecimal("19.99"))
				.productImage("12345-foto.jpg")
				.presentation(presentacion)
				.build();
	}

	/** Simula un usuario autenticado con los roles indicados (ROLE_ADMIN, ROLE_USER...). */
	private void autenticarComo(String... roles) {
		var authorities = java.util.Arrays.stream(roles).map(SimpleGrantedAuthority::new).toList();
		SecurityContextHolder.getContext()
				.setAuthentication(new UsernamePasswordAuthenticationToken("admin", "123456", authorities));
	}

	/** Métodos HTTP que el enlace expone a través de sus affordances. */
	private List<HttpMethod> metodosDe(Link enlace) {
		return enlace.getAffordances().stream()
				.flatMap(affordance -> {
					List<HttpMethod> metodos = new ArrayList<>();
					affordance.forEach(modelo -> metodos.add(modelo.getHttpMethod()));
					return metodos.stream();
				})
				.distinct()
				.toList();
	}

	@Test
	@DisplayName("como ADMIN, el self de un producto expone PUT y DELETE y hay enlace all-products")
	void toModelAdminExponeOperacionesDeEscritura() {

		autenticarComo(ERole.ROLE_ADMIN.name());

		EntityModel<ProductoDto> modelo = assembler.toModel(producto(7, "Camara"));

		Link self = modelo.getRequiredLink("self");
		assertTrue(self.getHref().endsWith("/products/7"), "self debe apuntar a /products/7 pero fue " + self.getHref());

		List<HttpMethod> metodos = metodosDe(self);
		assertTrue(metodos.contains(HttpMethod.PUT), "como ADMIN el self debe exponer PUT (actualizar): " + metodos);
		assertTrue(metodos.contains(HttpMethod.DELETE), "como ADMIN el self debe exponer DELETE (eliminar): " + metodos);

		assertTrue(modelo.hasLink("all-products"), "el recurso debe tener el enlace all-products");
	}

	@Test
	@DisplayName("como USER (solo lectura), el self NO expone PUT ni DELETE")
	void toModelUserNoExponeOperacionesDeEscritura() {

		autenticarComo(ERole.ROLE_USER.name());

		EntityModel<ProductoDto> modelo = assembler.toModel(producto(7, "Camara"));

		Link self = modelo.getRequiredLink("self");
		assertTrue(self.getHref().endsWith("/products/7"), "self debe apuntar a /products/7 pero fue " + self.getHref());

		List<HttpMethod> metodos = metodosDe(self);
		assertFalse(metodos.contains(HttpMethod.PUT), "como USER el self NO debe exponer PUT: " + metodos);
		assertFalse(metodos.contains(HttpMethod.DELETE), "como USER el self NO debe exponer DELETE: " + metodos);
		assertEquals(List.of(HttpMethod.GET), metodos,
				"como USER el self solo debe llevar el affordance de lectura (GET): " + metodos);

		assertTrue(modelo.hasLink("all-products"), "el usuario de lectura sigue teniendo el enlace all-products");
	}

	@Test
	@DisplayName("como ADMIN, la colección expone POST (crear)")
	void toColeccionAdminExponePost() {

		autenticarComo(ERole.ROLE_ADMIN.name());

		CollectionModel<EntityModel<ProductoDto>> modelo = assembler
				.toColeccion(List.of(producto(1, "Camara"), producto(2, "Televisor")));

		assertEquals(2, modelo.getContent().size(), "debe haber dos recursos en la colección");
		assertTrue(modelo.hasLink("self"), "la colección debe tener enlace self");
		assertTrue(modelo.hasLink("all-products"), "la colección debe tener enlace all-products");

		Link coleccion = modelo.getRequiredLink("all-products");
		assertTrue(metodosDe(coleccion).contains(HttpMethod.POST), "como ADMIN la colección debe exponer POST (crear): "
				+ metodosDe(coleccion));
	}

	@Test
	@DisplayName("como USER, la colección NO expone POST")
	void toColeccionUserNoExponePost() {

		autenticarComo(ERole.ROLE_USER.name());

		CollectionModel<EntityModel<ProductoDto>> modelo = assembler
				.toColeccion(List.of(producto(1, "Camara"), producto(2, "Televisor")));

		Link coleccion = modelo.getRequiredLink("all-products");
		assertFalse(metodosDe(coleccion).contains(HttpMethod.POST), "como USER la colección NO debe exponer POST: "
				+ metodosDe(coleccion));
		assertEquals(List.of(HttpMethod.GET), metodosDe(coleccion),
				"como USER la colección solo debe llevar el affordance de lectura (GET): "
						+ metodosDe(coleccion));
	}

	@Test
	@DisplayName("toPagedModel: metadata de la página y enlaces first/prev/self/next/last")
	void toPagedModelConstruyeNavegacion() {

		List<Product> todos = List.of(producto(1, "A"), producto(2, "B"), producto(3, "C"),
				producto(4, "D"), producto(5, "E"));
		Page<Product> pagina = new PageImpl<>(todos.subList(0, 2), PageRequest.of(0, 2), todos.size());

		PagedModel<EntityModel<ProductoDto>> modelo = assembler.toPagedModel(pagina);

		// Metadata de la página (bloque "page" en el JSON)
		assertEquals(2, modelo.getContent().size());
		assertEquals(0, modelo.getMetadata().getNumber());
		assertEquals(2, modelo.getMetadata().getSize());
		assertEquals(5, modelo.getMetadata().getTotalElements());
		assertEquals(3, modelo.getMetadata().getTotalPages());

		// Enlaces de navegación (de lectura: visibles para cualquier rol)
		assertTrue(modelo.hasLink("first"), "debe existir el enlace first");
		assertTrue(modelo.hasLink("self"), "debe existir el enlace self");
		assertTrue(modelo.hasLink("next"), "debe existir el enlace next");
		assertTrue(modelo.hasLink("last"), "debe existir el enlace last");
		assertFalse(modelo.hasLink("prev"), "en la primera página no debe existir el enlace prev");
		assertTrue(modelo.hasLink("all-products"), "debe existir el enlace all-products");

		assertTrue(modelo.getRequiredLink("next").getHref().contains("page=1&size=2"), "next debe apuntar a page=1&size=2: "
				+ modelo.getRequiredLink("next").getHref());
		assertTrue(modelo.getRequiredLink("last").getHref().contains("page=2&size=2"), "last debe apuntar a page=2&size=2: "
				+ modelo.getRequiredLink("last").getHref());
	}

	@Test
	@DisplayName("JSON HAL: PagedModel serializa con bloque page y enlaces de navegación")
	void serializacionHalDelPagedModel() throws Exception {

		ObjectMapper mapper = JsonMapper.builder()
				.addModule(new HalJacksonModule())
				.handlerInstantiator(new HalHandlerInstantiator(new DefaultLinkRelationProvider(), CurieProvider.NONE,
						MessageResolver.DEFAULTS_ONLY))
				.build();

		List<Product> todos = List.of(producto(1, "A"), producto(2, "B"), producto(3, "C"),
				producto(4, "D"), producto(5, "E"));
		Page<Product> pagina = new PageImpl<>(todos.subList(0, 2), PageRequest.of(0, 2), todos.size());

		String json = mapper.writeValueAsString(assembler.toPagedModel(pagina));
		System.out.println("JSON PagedModel => " + json);

		assertTrue(json.contains("\"page\""), "el JSON debe incluir el bloque page");
		assertTrue(json.contains("\"totalElements\":5"), "el bloque page debe incluir totalElements");
		assertTrue(json.contains("\"totalPages\":3"), "el bloque page debe incluir totalPages");
		assertTrue(json.contains("\"next\""), "los _links deben incluir next");
		assertTrue(json.contains("\"last\""), "los _links deben incluir last");
	}
}