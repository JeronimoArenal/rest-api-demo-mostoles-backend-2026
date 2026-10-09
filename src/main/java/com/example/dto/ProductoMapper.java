package com.example.dto;

import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import com.example.entities.Product;

/**
 * Mapper MapStruct entre la entidad JPA {@link Product} y el DTO de
 * presentación {@link ProductoDto}.
 *
 * <p>
 * Con {@code componentModel = "spring"} MapStruct genera una implementación
 * ({@code ProductoMapperImpl}) registrada como bean de Spring, inyectable en
 * los controladores.
 * </p>
 */
@Mapper(componentModel = "spring")
public interface ProductoMapper {

    /**
     * Mapea la entidad JPA (persistencia) al DTO (presentación).
     *
     * <p>
     * {@code presentationName} sale del nombre de la presentación: es seguro
     * porque todos los accesos de lectura del DAO hacen fetch-join de la
     * presentación y en save/update viene en memoria en el request.
     * </p>
     */
    @Mapping(target = "presentationName", source = "presentation.name")
    ProductoDto toProductoDto(Product product);

    /**
     * Mapea el DTO (presentación) a la entidad JPA (persistencia).
     *
     * <p>
     * La presentación no se mapea porque el DTO no la expone; de necesitarse se
     * resolvería desde la capa de servicios.
     * </p>
     */
    @Mapping(target = "presentation", ignore = true)
    Product toProduct(ProductoDto productoDto);
}