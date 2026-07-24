package com.p99soft.deskflow;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.p99soft.deskflow.dto.LoginRequest;
import com.p99soft.deskflow.dto.RegisterRequest;
import com.p99soft.deskflow.entity.User;
import com.p99soft.deskflow.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers;

import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
public class SecurityIntegrationTest {

    private MockMvc mockMvc;

    @Autowired
    private WebApplicationContext webApplicationContext;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());

    @BeforeEach
    void setUp() {
        userRepository.deleteAll();
        mockMvc = MockMvcBuilders.webAppContextSetup(webApplicationContext)
                .apply(SecurityMockMvcConfigurers.springSecurity())
                .build();

        // Seed initial admin user directly in database
        User admin = User.builder()
                .firstName("Ramesh")
                .lastName("Kumar")
                .email("admin@deskflow.com")
                .password(passwordEncoder.encode("password123"))
                .role("ADMIN")
                .status("ACTIVE")
                .employeeCode("ADM-01")
                .build();
        userRepository.save(admin);
    }

    @Test
    void testSecurityFlows() throws Exception {
        // 1. Verify protected endpoint rejects missing token (401)
        mockMvc.perform(get("/api/v1/admin/dashboard"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401))
                .andExpect(jsonPath("$.message").value(containsString("Authentication failed")));

        // 2. Login as the seeded admin to retrieve admin JWT
        LoginRequest adminLogin = LoginRequest.builder()
                .email("admin@deskflow.com")
                .password("password123")
                .build();

        MvcResult adminLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(adminLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andReturn();

        String adminToken = objectMapper.readTree(adminLoginResult.getResponse().getContentAsString())
                .get("token").asText();

        // 3. Verify public registration is blocked (403 Forbidden without Admin JWT)
        RegisterRequest registerEmployee = RegisterRequest.builder()
                .firstName("Ramesh")
                .lastName("Kumar")
                .email("employee@deskflow.com")
                .password("password123")
                .role("EMPLOYEE")
                .employeeCode("EMP-01")
                .build();

        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerEmployee)))
                .andExpect(status().isUnauthorized());

        // 4. Provision Employee using the Admin JWT (201 Created)
        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + adminToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerEmployee)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message").value("Employee registered successfully"));

        // 5. Login as the newly provisioned employee to retrieve employee JWT
        LoginRequest employeeLogin = LoginRequest.builder()
                .email("employee@deskflow.com")
                .password("password123")
                .build();

        MvcResult employeeLoginResult = mockMvc.perform(post("/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(employeeLogin)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").exists())
                .andExpect(jsonPath("$.role").value("EMPLOYEE"))
                .andReturn();

        String employeeToken = objectMapper.readTree(employeeLoginResult.getResponse().getContentAsString())
                .get("token").asText();

        // 5b. Verify that Employee role is forbidden from registering other users (403 Forbidden)
        RegisterRequest registerAnother = RegisterRequest.builder()
                .firstName("Another")
                .lastName("User")
                .email("another@deskflow.com")
                .password("password123")
                .role("EMPLOYEE")
                .employeeCode("EMP-02")
                .build();

        mockMvc.perform(post("/auth/register")
                        .header("Authorization", "Bearer " + employeeToken)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(registerAnother)))
                .andExpect(status().isForbidden());

        // 6. Verify role checks: ADMIN can access admin dashboard, EMPLOYEE is forbidden (403)
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isOk());

        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer " + employeeToken))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.status").value(403))
                .andExpect(jsonPath("$.message").value(containsString("Access Denied")));

        // 7. Verify invalid token is rejected (401)
        mockMvc.perform(get("/api/v1/admin/dashboard")
                        .header("Authorization", "Bearer invalidTokenContentHere"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }
}
