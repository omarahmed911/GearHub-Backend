package gearhub.website.gearhub.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.PositiveOrZero;
import lombok.Data;

@Data
public class ProductDto {
    @NotBlank
    private String name;

    private String description;

    @NotNull
    @DecimalMin(value = "0.01", message = "price must be positive")
    private Double price;

    @NotNull
    @PositiveOrZero
    private Integer stockQuantity;

    private String category;

    /**
     * Used when an admin creates or reassigns a listing to a trader account.
     */
    private Long traderId;
}
