package gearhub.website.gearhub.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

@Setter
@Getter
@ConfigurationProperties(prefix = "gearhub.bootstrap")
public class GearhubBootstrapProperties {

    private boolean enabled = false;
    private String adminUsername = "Administrator";
    private String adminEmail;
    private String adminPassword;

}
