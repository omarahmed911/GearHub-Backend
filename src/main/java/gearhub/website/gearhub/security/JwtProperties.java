package gearhub.website.gearhub.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "gearhub.jwt")
public class JwtProperties {

    /**
     * HS256 secret: at least 32 bytes (256 bits). Prefer a base64-encoded value in production.
     */
    private String secret = "change-me-in-production-use-at-least-32-chars!!";

    private long accessTokenExpirationMs = 86_400_000L;

}
