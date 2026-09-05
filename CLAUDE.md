# 오도로그 (OdoLog)

차량 관리 앱. 사용자가 자기 차량을 등록하고 정비 이력을 관리한다.

## 진행 방식 (가장 중요)

사용자는 Spring Boot / JPA 초보자이며, **코드를 한 줄씩 이해하면서 직접 작성하는 것**이
목표다. 한 번에 많은 코드를 쏟아내지 말 것.

각 단계마다:

1. 코드는 최소 단위로만 (파일 1~2개)
2. 새로 등장한 어노테이션·문법을 초보자 관점에서 한 줄씩 설명
3. 왜 그렇게 썼는지를 **대안과 비교해서** 설명 (예: EAGER 대신 LAZY를 쓰는 이유)
4. 작업이 하나 끝날 때마다 **커밋 메시지를 한 줄로 알려준다**

이해 확인 질문은 던지지 않는다. 설명 후 바로 다음 단계로 진행한다.

설명은 한국어로 한다.

**커밋과 푸시는 사용자가 직접 한다.** `git commit` / `git push` 를 대신 실행하지 않고,
쓸 커밋 메시지만 알려준다. "커밋할까요?" 라고 묻지도 않는다.

## 문서 작성 규칙

`README.md`의 **트러블슈팅** 섹션에는 **사용자가 실제로 겪은 문제만** 적는다.

- 사실 그대로 쓴다. 꾸미거나 과장하지 않고, 극적으로 만들지 않는다.
- 형식: **증상 → 원인 → 해결**. 재현할 수 있는 명령어나 에러 메시지를 그대로 남긴다.
- 겪지 않은 문제, 겪을 법한 문제, 일반적인 팁은 적지 않는다. 실제로 막혔던 것만 남긴다.
- 문제를 겪은 시점에 바로 후보로 올리고, 무엇을 적을지는 사용자와 함께 정한다.

문서 역할 구분:

- `README.md` — 남이 이 저장소를 봤을 때 필요한 것. 기술 스택, 실행 방법, 구조, API 개요, 트러블슈팅.
- `CLAUDE.md` — 작업용 기록. 설계 결정과 그 이유, 대안 비교, 진행 상황, 체크리스트.

## 기술 스택

### 백엔드
- Spring Boot 3.5.6 / Java 17 / Gradle
- Spring Data JPA (Hibernate 6.6.x)
- **MariaDB 12.3.2** (MySQL 아님 — 아래 주의사항 참고)
- 패키지 루트: `com.odolog.app`

### 프론트엔드 (`frontend/`)
- Node 26 (Homebrew) / React 19 / Vite 8 / TypeScript 6
- Tailwind CSS v4 (`@tailwindcss/vite` 플러그인) + shadcn/ui
- 린터는 ESLint가 아니라 **oxlint** (Vite 템플릿 기본값, Rust 기반)
- 개발 서버 `http://localhost:5173` — 백엔드 `WebConfig`의 CORS `allowedOrigins`와 짝이다
- 저장소 루트의 `frontend/`. 백엔드(Gradle)와 완전히 분리되어 있고 서로 빌드에 관여하지 않는다

## 개발 환경

세팅은 이미 끝났다. 매번 재확인하지 말 것.

- DB: MariaDB, `localhost:3306`, 스키마 `odolog` (utf8mb4 / utf8mb4_unicode_ci)
- 드라이버: `org.mariadb.jdbc:mariadb-java-client`, URL은 `jdbc:mariadb://`
- 실행: **IntelliJ IDEA**에서 `OdoLogApplication` 을 직접 실행한다.
  Gradle 래퍼(`./gradlew`)는 프로젝트에 있으므로 빌드 확인은 터미널에서도 가능하다.
- API 문서: 앱 실행 후 `http://localhost:8080/swagger-ui.html` (스펙 JSON은 `/v3/api-docs`).
- 터미널에서 앱을 띄워 확인해야 할 때는 테스트 계정을 쓴다 (운영 계정 비밀번호는 IntelliJ에만 있음):

      SPRING_DATASOURCE_URL='jdbc:mariadb://localhost:3306/odolog_test' \
      SPRING_DATASOURCE_USERNAME=odolog_test SPRING_DATASOURCE_PASSWORD=odolog_test ./gradlew bootRun

  (`odolog_test` 스키마는 테스트 실행 때마다 `create-drop`으로 초기화되므로 데이터가 남아도 무방하다.
  JDBC 유닉스 소켓 접속(`localSocket=`)은 시도해 봤으나 동작하지 않으니 시간 낭비하지 말 것.)
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
    │   │   └── VehicleRepository.java  (findByOwner, findByOwnerId(Pageable),
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
    │   │   └── MaintenanceRecordRepository.java (findByVehicleId(Pageable),
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
            ├── ErrorResponse.java              (record, message)
            └── PageResponse.java               (record<T>, items/page/size/totalElements/
                                                 totalPages/hasNext, Page<T>.from() 팩토리)

    frontend/
    ├── index.html
    ├── vite.config.ts                  (react + tailwindcss 플러그인, '@' → ./src 별칭)
    ├── tsconfig.json                   (references + paths — shadcn CLI가 이 파일을 읽는다)
    ├── tsconfig.app.json               (src/ 코드용. paths 여기에도 필요)
    ├── components.json                 (shadcn 설정. style=base-nova, 별칭 매핑)
    ├── .env.development                (VITE_API_BASE_URL=http://localhost:8080)
    └── src/
        ├── main.tsx                    (BrowserRouter > AuthProvider > App)
        ├── App.tsx                     (라우트 정의 + Header)
        ├── index.css                   (@import "tailwindcss" + shadcn 테마 변수)
        ├── env.d.ts                    (import.meta.env 타입)
        ├── types/api.ts                (백엔드 DTO 대응 타입 + ServiceType 한글 라벨)
        ├── lib/api.ts                  (fetch 래퍼 — credentials:'include', ApiError, 204 처리,
        │                                401 전역 핸들러)
        ├── auth/
        │   ├── AuthContext.ts          (Context + useAuth 훅 — 컴포넌트 아닌 것만)
        │   ├── AuthProvider.tsx        (세션 복구/login/logout 상태 관리)
        │   └── ProtectedRoute.tsx      (로그인 안 했으면 /login으로)
        ├── lib/format.ts               (formatKm / formatWon)
        ├── lib/vehicles.ts             (차량 엔드포인트 모음)
        ├── pages/
        │   ├── LoginPage.tsx
        │   ├── SignUpPage.tsx          (가입 성공 시 바로 로그인까지)
        │   ├── ProfilePage.tsx         (바뀐 필드만 PATCH)
        │   ├── VehicleListPage.tsx     (목록 + 페이지네이션 + 빈 상태)
        │   ├── VehicleNewPage.tsx      (등록 폼, 409 → 폼 에러)
        │   └── VehicleDetailPage.tsx   (상세 + 주행거리 갱신 + 삭제)
        ├── components/
        │   ├── Header.tsx
        │   └── ui/                     (shadcn: button/input/label/card)
        └── ...

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
- [x] Phase 4 — 차량 관리 화면 (목록/등록/상세/주행거리/삭제)
      → `lib/vehicles.ts`에 엔드포인트를 모았다. 화면이 URL 문자열을 직접 들고 있으면 경로가 바뀔 때
        여러 파일을 뒤져야 한다.
      → **shadcn v4 버튼에는 `asChild`가 없다.** Radix의 Slot 대신 Base UI의 `render` prop을 쓴다:
        `<Button render={<Link to="..." />}>텍스트</Button>`. 인터넷 예제 대부분이 `asChild`라 주의.
      → 숫자 입력(`연식`, `주행거리`)의 상태는 **문자열로 둔다**. 입력 도중의 빈 문자열을 숫자로
        표현할 방법이 없기 때문. 전송 직전에 `Number()`로 변환한다.
      → effect 안에서 `setLoading(true)`를 **동기적으로** 호출하면 렌더가 한 번 더 돈다
        (oxlint `react(set-state-in-effect)`). `await` 뒤에서만 setState 하도록 바꿨다.
        덤으로 페이지 이동 시 이전 목록이 유지돼 화면이 깜빡이지 않는다.
      → `cancelled` 플래그 + 정리 함수: 응답이 늦게 도착했을 때 이미 사라진 컴포넌트에
        setState 하는 것을 막는다. StrictMode의 effect 두 번 실행 때문에 반드시 필요.
      → 차량 상세는 404(없음)와 403(남의 차)을 **구분해서 보여주지 않는다.** 남의 차량이
        존재한다는 사실 자체를 알리지 않기 위해.
      → 주행거리 감소는 백엔드가 409 + "주행거리는 줄어들 수 없습니다."를 준다.
        화면에서는 현재 값을 덧붙여 "(현재 50,000km)"까지 보여준다.
      → 삭제는 `window.confirm`으로 "정비 이력도 함께 삭제됨"을 명시. Phase 6에서 다이얼로그로 교체 검토.

- [x] Phase 3 — 인증 화면 (라우팅 + 로그인 상태 전역 관리)
      → `react-router` 8. v7부터 패키지 이름이 `react-router-dom`이 아니라 `react-router`다.
      → `AuthContext.ts`(Context + `useAuth`)와 `AuthProvider.tsx`(컴포넌트)를 **파일로 분리**.
        한 파일에서 컴포넌트와 함수를 같이 내보내면 Vite 핫 리로드가 전체 새로고침으로 떨어진다
        (oxlint `react(only-export-components)` 경고).
      → Context 기본값을 `null`로 두고 `useAuth()`에서 던진다. 그럴듯한 가짜 기본값을 주면
        `<AuthProvider>`를 빠뜨렸을 때 에러 없이 "로그아웃 상태"로 조용히 동작해 원인 추적이 어렵다.
      → 로그인 유지의 정체는 앱 시작 시 `GET /api/users/me` 1회 호출. 쿠키는 남아 있으므로
        서버에 "누구냐"를 되묻는 것. 이 응답 전까지는 `loading`이라 로그인 화면이 깜빡이지 않는다.
      → 세션 만료(401) 전역 처리: `api.ts`가 `setUnauthorizedHandler()`로 콜백을 받아 두고 401에서 호출,
        `AuthProvider`가 사용자 정보를 비우면 `ProtectedRoute`가 `/login`으로 보낸다.
        단 `/api/users/login`(비밀번호 오류)과 `/api/users/me`(로그인 여부 확인)의 401은 제외.
      → `ProfilePage`는 `ProfilePage`(null 걸러냄) + `ProfileForm`(확정된 user를 props로)로 나눴다.
        TypeScript는 `function` 선언 안에서는 바깥 변수의 좁혀진 타입을 믿지 않아 `user!`가 필요해지는데,
        컴포넌트를 나누면 단언 없이 해결된다.
      → 회원가입 성공 후 이어서 로그인까지 호출한다(가입 API는 세션을 만들지 않음).

- [x] Phase 2 — 프론트엔드 프로젝트 셋업 (`frontend/`)
      → Node 26을 Homebrew로 설치(미설치 상태였음). `npm create vite@latest frontend -- --template react-ts`.
      → Tailwind v4는 `@tailwindcss/vite` 플러그인 + `src/index.css`의 `@import "tailwindcss";` 한 줄.
        v3의 `tailwind.config.js`/PostCSS 방식과 섞으면 스타일이 아예 안 먹으니 주의.
      → **함정 1**: 경로 별칭 `@/*`를 `tsconfig.app.json`에만 넣으면 shadcn CLI가 별칭을 못 풀고
        프로젝트 루트에 `@/` 라는 **디렉토리를 실제로 만들어 버린다**. shadcn은 루트 `tsconfig.json`을
        읽으므로 거기에도 `paths`를 넣어야 한다.
      → **함정 2**: TypeScript 6부터 `baseUrl`이 폐기 경고(TS5101)를 낸다. `paths`만 두면 tsconfig
        위치 기준으로 해석되므로 `baseUrl` 없이 쓴다.
      → shadcn v4는 예전과 달리 Radix가 아니라 `@base-ui/react`를 쓰고, `cn` 유틸도
        `@/lib/utils`가 아니라 `cn` npm 패키지에서 가져온다. 구버전 예제와 다르니 주의.
      → `src/lib/api.ts` — 모든 요청에 `credentials: 'include'`(세션 쿠키), 실패 시 백엔드
        `ErrorResponse.message`를 꺼내 `ApiError(status, message)`로 던짐, 204는 본문 없이 반환.
      → `src/types/api.ts` — `/v3/api-docs` 스펙을 보고 백엔드 DTO 13개를 수기로 옮김.
        `ServiceType`은 문자열 리터럴 유니온 + 한글 라벨 맵(`SERVICE_TYPE_LABELS`)을 같이 둠.
      → **연동 검증 완료**: Origin 헤더를 붙인 요청으로 CORS preflight(`Access-Control-Allow-Credentials: true`),
        로그인 시 `Set-Cookie: JSESSIONID`, 그 쿠키로 `GET /api/users/me` 200까지 확인.
        Vite dev proxy는 필요 없었다.

- [x] API 문서화 — `springdoc-openapi` 도입 (`/swagger-ui.html`, `/v3/api-docs`)
      → `org.springdoc:springdoc-openapi-starter-webmvc-ui:2.8.6`. 서드파티라 스프링 부트가
        버전을 관리해 주지 않으므로 버전을 직접 명시해야 함. `springfox`는 Jakarta 미지원이라 제외.
      → `common/config/OpenApiConfig` — `@OpenAPIDefinition`으로 제목/설명, 그리고 정적 블록의
        `SpringDocUtils.addAnnotationsToIgnore(LoginUser.class)`로 `@LoginUser Long userId`가
        문서에 요청 파라미터로 잘못 노출되는 것을 차단(세션에서 오는 값이라 클라이언트 입력이 아님).
      → 목록 API의 `Pageable`에 `@ParameterObject`를 붙여 `page`/`size`/`sort` 3개 쿼리 파라미터로 펼침.
      → 검증 결과: 엔드포인트 16개 자동 생성, `PageResponse<T>` 제네릭도
        `PageResponseVehicleResponse`처럼 타입별 스키마로 분리되어 `items` 원소 타입까지 정확히 나옴.
      → 세션 쿠키는 Swagger UI가 같은 출처(localhost:8080)라 자동 전송된다.
        `POST /api/users/login`을 먼저 실행하면 이후 요청이 그대로 인증됨 — `@SecurityScheme` 불필요.

- [x] 차량 목록 / 정비 이력 목록 페이지네이션 (`Pageable`, `Page<T>`)
      → 공용 `common/dto/PageResponse<T>` 신설(`items/page/size/totalElements/totalPages/hasNext`).
        스프링 `Page`를 그대로 내리면 필드가 20개 가까이 쏟아지고 부트 3.x가 직렬화 경고를 남기며
        버전에 따라 형태가 바뀔 수 있어 응답 형태를 우리가 고정함.
      → 리포지토리 메서드명에서 `OrderBy...`를 제거(`findByOwnerId`, `findByVehicleId`) —
        메서드 이름 정렬과 `Pageable`의 `sort`가 공존하면 우선순위가 코드에서 안 보임.
        정렬은 컨트롤러의 `@PageableDefault(sort=...)` 한 곳에서만 정한다.
      → 차량은 `createdAt DESC`, 정비 이력은 `serviceDate DESC`가 기본. 클라이언트가
        `?page=&size=&sort=` 로 덮어쓸 수 있음.
      → `Page.map(Response::from)`으로 엔티티→DTO 변환 — 페이지 메타데이터를 다시 조립할 필요 없음.
      → 응답 형태가 배열에서 객체로 바뀌었으므로 프론트는 `res.items`를 봐야 함.

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

- **Phase 1 — 백엔드 마무리** (완료)
  프론트가 호출할 API 표면 완성. 단건 조회, 날짜 기준 다음 정비, 페이지네이션, CORS, API 문서화.
- **Phase 2 — 프론트엔드 프로젝트 셋업** (완료)
  `frontend/`에 Vite+React+TypeScript, Tailwind/shadcn, API 클라이언트, 세션 쿠키 연동 확인.
- **Phase 3 — 인증 화면** (완료)
  회원가입/로그인/로그아웃, 로그인 상태 전역 관리, 보호 라우트.
- **Phase 4 — 차량 관리 화면** (완료)
  차량 목록/등록/상세/주행거리 갱신/삭제.
- **Phase 5 — 정비 이력 관리 화면**
  이력 목록/등록/수정/삭제, 다음 정비 시점(주행거리+날짜) 표시.
- **Phase 6 — 다듬기**
  로딩/에러/빈 상태, 폼 검증 연결, 반응형, 포맷팅.

아래 체크리스트는 **지금 시점의 계획**이다. 프론트엔드는 아직 한 줄도 안 짜본 영역이라
막상 시작하면 순서나 범위가 바뀔 수 있다. 각 항목은 착수할 때 다시 쪼갠다.
체크리스트 안의 순서는 **의존 관계가 있는 것만** 순서를 뜻하고, 나머지는 우선순위가 아니다.

---

## Phase 1 (잔여) — 백엔드 마무리

Phase 1은 **완료**. 아래는 조건이 갖춰지면 재검토할 보류 항목뿐이다.

### 1-B. 보류 (조건이 갖춰지면 재검토)

- [ ] `@EnableJpaAuditing` 도입 검토
      → 지금 엔티티 3개(`User`/`Vehicle`/`MaintenanceRecord`)에서 `@PrePersist`/`@PreUpdate`가
        반복 중. 아직 견딜 만하다. **엔티티가 4개째 생기는 시점**에 규칙 7을 재검토한다.
      → 도입하면 `BaseTimeEntity`(`@MappedSuperclass` + `@EntityListeners`)로 상속 구조가 생긴다.
        상속이 생기면 "이 필드가 어디서 오는지" 눈에 안 보이는 게 단점.

---

## Phase 2 — 프론트엔드 프로젝트 셋업 (완료)

셋업 자체는 끝났고 CORS·세션 쿠키도 커맨드라인으로 검증했다. 남은 것은 사용자의 눈 확인 하나뿐:

- [ ] 브라우저에서 `http://localhost:5173` 을 열고 회원가입 → 로그인 → "내 정보" 순서로 눌러
      200 응답과 DevTools > Application > Cookies의 `JSESSIONID`를 직접 확인
      → 백엔드를 IntelliJ에서 띄우면 스키마가 `odolog`라 사용자가 없다. "회원가입"부터 누를 것.

---

## Phase 3 — 인증 화면 (완료)

- [ ] 브라우저에서 확인: 회원가입 → 자동 로그인 → 새로고침해도 유지 → 로그아웃 →
      `/vehicles` 직접 접근 시 `/login`으로 튕기는지

---

## Phase 4 — 차량 관리 화면 (완료)

- [ ] 브라우저에서 확인: 빈 목록 → 차량 등록 → 상세 진입 → 주행거리 갱신(감소 시 에러) → 삭제

---

## Phase 5 — 정비 이력 관리 화면

- [ ] `ServiceType` 한글 라벨 매핑 테이블 (`ENGINE_OIL` → "엔진오일")
      → 백엔드 enum 값(통신용)과 화면 표시 문자열을 분리한다. 한 곳에 상수로 모아두면
        셀렉트 박스/목록/카드에서 전부 재사용된다.
- [ ] 차량 상세 안에 정비 이력 목록 (`GET .../maintenance-records`)
      → 여기도 `PageResponse`. 기본 정렬은 `serviceDate DESC`(최신순).
- [ ] 이력 등록 폼 (`POST .../maintenance-records`)
      → 종류(셀렉트) / 설명 / 비용 / 정비 시점 주행거리 / 정비 날짜.
      → 날짜는 `YYYY-MM-DD` 문자열로 보낸다(백엔드 `LocalDate`). `Date` 객체를 그대로 JSON에
        넣으면 UTC 변환 때문에 하루가 밀리는 사고가 잦다.
      → `serviceOdometer` 기본값을 차량의 현재 `odometer`로 채워 주면 입력이 편하다.
- [ ] 이력 수정 (`PATCH .../maintenance-records/{recordId}`)
      → 백엔드가 부분 수정이므로 **바뀐 필드만** 보낸다.
      → 비용을 0으로 바꾸는 것과 안 보내는 것은 다르다(백엔드가 `Integer`로 구분). 폼에서 빈 값을
        `0`으로 만들지 않도록 주의.
- [ ] 이력 삭제 (`DELETE .../maintenance-records/{recordId}`) — 확인 후 204 처리
- [ ] 다음 정비 시점 카드 (`GET .../next-service?type=`)
      → 주행거리 기준과 날짜 기준을 나란히 표시. 둘 다 `null`일 수 있다
        (이력 없음 / `OTHER`처럼 권장 주기 없음) — "이력이 없어 계산할 수 없습니다"로 분기.
      → 현재 주행거리와 비교해 "곧 교체 필요"/"지남" 같은 상태 배지를 붙일지 결정.
        이 판단 로직을 프론트에 둘지 백엔드에 둘지는 그때 상의(백엔드에 두면 기준이 한 곳).
      → **주의**: 지금 API는 `type` 하나씩만 계산한다. 화면에서 5종류를 다 보여주려면 요청이
        5번 나간다. 실제로 불편하면 아래 백로그의 "전체 종류 한 번에" API를 추가한다.

---

## Phase 6 — 다듬기

- [ ] 3상태(로딩 / 에러 / 빈 목록) 처리를 모든 목록·상세 화면에 일관되게 적용
      → 이 3가지를 매번 손으로 쓰면 화면마다 모양이 달라진다. 공용 컴포넌트로 뽑는다.
- [ ] 로딩 표시 — 목록은 스켈레톤, 버튼은 비활성화 + 스피너
      → 폼 제출 버튼은 반드시 비활성화. 안 하면 더블 클릭으로 차량이 2대 등록된다.
- [ ] 에러 토스트 / 인라인 에러 구분 기준 정리
      → 폼 검증 실패(400/409)는 해당 필드 아래 인라인, 그 외(500 등)는 토스트.
- [ ] 백엔드 400 검증 응답과 폼 필드 연결
      → 현재 `ErrorResponse`는 `message` 하나뿐이라 **어느 필드가 틀렸는지 모른다.**
        필드별 표시가 꼭 필요하면 백엔드 `ErrorResponse`에 `fieldErrors`를 추가해야 한다
        (백로그 항목으로 이관).
- [ ] 숫자·날짜 포맷 — 주행거리 `45,000km`, 비용 `50,000원`, 날짜 `2026-07-01`
      → `toLocaleString('ko-KR')` 사용. 포맷 함수를 `src/lib/format.ts`에 모은다.
- [ ] 반응형 — 모바일 우선. 차량 목록은 모바일에서 카드, 데스크톱에서 그리드.
      → 차량 관리 앱은 실제로 정비소나 주차장에서 폰으로 볼 가능성이 높다.
- [ ] 접근성 기본 — 모든 `input`에 `label` 연결, 버튼에 접근 가능한 이름, 포커스 링 유지
- [ ] 페이지 타이틀(`document.title`)과 파비콘

---

## 완료 판정 기준 (Definition of Done)

아래 시나리오를 브라우저에서 처음부터 끝까지 막힘없이 수행할 수 있으면 "완성"이다.

- [ ] 회원가입 → 로그아웃 → 로그인
- [ ] 새로고침해도 로그인 상태 유지
- [ ] 차량 등록 → 목록에 보임 → 상세 진입
- [ ] 주행거리 갱신, 더 작은 값으로 갱신 시 알아들을 수 있는 에러
- [ ] 정비 이력 등록/수정/삭제
- [ ] 다음 정비 시점이 주행거리·날짜 두 기준으로 표시됨
- [ ] 차량 삭제 시 이력도 함께 사라짐
- [ ] 로그인 안 한 상태로 `/vehicles` 직접 접근 시 로그인 페이지로 이동
- [ ] 다른 계정으로 로그인했을 때 남의 차량이 안 보임
- [ ] 백엔드 테스트 전체 통과

---

## 백로그 — 프론트를 만들다 필요해질 백엔드 보강 (후보)

지금 당장 하지 않는다. 프론트에서 실제로 불편해지면 그때 꺼내 쓴다.

- [ ] `ErrorResponse`에 `fieldErrors` 추가 — 어느 필드가 왜 틀렸는지 프론트가 알 수 있게
- [ ] 다음 정비 시점을 **전체 종류 한 번에** 반환하는 API (`GET .../next-services`)
      → 화면 하나 그리는 데 요청 5번은 낭비. 다만 API를 늘리는 비용도 있으니 실제 필요할 때.
- [ ] 정비 이력 등록 시 `serviceOdometer`가 차량 `odometer`보다 크면 차량 주행거리 자동 갱신
      → 사용자가 두 번 입력하지 않게. 비즈니스 규칙이므로 `Vehicle` 엔티티 안에 둔다.
- [ ] 이메일 중복 확인 API (`GET /api/users/exists?email=`) — 회원가입 폼 실시간 피드백용
      → 단, 이건 계정 존재 여부를 노출하는 API다. 로그인 실패 메시지를 일부러 통일해 둔 것과
        모순되므로 **도입 전에 트레이드오프를 다시 따진다.**
- [ ] 비밀번호 변경 / 회원 탈퇴 API
- [ ] 정비 이력 종류별 필터링 (`GET .../maintenance-records?type=`)
- [ ] 차량 목록에 각 차량의 "임박한 정비" 요약 포함 (목록 화면에서 바로 보이게)

---

단계를 완료할 때마다 "진행 상황 (완료)" 섹션을 갱신하고, 해당 항목을 위 체크리스트에서 제거한다.
