# Payment 컨텍스트 헥사고날 아키텍처 전환 보고서

> Phase 2 — 4단계 (2026-03-30)

---

## 1. 전환 개요

레이어드 아키텍처(Phase 1)의 Payment 코드를 헥사고날(Ports & Adapters) 아키텍처로 전환하여, 도메인 로직을 인프라로부터 완전 분리했다. Member(2단계), Product(3단계) 전환 패턴을 따르되, Payment 고유의 **외부 PG 연동 Adapter 추상화**와 **크로스 컨텍스트(Order) 의존성 Port 분리**가 핵심 차별점이다.

---

## 2. 구조 변화

### Before — 레이어드 아키텍처 (Phase 1)

```
payment/
├── controller/PaymentController.java     ← REST 엔드포인트
├── service/PaymentService.java           ← 모든 비즈니스 로직 + Order 직접 참조
├── repository/PaymentRepository.java     ← Spring Data JPA
├── entity/
│   ├── Payment.java                      ← JPA Entity = 도메인 모델
│   ├── PaymentStatus.java
│   └── PaymentMethod.java
├── dto/
│   ├── CreatePaymentRequest.java
│   └── PaymentResponse.java
└── pg/
    ├── FakePgClient.java                 ← PG 클라이언트 (Port 추상화 없음)
    └── PgPaymentResult.java
```

**의존 흐름**: `Controller → Service → Repository/OrderRepository/OrderService/FakePgClient → JPA Entity`

- Entity가 곧 도메인 모델 — `@Entity`, `@Version`, 비즈니스 메서드가 혼재
- PaymentService가 `OrderRepository`, `OrderService`를 **직접 import** (강결합)
- FakePgClient가 Port 추상화 없이 `@Component`로 직접 주입
- 예외 클래스가 `common/exception/`에 위치 (도메인 밖)

### After — 헥사고날 아키텍처 (Phase 2)

```
payment/
├── domain/                              ← 순수 Java (외부 의존 0)
│   ├── model/
│   │   ├── Payment.java                 ← 순수 도메인 모델
│   │   ├── PaymentStatus.java
│   │   └── PaymentMethod.java
│   ├── exception/
│   │   ├── InvalidPaymentStatusException.java
│   │   ├── DuplicatePaymentException.java
│   │   └── PaymentNotFoundException.java
│   └── event/
│       ├── PaymentCompletedEvent.java   ← Phase 3 이벤트 대비
│       ├── PaymentFailedEvent.java
│       └── PaymentCancelledEvent.java
├── application/
│   ├── port/in/                         ← Inbound Port (UseCase)
│   │   ├── RequestPaymentUseCase.java
│   │   ├── CancelPaymentUseCase.java
│   │   ├── GetPaymentUseCase.java
│   │   ├── RequestPaymentCommand.java
│   │   ├── CancelPaymentCommand.java
│   │   └── PaymentInfo.java
│   ├── port/out/                        ← Outbound Port (인프라 추상화)
│   │   ├── LoadPaymentPort.java
│   │   ├── SavePaymentPort.java
│   │   ├── PaymentGatewayPort.java
│   │   ├── PgPaymentResult.java
│   │   ├── LoadOrderForPaymentPort.java
│   │   ├── UpdateOrderStatusPort.java
│   │   └── OrderInfoForPayment.java
│   └── service/                         ← UseCase 구현체
│       ├── RequestPaymentService.java
│       ├── CancelPaymentService.java
│       └── GetPaymentService.java
└── adapter/
    ├── in/web/                          ← Inbound Adapter
    │   ├── PaymentController.java
    │   └── dto/
    │       ├── CreatePaymentRequest.java
    │       └── PaymentResponse.java
    └── out/
        ├── persistence/                 ← Outbound Adapter (DB)
        │   ├── PaymentJpaEntity.java    ← @Entity + @Version
        │   ├── PaymentJpaRepository.java
        │   ├── PaymentMapper.java
        │   └── PaymentPersistenceAdapter.java
        ├── pg/                          ← Outbound Adapter (PG사)
        │   └── FakePgAdapter.java
        └── order/                       ← Outbound Adapter (Order 컨텍스트)
            └── OrderAdapterForPayment.java
```

**의존 흐름**:
```
Controller → UseCase(interface) → Service → Port(interface) ← Adapter(구현체)
                                     ↓
                              Domain Model (순수 Java)
                              ├── 상태 전이 규칙 내재화
                              └── 도메인 이벤트 정의
```

---

## 3. 정량 비교

| 항목 | Phase 1 | Phase 2 | 변화 |
|------|---------|---------|------|
| 파일 수 | 10개 | 28개 | +18개 |
| 인터페이스 수 | 1개 (Repository) | 8개 (UseCase 3 + Port 5) | +7개 |
| 도메인 이벤트 | 0개 | 3개 | +3개 |
| 도메인 외부 의존 | JPA, Lombok, Order 직접 참조 | **없음** | 완전 분리 |
| 서비스 클래스 | 1개 (PaymentService) | 3개 (UseCase별 분리) | +2개 |
| Outbound Adapter 종류 | 0개 (직접 호출) | 3개 (persistence, pg, order) | +3개 |

---

## 4. 핵심 변경사항

### 4.1 도메인 순수화

**Phase 1** — JPA Entity가 곧 도메인 모델
```java
@Entity @Table(name = "payments")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Payment extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;
    @Enumerated(EnumType.STRING)
    private PaymentStatus status;
    @Version
    private Long version;

    public void complete(String pgTxnId) { ... }
    public void cancel() { ... }
}
```

**Phase 2** — 순수 Java 도메인 모델 (JPA/Lombok 의존 없음)
```java
public class Payment {
    private final UUID id;
    private final UUID orderId;
    private final Long memberId;
    private final long amount;
    private final PaymentMethod method;
    private PaymentStatus status;          // 상태 전이 가능
    private String pgTxnId;
    private String failReason;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Payment create(UUID orderId, Long memberId, long amount, PaymentMethod method) {
        // 검증 후 PENDING 상태로 생성
        return new Payment(null, orderId, memberId, amount, method,
                PaymentStatus.PENDING, null, null, null, null);
    }

    public static Payment reconstitute(...) { ... }  // DB 복원용

    public void complete(String pgTxnId) {
        if (this.status != PaymentStatus.PENDING)
            throw new InvalidPaymentStatusException(this.status);
        this.status = PaymentStatus.COMPLETED;
        this.pgTxnId = pgTxnId;
    }
}
```

**변경 포인트**:
- JPA 어노테이션 전부 제거 → `PaymentJpaEntity`로 분리
- `@Version` → JpaEntity에만 위치 (도메인 모델에서 제거)
- `create()` / `reconstitute()` 정적 팩토리 메서드로 생성 의도 분리
- Setter 없음, private 생성자 → 상태 전이 메서드(`complete`, `fail`, `cancel`)로만 변경 가능
- PaymentStatus, PaymentMethod enum을 `domain/model/`로 이동

### 4.2 도메인 이벤트 정의

Phase 3(MSA + 이벤트 드리븐) 전환을 대비하여 3종의 도메인 이벤트를 `record`로 정의:

```java
public record PaymentCompletedEvent(
        UUID paymentId, UUID orderId, long amount, String pgTxnId,
        LocalDateTime occurredAt
) implements DomainEvent { ... }

public record PaymentFailedEvent(
        UUID paymentId, UUID orderId, String reason,
        LocalDateTime occurredAt
) implements DomainEvent { ... }

public record PaymentCancelledEvent(
        UUID paymentId, UUID orderId,
        LocalDateTime occurredAt
) implements DomainEvent { ... }
```

- `DomainEvent` 마커 인터페이스 구현 (`common/domain/event/`)
- 현재는 정의만 해두고, Phase 3에서 Kafka 발행 시 활용

### 4.3 PG 연동 Port/Adapter 추상화

**Phase 1** — FakePgClient를 직접 주입
```java
// PaymentService
private final FakePgClient fakePgClient;
PgPaymentResult result = fakePgClient.processPayment(amount, method);
```

**Phase 2** — Port 인터페이스로 추상화
```java
// Port (application/port/out/)
public interface PaymentGatewayPort {
    PgPaymentResult processPayment(long amount, String method);
}

// Adapter (adapter/out/pg/)
@Component
public class FakePgAdapter implements PaymentGatewayPort {
    @Override
    public PgPaymentResult processPayment(long amount, String method) {
        return new PgPaymentResult("PG-" + UUID.randomUUID(), true, null);
    }
}
```

**의미**: 실제 PG사(토스페이먼츠, 이니시스 등) 연동 시 `FakePgAdapter` → `TossPaymentAdapter`로 **Adapter만 교체**하면 된다. Application Service 코드 변경 없음.

### 4.4 Order 의존성 Port 분리 (핵심 변경)

**Phase 1** — PaymentService가 Order를 직접 참조
```java
// PaymentService
private final OrderRepository orderRepository;
private final OrderService orderService;

Order order = orderRepository.findByIdWithOrderLines(orderId).orElseThrow(...);
order.markPaid();
orderService.cancelOrder(memberId, orderId);
```

**Phase 2** — Outbound Port로 추상화

| Port | 메서드 | 역할 |
|------|--------|------|
| `LoadOrderForPaymentPort` | `loadOrder(UUID orderId)` | 주문 정보 조회 (OrderInfoForPayment DTO 반환) |
| `UpdateOrderStatusPort` | `markOrderPaid(UUID)`, `cancelOrder(Long, UUID)` | 주문 상태 변경 |

```java
// Application Service — Port 인터페이스에만 의존
OrderInfoForPayment order = loadOrderForPaymentPort.loadOrder(cmd.orderId());
updateOrderStatusPort.markOrderPaid(cmd.orderId());

// Adapter — 실제 Order 접근은 여기서만
@Component
public class OrderAdapterForPayment implements LoadOrderForPaymentPort, UpdateOrderStatusPort {
    private final OrderRepository orderRepository;  // 모놀리스이므로 직접 참조
    private final OrderService orderService;
    // ...
}
```

**크로스 컨텍스트 DTO**:
```java
public record OrderInfoForPayment(
        UUID orderId, Long memberId, long totalAmount, String status
) {}
```

- Payment Application Service는 `Order` 엔티티를 **전혀 모름**
- `String status`로 받아서 비교 → Order의 `OrderStatus` enum에 대한 의존도 제거
- Phase 3 MSA 전환 시 `OrderAdapterForPayment`를 **Kafka 이벤트 기반 Adapter로 교체**

### 4.5 Port 인터페이스 정의

**Inbound Port** — "외부에서 나를 이렇게 호출해라"

| Port | 메서드 | Command → 반환값 |
|------|--------|-----------------|
| `RequestPaymentUseCase` | `requestPayment(RequestPaymentCommand)` | → `PaymentInfo` |
| `CancelPaymentUseCase` | `cancelPayment(CancelPaymentCommand)` | → `PaymentInfo` |
| `GetPaymentUseCase` | `getPayment(Long, UUID)` | → `PaymentInfo` |

**Outbound Port** — "나는 이런 기능이 필요하다"

| Port | 메서드 | 용도 |
|------|--------|------|
| `LoadPaymentPort` | `findByIdAndMemberId()`, `findByOrderIdExcludingCancelled()` | 결제 조회 |
| `SavePaymentPort` | `save()` | 결제 저장 |
| `PaymentGatewayPort` | `processPayment()` | PG사 결제 처리 |
| `LoadOrderForPaymentPort` | `loadOrder()` | 주문 정보 조회 |
| `UpdateOrderStatusPort` | `markOrderPaid()`, `cancelOrder()` | 주문 상태 변경 |

### 4.6 서비스 분리 (단일 책임)

**Phase 1**: `PaymentService` 1개가 결제 생성 + 조회 + 취소 모두 담당

**Phase 2**: UseCase별로 분리
- `RequestPaymentService` → 주문 검증 → 중복 확인 → Payment.create() → PG 호출 → 상태 전이 → 저장
- `CancelPaymentService` → 결제 조회 → cancel() → 저장 → 주문 취소
- `GetPaymentService` → 결제 조회 → PaymentInfo 변환

### 4.7 Persistence Adapter — save 신규/업데이트 분기

```java
@Override
public Payment save(Payment payment) {
    if (payment.getId() == null) {
        // 신규 생성 — toJpaEntity → repo.save
        PaymentJpaEntity entity = PaymentMapper.toJpaEntity(payment);
        PaymentJpaEntity saved = paymentJpaRepository.save(entity);
        return PaymentMapper.toDomain(saved);
    } else {
        // 업데이트 — 기존 엔티티 조회 → 필드 갱신 (dirty checking)
        PaymentJpaEntity entity = paymentJpaRepository.findById(payment.getId()).orElseThrow();
        entity.updateFrom(payment.getStatus(), payment.getPgTxnId(), payment.getFailReason());
        return PaymentMapper.toDomain(entity);
    }
}
```

- Product와 동일한 패턴: Mapper가 새 도메인 객체를 생성하므로 JPA 영속 컨텍스트와 분리됨
- `updateFrom()`은 **변경 가능한 필드(status, pgTxnId, failReason)만** 갱신

### 4.8 예외 이동

| 예외 | Phase 1 위치 | Phase 2 위치 |
|------|-------------|-------------|
| `InvalidPaymentStatusException` | `common/exception/` | `payment/domain/exception/` |
| `DuplicatePaymentException` | `common/exception/` | `payment/domain/exception/` |
| `PaymentNotFoundException` | `common/exception/` | `payment/domain/exception/` |

- `BusinessException` 상속 유지 — Member/Product 예외 패턴과 일관성
- `InvalidPaymentStatusException`의 import가 `payment.entity.PaymentStatus` → `payment.domain.model.PaymentStatus`로 변경

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
| POST | `/api/payments` | 없음 |
| GET | `/api/payments/{id}` | 없음 |
| POST | `/api/payments/{id}/cancel` | 없음 |

기존 통합 테스트(PaymentIntegrationTest) 10개 케이스 모두 통과.
전체 43개 테스트 PASSED (1개 SKIPPED는 기존과 동일).

---

## 7. 발견 및 해결된 이슈

### 7.1 Order 상태 검증 시 enum 의존 제거

- **증상**: 기존 `order.getStatus() != OrderStatus.PLACED`로 비교 → Payment가 Order의 `OrderStatus` enum에 의존
- **해결**: `OrderInfoForPayment`에서 `String status`로 반환, `"PLACED".equals(order.status())`로 문자열 비교
- **트레이드오프**: 타입 안전성은 떨어지지만, 컨텍스트 간 결합도를 완전히 제거

### 7.2 Order 상태 검증 예외 처리

- **증상**: 기존 `InvalidOrderStatusException(OrderStatus currentStatus)`는 `OrderStatus` enum을 인자로 받음
- **해결**: `BusinessException`을 직접 생성하여 동일한 HTTP 400 응답 유지, Order enum 의존 제거

### 7.3 통합 테스트 import 경로 변경

- **증상**: `PaymentIntegrationTest`에서 `PaymentRepository`, `CreatePaymentRequest` import 실패
- **해결**: `PaymentJpaRepository`, `adapter.in.web.dto.CreatePaymentRequest`로 경로 변경

---

## 8. Member/Product vs Payment 전환 비교

| 항목 | Member (2단계) | Product (3단계) | Payment (4단계) |
|------|---------------|-----------------|-----------------|
| Value Object | 1개 (Email) | 2개 (Money, Stock) | 0개 |
| 동시성 제어 | 없음 | 비관적 락 + 낙관적 락 | 낙관적 락 |
| 외부 시스템 연동 | Security (BCrypt, JWT) | 없음 | PG사 (FakePgAdapter) |
| 크로스 컨텍스트 | 없음 | OrderService 임시 수정 | Order Port 추상화 |
| Outbound Adapter 수 | 2개 (persistence, security) | 1개 (persistence) | 3개 (persistence, pg, order) |
| 도메인 이벤트 | 0개 | 0개 | 3개 |
| 서비스 분리 | 3개 | 3개 | 3개 |

**핵심 차이**: Payment는 **외부 PG 연동 추상화**와 **Order 컨텍스트 의존성을 Port로 분리**하는 것이 핵심 과제. 3종류의 Outbound Adapter(persistence, pg, order)를 갖는 유일한 컨텍스트.

---

## 9. Phase 3 전환 시 변경 포인트

Payment 헥사고날 전환으로 Phase 3(MSA + 이벤트 드리븐) 전환 시 변경이 필요한 지점이 명확해졌다:

| 현재 Adapter | Phase 3 변경 | 이유 |
|-------------|-------------|------|
| `FakePgAdapter` | 실제 PG Adapter 또는 유지 | PG 연동 시 Adapter만 교체 |
| `OrderAdapterForPayment` | Kafka 이벤트 기반 Adapter | 서비스 분리로 직접 호출 불가 |
| `PaymentPersistenceAdapter` | 독립 DB 연결 Adapter | 서비스별 DB 분리 |

**도메인 이벤트 활용**: `PaymentCompletedEvent`, `PaymentFailedEvent`, `PaymentCancelledEvent`를 Kafka로 발행하여 Order/Product 서비스에 알림

---

## 10. 트레이드오프

| 장점 | 비용 |
|------|------|
| PG 추상화 — Adapter만 교체로 PG사 변경 가능 | 파일 수 +18개 |
| Order 의존 Port 분리 — MSA 전환 준비 완료 | 크로스 컨텍스트 DTO(OrderInfoForPayment) 추가 |
| 도메인 이벤트 선제 정의 — Phase 3 즉시 활용 | 현재는 미사용 (정의만) |
| 상태 검증 문자열 비교 — Order enum 의존 제거 | 타입 안전성 약간 저하 |
| 3종 Adapter 분리 — 각각 독립 교체 가능 | Adapter 코드량 증가 |

> ROADMAP이 경고한 **"Port 뒤로 숨겨졌을 뿐 동기 호출의 장애 전파"** 문제는 여전히 존재한다. `RequestPaymentService` → `PaymentGatewayPort.processPayment()`에서 PG 3초 지연 시 주문 API 전체에 영향. 이 문제는 Phase 3에서 Kafka 비동기 전환으로 해결할 예정.
