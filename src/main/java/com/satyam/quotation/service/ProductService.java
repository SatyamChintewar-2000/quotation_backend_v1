package com.satyam.quotation.service;

import com.satyam.quotation.model.Product;

import java.util.List;
import java.util.Optional;

public interface ProductService {
    
    Product createProduct(Product product, Long userId, Long companyId);
    
    Optional<Product> getProductById(Long id);
    
    List<Product> getProductsByCompany(Long companyId);
    
    List<Product> getProductsByUser(Long userId);
    
    List<Product> getAllProducts();
    
    Product updateProduct(Long id, Product product, Long userId);
    
    void deleteProduct(Long id, Long userId);
}
