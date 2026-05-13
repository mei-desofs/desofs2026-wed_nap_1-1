package com.example.desofs.shared.dtos;

import java.math.BigDecimal;
import java.util.List;

public class OrderDTO {
    private Long id;
    private Long userId;
    private String userEmail;
    private List<OrderItemDTO> items;
    private BigDecimal total;

    public OrderDTO() {}

    public OrderDTO(Long id, Long userId, String userEmail, List<OrderItemDTO> items, BigDecimal total) {
        this.id = id;
        this.userId = userId;
        this.userEmail = userEmail;
        this.items = items;
        this.total = total;
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getUserEmail() { return userEmail; }
    public void setUserEmail(String userEmail) { this.userEmail = userEmail; }
    public List<OrderItemDTO> getItems() { return items; }
    public void setItems(List<OrderItemDTO> items) { this.items = items; }
    public BigDecimal getTotal() { return total; }
    public void setTotal(BigDecimal total) { this.total = total; }
}
