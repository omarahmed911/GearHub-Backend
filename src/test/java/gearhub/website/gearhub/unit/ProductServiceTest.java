package gearhub.website.gearhub.unit;

import gearhub.website.gearhub.dto.ProductDto;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.ProductRepository;
import gearhub.website.gearhub.repository.UserRepository;
import gearhub.website.gearhub.service.ProductService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductServiceTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProductService productService;

    @Test
    void addProduct_traderAssignsSelfAsOwner() {
        User trader = user(5L, Role.TRADER);
        ProductDto dto = new ProductDto();
        dto.setName("Oil filter");
        dto.setDescription("OEM");
        dto.setPrice(12.5);
        dto.setStockQuantity(4);
        dto.setCategory("Filters");

        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product p = inv.getArgument(0);
            p.setId(100L);
            return p;
        });

        Product saved = productService.addProduct(dto, trader);

        assertThat(saved.getId()).isEqualTo(100L);
        assertThat(saved.getTrader().getId()).isEqualTo(5L);
        assertThat(saved.getName()).isEqualTo("Oil filter");
    }

    @Test
    void addProduct_adminMustProvideTraderId() {
        User admin = user(1L, Role.ADMIN);
        ProductDto dto = new ProductDto();
        dto.setName("Part");
        dto.setDescription("d");
        dto.setPrice(1.0);
        dto.setStockQuantity(1);
        dto.setCategory("c");
        dto.setTraderId(null);

        assertThatThrownBy(() -> productService.addProduct(dto, admin))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void addProduct_traderCannotImpersonateAnotherTrader() {
        User trader = user(5L, Role.TRADER);
        ProductDto dto = new ProductDto();
        dto.setName("Part");
        dto.setDescription("d");
        dto.setPrice(1.0);
        dto.setStockQuantity(1);
        dto.setCategory("c");
        dto.setTraderId(99L);

        assertThatThrownBy(() -> productService.addProduct(dto, trader))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void deleteProduct_traderCanDeleteOwnListing() {
        User trader = user(5L, Role.TRADER);
        Product owned = productOwnedBy(88L, 5L);

        when(productRepository.findById(88L)).thenReturn(Optional.of(owned));

        productService.deleteProduct(88L, trader);

        verify(productRepository).delete(owned);
    }

    @Test
    void deleteProduct_traderForbiddenForSomeoneElsesListing() {
        User trader = user(5L, Role.TRADER);
        Product alien = productOwnedBy(88L, 999L);

        when(productRepository.findById(88L)).thenReturn(Optional.of(alien));

        assertThatThrownBy(() -> productService.deleteProduct(88L, trader))
                .isInstanceOf(ResponseStatusException.class)
                .extracting(ex -> ((ResponseStatusException) ex).getStatusCode())
                .isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void updateProduct_adminMayReassignTrader() {
        User admin = user(1L, Role.ADMIN);
        Product p = productOwnedBy(10L, 5L);
        User newTrader = user(20L, Role.TRADER);

        ProductDto dto = new ProductDto();
        dto.setName("Renamed");
        dto.setDescription("x");
        dto.setPrice(2.0);
        dto.setStockQuantity(3);
        dto.setCategory("cat");
        dto.setTraderId(20L);

        when(productRepository.findById(10L)).thenReturn(Optional.of(p));
        when(userRepository.findById(20L)).thenReturn(Optional.of(newTrader));
        ArgumentCaptor<Product> cap = ArgumentCaptor.forClass(Product.class);
        when(productRepository.save(cap.capture())).thenAnswer(inv -> inv.getArgument(0));

        productService.updateProduct(10L, dto, admin);

        assertThat(cap.getValue().getTrader().getId()).isEqualTo(20L);
        assertThat(cap.getValue().getName()).isEqualTo("Renamed");
    }

    private static User user(long id, Role role) {
        User u = new User();
        u.setId(id);
        u.setUsername("user" + id);
        u.setEmail("user" + id + "@t.test");
        u.setRole(role);
        return u;
    }

    private static Product productOwnedBy(long productId, long traderId) {
        Product p = new Product();
        p.setId(productId);
        p.setName("Spark plug");
        p.setPrice(5.0);
        p.setStockQuantity(10);
        p.setTrader(user(traderId, Role.TRADER));
        return p;
    }
}
