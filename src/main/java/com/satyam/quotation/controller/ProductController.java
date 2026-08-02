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
            // STAFF sees all products from their company — same as CLIENT
            // They are part of the company and need to use all products to create quotations
            products = productService.getProductsByCompany(user.getCompanyId());
            log.info("STAFF fetching products for company {}: {} products found", user.getCompanyId(), products.size());
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

    /**
     * Bulk create products from a JSON array.
     * Images are optional (imagePath field). All other validations apply per item.
     * Returns count of successfully created products.
     */
    @PostMapping("/bulk")
    public java.util.Map<String, Object> bulkCreateProducts(
            @RequestBody java.util.List<ProductRequestDTO> requests,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();

        log.info("User {} bulk uploading {} products", user.getUserId(), requests.size());

        Long companyId = user.getCompanyId();

        int created = 0;
        int failed  = 0;
        java.util.List<String> errors = new java.util.ArrayList<>();

        for (int i = 0; i < requests.size(); i++) {
            ProductRequestDTO req = requests.get(i);
            try {
                if (req.getProductName() == null || req.getProductName().isBlank()) {
                    errors.add("Row " + (i + 2) + ": Product name is required");
                    failed++;
                    continue;
                }
                if (req.getPrice() == null || req.getPrice().compareTo(java.math.BigDecimal.ZERO) <= 0) {
                    errors.add("Row " + (i + 2) + ": Valid price is required for " + req.getProductName());
                    failed++;
                    continue;
                }
                if ("SUPER_ADMIN".equals(user.getRole())) {
                    companyId = req.getCompanyId() != null ? req.getCompanyId() : user.getCompanyId();
                }
                var product = productMapper.toEntity(req);
                // Images are optional in bulk upload — skip if not provided
                if (req.getImagePath() == null || req.getImagePath().isBlank()) {
                    product.setImagePath(null);
                }
                productService.createProduct(product, user.getUserId(), companyId);
                created++;
            } catch (Exception e) {
                errors.add("Row " + (i + 2) + ": " + e.getMessage());
                failed++;
            }
        }

        log.info("Bulk upload complete: {} created, {} failed", created, failed);
        return java.util.Map.of("created", created, "failed", failed, "errors", errors);
    }
}
