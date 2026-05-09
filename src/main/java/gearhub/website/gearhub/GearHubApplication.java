package gearhub.website.gearhub;

import gearhub.website.gearhub.config.GearhubBootstrapProperties;
import gearhub.website.gearhub.security.GearhubCorsProperties;
import gearhub.website.gearhub.security.JwtProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties({JwtProperties.class, GearhubCorsProperties.class, GearhubBootstrapProperties.class})
public class GearHubApplication {

    public static void main(String[] args) {
        SpringApplication.run(GearHubApplication.class, args);
    }

}
