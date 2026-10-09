package com.example.dto;

import java.math.BigDecimal;

public record ProductDto(
        String name,
        int stock,
        BigDecimal price){
}

