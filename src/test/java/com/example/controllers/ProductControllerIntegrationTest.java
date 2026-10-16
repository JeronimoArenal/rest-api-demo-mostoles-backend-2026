package com.example.controllers;

import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class ProductControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    private String token;

    @BeforeEach
    void setUp() throws Exception {
        String loginJson = """
        {
            "username": "user",
            "password": "123456"
        }
        """;

        // Simula una petición HTTP
        MvcResult loginResult = mockMvc.perform(post("/api/auth/signin")
                                .contentType(MediaType.APPLICATION_JSON)
                                .content(loginJson)
                )
                .andExpect(status().isOk())
                .andReturn();

        // Guardamos en la variable responseBody el JSON completo como texto.
        String responseBody = loginResult.getResponse().getContentAsString();

        JSONObject jsonObject = new JSONObject(responseBody);
        token = "Bearer " + jsonObject.getString("token");
    }

    //....................... usuarioNoAutenticadoNoPuedeListarProductosPorPresentacion ............................
    @Test
    @DisplayName("Listar productos: usuario no autenticado")
    void usuarioNoAutenticadoNoPuedeListarProductosPorPresentacion()throws Exception {

        mockMvc.perform(get("/products/presentation")
                                .param("name", "Botella")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isUnauthorized());
    }

    //....................... usuarioAutenticadoPuedeListarProductosPorPresentacion ............................
    @Test
    @DisplayName("Listar productos: usuario autenticado")
    void usuarioAutenticadoPuedeListarProductosPorPresentacion()throws Exception {

        mockMvc.perform(get("/products/presentation")
                                .header("Authorization", token)
                                .param("name", "Botella")
                                .accept(MediaType.APPLICATION_JSON)
                )
                .andExpect(status().isOk());
    }
}