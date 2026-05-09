package gearhub.website.gearhub.controller;

import gearhub.website.gearhub.dto.CartItemRequest;
import gearhub.website.gearhub.dto.CartResponse;
import gearhub.website.gearhub.security.AuthSupport;
import gearhub.website.gearhub.service.CartService;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public CartResponse getCart(Authentication authentication) {
        return cartService.view(AuthSupport.requireUser(authentication));
    }

    @PostMapping("/items")
    public CartResponse addItem(
            Authentication authentication, @Valid @RequestBody CartItemRequest request) {
        return cartService.addOrUpdate(AuthSupport.requireUser(authentication), request);
    }

    @PutMapping("/items/{productId}")
    public CartResponse updateItem(
            Authentication authentication,
            @PathVariable Long productId,
            @Valid @RequestBody CartQuantityBody body) {
        return cartService.updateLine(AuthSupport.requireUser(authentication), productId, body.quantity());
    }

    @DeleteMapping("/items/{productId}")
    public CartResponse removeItem(Authentication authentication, @PathVariable Long productId) {
        return cartService.removeLine(AuthSupport.requireUser(authentication), productId);
    }

    public record CartQuantityBody(@NotNull @Min(1) Integer quantity) {}
}
