package com.satyam.quotation.service.impl;

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

    // Evict only the specific company's product cache — other companies unaffected
    private void evictProductCache(Long companyId, Long createdBy) {
        var cache = cacheManager.getCache("products");
        if (cache != null) {
            if (companyId != null) cache.evict("company:" + companyId);
            if (createdBy  != null) cache.evict("user:"    + createdBy);
            cache.evict("all");
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

        Product saved = productRepository.save(product);
        evictProductCache(companyId, userId);
        return saved;
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id).filter(Product::getActive);
    }

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
        product.setDescription(updatedProduct.getDescription());
        product.setPrice(updatedProduct.getPrice());
        product.setUnit(updatedProduct.getUnit());
        product.setQuantity(updatedProduct.getQuantity());
        product.setDiscountPercentage(updatedProduct.getDiscountPercentage());
        product.setTaxType(updatedProduct.getTaxType());
        product.setTaxPercentage(updatedProduct.getTaxPercentage());
        product.setExpiryDate(updatedProduct.getExpiryDate());
        product.setImagePath(updatedProduct.getImagePath());
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
