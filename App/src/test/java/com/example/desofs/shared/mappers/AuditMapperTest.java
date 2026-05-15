package com.example.desofs.shared.mappers;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.domain.Role;
import com.example.desofs.shared.dtos.AuditLogDTO;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class AuditMapperTest {

    @Test
    void toDTO_mapsAuditLogFields() {
        AuditLog audit = AuditLog.of("actor", "target", Role.ADMIN, "ASSIGN");

        AuditMapper mapper = new AuditMapper();
        AuditLogDTO dto = mapper.toDTO(audit);

        assertThat(dto).isNotNull();
        assertThat(dto.getActorId()).isEqualTo("actor");
        assertThat(dto.getTargetUserId()).isEqualTo("target");
        assertThat(dto.getRole()).isEqualTo(Role.ADMIN);
        assertThat(dto.getOperation()).isEqualTo("ASSIGN");
        assertThat(dto.getTimestamp()).isNotNull();
    }
}