package com.example.dto;

import java.util.List;

public record ProductSearchResponse(
        String mensaje,
        String presentation,
        List<ProductResponse> products) {}

