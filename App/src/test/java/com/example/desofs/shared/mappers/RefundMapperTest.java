package com.example.desofs.shared.mappers;

import com.example.desofs.domain.Order;
import com.example.desofs.domain.RefundRequest;
import com.example.desofs.shared.dtos.RefundRequestDTO;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class RefundMapperTest {

    @Test
    void toDTO_mapsRefundRequestFields() {
        Order order = new Order("auth0|u", "r.pdf");
        order.setId(555L);

        RefundRequest refund = new RefundRequest(order, "auth0|u", new BigDecimal("5.00"), "Reason");

        RefundMapper mapper = new RefundMapper();
        RefundRequestDTO dto = mapper.toDTO(refund);

        assertThat(dto).isNotNull();
        assertThat(dto.getOrderId()).isEqualTo(555L);
        assertThat(dto.getUserId()).isEqualTo("auth0|u");
        assertThat(dto.getAmount()).isEqualByComparingTo(new BigDecimal("5.00"));
        assertThat(dto.getReason()).isEqualTo("Reason");
        assertThat(dto.getStatus()).isEqualTo(refund.getStatus().toString());
    }
}
