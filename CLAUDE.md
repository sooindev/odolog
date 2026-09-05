# 오도로그 (OdoLog)

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

### 백엔드
- Spring Boot 3.5.6 / Java 17 / Gradle
- Spring Data JPA (Hibernate 6.6.x)
- **MariaDB 12.3.2** (MySQL 아님 — 아래 주의사항 참고)
- 패키지 루트: `com.odolog.app`

### 프론트엔드 (예정, 아직 착수 전)
- React + Vite + TypeScript
- Tailwind CSS + shadcn/ui
- 저장소 루트에 `frontend/` 디렉토리를 새로 만들어 백엔드(Gradle 프로젝트)와 분리해서 관리
- 세션 쿠키 기반 인증이라 CORS에 `allowCredentials(true)`가 필수 — 상세는 아래 로드맵 Phase 1 참고
- TypeScript는 실무 표준이라 골랐지만, 막상 시작할 때 부담이 크면 순수 JS로 시작하는 것도 그때 재논의 가능

## 개발 환경

세팅은 이미 끝났다. 매번 재확인하지 말 것.

- DB: MariaDB, `localhost:3306`, 스키마 `odolog` (utf8mb4 / utf8mb4_unicode_ci)
- 드라이버: `org.mariadb.jdbc:mariadb-java-client`, URL은 `jdbc:mariadb://`
- 실행: **IntelliJ IDEA**에서 `OdoLogApplication` 을 직접 실행한다.
  Gradle 래퍼(`./gradlew`)는 프로젝트에 있으므로 빌드 확인은 터미널에서도 가능하다.
- DB 자격증명은 IntelliJ 실행 구성의 **환경변수** `DB_USERNAME` / `DB_PASSWORD` 로 주입한다.
  `application.yml` 에는 `${DB_USERNAME:root}` / `${DB_PASSWORD:}` 형태로만 존재하며
  평문 비밀번호를 파일에 절대 적지 않는다.

### DB 접속 시 주의

터미널의 `mysql` 명령어는 **MariaDB 클라이언트**이고, `~/.my.cnf` 에 오래된 비밀번호가
남아 있어 자동으로 전송된다. 그래서 `--no-defaults` 없이 접속하면 `Access denied` 가 난다.

    /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW TABLES;"

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
10. **로그인한 사용자 식별은 세션에서만 한다.** 요청 바디나 URL의 사용자 ID는 클라이언트가
    조작할 수 있으므로 신뢰하지 않는다 (`SessionConst.LOGIN_USER_ID`).
11. **예외는 의미에 맞는 상태 코드로 세분화한다**: 400(입력 검증 실패) / 401(미인증) /
    403(권한 없음) / 404(리소스 없음) / 409(리소스 중복). 서버 쪽 불변식이 깨진 경우
    (예: 세션엔 있는데 DB엔 없는 사용자)는 일부러 핸들러를 만들지 않고 500으로 흘려보내
    로그에 남긴다 — 모든 예외를 친절한 응답으로 감쌀 필요는 없다.

## 현재 구조

**패키지 구조를 계층별(domain/repository/dto/service/controller)에서 기능별(user/vehicle/
maintenance/common)로 전환했다.** 계층별 구조는 파일이 몇 개 없을 땐 괜찮지만, 기능이 늘어나면서
"차량 관련 파일 다 모아서 보기"가 6개 폴더를 오가야 하는 문제가 생겼다. 기능별 구조는 한 기능을
고치거나 이해할 때 그 폴더 하나만 보면 되는 대신, `Vehicle`이 `User`를 참조하는 것처럼 **기능 간
경계를 넘는 import가 생긴다** (예: `vehicle.domain.Vehicle`이 `user.domain.User`를 import).
이건 자연스러운 트레이드오프이고, 숨기려 하지 않는다.

    src/main/java/com/odolog/app/
    ├── OdoLogApplication.java
    │
    ├── user/                              — 회원가입·로그인·프로필
    │   ├── domain/
    │   │   └── User.java              (id, email, password, nickname, phone, createdAt, updatedAt)
    │   ├── repository/
    │   │   └── UserRepository.java    (findByEmail, existsByEmail)
    │   ├── dto/
    │   │   ├── SignUpRequest.java         (record, @NotBlank/@Email/@Size 검증)
    │   │   ├── LoginRequest.java          (record, email/password)
    │   │   ├── UpdateProfileRequest.java  (record, nickname/phone 둘 다 nullable — 보낸 필드만 변경)
    │   │   └── UserResponse.java          (record, User.from() 팩토리)
    │   ├── service/
    │   │   └── UserService.java       (signUp — 중복 이메일 체크·BCrypt 암호화·@Transactional,
    │   │                                login — 이메일/비밀번호 검증, 실패 사유 통일 메시지,
    │   │                                findById — 없으면 IllegalStateException→500,
    │   │                                updateProfile — 널 아닌 필드만 changeNickname/changePhone 호출)
    │   └── controller/
    │       └── UserController.java    (POST /api/users, POST /api/users/login — 세션 저장 +
    │                                    changeSessionId()로 세션 고정 공격 방지,
    │                                    POST /api/users/logout — session.invalidate(),
    │                                    GET/PATCH /api/users/me — @LoginUser 사용)
    │
    ├── vehicle/                           — 차량 등록·조회·주행거리·삭제
    │   ├── domain/
    │   │   └── Vehicle.java            (id, owner→user.domain.User, plateNumber, manufacturer,
    │   │                                modelName, modelYear, odometer, createdAt, updatedAt)
    │   ├── repository/
    │   │   └── VehicleRepository.java  (findByOwner, findByOwnerIdOrderByCreatedAtDesc,
    │   │                                findByPlateNumber, existsByPlateNumber)
    │   ├── dto/
    │   │   ├── VehicleRegisterRequest.java (record, owner 없음 — 세션에서 식별)
    │   │   ├── VehicleResponse.java        (record, owner 없음 — LAZY 필드 미접근으로 N+1 방지)
    │   │   └── UpdateOdometerRequest.java  (record, @PositiveOrZero)
    │   ├── service/
    │   │   └── VehicleService.java     (register — 번호판 중복 체크·@Transactional,
    │   │                                findMyVehicles — 조회 전용, @Transactional 없음,
    │   │                                updateOdometer — dirty checking으로 save() 불필요,
    │   │                                delete — maintenance.repository로 이력 먼저 삭제 후 차량 삭제,
    │   │                                findOwnedVehicle() — 소유권 검증, maintenance 패키지도 재사용)
    │   └── controller/
    │       └── VehicleController.java  (POST /api/vehicles, GET /api/vehicles,
    │                                    GET/PATCH /api/vehicles/{vehicleId}(/odometer),
    │                                    DELETE /api/vehicles/{vehicleId} — @LoginUser로 소유자 식별)
    │
    ├── maintenance/                        — 정비 이력·다음 정비 시점 계산
    │   ├── domain/
    │   │   ├── ServiceType.java        (enum: ENGINE_OIL/TIRE/BRAKE_PAD/BATTERY/OTHER,
    │   │   │                            recommendedIntervalKm — 다음 정비 시점 계산용, OTHER는 null)
    │   │   └── MaintenanceRecord.java  (id, vehicle→vehicle.domain.Vehicle, type, description, cost,
    │   │                                serviceOdometer, serviceDate, createdAt, updatedAt,
    │   │                                changeType/changeDescription/changeCost/
    │   │                                changeServiceOdometer/changeServiceDate)
    │   ├── repository/
    │   │   └── MaintenanceRecordRepository.java (findByVehicleIdOrderByServiceDateDesc,
    │   │                                         findTopByVehicleIdAndTypeOrderByServiceDateDesc,
    │   │                                         findByIdAndVehicleId — 다른 차량 소속 id 접근 차단,
    │   │                                         deleteByVehicleId — 차량 삭제 시 이력 함께 삭제용)
    │   ├── dto/
    │   │   ├── MaintenanceRecordRegisterRequest.java (record, @NotNull/@PositiveOrZero/@Size 검증)
    │   │   ├── MaintenanceRecordUpdateRequest.java   (record, 전부 nullable — cost/serviceOdometer는
    │   │   │                                          Integer로 "안 보냄"과 "0" 구분)
    │   │   ├── MaintenanceRecordResponse.java        (record, MaintenanceRecord.from() 팩토리)
    │   │   └── NextServiceResponse.java              (record, type/lastServiceOdometer/
    │   │                                              nextServiceOdometer/lastServiceDate/
    │   │                                              nextServiceDate — 이력·주기 없으면 null)
    │   ├── service/
    │   │   └── MaintenanceRecordService.java (register/findByVehicle/calculateNextService/
    │   │                                      findOne/update/delete, vehicle.service.VehicleService.
    │   │                                      findOwnedVehicle()을 주입받아 재사용,
    │   │                                      findRecordInVehicle()로 레코드 소속 검증)
    │   └── controller/
    │       └── MaintenanceRecordController.java (POST/GET/PATCH/DELETE /api/vehicles/{vehicleId}/maintenance-records{/recordId},
    │                                             GET .../next-service?type=... — @LoginUser로 식별)
    │
    └── common/                             — 기능 어디에도 속하지 않는 공통 인프라
        ├── auth/
        │   ├── LoginUser.java                 (@Target(PARAMETER) 커스텀 애노테이션 — 로그인 사용자 주입 표시)
        │   ├── LoginUserArgumentResolver.java (HandlerMethodArgumentResolver — 세션에서 LOGIN_USER_ID
        │   │                                   추출, 없으면 AuthenticationFailedException)
        │   └── SessionConst.java              (세션 키 상수 LOGIN_USER_ID)
        ├── config/
        │   └── WebConfig.java                 (WebMvcConfigurer — LoginUserArgumentResolver 등록)
        ├── exception/
        │   ├── AuthenticationFailedException.java (RuntimeException — 로그인 실패/미인증 전용, 401)
        │   ├── ForbiddenAccessException.java      (RuntimeException — 소유자 아님, 403)
        │   ├── ResourceNotFoundException.java     (RuntimeException — 리소스 없음, 404)
        │   └── GlobalExceptionHandler.java        (@RestControllerAdvice — 리소스 중복 409, 인증 실패 401,
        │                                            권한 없음 403, 리소스 없음 404, 검증 실패/타입 변환 실패 400.
        │                                            IllegalStateException 등은 의도적으로 미처리 → 500)
        └── dto/
            └── ErrorResponse.java              (record, message)

    src/test/java/com/odolog/app/
    ├── user/
    │   ├── repository/UserRepositoryTest.java (@DataJpaTest — save, findByEmail, existsByEmail)
    │   ├── service/UserServiceTest.java       (Mockito — signUp 중복/암호화, login 성공·실패,
    │   │                                        findById, updateProfile 부분 수정)
    │   └── controller/UserControllerTest.java (@WebMvcTest+MockMvc — signUp 201/409,
    │                                            login 200(세션 저장 확인)/401, /me 401/200)
    ├── vehicle/
    │   ├── repository/VehicleRepositoryTest.java (@DataJpaTest — save, findByOwner, findByOwnerId,
    │   │                                          findByPlateNumber, ownerIsLazy, updateOdometer)
    │   ├── service/VehicleServiceTest.java       (Mockito — register 중복/성공, findOwnedVehicle
    │   │                                          404·403, updateOdometer 감소 방지,
    │   │                                          delete 순서(InOrder) 검증)
    │   └── controller/VehicleControllerTest.java (@WebMvcTest+MockMvc — 미인증 401, 검증 실패 400,
    │                                              register 201, findOne 200/404, updateOdometer 403)
    └── maintenance/
        ├── service/MaintenanceRecordServiceTest.java (Mockito — register, calculateNextService
        │                                              3가지 케이스, update 부분 수정,
        │                                              타 차량 소속 id 접근 404)
        └── controller/MaintenanceRecordControllerTest.java (@WebMvcTest+MockMvc — next-service 200,
                                                             잘못된 enum 값 400, 차량 없음 404,
                                                             findOne 200/404, delete 204)

**의존 방향**: `maintenance` → `vehicle` → `user`, 그리고 셋 다 필요하면 `common`을 본다.
반대 방향 의존(`user`가 `vehicle`을 알아야 하는 것 등)이 생기면 설계가 잘못된 신호로 보고 재검토한다.

## 진행 상황 (완료)

- [x] build.gradle / 실행 진입점
- [x] application.yml (MariaDB 연결)
- [x] `User` 엔티티 — 유니크 제약 이름을 `uk_users_email` 로 직접 지정 (규칙 6)
- [x] `Vehicle` 엔티티 — 검증 완료
- [x] `UserRepository` / `VehicleRepository` — 검증 완료
- [x] Repository 동작 확인 — `@DataJpaTest` 채택. 테스트 9개 통과
- [x] 회원가입 API: `UserService` + `UserController` + 요청/응답 DTO
      → `POST /api/users`. BCrypt 암호화, 계층 분리, `@Transactional`, 엔티티 미노출 설명 완료.
- [x] 전역 예외 처리: `GlobalExceptionHandler` (`@RestControllerAdvice`)
      → 이메일 중복 409, `@Valid` 검증 실패 400, `ErrorResponse`로 응답 형식 통일
- [x] 로그인 API: `POST /api/users/login` — 세션 기반(HttpSession) 인증 채택
      → 실패 사유(이메일 없음/비밀번호 틀림)를 401 + 동일 메시지로 통일 (user enumeration 방지)
      → `SessionConst.LOGIN_USER_ID`로 세션에 저장. 차량 API는 이 세션값으로 소유자를 식별할 예정
- [x] 로그아웃 API: `POST /api/users/logout` — `getSession(false)` + `invalidate()`, 204 응답
- [x] 차량 등록/조회 API: `VehicleService` + `VehicleController`
      → `POST /api/vehicles`, `GET /api/vehicles`. 소유자는 세션(`SessionConst.LOGIN_USER_ID`)에서 식별
      → 번호판 중복은 기존 409 핸들러 재사용, 세션 없으면 기존 401(`AuthenticationFailedException`) 재사용
      → 확장 지점: `extractLoginUserId()`가 컨트롤러 3개 이상에서 반복되면 `HandlerMethodArgumentResolver`로 추출 고려
- [x] `MaintenanceRecord`(정비 이력) 엔티티 — 이 앱의 핵심 기능
      → `type`은 자유 텍스트 대신 `ServiceType` enum + `@Enumerated(EnumType.STRING)` (ORDINAL은 순서 변경 시 데이터 깨짐)
      → `Vehicle.odometer`(현재 주행거리)와 구분하려고 필드명을 `serviceOdometer`로 지정
      → 아직 변경 메서드 없음 (수정 요구사항 생기면 그때 추가)
- [x] `MaintenanceRecordRepository` — 조회 메서드만 우선 작성
      → `findByVehicleIdOrderByServiceDateDesc`(이력 목록), `findTopByVehicleIdAndTypeOrderByServiceDateDesc`
        (같은 종류 중 최신 1건 — 다음 정비 시점 계산에 사용 예정)
- [x] 정비 이력 API + 다음 정비 시점 계산 로직
      → `POST/GET /api/vehicles/{vehicleId}/maintenance-records`, `GET .../next-service?type=`
      → 소유권 검증 실패를 404(리소스 없음)/403(소유자 아님)으로 구분, 로그인 안 함은 기존 401 재사용
      → `ServiceType.recommendedIntervalKm` + 최근 이력의 `serviceOdometer`로 다음 정비 시점 계산
      → LAZY `owner`의 `getId()`는 프록시가 FK를 이미 들고 있어 초기화 없이 조회 가능 (@Transactional 불필요)
- [x] 로그인 성공 시 세션 고정 공격(session fixation) 방지
      → `UserController.login()`에서 세션에 값을 저장한 직후 `httpRequest.changeSessionId()` 호출.
        세션 내용은 유지한 채 클라이언트에 내려가는 세션 ID만 새로 발급.
- [x] `extractLoginUserId()` 중복 제거 — `HandlerMethodArgumentResolver` + 커스텀 `@LoginUser` 애노테이션
      → `LoginUser`(애노테이션) + `LoginUserArgumentResolver` + `WebConfig`(등록) 추가.
        `VehicleController`/`MaintenanceRecordController`는 `@LoginUser Long userId` 파라미터로 정리.
- [x] `GET /api/users/me`, `PATCH /api/users/me`
      → `UpdateProfileRequest`(nickname/phone 둘 다 nullable, 보낸 필드만 반영).
        기존에 미사용이던 `User.changeNickname()`/`changePhone()`를 처음으로 API에 연결.
- [x] `PATCH /api/vehicles/{vehicleId}/odometer` — 주행거리 갱신
      → `Vehicle.updateOdometer()`를 처음으로 API에 연결. 감소 시 `IllegalArgumentException` →
        기존 409 핸들러 재사용 ("요청 값이 리소스의 현재 상태와 충돌"로 해석).
      → 영속 상태 엔티티라 `save()` 호출 없이 dirty checking으로 UPDATE 반영됨.
      → `findOwnedVehicle()`을 `MaintenanceRecordService`에서 `VehicleService`(원래 책임 소재)로 이동,
        `MaintenanceRecordService`는 `VehicleService`를 주입받아 재사용하도록 리팩토링.
- [x] `DELETE /api/vehicles/{vehicleId}` — 차량 삭제 (연관 정비 이력 함께 삭제)
      → JPA cascade(양방향 필요, 규칙 4 위배)나 DB `ON DELETE CASCADE`(코드에서 안 보이는 숨은 동작) 대신,
        서비스 계층에서 `MaintenanceRecordRepository.deleteByVehicleId()` → `vehicleRepository.delete()`
        순서로 명시적으로 삭제. 순서를 바꾸면 FK 제약 위반으로 실패함.
      → `VehicleService`는 `MaintenanceRecordService`가 아니라 `MaintenanceRecordRepository`를
        직접 주입받음 — 서비스끼리 서로 의존하면 순환 참조가 생기기 때문.
- [x] 패키지 구조를 계층별(domain/repository/dto/service/controller)에서
      기능별(user/vehicle/maintenance/common)로 재구성
      → 파일 30여 개의 package 선언과 import 경로를 전부 이동. 기능 간 참조(예: `Vehicle`이
        `User`를 참조)는 이제 명시적인 cross-package import로 드러남.
      → 의존 방향을 `maintenance → vehicle → user`로 정리, 공용 인프라(인증/예외/설정)는 `common`으로.
- [x] `@RequestParam` 타입 변환 실패 처리 — `MethodArgumentTypeMismatchException` → 400
      → `ServiceType` 같은 enum뿐 아니라 타입 변환이 필요한 모든 파라미터에 공통 적용되는 범용 핸들러.
- [x] 정비 이력 수정/삭제 API
      → `PATCH`, `DELETE /api/vehicles/{vehicleId}/maintenance-records/{recordId}`.
      → `MaintenanceRecord`에 필드별 변경 메서드(`changeType`/`changeDescription`/`changeCost`/
        `changeServiceOdometer`/`changeServiceDate`) 추가, `UpdateProfileRequest`와 같은 부분 수정 패턴.
      → 요청 DTO의 `cost`/`serviceOdometer`는 엔티티와 달리 `Integer` — "안 보냄(null)"과
        "0으로 변경"을 구분하기 위함(엔티티의 "없음" 상태와는 무관, DTO 자체의 필요).
      → `findByIdAndVehicleId()`로 다른 차량 소속 레코드 id 접근을 404로 차단.
- [x] Service 계층 단위 테스트 (Mockito) — `UserServiceTest`/`VehicleServiceTest`/
      `MaintenanceRecordServiceTest`, 총 18개
      → `@ExtendWith(MockitoExtension.class)` + `@Mock`/`@InjectMocks`로 Repository(또는
        `MaintenanceRecordService`가 의존하는 `VehicleService`)를 가짜로 만들어 DB 없이 로직만 검증.
      → `ReflectionTestUtils.setField()`로 `@GeneratedValue` id를 테스트에서만 강제로 채움.
      → `VehicleServiceTest.delete()` 테스트는 `InOrder`로 "이력 먼저, 차량 나중" 삭제 순서까지 검증.
      → 각 서비스는 자신이 직접 의존하는 대상만 mock — `MaintenanceRecordService`는
        `VehicleRepository`가 아니라 `VehicleService`를 mock (소유권 검증 로직 자체는
        `VehicleServiceTest`가 이미 검증했다고 신뢰).
- [x] Controller 계층 테스트 (`@WebMvcTest` + `MockMvc`) — `UserControllerTest`/
      `VehicleControllerTest`/`MaintenanceRecordControllerTest`, 총 14개
      → `@MockitoBean`(Spring Boot 3.4+의 `@MockBean` 대체)으로 Service를 컨테이너에 가짜로 등록,
        `@WebMvcTest`가 `GlobalExceptionHandler`/`WebConfig`까지 포함해 웹 계층만 띄움(DB 없음).
      → `MockHttpSession`으로 `@LoginUser` 인증 흐름(401/성공)을 실제 HTTP 요청처럼 검증.
      → 로그인 성공 테스트는 응답 바디가 아니라 `result.getRequest().getSession(false)`로
        "세션에 값이 저장됐는지, `changeSessionId()`가 동작하는지"라는 부수 효과를 검증.
      → 이전에 추가만 해두고 테스트가 없었던 `MethodArgumentTypeMismatchException`(400) 핸들러를
        `type=존재하지않는값` 요청으로 처음 실제 검증.
- [x] 차량 단건 상세 조회 API — `GET /api/vehicles/{vehicleId}`
      → `VehicleService.findOwnedVehicle()`을 그대로 재사용 (새 비즈니스 로직 없음).
        `VehicleControllerTest`에 성공/404 테스트 2개 추가.
- [x] 프로젝트 이름 변경: Cartree(차트리) → 오도로그(OdoLog)
      → 패키지 `com.cartree.app` → `com.odolog.app` 전체 이동(`git mv`로 히스토리 보존),
        메인 클래스 `CartreeApplication` → `OdoLogApplication`.
      → `build.gradle`(group)/`settings.gradle`(rootProject.name), DB 스키마명
        `cartree`/`cartree_test` → `odolog`/`odolog_test`까지 전부 갱신.
      → GitHub 저장소 이름(`CarTree`)은 `gh` CLI가 없어 대신 수동 변경 방법만 안내.
        운영 DB `cartree` 스키마는 애초에 존재하지 않아(아직 운영 전) 마이그레이션 없이 `odolog`만 새로 생성.
- [x] CORS 설정 — `WebConfig.addCorsMappings()`
      → `/api/**`에 대해 `http://localhost:5173`(Vite 기본 포트) origin 허용, GET/POST/PATCH/DELETE 메서드,
        `allowCredentials(true)`로 세션 쿠키 전송 허용.
      → `allowCredentials(true)`와 `allowedOrigins("*")`는 브라우저 스펙상 공존 불가 — CSRF 위험 때문에
        브라우저가 막으므로 origin을 구체적으로 나열해야 함.
      → 컨트롤러마다 `@CrossOrigin`을 붙이는 대신 `WebMvcConfigurer`에 전역 설정 — 컨트롤러 3개에
        같은 설정을 반복하지 않기 위해. `LoginUserArgumentResolver` 등록과 같은 자리.
      → 프론트 착수 시 `fetch(url, { credentials: 'include' })`가 짝으로 필요함. 실제 Vite 포트가
        5173이 아니면 `allowedOrigins`를 그때 갱신.
- [x] 다음 정비 시점 계산에 "날짜 기준" 추가
      → `ServiceType`에 `recommendedIntervalMonths` 추가(ENGINE_OIL 6 / TIRE·BRAKE_PAD 24 / BATTERY 36,
        OTHER는 null). `NextServiceResponse`에 `lastServiceDate`/`nextServiceDate` 필드 추가.
      → `LocalDate.plusMonths()`로 계산 — 달마다 길이가 다르므로 `plusDays(30 * months)`는 쓰지 않음.
        말일 보정(1/31 + 1개월 = 2/28)도 자동 처리됨.
      → `Integer`인 주기를 `plusMonths(long)`에 넘기면 언박싱되므로, null 검사를 삼항 연산자로 **먼저**
        해야 NPE가 안 남.
      → `lastServiceDate`까지 응답에 넣은 이유: 프론트가 "언제 정비했으니 언제가 다음"이라는 근거와
        결과를 한 화면에 보여줄 때 추가 API 호출이 필요 없게 하려고.

- [x] 정비 이력 단건 상세 조회 API — `GET /api/vehicles/{vehicleId}/maintenance-records/{recordId}`
      → 차량 단건 조회와 같은 패턴. `MaintenanceRecordService`의 기존 private `findRecordInVehicle()`을
        감싸는 `findOne()` public 메서드만 추가. `/next-service`(리터럴)와 `/{recordId}`(변수 경로)는
        스프링이 리터럴을 우선 매칭하므로 라우팅 충돌 없음.

## 완성까지의 로드맵

**"완성"의 정의**: 회원/차량/정비 이력을 관리하는 백엔드 API + 그걸 실제로 쓸 수 있는
프론트엔드 웹앱까지. 배포(서버 인프라, 도메인, CI/CD)는 범위 밖 — **로컬에서 완전히 동작하는 것**까지가 목표다.

아래 Phase 순서로 진행한다. 각 Phase의 세부 단계는 지금 전부 확정하지 않고, 착수 시점에
다시 쪼갠다 — 특히 프론트엔드는 아직 한 줄도 안 짜본 영역이라, 막상 시작하면 순서나 범위가
바뀔 가능성이 크다. 아래 "다음 단계 상세 체크리스트"는 **Phase 1(백엔드 마무리)**의 세부 항목이다.

- **Phase 1 — 백엔드 마무리** (지금 여기)
  차량/정비 이력 단건 조회, 날짜 기준 다음 정비 계산, 페이지네이션, CORS 설정.
  프론트가 호출할 API 표면을 먼저 완성한다.
- **Phase 2 — 프론트엔드 프로젝트 셋업**
  `frontend/`에 Vite+React+TypeScript 생성, Tailwind/shadcn 설치, 백엔드 CORS 연동 확인
  (로그인 API를 프론트에서 호출해 세션 쿠키가 실제로 오가는지가 첫 번째 관문).
- **Phase 3 — 인증 화면**
  회원가입/로그인 페이지, 로그인 상태 전역 관리(React Context), 로그아웃.
- **Phase 4 — 차량 관리 화면**
  차량 목록, 등록 폼, 상세 페이지(주행거리 갱신·삭제).
- **Phase 5 — 정비 이력 관리 화면**
  차량 상세 페이지 안에 이력 목록/등록·수정 폼, 다음 정비 시점(주행거리+날짜) 표시.
- **Phase 6 — 다듬기**
  로딩/에러 상태 UI, 백엔드 에러 메시지와 폼 검증 연결, 반응형 레이아웃.

## 다음 단계 (예정) — 상세 체크리스트 (Phase 1: 백엔드 마무리)

우선순위 순서를 뜻하지 않는다. 착수하는 시점에 사용자와 다시 상의해서 순서/범위를 정한다.

### 3. 목록 조회 확장성

- [ ] 차량 목록 / 정비 이력 목록에 페이지네이션 도입 (`Pageable`, `Page<T>`)
      → 지금은 차량이나 정비 이력이 몇 개 안 될 걸 가정하고 전체를 다 반환함.
      → `JpaRepository`는 이미 `findAll(Pageable)`을 기본 제공하지만, 우리가 쓰는 커스텀 조회
        메서드(`findByOwnerIdOrderByCreatedAtDesc` 등)에 `Pageable` 파라미터를 추가하고
        반환 타입을 `Page<Vehicle>`로 바꿔야 함.
      → 컨트롤러에서 `@PageableDefault(size = 20)`로 기본값을 정하고, 응답을 그대로 `Page<T>`로
        내려줄지 커스텀 DTO로 감쌀지는 그때 정함 (프론트에서 무한 스크롤 vs 페이지 번호 중
        뭘 쓸지에 따라 응답 형태가 달라질 수 있음).

### 4. 인프라/운영 (당장 급하지 않음)

- [ ] API 문서화 검토 (springdoc-openapi 등)
      → 프론트를 만들 때 매번 컨트롤러 코드를 열어보지 않고 Swagger UI에서 API 스펙을
        확인할 수 있으면 편함. 프론트 착수 직전에 붙이는 게 가장 효율적일 수 있음.
- [ ] `@EnableJpaAuditing` 도입 검토
      → 엔티티가 늘어날수록 `@PrePersist`/`@PreUpdate` 반복 코드가 계속 늘어남. 이 부담이
        커지면 규칙 7을 재검토하고 auditing 도입을 제안할 것.

---

단계를 완료할 때마다 "진행 상황 (완료)" 섹션을 갱신하고, 해당 항목을 위 체크리스트에서 제거한다.
Phase 1이 끝나면 이 문서에 "Phase 2 상세 체크리스트"를 새로 추가해서 같은 방식으로 진행한다.
