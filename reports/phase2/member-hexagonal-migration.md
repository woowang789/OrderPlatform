# Member 컨텍스트 헥사고날 아키텍처 전환 보고서

> Phase 2 — 2단계 (2026-03-30)

---

## 1. 전환 개요

레이어드 아키텍처(Phase 1)의 Member 코드를 헥사고날(Ports & Adapters) 아키텍처로 전환하여, 도메인 로직을 인프라로부터 완전 분리하고 테스트 용이성과 확장성을 확보했다.

**커밋 이력**

| 커밋 | 메시지 |
|------|--------|
| `1390bc8` | `[Phase 2] refactor: Member 컨텍스트 헥사고날 아키텍처 전환` |
| `c9d6804` | `[Phase 2] fix: .gitignore out/ 규칙 수정 및 누락된 Adapter/Port 파일 추가` |

---

## 2. 구조 변화

### Before — 레이어드 아키텍처 (Phase 1)

```
member/
├── controller/MemberController.java     ← REST 엔드포인트
├── service/MemberService.java           ← 모든 비즈니스 로직
├── repository/MemberRepository.java     ← Spring Data JPA
├── entity/Member.java                   ← JPA Entity = 도메인 모델
└── dto/                                 ← 요청/응답 DTO
    ├── SignupRequest.java
    ├── LoginRequest.java
    ├── LoginResponse.java
    └── MemberResponse.java
```

**의존 흐름**: `Controller → Service → Repository → JPA Entity`

- Service가 JPA, Spring Security, JWT에 **직접 의존**
- 도메인 모델(Member)이 JPA Entity 그 자체
- 기술 변경 시 Service 코드 수정 불가피

### After — 헥사고날 아키텍처 (Phase 2)

```
member/
├── domain/                              ← 순수 Java (외부 의존 0)
│   ├── model/
│   │   ├── Member.java                  ← 순수 도메인 모델
│   │   └── Email.java                   ← Value Object (record)
│   ├── exception/
│   │   ├── DuplicateEmailException.java
│   │   ├── MemberNotFoundException.java
│   │   └── AuthenticationFailedException.java
│   └── event/                           ← Phase 3 이벤트 대비
├── application/
│   ├── port/in/                         ← Inbound Port (UseCase)
│   │   ├── SignUpUseCase.java
│   │   ├── LoginUseCase.java
│   │   ├── GetMemberUseCase.java
│   │   ├── SignUpCommand.java
│   │   ├── LoginCommand.java
│   │   └── MemberInfo.java
│   ├── port/out/                        ← Outbound Port (인프라 추상화)
│   │   ├── LoadMemberPort.java
│   │   ├── SaveMemberPort.java
│   │   ├── PasswordEncoderPort.java
│   │   └── TokenGeneratorPort.java
│   └── service/                         ← UseCase 구현체
│       ├── SignUpService.java
│       ├── LoginService.java
│       └── GetMemberService.java
└── adapter/
    ├── in/web/                          ← Inbound Adapter
    │   ├── MemberController.java
    │   └── dto/
    │       ├── SignupRequest.java
    │       ├── LoginRequest.java
    │       ├── LoginResponse.java
    │       └── MemberResponse.java
    └── out/                             ← Outbound Adapter
        ├── persistence/
        │   ├── MemberJpaEntity.java
        │   ├── MemberJpaRepository.java
        │   ├── MemberMapper.java
        │   └── MemberPersistenceAdapter.java
        └── security/
            ├── BcryptPasswordEncoderAdapter.java
            └── JwtTokenGeneratorAdapter.java
```

**의존 흐름**:
```
Controller → UseCase(interface) → Service → Port(interface) ← Adapter(구현체)
                                     ↓
                              Domain Model (순수 Java)
```

---

## 3. 정량 비교

| 항목 | Phase 1 | Phase 2 | 변화 |
|------|---------|---------|------|
| 파일 수 | 9개 | 25개 | +16개 |
| 총 코드 라인 | ~180줄 | ~500줄 | +320줄 |
| 인터페이스 수 | 1개 (Repository) | 7개 (UseCase 3 + Port 4) | +6개 |
| 도메인 외부 의존 | JPA, Lombok | **없음** | 완전 분리 |
| 서비스 클래스 | 1개 (MemberService) | 3개 (UseCase별 분리) | +2개 |

---

## 4. 핵심 변경사항

### 4.1 도메인 순수화

**Phase 1** — JPA Entity가 곧 도메인 모델
```java
@Entity @Table(name = "members")
@Getter @NoArgsConstructor(access = AccessLevel.PROTECTED)
public class Member extends BaseEntity {
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    @Column(unique = true, nullable = false)
    private String email;
    private String password;
    private String name;
}
```

**Phase 2** — 순수 Java 도메인 모델 (JPA/Lombok 의존 없음)
```java
public class Member {
    private final Long id;
    private final Email email;       // Value Object
    private final String password;
    private final String name;
    private final LocalDateTime createdAt;
    private final LocalDateTime updatedAt;

    public static Member create(Email email, String encodedPassword, String name) {
        return new Member(null, email, encodedPassword, name, null, null);
    }

    public static Member reconstitute(Long id, Email email, String password,
                                      String name, LocalDateTime createdAt,
                                      LocalDateTime updatedAt) {
        return new Member(id, email, password, name, createdAt, updatedAt);
    }
}
```

**변경 포인트**:
- JPA 어노테이션 전부 제거 → `MemberJpaEntity`로 분리
- `String email` → `Email` Value Object (검증 내재화)
- `create()` / `reconstitute()` 정적 팩토리 메서드로 생성 의도 분리
- Setter 없음, private 생성자 → 불변 보장

### 4.2 Email Value Object 도입

```java
public record Email(String value) {
    private static final Pattern EMAIL_PATTERN =
            Pattern.compile("^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+$");

    public Email {
        if (value == null || value.isBlank())
            throw new IllegalArgumentException("이메일은 필수입니다.");
        if (!EMAIL_PATTERN.matcher(value).matches())
            throw new IllegalArgumentException("올바른 이메일 형식이 아닙니다.");
    }
}
```

- 이메일 형식 검증이 도메인 레이어에 내재화
- record 기반 불변 객체
- 도메인 내부에서만 사용, Command/DTO에서는 String → Service에서 변환

### 4.3 Port 인터페이스 정의

**Inbound Port** — "외부에서 나를 이렇게 호출해라"

| Port | 메서드 | Command → 반환값 |
|------|--------|-----------------|
| `SignUpUseCase` | `signUp(SignUpCommand)` | → `MemberInfo` |
| `LoginUseCase` | `login(LoginCommand)` | → `String` (token) |
| `GetMemberUseCase` | `getMember(Long)` | → `MemberInfo` |

**Outbound Port** — "나는 이런 기능이 필요하다"

| Port | 메서드 |
|------|--------|
| `LoadMemberPort` | `findById()`, `findByEmail()`, `existsByEmail()` |
| `SaveMemberPort` | `save()` |
| `PasswordEncoderPort` | `encode()`, `matches()` |
| `TokenGeneratorPort` | `generateToken()` |

### 4.4 서비스 분리 (단일 책임)

**Phase 1**: `MemberService` 1개가 회원가입 + 로그인 + 조회 모두 담당

**Phase 2**: UseCase별로 분리
- `SignUpService` → 이메일 중복 확인 → 비밀번호 인코딩 → 회원 생성 → 저장
- `LoginService` → 이메일로 조회 → 비밀번호 검증 → 토큰 생성
- `GetMemberService` → ID로 조회 → MemberInfo 반환

각 서비스는 **Outbound Port 인터페이스에만 의존** (구현체를 모름).

### 4.5 Adapter 구현

**Persistence Adapter** — JPA 기술을 캡슐화
```java
@SuppressWarnings("null")
@Component
public class MemberPersistenceAdapter implements LoadMemberPort, SaveMemberPort {
    private final MemberJpaRepository memberJpaRepository;
    // Domain Member ↔ MemberJpaEntity 변환은 MemberMapper가 담당
}
```

**Security Adapter** — Spring Security, JWT를 캡슐화
```java
@Component
public class BcryptPasswordEncoderAdapter implements PasswordEncoderPort {
    private final PasswordEncoder passwordEncoder;  // Spring Bean 래핑
}

@Component
public class JwtTokenGeneratorAdapter implements TokenGeneratorPort {
    private final JwtTokenProvider jwtTokenProvider;  // 기존 Bean 래핑
}
```

### 4.6 Mapper 도입

```java
public final class MemberMapper {
    static MemberJpaEntity toJpaEntity(Member member)  // Domain → JPA
    static Member toDomain(MemberJpaEntity entity)      // JPA → Domain
}
```

- 도메인 모델과 JPA Entity 간 양방향 변환
- `Email` VO ↔ `String` 변환 포함
- `createdAt`/`updatedAt`은 JPA Auditing 처리 후 Mapper에서 도메인으로 전달

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
| POST | `/api/members/signup` | 없음 |
| POST | `/api/members/login` | 없음 |
| GET | `/api/members/me` | 없음 |

기존 통합 테스트 7개 모두 통과 (전체 43 tests passed).

---

## 7. 발견 및 해결된 이슈

### 7.1 .gitignore `out/` 패턴 문제
- **증상**: `adapter/out/`, `port/out/` 디렉토리가 git에서 무시됨
- **원인**: `.gitignore`의 `out/` 규칙이 모든 `out` 디렉토리에 매칭
- **해결**: `out/` → `/out/` (루트 빌드 디렉토리만 무시)

### 7.2 ArchUnit port/in 자기 참조 규칙
- **증상**: `인바운드포트는_도메인에만_의존한다` 테스트 실패
- **원인**: port/in 패키지 내 클래스끼리 참조 (UseCase → MemberInfo) 시 규칙 위반
- **해결**: 허용 목록에 `..application.port.in..` 추가

### 7.3 JpaRepository @NonNull 경고
- **증상**: `MemberPersistenceAdapter`에서 null type safety 경고
- **원인**: Spring Data JPA의 `findById()`, `save()`가 `@NonNull` 파라미터 선언
- **해결**: Adapter 클래스에 `@SuppressWarnings("null")` 적용 (프로젝트 전체 일관성 유지)

---

## 8. 트레이드오프

| 장점 | 비용 |
|------|------|
| 도메인 순수성 — 프레임워크 독립 | 파일 수 +16개, 코드량 +320줄 |
| Port 추상화 — 기술 교체 용이 | 인터페이스 보일러플레이트 증가 |
| 단일 책임 서비스 — 변경 영향 최소 | Mapper 변환 코드 추가 |
| ArchUnit 자동 검증 — 규칙 위반 방지 | 초기 규칙 설정 비용 |
| 테스트 용이성 — Port Mock 가능 | 학습 곡선 |

> Phase 3(MSA 전환)에서 Port 추상화의 진가가 드러난다. Adapter만 교체하면 동일 도메인 로직으로 독립 서비스 전환 가능.
