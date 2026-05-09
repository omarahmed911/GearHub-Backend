package gearhub.website.gearhub.unit;

import gearhub.website.gearhub.dto.OrderDto;
import gearhub.website.gearhub.dto.OrderItemDto;
import gearhub.website.gearhub.model.AppOrder;
import gearhub.website.gearhub.model.Cart;
import gearhub.website.gearhub.model.CartItem;
import gearhub.website.gearhub.model.OrderItem;
import gearhub.website.gearhub.model.OrderStatus;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.AppOrderRepository;
import gearhub.website.gearhub.repository.CartRepository;
import gearhub.website.gearhub.repository.ProductRepository;
import gearhub.website.gearhub.repository.UserRepository;
import gearhub.website.gearhub.service.OrderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class OrderServiceTest {

    @Mock
    AppOrderRepository orderRepository;

    @Mock
    ProductRepository productRepository;

    @Mock
    CartRepository cartRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    OrderService orderService;

    @BeforeEach
    void stubCustomerRef() {
        when(userRepository.getReferenceById(anyLong())).thenAnswer(inv -> {
            User stub = new User();
            stub.setId(inv.getArgument(0));
            return stub;
        });
    }

    @Test
    void placeOrder_decrementsStockAndPersistsTotals() {
        AtomicReference<AppOrder> savedOrderHolder = new AtomicReference<>();

        Product p = product(3L, "Brake rotor", 40.0, 10, trader(7L));
        when(productRepository.findById(3L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        when(orderRepository.save(any(AppOrder.class))).thenAnswer(inv -> {
            AppOrder o = inv.getArgument(0);
            o.setId(100L);
            savedOrderHolder.set(o);
            return o;
        });
        when(orderRepository.findFetchedById(eq(100L))).thenAnswer(inv ->
                Optional.ofNullable(savedOrderHolder.get()));

        OrderDto dto = new OrderDto();
        OrderItemDto line = new OrderItemDto();
        line.setProductId(3L);
        line.setQuantity(2);
        dto.setItems(List.of(line));

        AppOrder result = orderService.placeOrder(dto, customer(44L));

        assertThat(result.getId()).isEqualTo(100L);
        assertThat(result.getTotalAmount()).isEqualTo(80.0);

        ArgumentCaptor<Product> prodCap = ArgumentCaptor.forClass(Product.class);
        verify(productRepository).save(prodCap.capture());
        assertThat(prodCap.getValue().getStockQuantity()).isEqualTo(8);

        ArgumentCaptor<AppOrder> orderCap = ArgumentCaptor.forClass(AppOrder.class);
        verify(orderRepository).save(orderCap.capture());
        assertThat(orderCap.getValue().getItems()).hasSize(1);
        assertThat(orderCap.getValue().getItems().get(0).getQuantity()).isEqualTo(2);
    }

    @Test
    void placeOrder_badRequestWhenNotCod() {
        OrderDto dto = new OrderDto();
        dto.setItems(List.of(item(1L, 1)));
        dto.setPaymentMethod("CARD");

        assertThatThrownBy(() -> orderService.placeOrder(dto, customer(1L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void placeOrder_badRequestWhenInsufficientStock() {
        Product p = product(1L, "Bolt", 1.0, 2, trader(55L));
        when(productRepository.findById(1L)).thenReturn(Optional.of(p));

        OrderDto dto = new OrderDto();
        dto.setItems(List.of(item(1L, 5)));

        assertThatThrownBy(() -> orderService.placeOrder(dto, customer(1L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getReason())
                .asString()
                .contains("Bolt");
    }

    @Test
    void getOrderById_customerCanViewTheirOrderOnly() {
        User owner = customer(9L);
        User stranger = customer(999L);
        Product product = productForTrader(77L);
        OrderItem line = orderLine(product);

        AppOrder mine = completedOrder(owner, List.of(line), 500L);
        when(orderRepository.findFetchedById(500L)).thenReturn(Optional.of(mine));

        assertThat(orderService.getOrderById(500L, owner)).isSameAs(mine);

        assertThatThrownBy(() -> orderService.getOrderById(500L, stranger))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateOrderStatus_traderMustParticipate() {
        User participating = trader(10L);
        User outsider = trader(11L);

        Product product = productForTrader(participating.getId());
        OrderItem line = orderLine(product);
        AppOrder order = completedOrder(customer(55L), new ArrayList<>(List.of(line)), 60L);

        when(orderRepository.findFetchedById(60L)).thenReturn(Optional.of(order));
        when(orderRepository.save(order)).thenAnswer(inv -> inv.getArgument(0));

        AppOrder updated = orderService.updateOrderStatus(60L, "PROCESSING", participating);
        assertThat(updated.getStatus()).isEqualTo(OrderStatus.PROCESSING.name());

        assertThatThrownBy(() -> orderService.updateOrderStatus(60L, "DELIVERED", outsider))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateOrderStatus_invalidStatus_returns400() {
        AppOrder order = completedOrder(customer(1L), Collections.emptyList(), 1L);
        when(orderRepository.findFetchedById(1L)).thenReturn(Optional.of(order));

        assertThatThrownBy(() -> orderService.updateOrderStatus(1L, "SHIPPED", trader(44L)))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void listOrders_routesQueriesByActorRole() {
        orderService.listOrders(admin(1L));
        verify(orderRepository).findAll();

        orderService.listOrders(customer(2L));
        verify(orderRepository).findByCustomer_Id(2L);

        orderService.listOrders(trader(3L));
        verify(orderRepository).findDistinctByTraderParticipation(3L);
    }

    @Test
    void checkoutFromCart_emptiesCartAfterPersistingOrder() {
        User buyer = customer(30L);

        Product p = product(9L, "Filter", 5.0, 4, trader(77L));

        CartItem ci = new CartItem();
        ci.setProduct(p);
        ci.setQuantity(2);

        Cart cart = new Cart();
        cart.setId(444L);
        cart.setCustomer(buyer);
        List<CartItem> lines = new ArrayList<>();
        lines.add(ci);
        ci.setCart(cart);
        cart.setItems(lines);

        when(cartRepository.findDetailedByCustomerId(30L)).thenReturn(Optional.of(cart));

        AtomicReference<AppOrder> savedOrderHolder = new AtomicReference<>();

        when(productRepository.findById(9L)).thenReturn(Optional.of(p));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> inv.getArgument(0));

        when(orderRepository.save(any(AppOrder.class))).thenAnswer(inv -> {
            AppOrder o = inv.getArgument(0);
            o.setId(900L);
            savedOrderHolder.set(o);
            return o;
        });
        when(orderRepository.findFetchedById(eq(900L))).thenAnswer(inv ->
                Optional.ofNullable(savedOrderHolder.get()));

        AppOrder placed = orderService.checkoutFromCart(buyer);

        assertThat(placed.getTotalAmount()).isEqualTo(10.0);
        assertThat(cart.getItems()).isEmpty();
        verify(cartRepository).save(cart);
    }

    private static OrderItemDto item(long productId, int qty) {
        OrderItemDto d = new OrderItemDto();
        d.setProductId(productId);
        d.setQuantity(qty);
        return d;
    }

    private static User customer(long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.CUSTOMER);
        u.setEmail("c%d@test.com".formatted(id));
        return u;
    }

    private static User trader(long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.TRADER);
        u.setEmail("t%d@test.com".formatted(id));
        return u;
    }

    private static User admin(long id) {
        User u = new User();
        u.setId(id);
        u.setRole(Role.ADMIN);
        u.setEmail("a%d@test.com".formatted(id));
        return u;
    }

    private static Product product(long id, String name, double price, int stock, User trader) {
        Product p = new Product();
        p.setId(id);
        p.setName(name);
        p.setPrice(price);
        p.setStockQuantity(stock);
        p.setTrader(trader);
        return p;
    }

    private static Product productForTrader(long traderId) {
        return product(777L, "Part X", 3.0, 10, trader(traderId));
    }

    private static OrderItem orderLine(Product product) {
        OrderItem line = new OrderItem();
        line.setProduct(product);
        line.setQuantity(1);
        return line;
    }

    /**
     * Pre-populated persisted-style graph for authorization checks.
     */
    private static AppOrder completedOrder(User customerUser, List<OrderItem> lines, Long idVal) {
        AppOrder o = new AppOrder();
        if (idVal != null) {
            o.setId(idVal);
        }
        o.setCustomer(customerUser);
        o.setItems(lines);
        o.setPaymentMethod("COD");
        if (lines != null && !lines.isEmpty()) {
            lines.forEach(li -> li.setOrder(o));
        }
        return o;
    }
}
