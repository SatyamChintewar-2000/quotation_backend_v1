package com.satyam.quotation.controller;

import com.satyam.quotation.dto.ProductDTO;
import com.satyam.quotation.dto.ProductListDTO;
import com.satyam.quotation.dto.ProductRequestDTO;
import com.satyam.quotation.mapper.ProductMapper;
import com.satyam.quotation.security.CustomUserDetails;
import com.satyam.quotation.service.ProductService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private static final Logger log = LoggerFactory.getLogger(ProductController.class);

    private final ProductService productService;
    private final ProductMapper  productMapper;

    public ProductController(ProductService productService,
                             ProductMapper productMapper) {
        this.productService = productService;
        this.productMapper  = productMapper;
    }

    // ── CREATE ────────────────────────────────────────────────────────────────

    @PostMapping
    @ResponseStatus(HttpStatus.CREATED)
    public ProductDTO createProduct(
            @Valid @RequestBody ProductRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} creating product {}", user.getUserId(), request.getProductName());

        Long companyId = user.getCompanyId();
        if ("SUPER_ADMIN".equals(user.getRole())) {
            if (request.getCompanyId() == null) {
                throw new com.satyam.quotation.exception.BadRequestException(
                        "SUPER_ADMIN must specify a companyId when creating a product");
            }
            companyId = request.getCompanyId();
        }

        var product     = productMapper.toEntity(request);
        var savedProduct = productService.createProduct(product, user.getUserId(), companyId);
        return productMapper.toDto(savedProduct);
    }

    // ── LIST (lightweight — no imagePath) ────────────────────────────────────
    //
    // PERFORMANCE: This endpoint previously returned the full ProductDTO which
    // included a base64-encoded imagePath (up to 2 MB per product).  With 300-400
    // products per company that produced 60–800 MB JSON responses, causing 5–10
    // minute page-load times for clients on slower connections.
    //
    // It now returns ProductListDTO which omits imagePath completely.
    // Images are fetched on-demand via GET /api/products/{id}/image.

    @GetMapping
    public List<ProductListDTO> getProducts(Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("Fetching products for user {}, role {}", user.getUserId(), user.getRole());

        if ("SUPER_ADMIN".equals(user.getRole())) {
            var list = productService.getAllProductList();
            log.info("SUPER_ADMIN fetching all products: {} products found", list.size());
            return list;
        }

        // Both CLIENT and STAFF see products scoped to their company
        var list = productService.getProductListByCompany(user.getCompanyId());
        log.info("{} fetching products for company {}: {} products found",
                user.getRole(), user.getCompanyId(), list.size());
        return list;
    }

    // ── GET SINGLE (full — includes imagePath, used by edit modal) ───────────

    @GetMapping("/{id}")
    public ProductDTO getProduct(@PathVariable("id") Long id) {
        return productService.getProductById(id)
                .map(productMapper::toDto)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Product not found with id: " + id));
    }

    // ── GET IMAGE (lazy — called only when edit modal opens) ─────────────────
    //
    // Returns {"imagePath": "<base64>"} or {"imagePath": null} for the given
    // product id.  The frontend calls this only when the user clicks Edit on a
    // product row in ProductManagement, so images are transferred one-at-a-time
    // on demand rather than for the entire catalogue up-front.

    @GetMapping("/{id}/image")
    public ResponseEntity<Map<String, String>> getProductImage(@PathVariable("id") Long id) {
        // Verify the product exists and is active first
        productService.getProductById(id)
                .orElseThrow(() -> new com.satyam.quotation.exception.ResourceNotFoundException(
                        "Product not found with id: " + id));

        String imagePath = productService.getProductImage(id).orElse(null);
        return ResponseEntity.ok(Map.of(
                "id",        String.valueOf(id),
                "imagePath", imagePath != null ? imagePath : ""
        ));
    }

    // ── UPDATE ────────────────────────────────────────────────────────────────

    @PutMapping("/{id}")
    public ProductDTO updateProduct(
            @PathVariable("id") Long id,
            @Valid @RequestBody ProductRequestDTO request,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} updating product {}", user.getUserId(), id);

        var product        = productMapper.toEntity(request);
        var updatedProduct = productService.updateProduct(id, product, user.getUserId());
        return productMapper.toDto(updatedProduct);
    }

    // ── DELETE ────────────────────────────────────────────────────────────────

    @DeleteMapping("/{id}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public void deleteProduct(
            @PathVariable("id") Long id,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} deleting product {}", user.getUserId(), id);
        productService.deleteProduct(id, user.getUserId());
    }

    // ── BULK CREATE ───────────────────────────────────────────────────────────

    @PostMapping("/bulk")
    public Map<String, Object> bulkCreateProducts(
            @RequestBody List<ProductRequestDTO> requests,
            Authentication authentication) {

        CustomUserDetails user = (CustomUserDetails) authentication.getPrincipal();
        log.info("User {} bulk uploading {} products", user.getUserId(), requests.size());

        Long companyId = user.getCompanyId();
        int  created   = 0;
        int  failed    = 0;
        var  errors    = new java.util.ArrayList<String>();

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
        return Map.of("created", created, "failed", failed, "errors", errors);
    }
}
