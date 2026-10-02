package com.satyam.quotation.service.impl;

import com.satyam.quotation.dto.ProductListDTO;
import com.satyam.quotation.exception.BadRequestException;
import com.satyam.quotation.exception.ResourceNotFoundException;
import com.satyam.quotation.model.Product;
import com.satyam.quotation.repository.CompanyRepository;
import com.satyam.quotation.repository.ProductRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.ProductService;
import org.springframework.cache.CacheManager;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Service
public class ProductServiceImpl implements ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final CacheManager cacheManager;

    public ProductServiceImpl(ProductRepository productRepository,
                              UserRepository userRepository,
                              CompanyRepository companyRepository,
                              CacheManager cacheManager) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
        this.cacheManager = cacheManager;
    }

    // Evict both full-entity cache keys AND lightweight list cache keys so both
    // the old full-ProductDTO responses and the new ProductListDTO responses
    // are invalidated consistently on every create / update / delete.
    private void evictProductCache(Long companyId, Long createdBy) {
        var cache = cacheManager.getCache("products");
        if (cache != null) {
            // Full-entity keys
            if (companyId != null) cache.evict("company:" + companyId);
            if (createdBy  != null) cache.evict("user:"    + createdBy);
            cache.evict("all");
            // Lightweight list keys (used by the new list endpoints)
            if (companyId != null) cache.evict("list:company:" + companyId);
            if (createdBy  != null) cache.evict("list:user:"    + createdBy);
            cache.evict("list:all");
        }
    }

    @Override
    @Transactional
    public Product createProduct(Product product, Long userId, Long companyId) {
        validateImageSize(product.getImagePath());
        product.setCreatedBy(userId);

        if (companyId != null) {
            product.setCompany(
                    companyRepository.findById(companyId)
                            .orElseThrow(() -> new ResourceNotFoundException("Company not found"))
            );
        }

        product.setActive(true);
        product.setCreatedAt(LocalDateTime.now());
        product.setUpdatedAt(LocalDateTime.now());
        // Sync hsnCode from hsnSacCode so both columns are always populated
        if ((product.getHsnCode() == null || product.getHsnCode().isBlank()) && product.getHsnSacCode() != null && !product.getHsnSacCode().isBlank()) {
            product.setHsnCode(product.getHsnSacCode());
        }
        if ((product.getHsnSacCode() == null || product.getHsnSacCode().isBlank()) && product.getHsnCode() != null && !product.getHsnCode().isBlank()) {
            product.setHsnSacCode(product.getHsnCode());
        }

        Product saved = productRepository.save(product);
        evictProductCache(companyId, userId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id).filter(Product::getActive);
    }

    // ── Image-only fetch ──────────────────────────────────────────────────────
    // Returns only the imagePath string. Used by GET /api/products/{id}/image.
    // The full product entity is NOT loaded into the Caffeine cache from here,
    // keeping memory pressure low.
    @Override
    @Transactional(readOnly = true)
    public Optional<String> getProductImage(Long id) {
        return productRepository.findById(id)
                .filter(Product::getActive)
                .map(Product::getImagePath);
    }

    // ── Lightweight list methods (no imagePath) ───────────────────────────────
    // These map Product → ProductListDTO, deliberately skipping imagePath.
    // The Caffeine cache key uses a "list:" prefix so it does NOT collide with
    // the full-entity cache keys used internally by create/update/delete.

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'list:company:' + #companyId")
    public List<ProductListDTO> getProductListByCompany(Long companyId) {
        return productRepository.findByCompanyId(companyId)
                .stream()
                .map(this::toListDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'list:user:' + #userId")
    public List<ProductListDTO> getProductListByUser(Long userId) {
        return productRepository.findByCreatedBy(userId)
                .stream()
                .map(this::toListDTO)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'list:all'")
    public List<ProductListDTO> getAllProductList() {
        return productRepository.findAllActive()
                .stream()
                .map(this::toListDTO)
                .toList();
    }

    /**
     * Maps a Product entity → ProductListDTO.
     * imagePath is explicitly NOT copied — that is the whole point of this DTO.
     */
    private ProductListDTO toListDTO(Product p) {
        ProductListDTO dto = new ProductListDTO();
        dto.setId(p.getId());
        dto.setProductName(p.getProductName());
        dto.setProductCode(p.getProductCode());
        dto.setHsnSacCode(p.getHsnSacCode());
        dto.setHsnCode(p.getHsnCode());
        dto.setBrand(p.getBrand());
        dto.setCategory(p.getCategory());
        dto.setDescription(p.getDescription());
        dto.setPrice(p.getPrice());
        dto.setPurchasePrice(p.getPurchasePrice());
        dto.setDiscountPercentage(p.getDiscountPercentage());
        dto.setTaxType(p.getTaxType());
        dto.setTaxPercentage(p.getTaxPercentage());
        dto.setUnit(p.getUnit());
        dto.setQuantity(p.getQuantity());
        dto.setExpiryDate(p.getExpiryDate());
        dto.setNetWeight(p.getNetWeight());
        dto.setStackWeight(p.getStackWeight());
        dto.setCbm(p.getCbm());
        if (p.getCompany() != null) {
            dto.setCompanyId(p.getCompany().getId());
            dto.setCompanyName(p.getCompany().getCompanyName());
        }
        dto.setPurchasePriceCurrency(p.getPurchasePriceCurrency());
        dto.setPurchasePriceUsd(p.getPurchasePriceUsd());
        dto.setShippingCostUsd(p.getShippingCostUsd());
        dto.setDutyGstPercent(p.getDutyGstPercent());
        dto.setClearanceCost(p.getClearanceCost());
        dto.setDomesticShippingInr(p.getDomesticShippingInr());
        // imagePath intentionally omitted
        return dto;
    }

    // ── Full entity list methods (kept for internal use by cache eviction) ────

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'company:' + #companyId")
    public List<Product> getProductsByCompany(Long companyId) {
        return productRepository.findByCompanyId(companyId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'user:' + #userId")
    public List<Product> getProductsByUser(Long userId) {
        return productRepository.findByCreatedBy(userId);
    }

    @Override
    @Transactional(readOnly = true)
    @Cacheable(value = "products", key = "'all'")
    public List<Product> getAllProducts() {
        return productRepository.findAllActive();
    }

    @Override
    @Transactional
    public Product updateProduct(Long id, Product updatedProduct, Long userId) {
        validateImageSize(updatedProduct.getImagePath());
        Product product = productRepository.findById(id)
                .filter(Product::getActive)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setProductName(updatedProduct.getProductName());
        product.setProductCode(updatedProduct.getProductCode());
        product.setBrand(updatedProduct.getBrand());
        product.setCategory(updatedProduct.getCategory());
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setPurchasePrice(updatedProduct.getPurchasePrice());
        product.setUnit(updatedProduct.getUnit());
        product.setQuantity(updatedProduct.getQuantity());
        product.setDiscountPercentage(updatedProduct.getDiscountPercentage());
        product.setTaxType(updatedProduct.getTaxType());
        product.setTaxPercentage(updatedProduct.getTaxPercentage());
        product.setExpiryDate(updatedProduct.getExpiryDate());
        product.setImagePath(updatedProduct.getImagePath());
        product.setHsnSacCode(updatedProduct.getHsnSacCode());
        product.setHsnCode(updatedProduct.getHsnSacCode());
        // CBM and net weight — optional export/logistics fields
        product.setCbm(updatedProduct.getCbm());
        product.setNetWeight(updatedProduct.getNetWeight());
        product.setStackWeight(updatedProduct.getStackWeight());
        // International Purchase (USD) fields
        product.setPurchasePriceCurrency(updatedProduct.getPurchasePriceCurrency());
        product.setPurchasePriceUsd(updatedProduct.getPurchasePriceUsd());
        product.setShippingCostUsd(updatedProduct.getShippingCostUsd());
        product.setDutyGstPercent(updatedProduct.getDutyGstPercent());
        product.setClearanceCost(updatedProduct.getClearanceCost());
        product.setDomesticShippingInr(updatedProduct.getDomesticShippingInr());
        product.setUpdatedAt(LocalDateTime.now());
        product.setUpdatedBy(userId);

        Product saved = productRepository.save(product);
        Long companyId = saved.getCompany() != null ? saved.getCompany().getId() : null;
        evictProductCache(companyId, saved.getCreatedBy());
        return saved;
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        Long companyId = product.getCompany() != null ? product.getCompany().getId() : null;
        Long createdBy = product.getCreatedBy();

        product.setActive(false);
        product.setDeletedAt(LocalDateTime.now());
        product.setDeletedBy(userId);
        productRepository.save(product);

        evictProductCache(companyId, createdBy);
    }

    // base64 string of 2MB image is ~2.73MB — limit raw bytes to 2MB
    private void validateImageSize(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            long approxBytes = (long) (imagePath.length() * 0.75);
            if (approxBytes > 2 * 1024 * 1024) {
                throw new BadRequestException("Image size must be under 2MB");
            }
        }
    }
}
