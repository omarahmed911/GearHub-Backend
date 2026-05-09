package gearhub.website.gearhub.security;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "gearhub.cors")
public class GearhubCorsProperties {

    private String[] allowedOriginPatterns = {"http://localhost:*"};

}
