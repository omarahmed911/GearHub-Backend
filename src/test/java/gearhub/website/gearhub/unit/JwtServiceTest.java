package gearhub.website.gearhub.unit;

import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.security.JwtProperties;
import gearhub.website.gearhub.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtServiceTest {

    JwtProperties properties;
    JwtService jwtService;

    @BeforeEach
    void init() {
        properties = new JwtProperties();
        properties.setSecret("0123456789abcdefghijklmnopqrstuvwxyz"); // 36 chars
        properties.setAccessTokenExpirationMs(60_000L);
        jwtService = new JwtService(properties);
    }

    @Test
    void roundTrip_plainTextSecretAtLeast32Bytes() {
        String token = jwtService.generateAccessToken(99L, "x@y.com", Role.CUSTOMER);

        assertThat(jwtService.extractUserId(token)).isEqualTo(99L);
        var claims = jwtService.parseClaims(token);
        assertThat(claims.get("email", String.class)).isEqualTo("x@y.com");
        assertThat(claims.get("role", String.class)).isEqualTo("CUSTOMER");
    }

    @Test
    void signingKey_acceptsBase64Secret32BytesOrMore() {
        byte[] raw = new byte[32];
        for (int i = 0; i < raw.length; i++) {
            raw[i] = (byte) i;
        }
        properties.setSecret(Base64.getEncoder().encodeToString(raw));
        jwtService = new JwtService(properties);

        String token = jwtService.generateAccessToken(1L, "a@b.com", Role.ADMIN);
        assertThat(jwtService.extractUserId(token)).isEqualTo(1L);
    }

    @Test
    void constructor_failsIfSecretTooShort() {
        properties.setSecret("short");
        assertThatThrownBy(() -> new JwtService(properties).generateAccessToken(1L, "a@b.com", Role.CUSTOMER))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("JWT secret");
    }

    @Test
    void extractUserId_rejectsTamperedToken() {
        String token = jwtService.generateAccessToken(1L, "a@b.com", Role.CUSTOMER);
        String tampered = token.substring(0, token.length() - 4) + "XXXX";

        assertThatThrownBy(() -> jwtService.extractUserId(tampered))
                .isInstanceOf(Exception.class);
    }
}
