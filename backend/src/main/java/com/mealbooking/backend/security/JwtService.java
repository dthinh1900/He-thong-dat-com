package com.mealbooking.backend.security;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mealbooking.backend.application.model.AuthenticatedAccount;
import com.mealbooking.backend.application.port.out.SessionTokenIssuer;
import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;
import java.time.Clock;
import java.time.Instant;
import java.util.Base64;
import java.util.Map;
import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JwtService implements SessionTokenIssuer {
    private static final Logger log = LoggerFactory.getLogger(JwtService.class);
    private static final SecureRandom RANDOM = new SecureRandom();
    private final byte[] secret;
    private final long expirationMinutes;
    private final Clock clock;
    private final ObjectMapper objectMapper;

    public JwtService(
        @Value("${app.jwt.secret}") String secret,
        @Value("${app.jwt.expiration-minutes:480}") long expirationMinutes,
        Clock clock,
        ObjectMapper objectMapper,
        Environment environment
    ) {
        String effectiveSecret = secret;
        if (!StringUtils.hasText(effectiveSecret)
                && environment.acceptsProfiles(Profiles.of("local"))) {
            byte[] generated = new byte[32];
            RANDOM.nextBytes(generated);
            effectiveSecret = Base64.getUrlEncoder().withoutPadding().encodeToString(generated);
            log.warn("JWT_SECRET is empty; generated an ephemeral secret for the local profile. "
                    + "Existing sessions will be invalid after restart.");
        }
        if (!StringUtils.hasText(effectiveSecret)
                || effectiveSecret.getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT_SECRET phải có ít nhất 32 byte.");
        }
        this.secret = effectiveSecret.getBytes(StandardCharsets.UTF_8);
        this.expirationMinutes = expirationMinutes;
        this.clock = clock;
        this.objectMapper = objectMapper;
    }

    @Override
    public String issue(AuthenticatedAccount account) {
        try {
            Instant now = Instant.now(clock);
            String header = encode(objectMapper.writeValueAsBytes(Map.of("alg", "HS256", "typ", "JWT")));
            String payload = encode(objectMapper.writeValueAsBytes(Map.of(
                "sub", account.username(),
                "role", account.role().name(),
                "uid", account.id(),
                "iat", now.getEpochSecond(),
                "exp", now.plusSeconds(expirationMinutes * 60).getEpochSecond()
            )));
            String unsigned = header + "." + payload;
            return unsigned + "." + sign(unsigned);
        } catch (Exception exception) {
            throw new IllegalStateException("Không thể tạo access token.", exception);
        }
    }

    public String extractUsername(String token) { return extractUsername(token, null); }

    public String extractUsername(String token, Long expectedUserId) {
        try {
            String[] parts = token.split("\\.");
            if (parts.length != 3 || !constantTimeEquals(sign(parts[0] + "." + parts[1]), parts[2])) return null;
            Map<?, ?> claims = objectMapper.readValue(Base64.getUrlDecoder().decode(parts[1]), Map.class);
            Object expiry = claims.get("exp");
            if (!(expiry instanceof Number number) || number.longValue() <= Instant.now(clock).getEpochSecond()) return null;
            Object uid = claims.get("uid");
            if (!(uid instanceof Number numberId) || (expectedUserId != null && numberId.longValue() != expectedUserId)) return null;
            Object subject = claims.get("sub");
            return subject instanceof String value ? value : null;
        } catch (Exception exception) {
            return null;
        }
    }

    private String sign(String value) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        mac.init(new SecretKeySpec(secret, "HmacSHA256"));
        return Base64.getUrlEncoder().withoutPadding().encodeToString(mac.doFinal(value.getBytes(StandardCharsets.UTF_8)));
    }
    private static String encode(byte[] bytes) { return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes); }
    private static boolean constantTimeEquals(String expected, String actual) {
        return java.security.MessageDigest.isEqual(expected.getBytes(StandardCharsets.UTF_8), actual.getBytes(StandardCharsets.UTF_8));
    }
}
