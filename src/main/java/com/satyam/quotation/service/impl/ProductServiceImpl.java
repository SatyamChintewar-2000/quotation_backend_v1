package com.satyam.quotation.service.impl;

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
