package com.ventureverse.ventureverse_api.security;

import com.ventureverse.ventureverse_api.entities.User;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;

public class CustomUserDetails implements UserDetails {

    private final User user;

    public CustomUserDetails(User user) {
        this.user = user;
    }

    public User getUser() {
        return user;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        // Default role for all users
        // You can enhance this later with actual roles
        return List.of(new SimpleGrantedAuthority("ROLE_USER"));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        // Return username instead of email
        // Spring Security uses this for authentication context
        return user.getUsername();
    }

    @Override
    public boolean isAccountNonExpired() {
        return true;
    }

    @Override
    public boolean isAccountNonLocked() {
        return true;
    }

    @Override
    public boolean isCredentialsNonExpired() {
        return true;
    }

    @Override
    public boolean isEnabled() {
        return user.getEnabled() != null ? user.getEnabled() : true;
    }

    // Utility method to get email
    public String getEmail() {
        return user.getEmail();
    }

    // Utility method to get full name
    public String getFullName() {
        return user.getFullName();
    }

    // Utility method to get user ID
    public Long getUserId() {
        return user.getId();
    }
}