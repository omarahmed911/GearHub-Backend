package gearhub.website.gearhub.unit;

import gearhub.website.gearhub.dto.LoginRequest;
import gearhub.website.gearhub.dto.RegisterRequest;
import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.UserRepository;
import gearhub.website.gearhub.security.JwtProperties;
import gearhub.website.gearhub.security.JwtService;
import gearhub.website.gearhub.service.UserService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock
    UserRepository userRepository;

    @Mock
    PasswordEncoder passwordEncoder;

    @Mock
    JwtService jwtService;

    @Mock
    JwtProperties jwtProperties;

    @Mock
    AuthenticationManager authenticationManager;

    @InjectMocks
    UserService userService;

    @Test
    void register_persistsCustomerAndEncodesPassword() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("Alice");
        req.setEmail("alice@market.test");
        req.setPassword("secretpass");
        req.setRole(Role.CUSTOMER);

        when(userRepository.findByUsername("Alice")).thenReturn(Optional.empty());
        when(userRepository.existsByEmailIgnoreCase("alice@market.test")).thenReturn(false);
        when(passwordEncoder.encode("secretpass")).thenReturn("$2a$BCRYPTHASH");
        when(userRepository.save(any(User.class))).thenAnswer(inv -> {
            User u = inv.getArgument(0);
            u.setId(42L);
            return u;
        });

        var out = userService.register(req);

        assertThat(out.getId()).isEqualTo(42L);
        assertThat(out.getEmail()).isEqualTo("alice@market.test");
        assertThat(out.getRole()).isEqualTo(Role.CUSTOMER);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword()).isEqualTo("$2a$BCRYPTHASH");
    }

    @Test
    void register_rejectsPublicAdminSignup() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("evil");
        req.setEmail("admin@hack.test");
        req.setPassword("password12");
        req.setRole(Role.ADMIN);

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);

        verify(userRepository, never()).save(any());
    }

    @Test
    void register_conflictWhenUsernameTaken() {
        RegisterRequest req = new RegisterRequest();
        req.setUsername("Bob");
        req.setEmail("bob@test.com");
        req.setPassword("password12");
        req.setRole(Role.TRADER);

        when(userRepository.findByUsername("Bob")).thenReturn(Optional.of(new User()));

        assertThatThrownBy(() -> userService.register(req))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void login_returnsJwtAfterSuccessfulAuthentication() {
        LoginRequest login = new LoginRequest();
        login.setEmail("u@corp.test");
        login.setPassword("plain");

        User user = new User();
        user.setId(8L);
        user.setUsername("Trader");
        user.setEmail("u@corp.test");
        user.setRole(Role.TRADER);

        var authOk = UsernamePasswordAuthenticationToken.authenticated(login.getEmail(), null, java.util.List.of());
        when(authenticationManager.authenticate(any(UsernamePasswordAuthenticationToken.class)))
                .thenReturn(authOk);
        when(userRepository.findByEmailIgnoreCase("u@corp.test")).thenReturn(Optional.of(user));
        when(jwtService.generateAccessToken(8L, "u@corp.test", Role.TRADER)).thenReturn("signed-token");
        when(jwtProperties.getAccessTokenExpirationMs()).thenReturn(3_600_000L);

        var response = userService.login(login);

        assertThat(response.getAccessToken()).isEqualTo("signed-token");
        assertThat(response.getExpiresInMs()).isEqualTo(3_600_000L);
        assertThat(response.getUser().getId()).isEqualTo(8L);
    }

    @Test
    void login_returns401WhenCredentialsInvalid() {
        LoginRequest login = new LoginRequest();
        login.setEmail("gone@corp.test");
        login.setPassword("wrong");

        when(authenticationManager.authenticate(any())).thenThrow(new BadCredentialsException("bad"));

        assertThatThrownBy(() -> userService.login(login))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.UNAUTHORIZED);

        verify(jwtService, never())
                .generateAccessToken(anyLong(), anyString(), org.mockito.ArgumentMatchers.any(Role.class));
    }
}
