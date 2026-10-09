package com.example.dto;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.hateoas.Affordance;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.hateoas.PagedModel;
import org.springframework.hateoas.PagedModel.PageMetadata;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

import com.example.controllers.ProductController;
import com.example.entities.Product;
import com.example.spring_security_jwt.model.ERole;

/**
 * Ensamblador de modelos HATEOAS: convierte la entidad {@link Product} (capa de
 * persistencia) en un {@link EntityModel} cuyo contenido es el DTO
 * {@link ProductoDto} (capa de presentación), con todos los enlaces
 * hipermedia.
 *
 * <p>
 * Al ser un {@code @Component} de Spring, centraliza la creación de los enlaces
 * y elimina el código repetitivo que antes se repetía en cada método de
 * {@link ProductController}. Sigue el patrón de la guía oficial de Spring
 * (sección "Eliminating boilerplate code"): la entidad JPA NO extiende
 * {@code RepresentationModel}.
 * </p>
 *
 * <p>
 * Además del patrón base, este ensamblador construye los enlaces dinámicos
 * completos:
 * </p>
 * <ul>
 * <li><b>Operaciones (affordances):</b> el enlace {@code self} de cada producto
 * expone los affordances {@code PUT} (actualizar) y {@code DELETE} (eliminar), y
 * el enlace de la colección expone el affordance {@code POST} (crear). Son
 * servidos con el media type {@code application/prs.hal-forms+json} (HAL-FORMS)
 * mediante {@code _templates}.</li>
 * <li><b>Enlaces condicionados por el rol:</b> solo el usuario autenticado con
 * {@code ROLE_ADMIN} ve los affordances de escritura (POST, PUT, DELETE); el
 * {@code ROLE_USER} (solo lectura) ve únicamente los enlaces {@code self} y
 * {@code all-products}.</li>
 * <li><b>Paginación:</b> {@link #toPagedModel(Page)} construye un
 * {@link PagedModel} con la metadata de la página ({@code page}) y los enlaces
 * de navegación {@code first}, {@code prev}, {@code self}, {@code next} y
 * {@code last}.</li>
 * </ul>
 */
@Component
public class ProductoModelAssembler extends RepresentationModelAssemblerSupport<Product, EntityModel<ProductoDto>> {

	private final ProductoMapper productoMapper;

	public ProductoModelAssembler(ProductoMapper productoMapper) {

		// resourceType solo lo usa instantiateModel() (no invocado: toModel crea el
		// EntityModel con EntityModel.of). Se pasa EntityModel.class como marcador.
		super(ProductController.class, (Class) EntityModel.class);
		this.productoMapper = productoMapper;
	}

	/**
	 * Convierte un producto en un {@link EntityModel} con el DTO
	 * {@link ProductoDto} y los enlaces dinámicos:
	 * {@code self} (GET del producto) y {@code all-products} (GET de la colección
	 * completa). Los affordances PUT y DELETE del enlace {@code self} solo se
	 * publican si el usuario autenticado es {@code ROLE_ADMIN}.
	 */
	@Override
	public EntityModel<ProductoDto> toModel(Product product) {

		int id = product.getId();

		// self: GET del producto; solo el ADMIN ve los affordances PUT (actualizar)
		// y DELETE (eliminar) que descubren las operaciones de escritura
		Link self = linkTo(methodOn(ProductController.class).findProductById(id)).withSelfRel();

		if (usuarioEsAdmin()) {
			self = self
					.andAffordance(affordanceActualizar(id))
					.andAffordance(afford(methodOn(ProductController.class).deleteProducto(id)));
		}

		return EntityModel.of(productoMapper.toProductoDto(product), self,
				linkTo(methodOn(ProductController.class).dameProductos(null, null)).withRel("all-products"));
	}

	/**
	 * Modelo paginado de la colección: cada producto con sus enlaces, la metadata
	 * de la página ({@code page}) y los enlaces de navegación {@code first},
	 * {@code prev}, {@code self}, {@code next} y {@code last} (estos dos últimos
	 * solo cuando existen).
	 *
	 * @param productos página de productos devuelta por el servicio
	 * @return el {@link PagedModel} con la colección paginada
	 */
	public PagedModel<EntityModel<ProductoDto>> toPagedModel(Page<Product> productos) {

		List<EntityModel<ProductoDto>> recursos = productos.getContent().stream().map(this::toModel).toList();

		PageMetadata metadata = new PageMetadata(productos.getSize(), productos.getNumber(),
				productos.getTotalElements(), productos.getTotalPages());

		List<Link> enlaces = new ArrayList<>();

		// self: la página actual
		enlaces.add(linkTo(methodOn(ProductController.class).dameProductos(productos.getNumber(),
				productos.getSize())).withSelfRel());

		// first: primera página (0)
		enlaces.add(linkTo(methodOn(ProductController.class).dameProductos(0, productos.getSize())).withRel("first"));

		// prev: solo si hay página anterior
		if (productos.hasPrevious()) {
			enlaces.add(linkTo(methodOn(ProductController.class).dameProductos(productos.getNumber() - 1,
					productos.getSize())).withRel("prev"));
		}

		// next: solo si hay página siguiente
		if (productos.hasNext()) {
			enlaces.add(linkTo(methodOn(ProductController.class).dameProductos(productos.getNumber() + 1,
					productos.getSize())).withRel("next"));
		}

		// last: última página (solo si hay al menos una página)
		if (productos.getTotalPages() > 0) {
			enlaces.add(linkTo(methodOn(ProductController.class).dameProductos(productos.getTotalPages() - 1,
					productos.getSize())).withRel("last"));
		}

		// all-products: colección completa (sin paginar), con el affordance POST
		enlaces.add(enlaceColeccionConPost());

		return PagedModel.of(recursos, metadata, enlaces);
	}

	/**
	 * Colección completa de productos (sin paginar), con los enlaces de colección
	 * ({@code self} y {@code all-products}) y el affordance POST sobre la
	 * colección.
	 *
	 * @param products lista completa de productos devuelta por el servicio
	 */
	public CollectionModel<EntityModel<ProductoDto>> toColeccion(Iterable<? extends Product> products) {

		CollectionModel<EntityModel<ProductoDto>> modelo = super.toCollectionModel(products);

		modelo.add(linkTo(methodOn(ProductController.class).dameProductos(null, null)).withSelfRel());
		modelo.add(enlaceColeccionConPost());

		return modelo;
	}

	/**
	 * Enlace a la colección completa de productos con el affordance POST (crear un
	 * nuevo producto en la colección). El affordance solo se publica si el usuario
	 * autenticado es {@code ROLE_ADMIN}.
	 */
	private Link enlaceColeccionConPost() {
		Link enlace = linkTo(methodOn(ProductController.class).dameProductos(null, null)).withRel("all-products");
		if (usuarioEsAdmin()) {
			enlace = enlace.andAffordance(affordanceCrearProducto());
		}
		return enlace;
	}

	/**
	 * Indica si el usuario autenticado en la petición actual tiene el rol
	 * {@code ROLE_ADMIN}. Los affordances de escritura (POST, PUT, DELETE) solo se
	 * publican en los enlaces cuando el usuario es administrador; el
	 * {@code ROLE_USER} (solo lectura) solo recibe los enlaces {@code self} y
	 * {@code all-products}.
	 */
	private boolean usuarioEsAdmin() {

		Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

		if (authentication == null || !authentication.isAuthenticated()) {
			return false;
		}

		return authentication.getAuthorities().stream()
				.anyMatch(authority -> ERole.ROLE_ADMIN.name().equals(authority.getAuthority()));
	}

	/**
	 * Affordance PUT (actualizar producto). updateProduct declara
	 * {@code throws IOException}; el proxy de {@code methodOn} nunca ejecuta el
	 * método, pero el compilador exige tratar la excepción.
	 */
	private Affordance affordanceActualizar(int productId) {
		try {
			return afford(methodOn(ProductController.class).updateProduct(null, null, null, productId));
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo construir el affordance PUT", e);
		}
	}

	/**
	 * Affordance POST (crear producto). saveProduct declara
	 * {@code throws IOException}; el proxy de {@code methodOn} nunca ejecuta el
	 * método, pero el compilador exige tratar la excepción.
	 */
	private Affordance affordanceCrearProducto() {
		try {
			return afford(methodOn(ProductController.class).saveProduct(null, null, null));
		} catch (IOException e) {
			throw new IllegalStateException("No se pudo construir el affordance POST", e);
		}
	}
}