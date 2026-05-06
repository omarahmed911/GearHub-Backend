package gearhub.website.gearhub.service;
import gearhub.website.gearhub.dto.ProductDto;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.List;
@Service
public class ProductService {
    private final ProductRepository productRepository;
    public ProductService(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }
    public List<Product> getAllProducts() {
        return productRepository.findAll();
    }
    public List<Product> searchProducts(String keyword) {
        return productRepository.findByNameContainingIgnoreCaseOrCategoryContainingIgnoreCase(keyword, keyword);
    }
    public Product getProductById(Long id) {
        return productRepository.findById(id).orElseThrow(() -> new RuntimeException("Product not found"));
    }
    public Product addProduct(ProductDto dto) {
        Product p = new Product();
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setStockQuantity(dto.getStockQuantity());
        p.setCategory(dto.getCategory());
        return productRepository.save(p);
    }
    public Product updateProduct(Long id, ProductDto dto) {
        Product p = getProductById(id);
        p.setName(dto.getName());
        p.setDescription(dto.getDescription());
        p.setPrice(dto.getPrice());
        p.setStockQuantity(dto.getStockQuantity());
        p.setCategory(dto.getCategory());
        return productRepository.save(p);
    }
    public void deleteProduct(Long id) {
        productRepository.deleteById(id);
    }
}
