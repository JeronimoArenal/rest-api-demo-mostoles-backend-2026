package com.example.dao;

import static org.assertj.core.api.Assertions.*;

import java.math.BigDecimal;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.data.jpa.test.autoconfigure.DataJpaTest;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;

import com.example.entities.Presentation;
import com.example.entities.Product;

/**
 * Esta anotación se utiliza para realizar pruebas de la capa de persistencia,
 * principalmente de las entidades JPA y los repositorios.
 *
 * Carga un contexto de Spring limitado a los componentes relacionados
 * con JPA, en lugar de cargar toda la aplicación.
 *
 * Por defecto, las pruebas se ejecutan dentro de una transacción que
 * se revierte al finalizar cada prueba. Por este motivo, normalmente
 * no es necesario añadir @Transactional a cada método de prueba.
 */
@DataJpaTest

/**
 * Indica a Spring Boot que no sustituya la base de datos configurada
 * por una base de datos embebida, como H2.
 *
 * Permite utilizar la base de datos configurada para las pruebas, por ejemplo PostgreSQL o MySQL.
 *
 * Importante: la base de datos configurada debe estar disponible y correctamente preparada.
 * Además, @DataJpaTest no garantiza que los cambios realizados fuera de la transacción de prueba se reviertan.
 */
@AutoConfigureTestDatabase(replace = Replace.NONE)
class ProductDaoTest {
	
	@Autowired
	private PresentationDao presentationDao;
	
	@Autowired
	private ProductDao productDao;
	
	private Presentation presentacionPorUnidades;
	private Presentation presentacionPorDecenas;
	
	private Product product0;
	
	@BeforeEach
	void setUp() {
		
		presentacionPorUnidades = Presentation.builder()
				.name("unidad")
				.description("por unidades")
				.build();
		
		presentacionPorDecenas = Presentation.builder()
				.name("decenas")
				.description("por decenas")
				.build();
		
	}

	@Test
	@DisplayName("Test para probar agregar, salvar o persistir un producto")
	void testSaveProduct() {
		
		/* Antes de poder probar que sucede cuando se persiste un producto hay que
		 * crearlo y como esto va a ser necesario para todos los demas metodos de prueba
		 * lo vamos a crear fuera de los metodos. Realmente lo que hemos creado son 
		 * las presentaciones */
		
		// given
		
		Presentation presentation0 = presentationDao.save(presentacionPorUnidades);
		
		product0 = Product.builder()
				.name("Google Pixel 11 Pro")
				.description("Google Smart Phone")
				.price(new BigDecimal(900))
				.presentation(presentation0)
				.build();
		// when
		Product productoGuardado = productDao.save(product0);
		
		// then
		assertThat(productoGuardado).isNotNull();
		assertThat(productoGuardado.getId()).isGreaterThan(0);
		
	}

}
