package gearhub.website.gearhub.controller;

import gearhub.website.gearhub.dto.OrderDto;
import gearhub.website.gearhub.dto.OrderStatusDto;
import gearhub.website.gearhub.model.AppOrder;
import gearhub.website.gearhub.security.AuthSupport;
import gearhub.website.gearhub.service.OrderService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class OrderController {

    private final OrderService orderService;

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @PostMapping
    public AppOrder placeOrder(Authentication authentication, @Valid @RequestBody OrderDto dto) {
        return orderService.placeOrder(dto, AuthSupport.requireUser(authentication));
    }

    @PostMapping("/checkout")
    public AppOrder checkoutFromCart(Authentication authentication) {
        return orderService.checkoutFromCart(AuthSupport.requireUser(authentication));
    }

    @GetMapping
    public List<AppOrder> getOrders(Authentication authentication) {
        return orderService.listOrders(AuthSupport.requireUser(authentication));
    }

    @GetMapping("/{id}")
    public AppOrder getOrderById(Authentication authentication, @PathVariable Long id) {
        return orderService.getOrderById(id, AuthSupport.requireUser(authentication));
    }

    @PutMapping("/{id}/status")
    public AppOrder updateOrderStatus(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody OrderStatusDto dto) {
        return orderService.updateOrderStatus(id, dto.getStatus(), AuthSupport.requireUser(authentication));
    }
}
