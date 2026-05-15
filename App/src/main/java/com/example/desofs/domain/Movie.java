package com.example.desofs.domain;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;

@Entity
@Table(name = "movies")
/**
 * Entity representing a movie available for purchase or rental.
 *
 * <p>This class models the persistent state for a movie, including its
 * title, description, genre, supported platform, price and current stock
 * quantity. Instances are managed by JPA/Hibernate.</p>
 */
public class Movie {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank(message = "Title is required")
    @Size(max = 255, message = "Title must not exceed 255 characters")
    @Column(nullable = false)
    private String title;

    @Size(max = 5000, message = "Description must not exceed 5000 characters")
    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank(message = "Genre is required")
    @Size(max = 100, message = "Genre must not exceed 100 characters")
    @Column(nullable = false)
    private String genre;

    @Size(max = 100, message = "Platform must not exceed 100 characters")
    @Column
    private String platform;

    @NotNull(message = "Price is required")
    @DecimalMin(value = "0.0", inclusive = false, message = "Price must be greater than zero")
    @Column(nullable = false, precision = 10, scale = 2)
    private BigDecimal price;

    @NotNull(message = "Stock quantity is required")
    @Min(value = 0, message = "Stock quantity must not be negative")
    @Column(nullable = false)
    private Integer stockQuantity;

    /**
     * Protected no-arg constructor required by JPA.
     *
     * <p>Intended only for use by the persistence framework.</p>
     */
    protected Movie() {
    }

    /**
     * Create a new Movie instance.
     *
     * @param title the movie title (required)
     * @param description the movie description (may be null)
     * @param genre the movie genre (required)
     * @param platform the platform where the movie is available (nullable)
     * @param price the price of the movie (required)
     * @param stockQuantity the initial available stock quantity (required)
     */
    public Movie(String title, String description, String genre, String platform, BigDecimal price, Integer stockQuantity) {
        this.title = title;
        this.description = description;
        this.genre = genre;
        this.platform = platform;
        this.price = price;
        this.stockQuantity = stockQuantity;
    }

    public Long getId() {
        return id;
    }

    /**
     * Returns the database identifier for this movie.
     *
     * @return the movie id, or {@code null} if the entity is not yet persisted
     */

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    /**
     * Returns the movie title.
     *
     * @return the title, never {@code null}
     */

    public String getDescription() {
        return description;
    }

    /**
     * Returns the movie description.
     *
     * @return the description, may be {@code null}
     */

    public String getGenre() {
        return genre;
    }

    /**
     * Returns the genre of the movie.
     *
     * @return the genre, never {@code null}
     */

    public String getPlatform() {
        return platform;
    }

    /**
     * Returns the platform where the movie is available.
     *
     * @return the platform, may be {@code null}
     */

    public BigDecimal getPrice() {
        return price;
    }

    /**
     * Returns the price of the movie.
     *
     * @return the price as a {@link BigDecimal}
     */

    public Integer getStockQuantity() {
        return stockQuantity;
    }

    /**
     * Returns the current available stock quantity.
     *
     * @return the available stock quantity (non-negative)
     */

    /**
     * Decrease the available stock by the specified quantity.
     *
     * @param quantity the number of items to remove from stock; must be positive
     * @throws IllegalArgumentException if {@code quantity} is not positive
     * @throws IllegalStateException if there is insufficient stock available
     */
    public void decreaseStock(int quantity) {
        if (quantity <= 0) {
            throw new IllegalArgumentException("Quantity must be positive");
        }
        if (this.stockQuantity < quantity) {
            throw new IllegalStateException("Insufficient stock for movie: " + this.title);
        }
        this.stockQuantity -= quantity;
    }
}
