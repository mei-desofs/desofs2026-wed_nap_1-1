package com.example.desofs.controller;

import com.example.desofs.config.SecurityConfig;
import com.example.desofs.controllers.MovieController;
import com.example.desofs.domain.Movie;
import com.example.desofs.domain.Role;
import com.example.desofs.security.RoleGuard;
import com.example.desofs.services.MovieService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

/**
 * Integration tests for MovieController using Spring @WebMvcTest.
 */
@WebMvcTest(MovieController.class)
@Import(SecurityConfig.class)
@DisplayName("MovieController Integration Tests")
class MovieControllerIntegrationTests {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private MovieService movieService;

    @MockitoBean
    private RoleGuard roleGuard;

    private Movie testMovie1;
    private Movie testMovie2;

    @BeforeEach
    void setUp() {
        testMovie1 = new Movie("Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray",
                new BigDecimal("14.99"), 10);
        testMovie1.setId(1L);

        testMovie2 = new Movie("Matrix", "Reality is a construct", "Sci-Fi", "DVD",
                new BigDecimal("12.99"), 5);
        testMovie2.setId(2L);
    }

    // ============ GET /api/movies (List All) ============

    @Test
    @DisplayName("GET /api/movies should return 200 OK with all movies")
    void testGetMoviesCatalog_Returns200_WithMoviesList() throws Exception {
        when(movieService.listAll()).thenReturn(List.of(testMovie1, testMovie2));

        mockMvc.perform(get("/api/movies")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[0].id").value(1L))
                .andExpect(jsonPath("$[0].title").value("Inception"))
                .andExpect(jsonPath("$[1].id").value(2L))
                .andExpect(jsonPath("$[1].title").value("Matrix"));

        verify(movieService, times(1)).listAll();
    }

    @Test
    @DisplayName("GET /api/movies should return empty array when no movies exist")
    void testGetMoviesCatalog_WithEmptyDatabase_ReturnsEmptyArray() throws Exception {
        when(movieService.listAll()).thenReturn(List.of());

        mockMvc.perform(get("/api/movies")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));

        verify(movieService, times(1)).listAll();
    }

    @Test
    @DisplayName("GET /api/movies response contains all required fields")
    void testGetMoviesCatalog_ResponseContainsAllRequiredFields() throws Exception {
        when(movieService.listAll()).thenReturn(List.of(testMovie1));

        mockMvc.perform(get("/api/movies")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].id").exists())
                .andExpect(jsonPath("$[0].title").value("Inception"))
                .andExpect(jsonPath("$[0].description").value("A mind-bending thriller"))
                .andExpect(jsonPath("$[0].genre").value("Sci-Fi"))
                .andExpect(jsonPath("$[0].platform").value("Blu-ray"))
                .andExpect(jsonPath("$[0].price").value("14.99"))
                .andExpect(jsonPath("$[0].stockQuantity").value(10));
    }

    // ============ GET /api/movies/{id} (Get by ID) ============

    @Test
    @DisplayName("GET /api/movies/{id} should return 200 OK with movie")
    void testGetMovieById_WithValidId_Returns200() throws Exception {
        when(movieService.get(1L)).thenReturn(testMovie1);

        mockMvc.perform(get("/api/movies/1")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Inception"));

        verify(movieService, times(1)).get(1L);
    }

    @Test
    @DisplayName("GET /api/movies/{id} should return 404 when movie not found")
    void testGetMovieById_WithInvalidId_Returns404() throws Exception {
        when(movieService.get(999L)).thenReturn(null);

        mockMvc.perform(get("/api/movies/999")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound());

        verify(movieService, times(1)).get(999L);
    }

    @Test
    @DisplayName("GET /api/movies/{id} returns correct movie data")
    void testGetMovieById_ReturnsCorrectMovie() throws Exception {
        when(movieService.get(1L)).thenReturn(testMovie1);

        mockMvc.perform(get("/api/movies/1")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(1L))
                .andExpect(jsonPath("$.title").value("Inception"))
                .andExpect(jsonPath("$.description").value("A mind-bending thriller"))
                .andExpect(jsonPath("$.genre").value("Sci-Fi"))
                .andExpect(jsonPath("$.platform").value("Blu-ray"))
                .andExpect(jsonPath("$.price").value("14.99"))
                .andExpect(jsonPath("$.stockQuantity").value(10));
    }

    // ============ POST /api/movies (Create) ============

    @Test
    @DisplayName("POST /api/movies should create movie and return 201 CREATED")
    void testCreateMovie_Returns201() throws Exception {
        Movie savedMovie = new Movie("Avatar", "Blue aliens", "Action", "4K",
                new BigDecimal("19.99"), 15);
        savedMovie.setId(3L);

        when(movieService.create(any(Movie.class))).thenReturn(savedMovie);

        String requestBody = "{"
                + "\"title\":\"Avatar\","
                + "\"description\":\"Blue aliens\","
                + "\"genre\":\"Action\","
                + "\"platform\":\"4K\","
                + "\"price\":19.99,"
                + "\"stockQuantity\":15"
                + "}";

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/movies/3")))
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.title").value("Avatar"));

        verify(movieService, times(1)).create(any(Movie.class));
    }

    // ============ SECURITY & HEADERS ============
    @Test
    @DisplayName("GET /api/movies should handle malicious input safely")
    void testGetMovieById_WithSQLInjection_DeformedButSafe() throws Exception {
        when(movieService.get(any())).thenReturn(null);

        mockMvc.perform(get("/api/movies/1%20OR%201=1")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/movies with non-ADMIN role should return 403 Forbidden")
    void testCreateMovie_WithNonAdminRole_Returns403() throws Exception {
        doThrow(new AccessDeniedException("Access denied. Required role: ADMIN"))
                .when(roleGuard).requireRole(any(Jwt.class), eq(Role.ADMIN));

        String requestBody = "{"
                + "\"title\":\"Avatar\","
                + "\"description\":\"Blue aliens\","
                + "\"genre\":\"Action\","
                + "\"platform\":\"4K\","
                + "\"price\":19.99,"
                + "\"stockQuantity\":15"
                + "}";

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody)
                        .with(jwt()
                                .jwt(jwt -> jwt.subject("auth0|user123"))
                        ))
                .andExpect(status().isForbidden());

        verify(movieService, never()).create(any(Movie.class));
    }

    // ============ AUTHENTICATION Tests ============

    @Test
    @DisplayName("GET /api/movies without JWT should return 401 Unauthorized")
    void testGetMovies_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/movies")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(movieService, never()).listAll();
    }

    @Test
    @DisplayName("GET /api/movies/{id} without JWT should return 401 Unauthorized")
    void testGetMovieById_WithoutJwt_Returns401() throws Exception {
        mockMvc.perform(get("/api/movies/1")
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isUnauthorized());

        verify(movieService, never()).get(any());
    }

    @Test
    @DisplayName("POST /api/movies without JWT should return 401 Unauthorized")
    void testCreateMovie_WithoutJwt_Returns401() throws Exception {
        String requestBody = "{"
                + "\"title\":\"Avatar\","
                + "\"description\":\"Blue aliens\","
                + "\"genre\":\"Action\","
                + "\"platform\":\"4K\","
                + "\"price\":19.99,"
                + "\"stockQuantity\":15"
                + "}";

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isUnauthorized());

        verify(movieService, never()).create(any(Movie.class));
    }

    // ============ REQUEST VALIDATION Tests ============

    @Test
    @DisplayName("POST /api/movies with malformed JSON should return 400 Bad Request")
    void testCreateMovie_WithMalformedJson_Returns400() throws Exception {
        String invalidJson = "{\"title\":invalid}";

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(invalidJson))
                .andExpect(status().isBadRequest());

        verify(movieService, never()).create(any(Movie.class));
    }

    @Test
    @DisplayName("POST /api/movies with negative price should return 400 Bad Request")
    void testCreateMovie_WithNegativePrice_Returns400() throws Exception {
        when(movieService.create(any(Movie.class)))
                .thenThrow(new IllegalArgumentException("Price must be positive"));

        String requestBody = "{"
                + "\"title\":\"Bad Movie\","
                + "\"description\":\"Invalid\","
                + "\"genre\":\"Action\","
                + "\"platform\":\"DVD\","
                + "\"price\":-5.00,"
                + "\"stockQuantity\":10"
                + "}";

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("POST /api/movies with negative stock should return 400 Bad Request")
    void testCreateMovie_WithNegativeStock_Returns400() throws Exception {
        when(movieService.create(any(Movie.class)))
                .thenThrow(new IllegalArgumentException("Stock cannot be negative"));

        String requestBody = "{"
                + "\"title\":\"Bad Movie\","
                + "\"description\":\"Invalid\","
                + "\"genre\":\"Action\","
                + "\"platform\":\"DVD\","
                + "\"price\":9.99,"
                + "\"stockQuantity\":-1"
                + "}";

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest());
    }

    @Test
    @DisplayName("Response should not expose sensitive information")
    void testGetMoviesCatalog_ResponseDoesNotExposeSensitiveData() throws Exception {
        when(movieService.listAll()).thenReturn(List.of(testMovie1));

        mockMvc.perform(get("/api/movies")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[0].createdBy").doesNotExist())
                .andExpect(jsonPath("$[0].internalId").doesNotExist());
    }
}