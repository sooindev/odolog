# Cartree (차트리)

차량 관리 앱. 사용자가 자기 차량을 등록하고 정비 이력을 관리한다.

## 진행 방식 (가장 중요)

사용자는 Spring Boot / JPA 초보자이며, **코드를 한 줄씩 이해하면서 직접 작성하는 것**이
목표다. 한 번에 많은 코드를 쏟아내지 말 것.

각 단계마다:

1. 코드는 최소 단위로만 (파일 1~2개)
2. 새로 등장한 어노테이션·문법을 초보자 관점에서 한 줄씩 설명
3. 왜 그렇게 썼는지를 **대안과 비교해서** 설명 (예: EAGER 대신 LAZY를 쓰는 이유)

이해 확인 질문은 던지지 않는다. 설명 후 바로 다음 단계로 진행한다.

설명은 한국어로 한다.

## 기술 스택

- Spring Boot 3.5.6 / Java 17 / Gradle
- Spring Data JPA (Hibernate 6.6.x)
- **MariaDB 12.3.2** (MySQL 아님 — 아래 주의사항 참고)
- 패키지 루트: `com.cartree.app`

## 개발 환경

세팅은 이미 끝났다. 매번 재확인하지 말 것.

- DB: MariaDB, `localhost:3306`, 스키마 `cartree` (utf8mb4 / utf8mb4_unicode_ci)
- 드라이버: `org.mariadb.jdbc:mariadb-java-client`, URL은 `jdbc:mariadb://`
- 실행: **IntelliJ IDEA**에서 `CartreeApplication` 을 직접 실행한다.
  Gradle 래퍼(`./gradlew`)는 프로젝트에 있으므로 빌드 확인은 터미널에서도 가능하다.
- DB 자격증명은 IntelliJ 실행 구성의 **환경변수** `DB_USERNAME` / `DB_PASSWORD` 로 주입한다.
  `application.yml` 에는 `${DB_USERNAME:root}` / `${DB_PASSWORD:}` 형태로만 존재하며
  평문 비밀번호를 파일에 절대 적지 않는다.

### DB 접속 시 주의

터미널의 `mysql` 명령어는 **MariaDB 클라이언트**이고, `~/.my.cnf` 에 오래된 비밀번호가
남아 있어 자동으로 전송된다. 그래서 `--no-defaults` 없이 접속하면 `Access denied` 가 난다.

    /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE cartree; SHOW TABLES;"

이 계정(`user@localhost`)은 `unix_socket` 인증이라 비밀번호 없이 붙는다. 진단용으로만 쓴다.

### 탐색 시 무시할 경로

`.gradle/`, `.idea/`, `build/` 는 빌드·IDE 산출물이므로 읽지 않는다.

## 코드 설계 원칙

계속 지켜야 하는 규칙들. 어기려면 먼저 사용자에게 이유를 설명하고 동의를 받는다.

1. **Lombok을 쓰지 않는다.** 생성자·getter를 직접 작성한다. 어떤 코드가 생성되는지
   눈으로 보는 것이 학습 목적이기 때문. 도입할 만한 시점이 오면 그때 제안한다.
2. **setter를 열지 않는다.** 변경이 필요한 값만 `changeNickname()`, `updateOdometer()`
   처럼 의미 있는 이름의 메서드로 연다. 비즈니스 규칙은 엔티티 안에 둔다.
   (예: `updateOdometer` 는 주행거리가 감소하면 예외를 던진다.)
3. **JPA 기본 생성자는 `protected`** 로 좁힌다.
4. **연관관계는 단방향으로 시작한다.** `User` 에 `@OneToMany` 를 넣지 않았다.
   DB 구조가 동일하고 양방향은 동기화 부담이 크기 때문. 필요해지면 그때 검토한다.
5. **`@ManyToOne` 에는 항상 `fetch = FetchType.LAZY`** 를 명시한다 (기본값이 EAGER라 N+1 유발).
   `optional = false` 와 `@JoinColumn(nullable = false)` 를 짝으로 쓴다.
6. **제약조건에는 이름을 직접 붙인다.** (`uk_vehicles_plate_number`, `fk_vehicles_user`)
   Hibernate가 짓는 해시 이름(`UK6dotkott2kjsp8vw4d0m25fb7`)은 로그 추적이 불가능하다.
7. **시간 필드는 `@PrePersist` / `@PreUpdate`** 로 채운다. `@EnableJpaAuditing` 은 아직 미도입.
   `createdAt` 에는 `updatable = false` 를 준다.
8. **타입 선택**: "없음"이라는 상태가 존재하는 값만 래퍼 타입(`Integer`), 아니면 기본형(`int`).
   PK는 저장 전 `null` 구분을 위해 항상 `Long`.
9. **테이블명은 복수형** (`users`, `vehicles`). `user` 는 예약어라 반드시 `users`.

## 현재 구조

    src/main/java/com/cartree/app/
    ├── CartreeApplication.java
    ├── domain/
    │   ├── User.java        (id, email, password, nickname, phone, createdAt, updatedAt)
    │   └── Vehicle.java     (id, owner→User, plateNumber, manufacturer, modelName,
    │                         modelYear, odometer, createdAt, updatedAt)
    ├── repository/
    │   ├── UserRepository.java     (findByEmail, existsByEmail)
    │   └── VehicleRepository.java  (findByOwner, findByOwnerIdOrderByCreatedAtDesc,
    │                                findByPlateNumber, existsByPlateNumber)
    ├── dto/
    │   ├── SignUpRequest.java   (record, @NotBlank/@Email/@Size 검증)
    │   └── UserResponse.java    (record, User.from() 팩토리)
    ├── service/
    │   └── UserService.java     (signUp — 중복 이메일 체크, BCrypt 암호화, @Transactional)
    └── controller/
        └── UserController.java  (POST /api/users)

    src/test/java/com/cartree/app/repository/
    ├── UserRepositoryTest.java     (save, findByEmail, existsByEmail)
    └── VehicleRepositoryTest.java  (save, findByOwner, findByOwnerId, findByPlateNumber,
                                     ownerIsLazy, updateOdometer)

## 진행 상황

- [x] build.gradle / 실행 진입점
- [x] application.yml (MariaDB 연결)
- [x] `User` 엔티티 — 유니크 제약 이름을 `uk_users_email` 로 직접 지정 (규칙 6)
- [x] `Vehicle` 엔티티 — 검증 완료
- [x] `UserRepository` / `VehicleRepository` — 검증 완료
- [x] Repository 동작 확인 — `@DataJpaTest` 채택. 테스트 9개 통과
- [x] 회원가입 API: `UserService` + `UserController` + 요청/응답 DTO
      → `POST /api/users`. BCrypt 암호화, 계층 분리, `@Transactional`, 엔티티 미노출 설명 완료.
      → 미해결: 이메일 중복 시 `IllegalArgumentException`이 500으로 나감 (전역 예외 처리 미도입)
- [ ] 차량 등록/조회 API: `VehicleService` + `VehicleController`
- [ ] `MaintenanceRecord`(정비 이력) 엔티티 — 이 앱의 핵심 기능
- [ ] 정비 이력 API + 다음 정비 시점 계산 로직

단계를 완료할 때마다 이 체크리스트를 갱신한다.
