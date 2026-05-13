package com.example.desofs.domain;

/*
import jakarta.persistence.*;
import java.time.LocalDateTime;
@Entity
@Table(name = "mfa_tokens")
public class MfaToken {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @OneToOne
    @JoinColumn(name = "user_id", unique = true)
    private User user;

    private String totpSecret; // Encrypted TOTP secret
    private String backupCodes; // JSON array of hashed backup codes
    private Boolean isEnabled;
    private String emailOtpCode; // Encrypted temporary email OTP code
    private LocalDateTime emailOtpExpiry;
    private LocalDateTime lastUsedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;

    public MfaToken() {
        this.isEnabled = false;
        this.createdAt = LocalDateTime.now();
        this.updatedAt = LocalDateTime.now();
    }

    public Long getId() { return id; }
    public User getUser() { return user; }
    public void setUser(User user) { this.user = user; }
    public String getTotpSecret() { return totpSecret; }
    public void setTotpSecret(String totpSecret) { this.totpSecret = totpSecret; }
    public String getBackupCodes() { return backupCodes; }
    public void setBackupCodes(String backupCodes) { this.backupCodes = backupCodes; }
    public Boolean getIsEnabled() { return isEnabled; }
    public void setIsEnabled(Boolean isEnabled) { this.isEnabled = isEnabled; this.updatedAt = LocalDateTime.now(); }
    public String getEmailOtpCode() { return emailOtpCode; }
    public void setEmailOtpCode(String emailOtpCode) { this.emailOtpCode = emailOtpCode; }
    public LocalDateTime getEmailOtpExpiry() { return emailOtpExpiry; }
    public void setEmailOtpExpiry(LocalDateTime emailOtpExpiry) { this.emailOtpExpiry = emailOtpExpiry; }
    public LocalDateTime getLastUsedAt() { return lastUsedAt; }
    public void setLastUsedAt(LocalDateTime lastUsedAt) { this.lastUsedAt = lastUsedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getUpdatedAt() { return updatedAt; }
}
*/
