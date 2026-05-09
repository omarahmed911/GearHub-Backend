package gearhub.website.gearhub.dto;

import gearhub.website.gearhub.model.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank
    @Size(min = 2, max = 120)
    private String username;

    @NotBlank
    @Email
    private String email;

    @NotBlank
    @Size(min = 8, max = 128)
    private String password;

    @NotNull
    private Role role;
}
