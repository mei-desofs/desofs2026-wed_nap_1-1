package com.example.desofs.services;

import com.example.desofs.domain.AuditLog;
import com.example.desofs.domain.Role;
import com.example.desofs.repositories.AuditLogRepository;
import com.example.desofs.shared.dtos.AuditLogDTO;
import com.example.desofs.shared.mappers.IAuditMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuditLogService Unit Tests")
class AuditLogServiceTest {

    @Mock
    private AuditLogRepository auditLogRepository;

    @Mock
    private IAuditMapper auditMapper;

    @InjectMocks
    private AuditLogService auditLogService;

    private AuditLog testAuditLog;

    @BeforeEach
    void setUp() {
        testAuditLog = AuditLog.of("auth0|admin", "auth0|user", Role.SUPPORT, "ASSIGN");
        setId(testAuditLog, 1L);
    }

    private void setupAuditMapper() {
        when(auditMapper.toDTO(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog auditLog = inv.getArgument(0);
            return new AuditLogDTO(
                auditLog.getId(),
                auditLog.getActorId(),
                auditLog.getTargetUserId(),
                auditLog.getRole(),
                auditLog.getOperation(),
                auditLog.getTimestamp()
            );
        });
    }

    @Test
    @DisplayName("listAll() should return all audit logs as DTOs")
    void listAll_returnsAllAuditLogs() {
        setupAuditMapper();
        when(auditLogRepository.findAll()).thenReturn(List.of(testAuditLog));

        List<AuditLogDTO> result = auditLogService.listAll();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(1L);
        assertThat(result.get(0).getActorId()).isEqualTo("auth0|admin");
        assertThat(result.get(0).getTargetUserId()).isEqualTo("auth0|user");
        assertThat(result.get(0).getRole()).isEqualTo(Role.SUPPORT);
        assertThat(result.get(0).getOperation()).isEqualTo("ASSIGN");
    }

    @Test
    @DisplayName("listAll() should return empty list when no audit logs exist")
    void listAll_returnsEmptyList() {
        when(auditLogRepository.findAll()).thenReturn(List.of());

        List<AuditLogDTO> result = auditLogService.listAll();

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("get(id) should return audit log when it exists")
    void get_returnsAuditLogWhenFound() {
        setupAuditMapper();
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(testAuditLog));

        Optional<AuditLogDTO> result = auditLogService.get(1L);

        assertThat(result).isPresent();
        assertThat(result.get().getId()).isEqualTo(1L);
        assertThat(result.get().getOperation()).isEqualTo("ASSIGN");
    }

    @Test
    @DisplayName("get(id) should return empty optional when audit log does not exist")
    void get_returnsEmptyWhenNotFound() {
        when(auditLogRepository.findById(999L)).thenReturn(Optional.empty());

        Optional<AuditLogDTO> result = auditLogService.get(999L);

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("log() should persist and return mapped audit log")
    void log_persistsAndReturnsMappedAuditLog() {
        setupAuditMapper();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog saved = inv.getArgument(0);
            setId(saved, 10L);
            return saved;
        });

        AuditLogDTO result = auditLogService.log("auth0|admin", "auth0|user", Role.ADMIN, "ASSIGN");

        assertThat(result.getId()).isEqualTo(10L);
        assertThat(result.getActorId()).isEqualTo("auth0|admin");
        assertThat(result.getTargetUserId()).isEqualTo("auth0|user");
        assertThat(result.getRole()).isEqualTo(Role.ADMIN);
        assertThat(result.getOperation()).isEqualTo("ASSIGN");
        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    @Test
    @DisplayName("logRoleAssignment() should use ASSIGN operation")
    void logRoleAssignment_usesAssignOperation() {
        setupAuditMapper();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog saved = inv.getArgument(0);
            setId(saved, 11L);
            return saved;
        });

        AuditLogDTO result = auditLogService.logRoleAssignment("auth0|admin", "auth0|user", Role.SUPPORT);

        assertThat(result.getOperation()).isEqualTo("ASSIGN");
        assertThat(result.getRole()).isEqualTo(Role.SUPPORT);
    }

    @Test
    @DisplayName("logRoleRemoval() should use REMOVE operation")
    void logRoleRemoval_usesRemoveOperation() {
        setupAuditMapper();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> {
            AuditLog saved = inv.getArgument(0);
            setId(saved, 12L);
            return saved;
        });

        AuditLogDTO result = auditLogService.logRoleRemoval("auth0|admin", "auth0|user", Role.CUSTOMER);

        assertThat(result.getOperation()).isEqualTo("REMOVE");
        assertThat(result.getRole()).isEqualTo(Role.CUSTOMER);
    }

    @Test
    @DisplayName("listAll() should call repository findAll exactly once")
    void listAll_callsRepositoryOnce() {
        when(auditLogRepository.findAll()).thenReturn(List.of());

        auditLogService.listAll();

        verify(auditLogRepository, times(1)).findAll();
    }

    @Test
    @DisplayName("get() should call repository findById exactly once")
    void get_callsRepositoryOnce() {
        when(auditLogRepository.findById(1L)).thenReturn(Optional.of(testAuditLog));

        auditLogService.get(1L);

        verify(auditLogRepository, times(1)).findById(1L);
    }

    @Test
    @DisplayName("log() should call repository save exactly once")
    void log_callsRepositorySaveOnce() {
        setupAuditMapper();
        when(auditLogRepository.save(any(AuditLog.class))).thenAnswer(inv -> inv.getArgument(0));

        auditLogService.log("auth0|admin", "auth0|user", Role.ADMIN, "ASSIGN");

        verify(auditLogRepository, times(1)).save(any(AuditLog.class));
    }

    // Helper method to set the ID of an entity using reflection
    private void setId(Object entity, Long id) {
        try {
            Field field = entity.getClass().getDeclaredField("id");
            field.setAccessible(true);
            field.set(entity, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }
}
