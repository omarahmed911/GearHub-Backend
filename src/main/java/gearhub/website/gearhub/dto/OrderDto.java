package gearhub.website.gearhub.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import lombok.Data;

import java.util.List;

@Data
public class OrderDto {
    @NotEmpty
    @Valid
    private List<OrderItemDto> items;
    /**
     * MVP supports cash on delivery only; other values are rejected.
     */
    private String paymentMethod;

    public String getPaymentMethodOrDefault() {
        return paymentMethod != null && !paymentMethod.isBlank() ? paymentMethod.trim() : "COD";
    }
}
