package com.mkappworks.authservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mkappworks.authservice.dto.AuthDtos.LoginRequest;
import com.mkappworks.authservice.dto.AuthDtos.RegisterRequest;
import com.mkappworks.authservice.entity.Role;
import jakarta.servlet.http.Cookie;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
@AutoConfigureMockMvc
@Testcontainers
@ActiveProfiles("test")
@TestMethodOrder(MethodOrderer.OrderAnnotation.class)
@DisplayName("Auth Service Integration Tests")
class AuthServiceIntegrationTest {

    @Container
    static PostgreSQLContainer<?> postgres = new PostgreSQLContainer<>("postgres:16-alpine")
            .withDatabaseName("test_auth_db")
            .withUsername("test_user")
            .withPassword("test_password");

    @DynamicPropertySource
    static void configureProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", postgres::getJdbcUrl);
        registry.add("spring.datasource.username", postgres::getUsername);
        registry.add("spring.datasource.password", postgres::getPassword);
        registry.add("eureka.client.enabled", () -> "false");
        registry.add("spring.cloud.config.enabled", () -> "false");
    }

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    private static final String REGISTER_URL = "/api/v1/auth/register";
    private static final String LOGIN_URL    = "/api/v1/auth/login";
    private static final String REFRESH_URL  = "/api/v1/auth/refresh";
    private static final String LOGOUT_URL   = "/api/v1/auth/logout";

    private static String capturedRefreshCookie;

    @Test
    @Order(1)
    @DisplayName("POST /register — should create user and return tokens and set HttpOnly cookie")
    void register_ValidRequest_Returns201WithTokens() throws Exception {
        var request = new RegisterRequest("student1", "student1@example.com", "Password@123", Role.STUDENT, null);

        MvcResult result = mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.refreshToken").isNotEmpty())
                .andExpect(jsonPath("$.tokenType").value("Bearer"))
                .andExpect(jsonPath("$.role").value("STUDENT"))
                .andExpect(jsonPath("$.username").value("student1"))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("sms_refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")))
                .andReturn();

        Cookie cookie = result.getResponse().getCookie("sms_refresh");
        assertNotNull(cookie);
        capturedRefreshCookie = cookie.getValue();
    }

    @Test
    @Order(2)
    @DisplayName("POST /register — should return 400 for invalid email format")
    void register_InvalidEmail_Returns400() throws Exception {
        var request = new RegisterRequest("baduser", "not-an-email", "Password@123", Role.STUDENT, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(3)
    @DisplayName("POST /register — should return 400 for password shorter than 8 characters")
    void register_ShortPassword_Returns400() throws Exception {
        var request = new RegisterRequest("shortpwd", "short@example.com", "abc", Role.STUDENT, null);

        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(4)
    @DisplayName("POST /register — should return 400 for duplicate username")
    void register_DuplicateUsername_Returns400() throws Exception {
        var request = new RegisterRequest("dupuser", "dup@example.com", "Password@123", Role.TEACHER, null);

        // First registration succeeds
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated());

        // Second registration with same username fails
        var duplicate = new RegisterRequest("dupuser", "other@example.com", "Password@123", Role.STUDENT, null);
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(duplicate)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(5)
    @DisplayName("POST /login — should return tokens and set HttpOnly cookie for valid credentials")
    void login_ValidCredentials_Returns200WithTokens() throws Exception {
        // Register a user first
        var reg = new RegisterRequest("loginuser", "loginuser@example.com", "Password@123", Role.ADMIN, null);
        mockMvc.perform(post(REGISTER_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(reg)))
                .andExpect(status().isCreated());

        // Then login
        var login = new LoginRequest("loginuser", "Password@123");
        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.role").value("ADMIN"))
                .andExpect(jsonPath("$.username").value("loginuser"))
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("sms_refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    @Order(6)
    @DisplayName("POST /login — should return 400 for wrong password")
    void login_WrongPassword_Returns400() throws Exception {
        var login = new LoginRequest("loginuser", "WrongPass@999");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(7)
    @DisplayName("POST /login — should return 400 for non-existent user")
    void login_NonExistentUser_Returns400() throws Exception {
        var login = new LoginRequest("ghost", "Password@123");

        mockMvc.perform(post(LOGIN_URL)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(login)))
                .andExpect(status().isBadRequest());
    }

    @Test
    @Order(8)
    @DisplayName("POST /refresh — should return new tokens when valid sms_refresh cookie is sent")
    void refresh_WithValidCookie_Returns200WithNewTokens() throws Exception {
        assertNotNull(capturedRefreshCookie, "Refresh cookie must be captured from Order 1 test");

        mockMvc.perform(post(REFRESH_URL)
                        .cookie(new Cookie("sms_refresh", capturedRefreshCookie)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.accessToken").isNotEmpty())
                .andExpect(jsonPath("$.data.refreshToken").isNotEmpty())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("sms_refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("HttpOnly")));
    }

    @Test
    @Order(9)
    @DisplayName("POST /logout — should clear the sms_refresh cookie")
    void logout_ClearsCookie_Returns200() throws Exception {
        mockMvc.perform(post(LOGOUT_URL))
                .andExpect(status().isOk())
                .andExpect(header().exists("Set-Cookie"))
                .andExpect(header().string("Set-Cookie", containsString("sms_refresh=")))
                .andExpect(header().string("Set-Cookie", containsString("Max-Age=0")));
    }

    @Test
    @Order(10)
    @DisplayName("POST /refresh — should return 400 when no cookie and no body")
    void refresh_WithNoCookieOrBody_Returns400() throws Exception {
        mockMvc.perform(post(REFRESH_URL)
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }
}
