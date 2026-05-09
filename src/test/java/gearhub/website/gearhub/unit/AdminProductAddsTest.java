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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminProductAddsTest {

    @Mock
    ProductRepository productRepository;

    @Mock
    UserRepository userRepository;

    @InjectMocks
    ProductService productService;

    @Test
    void addProduct_adminSuppliesTraderId_assignsTrader() {
        User admin = new User();
        admin.setId(1L);
        admin.setRole(Role.ADMIN);

        User trader = new User();
        trader.setId(20L);
        trader.setRole(Role.TRADER);

        ProductDto dto = new ProductDto();
        dto.setName("Starter motor");
        dto.setDescription("12V");
        dto.setPrice(199.99);
        dto.setStockQuantity(3);
        dto.setCategory("Electrical");
        dto.setTraderId(20L);

        when(userRepository.findById(20L)).thenReturn(Optional.of(trader));
        when(productRepository.save(any(Product.class))).thenAnswer(inv -> {
            Product arg = inv.getArgument(0);
            arg.setId(500L);
            return arg;
        });

        Product saved = productService.addProduct(dto, admin);

        assertThat(saved.getTrader().getId()).isEqualTo(20L);
        assertThat(saved.getId()).isEqualTo(500L);
    }
}
