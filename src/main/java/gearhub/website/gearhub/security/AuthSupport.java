package gearhub.website.gearhub.security;

import gearhub.website.gearhub.model.User;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.web.server.ResponseStatusException;

public final class AuthSupport {

    private AuthSupport() {}

    public static User requireUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof SecurityUser(User user))) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED);
        }
        return user;
    }
}
