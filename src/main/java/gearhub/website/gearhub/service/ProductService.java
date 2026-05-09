package gearhub.website.gearhub.service;

import gearhub.website.gearhub.dto.ProductDto;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.model.Role;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.ProductRepository;
import gearhub.website.gearhub.repository.UserRepository;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

@Service
public class ProductService {

    private final ProductRepository productRepository;
    private final UserRepository userRepository;

    public ProductService(ProductRepository productRepository, UserRepository userRepository) {
        this.productRepository = productRepository;
        this.userRepository = userRepository;
    }

    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }

    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword);
    }

    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(this::productNotFound);
    }

    public List<Product> getProductsForTrader(User actor) {
        requireTraderOrAdmin(actor);
        if (actor.getRole() == Role.ADMIN) {
            return productRepository.findAll();
        }
        return productRepository.findByTrader_Id(actor.getId());
    }

    @Transactional
    public Product addProduct(ProductDto dto, User actor) {
        requireTraderOrAdmin(actor);
        if (actor.getRole() == Role.TRADER
                && dto.getTraderId() != null && !dto.getTraderId().equals(actor.getId())) {
            throw forbidden("Traders cannot create listings for another account.");
        }
        User trader = resolveTrader(actor, dto.getTraderId());
        Product p = new Product();
        copyDto(dto, p);
        p.setTrader(trader);
        return productRepository.save(p);
    }

    @Transactional
    public Product updateProduct(Long id, ProductDto dto, User actor) {
        Product p = getProductById(id);
        assertCanModify(actor, p);
        copyDto(dto, p);
        if (actor.getRole() == Role.ADMIN && dto.getTraderId() != null) {
            p.setTrader(loadUser(dto.getTraderId()));
        }
        return productRepository.save(p);
    }

    @Transactional
    public void deleteProduct(Long id, User actor) {
        Product p = getProductById(id);
        assertCanModify(actor, p);
        productRepository.delete(p);
    }

    private User resolveTrader(User actor, Long traderIdRequest) {
        if (actor.getRole() == Role.ADMIN) {
            if (traderIdRequest == null) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "traderId is required for admin.");
            }
            return loadUser(traderIdRequest);
        }
        return actor;
    }

    private User loadUser(Long id) {
        return userRepository.findById(id)
                .filter(u -> u.getRole() == Role.TRADER)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.BAD_REQUEST, "Trader not found."));
    }

    private ResponseStatusException productNotFound() {
        return new ResponseStatusException(HttpStatus.NOT_FOUND, "Product not found.");
    }

    private void requireTraderOrAdmin(User actor) {
        if (actor.getRole() != Role.TRADER && actor.getRole() != Role.ADMIN) {
            throw forbidden("Only traders or administrators can manage product listings.");
        }
    }

    private void assertCanModify(User actor, Product p) {
        if (actor.getRole() == Role.ADMIN) {
            return;
        }
        if (actor.getRole() == Role.TRADER && p.getTrader() != null
                && p.getTrader().getId().equals(actor.getId())) {
            return;
        }
        throw forbidden("You cannot modify this product.");
    }

    private ResponseStatusException forbidden(String msg) {
        return new ResponseStatusException(HttpStatus.FORBIDDEN, msg);
    }

    private void copyDto(ProductDto dto, Product p) {
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setStockQuantity(dto.getStockQuantity());
        p.setCategory(dto.getCategory());
    }
}
