package com.example.desofs.exceptions;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;

import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Path;
import org.springframework.transaction.TransactionSystemException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.Map;
import java.util.Set;
import java.util.HashSet;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler handler = new GlobalExceptionHandler();

    @Test
    @DisplayName("handleConstraintViolation formats messages from ConstraintViolation set")
    void testHandleConstraintViolation_withViolations() {
        ConstraintViolation<?> cv = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("title");
        when(cv.getPropertyPath()).thenReturn(path);
        when(cv.getMessage()).thenReturn("must not be blank");

        Set<ConstraintViolation<?>> set = new HashSet<>();
        set.add(cv);

        ConstraintViolationException ex = new ConstraintViolationException("cv", set);
        var resp = handler.handleConstraintViolation(ex);

        assertEquals(400, resp.getStatusCodeValue());
        Map<String, Object> body = resp.getBody();
        assertNotNull(body);
        assertTrue(body.get("message").toString().contains("title: must not be blank"));
    }

    @Test
    @DisplayName("handleTransactionSystem delegates to constraint violation when cause present")
    void testHandleTransactionSystem_withConstraintCause() {
        ConstraintViolation<?> cv = mock(ConstraintViolation.class);
        Path path = mock(Path.class);
        when(path.toString()).thenReturn("field");
        when(cv.getPropertyPath()).thenReturn(path);
        when(cv.getMessage()).thenReturn("invalid");

        Set<ConstraintViolation<?>> set = new HashSet<>();
        set.add(cv);
        ConstraintViolationException cve = new ConstraintViolationException("cv", set);

        TransactionSystemException tse = new TransactionSystemException("tx", cve);

        var resp = handler.handleTransactionSystem(tse);

        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().get("message").toString().contains("field: invalid"));
    }

    @Test
    @DisplayName("handleTransactionSystem returns 500 for non-constraint causes")
    void testHandleTransactionSystem_withoutConstraintCause() {
        TransactionSystemException tse = new TransactionSystemException("tx");
        var resp = handler.handleTransactionSystem(tse);
        assertEquals(500, resp.getStatusCodeValue());
        assertEquals("An unexpected error occurred", resp.getBody().get("message"));
    }

    @Test
    @DisplayName("handleDataIntegrity returns 400 with sanitized message")
    void testHandleDataIntegrity() {
        DataIntegrityViolationException ex = new DataIntegrityViolationException("dup");
        var resp = handler.handleDataIntegrity(ex);
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().get("message").toString().startsWith("Invalid data"));
    }

    @Test
    @DisplayName("handleNoResourceFound returns 404 and generic message")
    void testHandleNoResourceFound() {
        NoResourceFoundException ex = new NoResourceFoundException(HttpMethod.GET, "not found");
        var resp = handler.handleNoResourceFound(ex);
        assertEquals(404, resp.getStatusCodeValue());
        assertEquals("Resource not found", resp.getBody().get("message"));
    }

    @Test
    @DisplayName("handleSecurityException returns 400 and 'Invalid request'")
    void testHandleSecurityException() {
        SecurityException ex = new SecurityException("bad");
        var resp = handler.handleSecurityException(ex);
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Invalid request", resp.getBody().get("message"));
    }

    @Test
    @DisplayName("handleValidation composes field error messages")
    void testHandleValidation_composesFieldErrors() {
        MethodArgumentNotValidException ex = mock(MethodArgumentNotValidException.class);
        BindingResult br = mock(BindingResult.class);
        when(br.getFieldErrors()).thenReturn(java.util.List.of(new FieldError("obj","title","Title cannot be blank")));
        when(ex.getBindingResult()).thenReturn(br);

        var resp = handler.handleValidation(ex);
        assertEquals(400, resp.getStatusCodeValue());
        assertTrue(resp.getBody().get("message").toString().contains("title: Title cannot be blank"));
    }

    @Test
    @DisplayName("handleTypeMismatch returns 400 and 'Invalid path parameter'")
    void testHandleTypeMismatch() {
        MethodArgumentTypeMismatchException ex = mock(MethodArgumentTypeMismatchException.class);
        var resp = handler.handleTypeMismatch(ex);
        assertEquals(400, resp.getStatusCodeValue());
        assertEquals("Invalid path parameter", resp.getBody().get("message"));
    }
}
