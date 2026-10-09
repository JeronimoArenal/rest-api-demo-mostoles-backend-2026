package com.example.controllers;

import java.io.IOException;
import java.util.List;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
import org.springframework.hateoas.Link;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.example.entities.Product;
import com.example.services.ProductService;
import com.example.utilities.FileDownloadUtil;
import com.example.utilities.FileUploadUtil;
import com.example.utilities.FileUtil;

import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import static org.springframework.hateoas.server.mvc.WebMvcLinkBuilder.*;

/**
 * La anotacion @RestController es para que todos los metodos que van a ser
 * creados dentro de este controlador y reciben peticiones a través del
 * protocolo HTTP, mediante los verbos correspondientes (GET, POST, PUT, DELETE,
 * PATCH, etc.) devuelvan o reciban datos en formato de JSON (JavaScript Object
 * Notation)
 */

@RestController

/**
 * Una API REST esta orientada al recurso, es decir, que el controlador necesita
 * que se le especifique que recurso va a responder, por ejemplo en esto seria
 * /products, y en dependencia del verbo del protocolo HTTP se estaria haciendo
 * una peticion (request) contreta. Por ejemplo: Si el verbo es GET, significa
 * que estamos solicitando todos los productos al recurso /products. Si el verbo
 * es POST significa que queremos recibir un producto en formato JSON, en el
 * cuerpo de la peticion (request) y persistirlo (guardarlo) en las tablas
 * correspondientes
 */
@RequestMapping("/products")
@RequiredArgsConstructor
public class ProductController {

	private final ProductService productService;
	private final FileUploadUtil fileUploadUtil;
	private final FileDownloadUtil fileDownloadUtil;
	private final FileUtil fileUtil;

	/**
	 * 
	 * IMPORTANTE!!!
	 * 
	 * Con HATEOAS cada respuesta lleva enlaces hipermedia ("_links") generados
	 * con EntityModel (recurso individual) o CollectionModel (coleccion),
	 * mediante linkTo(methodOn(...)), para que el cliente pueda navegar por la
	 * API sin conocer las URLs de memoria.
	 */

	/**
	 * El metodo siguiente va a responder a una peticion (request) del tipo:
	 * 
	 * http://localhost:8080/products?page=0&size=3
	 * 
	 * Donde los parametros page y size seran utilizados para la paginacion, y no
	 * seran requeridos, es decir, que no son obligatorios que se suministren. Y en
	 * caso de NO ser suministrados (page y size), los productos se van a devolver
	 * ordenados.
	 * 
	 */
	//....................... dameProductos .......................................
	@GetMapping
	@PreAuthorize("hasRole('ADMIN') or hasRole('USER')")
	public CollectionModel<EntityModel<Product>> dameProductos(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "size", required = false) Integer size) {

		Sort sort = Sort.by("name");
		List<Product> products = null;

		// Comprobar si en la peticion (request) me han suministrado los parametros page
		// y size
		if (page != null && size != null) {

			Pageable pageable = PageRequest.of(page, size, sort);

			// Implica devolver los productos paginados, es decir, una pagina de Product
			products = productService.findAll(pageable).getContent();

		} else {

			// Devolver los productos ordenados, por nombre (name), por ejemplo
			products = productService.findAll(sort);
		}

		// Envolver cada producto en un EntityModel con sus enlaces hipermedia
		List<EntityModel<Product>> entityModels = products.stream()
				.map(product -> EntityModel.of(product,
						linkTo(methodOn(ProductController.class).findProductById(product.getId())).withSelfRel(),
						linkTo(methodOn(ProductController.class).dameProductos(page, size)).withRel("products")))
				.collect(Collectors.toList());

		// Enlaces de la coleccion: a si misma y un enlace "all-products"
		Link selfLink = linkTo(methodOn(ProductController.class).dameProductos(page, size)).withSelfRel();
		Link allProductsLink = linkTo(methodOn(ProductController.class).dameProductos(null, null))
				.withRel("all-products");

		return CollectionModel.of(entityModels, selfLink, allProductsLink);
	}

	/**
	 * El metodo siguiente recupera un Producto por el id que se recibe como una
	 * variable en la ruta, mediante un end point (url o uri) que tiene el formato
	 * siguiente:
	 * 
	 * http://localhost:8080/productos/1
	 * 
	 * Donde el valor 1 al final del end point seria el id del producto
	 */
	//....................... findProductById .......................................
	@GetMapping("/{id}")
	@PreAuthorize("hasAnyRole('ADMIN', 'USER')")
	public ResponseEntity<EntityModel<Product>> findProductById(@PathVariable(name = "id",
			required = true) int product_id) {

		try {
			Product product = productService.findById(product_id);

			if (product != null) {

				// Envolver el producto en un EntityModel con enlace a si mismo (self) y a
				// la coleccion completa
				EntityModel<Product> entityModel = EntityModel.of(product,
						linkTo(methodOn(ProductController.class).findProductById(product_id)).withSelfRel(),
						linkTo(methodOn(ProductController.class).dameProductos(null, null)).withRel("all-products"));

				return ResponseEntity.ok(entityModel);
			}

			return ResponseEntity.notFound().build();

		} catch (DataAccessException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Metodo que recibe por POST el Producto para ser persistido, guardado, y que
	 * valida el JSON recibido, para comprobar si esta bien formado o no
	 * 
	 * El producto ya no viene ocupando todo el cuerpo de la peticion (request),
	 * sino una parte, y la otra parte la ocupa la imagen del producto
	 * 
	 * @throws IOException
	 * 
	 */
	//....................... saveProduct .......................................
	@PostMapping(consumes = "multipart/form-data")
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EntityModel<Product>> saveProduct(@Valid @RequestPart Product product, BindingResult result,
			@RequestPart(name = "file", required = false) MultipartFile imagenDelProducto) throws IOException {

		// Comprobar si hay errores en el producto recibido
		if (result.hasErrors()) {
			return ResponseEntity.badRequest().build();
		}

		// Si hemos recibido imagen del producto, la guardamos en el file system y
		// guardamos la referencia en el producto
		if (imagenDelProducto != null && !imagenDelProducto.isEmpty()) {

			String fileCode = fileUploadUtil.saveFile(imagenDelProducto.getOriginalFilename(), imagenDelProducto);
			product.setProductImage(fileCode + '-' + imagenDelProducto.getOriginalFilename());
		}

		try {
			Product productoPersistido = productService.save(product);

			// Envolver el producto persistido en un EntityModel con sus enlaces
			EntityModel<Product> entityModel = EntityModel.of(productoPersistido,
					linkTo(methodOn(ProductController.class).findProductById(productoPersistido.getId())).withSelfRel(),
					linkTo(methodOn(ProductController.class).dameProductos(null, null)).withRel("all-products"));

			return ResponseEntity.status(HttpStatus.CREATED).body(entityModel);

		} catch (DataAccessException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

	/**
	 * Metodo que recupera la imagen de un producto, dado el codigo que tiene como
	 * prefijo el nombre de la imagen
	 */
	//....................... downloadFile .......................................
	@GetMapping("/fileDownLoad/{fileCode}")
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<?> downloadFile(@PathVariable String fileCode) {

		Resource resource = null;

		try {
			resource = fileDownloadUtil.getFileAsResource(fileCode);
		} catch (IOException ioe) {
			return ResponseEntity.internalServerError().build();
		}

		if (resource == null)
			return new ResponseEntity<>("Imagen del producto no encontrada ", HttpStatus.NOT_FOUND);
		/**
		 * Si estamos en este punto quiere decir que el fichero (imagen del producto) ha
		 * sido encontrado y podemos enviarlo como respuesta a la peticion, como un
		 * fichero adjunto en el cuerpo de la respuesta
		 */

		String contentType = "application/octet-stream";
		String headerValue = "attachment; fileName=\"" + resource.getFilename() + "\"";

		return ResponseEntity.ok().contentType(MediaType.parseMediaType(contentType))
				.header(HttpHeaders.CONTENT_DISPOSITION, headerValue).body(resource);
	}

	/**
	 * Metodo que actualiza (update) un producto dado el id del mismo.
	 * 
	 * Es basicamente igual al metodo que persiste el producto. Respondera a una
	 * peticion del tipo siguiente, por ejemplo:
	 * 
	 * http://localhost:8080/productos/3
	 * 
	 * Y no habra ambiguedad con el metodo de buscar un producto por el id, porque
	 * el verbo utilizado del protocolo HTTP sera diferente, PUT en este caso.
	 * 
	 */
	//....................... updateProduct .......................................
	@PutMapping(value = "/{id}", consumes = "multipart/form-data")
	@Transactional
	@PreAuthorize("hasRole('ADMIN')")
	public ResponseEntity<EntityModel<Product>> updateProduct(@Valid @RequestPart Product product, BindingResult result,
			@RequestPart(name = "file", required = false) MultipartFile imagenDelProducto,
			@PathVariable(name = "id", required = true) int product_id) throws IOException {

		// comprobar errores de validación
		if (result.hasErrors()) {
			return ResponseEntity.badRequest().build();
		}

		// Buscar el producto que se va a actualizar para comprobar que existe
		Product productoParaActualizar = productService.findById(product_id);

		if (productoParaActualizar == null) {
			return ResponseEntity.notFound().build();
		}

		if (imagenDelProducto != null && !imagenDelProducto.isEmpty()) {

			// Si el producto que se va a actualizar tiene imagen, eliminarla del file
			// system
			if (productoParaActualizar.getProductImage() != null) {
				fileUtil.eliminarArchivo(productoParaActualizar.getProductImage());
			}

			// Guardar la nueva imagen y actualizar la referencia en el producto
			String fileCode = fileUploadUtil.saveFile(imagenDelProducto.getOriginalFilename(),
					imagenDelProducto);

			product.setProductImage(fileCode + '-' + imagenDelProducto.getOriginalFilename());
		}

		try {
			product.setId(product_id);
			Product productoAGuardar = productService.save(product);

			// Envolver el producto actualizado en un EntityModel con sus enlaces
			EntityModel<Product> entityModel = EntityModel.of(productoAGuardar,
					linkTo(methodOn(ProductController.class).findProductById(productoAGuardar.getId())).withSelfRel(),
					linkTo(methodOn(ProductController.class).dameProductos(null, null)).withRel("all-products"));

			return ResponseEntity.ok(entityModel);

		} catch (DataAccessException e) {
			return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
		}
	}

    /**
     * Metodo para eliminar un producto dado el id
     */
	//....................... deleteProducto .......................................
	@DeleteMapping("/{id}")
    @Transactional
	@PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<EntityModel<Product>> deleteProducto(@PathVariable Integer id) {

        try {

            // recuperar el producto y comprobar si hay foto y eliminar el archivo
            Product productToDelete = productService.findById(id);

            if (productToDelete == null) {
                return ResponseEntity.notFound().build();
            }

            if (productToDelete.getProductImage() != null) {
                fileUtil.eliminarArchivo(productToDelete.getProductImage());
            }

            productService.delete(productToDelete);

            // Devolver el producto eliminado envuelto en un EntityModel, con enlace a la
            // coleccion de productos
            EntityModel<Product> entityModel = EntityModel.of(productToDelete,
                    linkTo(methodOn(ProductController.class).dameProductos(null, null)).withRel("all-products"));

            return ResponseEntity.ok(entityModel);
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}