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

문서 역할 구분 (2026-10-05 에 다섯으로 나눴다):

- `README.md` — 남이 이 저장소를 봤을 때 필요한 것. 소개, 기술 스택, 실행 방법, 구조, API 개요, 보안 요약, 트러블슈팅.
- `CLAUDE.md`(이 파일) — 지금 지켜야 할 것. 진행 방식, 개발 환경, 코드 설계 원칙, 구조 지도, 의존 방향.
- `docs/DESIGN.md` — 프론트엔드 디자인 시스템. 색·글자·모션·레이아웃 규칙.
- `docs/QA.md` — 로드맵, 눈 확인 체크리스트(Phase 6·7), 완료 판정 기준, 백로그.
- `HISTORY.md` — 작업 일지. 이미 끝난 것: 무엇을 왜 그렇게 정했는지. 최신순으로 쌓는다.

**경위("전에는 ~였다")는 `HISTORY.md` 에만 둔다.** 규칙 문서에는 지금 지킬 것과 짧은 이유만 남긴다 —
같은 설명이 두 곳에 있으면 한쪽만 고치게 되고, 그때 규칙 쪽이 먼저 낡는다.
구조는 **폴더 단위 지도**만 적는다. 파일마다 적은 목록은 2026-10-05 에 걷어냈다(파일이 옮겨질 때마다 낡았다).

## 기술 스택

### 백엔드
- Spring Boot 3.5.6 / Java 17 / Gradle
- Spring Data JPA (Hibernate 6.6.x), **Flyway**(스키마), **Spring Session JDBC**(세션을 DB 에)
- **MariaDB 12.3.2** (MySQL 아님 — 아래 주의사항 참고)
- `spring-security-crypto`(BCrypt)만 쓴다. Spring Security 는 쓰지 않는다 — 세션·CSRF·시도 제한·보안 헤더는 직접 만들었다
- 패키지 루트: `com.odolog.app`

### 프론트엔드 (`frontend/`)
- Node 26 (Homebrew, `.nvmrc`) / React 19 / Vite 8 / TypeScript 6
- Tailwind CSS v4 (`@tailwindcss/vite` 플러그인) + shadcn/ui (Base UI)
- **TanStack Query**(`@tanstack/react-query`) — 차량 상세 화면의 조회 캐시. 나머지 화면은 `useAsyncData`
- 테스트: vitest + Testing Library(jsdom), 실제 브라우저 E2E 는 Playwright
- 린터는 ESLint가 아니라 **oxlint** (Vite 템플릿 기본값, Rust 기반)
- 개발 서버 `http://localhost:5173` — 백엔드 CORS 허용 주소(`odolog.cors.allowed-origins`)와 짝이다
- 저장소 루트의 `frontend/`. 백엔드(Gradle)와 완전히 분리되어 있고 서로 빌드에 관여하지 않는다
- 번들이 500kB 를 넘어 `vite build` 가 경고를 낸다(2026-10-05, 508kB). 실패가 아니라 경고이고 아직 나누지 않았다

## 개발 환경

세팅은 끝났다. 매번 재확인하지 말 것. 단, **DB 접속이 실패하면 계정 문제부터 의심한다**
(아래 "DB 접속 시 주의" 참고 — 한 번 크게 막혔던 지점이다).

- DB: MariaDB, `localhost:3306`, 스키마 `odolog` (utf8mb4 / utf8mb4_unicode_ci). 테스트는 `odolog_test`
- **스키마는 Flyway 가 만든다**(`src/main/resources/db/migration`). Hibernate 는 `ddl-auto: validate` 로
  엔티티와 맞는지 확인만 하고, 다르면 기동을 막는다.
  **엔티티를 바꾸면 V5… 마이그레이션을 같이 더한다** — 안 더하면 `FlywayMigrationTest` 가 실패한다.
  그 테스트는 nullable·유니크 제약까지 엔티티와 대조한다(`SchemaDrift` 도우미). Hibernate 검증은 그 둘을 보지 않는다.
  운영 DB 는 첫 기동 때 V1 을 기준점으로 표시(`baseline-on-migrate`)하고 V2 부터 돌았다.
  **운영 스키마가 V1 과 같은지는 2026-10-06 에 한 번 대조했다**(임시 스키마에 V1~V3 를 돌려 컬럼·유니크·외래키 비교 — 일치).
  런타임 대조기는 없으므로, 운영 DB 를 손으로 고쳤다면 같은 방법으로 다시 대조한다.
- **Flyway 로 옮긴 이유는 `ddl-auto: update` 의 함정 셋이다** — 제약을 추가는 해도 지우지 않고(9/7 번호판 유니크),
  `@Enumerated` 의 네이티브 `enum(...)` 컬럼 타입을 바꾸지 않고(9/16), `CHECK (type in (…))` 값 목록을 갱신하지 않는다(9/29).
  셋 다 **테스트(create-drop)는 통과하고 운영만 안 바뀌는** 모양이었다. 경위와 손으로 고친 SQL 은 `HISTORY.md` 와
  README 의 "2026-10-03 이전 DB" 절에 있다. 같은 실수를 다른 도구로 되풀이하지 않게:
  - **컬럼 단위 CHECK 는 `DROP CONSTRAINT` 로 안 지워진다.** ddl-auto 가 붙인 CHECK 는 컬럼 정의 안에 있어서 이름이
    컬럼명과 같아 보여도 표 단위 제약이 아니다. `IF EXISTS` 는 못 찾아도 오류 없이 넘어가 **Flyway 는 성공으로 기록한다**
    (V2 가 그랬고 V4 가 `MODIFY COLUMN` 으로 다시 정의해 지운다). 제약을 지우는 마이그레이션은 테스트에서 **실제 모양 그대로**
    재현하고, 운영에 적용한 뒤 `information_schema.CHECK_CONSTRAINTS` 로 확인한다
  - 새 enum 컬럼은 `@Enumerated` 가 아니라 **`@Convert`(enum 안의 `Converter`)** 로 매핑한다(`User.language`·`unitSystem`)
  - 제약을 바꿨으면 실제 상태를 `SHOW CREATE TABLE` 로 확인한다(`SHOW INDEX` 는 복합 유니크가 FK 인덱스를 대신할 때 목록에서 사라진다):

        /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW CREATE TABLE vehicles\G"

- **앱이 쓰는 계정은 `odolog`@localhost**. `odolog.*` 에만 권한이 있다.
  비밀번호는 어떤 파일에도 적지 않는다 — IntelliJ 실행 구성의 환경변수 `DB_USERNAME` / `DB_PASSWORD` 에만 있다.
  `application.yml` 에는 `${DB_USERNAME:root}` / `${DB_PASSWORD:}` 형태로만 존재한다.
- 드라이버: `org.mariadb.jdbc:mariadb-java-client`, URL은 `jdbc:mariadb://`
- 실행: **IntelliJ IDEA**에서 `OdoLogApplication` 을 직접 실행한다.
  Gradle 래퍼(`./gradlew`)는 프로젝트에 있으므로 빌드 확인은 터미널에서도 가능하다.
- API 문서: 앱 실행 후 `http://localhost:8080/swagger-ui.html` (스펙 JSON은 `/v3/api-docs`).
- 터미널에서 앱을 띄워 확인해야 할 때는 테스트 계정을 쓴다 (운영 계정 비밀번호는 IntelliJ에만 있음):

      SPRING_DATASOURCE_URL='jdbc:mariadb://localhost:3306/odolog_test' \
      SPRING_DATASOURCE_USERNAME=odolog_test SPRING_DATASOURCE_PASSWORD=odolog_test ./gradlew bootRun

  (`odolog_test` 스키마는 테스트 실행 때마다 `create-drop`으로 초기화되므로 데이터가 남아도 무방하다.
  JDBC 유닉스 소켓 접속(`localSocket=`)은 시도해 봤으나 동작하지 않으니 시간 낭비하지 말 것.)
- E2E 용 백엔드는 `./gradlew bootTestRun`(테스트 설정, 18080). `npm run e2e` 가 알아서 띄운다

### DB 접속 시 주의

터미널의 `mysql` 명령어는 **MariaDB 클라이언트**이고, `~/.my.cnf` 에 오래된 비밀번호가
남아 있어 자동으로 전송된다. 그래서 `--no-defaults` 없이 접속하면 `Access denied` 가 난다.

    /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW TABLES;"

이 계정(`user@localhost`)은 **진단용으로만** 쓴다. 권한은 이렇게 되어 있다:

    GRANT ALL PRIVILEGES ON *.* TO `user`@`localhost`
      IDENTIFIED VIA mysql_native_password USING 'invalid' OR unix_socket

비밀번호 해시가 문자 그대로 `'invalid'` 라서 **비밀번호로는 절대 접속되지 않고**, 유닉스 소켓으로만
붙는다. JDBC는 TCP로 접속하므로 이 계정을 애플리케이션에 쓸 수 없다.
그래서 앱 전용으로 `odolog`@localhost 계정을 따로 만들었다:

    CREATE USER 'odolog'@'localhost' IDENTIFIED BY '<비밀번호>';
    GRANT ALL PRIVILEGES ON odolog.* TO 'odolog'@'localhost';

`*.*` 가 아니라 `odolog.*` 로 제한한 이유: 이 계정이 새어 나가도 다른 스키마(`for_125` 등)는
건드릴 수 없게 하기 위해서다.

### 탐색 시 무시할 경로

`.gradle/`, `.idea/`, `build/`, `frontend/node_modules/`, `frontend/dist/` 는 빌드·IDE 산출물이므로 읽지 않는다.

## 코드 설계 원칙

계속 지켜야 하는 규칙들. 어기려면 먼저 사용자에게 이유를 설명하고 동의를 받는다.
각 규칙이 생긴 경위는 `HISTORY.md` 에 날짜로 찾을 수 있다.

1. **Lombok을 쓰지 않는다.** 생성자·getter를 직접 작성한다. 어떤 코드가 생성되는지
   눈으로 보는 것이 학습 목적이기 때문. 도입할 만한 시점이 오면 그때 제안한다.
2. **setter를 열지 않는다.** 변경이 필요한 값만 `changeNickname()`, `updateOdometer()`
   처럼 의미 있는 이름의 메서드로 연다. 비즈니스 규칙은 엔티티 안에 둔다.
   주행거리는 의도가 셋이라 메서드도 셋이다: `updateOdometer()`(감소 시 409) · `liftOdometerTo()`(크면 올리고
   작으면 넘어감 — 정비·주유를 기록하다 따라오는 경로) · `correctOdometer()`(감소도 반영 — `force` 로만 닿는다).
3. **JPA 기본 생성자는 `protected`** 로 좁힌다.
4. **연관관계는 단방향으로 시작한다.** `User` 에 `@OneToMany` 를 넣지 않았다.
   DB 구조가 동일하고 양방향은 동기화 부담이 크기 때문. 필요해지면 그때 검토한다.
5. **`@ManyToOne` 에는 항상 `fetch = FetchType.LAZY`** 를 명시한다 (기본값이 EAGER라 N+1 유발).
   `optional = false` 와 `@JoinColumn(nullable = false)` 를 짝으로 쓴다.
6. **제약조건에는 이름을 직접 붙인다.** (`uk_vehicles_user_plate_number`, `fk_vehicles_user`)
   Hibernate가 짓는 해시 이름은 로그 추적이 불가능하다. 이름이 있어서 예외 처리기가 **제약 이름으로 409 코드를
   고르고**(`uk_users_email` → `EMAIL_DUPLICATE`), `FlywayMigrationTest` 가 유니크 제약을 이름으로 양방향 대조한다.
7. **시간 필드는 `BaseTimeEntity` 를 상속해서 얻는다.** `createdAt` 에는 `updatable = false`.
   새 엔티티는 `extends BaseTimeEntity` 만 하면 된다. 스위치는 `common/config/JpaAuditingConfig` 이고,
   **`@DataJpaTest` 는 그걸 자동으로 집어 가지 못하므로 리포지토리 테스트에 `@Import(JpaAuditingConfig.class)` 가 필요하다.**
   `OdoLogApplication` 에 두면 `@WebMvcTest` 가 전부 깨진다(JPA 메타모델이 비어 있음).
8. **타입 선택**: "없음"이라는 상태가 존재하는 값만 래퍼 타입(`Integer`), 아니면 기본형(`int`).
   PK는 저장 전 `null` 구분을 위해 항상 `Long`.
   **모르는 값은 0 이 아니라 `null` 이다** — 주유의 주유량·금액, 정비의 비용·그때 주행거리. 0 은 "0원에 했다"는
   다른 사실이고 연비를 0 으로 나누게 만든다. 더할 때만 `totalCostOrZero()`·`costOrZero()` 로 0 취급.
   부분 수정에서 "안 보냄"과 "비움"을 가르려면 `clearXxx` 플래그를 따로 둔다 — JSON 은 키 없음과 `null` 이
   서버에 똑같이 도착하고, `Optional` 로 감싸도 Jackson 이 키가 없을 때 `Optional.empty()` 를 채운다.
9. **테이블명은 복수형** (`users`, `vehicles`). `user` 는 예약어라 반드시 `users`.
9-1. **숫자 PK 는 서버 밖으로 내보내지 않는다.** URL·API 에는 12자 무작위 `public_id`(`common/domain/PublicId`)가 나간다.
    1,2,3… 은 남의 차를 못 열어도 **서비스 규모와 등록 순서**를 말한다.
    PK 를 UUID 로 바꾸지 않은 이유: 외래키가 따라 바뀌고, InnoDB 는 PK 순서로 행을 저장해서 무작위 PK 는
    넣을 때마다 중간에 끼워 넣는다. 인코딩(Hashids/Sqids)은 되돌릴 수 있어 숨기는 게 아니다.
    서비스는 공개 id 로 `findOwnedVehicle` 을 부른 뒤로는 `vehicle.getId()` 만 쓴다.
    기록은 `findByPublicIdAndVehicleId` — 공개 id 를 알아도 다른 차량 경로로는 못 건드린다.
    **정렬에는 공개 id 를 쓰지 않는다**(무작위라 "나중에 넣은 것"을 말하지 못한다) — 동점은 서버 안에서 숫자 id 로 가른다.
    **사용자 id 는 응답에 아예 싣지 않는다** — URL 에 안 나오고 화면이 쓰는 곳도 없다.
10. **로그인한 사용자 식별은 세션에서만 한다.** 요청 바디나 URL의 사용자 ID는 클라이언트가
    조작할 수 있으므로 신뢰하지 않는다 (`SessionConst.LOGIN_USER_ID`, `@LoginUser`).
11. **예외는 의미에 맞는 상태 코드로 세분화한다**: 400(입력 검증 실패) / 401(미인증) /
    403(권한 없음) / 404(리소스 없음) / 409(리소스 중복·동시 수정 `CONCURRENT_UPDATE`) / 413(본문이 너무 큼) /
    429(시도 과다). 405·415 는 스프링이 정한 그대로 둔다(`GlobalExceptionHandler` 가 `ResponseEntityExceptionHandler`
    를 이어받아 상태 코드는 부모에게 맡기고 본문만 우리 모양으로 바꾼다).
    서버 쪽 불변식이 깨진 경우(예: 세션엔 있는데 DB엔 없는 사용자)는 일부러 핸들러를 만들지 않고 500으로 흘려보내
    로그에 남긴다. **다만 500 에도 `ErrorResponse` 본문은 준다** — 스프링 기본 응답엔 `message` 가 없어 화면이
    "요청에 실패했습니다 (HTTP 500)" 밖에 말하지 못한다. 예외 문구는 내보내지 않는다(내부 사정이 실린다).
    **남의 자원에는 403 이 아니라 404 를 준다.** 403 은 "권한이 없다"와 동시에 **"있긴 하다"** 를 말한다.
    **문구까지 같아야 한다** — 상태 코드만 맞추고 메시지가 다르면 그 메시지가 대신 알려준다.
    그래서 지금 `ForbiddenAccessException` 을 던지는 곳은 없다. 타입과 핸들러는 남겨 뒀다 —
    소유자가 아니어도 볼 수는 있는 자원(예: 공유받은 차량)이 생기면 그때가 진짜 403 이다.
12. **예외는 전용 타입으로 던진다.** `IllegalArgumentException` 같은 JDK 범용 예외를 핸들러에
    매핑하지 않는다. 우리가 안 던진 예외까지 잡혀서 500이어야 할 것이 조용히 4xx로 나간다.
    상태 코드 하나당 예외 클래스 하나(`InvalidRequestException`(400)/`AuthenticationFailedException`(401)/
    `ForbiddenAccessException`(403)/`ResourceNotFoundException`(404)/`ConflictException`(409)/
    `TooManyRequestsException`(429)). 공통 부모는 `ApiException`(규칙 16).
    스프링·JPA 가 던지는 것 중 뜻이 분명한 것은 핸들러가 직접 번역한다 — 유니크 위반(409, 제약 이름으로 코드.
    원인 사슬을 끝까지 본다), 동시 수정·데드락(`ConcurrencyFailureException` → 409 `CONCURRENT_UPDATE`),
    잘못된 sort(`PropertyReferenceException` → 400). 유니크가 아닌 `DataIntegrityViolation` 은 500.
    `HttpMessageNotReadable`·`MethodArgumentNotValid` 는 `@ExceptionHandler` 가 아니라 **부모 메서드 덮어쓰기**다 —
    둘 다 두면 기동이 실패한다.
13. **서비스는 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.**
    메서드 쪽이 클래스 쪽을 덮어쓴다. 새 메서드를 깜빡했을 때 기본이 안전한 쪽(읽기 전용)이라
    쓰기가 실패해서 바로 드러난다. 반대로 하면 아무 일도 안 일어나 영영 모른다.
    **예외는 하나 — `user/LoginService` 는 트랜잭션을 걸지 않는다**(2026-10-05). 로그인은 세션을 저장한 **뒤**
    비밀번호가 그대로인지 다시 보는데, 그 재확인이 **새 트랜잭션**이어야 그 사이 커밋된 재설정·변경·탈퇴가 보인다
    (REPEATABLE READ 는 첫 조회 때 스냅숏을 잡는다). 클래스에 읽기 전용을 걸면 재확인이 같은 스냅숏을 다시 읽는다.
    **잠금이 필요한 경로는 첫 조회를 잠금 조회로 한다**(`findOwnedVehicleForUpdate`·`findByIdForUpdate`·`findLockedBy…`).
    기록 쓰기와 차량 삭제, 동시 가져오기, 같은 주소의 재설정 발급이 그렇다. 첫 조회여야 기다린 뒤에 앞선 쪽의 결과가 보인다.
    잠금 순서는 **사용자 행 → 차량·토큰 행**으로 맞춘다(반대면 데드락).
14. **주석은 한 줄 명사구로 쓴다.** 종결어미(`~한다` / `~이다`)를 붙이지 않고
    명사나 명사구로 끝낸다. `<b>` · `<p>` 같은 Javadoc 태그도 쓰지 않는다.

        // 계기판 값이 더 최신이면 차량 쪽도 갱신
        // 이력 먼저, 차량 나중 — FK 제약
        /** 정렬 고정. sort 파라미터 무시 — 연비 계산의 전제 */

    **무엇을 하는지 + 건드리면 안 되는 이유**까지만 담는다. 긴 논증과 대안 비교는
    이 문서에 있으므로 코드에서 되풀이하지 않는다. 길어야 세 줄이고, 그보다 길어지면 그건 설계 기록이라 여기로 옮긴다.
    경위("전에는 ~였다")·날짜·줄표(—) 연쇄·굵게·⚠️ 같은 기호는 주석에 쓰지 않는다. 줄표가 꼭 필요하면 한 번만.
14-1. **입력 문자열은 서비스가 `common/validation/InputText` 로 정리한다.**
    - 선택 입력(메모·정비 설명 등)은 `InputText.optional` — 앞뒤 공백을 지우고, 비면 `''` 가 아니라 `null`,
      짝 없는 서로게이트는 `?` 로(DB 가 그렇게 저장해서, 안 바꾸면 비교 열쇠가 어긋난다).
      그대로 두면 "없음" 이 두 모양이 되고 **내보낸 JSON 에도 그 차이가 그대로 나간다.**
    - 필수 입력(번호판·제조사·모델명·닉네임)은 `InputText.required` — 지운 뒤 비면 400.
      `@NotBlank`(trim 기준)와 `\S` 는 전각 공백을 글자로 보고 통과시키므로 서비스에서 한 번 더 본다.
    - `trim()` 이 아니라 `strip()` — 전각 공백(U+3000)까지 지운다.
    - 자르는 자리는 서비스다 — DTO 접근자에서 자르면 부분 수정에서 "안 보냄"과 "지움"이 같아진다.
    - **"같은 번호판인가"는 DB 가 판단한다.** DB(unicode_ci)는 뒤 공백·대소문자·전각 숫자·악센트까지 같게 보는데,
      자바로 그 정렬 규칙을 흉내 내면 언젠가 갈린다. 차량 수정은 `existsBy…AndIdNot`(자기 자신 제외),
      가져오기는 `findLockedByOwnerIdAndPlateNumber` 로 묻는다.
15. **계정 존재 여부를 응답으로 알려주지 않는다 — 단, 회원가입은 예외다.**
    - 로그인은 실패 사유를 통일한다(없는 이메일도 "이메일 또는 비밀번호가 올바르지 않습니다").
      없는 계정의 실패도 횟수를 센다 — 안 세면 "빨리 답하는 쪽"이 곧 없는 계정이다.
    - **응답 시간도 같게 맞춘다.** 없는 이메일이어도 `dummyHash` 와 BCrypt 비교를 한 번 돌린다.
      `dummyHash` 는 같은 인코더(`common/config/PasswordEncoderConfig` 의 빈 하나)로 만든다 — 상수로 박으면 강도를 바꿀 때 혼자 옛 비용에 남는다.
    - 비밀번호 재설정은 가입 여부와 무관하게 204 를 주고 **메일 발송 실패까지 삼킨다**.
      **요청 스레드는 횟수만 센다** — 토큰 조회·저장·메일은 다른 스레드(대기 200개 상한, 넘으면 조용히 버림).
      그 자리에서 하면 가입된 주소만 DB 쓰기만큼 늦게 답한다.
    - **이메일은 출력 가능한 ASCII 만 받는다**(`InputLimits.EMAIL_CHARS`, 가입·로그인·재설정). DB(unicode_ci)는
      `kím@x.com` 을 `kim@x.com` 과 같게 봐서, 받으면 철자만 바꾼 주소가 같은 계정으로 로그인되면서 시도 횟수는 따로 세어진다.
    - **시도는 비교 전에 센다**(`acquire` 가 확인과 집계를 한 번에). 둘이 따로면 BCrypt 동안 동시 요청이 전부 확인을 통과한다.
    - **회원가입만 409 로 존재를 알려준다.** 가입하려는 사람에게 "이미 있습니다"는 맞는 안내이고, 없애려면
      가입을 메일 확인 흐름으로 바꿔야 하는데 **메일 설정이 없으면 아무도 가입을 끝내지 못한다.**
      대신 한 곳에서 주소를 쓸어 보는 것을 **IP 를 키로** 막는다(이메일로 세면 매번 다른 주소를 넣는 열거자는 늘 1이다).
      성공한 가입도 센다 — 409 만 세면 아직 없는 주소를 찔러 보는 쪽이 안 걸린다.
    - **`X-Forwarded-For` 는 읽지 않는다** — 보내는 쪽이 적는 값이라, 그 값을 덮어써 주는 프록시가 앞에 있기 전에는
      헤더 한 줄로 제한을 빠져나가게 만들 뿐이다. IPv6 는 **앞 64비트로 묶어** 센다(`ClientIp`).
    - **리미터 키에는 반드시 용도 접두사를 붙인다.** 접두사가 없던 로그인 키에 `password-check:1` 을 넣어
      남의 비밀번호 변경·탈퇴를 잠글 수 있었다. 지금은 `login:`·`login-ip:`·`password-reset:`·`password-reset-ip:`·
      `signup:`·`password-check:` 여섯이다. 로그인 IP 키는 여러 사람이 한 IP 를 쓰므로 한도가 다섯 배(`acquireShared`).
      잠겼을 때의 문구는 부르는 쪽이 넘긴다(안 그러면 가입 화면에 "로그인 시도가 너무 많습니다"가 뜬다).
    - **로그인은 세션을 저장한 뒤 비밀번호가 그대로인지 다시 본다**(`LoginService`, 규칙 13 의 예외). 맞춰 보는 60ms 사이에
      재설정·변경·탈퇴가 끝나면 그쪽의 세션 끊기를 이미 지나친 세션이 14일 남는다. 그래서 세션은 `flush-mode: immediate`.
    - 비밀번호 변경은 **지금 세션만 남기고**, 재설정·탈퇴는 **전부** 끊는다(`LoginSessionRegistry`). 변경하면 재설정 링크도 지운다.
      세션 종료·잠금 해제는 **커밋 뒤**(`afterCommit`) — Spring Session JDBC 는 세션 표를 별도 트랜잭션으로 다룬다.
16. **오류는 코드로 말한다.** 우리 예외는 전부 `ApiException` 을 이어받고
    생성자가 `ErrorCode` 를 **필수로** 받는다. 응답은 `{ code, message, field?, retryAfterMinutes? }` 이고
    화면은 `code` 로 자기 언어의 문구를 고른다. `message` 는 로그·개발자용 한국어 원문이라
    **화면에 그대로 내보내지 않는다**(영어 사용자에게 한국어가 뜬다).
    코드를 더하면 세 곳을 같이 고친다: 백엔드 `ErrorCode`, 프론트 `shared/api/types.ts` 의 `ERROR_CODES`, 사전 두 벌의
    `errors.codes` — 마지막은 `errorMessage.test.ts` 가 빠진 것을 잡는다.
17. **화면에 문구를 직접 적지 않는다.** 문구는 `shared/i18n/messages/{ko,en}.ts` 에만 있고
    화면은 `useI18n().t` 로 읽는다. 숫자·금액·날짜·거리·연비는 **반드시 `f`** 로 — `toLocaleString()`·
    `toFixed()` 로 직접 적으면 그 자리만 한국어 표기·km 로 남는다.
    **저장 단위는 km·L·통화의 최소 단위다.** 변환은 화면이 하고, 서버의 계산·임계값(연비 50km/L·주행거리 상한·
    정비 주기)은 km·L 기준 그대로다. 단위 체계는 네 값(`KM_PER_L`·`L_PER_100KM`·`MPG_US`·`MPG_UK`)이고 거리·부피·연비를
    따로 고르지 않는다. **통화 코드(ISO 4217)는 사용자가 아니라 기록마다** 붙는다 — 사용자에만 두면 설정을 바꾸는 순간
    옛 50,000원이 $500.00 이 된다. 합계는 사용자 통화 기록만 더하고 나머지는 건수로 밝힌다.
    입력칸은 화면 단위(마일·갤런·달러)로 받고 저장 직전에 바꾼다. **손대지 않은 칸은 저장값을 그대로 보낸다** —
    마일로 바꿨다 되돌리면 1km 가 어긋나 아무것도 안 고친 수정이 주행거리를 바꾼다.
    **"오늘"은 사용자 시간대로 계산한다**(`user/UserToday`). 미래 날짜 판정·정비 지남·홈 월별 12칸이 공유한다 —
    한 곳이라도 `LocalDate.now()` 로 남으면 화면마다 오늘이 갈린다. JVM 시간대(`Asia/Seoul`, `OdoLogApplication`)는
    `createdAt` 같은 기록 시각에만 남는다. 바꾸면 저장된 시각의 뜻이 9시간 밀리므로 따로 떼어 결정한다.
18. **계산 결과는 저장하지 않고 읽을 때 계산한다**(연비·단가·구간 거리·다음 정비·지남/곧). 앞 기록이 바뀌면
    뒤 기록의 값이 따라 바뀌기 때문이다. 같은 계산을 두 화면이 쓰면 **한 곳에 둔다** — `fuel/domain/FuelEfficiency`·
    `FuelAnomaly`(둘 다 `FuelSegment` 의 구간 순회를 쓴다), `maintenance/domain/NextService` 를 차량 상세·목록·홈이 공유한다.
    두 벌이면 한 화면은 지났다 하고 다른 화면은 아무 말도 안 한다. 판정을 화면이 아니라 서버가 하는 이유도 같다.
19. **목록 정렬은 화이트리스트다**(`common/dto/SortGuard`). Spring Data 는 `?sort=owner.password` 처럼 연관을
    타고 들어가는 정렬을 그대로 받는다. 통과하면 같은 방향의 `id` 를 보조 키로 붙인다(동점 페이지 겹침 방지).
    **주유 목록은 sort 를 받지 않는다** — 정렬(`odometer DESC, id DESC`)이 곧 연비 계산의 전제라 서버가 고정한다.
    페이지 크기 상한은 100(`spring.data.web.pageable.max-page-size`).
20. **입력 상한은 "되돌릴 수 없게 망가지는 것"을 막으려고 둔다**(`common/validation/InputLimits`, 프론트 `shared/lib/limits.ts` 와
    같은 숫자 — 의도한 중복). `liftOdometerTo` 가 최댓값을 잡아 두므로 한 번 크게 잘못 넣으면 그 뒤 모든 폼이 그 값을 기준으로 말한다.
    비밀번호는 글자 수가 아니라 **UTF-8 72바이트**(`@MaxBytes`, BCrypt 한계) — 가입·변경·재설정 셋이 같아야 한다.

## 디자인 시스템

**`docs/DESIGN.md` 에 있다.** 라이트/다크 모노톤, 색은 역할 토큰으로만, 각진 모서리, 시스템 폰트와 타입 스케일 토큰,
곡선 하나에 시간으로 위계, "움직일 이유가 있을 때만 움직인다". 화면을 고치기 전에 읽는다.

## 현재 구조

**백엔드·프론트엔드 모두 "기능별(package-by-feature)"로 나눈다.** 기능이 늘어도 한 기능을 고칠 때 그 폴더 하나만 보면
되는 대신, **기능 간 경계를 넘는 import가 드러난다**(`Vehicle`이 `User`를 참조하듯). 그건 숨기지 않는다 — 아래 "의존 방향"이 정한다.

### 저장소 루트

    odolog/
    ├── build.gradle · settings.gradle · gradlew   의존성(springdoc 은 서드파티라 버전을 직접 명시)
    ├── .github/workflows/ci.yml        main 푸시와 PR 마다 백엔드·프론트·E2E. 백엔드 잡은 MariaDB 컨테이너를 띄운다 —
    │                                   H2 로 바꾸면 스키마가 운영과 달라져 검증이 거짓말을 한다
    ├── CLAUDE.md · README.md · HISTORY.md · LICENSE(MIT)
    ├── docs/                           DESIGN.md(디자인 시스템) · QA.md(체크리스트·로드맵·백로그)
    ├── src/                            백엔드 (Spring Boot)
    └── frontend/                       프론트엔드 (Vite + React)

### 백엔드 — 기능 하나의 모양

**모든 기능이 같은 모양이다** (2026-10-05, 사용자 결정). 폴더에 파일이 하나뿐이어도 만든다 — 폴더 수를 줄이는 것보다
**어느 기능을 열어도 같은 자리에 같은 것이 있는 것**이 낫다. (10-03 의 "형제가 생겼을 때만 폴더" 기준을 뒤집었다.
기능마다 깊이가 달라 매번 찾아야 했고, 두 번째 파일이 생길 때마다 첫 파일을 옮겨야 했다.)

    <feature>/
    ├── domain/            엔티티 · enum · 값 계산(엔티티가 아닌 계산도 여기 — NextService, FuelEfficiency)
    ├── dto/request/       클라이언트가 보내는 것. 검증 애노테이션은 여기에만
    ├── dto/response/      서버가 돌려주는 것. from() 팩토리
    ├── XxxRepository.java
    ├── XxxService.java
    └── XxxController.java          리포지토리·서비스·컨트롤러는 기능 폴더 바로 아래, 평평하게

조율 층(`garage`·`account`·`summary`)도 같은 틀이고, 엔티티가 없으면 `domain/` 이 없다.
**테스트는 대상과 같은 경로를 따라간다**(`user/UserServiceTest`, `fuel/domain/FuelEfficiencyTest`).

### 백엔드 — 폴더 지도 (`src/main/java/com/odolog/app/`)

    OdoLogApplication.java   컴포넌트 스캔 기점. 앱 시간대 Asia/Seoul 고정(run() 보다 먼저 — 커넥션 풀이 뜰 때 읽는다)
    user/          회원가입·로그인(LoginService)·프로필·설정 넷(언어·시간대·통화·단위)·비밀번호 변경·재설정·UserToday
    vehicle/       차량 등록·조회·수정·주행거리. user 밖의 기능을 모른다. Vehicle 에 @Version(동시 수정 409)
    maintenance/   정비 이력 · 정비 종류 15종(ServiceType) · 차량별 권장 주기(ServiceInterval) · 다음 정비/지남/곧(NextService)
    fuel/          주유 기록 · 연비(FuelSegment → FuelEfficiency, FuelAnomaly) · 연비 초기화 기준점
    garage/        조율 ⓪ — 차량 목록(지남·곧 수, 리포지토리 주입) · 차량 삭제(정비·주유 → 차량 순서, 서비스 주입).
                   URL 은 /api/vehicles 인데 패키지는 garage
    account/       조율 ① — 탈퇴(서비스 주입) · 내보내기(리포지토리) · 가져오기(RestoreKeys 로 중복 판정).
                   URL 은 /api/users/me/… 인데 패키지는 account — user 에 두면 순환
    summary/       조율 ② — 홈 요약 GET /api/summary(HomeSummary*). 읽어서 합치기만 해서 리포지토리 주입. 쿼리 4번
    common/
      auth/        세션(LoginUser·SessionConst·리졸버) · CSRF(double submit) · 시도 제한 · 세션 끊기 · ClientIp
      config/      WebConfig(리졸버 + CORS 필터) · JpaAuditingConfig · OpenApiConfig · PasswordEncoderConfig(BCrypt 빈 하나)
      domain/      BaseTimeEntity · PublicId
      dto/         SortGuard · response/(ErrorResponse · PageResponse)
      exception/   ErrorCode · GlobalExceptionHandler · type/(상태 코드당 예외 하나)
      validation/  InputText · InputLimits · MaxBytes · CurrencyCode(ISO 4217 + 옛 기록의 KRW)
      web/         요청마다 도는 필터 — SecurityHeadersFilter · RequestSizeLimitFilter

`common` 은 기능별로 나누지 않는다. 모든 기능이 쓰는 것이라 어느 기능으로 옮겨도 잘못된 방향의 의존이 생긴다.

**건드리기 전에 알아야 할 자리**(이유가 코드 밖에 있는 것만):

- **필터 순서**: CORS 필터가 맨 앞(CSRF 403 에도 CORS 헤더가 붙어야 브라우저가 "연결 실패"로 읽지 않는다),
  `RequestSizeLimitFilter` 가 그 바로 뒤(`FormContentFilter` 가 폼 본문을 상한 없이 먼저 읽지 않게). 10MB, 넘으면 413
- **CSRF 는 경로와 무관하게 모든 쓰기 요청을 본다** — 날 URI 로 `/api/` 를 판정하면 `/%61pi/…` 가 통과한다.
  테스트에서는 `odolog.csrf.enabled=false` 로 꺼 둔다(`@WebMvcTest` 가 Filter 빈을 같이 올린다). 필터는 `CsrfTokenFilterTest` 가 직접 본다
- **시도 제한(`LoginAttemptLimiter`)은 인메모리**다. 재시작하면 잊고, 맵이 10만 개를 넘으면 잠기지 않은 키 중 가장 오래 쉰 것부터 버린다.
  한도는 `odolog.rate-limit.max-attempts`(E2E 만 늘린다)
- **세션은 DB**(Spring Session JDBC, 표는 V3). 쿠키 이름 `JSESSIONID`, 14일 — `timeout` 과 `cookie.max-age` 를 **같이** 늘려야 한다
- **가져오기(`AccountRestoreService`)는 조율 층 규칙의 예외**로 리포지토리에 직접 쓴다 — 기록 2만 건을 한 트랜잭션에 넣는
  일괄 작업이라 건마다 서비스를 거치면 같은 조회가 2만 번 돈다. 대신 등록과 같은 규칙(미래 날짜 금지 `UserToday.rejectFuture`,
  주행거리 올리기, 통화 판정, `InputText.optional`)을 그 안에서 다시 적용한다. **등록 규칙을 바꾸면 여기도 같이 본다.**
  첫 조회가 사용자 행 잠금, 이미 있던 기록과만 비교(파일 안끼리는 비교하지 않음), 정비는 날짜 오름차순으로 넣는다
- **재설정 메일**: 링크는 프런트 주소(`odolog.app.base-url`). 발송은 커밋 뒤 다른 스레드, SMTP 시간 제한 5초.
  실패 시 `odolog.mail.log-link-on-failure` 가 켜져 있으면 링크를 WARN 으로(로컬 true, 운영 false — 토큰이 로그에 남으면 안 된다)
- **차량 행 잠금**: 정비·주유의 등록·수정·삭제, 주기 변경, 차량 정보 수정·주행거리 갱신, 차량 삭제가 첫 조회로
  `findOwnedVehicleForUpdate` 를 부른다. 안 잠그면 삭제 중에 들어온 기록 때문에 마지막 차량 DELETE 가 FK 로 500 이다.
  **사용자 행 잠금**(`findByIdForUpdate`): 비밀번호 변경·탈퇴·가져오기의 첫 조회
- **내보내기·가져오기**: 내보내기는 계산값(연비·단가)과 비밀번호 해시를 담지 않고 **차량별 주기는 담는다**(빠지면 복원한 차의
  `지남` 이 기본 주기로 돌아간다). 가져오기는 **사용자 정보를 받지 않는다** — 내 계정에 기록을 더할 뿐이다.
  탈퇴 비밀번호는 URL 이 아니라 본문에(URL 은 로그에 남는다)
- `PasswordResetTokenRepository` 의 두 삭제는 **`@Modifying` DELETE 한 문장** — 메서드 이름 파생 삭제는 읽은 뒤 한 줄씩 지워
  동시 요청이 같은 행을 지울 때 충돌한다
- 이미 읽은 `User` 가 있으면 `UserToday.of(User)` — 같은 요청에서 사용자를 두 번 읽지 않게.
  홈 요약·내보내기는 "오늘"·`exportedAt` 을 밖에서 받는다(테스트에서 고정하려고). 홈 요약은 차량 이름을 LAZY 프록시가 아니라
  이미 읽은 목록에서 찾는다
- 테스트 `application.yml` 에 `spring.mail.host` 가 있어야 한다 — 없으면 `JavaMailSender` 빈이 안 생겨 `@SpringBootTest` 가 못 뜬다

### 백엔드 — 리소스와 테스트

    src/main/resources/application.yml       개발 설정. ddl-auto=validate, open-in-view=false, 세션 14일·immediate,
                                             SQL·bind 로깅(개발 전용 — 이메일·해시까지 찍는다), 메일·CORS·리미터 설정
    src/main/resources/application-prod.yml  SPRING_PROFILES_ACTIVE=prod 로 겹친다. 문서·SQL 로깅을 끄고 쿠키 secure.
                                             DB_USERNAME·DB_PASSWORD·APP_BASE_URL·CORS_ALLOWED_ORIGINS 는 기본값이 없어 빠지면 기동 실패
    src/main/resources/db/migration/         V1 기준점 · V2 정비 종류 CHECK 삭제(표 단위 — 운영에선 효과 없음) · V3 세션 표 ·
                                             V4 정비 종류 CHECK 삭제(컬럼 단위, 실제로 지움). 다음은 V5
    src/test/resources/application.yml       odolog_test, create-drop, Flyway 끔(세션 표만 V3 를 sql.init 으로), CSRF 끔

**테스트 설정은 운영 `application.yml` 을 통째로 가린다** — 테스트가 운영 DB 에 붙을 길을 아예 없애려고 일부러 그랬다.
`application-test` 프로파일로 겹쳐 쓰는 방법은 2026-10-05 에 다시 따져 보고 버렸다 — 겹쳐 쓰면 운영 설정이 먼저 읽히고,
테스트 쪽에서 DB 주소 한 줄만 빠져도 테스트(create-drop)가 운영 DB 에 붙는다.
대가로 세션·쿠키·페이지 상한처럼 양쪽이 같아야 하는 키는 두 번 적고 **`ConfigParityTest` 가 같은지 본다.**

테스트 종류 (`./gradlew test`, 364개):

- Mockito 단위 테스트 · `@WebMvcTest`(서비스는 `@MockitoBean`) · `@DataJpaTest`(`@Import(JpaAuditingConfig.class)`)
- **`@SpringBootTest` 여덟** — Mockito 는 스프링 프록시를 안 거치므로 `@Transactional` 이 적용되지 않고, `@WebMvcTest` 는
  진짜 서비스가 돌지 않는다. 트랜잭션·잠금·FK·데드락·마이그레이션은 이것들만 본다. 잠금 테스트들은 **잠금을 빼고 돌려
  실패하는 것까지 확인**하고 넣었다
- 루트의 `FlywayMigrationTest`(빈 스키마·운영 경로 + `SchemaDrift` 로 nullable·유니크 대조와 일부러 어긋낸 네 경우) ·
  `ConfigParityTest` · `DependencyDirectionTest` · `TestOdoLogApplication`(E2E 용 백엔드)

### 프론트엔드 — 폴더 지도 (`frontend/src/`)

프론트는 **"폴더는 형제가 생겼을 때, 또는 소유자가 다를 때만"** 기준을 그대로 쓴다(백엔드만 10-05 에 바꿨다).
화면이 여럿인 기능은 `pages/`, 하나면 기능 폴더 바로 아래(`account/ProfilePage.tsx`).
기능 폴더는 `api/`(`endpoints.ts` + 그 기능의 DTO `types.ts`)와 `pages/` 또는 `components/`(다른 화면에 얹히는 조각)로 나뉜다.

    main.tsx        진입점. ThemeProvider > QueryClientProvider > BrowserRouter > AuthProvider > I18nProvider > App
    index.css       디자인 토큰 전부(docs/DESIGN.md). 테스트 파일은 @source not 으로 Tailwind 스캔에서 뺀다
    app/            조립층 — 라우트 12개(App), HomePage(비로그인 랜딩 / 0대 등록 권유 / 대시보드 갈림), LandingPage,
                    LegalPage, ProtectedRoute, I18nProvider(사용자를 알아야 해서 여기), layout/(Header·Footer·AuthLayout)
    features/
      auth/         로그인·가입·재설정 화면, AuthContext(세션 복구·로그인·로그아웃·탈퇴·401)
      account/      프로필 화면(/me)과 그 구역 — 계정·비밀번호·언어/단위·화면·내보내기/가져오기·탈퇴
      vehicles/     목록·등록·상세. 상세가 정비·주유 카드를 얹는다. OdometerHero·OdometerForm·VehicleInfoForm·GettingStartedCard
      maintenance/  정비 이력 목록·폼·빠른 정비·다음 정비 카드(자기 라우트 없음)
      fuel/         주유 목록·폼·연비 카드(자기 라우트 없음). sort 를 보내지 않는다
      summary/      홈 대시보드(Dashboard)·차트(HomeCharts·niceMax). GET /api/summary 한 번 — 계산은 서버가 한다
    shared/
      api/          client(fetch 래퍼·ApiError·401 전역 처리) · types(PageResponse·ErrorResponse·ERROR_CODES) ·
                    queryClient · queryKeys
      i18n/         사전 두 벌(messages/ko·en — en 이 Messages 타입이라 모양이 다르면 컴파일 실패) · useI18n · errorMessage
      lib/          units·money·format·preferences·odometer·limits · renderWithProviders(테스트) · hooks/
      theme/        라이트/다크/시스템. ui 와 같은 층이라 ThemeToggle 은 Button 대신 평범한 <button>
      ui/           cn.ts(프로젝트 cn — docs/DESIGN.md) · base/(shadcn 자리) · form/ · layout/(Page·Section) · state · pagination · mark
    dependency-direction.test.ts

**건드리기 전에 알아야 할 자리**:

- **조회 캐시**: 차량 상세는 TanStack Query 를 쓴다. 키는 전부 `shared/api/queryKeys.ts` 의 `['vehicles', id, …]` 아래이고,
  저장·삭제 뒤에는 `invalidateAfterMaintenance` / `invalidateAfterFuel` 이 다시 읽을 것을 한 곳에서 정한다
  (주유가 주행거리를 올리면 지남 판정도 바뀌어 다음 정비까지). 기능끼리 import 하지 않으려고 shared 에 둔다.
  **재조회를 위해 `key` 를 바꿔 컴포넌트를 다시 만들지 않는다** — 다시 만들면 로딩이 다시 뜨고 열어 둔 폼과 쪽이 사라진다.
  다시 읽는 동안 카드는 옛 내용을 보여 주고, 스켈레톤은 처음 열 때만이다.
- **로그인·로그아웃·탈퇴·401 마다 `queryClient.clear()`** — 안 비우면 다른 계정으로 로그인했을 때 앞 계정의 차량이 캐시에서 보인다.
  E2E(`core-flow.spec.ts`)가 지킨다(clear 를 빼면 실패하는 것을 확인했다).
  **화면 밖에서 차량 데이터가 바뀌는 곳은 캐시를 버린다**(`removeQueries`) — 가져오기·언어/단위/통화/시간대 저장은
  `queryKeys.allVehicles()`, 차량 삭제는 그 차량. 무효화(`invalidate`)가 아니라 버리는 이유: 다시 열 때 옛 값이 먼저 보이고
  주행거리가 옛 값에서 굴러 오른다
- **`key` 는 "다른 것을 고치게 됐다"는 뜻일 때만 쓴다.** 수정 폼은 기록 id 로 key 를 준다 — 없으면 열린 폼의 입력이 다른 행에
  덮어써진다. `OdometerForm` 은 주행거리 값으로 key 를 준다 — 값이 바뀌면 입력칸을 새 값으로 되돌린다.
- **정비 목록의 쪽·종류 필터는 `VehicleDetailPage` 가 들고 있다.** 빠른 정비로 목록 밖에서 기록이 생기면 1쪽·전체 종류로
  되돌리되 열어 둔 수정 폼은 남긴다
- 홈·차량 목록은 아직 `useAsyncData`(오류 객체를 돌려주고 렌더할 때 번역). 목록 두 카드의 공통 상태(폼 열림·동작 실패·삭제 중인 행)는
  `useRecordList`, 범위 밖 페이지 복귀는 `usePageInRange`
- `date-input` 은 터치(`pointer: coarse`)면 드럼 휠, 아니면 네이티브 date. 둘 다 오늘에서 끊는다
- 내보내기는 받은 JSON 을 **Blob 으로 만들어** 내려준다 — `<a href>` 로 바로 받으면 세션·CSRF 헤더가 빠진다
- 가입 때의 지역 추정(`shared/lib/preferences`)은 태그에 **적힌** 지역으로만 — `maximize()` 는 `en` → `US` 라 영국 사용자도 달러로 시작한다
- 빠른 정비(`QuickServiceForm`)는 저장한 줄을 바로 '모름' 으로 되돌린다 — 중간 실패 뒤 다시 저장해도 두 번 넣지 않게
- `renderWithProviders`(`shared/lib`)는 테스트 전용 도우미다. 화면 코드에서 import 하지 않는다
- shadcn 설정: `components.json` 의 `aliases.ui` 가 `@/shared/ui/base`, `aliases.utils` 가 `@/shared/ui/cn` 을 가리킨다 —
  안 바꾸면 다음 `shadcn add` 가 base/ 밖에 파일을 만들거나 기본 `cn` 을 쓴다. `tsconfig.json` 의 `paths` 도 shadcn CLI 가 읽는다
- vitest 설정은 `vite.config.ts` 안에 둔다(별도 파일이면 `@` 별칭이 두 곳으로 갈린다)
- 컴포넌트와 값을 한 파일에서 내보내면 핫 리로드가 깨진다 — 그래서 `AuthContext.ts`·`control.ts`·`niceMax.ts` 가 따로 있다
- `index.html` 의 테마 스크립트와 `ThemeContext.ts` 가 `'odolog-theme'` 를, `index.css`·`index.html`·`ThemeProvider.tsx` 가
  다크 배경색을 중복으로 가진다. **한쪽만 고치면 안 된다**(docs/DESIGN.md)

테스트: **대상 파일 옆에 둔다**(`format.ts` 옆 `format.test.ts`). `npm run test` 97개(파일 14개) — 순수 함수, 사전 누락,
의존 방향, `cn` 토큰 인식, 그리고 `renderWithProviders` 로 그려 보는 `MaintenanceSection`·`FuelForm`.
실제 브라우저 E2E 는 `frontend/e2e`(`npm run e2e`, 14개, Playwright) — 이미 떠 있는 서버를 재사용하지 않는다(평소 서버가 운영 DB 라서).
콘솔 오류가 하나라도 나면 실패한다.

### 의존 방향

    백엔드:  account → garage
             {account, garage, summary} → {fuel, maintenance, vehicle, user},  전부 common 을 쓴다
             fuel → vehicle → user
             maintenance → vehicle → user
    프론트:  app → features → shared
             기능 사이: vehicles → {maintenance, fuel}, account → auth, summary → maintenance(정비 종류 타입만)

**이 방향은 테스트가 지킨다** — 백엔드 `DependencyDirectionTest`, 프론트 `src/dependency-direction.test.ts` 가 import 를 훑어
허용 목록 밖이면 실패한다. **새 패키지·기능을 만들면 허용 목록에 먼저 더한다** — 등록 안 된 패키지가 있으면 그 자체로 실패한다.

**`account`·`summary`·`garage` 가 백엔드의 조율 층이다** — 프론트의 `app/` 과 같은 성격으로, 여러 기능을 동시에 알아도 되는 자리다.
회원 탈퇴를 `UserService` 에 넣으면 `user → vehicle` 역방향이 생기고, `common` 에 두면 진짜 순환이 된다(모두가 `common` 을 안다).
하는 일에 따라 주입받는 것이 다르다: **순서를 조율**하는 쪽(탈퇴·차량 삭제)은 각 기능의 **서비스**를 받아 실제 삭제는 그쪽에 맡기고,
**읽어서 합치기**만 하는 쪽(홈 요약·차량 목록·내보내기)은 **리포지토리**를 받는다 — 집계에는 소유권 검사·삭제 순서가 필요 없다.
알려진 예외는 가져오기 하나다(위 "건드리기 전에").

`app` 이 여러 기능을 아는 유일한 프론트 층이라 `Header`(`useAuth`)와 `ProtectedRoute` 가 여기 있다.
반대 방향 의존(`user`가 `vehicle`을 알거나, `shared`가 `features`를 아는 것)이 생기면 설계가 잘못된 신호로 보고 재검토한다.

### 세분화가 멈추는 두 지점

깊이를 더 내려갈 수 없는 자리가 둘 있다. 둘 다 **바깥에서 경로를 이름으로 붙잡고 있기** 때문이다.

- `com/odolog/app/OdoLogApplication.java` — `@SpringBootApplication` 의 컴포넌트 스캔 기점이다.
  `bootstrap/` 으로 내리면 `scanBasePackages` / `@EntityScan` / `@EnableJpaRepositories` 를
  손으로 지정해야 하고, 그 순간 "어디까지 스캔되는가"가 코드에서 안 보이게 된다.
- `frontend/src/main.tsx` — `index.html` 의 `<script type="module" src="/src/main.tsx">` 가
  이 경로를 문자열로 가리킨다. 옮기면 HTML 도 같이 고쳐야 한다.

폴더를 옮기면 package 선언과 import 가 같이 바뀐다 — IDE 의 Move 를 쓴다.

## 진행 상황

코드는 끝났고 **남은 것은 브라우저 눈 확인이다** — `docs/QA.md` 의 Phase 6(6-B~6-E)과 Phase 7(7-H).
운영 DB 는 V4 적용 확인 하나가 남았다(같은 문서의 Phase 7 "운영 DB"). V1~V3 와 `vehicles.version` 은 2026-10-06 에 확인했다.
완료한 작업과 그 근거는 `HISTORY.md` 에 있다. 새 작업을 마치면 `HISTORY.md` 맨 위에 항목을 더하고,
`docs/QA.md` 의 체크리스트에서 그 줄을 지운다. 화면에 무언가를 더하면 6-B 에 확인 줄을 같이 넣는다.

**6-B(1회차 한 바퀴)를 끝내기 전에는 새 점검·보강 라운드를 열지 않는다** (2026-10-05).
10-01 부터 10-04 까지 점검이 연달아 이어지는 동안 화면은 한 번도 사람 눈으로 보지 못했고, 점검마다 고친 것이 다음 점검의 대상이 됐다.
새로 찾은 보안·동시성 문제는 그 자리에서 고치지 않고 `docs/QA.md` 의 **백로그**에 적는다.
막는 수준(데이터 손실·권한 우회)이면 사용자에게 먼저 알리고 고칠지 함께 정한다.
