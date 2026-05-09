package gearhub.website.gearhub.controller;

import gearhub.website.gearhub.dto.AuthResponse;
import gearhub.website.gearhub.dto.LoginRequest;
import gearhub.website.gearhub.dto.RegisterRequest;
import gearhub.website.gearhub.dto.UserResponse;
import gearhub.website.gearhub.security.AuthSupport;
import gearhub.website.gearhub.service.UserService;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return userService.register(request);
    }

    @PostMapping("/login")
    public AuthResponse login(@Valid @RequestBody LoginRequest request) {
        return userService.login(request);
    }

    @PostMapping("/logout")
    public ResponseEntity<java.util.Map<String, String>> logout(Authentication authentication) {
        AuthSupport.requireUser(authentication);
        return ResponseEntity.accepted()
                .body(java.util.Map.of(
                        "message",
                        "Token invalidated on the client side. JWTs are stateless; discard locally."));
    }
}
