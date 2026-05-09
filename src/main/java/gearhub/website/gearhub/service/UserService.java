package gearhub.website.gearhub.service;

import gearhub.website.gearhub.dto.AuthResponse;
import gearhub.website.gearhub.dto.LoginRequest;
import gearhub.website.gearhub.dto.RegisterRequest;
import gearhub.website.gearhub.dto.UserResponse;
import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.UserRepository;
import gearhub.website.gearhub.security.JwtProperties;
import gearhub.website.gearhub.security.JwtService;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final JwtProperties jwtProperties;
    private final AuthenticationManager authenticationManager;

    public UserService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService,
            JwtProperties jwtProperties,
            AuthenticationManager authenticationManager) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
        this.jwtProperties = jwtProperties;
        this.authenticationManager = authenticationManager;
    }

    @Transactional
    public UserResponse register(RegisterRequest dto) {
        if (dto.getRole() == Role.ADMIN) {
            throw new ResponseStatusException(
                    HttpStatus.BAD_REQUEST,
                    "Admin accounts cannot be created through public registration.");
        }
        if (dto.getRole() != Role.TRADER && dto.getRole() != Role.CUSTOMER) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid registration role.");
        }
        userRepository.findByUsername(dto.getUsername().trim()).ifPresent(u -> {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Username already exists.");
        });
        if (userRepository.existsByEmailIgnoreCase(dto.getEmail().trim())) {
            throw new ResponseStatusException(HttpStatus.CONFLICT, "Email already registered.");
        }
        User user = new User();
        user.setUsername(dto.getUsername().trim());
        user.setEmail(dto.getEmail().trim());
        user.setPassword(passwordEncoder.encode(dto.getPassword()));
        user.setRole(dto.getRole());
        User saved = userRepository.save(user);
        return UserMapper.toResponse(saved);
    }

    public AuthResponse login(LoginRequest request) {
        try {
            var token = UsernamePasswordAuthenticationToken.unauthenticated(
                    request.getEmail().trim(), request.getPassword());
            authenticationManager.authenticate(token);
        } catch (BadCredentialsException e) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password.");
        }
        User user = userRepository.findByEmailIgnoreCase(request.getEmail().trim()).orElseThrow(
                () -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Invalid email or password."));
        String jwt = jwtService.generateAccessToken(user.getId(), user.getEmail(), user.getRole());
        return new AuthResponse(jwt, jwtProperties.getAccessTokenExpirationMs(), UserMapper.toResponse(user));
    }

}
