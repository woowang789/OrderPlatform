package com.orderplatform.product;

import com.orderplatform.common.AbstractIntegrationTest;
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;
import com.orderplatform.product.service.ProductService;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * 동시성 테스트
 * - 락 없이 동시 재고 차감 시 정합성이 깨지는 문제를 재현한다.
 * - 비관적 락(SELECT FOR UPDATE) 적용 후 정합성 보장을 검증한다.
 * - 낙관적 락(@Version + 재시도) 방식과 성능을 비교한다.
 * - @Transactional을 클래스에 붙이지 않는다 (각 스레드가 독립 트랜잭션 사용).
 */
class ProductConcurrencyTest extends AbstractIntegrationTest {

    @Autowired
    private ProductService productService;

    @Autowired
    private ProductRepository productRepository;

    @Autowired
    private TransactionTemplate transactionTemplate;

    @Autowired
    private EntityManager entityManager;

    private Long productId;

    @BeforeEach
    void setUp() {
        // 기존 데이터 정리 후 테스트 상품 생성 (재고 100개)
        transactionTemplate.executeWithoutResult(status -> {
            productRepository.deleteAll();
            Product product = new Product("동시성 테스트 상품", 10000L, 100, "테스트");
            productId = productRepository.save(product).getId();
        });
    }

    @Test
    @Disabled("비관적 락 적용 후에는 race condition이 발생하지 않음 — 적용 전 문제 재현용")
    @DisplayName("[문제 재현] 락 없이 동시 재고 차감 시 Lost Update가 발생한다")
    void withoutLock_concurrentStockDecrease_causesLostUpdate() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount); // 모든 스레드 준비 대기
        CountDownLatch startLatch = new CountDownLatch(1);           // 동시 시작 신호
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown(); // 준비 완료 알림
                    startLatch.await();     // 시작 신호 대기

                    // 각 스레드에서 서비스 메서드 호출 → Spring이 스레드별 독립 트랜잭션 생성
                    productService.decreaseStock(productId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        // 모든 스레드가 준비될 때까지 대기 후 동시 시작
        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        // 1차 캐시를 비우고 DB에서 직접 조회
        entityManager.clear();
        Product product = transactionTemplate.execute(status ->
                productRepository.findById(productId).orElseThrow()
        );

        int remainingStock = product.getStock();
        int expectedStock = 100 - successCount.get();

        System.out.println("========== 동시성 문제 재현 결과 (락 없음) ==========");
        System.out.println("총 요청: " + threadCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패 (재고 부족 등): " + failCount.get());
        System.out.println("기대 재고: " + expectedStock);
        System.out.println("실제 재고: " + remainingStock);
        System.out.println("정합성 일치 여부: " + (remainingStock == expectedStock));
        System.out.println("=================================================");

        // Lost Update 증명:
        // 100개 스레드가 거의 모두 성공하지만 (재고 체크 통과),
        // 실제 DB에는 일부 차감만 반영됨 (덮어쓰기 발생)
        // → 실제 재고 > 기대 재고 (= 정합성 깨짐)
        assertThat(remainingStock)
                .as("락이 없으면 Lost Update로 인해 실제 재고가 기대 재고보다 많아야 한다")
                .isGreaterThan(expectedStock);
    }

    @Test
    @DisplayName("[비관적 락] 동시 재고 차감 시 정합성이 보장된다")
    void withPessimisticLock_concurrentStockDecrease_ensuresConsistency() throws InterruptedException {
        int threadCount = 100;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    productService.decreaseStock(productId, 1);
                    successCount.incrementAndGet();
                } catch (Exception e) {
                    failCount.incrementAndGet();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        assertThat(executor.awaitTermination(30, TimeUnit.SECONDS)).isTrue();

        long elapsed = System.nanoTime() - startTime;

        entityManager.clear();
        Product product = transactionTemplate.execute(status ->
                productRepository.findById(productId).orElseThrow()
        );

        System.out.println("========== 비관적 락 동시성 테스트 결과 ==========");
        System.out.println("총 요청: " + threadCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패 (재고 부족 등): " + failCount.get());
        System.out.println("최종 재고: " + product.getStock());
        System.out.println("소요 시간: " + (elapsed / 1_000_000) + "ms");
        System.out.println("===============================================");

        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(successCount.get()).isEqualTo(100);
        assertThat(product.getStock()).isZero();
    }

    @Test
    @DisplayName("[낙관적 락] 동시 재고 차감 시 재시도로 정합성을 보장한다")
    void withOptimisticLock_concurrentStockDecrease_ensuresConsistencyWithRetry() throws InterruptedException {
        int threadCount = 100;
        int maxRetry = 50;
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        CountDownLatch readyLatch = new CountDownLatch(threadCount);
        CountDownLatch startLatch = new CountDownLatch(1);
        AtomicInteger successCount = new AtomicInteger(0);
        AtomicInteger failCount = new AtomicInteger(0);
        AtomicInteger totalRetryCount = new AtomicInteger(0);

        long startTime = System.nanoTime();

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    readyLatch.countDown();
                    startLatch.await();

                    // 낙관적 락 + 재시도 패턴
                    for (int attempt = 0; attempt <= maxRetry; attempt++) {
                        try {
                            productService.decreaseStockWithOptimisticLock(productId, 1);
                            successCount.incrementAndGet();
                            break;
                        } catch (Exception e) {
                            if (attempt == maxRetry) {
                                failCount.incrementAndGet();
                            } else {
                                totalRetryCount.incrementAndGet();
                            }
                        }
                    }
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                    failCount.incrementAndGet();
                }
            });
        }

        readyLatch.await(10, TimeUnit.SECONDS);
        startLatch.countDown();

        executor.shutdown();
        assertThat(executor.awaitTermination(60, TimeUnit.SECONDS)).isTrue();

        long elapsed = System.nanoTime() - startTime;

        entityManager.clear();
        Product product = transactionTemplate.execute(status ->
                productRepository.findById(productId).orElseThrow()
        );

        System.out.println("========== 낙관적 락 동시성 테스트 결과 ==========");
        System.out.println("총 요청: " + threadCount);
        System.out.println("성공: " + successCount.get());
        System.out.println("실패 (재시도 초과): " + failCount.get());
        System.out.println("총 재시도 횟수: " + totalRetryCount.get());
        System.out.println("최종 재고: " + product.getStock());
        System.out.println("소요 시간: " + (elapsed / 1_000_000) + "ms");
        System.out.println("===============================================");

        // 낙관적 락 + 재시도로 정합성 보장
        assertThat(successCount.get() + failCount.get()).isEqualTo(threadCount);
        assertThat(product.getStock()).isEqualTo(100 - successCount.get());
    }
}
