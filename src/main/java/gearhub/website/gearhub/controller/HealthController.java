package gearhub.website.gearhub.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HealthController {

    @GetMapping("/")
    public Map<String, String> rootHealth() {
        return Map.of("status", "UP", "message", "Application is running");
    }

    /**
     * JSON health for frontends and gateways (also see {@code /actuator/health}).
     */
    @GetMapping("/api/health")
    public Map<String, String> apiHealth() {
        return Map.of(
                "status", "UP",
                "checks", "/actuator/health");
    }
}
