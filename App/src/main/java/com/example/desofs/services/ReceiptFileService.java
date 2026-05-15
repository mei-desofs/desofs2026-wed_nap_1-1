package com.example.desofs.services;

import com.example.desofs.domain.Order;
import com.example.desofs.domain.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.time.format.DateTimeFormatter;

/**
 * Service for managing receipt file creation and sanitization.
 * <p>
 * Provides secure receipt name validation using an allow-list approach, safe file
 * creation within a sandboxed directory with path traversal protection, and
 * formatted receipt content generation.
 */
@Service
public final class ReceiptFileService {

    /** Logger for file operations and errors. */
    private static final Logger logger = LoggerFactory.getLogger(ReceiptFileService.class);

    /** Date/time format for receipt timestamps. */
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Allow-list pattern for receipt name sanitization:
     * only alphanumeric, spaces, hyphens, and underscores are kept.
     * All other characters are stripped.
     */
    private static final String ALLOWED_CHARS_PATTERN = "[^a-zA-Z0-9 _-]";

    /** Absolute normalized path to the sandboxed receipts directory. */
    private final Path receiptsDirectory;

    /** Maximum allowed length for sanitized receipt names. */
    private final int maxNameLength;

    /**
     * Constructs the service with configured receipt directory and name constraints.
     *
     * @param receiptsDir directory path for storing receipts (defaults to ./receipts)
     * @param maxNameLength maximum characters allowed in sanitized receipt names (defaults to 100)
     */
    public ReceiptFileService(
            @Value("${emovieshop.receipts.directory:./receipts}") String receiptsDir,
            @Value("${emovieshop.receipts.max-name-length:100}") int maxNameLength) {
        Path path = Path.of(receiptsDir).toAbsolutePath().normalize();
        if (path.toString().contains("..")) {
            throw new IllegalArgumentException("Receipts directory contains invalid path components");
        }
        this.receiptsDirectory = path;
        this.maxNameLength = maxNameLength;
    }

    /**
     * Ensures the receipts directory exists, creating it if necessary.
     *
     * @throws IOException if directory creation fails
     */
    public void ensureReceiptsDirectoryExists() throws IOException {
        if (!Files.exists(receiptsDirectory)) {
            Files.createDirectories(receiptsDirectory);
            logger.info("Created receipts directory: {}", receiptsDirectory);
        }
    }

    /**
     * Sanitizes the receipt name using an allow-list approach.
     * <p>
     * Removes null bytes, path separators, and any character outside
     * [a-zA-Z0-9 _-]. Truncates to the configured maximum length.
     *
     * @param rawName unsanitized receipt name
     * @return sanitized receipt name
     * @throws IllegalArgumentException if name is blank or becomes blank after sanitization
     */
    public String sanitizeReceiptName(String rawName) {
        if (rawName == null || rawName.isBlank()) {
            throw new IllegalArgumentException("Receipt name must not be blank");
        }

        // Strip null bytes
        String sanitized = rawName.replace("\0", "");

        // Apply allow-list: keep only alphanumeric, spaces, hyphens, underscores
        sanitized = sanitized.replaceAll(ALLOWED_CHARS_PATTERN, "");

        // Trim whitespace
        sanitized = sanitized.trim();

        // Truncate to max length
        if (sanitized.length() > maxNameLength) {
            sanitized = sanitized.substring(0, maxNameLength);
        }

        if (sanitized.isBlank()) {
            throw new IllegalArgumentException("Receipt name contains no valid characters after sanitization");
        }

        return sanitized;
    }

    /**
     * Creates a receipt .txt file inside the sandboxed receipts directory.
     * <p>
     * Uses CREATE_NEW to fail if the file already exists (prevents overwrites).
     * Verifies the resolved path stays within the receipts directory to prevent
     * path traversal attacks.
     *
     * @param order the order for which to create a receipt
     * @return the absolute path to the created receipt file
     * @throws IOException if file creation fails
     * @throws SecurityException if path traversal is detected
     */
    public Path createReceiptFile(Order order) throws IOException {
        ensureReceiptsDirectoryExists();

        String sanitizedName = sanitizeReceiptName(order.getReceiptName());
        String filename = sanitizedName + "_" + order.getId() + ".txt";

        Path filePath = receiptsDirectory.resolve(filename).toAbsolutePath().normalize();

        // Path traversal defense: verify the resolved path is inside the sandboxed directory
        if (!filePath.startsWith(receiptsDirectory)) {
            logger.error("Path traversal attempt detected. Attempted path: {}", filePath);
            throw new SecurityException("Invalid receipt file path");
        }

        String content = buildReceiptContent(order);

        // CREATE_NEW ensures the file does not already exist (O_EXCL equivalent)
        Files.writeString(filePath, content, StandardOpenOption.CREATE_NEW, StandardOpenOption.WRITE);

        logger.info("Receipt file created: {} for order {}", filename, order.getId());
        return filePath;
    }

    /**
     * Builds the formatted receipt content as a plain text string.
     * <p>
     * Includes order details, itemized list with quantities and prices,
     * and order total.
     *
     * @param order the order to build receipt content for
     * @return formatted receipt content
     */
    private String buildReceiptContent(Order order) {
        StringBuilder sb = new StringBuilder();
        sb.append("========================================\n");
        sb.append("           eMovieShop Receipt\n");
        sb.append("========================================\n");
        sb.append("Order ID   : ").append(order.getId()).append("\n");
        sb.append("Date       : ").append(order.getCreatedAt().format(TIMESTAMP_FORMAT)).append("\n");
        sb.append("Receipt    : ").append(order.getReceiptName()).append("\n");
        sb.append("----------------------------------------\n");
        sb.append(String.format("%-30s %5s %10s%n", "Movie", "Qty", "Price"));
        sb.append("----------------------------------------\n");

        for (OrderItem item : order.getItems()) {
            sb.append(String.format("%-30s %5d %10s%n",
                    truncate(item.getMovie().getTitle(), 30),
                    item.getQuantity(),
                    item.getSubtotal().toPlainString()));
        }

        sb.append("----------------------------------------\n");
        sb.append(String.format("%-30s %5s %10s%n", "TOTAL", "", order.getTotalPrice().toPlainString()));
        sb.append("========================================\n");
        return sb.toString();
    }

    /**
     * Truncates text to the specified maximum length, appending "..." if truncated.
     *
     * @param text the text to truncate
     * @param maxLen maximum allowed length
     * @return truncated text, or original if it fits within maxLen
     */
    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }
}
