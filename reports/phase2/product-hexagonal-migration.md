# Product 컨텍스트 헥사고날 아키텍처 전환 보고서

> Phase 2 — 3단계 (2026-03-30)

---

## 1. 전환 개요

레이어드 아키텍처(Phase 1)의 Product 코드를 헥사고날(Ports & Adapters) 아키텍처로 전환하여, 도메인 로직을 인프라로부터 완전 분리했다. Member 컨텍스트(2단계) 전환 패턴을 따르되, Product 고유의 **동시성 제어(비관적/낙관적 락)** 캡슐화와 **Value Object(Money, Stock)** 도입이 핵심 차별점이다.

---

## 2. 구조 변화

### Before — 레이어드 아키텍처 (Phase 1)

```
product/
├── controller/ProductController.java     ← REST 엔드포인트
├── service/ProductService.java           ← 모든 비즈니스 로직 + 재고 관리
├── repository/ProductRepository.java     ← Spring Data JPA + 비관적 락 쿼리
├── entity/Product.java                   ← JPA Entity = 도메인 모델
└── dto/
    ├── CreateProductRequest.java
    └── ProductResponse.java
```

**의존 흐름**: `Controller → Service → Repository → JPA Entity`

- Entity가 곧 도메인 모델 — `@Entity`, `@Version`, 비즈니스 메서드가 혼재
- 비관적 락(`findByIdForUpdate`)이 Repository에 직접 노출
- `price`는 `long`, `stock`은 `int` — 원시 타입으로 비즈니스 규칙 분산

### After — 헥사고날 아키텍처 (Phase 2)

```
product/
├── domain/                              ← 순수 Java (외부 의존 0)
│   ├── model/
│   │   ├── Product.java                 ← 순수 도메인 모델
│   │   ├── Money.java                   ← Value Object (record)
│   │   └── Stock.java                   ← Value Object (record)
│   ├── exception/
│   │   ├── ProductNotFoundException.java
│   │   └── InsufficientStockException.java
│   └── event/                           ← Phase 3 이벤트 대비
├── application/
│   ├── port/in/                         ← Inbound Port (UseCase)
│   │   ├── CreateProductUseCase.java
│   │   ├── GetProductUseCase.java
│   │   ├── UpdateStockUseCase.java
│   │   ├── CreateProductCommand.java
│   │   └── ProductInfo.java
│   ├── port/out/                        ← Outbound Port (인프라 추상화)
│   │   ├── LoadProductPort.java
│   │   └── SaveProductPort.java
│   └── service/                         ← UseCase 구현체
│       ├── CreateProductService.java
│       ├── GetProductService.java
│       └── UpdateStockService.java
└── adapter/
    ├── in/web/                          ← Inbound Adapter
    │   ├── ProductController.java
    │   └── dto/
    │       ├── CreateProductRequest.java
    │       └── ProductResponse.java
    └── out/persistence/                 ← Outbound Adapter
        ├── ProductJpaEntity.java        ← @Entity + @Version
        ├── ProductJpaRepository.java    ← findByIdForUpdate(@Lock)
        ├── ProductMapper.java           ← Money↔long, Stock↔int 변환
        └── ProductPersistenceAdapter.java
```

**의존 흐름**:
```
Controller → UseCase(interface) → Service → Port(interface) ← Adapter(구현체)
                                     ↓
                              Domain Model (순수 Java)
                              ├── Money (Value Object)
                              └── Stock (Value Object)
```

---

## 3. 정량 비교

| 항목 | Phase 1 | Phase 2 | 변화 |
|------|---------|---------|------|
| 파일 수 | 6개 | 22개 | +16개 |
| 총 코드 라인 | ~228줄 | ~639줄 | +411줄 |
| 인터페이스 수 | 1개 (Repository) | 6개 (UseCase 3 + Port 2 + Repository) | +5개 |
| Value Object | 0개 | 2개 (Money, Stock) | +2개 |
| 도메인 외부 의존 | JPA, Lombok | **없음** | 완전 분리 |
| 서비스 클래스 | 1개 (ProductService) | 3개 (UseCase별 분리) | +2개 |

---

## 4. 핵심 변경사항

### 4.1 도메인 순수화

**Phase 1** — JPA Entity가 곧 도메인 모델
```java
@Entity @Table(name = "products")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Product extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(nullable = false)
    private long price;          // 원시 타입
    @Column(nullable = false)
    private int stock;           // 원시 타입
    @Version
    private Long version;

    public void decreaseStock(int quantity) { ... }
    public void restoreStock(int quantity) { ... }
}
```

**Phase 2** — 순수 Java 도메인 모델 (JPA/Lombok 의존 없음)
```java
public class Product {
    private final Long id;
    private final String name;
    private final Money price;    // Value Object
    private Stock stock;          // Value Object (재할당 가능)
    private final String category;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Product create(String name, Money price, Stock stock, String category) {
        return new Product(null, name, price, stock, category, null, null);
    }

    public static Product reconstitute(Long id, String name, Money price, Stock stock,
                                       String category, LocalDateTime createdAt,
                                       LocalDateTime updatedAt) { ... }

    public void decreaseStock(int quantity) {
        this.stock = this.stock.decrease(quantity);   // Stock VO에 위임
    }
}
```

**변경 포인트**:
- JPA 어노테이션 전부 제거 → `ProductJpaEntity`로 분리
- `long price` → `Money` VO, `int stock` → `Stock` VO
- `@Version` → JpaEntity에만 위치 (도메인 모델에서 제거)
- `create()` / `reconstitute()` 정적 팩토리 메서드로 생성 의도 분리
- Setter 없음, private 생성자 → 불변 보장 (stock 필드만 재할당)

### 4.2 Value Object 도입

**Money** — 금액을 표현하는 불변 객체
```java
public record Money(long amount) {
    public static final Money ZERO = new Money(0);

    public Money {
        if (amount < 0)
            throw new IllegalArgumentException("금액은 음수일 수 없습니다.");
    }

    public Money multiply(int quantity) {
        return new Money(this.amount * quantity);
    }
}
```

**Stock** — 재고를 표현하는 불변 객체
```java
public record Stock(int quantity, int threshold) {
    public Stock {
        if (quantity < 0)  throw new IllegalArgumentException("재고 수량은 음수일 수 없습니다.");
        if (threshold < 0) throw new IllegalArgumentException("임계값은 음수일 수 없습니다.");
    }

    public Stock decrease(int qty) {
        if (qty <= 0) throw new IllegalArgumentException("차감 수량은 양수여야 합니다.");
        if (this.quantity < qty) throw new InsufficientStockException(this.quantity, qty);
        return new Stock(this.quantity - qty, this.threshold);
    }

    public Stock increase(int qty) { ... }
    public boolean isLowStock() { return this.quantity <= this.threshold; }
}
```

- **비즈니스 규칙이 VO 안에 내재화**: 재고 부족 검증이 `Stock.decrease()`에 캡슐화
- `threshold`: 재고 임계값 (Phase 3+ 알림 기능 대비)
- 불변 — `decrease()`는 새 Stock을 반환, 기존 객체 변경 없음

### 4.3 동시성 제어의 Adapter 캡슐화

**비관적 락 — Adapter 내부에 캡슐화**

```java
// Port — 인프라 기술을 모름, 의도만 명시
public interface LoadProductPort {
    Optional<Product> findById(Long id);
    Optional<Product> findByIdForUpdate(Long id);   // "업데이트 위한 조회"라는 의도
}

// Adapter — 실제 비관적 락 구현
public interface ProductJpaRepository extends JpaRepository<ProductJpaEntity, Long> {
    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT p FROM ProductJpaEntity p WHERE p.id = :id")
    Optional<ProductJpaEntity> findByIdForUpdate(@Param("id") Long id);
}
```

**낙관적 락 — JpaEntity에만 @Version**

```java
@Entity
public class ProductJpaEntity extends BaseEntity {
    @Version
    private Long version;   // 도메인 모델에는 없음, JPA 레벨에서만 관리
}
```

| 락 전략 | Phase 1 위치 | Phase 2 위치 |
|---------|-------------|-------------|
| 비관적 락 (`SELECT FOR UPDATE`) | `ProductRepository` | `ProductJpaRepository` (Adapter) |
| 낙관적 락 (`@Version`) | `Product` Entity | `ProductJpaEntity` (Adapter) |

→ 도메인 모델은 **락 전략을 전혀 모름**. Adapter를 교체하면 락 전략도 변경 가능.

### 4.4 Port 인터페이스 정의

**Inbound Port** — "외부에서 나를 이렇게 호출해라"

| Port | 메서드 | Command → 반환값 |
|------|--------|-----------------|
| `CreateProductUseCase` | `createProduct(CreateProductCommand)` | → `ProductInfo` |
| `GetProductUseCase` | `getProduct(Long)`, `getProducts()` | → `ProductInfo` / `List<ProductInfo>` |
| `UpdateStockUseCase` | `decreaseStock(Long, int)`, `decreaseStockWithOptimisticLock(Long, int)` | → `void` |

**Outbound Port** — "나는 이런 기능이 필요하다"

| Port | 메서드 |
|------|--------|
| `LoadProductPort` | `findById()`, `findAll()`, `findByIdForUpdate()` |
| `SaveProductPort` | `save()` |

### 4.5 서비스 분리 (단일 책임)

**Phase 1**: `ProductService` 1개가 상품 생성 + 조회 + 재고 차감 모두 담당

**Phase 2**: UseCase별로 분리
- `CreateProductService` → `Product.create()` → 저장
- `GetProductService` → ID/전체 조회 → ProductInfo 변환
- `UpdateStockService` → 비관적 락 조회 → `product.decreaseStock()` → 저장

### 4.6 Persistence Adapter — save 신규/업데이트 분기

```java
@Override
public Product save(Product product) {
    if (product.getId() == null) {
        // 신규 생성 — toJpaEntity → repo.save
        ProductJpaEntity entity = ProductMapper.toJpaEntity(product);
        ProductJpaEntity saved = productJpaRepository.save(entity);
        return ProductMapper.toDomain(saved);
    } else {
        // 업데이트 — 기존 엔티티 조회 → 필드 갱신 (dirty checking)
        ProductJpaEntity entity = productJpaRepository.findById(product.getId()).orElseThrow();
        entity.updateFrom(product.getName(), product.getPrice().amount(),
                          product.getStock().quantity(), product.getStock().threshold(),
                          product.getCategory());
        return ProductMapper.toDomain(entity);
    }
}
```

- 헥사고날에서 조회한 도메인 객체는 JPA 영속성 컨텍스트와 분리됨 (Mapper가 새 객체 생성)
- 따라서 dirty checking이 작동하지 않으므로, **명시적 save + 기존 엔티티 업데이트** 필요
- Member와 달리 Product는 재고 차감 후 **업데이트 save가 필수**이므로 이 분기가 추가됨

### 4.7 Mapper — Money↔long, Stock↔int 변환

```java
public final class ProductMapper {
    static ProductJpaEntity toJpaEntity(Product product) {
        return new ProductJpaEntity(
                product.getName(),
                product.getPrice().amount(),         // Money → long
                product.getStock().quantity(),        // Stock → int
                product.getStock().threshold(),       // Stock → int
                product.getCategory()
        );
    }

    static Product toDomain(ProductJpaEntity entity) {
        return Product.reconstitute(
                entity.getId(), entity.getName(),
                new Money(entity.getPrice()),         // long → Money
                new Stock(entity.getStock(), entity.getStockThreshold()), // int → Stock
                entity.getCategory(),
                entity.getCreatedAt(), entity.getUpdatedAt()
        );
    }
}
```

---

## 5. 크로스 컨텍스트 의존성 처리

### OrderService → Product 참조 임시 수정

OrderService가 기존 `Product` Entity와 `ProductRepository`를 직접 참조하고 있었으므로, Product 전환 시 빌드가 깨짐.

**임시 해결**: OrderService가 `ProductJpaEntity` + `ProductJpaRepository`를 직접 참조하도록 변경

```java
// Before
import com.orderplatform.product.entity.Product;
import com.orderplatform.product.repository.ProductRepository;

// After (임시)
import com.orderplatform.product.adapter.out.persistence.ProductJpaEntity;
import com.orderplatform.product.adapter.out.persistence.ProductJpaRepository;
```

**ProductJpaEntity에 임시 비즈니스 메서드 유지**: `decreaseStock()`, `restoreStock()` — OrderService가 직접 호출

**해소 시점**: Order 컨텍스트 전환(5단계)에서 `DecreaseStockPort`/`RestoreStockPort`로 완전 교체 예정

### 예외 이동

| 예외 | Phase 1 위치 | Phase 2 위치 |
|------|-------------|-------------|
| `ProductNotFoundException` | `common/exception/` | `product/domain/exception/` |
| `InsufficientStockException` | `common/exception/` | `product/domain/exception/` |

- OrderService, 테스트 파일의 import도 새 경로로 변경
- `BusinessException` 상속 유지 — Member 예외 패턴과 일관성

---

## 6. 의존성 규칙 검증 (ArchUnit)

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

## 7. API 호환성

기존 API 엔드포인트와 응답 형식을 **100% 유지**.

| Method | Endpoint | 변화 |
|--------|----------|------|
| POST | `/api/products` | 없음 |
| GET | `/api/products` | 없음 |
| GET | `/api/products/{id}` | 없음 |

기존 통합 테스트(ProductIntegrationTest, ProductConcurrencyTest) 모두 통과.
Order/Payment 통합 테스트도 import 변경 후 정상 통과.

---

## 8. 발견 및 해결된 이슈

### 8.1 Persistence Adapter save 시 dirty checking 미작동

- **증상**: `UpdateStockService`에서 재고 차감 후 save 호출 시 DB에 반영되지 않음
- **원인**: `LoadProductPort.findByIdForUpdate()` → Mapper가 새 도메인 객체 생성 → JPA 영속 컨텍스트와 분리
- **해결**: `save()` 메서드에서 id 유무로 분기 — id가 있으면 기존 엔티티 조회 후 `updateFrom()`으로 필드 갱신

### 8.2 Order/Payment 테스트의 Product 참조

- **증상**: `./gradlew build` 실패 — `OrderIntegrationTest`, `OrderConcurrencyTest`, `PaymentIntegrationTest`에서 컴파일 에러
- **원인**: 이들 테스트도 기존 `product.entity.Product` + `product.repository.ProductRepository`를 직접 참조
- **해결**: 3개 테스트 파일의 import/타입을 `ProductJpaEntity` + `ProductJpaRepository`로 변경, 생성자에 `stockThreshold=0` 인수 추가

### 8.3 ProductJpaEntity 생성자 인수 변경

- **증상**: 기존 4개 인수(`name, price, stock, category`) → 5개 인수(`+ stockThreshold`)
- **원인**: `Stock` VO에 `threshold` 필드 추가로 인해 JpaEntity에도 `stockThreshold` 컬럼 추가
- **해결**: 모든 테스트의 `new ProductJpaEntity(...)` 호출에 `stockThreshold=0` 인수 추가

---

## 9. Member vs Product 전환 비교

| 항목 | Member (2단계) | Product (3단계) |
|------|---------------|-----------------|
| Value Object | 1개 (Email) | 2개 (Money, Stock) |
| 동시성 제어 | 없음 | 비관적 락 + 낙관적 락 |
| Adapter save | 신규 생성만 | 신규 + 업데이트 분기 |
| 크로스 컨텍스트 영향 | 없음 | OrderService 임시 수정 필요 |
| 테스트 영향 | Product 테스트만 | Order/Payment 테스트도 수정 |
| Outbound Adapter | persistence + security | persistence만 |

**핵심 차이**: Product는 **동시성 제어를 Adapter에 캡슐화**하는 것이 핵심 과제. Port 인터페이스(`findByIdForUpdate`)로 의도를 표현하되, 실제 락 전략(`@Lock`, `@Version`)은 Adapter에서 결정.

---

## 10. 트레이드오프

| 장점 | 비용 |
|------|------|
| 도메인 순수성 — 락 전략을 도메인이 모름 | 파일 수 +16개, 코드량 +411줄 |
| Money/Stock VO — 비즈니스 규칙 내재화 | Mapper에서 VO↔primitive 변환 보일러플레이트 |
| Port 추상화 — JPA 교체 시 도메인 무변경 | save 신규/업데이트 분기 로직 추가 |
| 단일 책임 서비스 — 변경 영향 최소 | 크로스 컨텍스트 임시 수정 필요 |
| threshold 선제 대비 — Phase 3+ 활용 가능 | 현재는 미사용 (기본값 0) |

> Phase 3(MSA 전환)에서 동시성 제어의 변화가 핵심이 된다. 현재 비관적 락으로 단일 DB에서 재고 정합성을 보장하지만, 서비스별 DB 분리 시 분산 락 또는 이벤트 기반 재고 관리로 전환해야 한다. 이때 **Adapter만 교체**하면 된다.
