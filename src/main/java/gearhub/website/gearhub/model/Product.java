package gearhub.website.gearhub.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Data
@Table(name = "products")
public class Product {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    private String name;
    private String description;
    private Double price;
    private Integer stockQuantity;
    private String category;
    @ManyToOne
    private User trader;
}
