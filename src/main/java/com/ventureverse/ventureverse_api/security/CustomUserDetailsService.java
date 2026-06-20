package com.ventureverse.ventureverse_api.security;

import com.ventureverse.ventureverse_api.entities.User;
import com.ventureverse.ventureverse_api.repositories.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.DisabledException; // ✅ ADD THIS IMPORT
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String login)
            throws UsernameNotFoundException {

        // Support login with email OR username
        User user = userRepository.findByEmailOrUsername(login.toLowerCase().trim())
                .orElseThrow(() -> new UsernameNotFoundException(
                        "User not found with email or username: " + login));

        // Check if account is enabled
        if (!user.getEnabled()) {
            throw new DisabledException("User account is disabled"); // ✅ DisabledException is now resolved
        }

        return new CustomUserDetails(user);
    }
}