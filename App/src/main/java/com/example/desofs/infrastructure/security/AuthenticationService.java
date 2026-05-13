package com.example.desofs.infrastructure.security;

import com.example.desofs.application.services.MfaTokenService;
import com.example.desofs.domain.entities.MfaToken;
import com.example.desofs.domain.entities.User;
import com.example.desofs.domain.repositories.UserRepository;
import org.springframework.stereotype.Service;
import java.util.Optional;
import java.time.LocalDateTime;

/**
 * Authentication Service for handling Auth0 login, token validation and user session management
 * To be implemented with Auth0 integration and account lockout logic
 */
@Service
public class AuthenticationService {

    private static final int MAX_FAILED_ATTEMPTS = 5;
    private static final int LOCK_MINUTES = 15;

    private final JwtTokenProvider jwtTokenProvider;
    private final UserRepository userRepository;
    private final MfaTokenService mfaTokenService;

    public AuthenticationService(JwtTokenProvider jwtTokenProvider, UserRepository userRepository, MfaTokenService mfaTokenService) {
        this.jwtTokenProvider = jwtTokenProvider;
        this.userRepository = userRepository;
        this.mfaTokenService = mfaTokenService;
    }

    /**
     * Authenticate user via Auth0 JWT token
     * @param token JWT token from Auth0
     * @return User if valid, empty if invalid
     */
    public Optional<User> authenticateWithToken(String token) {
        if (!jwtTokenProvider.validateToken(token)) {
            return Optional.empty();
        }

        Optional<String> email = jwtTokenProvider.getEmail(token);
        if (email.isEmpty()) {
            return Optional.empty();
        }

        User user = userRepository.findByEmail(email.get());
        if (user == null || isAccountLocked(user.getId())) {
            return Optional.empty();
        }

        resetFailedLoginAttempts(user.getId());
        return Optional.of(user);
    }

    /**
     * Verify MFA if enabled for user
     * @param userId user ID
     * @param mfaCode MFA code (TOTP or Email OTP)
     * @return true if MFA passes, false otherwise
     */
    public boolean verifyMfa(Long userId, String mfaCode) {
        Optional<User> optionalUser = userRepository.findById(userId);
        if (optionalUser.isEmpty()) {
            return false;
        }

        Optional<MfaToken> token = mfaTokenService.getByUser(optionalUser.get());
        if (token.isEmpty() || !Boolean.TRUE.equals(token.get().getIsEnabled())) {
            return true;
        }

        MfaToken mfaToken = token.get();
        boolean emailOtpValid = mfaToken.getEmailOtpCode() != null
            && mfaToken.getEmailOtpCode().equals(mfaCode)
            && mfaToken.getEmailOtpExpiry() != null
            && mfaToken.getEmailOtpExpiry().isAfter(LocalDateTime.now());

        boolean backupCodeValid = mfaToken.getBackupCodes() != null && mfaToken.getBackupCodes().contains(mfaCode);
        if (emailOtpValid || backupCodeValid) {
            mfaTokenService.recordMfaUsage(optionalUser.get());
            return true;
        }

        return false;
    }

    /**
     * Handle failed login attempt (for account lockout mechanism)
     * @param userId user ID
     */
    public void recordFailedLoginAttempt(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            int attempts = user.getFailedLoginAttempts() == null ? 0 : user.getFailedLoginAttempts();
            user.setFailedLoginAttempts(attempts + 1);
            user.setLastFailedLogin(LocalDateTime.now());
            if (attempts + 1 >= MAX_FAILED_ATTEMPTS) {
                user.setAccountLockedUntil(LocalDateTime.now().plusMinutes(LOCK_MINUTES));
            }
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Check if account is locked
     * @param userId user ID
     * @return true if locked, false otherwise
     */
    public boolean isAccountLocked(Long userId) {
        return userRepository.findById(userId)
            .map(user -> user.getAccountLockedUntil() != null && user.getAccountLockedUntil().isAfter(LocalDateTime.now()))
            .orElse(false);
    }

    /**
     * Reset failed login attempts on successful login
     * @param userId user ID
     */
    public void resetFailedLoginAttempts(Long userId) {
        userRepository.findById(userId).ifPresent(user -> {
            user.setFailedLoginAttempts(0);
            user.setLastFailedLogin(null);
            user.setAccountLockedUntil(null);
            user.setUpdatedAt(LocalDateTime.now());
            userRepository.save(user);
        });
    }

    /**
     * Revoke/logout user token
     * @param token JWT token to revoke
     */
    public void revokeToken(String token) {
        // Stateless placeholder: token revocation must be implemented with Auth0 blacklist/session strategy.
    }
}
