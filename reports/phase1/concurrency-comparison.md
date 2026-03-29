# 동시성 전략 비교: 비관적 락 vs 낙관적 락

> Phase 1 - 6단계: 동시성 문제 재현 및 해결

## 1. 개요

주문 플랫폼에서 동일 상품에 동시 주문이 들어올 때 **재고 정합성**이 깨지는 문제를 발견하고,
비관적 락과 낙관적 락 두 가지 전략을 적용하여 해결한 과정을 기록한다.

### 환경
- Spring Boot 3.5.0 + JPA (Hibernate) + PostgreSQL 16
- TestContainers 기반 통합 테스트
- 100개 스레드 동시 요청 (ExecutorService + CountDownLatch)

---

## 2. 문제 재현: 락 없는 상태의 Race Condition

### 원인

`ProductService.decreaseStock()`에서 일반 `findById()`로 상품을 조회하면,
여러 트랜잭션이 **동시에 같은 재고 값**을 읽고 각각 차감하여 **Lost Update**가 발생한다.

```
스레드 A: read stock=100 → check(100>=1) → stock=99 → write 99
스레드 B: read stock=100 → check(100>=1) → stock=99 → write 99  ← 덮어쓰기!
결과: 2개 차감했지만 stock=99 (1개만 반영)
```

### 테스트 결과

| 항목 | 값 |
|------|-----|
| 총 요청 | 100개 (각 1개씩 차감) |
| 초기 재고 | 100개 |
| 성공 건수 | 100 (재고 체크를 모두 통과) |
| **실제 남은 재고** | **91** (기대값 0) |
| 정합성 | **깨짐 - Lost Update 발생** |

100개 요청이 모두 성공했지만, 실제로는 9개만 차감되었다.
나머지 91개의 차감이 덮어쓰기로 유실되었다.

---

## 3. 비관적 락 (Pessimistic Lock)

### 원리

```sql
SELECT * FROM products WHERE id = ? FOR UPDATE
```

조회 시점에 **DB 행 잠금**을 획득한다. 다른 트랜잭션이 같은 행을 조회하려 하면
락이 해제될 때까지 **대기**한 후, 갱신된 데이터를 읽는다.

### 구현

```java
// ProductRepository
@Lock(LockModeType.PESSIMISTIC_WRITE)
@Query("SELECT p FROM Product p WHERE p.id = :id")
Optional<Product> findByIdForUpdate(@Param("id") Long id);

// ProductService
@Transactional
public void decreaseStock(Long productId, int quantity) {
    Product product = productRepository.findByIdForUpdate(productId)  // 락 획득
            .orElseThrow(() -> new ProductNotFoundException(productId));
    product.decreaseStock(quantity);
}
```

### 동작 흐름

```
스레드 A: 락 획득 → read stock=100 → stock=99 → COMMIT → 락 해제
스레드 B:          ⏳ 대기...                            → 락 획득 → read stock=99 → stock=98 → COMMIT
```

### 테스트 결과

| 항목 | 값 |
|------|-----|
| 총 요청 | 100개 |
| 성공 건수 | **100** |
| 실패 건수 | 0 |
| 재시도 횟수 | **0회** |
| 최종 재고 | **0** (정확) |
| 소요 시간 | **256ms** |
| 정합성 | **보장** |

### 장점
- 충돌 시 대기하므로 **재시도 로직이 불필요**
- 데이터 정합성이 **확실하게** 보장됨
- 구현이 단순 (`@Lock` 어노테이션 한 줄)

### 단점
- 락 대기로 인한 **처리량 제한** (직렬화)
- 여러 행을 동시에 잠글 때 **데드락 가능성**
- 트랜잭션이 길어지면 다른 요청의 대기 시간 증가

---

## 4. 낙관적 락 (Optimistic Lock)

### 원리

엔티티에 `@Version` 필드를 추가하면, UPDATE 시 JPA가 자동으로 버전을 검증한다.

```sql
UPDATE products SET stock=99, version=1 WHERE id=? AND version=0
-- 영향 행 = 0이면 → ObjectOptimisticLockingFailureException 발생
```

### 구현

```java
// Product 엔티티
@Version
private Long version;

// ProductService (비교용)
@Transactional
public void decreaseStockWithOptimisticLock(Long productId, int quantity) {
    Product product = productRepository.findById(productId)  // 락 없이 조회
            .orElseThrow(() -> new ProductNotFoundException(productId));
    product.decreaseStock(quantity);  // 커밋 시 @Version으로 충돌 감지
}
```

### 동작 흐름

```
스레드 A: read(v=0) → stock=99 → COMMIT(v=0→1) ✅ 성공
스레드 B: read(v=0) → stock=99 → COMMIT(v=0→?) ❌ 충돌 → 재시도 → read(v=1) → COMMIT(v=1→2) ✅
```

### 테스트 결과 (재시도 포함)

| 항목 | 값 |
|------|-----|
| 총 요청 | 100개 |
| 성공 건수 | **100** |
| 실패 건수 | 0 |
| 재시도 횟수 | **473회** |
| 최종 재고 | **0** (정확) |
| 소요 시간 | **778ms** |
| 정합성 | **보장** (재시도 전제) |

### Order 엔티티 낙관적 락 테스트

| 항목 | 값 |
|------|-----|
| 동시 cancel 요청 | 10개 |
| 성공 | 1개 |
| @Version 충돌 | 9개 (StaleObjectStateException) |
| 최종 상태 | CANCELLED (정확히 1회만 반영) |

### 장점
- 조회 시 락을 잡지 않으므로 **읽기 성능 우수**
- **데드락 위험 없음**
- 충돌이 드문 경우 락 오버헤드 없이 빠르게 처리

### 단점
- 충돌 시 **재시도 로직을 직접 구현**해야 함
- 충돌률이 높으면 재시도 폭증 → **성능 급락** (473회 재시도 발생)
- 재시도 횟수 제한 시 **요청 실패 가능성**

---

## 5. 성능 비교 요약

### 재고 차감 시나리오 (100개 동시 요청, 고충돌)

| 지표 | 비관적 락 | 낙관적 락 | 차이 |
|------|-----------|-----------|------|
| 성공률 | 100% | 100% | 동일 |
| 재시도 | 0회 | **473회** | 낙관적 락 473회 추가 |
| 소요 시간 | **256ms** | 778ms | 낙관적 락 **3배 느림** |
| 정합성 | 보장 | 보장 (재시도 전제) | 동일 |
| 구현 복잡도 | 낮음 | 높음 (재시도 로직) | 비관적 락 유리 |

### 처리 방식 차이

```
비관적 락: ████████████████████ 256ms (순차 대기, 재시도 0)
낙관적 락: ██████████████████████████████████████████████████████████ 778ms (재시도 473회)
```

---

## 6. 전략 선택 기준

### 최종 결정

| 대상 | 전략 | 근거 |
|------|------|------|
| **상품 재고 차감** | 비관적 락 (`SELECT FOR UPDATE`) | 동일 상품에 동시 주문 집중 → 충돌 빈도 높음 |
| **주문 상태 변경** | 낙관적 락 (`@Version`) | 주문별 독립적 → 충돌 빈도 낮음 |

### 선택 판단 기준

```
충돌 빈도를 기준으로 판단한다:

충돌 빈도 높음 (핫스팟) → 비관적 락
├── 재고 차감 (인기 상품에 주문 집중)
├── 좌석 예약 (같은 좌석에 동시 요청)
└── 포인트/잔액 차감 (동일 계정)

충돌 빈도 낮음 → 낙관적 락
├── 주문 상태 변경 (주문 ID별 독립)
├── 사용자 프로필 수정
└── 게시글 수정
```

### 왜 재고에 비관적 락을 선택했는가?

1. **충돌 확실성**: 인기 상품은 동시 주문이 같은 행에 집중되므로 충돌이 거의 확실하다
2. **재시도 비용**: 낙관적 락은 473회 재시도 + 3배 느린 응답 → 사용자 경험 저하
3. **구현 단순성**: `@Lock` 한 줄 vs 재시도 루프 + 예외 처리 + 최대 재시도 제한
4. **안정성**: 비관적 락은 재시도 없이 100% 성공, 낙관적 락은 재시도 초과 시 실패 가능

### 왜 주문 상태에 낙관적 락을 선택했는가?

1. **충돌 희소성**: 같은 주문을 동시에 변경하는 경우는 드묾
2. **읽기 성능**: 주문 조회가 잦으므로 락 없는 조회가 유리
3. **데드락 방지**: 주문은 여러 엔티티를 함께 수정할 수 있어 비관적 락 시 데드락 위험

---

## 7. Phase 2+ 전환 시 고려사항

### 헥사고날 아키텍처 (Phase 2)
- `ProductRepository`의 `findByIdForUpdate()`는 포트 인터페이스로 추상화
- 락 전략은 어댑터(인프라) 계층에서 결정 → 도메인 순수성 유지

### MSA 전환 (Phase 3)
- 서비스별 DB 분리 → 단일 DB 비관적 락은 그대로 유효
- 서비스 간 재고 차감은 **이벤트 기반** + **멱등성**으로 전환
- 분산 환경에서는 Redis 분산 락 또는 DB 비관적 락 + Outbox 패턴 고려

### Saga 패턴 (Phase 4)
- 비관적 락은 로컬 트랜잭션 내에서 계속 사용
- 보상 트랜잭션(재고 복원)에서도 `findByIdForUpdate()`로 정합성 보장
