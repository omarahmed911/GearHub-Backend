package gearhub.website.gearhub.service;

import gearhub.website.gearhub.dto.OrderDto;
import gearhub.website.gearhub.dto.OrderItemDto;
import gearhub.website.gearhub.model.AppOrder;
import gearhub.website.gearhub.model.OrderItem;
import gearhub.website.gearhub.model.OrderStatus;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.AppOrderRepository;
import gearhub.website.gearhub.repository.CartRepository;
import gearhub.website.gearhub.repository.ProductRepository;
import gearhub.website.gearhub.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

@Service
public class OrderService {

    private static final Logger log = LoggerFactory.getLogger(OrderService.class);

    private final AppOrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final CartRepository cartRepository;
    private final UserRepository userRepository;

    public OrderService(
            AppOrderRepository orderRepository,
            ProductRepository productRepository,
            CartRepository cartRepository,
            UserRepository userRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
        this.cartRepository = cartRepository;
        this.userRepository = userRepository;
    }

    public List<AppOrder> listOrders(User actor) {
        return switch (actor.getRole()) {
            case ADMIN -> orderRepository.findAll();
            case CUSTOMER -> orderRepository.findByCustomer_Id(actor.getId());
            case TRADER -> orderRepository.findDistinctByTraderParticipation(actor.getId());
        };
    }

    public AppOrder getOrderById(Long id, User actor) {
        AppOrder order = orderRepository.findFetchedById(id).orElseThrow(this::orderNotFound);
        if (!canView(actor, order)) {
            throw forbidden("You cannot view this order.");
        }
        return order;
    }

    @Transactional
    public AppOrder placeOrder(OrderDto dto, User customer) {
        String pay = dto.getPaymentMethodOrDefault();
        if (!"COD".equalsIgnoreCase(pay)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only COD (cash on delivery) is supported.");
        }
        return persistOrder(customer, dto.getItems(), "COD");
    }

    /**
     * Cash-on-delivery checkout from persisted cart contents.
     */
    @Transactional
    public AppOrder checkoutFromCart(User customer) {
        var cart = cartRepository.findDetailedByCustomerId(customer.getId())
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart is empty."));
        if (cart.getItems() == null || cart.getItems().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Cart has no items.");
        }
        List<OrderItemDto> lines = cart.getItems().stream()
                .map(ci -> {
                    OrderItemDto d = new OrderItemDto();
                    d.setProductId(ci.getProduct().getId());
                    d.setQuantity(ci.getQuantity());
                    return d;
                })
                .toList();
        AppOrder saved = persistOrder(customer, lines, "COD");
        cart.getItems().clear();
        cartRepository.save(cart);
        return saved;
    }

    private AppOrder persistOrder(User customer, List<OrderItemDto> itemDtos, String paymentMethod) {
        AppOrder order = new AppOrder();
        order.setCustomer(userRepository.getReferenceById(customer.getId()));
        order.setStatus(OrderStatus.PENDING.name());
        order.setPaymentMethod(paymentMethod);
        List<OrderItem> items = new ArrayList<>();
        double total = 0.0;
        for (OrderItemDto itemDto : itemDtos) {
            Product p = productRepository.findById(itemDto.getProductId()).orElseThrow(() ->
                    new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown product in order."));
            if (p.getStockQuantity() == null || p.getStockQuantity() < itemDto.getQuantity()) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                        "Not enough stock for product '%s'.".formatted(p.getName()));
            }
            p.setStockQuantity(p.getStockQuantity() - itemDto.getQuantity());
            productRepository.save(p);

            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(p);
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(p.getPrice());
            items.add(item);
            total += p.getPrice() * itemDto.getQuantity();
        }
        order.setItems(items);
        order.setTotalAmount(total);
        AppOrder saved = orderRepository.save(order);
        return orderRepository.findFetchedById(saved.getId()).orElse(saved);
    }

    @Transactional
    public AppOrder updateOrderStatus(Long id, String statusRaw, User actor) {
        AppOrder order = orderRepository.findFetchedById(id).orElseThrow(this::orderNotFound);
        OrderStatus status;
        try {
            status = OrderStatus.parse(statusRaw);
        } catch (IllegalArgumentException e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Unknown order status.");
        }
        if (actor.getRole() != Role.ADMIN && !participatingTrader(actor, order)) {
            throw forbidden("You cannot update status for this order.");
        }
        String previous = order.getStatus();
        order.setStatus(status.name());
        AppOrder saved = orderRepository.save(order);
        log.info(
                "Order {} status {} -> {} (actor userId={}, role={})",
                saved.getId(),
                previous,
                saved.getStatus(),
                actor.getId(),
                actor.getRole());
        return saved;
    }

    private boolean canView(User actor, AppOrder order) {
        if (actor.getRole() == Role.ADMIN) {
            return true;
        }
        if (actor.getRole() == Role.CUSTOMER
                && order.getCustomer() != null
                && order.getCustomer().getId().equals(actor.getId())) {
            return true;
        }
        return actor.getRole() == Role.TRADER && participatingTrader(actor, order);
    }

    private boolean participatingTrader(User trader, AppOrder order) {
        if (order.getItems() == null) {
            return false;
        }
        return order.getItems().stream()
                .anyMatch(i -> i.getProduct() != null
                        && i.getProduct().getTrader() != null
                        && i.getProduct().getTrader().getId().equals(trader.getId()));
    }

    private ResponseStatusException orderNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found.");
    }

    private ResponseStatusException forbidden(String msg) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
    }
}
