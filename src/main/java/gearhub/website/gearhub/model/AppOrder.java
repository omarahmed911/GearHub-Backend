package gearhub.website.gearhub.model;
import jakarta.persistence.*;
import lombok.Data;
import java.util.List;
@Entity
@Data
@Table(name = "orders")
public class AppOrder {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne
    private User customer;
    private String status; // PENDING, PROCESSING, DELIVERED
    
    @Column(name = "total_price")
    private Double totalAmount;
    
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL)
    private List<OrderItem> items;
}
