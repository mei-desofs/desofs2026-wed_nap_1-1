package com.example.desofs.controller;

import com.example.desofs.config.SecurityConfig;
import com.example.desofs.controllers.MovieController;
import com.example.desofs.domain.Role;
import com.example.desofs.security.IRoleGuard;
import com.example.desofs.services.IAuditLogService;
import com.example.desofs.services.IMovieService;
import com.example.desofs.shared.dtos.MovieDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import org.springframework.context.annotation.Import;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;

import java.math.BigDecimal;
import java.util.List;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.TransactionSystemException;
import jakarta.validation.ConstraintViolationException;
import java.util.HashSet;

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
        private IMovieService movieService;

        @MockitoBean
                private IAuditLogService auditLogService;

    @MockitoBean
        private IRoleGuard roleGuard;

        private MovieDTO testMovie1;
        private MovieDTO testMovie2;

    @BeforeEach
    void setUp() {
        testMovie1 = new MovieDTO(1L, "Inception", "A mind-bending thriller", "Sci-Fi", "Blu-ray",
                new BigDecimal("14.99"), 10);

        testMovie2 = new MovieDTO(2L, "Matrix", "Reality is a construct", "Sci-Fi", "DVD",
                new BigDecimal("12.99"), 5);
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
        @DisplayName("GET /api/movies should return 200 with empty array when service fails")
        void testGetMoviesCatalog_WhenServiceThrows_Returns200EmptyArray() throws Exception {
                when(movieService.listAll()).thenThrow(new RuntimeException("DB failure"));

                mockMvc.perform(get("/api/movies")
                                                .with(jwt())
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                                .andExpect(jsonPath("$", hasSize(0)));

                verify(movieService, times(1)).listAll();
        }

        @Test
        @DisplayName("GET /api/movies should return 200 with empty array when service throws an error")
        void testGetMoviesCatalog_WhenServiceThrowsError_Returns200EmptyArray() throws Exception {
                when(movieService.listAll()).thenThrow(new AssertionError("Unexpected failure"));

                mockMvc.perform(get("/api/movies")
                                                .with(jwt())
                                                .accept(MediaType.APPLICATION_JSON))
                                .andExpect(status().isOk())
                                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
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
        MovieDTO savedMovie = new MovieDTO(3L, "Avatar", "Blue aliens", "Action", "4K",
                new BigDecimal("19.99"), 15);

        when(movieService.create(any(MovieDTO.class))).thenReturn(savedMovie);

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
                        .with(jwt().jwt(jwt -> jwt.subject("auth0|admin123")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/movies/3")))
                .andExpect(jsonPath("$.id").value(3L))
                .andExpect(jsonPath("$.title").value("Avatar"));

        verify(movieService, times(1)).create(any(MovieDTO.class));
        verify(auditLogService, times(1)).log(eq("auth0|admin123"), eq("auth0|admin123"), eq(Role.ADMIN), eq("CREATE_MOVIE"));
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

    // ============ Exception handling tests ============

    @Test
    @DisplayName("GET /api/movies/{id} with non-numeric id should return 400 Bad Request")
    void testGetMovieById_WithNonNumericId_Returns400() throws Exception {
        mockMvc.perform(get("/api/movies/abc")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Invalid path parameter"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("GET /api/movies/{id} when service throws unexpected exception returns 500 sanitized")
    void testGetMovieById_WhenServiceThrowsUnexpected_Returns500() throws Exception {
        when(movieService.get(1L)).thenThrow(new RuntimeException("boom"));

        mockMvc.perform(get("/api/movies/1")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isInternalServerError())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("An unexpected error occurred"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.status").value(500));

        verify(movieService, times(1)).get(1L);
    }

    @Test
    @DisplayName("POST /api/movies should return 403 when role guard denies access")
    void testCreateMovie_WhenRoleGuardDenies_Returns403() throws Exception {
        // Arrange: role guard will throw AccessDeniedException
        doThrow(new AccessDeniedException("no access")).when(roleGuard).requireRole(any(), any());

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
                .andExpect(status().isForbidden())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.message").value("Access denied"))
                .andExpect(jsonPath("$.correlationId").exists())
                .andExpect(jsonPath("$.status").value(403));

        verify(roleGuard, times(1)).requireRole(any(), any());
        verify(movieService, times(0)).create(any(MovieDTO.class));
    }

    @Test
    @DisplayName("POST /api/movies with malformed JSON returns 400 Malformed request body")
    void testCreateMovie_MalformedJson_Returns400() throws Exception {
        String badJson = "{"; // intentionally malformed

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .with(jwt())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(badJson))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Malformed request body"))
                .andExpect(jsonPath("$.status").value(400));
    }

    @Test
    @DisplayName("POST /api/movies with unsupported media type returns 415")
    void testCreateMovie_UnsupportedMediaType_Returns415() throws Exception {
        String body = "title=foo";

        mockMvc.perform(post("/api/movies")
                        .with(csrf())
                        .with(jwt())
                        .contentType(MediaType.TEXT_PLAIN)
                        .content(body))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(jsonPath("$.message").value("Unsupported media type"))
                .andExpect(jsonPath("$.status").value(415));
    }

    @Test
    @DisplayName("DELETE /api/movies should return 405 Method Not Allowed")
    void testDeleteCollection_MethodNotAllowed_Returns405() throws Exception {
        mockMvc.perform(delete("/api/movies")
                        .with(jwt()))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.message").value("Method not allowed"))
                .andExpect(jsonPath("$.status").value(405));
    }

    @Test
    @DisplayName("POST /api/movies should return 400 Data Integrity Violation when service fails")
    void testCreateMovie_DataIntegrityViolation_Returns400() throws Exception {
        when(movieService.create(any(MovieDTO.class))).thenThrow(new DataIntegrityViolationException("duplicate"));

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid data: a required field is missing or violates constraints"))
                .andExpect(jsonPath("$.status").value(400));

        verify(movieService, times(1)).create(any(MovieDTO.class));
    }

    @Test
    @DisplayName("GET /api/movies/{id} when service throws IllegalState returns 409 Conflict")
    void testGetMovieById_WhenServiceThrowsIllegalState_Returns409() throws Exception {
        when(movieService.get(2L)).thenThrow(new IllegalStateException("conflict state"));

        mockMvc.perform(get("/api/movies/2")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("conflict state"))
                .andExpect(jsonPath("$.status").value(409));

        verify(movieService, times(1)).get(2L);
    }

    @Test
    @DisplayName("GET /api/movies/{id} when service throws IllegalArgument returns 400 Bad Request")
    void testGetMovieById_WhenServiceThrowsIllegalArgument_Returns400() throws Exception {
        when(movieService.get(10L)).thenThrow(new IllegalArgumentException("bad input"));

        mockMvc.perform(get("/api/movies/10")
                        .with(jwt())
                        .accept(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("bad input"))
                .andExpect(jsonPath("$.status").value(400));

        verify(movieService, times(1)).get(10L);
    }

    @Test
    @DisplayName("POST /api/movies when service throws ConstraintViolationException returns 400")
    void testCreateMovie_WhenServiceThrowsConstraintViolation_Returns400() throws Exception {
        when(movieService.create(any(MovieDTO.class))).thenThrow(new ConstraintViolationException("cv", new HashSet<>()));

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400));

        verify(movieService, times(1)).create(any(MovieDTO.class));
    }

    @Test
    @DisplayName("POST /api/movies when service throws TransactionSystemException with ConstraintViolation cause returns 400")
    void testCreateMovie_WhenServiceThrowsTransactionConstraintViolation_Returns400() throws Exception {
        ConstraintViolationException cve = new ConstraintViolationException("cv", new HashSet<>());
        TransactionSystemException tse = new TransactionSystemException("tx", cve);

        when(movieService.create(any(MovieDTO.class))).thenThrow(tse);

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Validation failed"))
                .andExpect(jsonPath("$.status").value(400));

        verify(movieService, times(1)).create(any(MovieDTO.class));
    }

    @Test
    @DisplayName("POST /api/movies when role guard throws SecurityException returns 400")
    void testCreateMovie_WhenRoleGuardThrowsSecurityException_Returns400() throws Exception {
        doThrow(new SecurityException("bad token")).when(roleGuard).requireRole(any(), any());

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
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid request"))
                .andExpect(jsonPath("$.status").value(400));

        verify(roleGuard, times(1)).requireRole(any(), any());
    }
}