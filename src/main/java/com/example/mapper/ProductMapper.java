package com.example.mapper;

import com.example.dto.ProductResponse;
import com.example.entities.Product;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class ProductMapper {

    public ProductResponse toResponse(Product product) {
        if (product == null) {
            return null;
        }

        return new ProductResponse(
                product.getId(),
                product.getName(),
                product.getDescription(),
                product.getStock(),
                product.getPrice(),
                product.getProductImage()
        );
    }

    public List<ProductResponse> toResponseList(List<Product> products) {

        return products.stream()
                .map(this::toResponse)
                .toList();
    }
}