package com.satyam.quotation.security;

import com.satyam.quotation.model.User;
import com.satyam.quotation.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private static final Logger log = LoggerFactory.getLogger(CustomUserDetailsService.class);

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        log.debug("Loaded user: email={}, active={}, passwordHash={}",
                user.getEmail(),
                user.getActive(),
                user.getPassword() != null ? user.getPassword().substring(0, Math.min(10, user.getPassword().length())) + "..." : "NULL");

        // Check if user is active
        if (user.getActive() == null || !user.getActive()) {
            throw new UsernameNotFoundException("User account is inactive");
        }

        return new CustomUserDetails(user);
    }
}
