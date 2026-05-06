package gearhub.website.gearhub.dto;
import lombok.Data;
import java.util.List;
@Data
public class OrderDto {
    private List<OrderItemDto> items;
}
