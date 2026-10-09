package com.example;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.hateoas.config.EnableHypermediaSupport;
import org.springframework.hateoas.config.EnableHypermediaSupport.HypermediaType;

/**
 * Clase principal de la aplicacion.
 *
 * <p>
 * Se activa el soporte hipermedia de HAL y HAL-FORMS: con HAL las respuestas se
 * serializan como {@code _embedded} / {@code _links}, y con HAL-FORMS
 * (media type {@code application/prs.hal-forms+json}) los affordances de las
 * operaciones (POST, PUT, DELETE) que el assembler agrega a los enlaces se
 * sirven como {@code _templates} para que un cliente hipermedia descubra las
 * operaciones permitidas sobre cada recurso.
 * </p>
 */
@SpringBootApplication
@EnableHypermediaSupport(type = { HypermediaType.HAL, HypermediaType.HAL_FORMS })
public class RestApiDemoMostolesBackend2026Application {

	public static void main(String[] args) {
		SpringApplication.run(RestApiDemoMostolesBackend2026Application.class, args);
	}

}
