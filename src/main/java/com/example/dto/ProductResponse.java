package com.example.dto;

import com.fasterxml.jackson.annotation.JsonInclude;

import java.math.BigDecimal;

@JsonInclude(JsonInclude.Include.NON_NULL)
public record ProductResponse(
        int id,
        String name,
        String description,
        Integer stock,
        BigDecimal price,
        String productImage) {}