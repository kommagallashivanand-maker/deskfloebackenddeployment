package com.p99soft.deskflow.security;

import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Loads user-specific data for Spring Security during authentication.
 * <p>
 * Spring Security calls {@link #loadUserByUsername} with the value supplied
 * as the "username" field — in DeskFlow that value is the user's email address.
 * </p>
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class UserDetailsServiceImpl implements UserDetailsService {

    private final UserRepository userRepository;

    /**
     * Locates the user by email address.
     *
     * @param email the email address submitted in the login request
     * @return a fully populated {@link UserDetailsImpl} instance
     * @throws UsernameNotFoundException if no user with that email exists
     */
    @Override
    @Transactional(readOnly = true)
    public UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        log.debug("Loading user by email: {}", email);

        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> {
                    log.warn("Authentication attempt failed — no account found for email: {}", email);
                    return new UsernameNotFoundException("No account found with email: " + email);
                });

        return new UserDetailsImpl(user);
    }
}
