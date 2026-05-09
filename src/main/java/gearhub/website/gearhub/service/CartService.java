package gearhub.website.gearhub.service;

import gearhub.website.gearhub.dto.CartItemRequest;
import gearhub.website.gearhub.dto.CartResponse;
import gearhub.website.gearhub.model.Cart;
import gearhub.website.gearhub.model.CartItem;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.CartRepository;
import gearhub.website.gearhub.repository.ProductRepository;
import gearhub.website.gearhub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class CartService {

    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public CartService(
            CartRepository cartRepository,
            ProductRepository productRepository,
            UserRepository userRepository) {
        this.cartRepository = cartRepository;
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public CartResponse view(User customer) {
        return cartRepository.findDetailedByCustomerId(customer.getId())
                .map(this::toResponse)
                .orElse(new CartResponse(null, List.of()));
    }

    @Transactional
    public CartResponse addOrUpdate(User customer, CartItemRequest request) {
        Cart cart = loadOrCreateCart(customer.getId());
        Product product = productRepository.findById(request.getProductId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found."));
        CartItem existing = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(product.getId()))
                .findFirst()
                .orElse(null);
        if (existing != null) {
            existing.setQuantity(existing.getQuantity() + request.getQuantity());
        } else {
            CartItem line = new CartItem();
            line.setCart(cart);
            line.setProduct(product);
            line.setQuantity(request.getQuantity());
            cart.getItems().add(line);
        }
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse updateLine(User customer, Long productId, int quantity) {
        if (quantity < 1) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Quantity must be at least 1.");
        }
        Cart cart = cartRepository.findDetailedByCustomerId(customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found."));
        CartItem line = cart.getItems().stream()
                .filter(ci -> ci.getProduct().getId().equals(productId))
                .findFirst()
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not in cart."));
        line.setQuantity(quantity);
        return toResponse(cartRepository.save(cart));
    }

    @Transactional
    public CartResponse removeLine(User customer, Long productId) {
        Cart cart = cartRepository.findDetailedByCustomerId(customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Cart not found."));
        boolean removed = cart.getItems().removeIf(ci -> ci.getProduct().getId().equals(productId));
        if (!removed) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "Item not in cart.");
        }
        return toResponse(cartRepository.save(cart));
    }

    private Cart loadOrCreateCart(Long customerId) {
        return cartRepository.findDetailedByCustomerId(customerId).orElseGet(() -> {
            Cart c = new Cart();
            c.setCustomer(userRepository.getReferenceById(customerId));
            c.setItems(new ArrayList<>());
            return cartRepository.save(c);
        });
    }

    private CartResponse toResponse(Cart cart) {
        List<CartResponse.Line> lines = cart.getItems() == null
                ? List.of()
                : cart.getItems().stream()
                        .map(ci -> new CartResponse.Line(
                                ci.getId(),
                                ci.getProduct().getId(),
                                ci.getProduct().getName(),
                                ci.getQuantity(),
                                ci.getProduct().getPrice()))
                        .toList();
        return new CartResponse(cart.getId(), lines);
    }
}
