package gearhub.website.gearhub.controller;
import gearhub.website.gearhub.dto.OrderDto;
import gearhub.website.gearhub.dto.OrderStatusDto;
import gearhub.website.gearhub.model.AppOrder;
import gearhub.website.gearhub.service.OrderService;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import java.util.List;
@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }
    @PostMapping
    public AppOrder placeOrder(@RequestBody OrderDto dto) {
        return orderService.placeOrder(dto);
    }
    @GetMapping
    public List<AppOrder> getOrderHistory() {
        return orderService.getOrderHistory();
    }
    @GetMapping("/{id}")
    public AppOrder getOrderById(@PathVariable Long id) {
        return orderService.getOrderById(id);
    }
    @PutMapping("/{id}/status")
    public AppOrder updateOrderStatus(@PathVariable Long id, @RequestBody OrderStatusDto dto) {
        return orderService.updateOrderStatus(id, dto.getStatus());
    }
}
