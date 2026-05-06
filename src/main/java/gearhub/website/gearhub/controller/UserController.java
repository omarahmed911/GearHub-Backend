package gearhub.website.gearhub.controller;
import gearhub.website.gearhub.dto.UserDto;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.service.UserService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
@RestController
@RequestMapping("/api/auth")
public class UserController {
    private final UserService userService;
    public UserController(UserService userService) {
        this.userService = userService;
    }
    @PostMapping("/register")
    public User register(@RequestBody UserDto dto) {
        return userService.registerUser(dto);
    }
    // Login in basic auth is usually handled by Spring Security setup, 
    // but adding an explicit endpoint for clarity.
    @PostMapping("/login")
    public java.util.Map<String, String> login(@RequestBody UserDto dto) {
        return java.util.Map.of("message", "User " + dto.getUsername() + " essentially authenticated by Spring Security basic auth filter");
    }
}
