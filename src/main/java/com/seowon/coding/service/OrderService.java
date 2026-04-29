package com.seowon.coding.service;

import com.seowon.coding.domain.model.Order;
import com.seowon.coding.domain.model.OrderItem;
import com.seowon.coding.domain.model.ProcessingStatus;
import com.seowon.coding.domain.model.Product;
import com.seowon.coding.domain.repository.OrderRepository;
import com.seowon.coding.domain.repository.ProcessingStatusRepository;
import com.seowon.coding.domain.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
public class OrderService {
    
    private final OrderRepository orderRepository;
    private final ProductRepository productRepository;
    private final ProcessingStatusRepository processingStatusRepository;

    @Transactional(readOnly = true)
    public List<Order> getAllOrders() {
        return orderRepository.findAll();
    }
    
    @Transactional(readOnly = true)
    public Optional<Order> getOrderById(Long id) {
        return orderRepository.findById(id);
    }
    

    public Order updateOrder(Long id, Order order) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        order.setId(id);
        return orderRepository.save(order);
    }
    
    public void deleteOrder(Long id) {
        if (!orderRepository.existsById(id)) {
            throw new RuntimeException("Order not found with id: " + id);
        }
        orderRepository.deleteById(id);
    }


    /**
     * TODO #3: 구현 항목
     * 주어진 고객 정보로 새 Order를 생성
     * 지정된 Product를 주문에 추가
     * order 의 상태를 PENDING 으로 변경
     * orderDate 를 현재시간으로 설정
     * order 를 저장 (cascade 로 OrderItem 일괄 저장)
     * 각 Product 의 재고를 수정 (변경 감지로 자동 반영)
     * placeOrder 메소드의 시그니처는 변경하지 않은 채 구현하세요.
     */
    public Order placeOrder(String customerName, String customerEmail, List<Long> productIds, List<Integer> quantities) {
		Order order = Order.create(customerName, customerEmail);

		for (int i = 0; i < productIds.size(); i++) {
			long id = productIds.get(i);
			Product product = productRepository.findById(id).orElseThrow(() -> new IllegalArgumentException("Product not found: " + id));
			int quantity = quantities.get(i);

			OrderItem orderItem = OrderItem.create(order, product, quantity);

			product.decreaseStock(quantity);
			productRepository.save(product);

			order.addItem(orderItem);
		}

		return orderRepository.save(order);
    }

    /**
     * TODO #4 (리팩토링): Service 에 몰린 도메인 로직을 도메인 객체 안으로 이동
     * - Repository 또는 Mapper 조회는 도메인 객체 밖에서 해결하여 의존을 차단 합니다.
     * - #3 에서 추가한 도메인 메소드가 있을 경우 재사용해도 됩니다.
     */
    public Order checkoutOrder(String customerName,
                               String customerEmail,
                               List<OrderProduct> orderProducts,
                               String couponCode) {
        if (customerName == null || customerEmail == null) {
            throw new IllegalArgumentException("customer info required");
        }
        if (orderProducts == null || orderProducts.isEmpty()) {
            throw new IllegalArgumentException("orderReqs invalid");
        }

        Order order = Order.create(customerName, customerEmail);

        BigDecimal subtotal = BigDecimal.ZERO;
        for (OrderProduct req : orderProducts) {
            Long pid = req.getProductId();
            int qty = req.getQuantity();

            Product product = productRepository.findById(pid)
                    .orElseThrow(() -> new IllegalArgumentException("Product not found: " + pid));
            if (qty <= 0) {
                throw new IllegalArgumentException("quantity must be positive: " + qty);
            }
            if (product.getStockQuantity() < qty) {
                throw new IllegalStateException("insufficient stock for product " + pid);
            }

            OrderItem item = OrderItem.create(order, product, qty);
            order.getItems().add(item);

            product.decreaseStock(qty);
            subtotal = subtotal.add(product.getPrice().multiply(BigDecimal.valueOf(qty)));
        }

        BigDecimal shipping = subtotal.compareTo(new BigDecimal("100.00")) >= 0 ? BigDecimal.ZERO : new BigDecimal("5.00");
        BigDecimal discount = (couponCode != null && couponCode.startsWith("SALE")) ? new BigDecimal("10.00") : BigDecimal.ZERO;

        order.setTotalAmount(subtotal.add(shipping).subtract(discount));
        order.setStatus(Order.OrderStatus.PROCESSING);
        return orderRepository.save(order);
    }

    /**
     * TODO #5: 코드 리뷰 - 장시간 작업을 간주하여 진행률 저장을 위한 트랜잭션 분리
     *   (MyBatis 사용시 JpaRepository 호출을 Mapper 호출로 치환했다고 가정)
     * - 시나리오: 일괄 배송 처리(장시간 작업이라고 가정함) 중 진행률을 저장하여 다른 사용자가 변화하는 진행률을 조회 가능해야 함.
     * - 리뷰 포인트: proxy 및 transaction 분리, 예외 전파/롤백 범위, 가독성 등
     * - 상식적인 수준에서 요구사항(기획)을 가정하며 최대한 상세히 작성하세요.
     */
    @Transactional
    public void bulkShipOrdersParent(String jobId, List<Long> orderIds) {
		/*
		  존재하지 않는 작업 Id인 경우 새로운 객체를 생성하는 것이 아닌, 404 예외를 통해서 사용자가 존재하지 않는 작업의 Id를 입력했다는 사실을 반환하는게 좋을 것 같습니다.
		  이는 `.orELseGet()` 메서드 대신 `.orElseThrow()` 메서드를 사용하면, 해결할 수 있을 것 같습니다.
		 */
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseGet(() -> processingStatusRepository.save(ProcessingStatus.builder().jobId(jobId).build()));

        ps.markRunning(orderIds == null ? 0 : orderIds.size());
		/*
		  두번째 `save` 로직에서 변경된 모든 사항을 반영해서 저장하고 있기 때문에, 해당 로직은 불필요하다고 생각합니다.
		*/
        processingStatusRepository.save(ps);

        int processed = 0;

		/*
		  `orderIds` 에 대한 null check 로직이 2번 수행되고 있습니다. `if` 문을 사용해서 `orderIds` 가 `null`인 경우와 그렇지 않은 경우를 나눠서 진행하면, 불필요한 연산을 줄일 수 있을 것 같습니다.
		  혹시 코드의 가독성을 위해서 진행을 한 부분이라면, `ps.markRunning(orderIds)` 로직 이전에 `orderIds == null ? List.<Long>of() : orderIds` 로직을 수행하면, 가독성과 함께 연산횟수도 줄일 수 있을 것 같습니다.
		 */
        for (Long orderId : (orderIds == null ? List.<Long>of() : orderIds)) {
            try {
                // 오래 걸리는 작업 이라는 가정 시뮬레이션 (예: 외부 시스템 연동, 대용량 계산 등)
                orderRepository.findById(orderId).ifPresent(o -> o.setStatus(Order.OrderStatus.PROCESSING));
                // 중간 진행률 저장
                this.updateProgressRequiresNew(jobId, ++processed, orderIds.size());
			/*
			  `catch` 문에서 `Exception`으로 잡을 경우 모든 예외가 잡히게 됩니다. 이는 의도치 않은 예외도 같이 `catch` 문에 잡히기 때문에, 매우 위험한 로직이라고 생각합니다.
			  의도한 예외만 `catch` 문이 잡을 수 있도록 수정 부탁드립니다.
			*/
            } catch (Exception e) {
			/*
			  `catch` 문 내부에 아무런 로직이 없습니다. 이는 `catch` 문에 잡힌 예외를 허용하는 의미입니다. 이 경우 추후 디버깅을 수행할 때, 원인을 찾는데, 어려움이 발생할 가능성이 높습니다.
			  warn 로그 같은 방법으로 해당 부분에서 예외가 발생했다는 내용을 추가하면, 추후 디버깅을 할 때 원인을 찾는데 큰 도움이 될 것 같습니다.
			*/
            }
        }
        ps = processingStatusRepository.findByJobId(jobId).orElse(ps);
        ps.markCompleted();
        processingStatusRepository.save(ps);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateProgressRequiresNew(String jobId, int processed, int total) {
        ProcessingStatus ps = processingStatusRepository.findByJobId(jobId)
                .orElseGet(() -> ProcessingStatus.builder().jobId(jobId).build());
        ps.updateProgress(processed, total);
        processingStatusRepository.save(ps);
    }

}
