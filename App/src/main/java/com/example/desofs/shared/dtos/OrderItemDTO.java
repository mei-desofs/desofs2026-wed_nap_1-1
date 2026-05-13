package com.example.desofs.shared.dtos;

import java.math.BigDecimal;

public class OrderItemDTO {
    private Long id;
    private Long movieId;
    private String movieTitle;
    private Integer quantity;
    private BigDecimal unitPrice;
    private BigDecimal subtotal;

    public OrderItemDTO() {}

    public OrderItemDTO(Long id, Long movieId, String movieTitle, Integer quantity, BigDecimal unitPrice) {
        this.id = id;
        this.movieId = movieId;
        this.movieTitle = movieTitle;
        this.quantity = quantity;
        this.unitPrice = unitPrice;
        this.subtotal = unitPrice != null && quantity != null ? unitPrice.multiply(new java.math.BigDecimal(quantity)) : null;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getMovieId() { return movieId; }
    public void setMovieId(Long movieId) { this.movieId = movieId; }
    public String getMovieTitle() { return movieTitle; }
    public void setMovieTitle(String movieTitle) { this.movieTitle = movieTitle; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public BigDecimal getSubtotal() { return subtotal; }
    public void setSubtotal(BigDecimal subtotal) { this.subtotal = subtotal; }
}
