package com.p99soft.deskflow.security;

import com.p99soft.deskflow.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collection;
import java.util.List;
import java.util.UUID;

/**
 * Spring Security {@link UserDetails} adapter for the {@link User} domain entity.
 * <p>
 * Wraps the persisted {@code User} and exposes only what Spring Security needs,
 * keeping the domain entity free of framework concerns.
 * </p>
 */
@RequiredArgsConstructor
public class UserDetailsImpl implements UserDetails {

    private final User user;

    // ------------------------------------------------------------------ //
    // Convenience accessors used by services (not part of UserDetails API)
    // ------------------------------------------------------------------ //

    public UUID getId() {
        return user.getId();
    }

    public User getUser() {
        return user;
    }

    // ------------------------------------------------------------------ //
    // UserDetails contract
    // ------------------------------------------------------------------ //

    /**
     * Returns a single authority prefixed with {@code ROLE_} so that Spring
     * Security's {@code hasRole("ADMIN")} expressions work out of the box.
     */
    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name()));
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    /** Email is used as the unique login identifier. */
    @Override
    public String getUsername() {
        return user.getEmail();
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

    /**
     * An account is enabled when the user's status is {@code "ACTIVE"}.
     * Any other value (e.g. {@code "INACTIVE"}, {@code "SUSPENDED"}) will
     * cause Spring Security to reject the login.
     */
    @Override
    public boolean isEnabled() {
        return "ACTIVE".equalsIgnoreCase(user.getStatus());
    }
}
