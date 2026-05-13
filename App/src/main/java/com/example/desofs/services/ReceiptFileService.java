package com.emovieshop.service;

import com.emovieshop.domain.model.Order;
import com.emovieshop.domain.model.OrderItem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.*;
import java.time.format.DateTimeFormatter;

@Service
public class ReceiptFileService {

    private static final Logger logger = LoggerFactory.getLogger(ReceiptFileService.class);
    private static final DateTimeFormatter TIMESTAMP_FORMAT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    /**
     * Allow-list: only alphanumeric, spaces, hyphens, and underscores.
     * Everything else is stripped.
     */
    private static final String ALLOWED_CHARS_PATTERN = "[^a-zA-Z0-9 _-]";

    private final Path receiptsDirectory;
    private final int maxNameLength;

    public ReceiptFileService(
            @Value("${emovieshop.receipts.directory:./receipts}") String receiptsDir,
            @Value("${emovieshop.receipts.max-name-length:100}") int maxNameLength) {
        this.receiptsDirectory = Paths.get(receiptsDir).toAbsolutePath().normalize();
        this.maxNameLength = maxNameLength;
    }

    public void ensureReceiptsDirectoryExists() throws IOException {
        if (!Files.exists(receiptsDirectory)) {
            Files.createDirectories(receiptsDirectory);
            logger.info("Created receipts directory: {}", receiptsDirectory);
        }
    }

    /**
     * Sanitizes the receipt name using an allow-list approach.
     * Strips path separators, null bytes, and any character outside [a-zA-Z0-9 _-].
     * Truncates to the configured max length.
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
     * Uses CREATE_NEW to fail if the file already exists (prevents overwrites).
     * Verifies the resolved path stays within the receipts directory (path traversal protection).
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

    private String truncate(String text, int maxLen) {
        if (text.length() <= maxLen) {
            return text;
        }
        return text.substring(0, maxLen - 3) + "...";
    }
}
