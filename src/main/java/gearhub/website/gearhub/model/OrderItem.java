package gearhub.website.gearhub.model;
import jakarta.persistence.*;
import lombok.Data;
import com.fasterxml.jackson.annotation.JsonIgnore;
@Entity
@Data
public class OrderItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @JsonIgnore
    @ManyToOne
    private AppOrder order;
    @ManyToOne
    private Product product;
    private Integer quantity;
    private Double price;
}
