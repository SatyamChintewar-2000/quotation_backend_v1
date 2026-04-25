package com.satyam.quotation.service.impl;

import com.satyam.quotation.exception.BadRequestException;
import com.satyam.quotation.exception.ResourceNotFoundException;
import com.satyam.quotation.model.Product;
import com.satyam.quotation.repository.CompanyRepository;
import com.satyam.quotation.repository.ProductRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.service.ProductService;
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

    public ProductServiceImpl(ProductRepository productRepository,
                             UserRepository userRepository,
                             CompanyRepository companyRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
        this.companyRepository = companyRepository;
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

        return productRepository.save(product);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Product> getProductById(Long id) {
        return productRepository.findById(id)
                .filter(Product::getActive);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByCompany(Long companyId) {
        return productRepository.findByCompanyId(companyId).stream()
                .filter(Product::getActive)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getProductsByUser(Long userId) {
        return productRepository.findAll().stream()
                .filter(p -> p.getCreatedBy() != null && 
                           p.getCreatedBy().equals(userId) && 
                           p.getActive())
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<Product> getAllProducts() {
        return productRepository.findAll().stream()
                .filter(Product::getActive)
                .toList();
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

        return productRepository.save(product);
    }

    // base64 string of 2MB image is ~2.73MB — limit raw bytes to 2MB = ~2.73MB base64
    private void validateImageSize(String imagePath) {
        if (imagePath != null && !imagePath.isEmpty()) {
            // base64 encoded size * 3/4 gives approximate original byte size
            long approxBytes = (long)(imagePath.length() * 0.75);
            if (approxBytes > 2 * 1024 * 1024) {
                throw new BadRequestException("Image size must be under 2MB");
            }
        }
    }

    @Override
    @Transactional
    public void deleteProduct(Long id, Long userId) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found"));

        product.setActive(false);
        product.setDeletedAt(LocalDateTime.now());
        product.setDeletedBy(userId);

        productRepository.save(product);
    }
}
