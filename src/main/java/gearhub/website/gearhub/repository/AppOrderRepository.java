package gearhub.website.gearhub.repository;

import gearhub.website.gearhub.model.AppOrder;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface AppOrderRepository extends JpaRepository<AppOrder, Long> {
    List<AppOrder> findByCustomer_Id(Long customerId);

    @Query("SELECT DISTINCT o FROM AppOrder o JOIN o.items i WHERE i.product.trader.id = :traderId")
    List<AppOrder> findDistinctByTraderParticipation(@Param("traderId") Long traderId);

    @EntityGraph(attributePaths = {"customer", "items", "items.product", "items.product.trader"})
    @Query("SELECT o FROM AppOrder o WHERE o.id = :id")
    Optional<AppOrder> findFetchedById(@Param("id") Long id);
}
