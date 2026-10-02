package com.example.controllers;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.hamcrest.CoreMatchers.is;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import com.example.spring_security_jwt.payload.request.LoginRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase;
import org.springframework.boot.jdbc.test.autoconfigure.AutoConfigureTestDatabase.Replace;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Sort;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import com.example.entities.Presentation;
import com.example.entities.Product;
import com.example.services.ProductService;
import com.example.utilities.FileDownloadUtil;
import com.example.utilities.FileUploadUtil;
import com.example.utilities.FileUtil;

import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;

//@WebMvcTest(ProductController.class)

/**
 * La anotacion anterior es la recomendada para implementar test de Integracion,
 * a la capa de controladores que conlleva la realizacion de peticiones HTTP.
 * 
 * Esta anotacion no serviria si tuviesemos implementada la seguridad con Spring
 * Security porque no carga todo el contexto de Spring. Cuando se implemente la
 * seguridad, comentaremos esta anotacion y utilizaremos @SpringBootTest
 */
@SpringBootTest

/*
 * La siguiente anotacion se utiliza cuando queremos utilizar una base de datos
 * real, que no sea H2 Database, MySQL por ejemplo, y que al terminar la prueba
 * se deje la base de datos tal y como estaba
 */
@AutoConfigureTestDatabase(replace = Replace.NONE)

/**
 * Se necesita MockMvc para realizar peticiones a los end points, lo cual
 * suministra y configura la anotacion siguiente
 */
@AutoConfigureMockMvc
class ProductControllerTest {

	@Autowired
	MockMvc mockMvc;

	@MockitoBean
	ProductService productService;

	@MockitoBean
	FileUploadUtil fileUploadUtil;

	@MockitoBean
	FileDownloadUtil fileDownloadUtil;
	
	@MockitoBean
	FileUtil fileUtil;
	
	@Autowired
	ObjectMapper objectMapper;
	
	List<Product> products = new ArrayList<>();
	Presentation presentation1, presentation2;
	Product product1, product2;
	
	@BeforeEach
	void setUp() {
		/**
		 * Necesitamos obtener un token valido para presentarlo en cada test
		 */
		LoginRequest logginRequest = LoginRequest.builder()
				.username("admin1")
				.password("123456")
				.build();

		/**
		 * El objeto anterior, tiene que ser convertido a formato JSON, para llo caul utillizamos el ObjectMapper
		 * que convierte a Strin el formato JSON
		 */

		String jsonLLoginRequest = objectMapper.writeValueAsString(logginRequest);

		presentation1 = Presentation.builder()
				.name("decenas")
				.description("Por decenas")
				.build();
		
		presentation2 = Presentation.builder()
				.name("unidades")
				.description("Por unidades")
				.build();
			
		product1 = Product.builder()
				.name("Camara")
				.description("HP Camara")
				.price(new BigDecimal(500))
				.stock(1900)
				.productImage(null)
				.presentation(presentation1)
				.build();
		
			
		product2 = Product.builder()
				.name("Frigorifico")
				.description("General Electric")
				.price(new BigDecimal(2500))
				.stock(3900)
				.productImage(null)
				.presentation(presentation2)
				.build();
		
		products.add(product1);
		products.add(product2);
	}

	@Test
	@DisplayName("Controller Test que recupera todos los productos")
	void testFindAll() throws Exception {

		// given
		
		given(productService.findAll(Sort.by("name")))
			.willReturn(products);

		// when => Realizar la peticion (request) HTTP, mediante el metodo GET
		// al end point de products ("/products"). Aqui se utiliza MockMvc

		ResultActions response = mockMvc
				.perform(get("/products")
				.accept(MediaType.APPLICATION_JSON));
		// then

		response.andExpect(status().isOk()).andDo(print())
				.andExpect(jsonPath("$.products.size()",
						is(products.size())));

	}

	@Test
	@DisplayName("Controller Test para Persistir un Producto")
	void testSaveProduct()  {
		
		// given
		given(productService.save(any(Product.class)))
			.willAnswer(invocation -> invocation.getArgument(0));
		
		// when
		
		/* Convertir el producto a formato JSON, es decir, una cadena (String)
		 * en formato de JSON, lo cual hace el objectMapper que hemos inyectado como 
		 * dependencia al principio de la clase bajo Test */
		
		String jsonStringProduct = objectMapper.writeValueAsString(product1);
		
		MockMultipartFile bytesArrayProduct = new MockMultipartFile(
				    "product", 
				    null, 
				    "application/json", 
				    jsonStringProduct.getBytes());
		
		try {
				mockMvc
				    .perform(multipart("/products")
					.file(bytesArrayProduct)
					.file("file", null))			    
				    	.andDo(print())
				    	.andExpect(status().isCreated())
				    	.andExpect(jsonPath("$.product.name",
		  			is(product1.getName())));
		  	
		  
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		// then
		
	}

	@Test
	@DisplayName("Controller Test para recuperar un producto por su ID")
	void testRecuperarProductoPorSuID() throws Exception {
		
		// given
		
		int productId = 1;
		
		given(productService.findById(productId))
			.willReturn(product1);
		
		// when
		mockMvc.perform(get("/products/{id}",
				productId))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(jsonPath("$['producto encontrado: '].name",
						is(product1.getName())));
	}

	@Test
	@DisplayName("Controller Test Producto no encontrado")
	void testProductoNoEncontrado() throws Exception {
		
		// given
		given(productService.findById(20)).willReturn(null);
		
		// when
		
		mockMvc.perform(get("/products/{id}", 20))
			.andDo(print())
			.andExpect(status().isNotFound());
	}
	
    @Test 
    @DisplayName("Controller Test que actualiza un producto con su imagen")
    void testUpdateProduct() throws Exception{

        //given
        int id = 1;
        given(productService.findById(id)).willReturn(product1);
        given(productService.save(any(Product.class)))
                .willAnswer(invocation -> invocation.getArgument(0));

        //when
        String jsonStringProduct = objectMapper.writeValueAsString(product1);

        MockMultipartFile bytesArrayProduct = new MockMultipartFile("product", 
                            null,
                            "application/json",
                            jsonStringProduct.getBytes());

        ResultActions response = this.mockMvc
        		.perform(multipart("/products/{id}", id)
                        .with(request -> {
                            request.setMethod("PUT");
                            return request;
                        })
                        .file("image", null)
                        .file(bytesArrayProduct));

        //then
        response.andDo(print())
            .andExpect(status().isOk())
            .andExpect(jsonPath("$['producto actualizado: '].name",
            		is(product1.getName())))
            .andExpect(jsonPath("$['producto actualizado: '].description",
            		is(product1.getDescription())));
        

    }
    
    @Test 
    @DisplayName("Controller Test que elimina un producto")
    void testDeleteProduct() throws Exception{

        //given
        int ProductId = 1;

        given(productService.findById(ProductId)).willReturn(product1);
        doNothing().when(productService).delete(product1);

        //when
        mockMvc.perform(delete("/products/{id}", ProductId))
                .andExpect(status().isOk());

    }
	
}











