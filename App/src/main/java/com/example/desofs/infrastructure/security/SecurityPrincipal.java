package com.example.desofs.infrastructure.security;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Security Principal representing authenticated user in Spring Security context
 * To be populated from JWT claims and used for authorization
 */
public class SecurityPrincipal implements UserDetails {
    private Long userId;
    private String email;
    private String username;
    private Collection<GrantedAuthority> authorities;
    private boolean mfaVerified;

    public SecurityPrincipal(Long userId, String email, String username) {
        this.userId = userId;
        this.email = email;
        this.username = username;
        this.authorities = new ArrayList<>();
        this.mfaVerified = false;
    }

    public Long getUserId() { return userId; }
    public String getEmail() { return email; }
    public boolean isMfaVerified() { return mfaVerified; }
    public void setMfaVerified(boolean mfaVerified) { this.mfaVerified = mfaVerified; }

    @Override
    public String getUsername() { return username; }

    @Override
    public String getPassword() { return null; }

    @Override
    public Collection<GrantedAuthority> getAuthorities() { return authorities; }

    @Override
    public boolean isAccountNonExpired() { return true; }

    @Override
    public boolean isAccountNonLocked() { return true; }

    @Override
    public boolean isCredentialsNonExpired() { return true; }

    @Override
    public boolean isEnabled() { return true; }

    public void addRole(String role) {
        this.authorities.add(new SimpleGrantedAuthority("ROLE_" + role));
    }
}
