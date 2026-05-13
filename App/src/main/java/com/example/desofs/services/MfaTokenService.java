package com.example.desofs.services;

import com.example.desofs.domain.entities.MfaToken;
import com.example.desofs.domain.entities.User;
import com.example.desofs.domain.repositories.MfaTokenRepository;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.Optional;

@Service
public class MfaTokenService {
    private final MfaTokenRepository mfaTokenRepository;

    public MfaTokenService(MfaTokenRepository mfaTokenRepository) {
        this.mfaTokenRepository = mfaTokenRepository;
    }

    public Optional<MfaToken> getByUser(User user) {
        return mfaTokenRepository.findByUser(user);
    }

    public MfaToken createOrUpdate(User user) {
        Optional<MfaToken> existing = mfaTokenRepository.findByUser(user);
        if (existing.isPresent()) {
            return existing.get();
        }
        MfaToken token = new MfaToken();
        token.setUser(user);
        return mfaTokenRepository.save(token);
    }

    public MfaToken enableMfa(User user, String totpSecret, String backupCodes) {
        MfaToken token = createOrUpdate(user);
        token.setTotpSecret(totpSecret);
        token.setBackupCodes(backupCodes);
        token.setIsEnabled(true);
        return mfaTokenRepository.save(token);
    }

    public void disableMfa(User user) {
        mfaTokenRepository.findByUser(user).ifPresent(token -> {
            token.setIsEnabled(false);
            token.setTotpSecret(null);
            token.setBackupCodes(null);
            mfaTokenRepository.save(token);
        });
    }

    public void setEmailOtp(User user, String emailOtpCode, LocalDateTime expiry) {
        mfaTokenRepository.findByUser(user).ifPresent(token -> {
            token.setEmailOtpCode(emailOtpCode);
            token.setEmailOtpExpiry(expiry);
            mfaTokenRepository.save(token);
        });
    }

    public void recordMfaUsage(User user) {
        mfaTokenRepository.findByUser(user).ifPresent(token -> {
            token.setLastUsedAt(LocalDateTime.now());
            mfaTokenRepository.save(token);
        });
    }
}
