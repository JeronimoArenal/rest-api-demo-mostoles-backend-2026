package com.example.dto;

import java.math.BigDecimal;

/**
 * DTO de presentación de {@code Product}: separa la capa de persistencia
 * (entidad JPA) de la representación expuesta por la API REST.
 *
 * @param id                identificador del producto
 * @param name              nombre del producto
 * @param description       descripción del producto
 * @param stock             cantidad disponible del producto
 * @param price             precio del producto
 * @param productImage      nombre de la imagen del producto
 * @param presentationName  nombre de la presentación del producto
 */
public record ProductoDto(
        int id,
        String name,
        String description,
        int stock,
        BigDecimal price,
        String productImage,
        String presentationName) {
}