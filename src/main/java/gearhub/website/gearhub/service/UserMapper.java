package gearhub.website.gearhub.service;

import gearhub.website.gearhub.dto.UserResponse;
import gearhub.website.gearhub.model.User;

public final class UserMapper {
    private UserMapper() {}

    public static UserResponse toResponse(User user) {
        if (user == null) {
            return null;
        }
        return new UserResponse(user.getId(), user.getUsername(), user.getEmail(), user.getRole());
    }
}
