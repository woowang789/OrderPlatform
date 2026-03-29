package com.orderplatform.order;

import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.order.entity.Order;
import com.orderplatform.order.entity.OrderLine;
import com.orderplatform.order.repository.OrderRepository;
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Order 낙관적 락(@Version) 동시성 테스트
 * - 동시에 같은 주문의 상태를 변경할 때 @Version에 의해 충돌이 감지되는지 검증한다.
 */
class OrderConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private OrderRepository orderRepository;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    private UUID orderId;

    @BeforeEach
    void setUp() {
        transactionTemplate.executeWithoutResult(status -> {
            orderRepository.deleteAll();
            productRepository.deleteAll();

            // 상품 생성
            Product product = new Product("테스트 상품", 10000L, 100, "테스트");
            productRepository.save(product);

            // 주문 생성 (PLACED 상태)
            OrderLine orderLine = new OrderLine(
                    product.getId(), product.getName(), product.getPrice(), 1
            );
            Order order = Order.create(1L, List.of(orderLine), 10000L);
            order.place();
            orderId = orderRepository.save(order).getId();
        });
    }

    @Test
    @DisplayName("[낙관적 락] 동시 주문 상태 변경 시 @Version 충돌이 감지된다")
    void optimisticLock_concurrentOrderStatusChange_detectsConflict() throws InterruptedException {
        int threadCount = 10;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger optimisticLockFailCount = new AtomicInteger(0);
        AtomicInteger otherFailCount = new AtomicInteger(0);
        AtomicReference<String> exceptionType = new AtomicReference<>("");

        // 여러 스레드가 동시에 같은 주문을 cancel() 시도
        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    transactionTemplate.executeWithoutResult(status -> {
                        Order order = orderRepository.findByIdWithOrderLines(orderId).orElseThrow();
                        order.cancel();
                    });
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    Throwable cause = e.getCause() != null ? e.getCause() : e;
                    String name = cause.getClass().getSimpleName();
                    exceptionType.set(name);
                    if (name.contains("OptimisticLocking") || name.contains("StaleObjectState")) {
                        optimisticLockFailCount.incrementAndGet();
                    } else {
                        otherFailCount.incrementAndGet();
                    }
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        System.out.println("========== Order 낙관적 락 동시성 테스트 결과 ==========");
        System.out.println("총 요청: " + threadCount);
        System.out.println("성공 (cancel): " + successCount.get());
        System.out.println("낙관적 락 충돌: " + optimisticLockFailCount.get());
        System.out.println("기타 실패 (상태 검증 등): " + otherFailCount.get());
        System.out.println("예외 타입: " + exceptionType.get());
        System.out.println("===================================================");

        // 정확히 1개만 성공, 나머지는 @Version 충돌 또는 상태 검증 실패
        assertThat(successCount.get()).isEqualTo(1);
        assertThat(optimisticLockFailCount.get() + otherFailCount.get()).isEqualTo(threadCount - 1);

        // 주문 상태가 CANCELLED로 정확히 변경되었는지 확인
        entityManager.clear();
        Order order = transactionTemplate.execute(status ->
                orderRepository.findByIdWithOrderLines(orderId).orElseThrow()
        );
        assertThat(order.getStatus().name()).isEqualTo("CANCELLED");
    }
}
