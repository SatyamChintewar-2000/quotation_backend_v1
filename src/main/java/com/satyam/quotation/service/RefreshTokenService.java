package com.satyam.quotation.service;

import com.satyam.quotation.model.RefreshToken;
import com.satyam.quotation.model.User;
import com.satyam.quotation.repository.RefreshTokenRepository;
import com.satyam.quotation.repository.UserRepository;
import com.satyam.quotation.security.JwtService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class RefreshTokenService {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;
    private final JwtService jwtService;

    public RefreshTokenService(RefreshTokenRepository refreshTokenRepository,
                               UserRepository userRepository,
                               JwtService jwtService) {
        this.refreshTokenRepository = refreshTokenRepository;
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    /**
     * Create and save a new refresh token for a user
     */
    @Transactional
    public RefreshToken createRefreshToken(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        // Delete any existing refresh tokens for this user
        refreshTokenRepository.deleteByUser(user);

        // Generate new refresh token
        String tokenString = jwtService.generateRefreshToken(email);

        RefreshToken refreshToken = RefreshToken.builder()
                .token(tokenString)
                .user(user)
                .expiryDate(LocalDateTime.now().plusDays(7))
                .createdAt(LocalDateTime.now())
                .revoked(false)
                .build();

        return refreshTokenRepository.save(refreshToken);
    }

    /**
     * Verify refresh token validity
     */
    public RefreshToken verifyRefreshToken(String token) {
        RefreshToken refreshToken = refreshTokenRepository.findByToken(token)
                .orElseThrow(() -> new RuntimeException("Refresh token not found"));

        if (refreshToken.getRevoked()) {
            throw new RuntimeException("Refresh token has been revoked");
        }

        if (refreshToken.isExpired()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("Refresh token has expired");
        }

        // Check if user is still active
        User user = refreshToken.getUser();
        if (user.getActive() == null || !user.getActive()) {
            refreshTokenRepository.delete(refreshToken);
            throw new RuntimeException("User account is inactive");
        }

        return refreshToken;
    }

    /**
     * Delete refresh token (logout)
     */
    @Transactional
    public void deleteRefreshToken(String token) {
        refreshTokenRepository.deleteByToken(token);
    }

    /**
     * Delete all refresh tokens for a user
     */
    @Transactional
    public void deleteUserRefreshTokens(User user) {
        refreshTokenRepository.deleteByUser(user);
    }

    /**
     * Find refresh token by token string
     */
    public Optional<RefreshToken> findByToken(String token) {
        return refreshTokenRepository.findByToken(token);
    }
}
