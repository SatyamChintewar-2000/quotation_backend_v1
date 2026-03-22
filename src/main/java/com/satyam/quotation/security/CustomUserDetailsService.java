package com.satyam.quotation.security;

import com.satyam.quotation.model.User;
import com.satyam.quotation.repository.UserRepository;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    public CustomUserDetailsService(UserRepository userRepository) {
        this.userRepository = userRepository;
    }

    @Override
    public UserDetails loadUserByUsername(String email)
            throws UsernameNotFoundException {

        User user = userRepository.findByEmailIgnoreCase(email.toLowerCase())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));

        // Check if user is active
        if (user.getActive() == null || !user.getActive()) {
            throw new UsernameNotFoundException("User account is inactive");
        }

        return new CustomUserDetails(user);
    }
}
