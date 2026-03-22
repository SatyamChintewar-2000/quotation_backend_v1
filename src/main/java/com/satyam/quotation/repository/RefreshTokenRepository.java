package com.satyam.quotation.repository;

import com.satyam.quotation.model.RefreshToken;
import com.satyam.quotation.model.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefreshTokenRepository extends JpaRepository<RefreshToken, Long> {
    
    Optional<RefreshToken> findByToken(String token);
    
    void deleteByUser(User user);
    
    void deleteByToken(String token);
}
