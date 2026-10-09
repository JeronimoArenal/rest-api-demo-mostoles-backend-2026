package com.example.dto;

import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.server.mvc.RepresentationModelAssemblerSupport;
import org.springframework.stereotype.Component;

import com.example.controllers.ProductController;
import com.example.entities.Product;

/**
 * Ensamblador de modelos HATEOAS: convierte la entidad {@link Product} (capa
 * de persistencia) en un {@link EntityModel} cuyo contenido es el DTO
 * {@link ProductoDto} (capa de presentación), con todos los enlaces
 * hipermedia.
 *
 * <p>
 * Al ser un {@code @Component} de Spring, centraliza la creación de los
 * enlaces y elimina el código repetitivo que antes se repetía en cada método
 * de {@link ProductController}. Es el patrón de la guía oficial de Spring
 * (sección "Eliminating boilerplate code"): la entidad JPA NO extiende
 * {@code RepresentationModel} y el DTO sí queda envuelto en {@link EntityModel}.
 * </p>
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

	@Override
	public EntityModel<ProductoDto> toModel(Product product) {
		return EntityModel.of(productoMapper.toProductoDto(product),
				linkTo(methodOn(ProductController.class).findProductById(product.getId())).withSelfRel(),
				linkTo(methodOn(ProductController.class).dameProductos(null, null)).withRel("all-products"));
	}
}