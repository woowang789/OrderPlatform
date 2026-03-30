# Order 컨텍스트 헥사고날 아키텍처 전환 보고서

> Phase 2 — 5단계 (2026-03-30)

---

## 1. 전환 개요

레이어드 아키텍처(Phase 1)의 Order 코드를 헥사고날(Ports & Adapters) 아키텍처로 전환하여, 도메인 로직을 인프라로부터 완전 분리했다. Member(2단계), Product(3단계), Payment(4단계) 전환 패턴을 따르되, Order 고유의 **OrderLine record↔JPA Entity 양방향 매핑**, **크로스 컨텍스트 오케스트레이션(Product 재고 차감, Payment 결제 요청)**, **트랜잭션 경계 관리**가 핵심 차별점이다.

4개 컨텍스트 중 가장 복잡한 전환으로, 다른 3개 컨텍스트를 모두 참조하는 오케스트레이터 역할이다.

---

## 2. 구조 변화

### Before — 레이어드 아키텍처 (Phase 1)

```
order/
├── controller/OrderController.java       ← REST 엔드포인트
├── service/OrderService.java             ← 모든 비즈니스 로직 + Product 직접 참조
├── repository/OrderRepository.java       ← Spring Data JPA
├── entity/
│   ├── Order.java                        ← JPA Entity = 도메인 모델
│   ├── OrderLine.java                    ← JPA Entity (주문 항목)
│   └── OrderStatus.java
└── dto/
    ├── CreateOrderRequest.java
    ├── OrderItemRequest.java
    ├── OrderResponse.java
    └── OrderLineResponse.java
```

**의존 흐름**: `Controller → OrderService → OrderRepository/ProductJpaRepository → JPA Entity`

- Entity가 곧 도메인 모델 — `@Entity`, `@Version`, 비즈니스 메서드가 혼재
- OrderService가 `ProductJpaRepository`, `ProductJpaEntity`를 **직접 import** (아키텍처 위반)
- OrderLine이 `@ManyToOne`으로 Order와 양방향 관계 → 도메인 로직과 영속화가 혼합
- 예외 클래스가 `common/exception/`에 위치 (도메인 밖)

### After — 헥사고날 아키텍처 (Phase 2)

```
order/
├── domain/                                ← 순수 Java (외부 의존 0)
│   ├── model/
│   │   ├── Order.java                     ← 순수 도메인 모델
│   │   ├── OrderLine.java                 ← record (Value Object)
│   │   └── OrderStatus.java
│   ├── exception/
│   │   ├── InvalidOrderStatusException.java
│   │   └── OrderNotFoundException.java
│   └── event/
│       ├── OrderPlacedEvent.java          ← Phase 3 이벤트 대비
│       └── OrderCancelledEvent.java
├── application/
│   ├── port/in/                           ← Inbound Port (UseCase)
│   │   ├── CreateOrderUseCase.java
│   │   ├── CancelOrderUseCase.java
│   │   ├── GetOrderUseCase.java
│   │   ├── CreateOrderCommand.java
│   │   ├── CancelOrderCommand.java
│   │   ├── OrderItemCommand.java
│   │   ├── OrderInfo.java
│   │   └── OrderLineInfo.java
│   ├── port/out/                          ← Outbound Port (인프라 추상화)
│   │   ├── LoadOrderPort.java
│   │   ├── SaveOrderPort.java
│   │   ├── DecreaseStockPort.java
│   │   ├── RestoreStockPort.java
│   │   ├── RequestPaymentPort.java
│   │   └── StockInfo.java
│   └── service/                           ← UseCase 구현체
│       ├── CreateOrderService.java
│       ├── CancelOrderService.java
│       └── GetOrderService.java
└── adapter/
    ├── in/web/                            ← Inbound Adapter
    │   ├── OrderController.java
    │   └── dto/
    │       ├── CreateOrderRequest.java
    │       ├── OrderItemRequest.java
    │       ├── OrderResponse.java
    │       └── OrderLineResponse.java
    └── out/
        ├── persistence/                   ← Outbound Adapter (DB)
        │   ├── OrderJpaEntity.java        ← @Entity + @Version
        │   ├── OrderLineJpaEntity.java    ← @Entity + @ManyToOne
        │   ├── OrderJpaRepository.java
        │   ├── OrderMapper.java
        │   └── OrderPersistenceAdapter.java
        ├── stock/                         ← Outbound Adapter (Product 재고)
        │   └── StockAdapter.java
        └── payment/                       ← Outbound Adapter (Payment 결제)
            └── PaymentRequestAdapter.java
```

**의존 흐름**:
```
Controller → UseCase(interface) → Service → Port(interface) ← Adapter(구현체)
                                     ↓
                              Domain Model (순수 Java)
                              ├── Order.create() → PLACED 자동 전이
                              ├── OrderLine record (Value Object)
                              └── 도메인 이벤트 정의
```

---

## 3. 정량 비교

| 항목 | Phase 1 | Phase 2 | 변화 |
|------|---------|---------|------|
| 파일 수 | 10개 | 31개 | +21개 |
| 인터페이스 수 | 1개 (Repository) | 8개 (UseCase 3 + Port 5) | +7개 |
| 도메인 이벤트 | 0개 | 2개 | +2개 |
| 도메인 외부 의존 | JPA, Lombok, Product 직접 참조 | **없음** | 완전 분리 |
| 서비스 클래스 | 1개 (OrderService) | 3개 (UseCase별 분리) | +2개 |
| Outbound Adapter 종류 | 0개 (직접 호출) | 3개 (persistence, stock, payment) | +3개 |
| DTO 변환 단계 | 1단계 (Entity→Response) | 3단계 (Domain→Info→Response) | +2단계 |

---

## 4. 핵심 변경사항

### 4.1 도메인 순수화

**Phase 1** — JPA Entity가 곧 도메인 모델
```java
@Entity @Table(name = "orders")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Order extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    private OrderStatus status;
    @Version
    private Long version;
    @OneToMany(mappedBy = "order", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<OrderLine> orderLines = new ArrayList<>();

    public void place() { ... }
    public void cancel() { ... }
}
```

**Phase 2** — 순수 Java 도메인 모델 (JPA/Lombok 의존 없음)
```java
public class Order {
    private final UUID id;
    private final Long memberId;
    private OrderStatus status;
    private final long totalAmount;
    private final Long version;
    private final List<OrderLine> orderLines;  // OrderLine = record (VO)
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Order create(Long memberId, List<OrderLine> orderLines) {
        // OrderLine에서 totalAmount 자동 계산, CREATED → place() → PLACED
        long totalAmount = orderLines.stream()
                .mapToLong(line -> line.price() * line.quantity()).sum();
        Order order = new Order(null, memberId, OrderStatus.CREATED,
                totalAmount, null, List.copyOf(orderLines), null, null);
        order.place();
        return order;
    }

    public static Order reconstitute(...) { ... }  // DB 복원용
}
```

**변경 포인트**:
- JPA 어노테이션 전부 제거 → `OrderJpaEntity`, `OrderLineJpaEntity`로 분리
- `OrderLine`을 `@Entity` → `record`(Value Object)로 전환 — 불변, ID 없음
- `create()`에서 totalAmount를 OrderLine 목록에서 자동 계산 (외부 전달 불필요)
- `create()`에서 PLACED까지 자동 전이 — 생성 의도 캡슐화
- `id = null` — Payment 패턴 일관성 (`@GeneratedValue`가 DB 저장 시 할당)
- `List.copyOf()` + `Collections.unmodifiableList()` — 불변 컬렉션 보장

### 4.2 OrderLine: JPA Entity → record Value Object

**Phase 1**:
```java
@Entity @Table(name = "order_lines")
public class OrderLine {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "order_id", nullable = false)
    private Order order;
    private Long productId;
    private String productName;
    private long price;
    private int quantity;
}
```

**Phase 2** — 도메인 모델:
```java
public record OrderLine(
        Long productId,
        String productName,
        long price,
        int quantity
) {}
```

- `id`, `order` 필드 제거 — 도메인에서 OrderLine은 Order Aggregate 내에서만 의미
- record로 선언 — 불변, equals/hashCode/toString 자동 생성
- 영속화 시 `OrderLineJpaEntity`로 변환 (OrderMapper가 담당)

### 4.3 도메인 이벤트 정의

Phase 3(MSA + 이벤트 드리븐) 전환을 대비하여 2종의 도메인 이벤트를 `record`로 정의:

```java
public record OrderPlacedEvent(
        UUID orderId, Long memberId, long totalAmount,
        LocalDateTime occurredAt
) implements DomainEvent { ... }

public record OrderCancelledEvent(
        UUID orderId, Long memberId,
        LocalDateTime occurredAt
) implements DomainEvent { ... }
```

- 현재는 정의만 해두고, Phase 3에서 `order.placed → payment.completed → stock.deducted → order.confirmed` 이벤트 체인 시 활용

### 4.4 크로스 컨텍스트 오케스트레이션 — 재고 Port 분리

**Phase 1** — OrderService가 Product JPA를 직접 참조
```java
// OrderService
private final ProductJpaRepository productRepository;  // 아키텍처 위반

ProductJpaEntity product = productRepository.findByIdForUpdate(item.productId()).orElseThrow();
product.decreaseStock(item.quantity());
totalAmount += product.getPrice() * item.quantity();
```

**Phase 2** — Outbound Port로 추상화

| Port | 메서드 | 역할 |
|------|--------|------|
| `DecreaseStockPort` | `decreaseStock(Long productId, int quantity)` → `StockInfo` | 재고 차감 + 상품 정보 반환 |
| `RestoreStockPort` | `restoreStock(Long productId, int quantity)` | 재고 복원 (주문 취소 시) |

```java
// Application Service — Port 인터페이스에만 의존
StockInfo stockInfo = decreaseStockPort.decreaseStock(item.productId(), item.quantity());
orderLines.add(new OrderLine(item.productId(), stockInfo.productName(), stockInfo.price(), item.quantity()));

// StockAdapter — Product JPA를 직접 사용 (모놀리스 한정)
@Component
public class StockAdapter implements DecreaseStockPort, RestoreStockPort {
    private final ProductJpaRepository productJpaRepository;
    // Phase 3 MSA 전환 시 Kafka 이벤트 기반으로 교체 예정
}
```

**StockInfo** — 크로스 컨텍스트 DTO:
```java
public record StockInfo(String productName, long price) {}
```

- 재고 차감 시 상품명/가격을 함께 반환 → OrderLine 스냅샷 생성에 사용
- CreateOrderService는 `ProductJpaEntity`를 **전혀 모름**

### 4.5 결제 요청 Port — 의도적 동기 호출 설계

```java
public interface RequestPaymentPort {
    void requestPayment(UUID orderId, Long memberId, long totalAmount);
}

@Component
public class PaymentRequestAdapter implements RequestPaymentPort {
    private final RequestPaymentUseCase requestPaymentUseCase;
    // 의도적 동기 호출 — Phase 3에서 Kafka 이벤트 기반 비동기로 전환
}
```

- **현재(Phase 2)**: 결제는 기존과 동일하게 별도 API(`POST /api/payments`)로 요청 — API 호환 유지
- **Port 정의 이유**: Phase 3에서 주문 생성 시 자동 결제 이벤트 발행으로 전환할 때 활용
- ROADMAP이 경고한 "Port 뒤로 숨겨졌을 뿐 동기 호출의 장애 전파" 문제를 체감하기 위한 설계

### 4.6 OrderLine 양방향 매핑 (Mapper)

가장 복잡한 매핑 — 도메인 `record` ↔ JPA `@Entity` 변환:

```java
public final class OrderMapper {

    // JPA → Domain
    public static Order toDomain(OrderJpaEntity entity) {
        List<OrderLine> orderLines = entity.getOrderLines().stream()
                .map(line -> new OrderLine(
                        line.getProductId(), line.getProductName(),
                        line.getPrice(), line.getQuantity()))
                .toList();
        return Order.reconstitute(entity.getId(), entity.getMemberId(),
                entity.getStatus(), entity.getTotalAmount(), entity.getVersion(),
                orderLines, entity.getCreatedAt(), entity.getUpdatedAt());
    }

    // Domain → JPA
    public static OrderJpaEntity toJpaEntity(Order order) {
        OrderJpaEntity entity = new OrderJpaEntity(
                order.getMemberId(), order.getStatus(), order.getTotalAmount());
        for (OrderLine line : order.getOrderLines()) {
            entity.addOrderLine(new OrderLineJpaEntity(
                    line.productId(), line.productName(), line.price(), line.quantity()));
        }
        return entity;
    }
}
```

- `toDomain`: OrderLineJpaEntity → OrderLine record (id, order 참조 무시)
- `toJpaEntity`: OrderLine record → OrderLineJpaEntity (id=null, JPA가 IDENTITY 할당)
- `orphanRemoval = true`로 OrderLine 생명주기 관리

### 4.7 Persistence Adapter — save 신규/업데이트 분기

```java
@Override
public Order save(Order order) {
    if (order.getId() == null) {
        // 신규 생성 — toJpaEntity → repo.save
        OrderJpaEntity entity = OrderMapper.toJpaEntity(order);
        OrderJpaEntity saved = orderJpaRepository.save(entity);
        return OrderMapper.toDomain(saved);
    } else {
        // 업데이트 — 기존 엔티티 조회 → 상태만 갱신 (dirty checking)
        OrderJpaEntity entity = orderJpaRepository.findByIdWithOrderLines(order.getId())
                .orElseThrow();
        entity.updateFrom(order.getStatus());
        return OrderMapper.toDomain(entity);
    }
}
```

- `id == null`로 신규/기존 판별 (Payment 패턴 일관성)
- 업데이트 시 `updateFrom(status)`만 갱신 — OrderLine은 변경 불가(불변 스냅샷)

### 4.8 서비스 분리 (단일 책임)

**Phase 1**: `OrderService` 1개가 생성 + 조회 + 취소 모두 담당 + Product 직접 참조

**Phase 2**: UseCase별로 분리
- `CreateOrderService` → 재고 차감(DecreaseStockPort) → Order.create() → 저장
- `CancelOrderService` → 주문 조회 → cancel() → 재고 복원(RestoreStockPort) → 저장
- `GetOrderService` → 조회 → OrderInfo 변환

### 4.9 예외 이동

| 예외 | Phase 1 위치 | Phase 2 위치 |
|------|-------------|-------------|
| `InvalidOrderStatusException` | `common/exception/` | `order/domain/exception/` |
| `OrderNotFoundException` | `common/exception/` | `order/domain/exception/` |

- `BusinessException` 상속 유지 — 기존 GlobalExceptionHandler와 호환
- Payment의 `RequestPaymentService`에서 `OrderNotFoundException` import 경로 변경

---

## 5. 의존성 규칙 검증 (ArchUnit)

```java
// 도메인은 application.service, adapter에 의존하지 않는다
noClasses().that().resideInAPackage("..domain..")
    .should().dependOnClassesThat()
    .resideInAnyPackage("..application.service..", "..adapter..");

// 인바운드 포트는 도메인과 자기 패키지에만 의존한다
classes().that().resideInAPackage("..application.port.in..")
    .should().onlyDependOnClassesThat()
    .resideInAnyPackage("..domain..", "..application.port.in..", "java..", "jakarta..");

// 웹 어댑터는 도메인 모델에 직접 접근하지 않는다
noClasses().that().resideInAPackage("..adapter.in..")
    .should().dependOnClassesThat()
    .resideInAPackage("..domain.model..");
```

3개 규칙 모두 **PASSED**.

---

## 6. API 호환성

기존 API 엔드포인트와 응답 형식을 **100% 유지**.

| Method | Endpoint | 변화 |
|--------|----------|------|
| POST | `/api/orders` | 없음 |
| GET | `/api/orders/{id}` | 없음 |
| GET | `/api/orders` | 없음 |
| POST | `/api/orders/{id}/cancel` | 없음 |

기존 통합 테스트(OrderIntegrationTest) 9개 케이스, PaymentIntegrationTest 10개 케이스 모두 통과.
전체 43개 테스트 PASSED (1개 SKIPPED는 기존과 동일).

---

## 7. 발견 및 해결된 이슈

### 7.1 Order.create()에서 UUID 생성 vs Payment 패턴 일관성

- **증상**: 최초 설계에서 `Order.create()`가 `UUID.randomUUID()`로 id를 생성 → `@GeneratedValue(UUID)` JPA와 충돌하여 id가 이중 생성됨
- **원인**: Payment는 `create()` 시 `id = null`로 두고 DB 저장 시 할당하는 패턴
- **해결**: Order도 동일하게 `id = null`로 변경, `@GeneratedValue(UUID)`가 저장 시 할당

### 7.2 CreateOrderService의 RequestPaymentPort 호출과 API 호환성

- **증상**: CreateOrderService에서 `requestPaymentPort.requestPayment()`를 호출하면 주문 생성 시 자동 결제 → 기존 PaymentIntegrationTest가 중복 결제(409 CONFLICT)로 실패
- **원인**: 기존 API에서는 주문 생성(`POST /api/orders`)과 결제(`POST /api/payments`)가 별도 API. 주문 생성 시 자동 결제하면 기존 호환 깨짐
- **해결**: CreateOrderService에서 RequestPaymentPort 호출 제거. Port는 정의만 유지하고 Phase 3 이벤트 전환 시 활용. API 호환 100% 유지

### 7.3 OrderJpaEntity의 package-private 메서드 접근

- **증상**: `OrderAdapterForPayment`(Payment 컨텍스트)에서 `OrderJpaEntity.updateFrom()` 호출 시 접근 불가
- **원인**: `updateFrom()`, `addOrderLine()`이 package-private으로 선언 — 같은 패키지 내에서만 접근
- **해결**: 크로스 컨텍스트 접근이 필요한 `updateFrom()`, `addOrderLine()`을 `public`으로 변경

### 7.4 통합 테스트 import 경로 변경

- **증상**: OrderIntegrationTest, OrderConcurrencyTest, PaymentIntegrationTest에서 레거시 import 실패
- **해결**:
  - `order.entity.Order` → `order.adapter.out.persistence.OrderJpaEntity`
  - `order.repository.OrderRepository` → `order.adapter.out.persistence.OrderJpaRepository`
  - `order.dto.CreateOrderRequest` → `order.adapter.in.web.dto.CreateOrderRequest`
  - `common.exception.OrderNotFoundException` → `order.domain.exception.OrderNotFoundException`

---

## 8. 4개 컨텍스트 전환 비교

| 항목 | Member (2단계) | Product (3단계) | Payment (4단계) | Order (5단계) |
|------|---------------|-----------------|-----------------|---------------|
| Value Object | 1개 (Email) | 2개 (Money, Stock) | 0개 | 1개 (OrderLine record) |
| 동시성 제어 | 없음 | 비관적 락 + 낙관적 락 | 낙관적 락 | 비관적 락(재고) + 낙관적 락(@Version) |
| 외부 시스템 연동 | Security (BCrypt, JWT) | 없음 | PG사 (FakePgAdapter) | 없음 |
| 크로스 컨텍스트 | 없음 | OrderService 임시 수정 | Order Port 추상화 | Product Port + Payment Port |
| Outbound Adapter 수 | 2개 | 1개 | 3개 | 3개 (persistence, stock, payment) |
| 도메인 이벤트 | 0개 | 0개 | 3개 | 2개 |
| 서비스 분리 | 3개 | 3개 | 3개 | 3개 |
| Mapper 복잡도 | 단순 (1:1) | 중간 (VO 변환) | 중간 (상태만 업데이트) | **높음** (OrderLine record↔JPA Entity) |

**핵심 차이**: Order는 **유일한 오케스트레이터 컨텍스트**. 재고 차감(Product)과 결제 요청(Payment) 두 가지 크로스 컨텍스트 Port를 가지며, OrderLine의 record↔JPA Entity 양방향 매핑이 가장 복잡한 Mapper를 만들어낸다.

---

## 9. Phase 3 전환 시 변경 포인트

Order 헥사고날 전환으로 Phase 3(MSA + 이벤트 드리븐) 전환 시 변경이 필요한 지점이 명확해졌다:

| 현재 Adapter | Phase 3 변경 | 이유 |
|-------------|-------------|------|
| `StockAdapter` (Product JPA 직접 사용) | Kafka 이벤트 기반 Adapter | 서비스 분리로 직접 DB 접근 불가 |
| `PaymentRequestAdapter` (동기 호출) | Kafka 이벤트 발행 Adapter | 비동기 결제 요청으로 전환 |
| `OrderPersistenceAdapter` | 독립 DB 연결 Adapter | 서비스별 DB 분리 |

**재고 차감 시점 변경**: Phase 2에서는 `주문 생성 시 재고 차감` → Phase 3에서는 `결제 완료 후 재고 차감`으로 변경 (MSA에서 단일 트랜잭션 불가)

**도메인 이벤트 활용**: `OrderPlacedEvent`를 Kafka로 발행하여 Payment 서비스에 결제 요청, `OrderCancelledEvent`로 재고 복원 트리거

**Saga 보상 트랜잭션**: `stock.deduction.failed → payment.cancel.requested → payment.cancelled → order.cancelled` 보상 체인은 Phase 4에서 구현

---

## 10. 트레이드오프

| 장점 | 비용 |
|------|------|
| Product 의존 완전 제거 — StockAdapter만 교체로 MSA 전환 가능 | 파일 수 +21개 (최대 증가) |
| OrderLine record로 불변성 보장 — 스냅샷 데이터 무결성 | Mapper 복잡도 증가 (record↔JPA Entity 양방향) |
| 결제 Port 선제 정의 — Phase 3 즉시 활용 가능 | 현재는 미사용 (RequestPaymentPort 정의만) |
| 오케스트레이터 의존 방향 명확화 | DTO 변환 3단계 (Domain→Info→Response) 보일러플레이트 |
| 3종 Adapter 분리 — 각각 독립 교체 가능 | Adapter 코드량 증가 |

> Phase 2 전환이 완료되면서 ROADMAP이 예고한 **"체감할 문제들"**이 구체화되었다:
> 1. **동기 호출의 장애 전파** — `RequestPaymentPort`가 Port 뒤에 숨겨졌지만 동기 호출 본질은 동일
> 2. **단일 DB 공유 락 경합** — StockAdapter가 orders + products 테이블을 같은 트랜잭션에서 접근
> 3. **Mapper 보일러플레이트** — Order가 4개 컨텍스트 중 가장 많은 변환 코드 보유
> 4. **모놀리스 한계** — 코드는 깔끔해졌지만 여전히 하나의 프로세스, 하나의 배포 단위
>
> 이 문제들이 Phase 3(MSA + 이벤트 드리븐) 전환의 직접적 근거가 된다.
