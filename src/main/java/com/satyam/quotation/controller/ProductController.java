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

        // Determine company ID:
        // - SUPER_ADMIN: use companyId from request (must be provided)
        // - Others: use company from logged-in user
        Long companyId = user.getCompanyId();
        if ("SUPER_ADMIN".equals(user.getRole())) {
            if (request.getCompanyId() == null) {
                throw new com.satyam.quotation.exception.BadRequestException(
                    "SUPER_ADMIN must specify a companyId when creating a product");
            }
            companyId = request.getCompanyId();
        }

        var product = productMapper.toEntity(request);
        var savedProduct = productService.createProduct(
                product,
                user.getUserId(),
                companyId
        );

        return productMapper.toDto(savedProduct);
    }

    @GetMapping
    public List<ProductDTO> getProducts(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("Fetching products for user {}, role {}", user.getUserId(), user.getRole());

        List<com.satyam.quotation.model.Product> products;

        if ("SUPER_ADMIN".equals(user.getRole())) {
            // SUPER_ADMIN can see ALL products from ALL companies
            products = productService.getAllProducts();
            log.info("SUPER_ADMIN fetching all products: {} products found", products.size());
        } else if ("CLIENT".equals(user.getRole())) {
            // CLIENT can see products from their company only
            products = productService.getProductsByCompany(user.getCompanyId());
            log.info("CLIENT fetching products for company {}: {} products found", user.getCompanyId(), products.size());
        } else {
            // STAFF can only see products they created
            products = productService.getProductsByUser(user.getUserId());
            log.info("STAFF fetching products created by them: {} products found", products.size());
        }

        return products.stream()
                .map(productMapper::toDto)
                .toList();
    }

    @GetMapping("/{id}")
    public ProductDTO getProduct(@PathVariable("id") Long id) {
        return productService.getProductById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Product not found with id: " + id));
    }

    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable("id") Long id,
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
            @PathVariable("id") Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} deleting product {}", user.getUserId(), id);

        productService.deleteProduct(id, user.getUserId());
    }
}
