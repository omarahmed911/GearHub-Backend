package gearhub.website.gearhub.service;
import gearhub.website.gearhub.dto.OrderDto;
import gearhub.website.gearhub.dto.OrderItemDto;
import gearhub.website.gearhub.model.AppOrder;
import gearhub.website.gearhub.model.OrderItem;
import gearhub.website.gearhub.model.Product;
import gearhub.website.gearhub.repository.AppOrderRepository;
import gearhub.website.gearhub.repository.ProductRepository;
import org.springframework.stereotype.Service;
import java.util.ArrayList;
import java.util.List;
@Service
public class OrderService {
    private final AppOrderRepository orderRepository;
    private final ProductRepository productRepository;
    public OrderService(AppOrderRepository orderRepository, ProductRepository productRepository) {
        this.orderRepository = orderRepository;
        this.productRepository = productRepository;
    }
    public List<AppOrder> getOrderHistory() {
        return orderRepository.findAll();
    }
    public AppOrder getOrderById(Long id) {
        return orderRepository.findById(id).orElseThrow(() -> new RuntimeException("Order not found"));
    }
    public AppOrder placeOrder(OrderDto dto) {
        AppOrder order = new AppOrder();
        order.setStatus("PENDING");
        List<OrderItem> items = new ArrayList<>();
        double total = 0.0;
        for (OrderItemDto itemDto : dto.getItems()) {
            Product p = productRepository.findById(itemDto.getProductId()).orElseThrow();
            if (p.getStockQuantity() < itemDto.getQuantity()) {
                throw new RuntimeException("Not enough stock for product " + p.getName());
            }
            p.setStockQuantity(p.getStockQuantity() - itemDto.getQuantity());
            productRepository.save(p);
            
            OrderItem item = new OrderItem();
            item.setOrder(order);
            item.setProduct(p);
            item.setQuantity(itemDto.getQuantity());
            item.setPrice(p.getPrice());
            items.add(item);
            total += (p.getPrice() * itemDto.getQuantity());
        }
        order.setItems(items);
        order.setTotalAmount(total);
        return orderRepository.save(order);
    }
    public AppOrder updateOrderStatus(Long id, String status) {
        AppOrder order = getOrderById(id);
        order.setStatus(status);
        return orderRepository.save(order);
    }
}
