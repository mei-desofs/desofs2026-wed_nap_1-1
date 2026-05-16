package com.example.desofs.shared.mappers;

import com.example.desofs.domain.Movie;
import com.example.desofs.domain.Order;
import com.example.desofs.domain.OrderItem;
import com.example.desofs.shared.dtos.OrderResponseDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class OrderMapperTest {

    @Test
    void toResponseDTO_mapsOrderAndItems() {
        Movie movie = new Movie("Title", "Desc", "Genre", "Platform", new BigDecimal("9.99"), 5);
        movie.setId(10L);

        Order order = new Order("auth0|user", "receipt.pdf");
        order.setId(100L);

        OrderItem item = new OrderItem(movie, 2, movie.getPrice());
        order.addItem(item);

        OrderMapper mapper = new OrderMapper();
        OrderResponseDTO dto = mapper.toResponseDTO(order);

        assertThat(dto).isNotNull();
        assertThat(dto.orderId()).isEqualTo(100L);
        assertThat(dto.items()).hasSize(1);
        assertThat(dto.items().get(0).movieId()).isEqualTo(10L);
        assertThat(dto.items().get(0).movieTitle()).isEqualTo("Title");
        assertThat(dto.totalPrice()).isEqualByComparingTo(new BigDecimal("19.98"));
    }
}