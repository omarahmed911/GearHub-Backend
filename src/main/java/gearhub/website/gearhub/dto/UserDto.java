package gearhub.website.gearhub.dto;
import gearhub.website.gearhub.model.Role;
import lombok.Data;
@Data
public class UserDto {
    private String username;
    private String password;
    private Role role;
}
