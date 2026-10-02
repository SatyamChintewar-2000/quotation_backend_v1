package com.satyam.quotation.service;

import com.satyam.quotation.dto.ProductListDTO;
import com.satyam.quotation.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {

    Product createProduct(Product product, Long userId, Long companyId);

    Optional<Product> getProductById(Long id);

    // ── Lightweight list methods (no imagePath) ────────────────────────────
    // Use these for all list endpoints. Images are served via getProductImage().

    List<ProductListDTO> getProductListByCompany(Long companyId);

    List<ProductListDTO> getProductListByUser(Long userId);

    List<ProductListDTO> getAllProductList();

    // ── Image-only fetch ───────────────────────────────────────────────────
    // Returns only the imagePath string for a single product.
    // Called by GET /api/products/{id}/image when the edit modal opens.

    Optional<String> getProductImage(Long id);

    // ── Full entity methods (used internally by create/update/delete) ──────

    List<Product> getProductsByCompany(Long companyId);

    List<Product> getProductsByUser(Long userId);

    List<Product> getAllProducts();

    Product updateProduct(Long id, Product product, Long userId);

    void deleteProduct(Long id, Long userId);
}
