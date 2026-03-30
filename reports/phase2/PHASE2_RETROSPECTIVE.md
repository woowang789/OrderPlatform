# Phase 2 헥사고날 아키텍처 전환 회고

> Phase 1의 레이어드 모놀리스를 헥사고날(Ports & Adapters) 아키텍처로 리팩토링한 결과를 정리하고,
> 체감한 문제점을 기록하여 Phase 3(MSA + 이벤트 드리븐) 전환의 근거로 삼는다.

---

## 1. 전환 결과 요약

### 패키지 구조

```
{context}/
├── domain/          # 순수 Java (프레임워크 의존 없음)
│   ├── model/       # Aggregate Root, Value Object
│   ├── exception/   # 도메인 예외
│   └── event/       # 도메인 이벤트
├── application/
│   ├── port/in/     # UseCase 인터페이스 + Command
│   ├── port/out/    # Repository/Gateway 인터페이스
│   └── service/     # UseCase 구현체
└── adapter/
    ├── in/web/      # Controller + DTO
    └── out/         # JPA, PG, 크로스 컨텍스트 Adapter
```

### 전환 범위


| 컨텍스트    | 파일 수 | 도메인 모델  | Value Object                 | Port | Service | Adapter |
| ------- | ---- | ------- | ---------------------------- | ---- | ------- | ------- |
| Member  | 29   | Member  | Email                        | 7    | 3       | 4       |
| Product | 22   | Product | Money, Stock                 | 7    | 3       | 2       |
| Payment | 34   | Payment | PaymentStatus, PaymentMethod | 11   | 3       | 4       |
| Order   | 36   | Order   | OrderLine, OrderStatus       | 12   | 3       | 4       |


### 빌드 및 테스트 상태

- **빌드**: `./gradlew build` 성공
- **전체 테스트**: 146개 (145 PASSED, 1 SKIPPED)
- **ArchUnit 규칙**: 6개 전체 통과
  - 도메인 → 애플리케이션서비스/어댑터 의존 금지
  - 인바운드포트 → 도메인에만 의존
  - 웹어댑터 → 도메인모델 직접 접근 금지
  - 애플리케이션서비스 → 어댑터 의존 금지
  - 아웃바운드어댑터 → 같은 컨텍스트 인바운드포트 의존 금지
  - 컨텍스트 간 도메인 직접 의존 금지

### 테스트 유형별 분포


| 유형                    | 테스트 수 | Spring 컨텍스트 | 특징                               |
| --------------------- | ----- | ----------- | -------------------------------- |
| 도메인 단위 테스트            | 43    | 없음          | 순수 Java, 빠른 실행                   |
| UseCase 단위 테스트 (Mock) | 20    | 없음          | Port Mock 주입                     |
| Adapter 통합 테스트        | 12    | 있음          | Testcontainers DB                |
| API 통합 테스트            | 55    | 있음          | MockMvc + Testcontainers         |
| 동시성 테스트               | 4     | 있음          | ExecutorService + CountDownLatch |
| E2E 플로우 테스트           | 3     | 있음          | 전체 API 체인 검증                     |
| ArchUnit 아키텍처 테스트     | 6     | 없음          | 의존성 규칙 검증                        |
| 기타 (SKIPPED)          | 1     | -           | 락 없는 문제 재현용                      |


---

## 2. 코드 품질 정량 데이터

### 소스 코드 규모


| 구분             | 라인 수       | 파일 수     |
| -------------- | ---------- | -------- |
| 프로덕션 코드 (main) | 3,460줄     | 134개     |
| 테스트 코드 (test)  | 3,439줄     | 32개      |
| **합계**         | **6,899줄** | **166개** |


### 헥사고날 레이어별 코드량


| 레이어                   | 라인 수     | 비율       | 설명                                 |
| --------------------- | -------- | -------- | ---------------------------------- |
| Domain Model          | 551줄     | 15.9%    | 순수 비즈니스 로직, 프레임워크 의존 없음            |
| Port 인터페이스            | 480줄     | 13.9%    | UseCase, Command, Outbound Port    |
| Application Service   | 543줄     | 15.7%    | UseCase 구현체 (오케스트레이션)              |
| JPA Entity            | 285줄     | 8.2%     | 인프라 전용 영속화 객체                      |
| Mapper                | **164줄** | **4.7%** | **Domain ↔ JPA Entity 변환 보일러플레이트** |
| PersistenceAdapter    | 206줄     | 6.0%     | Port 구현체 (DB 접근)                   |
| Controller            | 212줄     | 6.1%     | Web Adapter                        |
| DTO                   | 226줄     | 6.5%     | Request/Response DTO               |
| Cross-context Adapter | 142줄     | 4.1%     | 컨텍스트 간 동기 호출 Adapter               |


### Mapper 보일러플레이트 상세


| Mapper        | 라인 수     | 변환 복잡도                     |
| ------------- | -------- | -------------------------- |
| MemberMapper  | 32줄      | Email VO ↔ String          |
| ProductMapper | 36줄      | Money/Stock VO ↔ primitive |
| PaymentMapper | 39줄      | 단순 필드 매핑                   |
| OrderMapper   | 57줄      | OrderLine 리스트 변환 포함        |
| **합계**        | **164줄** |                            |


Phase 1에서는 도메인 모델 = JPA Entity였으므로 Mapper가 불필요했다. 헥사고날 전환으로 **164줄의 순수 보일러플레이트**가 추가되었다.

### Phase 2 커밋 이력

총 7개 커밋으로 전환 완료:

1. `infra: 헥사고날 아키텍처 패키지 구조 및 공통 인프라 세팅`
2. `refactor: Member 컨텍스트 헥사고날 아키텍처 전환`
3. `fix: .gitignore out/ 규칙 수정 및 누락된 Adapter/Port 파일 추가`
4. `docs: Member 헥사고날 전환 보고서 추가`
5. `refactor: Product 컨텍스트 헥사고날 아키텍처 전환`
6. `refactor: Payment 컨텍스트 헥사고날 아키텍처 전환`
7. `refactor: Order 컨텍스트 헥사고날 아키텍처 전환`

---

## 3. 체감한 문제점 (6가지)

### 문제 1: 모놀리스 구조적 한계 — 전체 재배포

**증상**: 코드는 바운디드 컨텍스트별로 깔끔하게 분리되었지만, 여전히 하나의 Spring Boot 애플리케이션(`OrderPlatformApplication`)에서 동작한다. Payment의 `FakePgAdapter` 하나만 수정해도 전체 서비스를 재배포해야 한다.

**코드 참조**:

- `src/main/java/com/orderplatform/OrderPlatformApplication.java` — 단일 진입점
- `build.gradle.kts` — 단일 모듈, 모든 의존성 공유

**Phase 3 해결**: 각 컨텍스트를 독립 Spring Boot 애플리케이션으로 분리하여 독립 배포 가능하게 한다.

---

### 문제 2: 동기 호출의 장애 전파

**증상**: `RequestPaymentService`가 `PaymentGatewayPort.processPayment()`를 동기 호출한다. `FakePgAdapter`에 3초 지연이 발생하면 POST `/api/payments` 전체가 3초 이상 블로킹되고, 스레드 풀이 고갈되면 다른 API(주문 조회 등)에도 영향을 미친다.

**코드 참조**:

```java
// RequestPaymentService.java:62-63
PgPaymentResult pgResult = paymentGatewayPort.processPayment(
        order.totalAmount(), command.method()
);
```

- `src/main/java/com/orderplatform/payment/application/service/RequestPaymentService.java:62`
- `src/main/java/com/orderplatform/payment/adapter/out/pg/FakePgAdapter.java`

**의도적 설계**: Port 추상화 뒤에 숨겨졌지만 여전히 동기 호출이라는 것을 체감하기 위해 Phase 2에서 의도적으로 유지했다.

**Phase 3 해결**: Kafka 이벤트 기반 비동기 호출로 전환. `order.placed` 이벤트를 발행하면 Payment 서비스가 독립적으로 소비한다.

---

### 문제 3: 단일 DB 공유 락 경합

**증상**: `orders`, `products`, `payments` 테이블이 모두 같은 PostgreSQL 인스턴스에 존재한다. `CreateOrderService`의 `@Transactional` 안에서 `products` 테이블에 비관적 락(`SELECT FOR UPDATE`)을 걸면, 같은 시점에 상품 조회 API도 해당 행의 락 해제를 기다려야 한다.

**코드 참조**:

```java
// CreateOrderService.java:31 — @Transactional 범위
StockInfo stockInfo = decreaseStockPort.decreaseStock(item.productId(), item.quantity());
// → ProductPersistenceAdapter → ProductJpaRepository.findByIdForUpdate() → SELECT FOR UPDATE
```

- `src/main/java/com/orderplatform/order/application/service/CreateOrderService.java:31`
- `src/main/java/com/orderplatform/product/adapter/out/persistence/ProductJpaRepository.java:18-20` (PESSIMISTIC_WRITE)

**Phase 3 해결**: 서비스별 DB 분리. Order DB, Product DB, Payment DB를 각각 운영하여 락 경합 제거.

---

### 문제 4: 결제 성공 후 상태 저장 실패 — 데이터 불일치

**증상**: `RequestPaymentService`에서 PG 결제(`paymentGatewayPort.processPayment()`)는 성공했지만, 이후 `savePaymentPort.save()` 또는 `updateOrderStatusPort.markOrderPaid()` 시점에 DB 에러가 발생하면 돈은 빠져나갔는데 시스템에는 결제 실패로 기록된다.

**코드 참조**:

```java
// RequestPaymentService.java:66-69
if (pgResult.success()) {
    payment.complete(pgResult.pgTxnId());              // 도메인 상태만 변경
    updateOrderStatusPort.markOrderPaid(command.orderId()); // 여기서 DB 에러 발생 가능
}
Payment saved = savePaymentPort.save(payment);          // 또는 여기서 실패
```

- `src/main/java/com/orderplatform/payment/application/service/RequestPaymentService.java:66-74`

**Phase 3/4 해결**: Transactional Outbox 패턴 + 멱등성 키로 "최소 1회 전달"을 보장하고, 보상 트랜잭션(Saga)으로 불일치를 복구한다.

---

### 문제 5: 재고 차감-결제 순서 복원 복잡성

**증상**: 현재는 `CreateOrderService`의 단일 `@Transactional` 안에서 재고 차감 + 주문 저장을 처리하므로, 중간에 예외가 발생하면 자동 롤백된다. 그러나 MSA로 전환하면 Product 서비스와 Order 서비스가 별도 프로세스이므로 단일 트랜잭션이 불가능하다.

**코드 참조**:

```java
// CreateOrderService.java:30-48
@Transactional  // ← 모놀리스라서 가능한 단일 트랜잭션
public OrderInfo createOrder(CreateOrderCommand command) {
    // 1. 재고 차감 (Product DB)
    StockInfo stockInfo = decreaseStockPort.decreaseStock(...);
    // 2. 주문 생성 + 저장 (Order DB) — 같은 트랜잭션
    Order savedOrder = saveOrderPort.save(order);
}
```

- `src/main/java/com/orderplatform/order/application/service/CreateOrderService.java:30-48`
- `src/main/java/com/orderplatform/order/application/service/CancelOrderService.java` (재고 복원 로직)

**Phase 3/4 해결**: 이벤트 체인(`order.placed → stock.deducted → payment.completed`)과 Saga 보상 트랜잭션(`stock.deduction.failed → payment.cancel.requested → order.cancelled`)으로 분산 트랜잭션을 대체한다.

---

### 문제 6: Mapper 보일러플레이트 — 코드량 증가

**증상**: 도메인 모델과 JPA Entity를 분리하면서 양방향 변환 코드(Mapper)가 필수가 되었다. 4개 Mapper에서 총 164줄의 순수 보일러플레이트가 추가되었다. 필드가 추가/변경될 때마다 Domain Model, JPA Entity, Mapper, DTO를 모두 수정해야 하는 산탄총 수술(Shotgun Surgery)이 발생한다.

**코드 참조**:

- `src/main/java/com/orderplatform/member/adapter/out/persistence/MemberMapper.java` (32줄)
- `src/main/java/com/orderplatform/product/adapter/out/persistence/ProductMapper.java` (36줄)
- `src/main/java/com/orderplatform/payment/adapter/out/persistence/PaymentMapper.java` (39줄)
- `src/main/java/com/orderplatform/order/adapter/out/persistence/OrderMapper.java` (57줄)

**정량 데이터**:

- Mapper 총 라인 수: 164줄 (전체 프로덕션 코드의 4.7%)
- JPA Entity 총 라인 수: 285줄 (Phase 1에서는 Domain Model과 합쳐져 있었음)
- Phase 1 대비 추가된 인프라 코드: Mapper(164줄) + JPA Entity(285줄) + PersistenceAdapter(206줄) + Port(480줄) = **1,135줄**

**트레이드오프**: 보일러플레이트 증가의 대가로 도메인 로직이 인프라에서 완전히 분리되어 테스트 용이성과 유연성을 확보했다. 도메인 단위 테스트 43개가 Spring 컨텍스트 없이 실행된다.

---

## 4. Phase 3 진입 근거

### 체감한 문제 체크리스트


| #   | 문제                   | 체감 여부 |
| --- | -------------------- | ----- |
| 1   | 모놀리스 구조적 한계 — 전체 재배포 | [x]   |
| 2   | 동기 호출의 장애 전파         | [x]   |
| 3   | 단일 DB 공유 락 경합        | [x]   |
| 4   | 결제 성공 후 상태 저장 실패     | [x]   |
| 5   | 재고 차감-결제 순서 복원 복잡성   | [x]   |
| 6   | Mapper 보일러플레이트       | [x]   |


### Phase 3 진입 조건 충족 여부


| 조건                                   | 충족  |
| ------------------------------------ | --- |
| 결제/알림 동기 호출의 장애 전파를 Port 추상화 뒤에서도 경험 | [x] |
| 단일 모놀리스 배포의 비효율성을 느낌                 | [x] |
| 단일 DB 테이블 간 락 경합이 발생                 | [x] |
| "서비스들을 독립 프로세스로 분리하고 싶다"는 욕구         | [x] |


### Phase 3 해결 방안 요약


| 문제             | Phase 3 해결                            |
| -------------- | ------------------------------------- |
| 전체 재배포         | 서비스별 독립 배포 (Docker 컨테이너)              |
| 동기 호출 장애 전파    | Kafka 이벤트 기반 비동기 통신                   |
| 단일 DB 락 경합     | 서비스별 DB 분리                            |
| 결제-저장 불일치      | Transactional Outbox + Saga (Phase 4) |
| 재고-결제 분산 트랜잭션  | 이벤트 체인 + 보상 트랜잭션                      |
| Mapper 보일러플레이트 | MSA에서도 유지 (도메인 순수성의 비용)               |


---

## 5. 테스트 실행 시간


| 구분                      | 시간                               |
| ----------------------- | -------------------------------- |
| 전체 테스트 스위트 (146개)       | ~12초                             |
| 도메인 단위 테스트 (43개)        | Spring 컨텍스트 불필요, JVM 워밍업 후 즉시 완료 |
| 통합 테스트 (Testcontainers) | ~10초 (PostgreSQL 컨테이너 시작 포함)     |


**핵심 성과**: 도메인 로직 변경 시 Spring 컨텍스트 로딩 없이 빠른 피드백 루프를 확보했다. Phase 1에서는 모든 테스트가 Spring 컨텍스트를 필요로 했으나, Phase 2에서는 도메인/UseCase 테스트(63개)가 프레임워크 독립적으로 실행된다.

---

## 6. 결론

Phase 2 헥사고날 전환을 통해 달성한 것:

1. **도메인 순수성**: 비즈니스 로직이 인프라(JPA, Spring)에서 완전히 분리
2. **테스트 용이성**: 63개 테스트가 Spring 없이 실행 (전체의 43%)
3. **의존성 규칙 자동 검증**: ArchUnit 6개 규칙으로 아키텍처 붕괴 방지
4. **Port 추상화**: MSA 전환 시 Adapter만 교체하면 되는 구조 확보

그러나 **모놀리스의 근본적 한계**(단일 프로세스, 단일 DB, 동기 호출)는 아키텍처 패턴으로 해결할 수 없다. 이제 Phase 3(MSA + 이벤트 드리븐)으로 진입할 준비가 되었다.