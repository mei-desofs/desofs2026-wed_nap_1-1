package com.example.desofs.shared.dtos;

import java.math.BigDecimal;

/**
 * Data Transfer Object representing a movie in API responses.
 *
 * <p>Contains the public movie fields exposed by the application layer.</p>
 */
public class MovieDTO {
    /** Movie identifier. */
    private Long id;

    /** Movie title. */
    private String title;

    /** Movie description. */
    private String description;

    /** Movie price. */
    private BigDecimal price;

    /**
     * Creates an empty movie DTO.
     */
    public MovieDTO() {}

    /**
     * Creates a movie DTO with all fields populated.
     *
     * @param id movie identifier
     * @param title movie title
     * @param description movie description
     * @param price movie price
     */
    public MovieDTO(Long id, String title, String description, BigDecimal price) {
        this.id = id;
        this.title = title;
        this.description = description;
        this.price = price;
    }

    /**
     * Returns the movie identifier.
     *
     * @return movie identifier
     */
    public Long getId() { return id; }

    /**
     * Sets the movie identifier.
     *
     * @param id movie identifier
     */
    public void setId(Long id) { this.id = id; }

    /**
     * Returns the movie title.
     *
     * @return movie title
     */
    public String getTitle() { return title; }

    /**
     * Sets the movie title.
     *
     * @param title movie title
     */
    public void setTitle(String title) { this.title = title; }

    /**
     * Returns the movie description.
     *
     * @return movie description
     */
    public String getDescription() { return description; }

    /**
     * Sets the movie description.
     *
     * @param description movie description
     */
    public void setDescription(String description) { this.description = description; }

    /**
     * Returns the movie price.
     *
     * @return movie price
     */
    public BigDecimal getPrice() { return price; }

    /**
     * Sets the movie price.
     *
     * @param price movie price
     */
    public void setPrice(BigDecimal price) { this.price = price; }
}
