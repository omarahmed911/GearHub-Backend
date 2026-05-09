package gearhub.website.gearhub.controller;

import gearhub.website.gearhub.dto.UserResponse;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.model.User;
import gearhub.website.gearhub.repository.UserRepository;
import gearhub.website.gearhub.security.AuthSupport;
import gearhub.website.gearhub.service.ProductService;
import gearhub.website.gearhub.service.UserMapper;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final UserRepository userRepository;
    private final ProductService productService;

    public AdminController(UserRepository userRepository, ProductService productService) {
        this.userRepository = userRepository;
        this.productService = productService;
    }

    @GetMapping("/users")
    public List<UserResponse> listUsers(Authentication authentication) {
        AuthSupport.requireUser(authentication);
        return userRepository.findAll().stream().map(UserMapper::toResponse).toList();
    }

    @GetMapping("/products")
    public List<Product> listAllProducts(Authentication authentication) {
        User admin = AuthSupport.requireUser(authentication);
        return productService.getProductsForTrader(admin);
    }

    @DeleteMapping("/products/{id}")
    public void removeProduct(Authentication authentication, @PathVariable Long id) {
        productService.deleteProduct(id, AuthSupport.requireUser(authentication));
    }
}
