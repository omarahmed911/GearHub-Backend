package gearhub.website.gearhub.model;
import jakarta.persistence.*;
import lombok.Data;
@Entity
@Data
@Table(name = "users")
public class User {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(name = "name", nullable = false)
    private String username;
    
    @Column(name = "password_hash", nullable = false)
    private String password;
    
    @Enumerated(EnumType.STRING)
    private Role role;
}
