package com.p99soft.deskflow.service;

import com.p99soft.deskflow.dto.AuthResponse;
import com.p99soft.deskflow.dto.LoginRequest;
import com.p99soft.deskflow.dto.LoginResponse;
import com.p99soft.deskflow.dto.RegisterRequest;
import com.p99soft.deskflow.entity.Team;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.enums.Role;
import com.p99soft.deskflow.exception.ResourceNotFoundException;
import com.p99soft.deskflow.repository.TeamRepository;
import com.p99soft.deskflow.repository.UserRepository;
import com.p99soft.deskflow.security.JwtService;
import com.p99soft.deskflow.security.UserDetailsImpl;
import com.p99soft.deskflow.service.Impl.AuthServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock private UserRepository       userRepository;
    @Mock private TeamRepository       teamRepository;
    @Mock private PasswordEncoder      passwordEncoder;
    @Mock private AuthenticationManager authenticationManager;
    @Mock private JwtService           jwtService;

    @InjectMocks
    private AuthServiceImpl authService;

    private UUID userId;
    private UUID teamId;
    private User savedUser;
    private Team team;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        teamId = UUID.randomUUID();

        team = Team.builder()
                .id(teamId)
                .name("Engineering")
                .status("ACTIVE")
                .build();

        savedUser = User.builder()
                .id(userId)
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("$2a$10$hashedpassword")
                .role(Role.EMPLOYEE)
                .status("ACTIVE")
                .build();
    }

    // ------------------------------------------------------------------ //
    // Register tests
    // ------------------------------------------------------------------ //

    @Test
    void testRegister_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .role(Role.EMPLOYEE)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(userId, response.getUserId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals(Role.EMPLOYEE, response.getRole());
        assertEquals("User registered successfully", response.getMessage());
        verify(userRepository).save(any(User.class));
        verify(passwordEncoder).encode("password123");
    }

    @Test
    void testRegister_EmailAlreadyExists_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .email("john@example.com")
                .password("password123")
                .role(Role.EMPLOYEE)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        IllegalArgumentException ex = assertThrows(IllegalArgumentException.class,
                () -> authService.register(request));

        assertTrue(ex.getMessage().contains("Email is already registered"));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_TeamNotFound_ThrowsException() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .role(Role.EMPLOYEE)
                .teamId(teamId)
                .build();

        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(teamRepository.findById(teamId)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> authService.register(request));
        verify(userRepository, never()).save(any());
    }

    @Test
    void testRegister_WithTeam_Success() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("password123")
                .role(Role.EMPLOYEE)
                .teamId(teamId)
                .build();

        savedUser.setTeam(team);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(teamRepository.findById(teamId)).thenReturn(Optional.of(team));
        when(passwordEncoder.encode("password123")).thenReturn("$2a$10$hashedpassword");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        AuthResponse response = authService.register(request);

        assertNotNull(response);
        assertEquals(Role.EMPLOYEE, response.getRole());
        verify(teamRepository).findById(teamId);
        verify(userRepository).save(any(User.class));
    }

    @Test
    void testRegister_PasswordIsHashed() {
        RegisterRequest request = RegisterRequest.builder()
                .firstName("John")
                .lastName("Doe")
                .email("john@example.com")
                .password("plaintext")
                .role(Role.EMPLOYEE)
                .build();

        when(userRepository.existsByEmail(anyString())).thenReturn(false);
        when(passwordEncoder.encode("plaintext")).thenReturn("$2a$10$hashed");
        when(userRepository.save(any(User.class))).thenReturn(savedUser);

        authService.register(request);

        // Verify the plain password was never stored — encoder was called
        verify(passwordEncoder).encode("plaintext");
        verify(passwordEncoder, never()).encode("$2a$10$hashed");
    }

    // ------------------------------------------------------------------ //
    // Login tests
    // ------------------------------------------------------------------ //

    @Test
    void testLogin_Success() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(savedUser);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(auth);
        when(jwtService.generateToken(savedUser)).thenReturn("mock.jwt.token");

        LoginResponse response = authService.login(request);

        assertNotNull(response);
        assertEquals("mock.jwt.token", response.getToken());
        assertEquals("Bearer", response.getTokenType());
        assertEquals(userId, response.getUserId());
        assertEquals("john@example.com", response.getEmail());
        assertEquals("John Doe", response.getFullName());
        assertEquals(Role.EMPLOYEE, response.getRole());
    }

    @Test
    void testLogin_InvalidCredentials_ThrowsException() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("wrongpassword")
                .build();

        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenThrow(new BadCredentialsException("Bad credentials"));

        assertThrows(BadCredentialsException.class, () -> authService.login(request));
        verify(jwtService, never()).generateToken(any());
    }

    @Test
    void testLogin_TokenContainsCorrectRole() {
        LoginRequest request = LoginRequest.builder()
                .email("admin@example.com")
                .password("adminpass")
                .build();

        User adminUser = User.builder()
                .id(UUID.randomUUID())
                .firstName("Admin")
                .lastName("User")
                .email("admin@example.com")
                .password("hashed")
                .role(Role.ADMIN)
                .status("ACTIVE")
                .build();

        UserDetailsImpl adminDetails = new UserDetailsImpl(adminUser);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(adminDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(adminUser)).thenReturn("admin.jwt.token");

        LoginResponse response = authService.login(request);

        assertEquals(Role.ADMIN, response.getRole());
        assertEquals("admin.jwt.token", response.getToken());
        verify(jwtService).generateToken(adminUser);
    }

    @Test
    void testLogin_JwtServiceCalledWithCorrectUser() {
        LoginRequest request = LoginRequest.builder()
                .email("john@example.com")
                .password("password123")
                .build();

        UserDetailsImpl userDetails = new UserDetailsImpl(savedUser);
        Authentication auth = mock(Authentication.class);
        when(auth.getPrincipal()).thenReturn(userDetails);
        when(authenticationManager.authenticate(any())).thenReturn(auth);
        when(jwtService.generateToken(savedUser)).thenReturn("token");

        authService.login(request);

        // Verify JWT was generated with the actual User entity, not some stub
        verify(jwtService).generateToken(savedUser);
    }
}
