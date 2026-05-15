package com.example.desofs.shared.dtos;

import com.example.desofs.domain.Role;

import java.time.LocalDateTime;

/**
 * Data Transfer Object representing an audit log entry that is
 * exchanged over the REST API. This DTO is a flattened, transport-safe
 * representation of the {@code AuditLog} domain entity and intentionally
 * exposes only the fields required by clients.
 */
public class AuditLogDTO {
	private Long id;
	private String actorId;
	private String targetUserId;
	private Role role;
	private String operation;
	private LocalDateTime timestamp;

	/**
	 * Default constructor for frameworks that require a no-args constructor.
	 */
	public AuditLogDTO() {
	}

	/**
	 * Creates a fully-initialized DTO instance.
	 *
	 * @param id identifier of the audit entry
	 * @param actorId id of the user performing the action
	 * @param targetUserId id of the user affected by the action
	 * @param role role involved in the action
	 * @param operation operation name (e.g. "ASSIGN", "REMOVE")
	 * @param timestamp time when the event occurred
	 */
	public AuditLogDTO(Long id, String actorId, String targetUserId, Role role, String operation, LocalDateTime timestamp) {
		this.id = id;
		this.actorId = actorId;
		this.targetUserId = targetUserId;
		this.role = role;
		this.operation = operation;
		this.timestamp = timestamp;
	}

	/**
	 * Returns the audit log identifier.
	 */
	public Long getId() {
		return id;
	}

	/** Sets the audit log identifier. */
	public void setId(Long id) {
		this.id = id;
	}

	/** Returns the id of the actor (user performing the action). */
	public String getActorId() {
		return actorId;
	}

	/** Sets the actor id. */
	public void setActorId(String actorId) {
		this.actorId = actorId;
	}

	/** Returns the id of the target user (user affected by the action). */
	public String getTargetUserId() {
		return targetUserId;
	}

	/** Sets the target user id. */
	public void setTargetUserId(String targetUserId) {
		this.targetUserId = targetUserId;
	}

	/** Returns the role involved in the audit event. */
	public Role getRole() {
		return role;
	}

	/** Sets the role involved in the audit event. */
	public void setRole(Role role) {
		this.role = role;
	}

	/** Returns the operation name recorded in the audit entry. */
	public String getOperation() {
		return operation;
	}

	/** Sets the operation name. */
	public void setOperation(String operation) {
		this.operation = operation;
	}

	/** Returns the timestamp when the audit event occurred. */
	public LocalDateTime getTimestamp() {
		return timestamp;
	}

	/** Sets the timestamp for the audit event. */
	public void setTimestamp(LocalDateTime timestamp) {
		this.timestamp = timestamp;
	}
}
