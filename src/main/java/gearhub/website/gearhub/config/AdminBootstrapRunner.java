package gearhub.website.gearhub.config;

import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Component
@Profile("!test")
public class AdminBootstrapRunner implements ApplicationRunner {

    private static final Logger log = LoggerFactory.getLogger(AdminBootstrapRunner.class);

    private final GearhubBootstrapProperties properties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public AdminBootstrapRunner(
            GearhubBootstrapProperties properties,
            UserRepository userRepository,
            PasswordEncoder passwordEncoder) {
        this.properties = properties;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!properties.isEnabled()) {
            return;
        }
        if (!StringUtils.hasText(properties.getAdminEmail())
                || !StringUtils.hasText(properties.getAdminPassword())) {
            log.warn(
                    "Bootstrap admin is enabled but gearhub.bootstrap.admin-email or admin-password is missing; skipping.");
            return;
        }
        if (userRepository.existsByEmailIgnoreCase(properties.getAdminEmail().trim())) {
            log.info("Bootstrap admin skipped: email '{}' already exists.", properties.getAdminEmail().trim());
            return;
        }
        User admin = new User();
        admin.setUsername(properties.getAdminUsername().trim());
        admin.setEmail(properties.getAdminEmail().trim());
        admin.setPassword(passwordEncoder.encode(properties.getAdminPassword()));
        admin.setRole(Role.ADMIN);
        userRepository.save(admin);
        log.info("Created bootstrap admin user for email '{}'. Change the password after first login.", admin.getEmail());
    }
}
