package gearhub.website.gearhub.security;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.config.annotation.authentication.configuration.AuthenticationConfiguration;
import org.springframework.security.config.annotation.method.configuration.EnableMethodSecurity;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

import java.util.List;

@Configuration
@EnableMethodSecurity
public class SecurityConfig {

    private final JwtAuthenticationFilter jwtAuthenticationFilter;
    private final JsonAuthEntryPoint jsonAuthEntryPoint;
    private final JsonAccessDeniedHandler jsonAccessDeniedHandler;
    private final GearhubCorsProperties corsProperties;

    public SecurityConfig(
            JwtAuthenticationFilter jwtAuthenticationFilter,
            JsonAuthEntryPoint jsonAuthEntryPoint,
            JsonAccessDeniedHandler jsonAccessDeniedHandler,
            GearhubCorsProperties corsProperties) {
        this.jwtAuthenticationFilter = jwtAuthenticationFilter;
        this.jsonAuthEntryPoint = jsonAuthEntryPoint;
        this.jsonAccessDeniedHandler = jsonAccessDeniedHandler;
        this.corsProperties = corsProperties;
    }

    @Bean
    public SecurityFilterChain filterChain(HttpSecurity http) throws Exception {
        http
                .csrf(AbstractHttpConfigurer::disable)
                .cors(c -> c.configurationSource(corsConfigurationSource()))
                .sessionManagement(sm -> sm.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .exceptionHandling(e -> e
                        .authenticationEntryPoint(jsonAuthEntryPoint)
                        .accessDeniedHandler(jsonAccessDeniedHandler))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers("/", "/error").permitAll()
                        .requestMatchers(HttpMethod.GET, "/api/health", "/actuator/health").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/register", "/api/auth/login").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/auth/logout").authenticated()
                        .requestMatchers(HttpMethod.GET, "/api/products/mine").hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.GET, "/api/products", "/api/products/*").permitAll()
                        .requestMatchers(HttpMethod.POST, "/api/products").hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers(HttpMethod.PUT, "/api/products/*").authenticated()
                        .requestMatchers(HttpMethod.DELETE, "/api/products/*").authenticated()
                        .requestMatchers(HttpMethod.POST, "/api/orders", "/api/orders/checkout").hasRole("CUSTOMER")
                        .requestMatchers(HttpMethod.PUT, "/api/orders/*/status").hasAnyRole("TRADER", "ADMIN")
                        .requestMatchers("/api/cart/**").hasRole("CUSTOMER")
                        .requestMatchers("/api/admin/**").hasRole("ADMIN")
                        .anyRequest().authenticated())
                .addFilterBefore(jwtAuthenticationFilter, UsernamePasswordAuthenticationFilter.class);

        return http.build();
    }

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration config = new CorsConfiguration();
        config.setAllowedOriginPatterns(List.of(corsProperties.getAllowedOriginPatterns()));
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    @Bean
    public AuthenticationManager authenticationManager(AuthenticationConfiguration configuration) throws Exception {
        return configuration.getAuthenticationManager();
    }
}
