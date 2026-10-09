package com.example.spring_security_jwt.config;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.example.spring_security_jwt.payload.request.LoginRequest;
import org.json.JSONObject;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import tools.jackson.databind.ObjectMapper;

/**
 * Comprueba que la documentación de la API (OpenAPI {@code /v3/api-docs},
 * Swagger UI y Scalar) también está protegida: sin un JWT válido todas esas
 * rutas responden 401, y el documento OpenAPI declara el esquema de seguridad
 * {@code bearerAuth} (HTTP Bearer / JWT) para que las UIs pidan el token.
 *
 * <p>
 * Requiere MySQL (login real contra la base) y los usuarios sembrados por
 * {@code CreateSampleData} (admin/123456), igual que {@code ProductControllerTest}.
 * </p>
 */
@SpringBootTest
@AutoConfigureTestDatabase(replace = Replace.NONE)
@AutoConfigureMockMvc
class DocumentacionApiSecurityTest {

	@Autowired
	MockMvc mockMvc;

	@Autowired
	ObjectMapper objectMapper;

	String token;

	@BeforeEach
	void loginComoAdmin() throws Exception {

		LoginRequest login = LoginRequest.builder()
				.username("admin")
				.password("123456")
				.build();

		MvcResult result = mockMvc.perform(post("/api/auth/signin")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(login)))
				.andExpect(status().isOk())
				.andReturn();

		this.token = "Bearer " + new JSONObject(result.getResponse().getContentAsString()).getString("token");
	}

	@Test
	@DisplayName("OpenAPI /v3/api-docs sin token responde 401")
	void apiDocsSinTokenEs401() throws Exception {
		mockMvc.perform(get("/v3/api-docs"))
				.andExpect(status().isUnauthorized());
	}

	@Test
	@DisplayName("OpenAPI /v3/api-docs con token responde 200 y declara el security scheme bearerAuth")
	void apiDocsConTokenDeclaraBearerJwt() throws Exception {

		mockMvc.perform(get("/v3/api-docs")
						.header("Authorization", token))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.openapi").exists())
				.andExpect(jsonPath("$.info.title").value("API REST Móstoles Backend 2026"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.type").value("http"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.scheme").value("bearer"))
				.andExpect(jsonPath("$.components.securitySchemes.bearerAuth.bearerFormat").value("JWT"))
				.andExpect(jsonPath("$.security[0].bearerAuth").exists())
				.andExpect(jsonPath("$.paths./api/auth/signin.post.security").isEmpty());
	}

	@Test
	@DisplayName("Swagger UI sin token responde 401 y con token redirige/abre la UI")
	void swaggerUiRequiereToken() throws Exception {

		// Sin token: la propia página de Swagger UI exige autenticación
		mockMvc.perform(get("/swagger-ui.html"))
				.andExpect(status().isUnauthorized());

		// Con token: /swagger-ui.html redirige (302) a la UI real
		mockMvc.perform(get("/swagger-ui.html")
						.header("Authorization", token))
				.andExpect(status().is3xxRedirection());

		// Con token: la UI de Swagger se sirve correctamente
		mockMvc.perform(get("/swagger-ui/index.html")
						.header("Authorization", token))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Scalar sin token responde 401 y con token responde 200")
	void scalarRequiereToken() throws Exception {

		mockMvc.perform(get("/scalar"))
				.andExpect(status().isUnauthorized());

		mockMvc.perform(get("/scalar")
						.header("Authorization", token))
				.andExpect(status().isOk());
	}

	@Test
	@DisplayName("Login /api/auth/signin sigue siendo público (sin token responde 200)")
	void loginSigueSiendoPublico() throws Exception {

		LoginRequest login = LoginRequest.builder()
				.username("admin")
				.password("123456")
				.build();

		mockMvc.perform(post("/api/auth/signin")
						.contentType(MediaType.APPLICATION_JSON)
						.content(objectMapper.writeValueAsString(login)))
				.andExpect(status().isOk())
				.andExpect(jsonPath("$.token").exists());
	}
}