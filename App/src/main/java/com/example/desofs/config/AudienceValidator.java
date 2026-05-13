package com.example.desofs.config;

import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2TokenValidator;
import org.springframework.security.oauth2.core.OAuth2TokenValidatorResult;
import org.springframework.security.oauth2.jwt.Jwt;

/**
 * Validates that the JWT contains the expected audience claim.
 */
public class AudienceValidator implements OAuth2TokenValidator<Jwt> {

    /**
     * Expected audience value that must be present in the JWT audience claim.
     */
    private final String audience;

    /**
     * Creates an audience validator for JWT tokens.
     *
     * @param audience expected audience claim value
     */
    public AudienceValidator(String audience) {
        this.audience = audience;
    }

    /**
     * Validates that the token includes the configured audience.
     *
     * @param jwt token to validate
     * @return success when the audience is present, otherwise a failure result with an
     *         {@code invalid_token} error
     */
    @Override
    public OAuth2TokenValidatorResult validate(Jwt jwt) {
        if (jwt.getAudience() != null && jwt.getAudience().contains(audience)) {
            return OAuth2TokenValidatorResult.success();
        }
        OAuth2Error error = new OAuth2Error("invalid_token", "The required audience is missing", null);
        return OAuth2TokenValidatorResult.failure(error);
    }
}
