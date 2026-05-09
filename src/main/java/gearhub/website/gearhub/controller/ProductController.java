package gearhub.website.gearhub.controller;

import gearhub.website.gearhub.dto.ProductDto;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.security.AuthSupport;
import gearhub.website.gearhub.service.ProductService;
import jakarta.validation.Valid;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/products")
public class ProductController {

    private final ProductService productService;

    public ProductController(ProductService productService) {
        this.productService = productService;
    }

    @GetMapping
    public List<Product> getAllProducts(@RequestParam(required = false) String search) {
        if (search != null && !search.trim().isEmpty()) {
            return productService.searchProducts(search.trim());
        }
        return productService.getAllProducts();
    }

    @GetMapping("/mine")
    public List<Product> getMyProducts(Authentication authentication) {
        return productService.getProductsForTrader(AuthSupport.requireUser(authentication));
    }

    @GetMapping("/{id}")
    public Product getProductById(@PathVariable Long id) {
        return productService.getProductById(id);
    }

    @PostMapping
    public Product addProduct(Authentication authentication, @Valid @RequestBody ProductDto dto) {
        return productService.addProduct(dto, AuthSupport.requireUser(authentication));
    }

    @PutMapping("/{id}")
    public Product updateProduct(
            Authentication authentication,
            @PathVariable Long id,
            @Valid @RequestBody ProductDto dto) {
        return productService.updateProduct(id, dto, AuthSupport.requireUser(authentication));
    }

    @DeleteMapping("/{id}")
    public void deleteProduct(Authentication authentication, @PathVariable Long id) {
        productService.deleteProduct(id, AuthSupport.requireUser(authentication));
    }
}
