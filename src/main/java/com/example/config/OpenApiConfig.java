package com.example.config;

import io.swagger.v3.oas.models.Components;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import io.swagger.v3.oas.models.security.SecurityRequirement;
import io.swagger.v3.oas.models.security.SecurityScheme;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuración OpenAPI 3 (springdoc) para la API.
 *
 * <p>
 * Declara el esquema de seguridad {@code bearerAuth} (HTTP Bearer / JWT) y lo
 * aplica globalmente a toda la API: tanto Swagger UI ({@code /swagger-ui.html})
 * como Scalar ({@code /scalar}) muestran el diálogo de autenticación pidiendo el
 * token antes de poder ejecutar cualquier operación. Los endpoints públicos de
 * {@code /api/auth/**} se excluyen de este requisito con
 * {@code @SecurityRequirements({})} en el controlador de autenticación.
 * </p>
 *
 * <p>
 * Además de pedir el token dentro de las UIs, el propio acceso a la
 * documentación (OpenAPI, Swagger UI y Scalar) está protegido: sin un JWT
 * válido esas rutas responden 401 (ver {@code WebSecurityConfig}).
 * </p>
 */
@Configuration
public class OpenApiConfig {

	/** Nombre del esquema de seguridad Bearer JWT en el documento OpenAPI. */
	public static final String SECURITY_SCHEME_BEARER = "bearerAuth";

	@Bean
	public OpenAPI apiDocsOpenAPI() {

		return new OpenAPI()
				.info(new Info()
						.title("API REST Móstoles Backend 2026")
						.description("""
								Documentación de la API REST de productos con autenticación JWT (stateless).

								Casi todos los endpoints exigen un token JWT en la cabecera
								`Authorization: Bearer <token>`. Obtenlo primero llamando a
								POST /api/auth/signin con un usuario existente
								(p. ej. admin/123456 o user/123456) y pégalo en el botón
								"Authorize" de Swagger UI o en el panel "Authentication" de Scalar.

								La propia documentación (OpenAPI, Swagger UI y Scalar) también está
								protegida: sin token no se puede acceder a /v3/api-docs, /swagger-ui.html o /scalar.
								""")
						.version("1.0.0"))
				.components(new Components().addSecuritySchemes(SECURITY_SCHEME_BEARER,
						new SecurityScheme()
								.type(SecurityScheme.Type.HTTP)
								.scheme("bearer")
								.bearerFormat("JWT")
								.description("Token JWT obtenido en POST /api/auth/signin.")))
				.addSecurityItem(new SecurityRequirement().addList(SECURITY_SCHEME_BEARER));
	}
}