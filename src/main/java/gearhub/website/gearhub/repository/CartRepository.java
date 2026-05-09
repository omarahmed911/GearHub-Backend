package gearhub.website.gearhub.repository;

import gearhub.website.gearhub.model.Cart;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface CartRepository extends JpaRepository<Cart, Long> {
    Optional<Cart> findByCustomer_Id(Long customerId);

    @Query("""
            SELECT DISTINCT c FROM Cart c
            LEFT JOIN FETCH c.items ci
            LEFT JOIN FETCH ci.product
            WHERE c.customer.id = :customerId
            """)
    Optional<Cart> findDetailedByCustomerId(@Param("customerId") Long customerId);
}
