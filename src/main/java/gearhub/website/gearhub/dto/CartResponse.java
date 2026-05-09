package gearhub.website.gearhub.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CartResponse {
    private Long cartId;
    private List<Line> items;

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class Line {
        private Long cartItemId;
        private Long productId;
        private String productName;
        private Integer quantity;
        private Double unitPrice;
    }
}
