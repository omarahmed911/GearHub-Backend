package gearhub.website.gearhub.repository;
import gearhub.website.gearhub.model.*;
import org.springframework.data.jpa.repository.JpaRepository;
public interface AppOrderRepository extends JpaRepository<AppOrder, Long> {
}
