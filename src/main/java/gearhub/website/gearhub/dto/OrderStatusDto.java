package gearhub.website.gearhub.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class OrderStatusDto {
    @NotBlank(message = "status is required")
    private String status;
}
