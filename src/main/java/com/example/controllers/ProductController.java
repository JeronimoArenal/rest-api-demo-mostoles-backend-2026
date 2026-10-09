package com.example.controllers;

import java.io.IOException;
import java.util.List;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.hateoas.CollectionModel;
import org.springframework.hateoas.EntityModel;
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

import com.example.dto.ProductoDto;
import com.example.dto.ProductoModelAssembler;
import com.example.entities.Product;
import com.example.services.ProductService;
import com.example.utilities.FileDownloadUtil;
import com.example.utilities.FileUploadUtil;
import com.example.utilities.FileUtil;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.transaction.Transactional;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

/**
 * La anotacion @RestController es para que todos los metodos que van a ser
 * creados dentro de este controlador y reciben peticiones a través del
 * protocolo HTTP, mediante los verbos correspondientes (GET, POST, PUT, DELETE,
 * PATCH, etc.) devuelvan o reciban datos en formato de JSON (JavaScript Object
 * Notation)
 */

@RestController
@Tag(name = "Productos", description = "CRUD de productos con HATEOAS (los de escritura requieren ROLE_ADMIN)")

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
	private final ProductoModelAssembler productoModelAssembler;

	/**
	 * 
	 * IMPORTANTE!!!
	 * 
	 * Con HATEOAS cada respuesta lleva enlaces hipermedia ("_links"). Para no
	 * repetir la construccion de los enlaces en cada metodo, esta centralizada en
	 * el componente ProductoModelAssembler (RepresentationModelAssemblerSupport),
	 * que envuelve cada ProductoDto en un EntityModel y le agrega:
	 * 
	 * - Los enlaces self (GET de un producto) y all-products (GET de la
	 *   coleccion), generados dinamicamente a partir del controlador con
	 *   linkTo(methodOn(...)).
	 * - Los affordances de las operaciones que se le pueden aplicar al recurso:
	 *   PUT (actualizar) y DELETE (eliminar) sobre cada producto, y POST
	 *   (crear) sobre la coleccion. Se sirven con el media type HAL-FORMS.
	 * - La paginacion: cuando dameProductos recibe page y size, el assembler
	 *   construye un PagedModel con la metadata de la pagina ("page") y los
	 *   enlaces de navegacion first, prev, self, next y last.
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
	public CollectionModel<EntityModel<ProductoDto>> dameProductos(
			@RequestParam(name = "page", required = false) Integer page,
			@RequestParam(name = "size", required = false) Integer size) {

		Sort sort = Sort.by("name");

		// Comprobar si en la peticion (request) me han suministrado los parametros page
		// y size
		if (page != null && size != null) {

			Pageable pageable = PageRequest.of(page, size, sort);

			// Respuesta paginada: PagedModel con la metadata de la pagina y los enlaces
			// de navegacion first/prev/self/next/last, construidos por el assembler
			Page<Product> paginaDeProductos = productService.findAll(pageable);
			return productoModelAssembler.toPagedModel(paginaDeProductos);

		} else {

			// Devolver los productos ordenados, por nombre (name), por ejemplo
			List<Product> products = productService.findAll(sort);

			// Coleccion completa con los enlaces de coleccion (self y all-products +
			// affordance POST para crear), construidos por el assembler
			return productoModelAssembler.toColeccion(products);
		}
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
	public ResponseEntity<EntityModel<ProductoDto>> findProductById(@PathVariable(name = "id",
			required = true) int product_id) {

		try {
			Product product = productService.findById(product_id);

			if (product != null) {

				// El assembler devuelve el EntityModel<ProductoDto> con el enlace self y
				// el enlace a la coleccion completa
				return ResponseEntity.ok(productoModelAssembler.toModel(product));
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
	public ResponseEntity<EntityModel<ProductoDto>> saveProduct(@Valid @RequestPart Product product, BindingResult result,
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

			// El assembler devuelve el EntityModel<ProductoDto> del producto persistido
			return ResponseEntity.status(HttpStatus.CREATED).body(productoModelAssembler.toModel(productoPersistido));

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
	public ResponseEntity<EntityModel<ProductoDto>> updateProduct(@Valid @RequestPart Product product, BindingResult result,
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

			// El assembler devuelve el EntityModel<ProductoDto> del producto actualizado
			return ResponseEntity.ok(productoModelAssembler.toModel(productoAGuardar));

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
    public ResponseEntity<EntityModel<ProductoDto>> deleteProducto(@PathVariable Integer id) {

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

            // El assembler devuelve el EntityModel<ProductoDto> del producto eliminado,
            // con su enlace a la coleccion de productos
            return ResponseEntity.ok(productoModelAssembler.toModel(productToDelete));
        } catch (DataAccessException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).build();
        }
    }

}