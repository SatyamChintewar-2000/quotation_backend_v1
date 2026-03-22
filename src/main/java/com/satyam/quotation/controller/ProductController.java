package com.satyam.quotation.controller;

import com.satyam.quotation.dto.ProductDTO;
import com.satyam.quotation.dto.ProductRequestDTO;
import com.satyam.quotation.mapper.ProductMapper;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final ProductMapper productMapper;

    public ProductController(ProductService productService,
                            ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper = productMapper;
    }

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO createProduct(
            @Valid @RequestBody ProductRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} creating product {}", user.getUserId(), request.getProductName());

        var product = productMapper.toEntity(request);
        var savedProduct = productService.createProduct(
                product,
                user.getUserId(),
                user.getCompanyId()
        );

        return productMapper.toDto(savedProduct);
    }

    @GetMapping
    public List<ProductDTO> getProducts(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Fetching products for user {}, role {}", user.getUserId(), user.getRole());

        List<com.satyam.quotation.model.Product> products;

        if ("SUPER_ADMIN".equals(user.getRole())) {
            products = productService.getAllProducts();
        } else if ("CLIENT".equals(user.getRole()) && user.getCompanyId() != null) {
            products = productService.getProductsByCompany(user.getCompanyId());
        } else {
            products = productService.getProductsByUser(user.getUserId());
        }

        return products.stream()
                .map(productMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductDTO getProduct(@PathVariable Long id) {
        return productService.getProductById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Product not found with id: " + id));
    }

    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable Long id,
            @Valid @RequestBody ProductRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} updating product {}", user.getUserId(), id);

        var product = productMapper.toEntity(request);
        var updatedProduct = productService.updateProduct(id, product, user.getUserId());

        return productMapper.toDto(updatedProduct);
    }

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @PathVariable Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} deleting product {}", user.getUserId(), id);

        productService.deleteProduct(id, user.getUserId());
    }
}
