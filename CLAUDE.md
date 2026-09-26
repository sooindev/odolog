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
- `CLAUDE.md` — 작업용 기록. 지금 지켜야 할 것: 설계 결정과 그 이유, 대안 비교, 체크리스트.
- `HISTORY.md` — 작업 일지. 이미 끝난 것: 무엇을 왜 그렇게 정했는지. 최신순으로 쌓는다.

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

세팅은 끝났다. 매번 재확인하지 말 것. 단, **DB 접속이 실패하면 계정 문제부터 의심한다**
(아래 "DB 접속 시 주의" 참고 — 한 번 크게 막혔던 지점이다).

- DB: MariaDB, `localhost:3306`, 스키마 `odolog` (utf8mb4 / utf8mb4_unicode_ci)
- **`ddl-auto: update` 는 제약을 추가는 해도 절대 지우지 않는다.** 2026-09-07에 실제로 겪었다:
  번호판 유니크를 전역 → 소유자별로 바꿨을 때, 앱을 띄우면 새 복합 유니크
  `uk_vehicles_user_plate_number` 는 Hibernate 가 만들어 줬지만 옛
  `uk_vehicles_plate_number` 는 그대로 남았다. 둘 다 있으면 더 엄격한 옛것이 이겨서
  **코드만 고치면 아무것도 안 바뀌고 조용히 예전대로 동작한다.** 옛 제약은 손으로 지웠다:

      /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults \
        -e "USE odolog; ALTER TABLE vehicles DROP INDEX uk_vehicles_plate_number;"

  `odolog_test` 는 매번 `create-drop` 이라 이 문제가 안 생긴다 — 그래서 **테스트는 통과하는데
  운영만 안 바뀌는** 상황이 된다. 앞으로 제약을 바꿀 때마다 아래로 실제 상태를 확인할 것:

      /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW CREATE TABLE vehicles\G"

  **2026-09-16에 같은 함정을 다른 모양으로 또 만났다**: `@Enumerated(STRING)` 이 만든 네이티브
  `enum(...)` 컬럼은 자바 enum 에 값을 더해도 `ddl-auto: update` 가 바꿔 주지 않는다.
  이번엔 컬럼을 **varchar 로 바꿔 문제 자체를 없앴다**(`@JdbcTypeCode(SqlTypes.VARCHAR)`).
  운영 DB 에 한 번만 아래를 실행하면 된다 — enum 은 값을 문자열로 저장하므로 데이터는 보존된다:

      /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults \
        -e "USE odolog; ALTER TABLE maintenance_records MODIFY COLUMN type VARCHAR(30) NOT NULL;"

  **✅ 이 저장소의 운영 DB 에는 2026-09-17 에 반영을 확인했다** (`type varchar(30) NOT NULL`).
  README 에 남겨 둔 안내는 **이 저장소를 예전부터 쓰던 다른 DB** 를 위한 것이라 그대로 둔다.

  **같은 날 `fuel_records.reset_point` 도 확인했다** — `bit(1) NOT NULL DEFAULT b'0'` 로
  제대로 붙었다. `@ColumnDefault("false")` 를 붙여 둔 덕에 이미 있던 3건이 무엇으로 채워질지를
  DB 구현에 맡기지 않았다. 여기는 손댈 것이 없었다.

  (`SHOW INDEX` 보다 `SHOW CREATE TABLE` 이 낫다. 복합 유니크가 `user_id` 로 시작하면 외래키용
  인덱스 `fk_vehicles_user` 가 그 역할을 대신해 `SHOW INDEX` 목록에서 사라지는데, FK 제약 자체는
  멀쩡히 살아 있다. `SHOW CREATE TABLE` 은 그걸 그대로 보여준다.)
- **앱이 쓰는 계정은 `odolog`@localhost** (2026-09-06 생성). `odolog.*` 에만 권한이 있다.
  비밀번호는 어떤 파일에도 적지 않는다 — IntelliJ 실행 구성의 환경변수에만 있다.
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

이 계정(`user@localhost`)은 **진단용으로만** 쓴다. 권한은 이렇게 되어 있다:

    GRANT ALL PRIVILEGES ON *.* TO `user`@`localhost`
      IDENTIFIED VIA mysql_native_password USING 'invalid' OR unix_socket

비밀번호 해시가 문자 그대로 `'invalid'` 라서 **비밀번호로는 절대 접속되지 않고**, 유닉스 소켓으로만
붙는다. JDBC는 TCP로 접속하므로 이 계정을 애플리케이션에 쓸 수 없다.
JDBC의 `localSocket=` 파라미터도 시도했으나 동작하지 않았다.

그래서 앱 전용으로 `odolog`@localhost 계정을 따로 만들었다:

    CREATE USER 'odolog'@'localhost' IDENTIFIED BY '<비밀번호>';
    GRANT ALL PRIVILEGES ON odolog.* TO 'odolog'@'localhost';

`*.*` 가 아니라 `odolog.*` 로 제한한 이유: 이 계정이 새어 나가도 다른 스키마(`for_125` 등)는
건드릴 수 없게 하기 위해서다.

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
6. **제약조건에는 이름을 직접 붙인다.** (`uk_vehicles_user_plate_number`, `fk_vehicles_user`)
   Hibernate가 짓는 해시 이름(`UK6dotkott2kjsp8vw4d0m25fb7`)은 로그 추적이 불가능하다.
7. **시간 필드는 `BaseTimeEntity` 를 상속해서 얻는다** (2026-09-16 부터. 그전에는 엔티티마다
   `@PrePersist`/`@PreUpdate` 를 복사했다). `createdAt` 에는 `updatable = false` 를 준다.
   **엔티티 3개까지는 복사가 옳았다** — 상속이 없으면 파일 하나만 열어도 모든 필드가 보인다.
   4개째(`FuelRecord`)에서 뒤집혔다. 새 엔티티는 `extends BaseTimeEntity` 만 하면 된다.
   **스위치는 `common/config/jpa/JpaAuditingConfig`** 이고, `@DataJpaTest` 는 그걸 자동으로
   집어 가지 못하므로 리포지토리 테스트에 `@Import(JpaAuditingConfig.class)` 가 필요하다.
8. **타입 선택**: "없음"이라는 상태가 존재하는 값만 래퍼 타입(`Integer`), 아니면 기본형(`int`).
   PK는 저장 전 `null` 구분을 위해 항상 `Long`.
9. **테이블명은 복수형** (`users`, `vehicles`). `user` 는 예약어라 반드시 `users`.
9-1. **숫자 PK 는 서버 밖으로 내보내지 않는다** (2026-09-26 — 차량, 같은 날 정비·주유 기록까지).
    URL·API 에는 12자 무작위
    `public_id`(`common/domain/identifier/PublicId`)가 나간다. 1,2,3… 은 남의 차를 못 열어도
    **서비스 규모와 등록 순서**를 말한다 — 화면 캡처 한 장에 실려 나간다.
    PK 를 UUID 로 바꾸지 않은 이유: 외래키 네 곳이 따라 바뀌고, InnoDB 는 PK 순서로 행을 저장해서
    무작위 PK 는 넣을 때마다 중간에 끼워 넣는다. 인코딩(Hashids/Sqids)은 되돌릴 수 있어 숨기는 게 아니다.
    서비스는 공개 id 로 `findOwnedVehicle` 을 부른 뒤로는 `vehicle.getId()` 만 쓴다.
    기록은 `findByPublicIdAndVehicleId` — 공개 id 를 알아도 다른 차량 경로로는 못 건드린다.
    **정렬에는 공개 id 를 쓰지 않는다.** 무작위라 "나중에 넣은 것" 을 말하지 못한다 —
    같은 날짜 동점은 서버 안에서 숫자 id 로 가른 뒤 응답에만 공개 id 를 싣는다(홈 최근 활동).
    **사용자 id 는 응답에서 아예 뺐다**(2026-09-27). URL 에 안 나와 공개 id 도 필요 없고,
    화면이 쓰는 곳도 없었다 — 남아 있던 이유 없이 "몇 번째 가입자인가" 만 말하고 있었다.
10. **로그인한 사용자 식별은 세션에서만 한다.** 요청 바디나 URL의 사용자 ID는 클라이언트가
    조작할 수 있으므로 신뢰하지 않는다 (`SessionConst.LOGIN_USER_ID`).
11. **예외는 의미에 맞는 상태 코드로 세분화한다**: 400(입력 검증 실패) / 401(미인증) /
    403(권한 없음) / 404(리소스 없음) / 409(리소스 중복) / 429(시도 과다). 서버 쪽 불변식이 깨진 경우
    (예: 세션엔 있는데 DB엔 없는 사용자)는 일부러 핸들러를 만들지 않고 500으로 흘려보내
    로그에 남긴다 — 모든 예외를 친절한 응답으로 감쌀 필요는 없다.
    **다만 "상태 코드를 낮추지 않는다" 와 "본문을 주지 않는다" 는 다른 얘기다**(2026-09-23).
    500 은 500 으로 두되 `ErrorResponse` 는 돌려준다 — 안 그러면 스프링 기본 응답이 나가는데
    거기엔 `message` 가 없어 화면이 "요청에 실패했습니다 (HTTP 500)" 밖에 말하지 못한다.
    **단, 남의 자원에는 403 이 아니라 404 를 준다** (2026-09-22). 403 은 "권한이 없다"와
    동시에 **"있긴 하다"** 를 말하고, 차량 id 는 1,2,3… 으로 이어지므로 둘이 갈리면
    훑어서 어느 번호가 쓰이는지 셀 수 있다. **문구까지 같아야 한다** — 상태 코드만 맞추고
    메시지가 다르면 그 메시지가 대신 알려준다. 정비·주유는 `findByPublicIdAndVehicleId` 라
    처음부터 404 하나였고, 차량만 혼자 달랐다.
    그래서 지금 `ForbiddenAccessException` 을 던지는 곳은 **한 군데도 없다.**
    타입과 핸들러는 남겨 뒀다 — 소유자가 아니어도 볼 수는 있는 자원(예: 공유받은 차량)이
    생기면 그때가 진짜 403 이다.
12. **예외는 전용 타입으로 던진다.** `IllegalArgumentException` 같은 JDK 범용 예외를 핸들러에
    매핑하지 않는다. 우리가 안 던진 예외까지 잡혀서 500이어야 할 것이 조용히 4xx로 나간다.
    상태 코드 하나당 예외 클래스 하나(`ConflictException`/`AuthenticationFailedException`/
    `ForbiddenAccessException`/`ResourceNotFoundException`).
13. **서비스는 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.**
    메서드 쪽이 클래스 쪽을 덮어쓴다. 새 메서드를 깜빡했을 때 기본이 안전한 쪽(읽기 전용)이라
    쓰기가 실패해서 바로 드러난다. 반대로 하면 아무 일도 안 일어나 영영 모른다.
14-1. **빈 문자열로 저장하지 않는다.** 선택 입력 칸(전화번호·메모·정비 설명)을 비우면
    `''` 가 아니라 `null` 로 저장한다(2026-09-23 에 메모·설명까지 맞췄다). 그대로 두면
    "없음" 이 두 모양이 되고, **내보낸 JSON 에도 그 차이가 그대로 나간다.**
    자르는 자리는 서비스다 — DTO 접근자에서 자르면 부분 수정에서 "안 보냄"과 "지움"이 같아진다.
    **필수 입력의 앞뒤 공백도 서비스가 자른다**(2026-09-26, `common/text/InputText.strip`).
    번호판·제조사·모델명·닉네임. DB(unicode_ci)는 뒤 공백·대소문자를 무시하고 비교하는데 자바
    `equals` 는 구분해서, `"12가3456 "` 을 공백만 지워 고치면 **자기 자신과 중복으로 409** 였고,
    앞 공백은 같은 번호판 두 대를, 가져오기는 전체 실패를 만들었다(셋 다 재현).
    "같은 번호판인가" 를 자바에서 판단하는 곳(차량 수정·가져오기)은 **DB 와 같은 기준**
    (공백·대소문자 무시)으로 본다. `trim()` 이 아니라 `strip()` — 전각 공백(U+3000)까지 지운다.

14. **주석은 한 줄 명사구로 쓴다** (2026-09-18 부터). 종결어미(`~한다` / `~이다`)를 붙이지 않고
    명사나 명사구로 끝낸다. `<b>` · `<p>` 같은 Javadoc 태그도 쓰지 않는다.

        // 계기판 값이 더 최신이면 차량 쪽도 갱신
        // 이력 먼저, 차량 나중 — FK 제약
        /** 정렬 고정. sort 파라미터 무시 — 연비 계산의 전제 */

    **무엇을 하는지 + 건드리면 안 되는 이유**까지만 담는다. 긴 논증과 대안 비교는
    이 문서(`CLAUDE.md`)에 있으므로 코드에서 되풀이하지 않는다 — 같은 설명이 두 곳에 있으면
    한쪽만 고치게 되고, 그때 **코드 옆의 설명이 먼저 낡는다.**
    길어야 세 줄이고, 그보다 길어지면 그건 주석이 아니라 설계 기록이라 여기로 옮긴다.

15. **계정 존재 여부를 응답으로 알려주지 않는다 — 단, 회원가입은 예외다** (2026-09-22).
    로그인은 실패 사유를 통일하고(없는 이메일도 "이메일 또는 비밀번호가 올바르지 않습니다"),
    비밀번호 재설정은 가입 여부와 무관하게 204 를 주며 **메일 발송 실패까지 삼킨다**.
    없는 계정의 로그인 실패도 횟수를 센다 — 안 세면 "빨리 답하는 쪽"이 곧 없는 계정이다.
    **회원가입만 409 로 존재를 알려준다.** 가입하려는 사람에게 "이미 있습니다"는 맞는 안내이고,
    이걸 없애려면 가입을 메일 확인 흐름으로 바꿔야 하는데 **메일 설정이 없으면 아무도 가입을
    끝내지 못하게 된다.** 대신 **한 곳에서 주소를 쓸어 보는 것**을 막는다 —
    `UserController` 가 **IP 를 키로** 가입 시도를 센다(이메일로 세면 매번 다른 주소를 넣는
    열거자는 카운터가 늘 1 이라 그냥 빠져나간다). 성공한 가입도 센다: 409 만 세면 아직 없는
    주소를 찔러 보는 쪽이 안 걸리는데, 그쪽은 계정을 실제로 만들어 버려 더 나쁘다.
    **`X-Forwarded-For` 는 읽지 않는다** — 보내는 쪽이 적는 값이라, 그 값을 덮어써 주는
    프록시를 앞에 두기 전까지는 헤더 한 줄로 제한을 빠져나가게 만들 뿐이다.
    **응답 시간도 같게 맞춘다**(2026-09-26). 없는 이메일이어도 `dummyHash` 와 BCrypt 비교를
    한 번 돌린다 — 건너뛰면 그쪽만 0.1ms, 있는 쪽은 60ms 라 550배 차이가 났다.
    `dummyHash` 는 같은 인코더로 만든다. 상수로 박으면 강도(라운드 수)를 바꿀 때 혼자 옛 비용에 남는다.

## 디자인 시스템 (프론트엔드)

**라이트/다크 모노톤.** 토큰 이름은 한 벌이고 값만 두 벌이다. 값은 전부 `src/index.css`
한 파일의 `:root`(라이트)와 `:root.dark`(다크) 두 블록에만 있다.

0. **컴포넌트에는 색 값을 적지 않는다.** `bg-card`, `bg-fill`, `hover:bg-wash` 처럼 **역할**만
   말한다. `bg-white/[0.04]` 같은 값이 한 곳이라도 남아 있으면 **그 요소만 반대 테마에서
   안 보인다** — 실제로 재설계 직후 8곳이 그랬다. 새 색이 필요하면 토큰을 먼저 만든다.
1. **색은 α(불투명도)만 조절한다.** 다크는 어두운 바닥 위에 흰색을, 라이트는 밝은 바닥 위에
   검정을 몇 % 얹느냐로 모든 단계를 만든다. 숫자가 곧 의도를 말해 준다.
   oklch 는 읽어서 밝기를 가늠할 수 없어 쓰지 않는다.
2. **글자 4단계, 면 5종, 선 2단계.**
   글자 `strong`(제목) → `foreground`(본문) → `muted-foreground`(보조) → `faint`.
   **앞의 세 단계는 두 테마 모두 WCAG AA(4.5:1)를 넘긴다.** 수치는 배경 위 / 카드 위 두 벌로
   본다 — 카드가 배경보다 밝아 그쪽이 더 빡빡하고, 글자는 대부분 카드 위에 놓인다.
   라이트 15.30 / 8.16 / 4.82, 다크 16.55 / 9.63 / 5.26(카드 위 14.52 / 8.72 / 4.98).
   `faint` 만 그 아래(3.2 안팎)라서 **placeholder와 장식용 아이콘에만** 쓴다 —
   날짜·번호판처럼 읽어야 하는 값에 쓰면 그 값만 안 보인다.
   면은 `card`(카드) / `card-hover` / `fill`(입력) / `wash`(호버 워시) / `sunken`(한 겹 안쪽).
   **본문이 순수 흰색/검정이 아닌 게 핵심이다.** 검정 위의 `#FFF` 는 대비가 너무 세서
   글자가 번져 보이고(halation), 흰 위의 `#000` 은 반대로 너무 딱딱하다.
   **바닥도 양쪽 다 순색이 아니다**: 라이트 `#F4F4F6`, 다크 `#17171A`.
   순백 위의 흰 카드, 순검정 위의 검은 카드는 둘 다 배경과 구분되지 않아 레이아웃이 평평해진다.
   특히 다크는 순검정(`#000000`)에 흰색 3%를 얹으면 카드가 `#080808` 이라 사실상 같은 색이었다
   — 2026-09-13에 바닥을 올리고 면 α 도 함께 올렸다.
   **다크 배경색은 세 곳에 중복이다**: `index.css`(`--background`) · `index.html`의
   `theme-color` · `ThemeProvider.tsx`. 한 곳만 고치면 모바일 주소창만 옛 색으로 남는다.
3. **Accent 는 흰색 하나.** 브랜드 색을 두지 않는다. 강조는 "흰색을 얼마나 얹느냐"로만 한다.
   한 화면에 채워진 흰 버튼은 **하나만** 둔다. 두 개면 무엇이 기본 동작인지 사라진다.
   빨강(`--destructive`)은 유일한 예외 — 장식이 아니라 "실패"라는 뜻을 나르는 기능색이다.
   **경고색(노랑·주황)을 새로 만들지 않는다.** 정비 시기가 지난 것처럼 "주의"에 해당하는
   상태가 생겼을 때(2026-09-25) 색을 늘리는 대신 **대비를 올렸다** — `지남` 라벨은
   테두리 + `text-strong` 이다. 빨강을 쓰지 않은 이유: 그건 실패가 아니라 **할 일**이고,
   빨강이 두 가지를 뜻하기 시작하면 `확인 필요`(입력 오류)의 무게가 같이 가벼워진다.
   같은 판단으로 `기록 빠짐?` 도 회색이다.
4. **경계선은 `rgba(255,255,255,0.08)` 한 값뿐.** 그보다 진하면 선 자체가 요소로 보이기 시작한다.
   **그림자는 쓰지 않는다** — 검정 위의 그림자는 보이지도 않으면서 가장자리만 탁하게 만든다.
   높이 차이는 경계선과 배경 농도로만 표현한다.
5. **면은 평면이다. 깊이는 괘선과 여백이 만든다.** (2026-09-14에 뒤집혔다 — 전에는
   "면은 유리로 만든다"였고 카드·헤더가 `흰색 3% + backdrop-blur(20px)` 였다.)
   흐림 효과는 2020년 이후 템플릿 UI 의 서명 같은 것이라 어느 서비스에나 있고,
   무엇보다 **뒤가 비치는 면은 그 위에 놓인 글자의 배경을 불확실하게 만든다.**
   유리를 받쳐 주던 상단 방사형 광채(`--glow`)도 함께 지웠다 — 유리가 없으니 화면 위쪽이
   뿌옇게 뜨는 얼룩으로만 남았다. 그림자도 여전히 쓰지 않는다.
   지금 면이 하는 일은 둘뿐이다: 아주 옅은 배경 농도, 그리고 1px 괘선.
6. **폰트는 시스템 폰트만.** `-apple-system` → `SF Pro` → `Apple SD Gothic Neo`(한글).
   웹폰트를 받지 않으므로 글꼴이 바뀌며 깜빡이는 현상(FOUT)이 없다.
   **글꼴이 하나뿐이므로 위계는 크기·굵기·자간 셋으로만 만든다.** 그래서 단계 사이를
   과감하게 벌린다 — 제목 52px 과 본문 15px 은 3.5배 차이다. 어중간하게 벌리면
   "조금 큰 글씨"로 보이고, 크게 벌려야 다른 종류의 글자로 읽힌다.
   **크기는 전부 `index.css` 의 타입 스케일 토큰으로 쓴다** (2026-09-14 신설).
   화면에 `text-[2rem]` 처럼 크기만 적으면 자간·행간이 따라오지 않아 어정쩡해진다:

       text-eyebrow  11px  자간 +0.2em   분류 한 줄 (대문자)
       text-title    32→52px clamp       화면 제목(h1)
       text-headline 28→40px clamp       랜딩의 구역 제목 (2026-09-24 신설)
       text-display  52→80px clamp 굵기300 히어로 숫자 하나
       text-figure   22px                목록 행의 수치
       text-section  17px                카드·구역 제목
       text-lede     16px                제목 아래 설명
       text-body     15px 자간 -0.01em   그냥 읽는 글 (2026-09-19 신설)
       text-caption  13px                설명·날짜·부가 정보 (2026-09-19 신설)
       text-unit     11px                단위·부가 라벨 (2026-09-24 신설)
       text-axis     10px                차트 축 눈금 (2026-09-24 신설)

   **`text-unit` 은 `text-eyebrow` 와 크기가 같다.** 자간을 벌리지 않는 것이 다르다 —
   대문자 분류 라벨이 아니라 숫자 옆에 붙는 `L`·`월` 같은 것이라 벌리면 흩어져 보인다.

   ⚠️ **`text-xs`·`text-sm` 같은 기본 스케일도 쓰지 않는다**(2026-09-26 에 22곳을 걷어냈다).
   토큰만 쓰기로 해 놓고 정작 12px·14px 이 화면 곳곳에 섞여 있었다 — 작은 글씨가
   **12·13·14·15px 네 단으로 3px 안에 몰려** 위계가 사라진 상태였다. 지금은 둘로 접었다:
   보조 문장·단위는 `text-caption`(13px), 배지와 차트 안의 작은 값은 `text-unit`(11px).
   **`cn()`·`cva()` 를 거치는 파일은 예외다** — 거기는 토큰이 지워지므로 기본 스케일이 맞다
   (`state.tsx`·`card.tsx`·`input.tsx`·`button.tsx`). `date-input` 의 `text-base` 도 예외 —
   모바일에서 16px 보다 작으면 iOS 사파리가 화면을 확대한다.

   ⚠️ **토큰은 두 곳 이상에서 반복될 때만 만든다** (2026-09-24 에 기준을 적었다).
   화면 하나에만 쓰이는 고유 크기 — 랜딩 h1, `AuthLayout` 태그라인, 홈 통계 타일 — 는
   임의 값으로 둔다. 쓰는 곳이 하나인 척도를 토큰으로 올리면 **척도가 아니라 별명이 된다.**
   전에는 "크기는 전부 토큰으로" 라고만 적혀 있었는데 실제로는 화면 6곳이 임의 값을 쓰고
   있었다 — 문서가 사실이 아니었던 자리라 기준을 명시한다.
   새 토큰을 만들면 **`cn-usage.test.ts` 의 `TYPE_SCALE` 목록에도 더한다.**

   **아래 둘에는 행간을 묶지 않았다.** 화면에서 `leading-relaxed` 를 붙여 쓰는 자리가 있어
   토큰에 넣으면 그 자리들이 한꺼번에 달라진다. 제목 계열과 달리 본문 행간은 문단 길이에 따라
   달라지는 것이 자연스럽다.

   ⚠️ **`cn()` 을 거치는 컴포넌트에서는 이 토큰들을 쓸 수 없다.** 2026-09-19 에 두 가지 방식으로
   깨지는 것을 확인했다. 둘 다 원인은 하나다 — **`cn`(tailwind-merge 계열)은 커스텀 테마 이름을
   크기로 인식하지 못하고 색 유틸리티로 추정한다.**

       cn('text-caption', 'text-strong')        → 'text-strong'      크기가 사라진다
       cn('text-[0.8125rem]', 'text-strong')    → 둘 다 남는다        임의 값은 크기로 인식
       cn('text-sm', 'text-strong')             → 둘 다 남는다        기본 스케일도 인식

   · **`base/card.tsx`·`label.tsx`·`feedback/state.tsx`** — 한 문자열에 크기 토큰과 색이 같이
     있어 **크기가 통째로 지워졌다.** `CardTitle` 은 2026-09-11 에 `text-section` 을 넣은 뒤로
     줄곧 17px·굵기 600·자간 -0.022em 을 **전부 잃고** 16px·굵기 400 으로 렌더되고 있었다.
   · **`button.tsx` 의 size variant** — `cva` 는 클래스를 이어 붙이기만 하므로 base 의
     `text-[0.875rem]` 과 size 의 토큰이 둘 다 남고, 승자를 **Tailwind 의 CSS 배치 순서**가
     정한다. **토큰 유틸리티는 임의 값보다 먼저 배치되므로** base 의 14px 이 이겨
     `size="lg"` 버튼이 14px 로 작아졌다(랜딩의 `시작하기` 가 "잘 안 보인다"로 드러난 것).

   **그래서 저 네 파일은 임의 값을 쓴다.** `card.tsx` 처럼 토큰이 행간·굵기·자간까지 묶고 있던
   자리는 그 값들을 그대로 풀어 적었다. 화면 파일(`features/`·`app/`)은 `cn()` 을 거치지 않아
   토큰을 그대로 쓴다 — 45곳이 그쪽이다.
   **이 둘이 없던 동안 화면 45곳이 `text-[0.8125rem]` 처럼 크기를 직접 적고 있었다** —
   "크기는 전부 토큰으로"라는 이 규칙이 정작 가장 많이 쓰는 두 크기에서 지켜지지 않았다.

   **히어로 숫자의 굵기는 300이다.** 크기가 이미 강조를 다 하고 있어서 굵기까지 올리면
   숫자가 뭉쳐 보인다 — 크게 키울수록 얇게 두어야 정밀해 보인다.
   `clamp()` 를 쓰는 이유는 `sm:text-[...]` 계단을 만들면 그 분기점을 빠뜨린 화면만
   혼자 작아지기 때문이다(차량 상세가 실제로 그랬다).
   **한글 제목에는 eyebrow 처리를 쓰지 않는다.** 대문자가 없어서 자간만 벌어진 2글자
   ("계정")는 흩어져 보인다. 라틴 문자 분류 라벨(`GARAGE`, `ODOMETER`)에만 쓴다.
7. **`tabular-nums` 는 "세로로 줄 맞출 상대가 있을 때만".** 시스템 폰트의 기본 숫자는
   글자마다 폭이 달라서, 표의 열이나 페이지 번호처럼 값이 바뀌는 자리에서는 줄이 떨린다.
   목록·표·축 눈금에는 붙인다.
   **반대로 큰 숫자 하나(히어로 숫자, 통계 타일 값, 차량 상세의 주행거리)에는 붙이지 않는다** —
   모든 글자를 `0` 너비로 맞추는 설정이라, 큰 글씨에서는 `1` 같은 좁은 글자 주변이 휑하게
   벌어져 보인다. 맞출 상대가 없는데 폭만 벌리는 셈이다.
8. **차트는 색을 늘려서 화려하게 만들지 않는다.** 이 앱의 차트는 전부 **계열이 하나**라
   색이 구분할 것이 애초에 없다 — 그래서 범례도 없다(계열이 하나면 제목이 이미 무엇을 그린
   것인지 말해 준다). 화려함은 크기(히어로 숫자)·밀도·자라나는 움직임이 만든다.
   - 막대 두께 24px 이하, **네 모서리 전부 각지게**. (2026-09-13 이전에는 데이터가 끝나는 쪽만
     4px 둥글렸으나, 화면 전체를 각지게 바꾸면서 없앴다 — 16번 참고.)
   - 눈금선은 **1px 실선**. 점선은 "예측"이나 "임계선"처럼 읽혀서 그냥 눈금일 때 쓰면 안 된다.
   - 축 눈금은 `niceMax()` 로 1·2·5 × 10ⁿ 에 맞춘다. `1,873` 같은 수로 끝나는 축은 못 읽는다.
   - **값을 모든 막대에 적지 않는다.** 가장 높은 것 하나만 직접 적고 나머지는 축·말풍선·표가 맡는다.
   - **정비 종류처럼 순서가 없는 항목에 "클수록 진하게"를 쓰지 않는다.** 막대 길이가 이미
     말한 것을 색으로 한 번 더 말하는 꼴이고, 색이라는 채널 하나를 그냥 태운다. 전부 같은 색.
   - **마우스를 올려야만 보이는 값을 만들지 않는다.** 말풍선은 정보를 보태는 장치지
     감췄다 보여주는 장치가 아니다. 월별 차트에는 `<details>` 표를 같이 둔다(키보드·스크린리더).
   - 막대 자라는 연출은 `transform: scaleY()` 가 아니라 **`clip-path`** 로 한다.
     scaleY 는 둥근 모서리까지 같이 눌러서 자라는 동안 모양이 찌그러진다.
9. **곡선은 `cubic-bezier(0.16, 1, 0.3, 1)`(`ease-apple`) 하나뿐이고, 위계는 시간으로 만든다.**
   곡선을 여러 개 두면 어떤 움직임이 어떤 성격인지 눈이 학습하지 못한다. 대신 길이를 나눈다:

       경고·오류            0.24s / 4px   방금 누른 것에 대한 답 — 가장 짧고 가깝다
       반응(호버·포커스)     0.2~0.3s      손가락을 따라와야 한다
       폼 펼쳐지기          0.36s         자리를 밀어내는 동작
       괘선 그리기          0.7s          지면이 짜이는 동작
       숫자 굴러가기        0.45~1.4s     변화 폭에 비례 — 아래 참고

   **가장 중요한 규칙: 움직일 이유가 있을 때만 움직인다.** 2026-09-14에 모든 페이지의
   모든 블록을 같은 거리·같은 시간으로 띄우는 `.stagger` 를 만들었다가 **전부 걷어냈다.**
   규칙 하나를 만들어 화면 전체에 일괄 적용하면 개별 요소를 들여다본 흔적이 남지 않고,
   그게 정확히 "어디서 본 듯한" 인상의 정체다. 지금 남은 연출은 전부 **무슨 일이
   일어났는지 말하는** 것뿐이다:
   - **행 왼쪽의 1px 표식**(차량 목록 호버) — 배경만 옅게 바뀌면 어느 행에 있는지
     훑어봐야 안다. 세로로 그어지게 한 이유: 가로로 늘리면 글자를 밀어내는 것처럼 보인다.
     목록의 가로 괘선과 같은 1px 이라 새 요소가 아니라 **있던 선이 세로로 서는 것**으로 읽힌다.
   - **폼이 펼쳐지기**(정비 이력) — `grid-template-rows: 0fr → 1fr`. `height` 는 내용 높이를
     JS 로 재야 하고 창 크기가 바뀔 때마다 다시 재야 한다. 안쪽 내용은 0.12s 늦게 들어온다 —
     칸이 열리는 것과 글자가 나타나는 것이 동시면 글자가 찌그러지며 늘어나 보인다.
     **닫을 때는 연출하지 않는다.** 닫기는 사용자가 이미 결정한 일이라 기다릴 이유가 없다.
   - **숫자 굴러가기**(주행거리) — **화면을 열 때는 움직이지 않는다.** 0 에서 굴러 오르는
     연출은 대시보드 템플릿의 상투구이고, 아무 일도 없었는데 움직이는 것이다. 값이 실제로
     바뀐 순간에만 직전 값에서 새 값으로 굴러간다 — 그 움직임이 "얼마나 올랐는지"를 나른다.
     **지속 시간은 변화 폭에 비례한다**(0.45~1.4s). 10km 와 20,000km 가 같은 시간이면
     작은 변화는 굼뜨고 큰 변화는 순식간에 지나간다.
     굴러가는 동안에만 `tabular-nums` 를 붙인다 — 7번의 "큰 숫자에 tabular 금지"는
     멈춰 있는 숫자에 대한 규칙이고, 매 프레임 폭이 달라지면 숫자가 떨린다.
   - **차트 막대**(칸마다 45ms) — 데이터가 그려지는 동작이라 값이 있다.
   - **괘선 그리기**(`Page` 머리말 아래) — 화면당 하나뿐인 '자리 잡기' 연출. 이 선이
     이 지면의 기준선이라서 값을 한다. 페이지를 옮길 때마다 보게 되므로 0.7s 로 짧게 둔다.
   - 페이지 전환은 **0.18s 페이드만.** 화면이 통째로 떠오르는 연출은 처음 한 번은 근사하지만
     몇 번 지나면 화면이 뜨기를 기다리는 시간으로만 남는다. 갈아 끼우는 순간을 덮을 뿐이다.
   - **정비 이력 목록에는 등장 연출을 걸지 않는다.** 삭제·수정으로 자주 다시 그려지는
     목록이라 그때마다 행이 나타나면 연출이 아니라 소음이 된다.
   - **`gap-px` 격자에도 걸 수 없다.** 칸 사이 틈으로 부모의 선 색이 비치는 구조라,
     칸이 투명한 동안 격자 전체가 색 덩어리로 번쩍인다(홈 통계 타일).
   - **호버에서 크기나 색을 바꾸지 않는다.** 배경 농도와 투명도만 움직인다.
   - **누를 때도 크기를 바꾸지 않는다**(`scale(0.97)` 을 걷어냈다). 각진 사각형이 눌릴 때
     줄어들면 고무처럼 보이고, 버튼의 변이 주변 괘선과 맞춰 둔 정렬이 그 순간 어긋난다.
   - **`prefers-reduced-motion` 에서는 지연도 함께 0 으로 만든다.** 지속 시간만 없애면
     차트 막대의 지연이 그대로 남아 **내용이 잠깐 안 보이는 시간**으로만 나타난다.

9-1. **제목 태그는 목차를 만든다 — 크게 보인다고 제목이 아니다** (2026-09-24).
   `Page` 가 `h1`, `CardTitle` 과 `Section` 이 `h2` 다. `AuthLayout` 왼쪽의 큰 문장은
   **`p` 다** — 아무 구역도 이끌지 않는 태그라인인데 `h2` 로 두었더니, 그 패널이 `Outlet`
   보다 앞서 있어 스크린리더 목차가 `h2 → h1` 순서가 됐다. 게다가 `hidden lg:flex` 라
   **화면 폭에 따라 목차가 달라졌다.** 크기는 클래스가 정하고, 태그는 구조가 정한다.

10. **모든 앱 화면은 `Page` 로 시작한다.** 뒤로가기·eyebrow·제목·설명·액션·간격이 전부 거기 있다.
   화면마다 머리말을 직접 그리면 반드시 어긋난다 — 실제로 차량 상세가 그렇게 드리프트해서
   `sm:text-[2rem]` 을 빠뜨렸고, 페이지 루트 간격이 `gap-8/10/12` 세 값으로 갈렸다.
   - **eyebrow** = "지금 보는 것이 어디에 속하는가": `Overview`(홈 통계), `Garage`(차량 목록·등록,
     그리고 차량이 0대일 때의 홈), `Account`(계정),
     차량 상세는 번호판. **로그인·회원가입은 아직 아무 데도 속하지 않으므로 비운다.**
   - **back** = 목록에서 파고 들어간 화면에만: 차량 등록·차량 상세.
   - **action** = 이 화면에서 새로 만드는 동작 하나. 목록의 "차량 등록"이 유일하다.
   - 폼 맨 아래 버튼 줄은 `FormActions`. 주 동작 먼저, 취소는 `ghost` 로 오른쪽.
   **랜딩(`/`)만 `Page` 를 쓰지 않는다** — 앱 화면이 아니라 문서다. 가운데 정렬과 큰 세로
   리듬(`gap-28`)이 그 신호이고, 그래서 머리말 규칙도 적용받지 않는다.
11. **넓은 화면은 "폭을 늘려" 채우지 않고 "열을 나눠" 채운다.** 컨테이너는 76rem(1216px)이지만
   **글이 담기는 열은 어디서도 700px를 넘지 않는다.** 줄이 길수록 다음 줄 첫 글자를 찾는
   눈의 왕복 거리가 늘어 읽기가 급격히 피곤해지기 때문이다. 화면별로:
   목록은 **괘선으로 나눈 행**(2026-09-14에 격자에서 바뀌었다 — 카드를 하나씩 씌우면
   차량 수만큼 상자가 늘어 화면이 상자 목록이 되고, 행으로 깔면 번호판은 번호판끼리
   주행거리는 주행거리끼리 세로로 정렬되어 **여러 대를 훑어 비교할 수 있다**.
   표 형태의 행은 "글이 담기는 열"이 아니므로 700px 제한의 예외다),
   차량 상세는 `사이드바(21rem) + 본문`,
   설정·등록 폼은 `Section`(왼쪽 설명 / 오른쪽 폼), 로그인·가입은 `AuthLayout`(반반).
   **모든 2단은 `lg:` 아래에서 grid가 풀려 저절로 한 줄로 쌓인다** — 모바일용 마크업을
   따로 쓰지 않는 이유다.
11-1. **좁은 화면은 "줄여서" 맞추지 않고 "덜어내서" 맞춘다.** 375px 기준으로 잡는다.
   - **헤더에서 화면 모드 토글을 숨긴다**(`sm` 미만). 로고 92 + 토글 92 + 닉네임 96 +
     로그아웃 80 + 간격이 386px 라 335px 에 60px 모자라고, 그러면 닉네임이 말줄임만 남는다.
     토글은 프로필의 '화면' 구역에도 있어서 기능이 사라지지는 않는다.
   - **차량 목록 행의 `Odometer` 라벨을 숨긴다.** 바로 아래 `km` 이 같은 말을 하고 있고,
     넓은 자간 때문에 오른쪽 열을 75px 넘게 잡아먹는다.
   - **정비 이력 행은 두 줄로 접는다**(`flex-wrap` + `basis-full`). 한 줄에 다 넣으면
     종류와 날짜가 들어갈 폭이 100px 남짓밖에 안 된다.
   - **차트 가로축 라벨은 홀수 칸만 남긴다.** 칸 하나가 20px 남짓이라 12개를 다 적으면
     숫자가 서로 붙는다. 막대는 12개 그대로다.
   - **크기는 `clamp()` 로 이어서 줄인다.** 히어로 숫자 40→80px, 통계 타일 32→44px.
     고정값으로 두면 7자리가 넘는 순간 줄이 넘친다.
   - **터치 기기에서는 `::after` 로 판정 영역만 44px 로 넓힌다**(`@media (pointer: coarse)`).
     버튼 자체를 키우면 목록 행의 리듬이 무너진다. 호버로만 드러나는 요소도 거기서는
     항상 진하게 둔다 — 터치에는 호버가 없다.
   - **노치 대응**: `index.html` 이 `viewport-fit=cover` 라 가로 모드에서 내용이 깎인다.
     바깥 래퍼에 `padding-inline: env(safe-area-inset-*)`, 본문 아래에 `safe-area-inset-bottom`.
   - 입력창 글자는 모바일에서 16px 를 유지한다. iOS 사파리가 그보다 작으면 화면을 확대한다.
   - **날짜는 터치에서 드럼 휠, 그 외에는 네이티브 date 입력이다**(`shared/ui/form/date-input.tsx`).
     OS 캘린더는 몇 달 전 기록을 넣으려면 달을 여러 번 넘겨야 하고, 반대로 데스크톱에서는
     타이핑이 제일 빠르다. 판단은 CSS 가 아니라 `matchMedia('(pointer: coarse)')` 가 한다 —
     미디어 쿼리로는 컴포넌트를 갈아 끼울 수 없다.
     **중첩 스크롤에는 `overscroll-contain` 이 필수다.** 관성·스냅·스크린리더는 브라우저가
     주지만 **연쇄(chaining)만은 명시적으로 꺼야** 바깥 페이지가 안 따라 움직인다.

12. **`grid` 자식에는 `minmax(0,1fr)` 또는 `min-w-0` 을 붙인다.** grid 자식의 기본
   `min-width`가 `auto`라서, 긴 메모나 긴 닉네임 한 줄이 열을 통째로 밀어내 격자가 넘친다.
   `lg:sticky` 를 쓸 때는 `self-start` 도 함께 — 없으면 칸이 옆 열 높이만큼 늘어나 안 걸린다.
13. **`color-scheme` 를 테마마다 둔다.** 이게 없으면 `<input type="date">` 의 달력 아이콘,
   `<select>` 펼침 목록, 스크롤바만 반대 테마로 남는다. CSS로는 못 고치는 영역이다.
14. **테마는 `light` / `dark` / `system` 세 값이다.** `system` 은 세 번째 색이 아니라
   "정하지 않음"이고, 저장된 값이 없을 때의 기본값이다. 이게 없으면 낮에 라이트를 고른
   사용자가 밤에 OS가 다크로 바뀌어도 계속 라이트를 본다.
15. **첫 프레임 깜빡임(FOUC)은 React로 못 막는다.** 브라우저는 번들을 받기 훨씬 전에 화면을
   한 번 그린다. `index.html` 의 인라인 스크립트가 `<html>` 에 클래스를 미리 붙이는 이유이고,
   그래서 `'odolog-theme'` 문자열이 HTML과 `ThemeContext.ts` 양쪽에 중복으로 존재한다.
   **한쪽만 고치면 안 된다.**

16. **모서리는 각지게 — 반경은 0 하나뿐이다.** `--radius-sm ~ --radius-3xl` 이 전부 `0` 이라
   아무것도 안 쓰면 그대로 각진다. **척도를 지우지 않고 0 으로 둔 이유**는 `shadcn add` 로
   새로 받는 컴포넌트가 `rounded-md` 같은 유틸을 달고 오기 때문이다 — 토큰이 0 이면
   그것들도 자동으로 각지게 나와서 매번 손으로 지울 필요가 없다.
   **컴포넌트에는 `rounded-*` 를 적지 않는다.** 적어 두면 클래스 이름이 거짓말을 한다
   (`rounded-2xl` 인데 안 둥근 것). 지금 `frontend/src` 전체에 `rounded` 문자열은 0건이고,
   빌드된 CSS 에 남은 `border-radius` 는 Tailwind 가 폼 요소에 거는 초기화 두 줄뿐이다.
   - 예외를 두지 않는다. 버튼·입력창·카드·차트 막대·세그먼트 컨트롤·로딩 점까지 전부.
     **한 군데만 둥글면 그것이 장식으로 보인다** — 앞의 알약 버튼이 정확히 그랬다.
   - 각지게 하면 버튼의 아래변이 표의 선, 카드의 변과 **같은 방향으로 정렬된다.**
     둥근 모서리는 그 정렬을 흐려서 요소마다 따로 노는 인상을 준다.
   - `overflow-hidden` 이 몇 군데 남아 있다. 원래는 둥근 컨테이너가 자식의 모서리를
     잘라 내려고 쓴 것인데, 지금은 `gap-px` 격자에서 자식 배경이 새는 것만 막는다.

## 현재 구조

**백엔드·프론트엔드 모두 "기능별(package-by-feature)"로 나눈다.** 계층별 구조는 파일이 몇 개
없을 땐 괜찮지만, 기능이 늘어나면 "차량 관련 파일 다 모아서 보기"가 여러 폴더를 오가야 한다.
기능별은 한 기능을 고칠 때 그 폴더 하나만 보면 되는 대신, **기능 간 경계를 넘는 import가 드러난다**
(`Vehicle`이 `User`를 참조하듯). 이건 자연스러운 트레이드오프이고 숨기려 하지 않는다.

DTO는 `request/` 와 `response/` 로 한 겹 더 나눈다. 폴더 수는 늘지만, "클라이언트가 보내는 것"과
"서버가 돌려주는 것"이 섞이지 않아 검증 애노테이션을 어디에 붙일지 헷갈리지 않는다.

**그 아래 한 겹이 더 있다 (2026-09-13).** `dto/request/login/`, `domain/entity/`,
`repository/jpa/`, `controller/rest/`, `shared/ui/base/` 처럼 **파일의 성격을 폴더 이름이 말한다.**
파일 개수를 기준으로 삼지 않으므로 파일 1개짜리 폴더가 많다 — 기준과 그 대가는
아래 트리 뒤에 정리해 뒀다.

### 저장소 루트

    odolog/
    ├── build.gradle                    의존성 (web/jpa/validation/crypto/springdoc/mariadb)
    │                                   springdoc은 서드파티라 버전을 직접 명시해야 함
    ├── settings.gradle                 rootProject.name = 'odolog'
    ├── gradlew / gradlew.bat           Gradle 래퍼 실행 스크립트
    ├── gradle/wrapper/
    │   ├── gradle-wrapper.jar
    │   └── gradle-wrapper.properties   받아올 Gradle 배포판 정보
    ├── .github/workflows/ci.yml        커밋마다 백엔드·프론트 검사. 백엔드 잡은 MariaDB
    │                                   컨테이너를 띄운다 — H2 로 바꾸면 ddl-auto 가 만드는
    │                                   스키마가 운영과 달라져 검증이 거짓말을 한다
    ├── .gitignore                      Gradle·IntelliJ·macOS 산출물 + .env
    ├── LICENSE                         MIT
    ├── CLAUDE.md                       설계 결정·이유·체크리스트 (작업용)
    ├── HISTORY.md                      완료한 작업과 그 근거 (작업 일지)
    ├── README.md                       소개·실행법·API 개요·트러블슈팅 (공개용)
    ├── src/                            백엔드 (Spring Boot)
    └── frontend/                       프론트엔드 (Vite + React)

    읽지 않는 경로: build/  .gradle/  .idea/  frontend/node_modules/  frontend/dist/

### 백엔드 — `src/main/java/com/odolog/app/`

계층(`domain`/`repository`/`dto`/`service`/`controller`) 아래에 **성격을 말하는 한 겹이 더** 있다.
`domain/entity` 와 `domain/type`(enum), `repository/jpa`(구현 기술), `dto/request/<유스케이스>`,
`service/application`, `controller/rest`(노출 방식).

    com/odolog/app/
    ├── OdoLogApplication.java                @SpringBootApplication.
    │                                         **앱 시간대를 Asia/Seoul 로 고정한다**(2026-09-23).
    │                                         @PastOrPresent 와 LocalDate.now() 가 JVM 기본
    │                                         시간대를 따라서, UTC 서버면 한국 사용자가 고른
    │                                         "오늘" 이 미래라 매일 오전 9시까지 400 이 난다.
    │                                         run() 보다 먼저 부른다 — 커넥션 풀과 Hibernate 가
    │                                         뜰 때 한 번 읽어 가므로 그 뒤엔 늦다.
    │                                         **이 파일만 더 내려가지 못한다.** 컴포넌트 스캔이
    │                                         이 클래스의 패키지부터 시작하므로 bootstrap/ 같은
    │                                         하위 폴더로 옮기면 scanBasePackages·@EntityScan·
    │                                         @EnableJpaRepositories 를 전부 손으로 지정해야 한다
    │
    ├── user/  ────────────────────────────── 회원가입·로그인·프로필
    │   ├── domain/
    │   │   └── entity/
    │   │       ├── User.java                 @Entity(users). uk_users_email 유니크 제약.
    │   │       │                             changeNickname()/changePhone() — setter 없음
    │   │       └── PasswordResetToken.java   @Entity. **원본이 아니라 SHA-256 해시를 저장한다** —
    │   │                                     DB 가 새어도 그것만으로 남의 비밀번호를 못 바꾼다.
    │   │                                     한 번 쓰면 used_at 이 찍혀 죽는다
    │   ├── repository/
    │   │   └── jpa/
    │   │       ├── UserRepository.java       findByEmail, existsByEmail
    │   │       └── PasswordResetTokenRepository.java
    │   │                                     findByTokenHash, deleteByUserId(재발급·탈퇴 공용),
    │   │                                     deleteByExpiresAtBefore(만료분 정리 — 스케줄러를
    │   │                                     두지 않고 request() 가 부른다. 토큰이 쌓이는
    │   │                                     유일한 경로가 거기라 쌓이는 만큼 치워진다)
    │   ├── dto/
    │   │   ├── request/
    │   │   │   ├── signup/
    │   │   │   │   └── SignUpRequest.java    @NotBlank/@Email/@Size(min=8,max=100).
    │   │   │   │                             phone은 선택이지만 @Size(max=20) 필수
    │   │   │   ├── login/
    │   │   │   │   └── LoginRequest.java     email, password
    │   │   │   └── profile/
    │   │   │       ├── UpdateProfileRequest.java
    │   │   │       │                         둘 다 nullable — 보낸 필드만 변경
    │   │   │       └── (password/)ChangePasswordRequest.java
    │   │   │                                 current/new 둘 다 @NotBlank — 부분 수정이 아니다.
    │   │   │                                 new 의 길이 제한은 가입과 같아야 한다
    │   │   └── response/
    │   │       └── profile/
    │   │           └── UserResponse.java     from() 팩토리. password는 절대 담지 않음
    │   ├── service/
    │   │   ├── mail/
    │   │   │   └── PasswordResetMailer.java  링크는 백엔드가 아니라 **프런트 주소**를 가리킨다 —
    │   │   │                                 토큰을 받아 입력받는 것은 화면의 일이다.
    │   │   │                                 **발송은 커밋 뒤, 다른 스레드에서**(2026-09-25) —
    │   │   │                                 요청 스레드에서 보내면 가입된 주소만 SMTP 시간만큼
    │   │   │                                 늦게 답해 응답 시간이 가입 여부를 알려준다
    │   │   └── application/
    │   │       ├── PasswordResetService.java request(메일 발송) / confirm(비밀번호 교체).
    │   │       │                             **없는 주소도 조용히 성공**시킨다 — 응답이 갈리면
    │   │       │                             그게 가입 여부 조회 API 가 된다.
    │   │       │                             메일 발송 실패도 삼키고 로그로만 남긴다(같은 이유)
    │   │       └── UserService.java          signUp(중복 체크·BCrypt), login(사유 통일),
    │   │                                     findById, updateProfile(널 아닌 필드만),
    │   │                                     verifyPassword(되돌릴 수 없는 동작 앞의 관문 —
    │   │                                     changePassword 와 탈퇴가 공유), changePassword, delete
    │   └── controller/
    │       └── rest/
    │           ├── PasswordResetController.java
    │           │                             POST·PATCH /api/users/password-reset (둘 다 204).
    │           │                             **로그인하지 않은 사람이 쓰는 유일한 쓰기 경로**
    │           └── UserController.java       POST /api/users, /login(+changeSessionId),
    │                                         /logout(204), GET·PATCH /api/users/me,
    │                                         PATCH /api/users/me/password(204)
    │
    ├── vehicle/  ─────────────────────────── 차량 등록·조회·주행거리·삭제
    │   ├── domain/entity/Vehicle.java        @Entity(vehicles). owner→User(@ManyToOne LAZY).
    │   │                                     uk_vehicles_user_plate_number(소유자+번호판 복합).
    │   │                                     주행거리를 건드리는 메서드가 셋. 의도가 달라 이름도 셋이다:
    │   │                                     updateOdometer()는 감소 시 ConflictException,
    │   │                                     liftOdometerTo()는 크면 올리고 작으면 넘어간다
    │   │                                     (정비·주유를 기록하다 따라오는 경로),
    │   │                                     correctOdometer()는 감소도 그대로 반영한다
    │   │                                     — 계기판 교체·자리수 오타 정정 전용, force 로만 닿는다
    │   ├── repository/jpa/VehicleRepository.java
    │   │                                     findByPublicId(URL 의 공개 id — findOwnedVehicle 이 쓴다),
    │   │                                     findByOwnerId(Pageable), findAllByOwnerId(탈퇴용 —
    │   │                                     "한 사람의 전부"가 대상이라 페이지를 나눌 수 없다),
    │   │                                     existsByOwnerIdAndPlateNumber(소유자별 중복 검사)
    │   ├── dto/
    │   │   ├── request/
    │   │   │   ├── register/VehicleRegisterRequest.java
    │   │   │   │                             owner 없음 — 세션에서 식별.
    │   │   │   │                             modelYear @NotNull/@Min(1900)/@Max(2100)
    │   │   │   ├── odometer/UpdateOdometerRequest.java   @PositiveOrZero
    │   │   │   └── update/VehicleUpdateRequest.java
    │   │   │                                 전부 nullable. @NotBlank 대신
    │   │   │                                 @Size(min=1)+@Pattern — 둘 다 null 을 통과시킨다
    │   │   └── response/
    │   │       └── vehicle/VehicleResponse.java
    │   │                                     owner 없음 — LAZY 미접근으로 N+1 방지
    │   ├── service/application/VehicleService.java
    │   │                                     register,
    │   │                                     findMyVehicles(Pageable) — **DTO 를 돌려준다.**
    │   │                                     지남 수가 엔티티에 없는 계산값이라서
    │   │                                     (FuelRecordService.findByVehicle 과 같은 이유).
    │   │                                     쿼리 3번 — 이력·주기를 소유자 단위로 한 번에 읽고
    │   │                                     나눈다. 차량마다면 페이지 크기만큼 는다,
    │   │                                     update(번호판이 실제로 바뀔 때만 중복 검사),
    │   │                                     updateOdometer(dirty checking),
    │   │                                     delete(이력 먼저 → 차량),
    │   │                                     deleteAllOwnedBy(탈퇴용 일괄 삭제),
    │   │                                     findOwnedVehicle(남의 차량도 404 — 규칙 11.
    │   │                                     maintenance·fuel 도 이걸 재사용)
    │   └── controller/rest/VehicleController.java
    │                                         POST·GET /api/vehicles,
    │                                         GET·PATCH·DELETE /api/vehicles/{id},
    │                                         PATCH /api/vehicles/{id}/odometer
    │
    ├── maintenance/  ─────────────────────── 정비 이력·다음 정비 시점
    │   ├── domain/
    │   │   ├── calculation/NextService.java  다음 정비 시점 + **지남 판정**(2026-09-25 신설).
    │   │   │                                 엔티티가 아니라 값 계산이라 entity 와 형제 —
    │   │   │                                 fuel/domain/calculation 과 같은 자리.
    │   │   │                                 **차량 상세와 홈 요약이 공유한다** — 두 벌이면
    │   │   │                                 한 화면은 지났다 하고 한 화면은 아무 말도 안 한다.
    │   │   │                                 km·개월 중 **하나라도** 넘으면 지남(권장 주기가
    │   │   │                                 "먼저 오는 것"이라서). 딱 그 값·그 날도 지남.
    │   │   │                                 **지난 것이 먼저** 오도록 정렬 — 이 목록은
    │   │   │                                 "뭘 해야 하나"를 보는 자리다
    │   │   ├── entity/ServiceInterval.java   차량별 권장 주기(2026-09-25 신설).
    │   │   │                                 주기가 enum 상수로 고정돼 있어 엔진오일이 언제나
    │   │   │                                 5,000km(광유 기준)였다 — 합성유는 10,000~15,000km 라
    │   │   │                                 **`지남` 이 늘 켜진 경고등**이 됐고, 늘 켜진 경고는
    │   │   │                                 아무도 안 본다. 차량 단위인 이유: 주기는 사람이 아니라
    │   │   │                                 차의 성질이다. km·개월을 따로 비울 수 있다 —
    │   │   │                                 합성유는 거리만 늘고 기간은 그대로인 게 보통.
    │   │   │                                 둘 다 비면 행을 지운다(customized 가 거짓말하지 않게)
    │   │   ├── entity/MaintenanceRecord.java @Entity. type은 @Enumerated(STRING).
    │   │   │                                 필드별 change 메서드 5개
    │   │   └── type/ServiceType.java         enum 15종(부위별로 묶어 선언 — 화면 선택 목록이
    │   │                                     이 순서를 따른다). recommendedIntervalKm +
    │   │                                     recommendedIntervalMonths (OTHER는 둘 다 null).
    │   │                                     **entity 와 형제 폴더로 갈라 둔 이유**: 엔티티가 아니라
    │   │                                     값의 종류라서, 한 폴더에 섞이면 @Entity 인지
    │   │                                     아닌지를 파일을 열어 봐야 안다
    │   ├── repository/jpa/MaintenanceRecordRepository.java
    │   │                                     findByVehicleId(Pageable),
    │   │                                     findByVehicleIdOrderByServiceDateDescIdDesc(종류별
    │   │                                     최신 1건을 한 번에 — 종류마다 findTopBy 면 15쿼리),
    │   │                                     findByPublicIdAndVehicleId(타 차량 소속 차단),
    │   │                                     deleteByVehicleId
    │   ├── dto/
    │   │   ├── request/
    │   │   │   ├── register/MaintenanceRecordRegisterRequest.java
    │   │   │   │                             @NotNull/@PositiveOrZero/@Size
    │   │   │   └── update/MaintenanceRecordUpdateRequest.java
    │   │   │                                 전부 nullable. cost/serviceOdometer는
    │   │   │                                 Integer로 "안 보냄"과 "0"을 구분
    │   │   └── response/
    │   │       ├── record/MaintenanceRecordResponse.java     from() 팩토리
    │   │       └── schedule/NextServiceResponse.java         주행거리·날짜 두 기준 + overdue.
    │   │                                                     판정을 서버가 하는 이유 — 화면이
    │   │                                                     직접 오늘과 비교하면 차량 상세와
    │   │                                                     홈이 다른 말을 하게 된다
    │   ├── service/application/MaintenanceRecordService.java
    │   │                                     register(+차량 주행거리 자동 갱신),
    │   │                                     findByVehicle(Pageable),
    │   │                                     calculateAllNextServices(km·개월, 이력 있는 종류만),
    │   │                                     update(부분), delete.
    │   │                                     VehicleService.findOwnedVehicle()를 주입받아 재사용
    │   └── controller/rest/MaintenanceRecordController.java
    │                                         POST·GET  .../maintenance-records,
    │                                         PATCH·DELETE  .../{recordId},
    │                                         GET  .../next-services (이력 있는 종류 전체).
    │                                         단건 조회와 next-service(단수)는 화면이 안 써서
    │                                         2026-09-16 에 걷어냈다
    │
    ├── fuel/  ────────────────────────────── 주유 기록·연비. maintenance 와 같은 모양이다
    │   ├── domain/entity/FuelRecord.java     @Entity(fuel_records). vehicle→Vehicle(LAZY).
    │   │                                     liters 는 BigDecimal(6,2) — int 로는 32.45L 를
    │   │                                     못 담고, double 은 합산 시 오차가 쌓인다.
    │   │                                     단가가 아니라 총액(total_cost)을 저장한다.
    │   │                                     **liters·total_cost 는 비어 있을 수 있다**
    │   │                                     (2026-09-23). 그래서 total_cost 가 int 가 아니라
    │   │                                     Integer 다(원칙 8). 0 으로 채우지 않는 이유 —
    │   │                                     0 은 "0L 를 0원에 넣었다" 라는 다른 사실이고,
    │   │                                     연비가 0 으로 나누기가 된다.
    │   │                                     더할 때만 둘이 같은 뜻이라 totalCostOrZero() 가 있다
    │   ├── repository/jpa/FuelRecordRepository.java
    │   │                                     findByVehicleId(Pageable), findByPublicIdAndVehicleId,
    │   │                                     findPrevious(직전 1건 — **(주행거리, id) 순서**.
    │   │                                     주행거리만 보면 같은 값 2건이 페이지 경계에 걸릴 때
    │   │                                     같은 구간이 두 번 보인다. 이 저장소의 유일한 @Query),
    │   │                                     findAllByVehicleIdOrderByOdometerAscIdAsc(요약용),
    │   │                                     deleteByVehicleId
    │   ├── domain/calculation/              엔티티가 아니라 **값 계산**이라 entity 와 형제로 뒀다
    │   │   ├── FuelEfficiency.java           평균 연비 공식. 차량 상세와 홈 요약이 **공유한다** —
    │   │   │                                 두 벌이면 화면마다 다른 연비가 뜬다(2026-09-17 통합).
    │   │   │                                 Σ(구간 거리) ÷ Σ(구간 주유량). 총합 나누기와 결과는
    │   │   │                                 같지만, 구간 단위라야 불가능한 구간을 골라낼 수 있다
    │   │   └── FuelAnomaly.java              빠진 기록 탐지. 절대 임계값으로는 못 잡아 그 차량의
    │   │                                     평소 구간과 견준다. 기준은 평균이 아니라 **중앙값** —
    │   │                                     평균은 잡으려는 이상값 자체에 끌려 올라간다
    │   ├── dto/
    │   │   ├── request/{register,update}/    liters 는 @Positive — 0 이면 연비가 0으로 나누기다.
    │   │   │                                 날짜에 @PastOrPresent (2026-09-18).
    │   │   │                                 **liters·totalCost 에 @NotNull 이 없다**(2026-09-23).
    │   │   │                                 수정 쪽은 clearLiters·clearTotalCost 플래그가 따로 있다 —
    │   │   │                                 JSON 은 "키가 없음"과 "null"이 서버에 똑같이 도착해서
    │   │   │                                 null 하나로는 '유지'와 '비움'을 못 가른다.
    │   │   │                                 **Optional 로 감싸는 방법은 안 된다** — Jackson 이
    │   │   │                                 키가 없을 때도 Optional.empty() 를 채운다(테스트로 확인)
    │   │   └── response/
    │   │       ├── record/FuelRecordResponse.java
    │   │       │                             저장값 + 계산값(단가·거리·연비)이 함께 온다.
    │   │       │                             표시 플래그 둘: efficiencySuspicious(불가능한 값) ·
    │   │       │                             missingRecordSuspected(기록이 빠진 구간).
    │   │       │                             **한 행에 둘이 같이 붙지 않는다** — 무엇을 하라는
    │   │       │                             건지 흐려지므로 불가능한 값 쪽이 먼저다.
    │   │       │                             **계산값은 DB 에 없다** — 직전 기록이 바뀌면
    │   │       │                             달라지므로 읽을 때 계산해야 언제나 맞다.
    │   │       │                             구간이 성립 안 하면 null(0 이 아니다)
    │   │       └── summary/FuelSummaryResponse.java
    │   │                                     평균 + latestRecordId·resetPointId(연비 초기화용),
    │   │                                     longSegmentCount(빠진 기록으로 보여 평균에서 뺀
    │   │                                     구간 — 2026-09-23 부터 세기만 하지 않고 실제로 뺀다),
    │   │                                     excludedSegmentCount(평균에서 뺀 구간 수 —
    │   │                                     말없이 빼면 그것도 거짓말이라 개수를 밝힌다)
    │   ├── service/application/FuelRecordService.java
    │   │                                     register(+차량 주행거리 자동 갱신),
    │   │                                     findByVehicle(페이지당 쿼리 3번 — 세 번째가
    │   │                                     '평소 구간' 전체 조회. 페이지 안에서 중앙값을 내면
    │   │                                     같은 기록이 페이지마다 다르게 판정된다),
    │   │                                     update, delete, summary.
    │   │                                     **FIXED_SORT 로 정렬을 고정한다** — 정렬이 곧
    │   │                                     연비 계산의 전제라 sort 파라미터를 무시한다
    │   └── controller/rest/FuelRecordController.java
    │                                         POST·GET  .../fuel-records,
    │                                         GET  .../summary (리터럴이 {recordId} 보다 우선),
    │                                         PATCH·DELETE  .../{recordId}
    │
    ├── account/  ─────────────────────────── 조율 층 ①. **여러 기능을 동시에 알아도 되는 자리**
    │   │                                     (프론트의 app/ 과 같은 성격 — 아래 "의존 방향" 참고).
    │   │                                     계정 전체에 걸친 동작 둘이 여기 있다. **거울상이다** —
    │   │                                     한쪽은 전부 지우고(탈퇴) 한쪽은 전부 가져간다(내보내기).
    │   │                                     그래서 주입받는 것도 다르다: 지우는 쪽은 순서를 조율해야
    │   │                                     해서 서비스를, 내보내는 쪽은 원본만 필요해 리포지토리를
    │   ├── dto/response/export/AccountExportResponse.java
    │   │                                     차량 밑에 이력·주유·**차량별 주기**를 중첩한다 — 평평하게
    │   │                                     내보내면 어느 기록이 어느 차의 것인지 우리 DB 안에서만
    │   │                                     뜻이 있는 id 로만 안다.
    │   │                                     **계산값(연비·단가)과 비밀번호 해시는 담지 않는다.**
    │   │                                     주기가 빠져 있으면 복원한 차가 기본값으로 돌아가
    │   │                                     `지남` 이 다시 늘 켜진다 — 말없이 사라지는 설정이다
    │   ├── dto/request/restore/AccountRestoreRequest.java
    │   │                                     내보낸 JSON 을 되돌려받는다. 패키지가 restore 인
    │   │                                     이유는 **import 가 자바 예약어**라서.
    │   │                                     **사용자 정보는 안 받는다** — 가져오기는 내 계정에
    │   │                                     기록을 더하는 것이지 계정을 바꾸는 게 아니다
    │   ├── service/application/AccountRestoreService.java
    │   │                                     복원할 수 없으면 백업이 아니라 기념품이다.
    │   │                                     규칙 셋: 같은 번호판이면 기록만 붙이고 차량 정보는
    │   │                                     안 건드린다(파일이 옛날 것일 수 있다) ·
    │   │                                     **같은 기록은 건너뛴다**(두 번 넣어도 두 배가 되지
    │   │                                     않아야 한다 — id 가 JSON 에 없어 종류·날짜·주행거리로
    │   │                                     판정) · 하나라도 걸리면 전부 안 들어간다
    │   ├── service/application/AccountExportService.java
    │   │                                     export(ownerId, exportedAt) — 쿼리 3번.
    │   │                                     "언제" 를 밖에서 받는다(테스트에서 고정하려고)
    │   ├── dto/request/withdraw/WithdrawRequest.java
    │   │                                     비밀번호 @NotBlank. 체크박스로 대신하지 않는다 —
    │   │                                     그건 실수만 막고 본인 확인이 아니다
    │   ├── service/application/AccountWithdrawalService.java
    │   │                                     withdraw(비밀번호 확인 → 차량·이력 → 사용자).
    │   │                                     순서만 정하고 실제 삭제는 각 기능이 한다
    │   └── controller/rest/AccountController.java
    │                                         GET /api/users/me/export,
    │                                         DELETE /api/users/me(204) + 세션 invalidate.
    │                                         **URL 은 users 인데 패키지는 account** — UserController
    │                                         에 두면 user 가 account 를 알게 되어 순환이다.
    │                                         비밀번호는 본문에 싣는다(URL 에 넣으면 로그에 남는다)
    │
    ├── summary/  ─────────────────────────── 조율 층 ②. 홈 화면 요약 (2026-09-17 신설)
    │   │                                     account 와 같은 문제를 풀지만 성격이 다르다 —
    │   │                                     이쪽은 **읽어서 합친다.** 그래서 서비스가 아니라
    │   │                                     **리포지토리를 주입받는다**: 집계에는 각 기능의
    │   │                                     비즈니스 규칙(소유권 검사·삭제 순서)이 필요 없고,
    │   │                                     소유자 id 로 조회하므로 남의 데이터가 안 섞인다
    │   ├── dto/response/garage/GarageSummaryResponse.java
    │   │                                     홈 한 장에 필요한 값 전부. 타일·월별·종류별·
    │   │                                     차량별(평균 연비 + **지난 정비 수**)·최근 활동.
    │   │                                     안에 record 넷이 중첩돼 있고,
    │   │                                     RecentActivity 는 정비·주유 공용이라 kind 로 가른다
    │   ├── service/application/GarageSummaryService.java
    │   │                                     summarize(ownerId, today) — **쿼리 3번, HTTP 1번**.
    │   │                                     "오늘"을 밖에서 받는다(안에서 now() 를 부르면
    │   │                                     월별 12칸을 테스트에서 고정할 수 없다).
    │   │                                     차량 이름은 이미 읽어 둔 목록에서 찾는다 — LAZY
    │   │                                     프록시를 건드리면 1차 캐시에 기대는 코드가 된다
    │   └── controller/rest/GarageSummaryController.java
    │                                         GET /api/summary. **/api/vehicles/summary 가 아닌
    │                                         이유** — 정비·주유까지 담아 차량의 하위 자원이 아니다
    │
    └── common/  ──────────────────────────── 기능 어디에도 속하지 않는 공통 인프라
        ├── auth/
        │   ├── ratelimit/LoginAttemptLimiter.java
        │   │                                 비밀번호 대입 방어. 10분 안에 10번 실패하면 10분 잠금.
        │   │                                 **계정이 없어도 센다** — 없는 이메일만 빨리 답하면
        │   │                                 그 자체가 존재 여부를 알려준다. 인메모리라 재시작하면 잊는다.
        │   │                                 **세 곳이 키만 갈라 쓴다**: 로그인(이메일) ·
        │   │                                 재설정 요청(`password-reset:`+이메일) ·
        │   │                                 회원가입(`signup:`+IP). 그래서 잠겼을 때의 문구는
        │   │                                 부르는 쪽이 넘긴다 — 안 그러면 가입 화면에
        │   │                                 "로그인 시도가 너무 많습니다" 가 뜬다.
        │   │                                 **이름이 이미 좁다** — 셋을 다 뜻하는 이름으로
        │   │                                 바꿀 값이 생기면 그때 바꾼다
        │   ├── csrf/CsrfTokenFilter.java     쿠키의 토큰과 헤더의 토큰을 비교(double submit).
        │   │                                 세션 보관 방식을 안 쓴 이유는 토큰을 내주려면 세션이
        │   │                                 필요해져 비로그인 방문자에게도 세션이 생기기 때문.
        │   │                                 **테스트에서는 꺼 둔다** — @WebMvcTest 가 Filter 빈을
        │   │                                 같이 올려서 기존 쓰기 테스트가 전부 403 이 된다
        │   ├── session/LoginSessionRegistry.java
        │   │                                 사용자별 로그인 세션 목록(2026-09-25). 비밀번호 변경은
        │   │                                 **지금 세션만 남기고**, 재설정·탈퇴는 **전부** 끊는다.
        │   │                                 세션이 14일이라 안 끊으면 훔친 세션이 재설정 뒤에도 산다.
        │   │                                 세션과 같이 메모리에 둔다 — DB 버전 비교는 요청마다
        │   │                                 조회가 늘고 WebConfig 가 리포지토리를 알게 된다.
        │   │                                 등록할 때 다른 사용자 목록에서 먼저 뺀다 — 로그인은 세션을
        │   │                                 id 만 바꿔 다시 쓰므로 로그아웃 없이 계정을 바꾸면 둘에 남는다
        │   ├── annotation/LoginUser.java     @Target(PARAMETER) 커스텀 애노테이션
        │   ├── resolver/LoginUserArgumentResolver.java
        │   │                                 세션 LOGIN_USER_ID → Long 주입. 없으면 401
        │   └── constant/SessionConst.java    세션 키 상수
        ├── domain/
        │   ├── identifier/PublicId.java      URL·API 용 12자 무작위 id(SecureRandom, 약 71비트).
        │   │                                 차량·정비 이력·주유 기록이 쓴다 — 규칙 9-1
        │   └── entity/BaseTimeEntity.java    @MappedSuperclass + @EntityListeners.
        │                                     createdAt/updatedAt 을 네 엔티티가 상속받는다.
        │                                     테이블을 만들지 않고 필드만 자식에 합쳐지므로
        │                                     컬럼 이름이 그대로다(ddl-auto 가 안 건드린다)
        ├── text/InputText.java               입력 앞뒤 공백 정리(strip). 규칙 14-1
        ├── validation/
        │   ├── limit/InputLimits.java         주행거리·금액 상한 상수. 애노테이션에 숫자를 직접
        │   │                                  적으면 8곳에 흩어져 한 곳만 고치게 된다.
        │   │                                  **상한의 목적은 "말이 되는 값인가" 가 아니라
        │   │                                  되돌릴 수 없게 망가지는 것을 막는 것** —
        │   │                                  liftOdometerTo 가 최댓값을 잡아 두므로 한 번 크게
        │   │                                  잘못 넣으면 그 뒤 모든 폼이 그 값을 기준으로 말한다.
        │   │                                  프런트 shared/lib/limits 와 같은 숫자(의도한 중복)
        │   ├── annotation/MaxBytes.java       UTF-8 바이트 상한. @Size 는 글자 수라
        │   │                                  한글에서 3배로 벌어진다 — BCrypt 의 72바이트
        │   │                                  상한을 @Size(max = 100) 이 못 막았다
        │   └── validator/MaxBytesValidator.java
        │                                      null 은 통과시킨다 — "비었는가"는 @NotBlank 의 몫
        ├── schema/
        │   └── drift/SchemaDriftChecker.java
        │                                     기동할 때 엔티티의 nullable 과 실제 DB 컬럼을 한 번
        │                                     대조하고, 어긋나면 고칠 ALTER 까지 찍는다(경고만,
        │                                     막지는 않는다). **ddl-auto: validate 로는 안 된다** —
        │                                     Hibernate 의 스키마 검증은 존재와 타입만 보고
        │                                     nullability 는 아예 보지 않는다(2026-09-23 실험으로 확인).
        │                                     @Column·@JoinColumn 이 붙은 필드만 본다 — 애노테이션이
        │                                     없으면 기본값을 추측해야 하는데 기본형에 Hibernate 가
        │                                     NOT NULL 을 붙이는 등 예외가 많아 오탐이 난다.
        │                                     **유니크 제약도 이름으로 양방향 대조한다**(2026-09-27):
        │                                     DB 에 없음(생성이 조용히 실패 — 공개 id 때 밟을 뻔했다) /
        │                                     엔티티에 없음(옛 제약이 남음 — 9/7 번호판 때 밟았다).
        │                                     규칙 6 덕에 모든 유니크에 이름이 있어 가능하다
        ├── web/
        │   └── header/SecurityHeadersFilter.java
        │                                     모든 응답에 nosniff · X-Frame-Options: DENY ·
        │                                     Referrer-Policy: no-referrer. 스프링 시큐리티를
        │                                     안 써서 공짜로 따라오는 헤더가 하나도 없다.
        │                                     HSTS 는 request.isSecure() 일 때만 — http 에서
        │                                     켜면 그 도메인이 https 전용으로 굳는다.
        │                                     config/web 이 아닌 이유: WebConfig 는 한 번 알려주는
        │                                     설정이고 이쪽은 요청마다 도는 실행 코드
        ├── config/
        │   ├── web/WebConfig.java            ArgumentResolver 등록 + CORS(5173, credentials).
        │   │                                 **CORS 는 필터로, 맨 앞에**(2026-09-25). addCorsMappings 는
        │   │                                 필터 뒤에서 붙어서 CsrfTokenFilter 의 403 에 헤더가 없었다 —
        │   │                                 브라우저가 그걸 "서버에 연결하지 못했습니다" 로 읽는다
        │   ├── jpa/JpaAuditingConfig.java    @EnableJpaAuditing 스위치.
        │   │                                 **OdoLogApplication 에 두면 @WebMvcTest 가 전부
        │   │                                 깨진다**(JPA 메타모델이 비어 있음). 대신 여기 두면
        │   │                                 @DataJpaTest 가 못 집어 가므로 리포지토리 테스트에
        │   │                                 @Import 가 필요하다 — 실제로 둘 다 밟고 정했다
        │   └── openapi/OpenApiConfig.java    문서 제목/설명 + @LoginUser를 스펙에서 제외
        ├── dto/
        │   ├── request/
        │   │   └── page/SortGuard.java       정렬 가능한 속성을 화이트리스트로 제한(2026-09-23).
        │   │                                 Spring Data 는 ?sort=owner.password 처럼 연관
        │   │                                 엔티티를 타고 들어가는 정렬을 그대로 받는다 —
        │   │                                 암묵적 조인이 생기고 의도한 적 없는 표면이 열린다.
        │   │                                 **블랙리스트가 아닌 이유**: 엔티티에 필드를 더하면
        │   │                                 자동으로 정렬 대상이 된다. 막을 것을 세는 쪽은 뒤처진다
        │   └── response/                     요청 DTO가 없어 response만 있다
        │       ├── error/ErrorResponse.java   record(message)
        │       └── page/PageResponse.java     record<T>(items/page/size/totalElements/
        │                                      totalPages/hasNext) + Page<T>.from()
        └── exception/                        ※ 기능별로 나누지 않는다. 세 기능이 모두 쓰는
            │                                   것이라 어느 한 기능으로 옮기면 잘못된 방향의
            │                                   의존이 생긴다
            ├── type/                         예외 타입만 모아 둔다 (상태 코드 하나당 하나)
            │   ├── ConflictException.java              409 전용
            │   ├── InvalidRequestException.java        400 전용. 지금까지 400 은 전부
            │   │                                        프레임워크가 만들었는데, 검증 애노테이션으로
            │   │                                        표현할 수 없는 규칙(정렬 화이트리스트)이
            │   │                                        생겨 추가했다
            │   ├── TooManyRequestsException.java       429 전용. 로그인·재설정 요청·회원가입
            │   │                                        셋이 같은 리미터를 키만 갈라 쓴다
            │   ├── AuthenticationFailedException.java  401 전용
            │   ├── ForbiddenAccessException.java       403 전용. **지금 던지는 곳이 없다** —
            │   │                                        남의 자원은 404 로 통일(규칙 11)
            │   └── ResourceNotFoundException.java      404 전용
            └── handler/GlobalExceptionHandler.java
                                              **ResponseEntityExceptionHandler 를 이어받는다**(2026-09-26).
                                              405·415·404 같은 스프링 내부 예외는 부모가 맡아 상태 코드를
                                              지키고, 본문만 handleExceptionInternal 이 우리 모양으로 바꾼다.
                                              그 밖의 예외는 맨 아래 Exception 처리기가 **500 + 우리 문구**
                                              (규칙 11). 부모 없이 Exception 을 잡으면 4xx 까지 500 이 된다.
                                              ⚠️ HttpMessageNotReadable·MethodArgumentNotValid 는
                                              @ExceptionHandler 가 아니라 **덮어쓰기**다 — 부모가 이미 맡고 있어
                                              둘이면 기동이 실패한다.
                                              409/401/403/404/400/429 는 전용 예외만 잡는다 — IllegalArgument
                                              같은 JDK 범용 예외를 4xx 로 매핑하지 않는다(규칙 12). 그것들은
                                              500 으로 간다. 예외 문구는 내보내지 않는다(내부 사정이 실린다).
                                              DataIntegrityViolation 은 UNIQUE 면 409, 아니면 500 + 우리 문구.
                                              PropertyReferenceException(잘못된 sort) → 400

**같은 패키지였던 것이 갈라지면 import 가 새로 필요해진다.** 세분화하면서 실제로 컴파일이
세 곳에서 깨졌다: `LoginUserArgumentResolver`(→`LoginUser`,`SessionConst`),
`GlobalExceptionHandler`(→예외 4개), `MaintenanceRecord`(→`ServiceType`). 전에는 같은 패키지라
import 없이 쓰던 것들이다. **이건 부작용이 아니라 세분화가 드러낸 결합이다** — 이제 파일 맨 위만
봐도 그 클래스가 무엇에 기대는지 보인다.

### 백엔드 — 리소스와 테스트

    src/main/resources/application.yml   MariaDB 접속(${DB_USERNAME}/${DB_PASSWORD}),
                                         ddl-auto=update, open-in-view=false.
                                         세션 쿠키 http-only + same-site=lax (브라우저 기본값에
                                         기대지 않는다). **세션 14일**(2026-09-25) —
                                         톰캣 기본 30분이면 영수증 정리하다 로그아웃된다.
                                         timeout 과 cookie.max-age 를 **같이** 늘려야 한다:
                                         서버가 기억해도 세션 쿠키면 브라우저를 닫는 순간 끝난다. secure 는 ${SESSION_COOKIE_SECURE:false} —
                                         로컬이 http 라 기본은 꺼짐이고 배포에서 환경변수로 켠다.
                                         springdoc.swagger-ui.csrf 로 Swagger 가 토큰을 실어 보낸다
                                         (없으면 문서에서 쓰기 요청을 못 쏜다).
                                         spring.mail.* 은 재설정 메일용 — 자격증명은 DB 와 같이
                                         환경변수로만 받는다. 안 넣으면 발송만 실패하고 앱은 뜬다.
                                         odolog.app.base-url 은 메일 본문의 링크가 가리킬
                                         **프런트** 주소다(백엔드가 아니다)
                                         **SQL 로깅은 개발 전용** — bind:trace 가 이메일·닉네임과
                                         BCrypt 해시까지 찍는다. 배포하면 반드시 꺼야 한다
    src/main/resources/application-prod.yml
                                         운영 전용 덮어쓰기. SPRING_PROFILES_ACTIVE=prod 하나로
                                         springdoc 문서를 닫고, SQL·bind 로깅을 끄고,
                                         세션 쿠키 secure 를 켠다(CsrfTokenFilter 가 같은 키를
                                         읽으므로 CSRF 쿠키도 같이 따라온다).
                                         **주석으로 "배포할 때 끄세요" 라고 적어 두는 것과의
                                         차이가 이 파일의 전부다** — 주석은 사람이 기억해야 하고
                                         프로파일은 환경변수가 대신 기억한다

    src/test/resources/application.yml   odolog_test 스키마, ddl-auto=create-drop.
                                         계정이 이 스키마 전용이라 파일에 그대로 적혀 있음.
                                         odolog.csrf.enabled=false — 켜 두면 @WebMvcTest 가 Filter 빈을
                                         함께 올려 기존 쓰기 테스트 30여 개가 토큰 없이 403 이 된다.
                                         spring.mail.host 도 있어야 한다 — 없으면 JavaMailSender 빈이
                                         안 만들어져 @SpringBootTest 가 컨텍스트를 못 띄운다

**테스트는 대상과 같은 경로를 그대로 따라간다.** 총 264개.

    src/test/java/com/odolog/app/
    ├── common/
    │   ├── auth/
    │   │   ├── ratelimit/LoginAttemptLimiterTest.java
    │   │   │                                   시계를 밖에서 넣는다 — 안에서 now() 를 부르면
    │   │   │                                   잠금 만료를 테스트할 수 없다. 대소문자 우회도 본다
    │   │   └── csrf/CsrfTokenFilterTest.java   필터를 직접 호출한다. @WebMvcTest 로 하면
    │   │                                       Filter 빈이 같이 올라와 기존 테스트가 전부 403
    │   ├── web/header/SecurityHeadersFilterTest.java
    │   │                                       헤더 셋이 붙는지 + HSTS 는 https 에만 붙는지
    │   └── schema/drift/SchemaDriftCheckerTest.java
    │                                           @SpringBootTest — 컬럼을 일부러 어긋나게 만들고
    │                                           되돌린다. 양방향 다 본다.
    │                                           **이 장치가 조용히 고장 나면 그때부터
    │                                           아무것도 못 잡는다**
    ├── user/
    │   ├── repository/jpa/PasswordResetTokenRepositoryTest.java
    │   │                                              @DataJpaTest — 해시 조회, 해시 유니크,
    │   │                                              일괄 삭제. IDENTITY 라 save() 시점에 터진다
    │   ├── service/application/PasswordResetServiceTest.java
    │   │                                              Mockito — 없는 주소는 조용히, 저장은 해시로,
    │   │                                              만료·재사용 거절, **메일 실패해도 성공**
    │   ├── controller/rest/PasswordResetControllerTest.java
    │   │                                              @WebMvcTest — 204/400/401/429.
    │   │                                              가입 여부와 무관하게 같은 응답인지
    │   ├── repository/jpa/UserRepositoryTest.java      @DataJpaTest — save/findByEmail/
    │   │                                              existsByEmail + 이메일 유니크 위반 시
    │   │                                              올라오는 예외의 "모양" 고정
    │   ├── service/application/UserServiceTest.java    Mockito — 중복·암호화·로그인·부분수정
    │   └── controller/rest/UserControllerTest.java     @WebMvcTest — 201/409, 세션 저장, /me
    ├── vehicle/
    │   ├── repository/jpa/VehicleRepositoryTest.java   @DataJpaTest — 페이징·LAZY·주행거리·
    │   │                                              소유자별 번호판 중복
    │   ├── service/application/
    │   │   ├── VehicleServiceTest.java                 Mockito — 404·403·감소방지·
    │   │   │                                           삭제순서(InOrder)
    │   │   └── VehicleServiceTransactionTest.java      @SpringBootTest — 유일하게 진짜 컨테이너를
    │   │                                               띄운다. dirty checking이 DB까지 가는지 검증
    │   └── controller/rest/VehicleControllerTest.java  @WebMvcTest — 401/400/201/404/403,
    │                                                   페이지 응답
    ├── fuel/
    │   ├── domain/calculation/
    │   │   ├── FuelEfficiencyTest.java             순수 계산 — 첫 주유량 제외, 불가능 구간 제외,
    │   │   │                                       거리 0 구간, 기준점 이후만, 2건 미만
    │   │   └── FuelAnomalyTest.java                임계보다 **판단하지 않아야 할 때**를 더 본다:
    │   │                                           고른 구간, 1.5배 편차, 구간 3개 미만,
    │   │                                           그리고 **장거리 여행**(거리만 두 배)
    │   ├── repository/jpa/FuelRecordRepositoryTest.java
    │   │                                           @DataJpaTest — 직전 기록 조회, 타 차량 차단,
    │   │                                           BigDecimal 소수 보존, 일괄 삭제
    │   ├── service/application/FuelRecordServiceTest.java
    │   │                                           Mockito — 연비 계산, 페이지 경계(쿼리 2번),
    │   │                                           차량 주행거리 자동 갱신, 평균 연비, 연비 초기화
    │   └── controller/rest/FuelRecordControllerTest.java
    │                                               @WebMvcTest — 201/401/400(0L·누락·소수 3자리·
    │                                               미래 날짜), /summary 라우팅, 목록 페이지
    ├── account/
    │   ├── service/application/AccountExportServiceTest.java
    │   │                                           Mockito — 이력을 각 차량 밑으로 나누는지,
    │   │                                           비밀번호 해시가 안 담기는지, 빈 계정
    │   ├── service/application/AccountWithdrawalServiceTest.java
    │   │                                           Mockito — 삭제 순서(InOrder),
    │   │                                           비밀번호 틀리면 아무것도 안 지움
    │   └── controller/rest/AccountControllerTest.java
    │                                               @WebMvcTest — 204+세션 무효화, 401, 400
    ├── summary/
    │   ├── service/application/GarageSummaryServiceTest.java
    │   │                                           Mockito — 월별 12칸 경계, 종류별 정렬,
    │   │                                           차량별 평균 연비, **같은 날짜면 정비 먼저**
    │   └── controller/rest/GarageSummaryControllerTest.java
    │                                               @WebMvcTest — 200/401
    └── maintenance/
        ├── domain/type/ServiceTypeTest.java            값을 다시 적지 않고 **약속만** 고정 —
        │                                               "OTHER 를 뺀 모든 종류는 주기가 최소
        │                                               하나", 양수, 이름 30자 이하(컬럼 폭)
        ├── repository/jpa/MaintenanceRecordRepositoryTest.java
        │                                               @DataJpaTest — 같은 날짜 동점 처리,
        │                                               페이징, 타 차량 차단, 이력 일괄 삭제
        ├── service/application/MaintenanceRecordServiceTest.java
        │                                               Mockito — 다음정비 3케이스, 부분수정,
        │                                               차량 주행거리 따라 올리기
        └── controller/rest/MaintenanceRecordControllerTest.java
                                                        @WebMvcTest — next-services, enum 400,
                                                        미래 날짜 400·오늘 201, delete 204

    ※ Mockito 테스트는 스프링 프록시를 안 거치므로 `@Transactional` 이 아예 적용되지 않고,
      `@WebMvcTest` 는 서비스가 `@MockitoBean` 이라 진짜 코드가 돌지 않는다. 즉 트랜잭션 설정
      실수는 이 둘로는 절대 못 잡는다 — 그래서 `VehicleServiceTransactionTest` 하나를 둔다.

### 프론트엔드 — `frontend/`

백엔드와 같은 기준으로 한 겹 더 내려간다. 화면은 화면 이름 폴더 안에(`pages/login/LoginPage.tsx`),
`api` 는 `endpoints/` 와 `types/` 로, `shared/ui` 는 성격별로.

    frontend/
    ├── .nvmrc                        Node 26. Java 는 Gradle toolchain 이 박아 두는데
    │                                 Node 는 고정하는 곳이 CI 뿐이었다
    ├── package.json                  스크립트: dev / build / test / lint / preview
    ├── package-lock.json             설치된 정확한 버전 고정 — 반드시 커밋
    ├── vite.config.ts                react + tailwindcss 플러그인, '@' → ./src 별칭.
    │                                   vitest 설정(jsdom)도 여기 — 별도 파일로 빼면 '@' 별칭이
    │                                   두 곳으로 갈린다
    ├── tsconfig.json                 references + paths ← shadcn CLI가 읽는 파일 (지우면 안 됨)
    ├── tsconfig.app.json             src/ 코드용 (브라우저). paths 여기에도
    ├── tsconfig.node.json            vite.config.ts용 (Node 환경)
    ├── components.json               shadcn 설정. aliases.ui 가 **`@/shared/ui/base`** 를
    │                                 가리킨다 — 안 바꾸면 다음 `shadcn add` 가 base/ 밖에
    │                                 파일을 만들어 우리 파일과 다시 섞인다
    ├── .oxlintrc.json                린터 설정 (ESLint 아님)
    ├── .env.development              VITE_API_BASE_URL=http://localhost:8080
    ├── .gitignore                    node_modules/, dist/
    ├── index.html                    <div id="root"> + main.tsx 로드
    ├── README.md                     프론트 실행법 (백엔드가 먼저 떠 있어야 함)
    ├── public/favicon.svg            계기판 마크. mark.tsx 와 같은 도형이지만 이쪽은 값이 박혀 있다
    │                                 — 정적 파일이라 테마를 못 따라가므로 다크 바닥(#17171a) 고정
    └── src/
        ├── main.tsx                  Vite 진입점. **index.html이 이 경로를 직접 가리키므로
        │                             폴더로 내려보낼 수 없다** (백엔드의 OdoLogApplication 과
        │                             같은 이유로 남은 예외).
        │                             ThemeProvider > BrowserRouter > AuthProvider > App
        ├── index.css                 디자인 토큰 전부가 여기 한 파일에 있다 (위 "디자인 시스템").
        │                             :root = 라이트, :root.dark = 다크. 값은 여기에만 있다.
        │                             .reveal(스크롤 진입 연출)과 View Transition 규칙도 여기.
        │                             @source not 으로 테스트 파일을 스캔에서 뺀다 — 안 빼면
        │                             테스트가 적은 클래스 이름이 운영 CSS 에 섞인다
        ├── env.d.ts                  import.meta.env 타입 선언
        │
        ├── app/  ──────────────────── 조립층. **여러 기능을 동시에 알아도 되는 유일한 자리**
        │   ├── root/App.tsx          라우트 10개 정의 + Header 배치. 본문 폭 76rem
        │   ├── routing/ProtectedRoute.tsx
        │   │                         로그인 안 했으면 /login으로. loading 중엔 대기
        │   ├── layout/
        │   │   ├── Header.tsx        로고 · 화면 모드 · (로그인 | 닉네임·로그아웃)
        │   │   └── AuthLayout.tsx    로그인·회원가입을 감싸는 2단 레이아웃(lg 이상).
        │   │                         ProtectedRoute 와 같은 "라우트를 감싸는 울타리"라 여기 있다
        │   ├── home/
        │   │   ├── HomePage.tsx      '/' 의 갈림. 비로그인 → LandingPage, 로그인+0대 → 등록 권유,
        │   │   │                     로그인+차량 있음 → 통계(Dashboard).
        │   │   │                     통계·차트·최근 활동이 모두 정비 + 주유를 함께 본다
        │   │   ├── charts/HomeCharts.tsx
        │   │   │                     월별 비용(세로 막대) · 종류별 비용(가로 막대).
        │   │   │                     라이브러리 없이 HTML/CSS 로만 그린다
        │   │   ├── charts/niceMax.ts 축 눈금을 1·2·5 × 10ⁿ 로 올림. 컴포넌트 파일에서
        │   │   │                     내보내면 핫 리로드가 깨져 .ts 로 갈라 둔다
        │   │   └── stats/homeStats.ts
        │   │                         홈 요약 타입 + 조회. GET /api/summary 한 번 — 계산은 서버가 한다
        │   └── landing/LandingPage.tsx
        │                             소개 화면(비로그인 전용). API·상태 없이 shared/ui 조립만
        │                             하는 화면이라 features/ 가 아니라 여기 있다
        │
        ├── features/  ─────────────── 기능별. 백엔드의 user/vehicle/maintenance와 짝을 이룬다
        │   ├── auth/
        │   │   ├── api/
        │   │   │   ├── endpoints/endpoints.ts  fetchMe·signUp·login·logout·updateProfile
        │   │   │   └── types/types.ts          백엔드 user.dto 대응
        │   │   ├── context/
        │   │   │   ├── definition/AuthContext.ts
        │   │   │   │                     Context 정의 + useAuth 훅 (컴포넌트 아닌 것만)
        │   │   │   └── provider/AuthProvider.tsx
        │   │   │                         세션 복구(/me 1회)·login·logout·401 핸들러 등록
        │   │   └── pages/
        │   │       ├── forgot-password/ForgotPasswordPage.tsx
        │   │       │                         재설정 링크 요청. **보냈는지 여부를 말하지 않는다** —
        │   │       │                         "가입된 주소라면 보냈습니다" 하나로 끝낸다
        │   │       ├── reset-password/ResetPasswordPage.tsx
        │   │       │                         ?token= 을 읽어 새 비밀번호를 받는다. 토큰이 없으면
        │   │       │                         폼 대신 안내. **성공해도 자동 로그인시키지 않는다**
        │   │       ├── login/LoginPage.tsx    401 → 폼 에러. 원래 가려던 곳으로 복귀
        │   │       ├── signup/SignUpPage.tsx  가입 후 이어서 로그인까지. 409 → 폼 에러
        │   │       └── profile/ProfilePage.tsx
        │   │                                  Section 5개(계정 / 비밀번호 / 화면 / 내 기록 / 탈퇴).
        │   │                                  바뀐 필드만 PATCH. null 걸러내는 겉 + 폼 2단 구조.
        │   │                                  내보내기는 받아 온 JSON 을 Blob 으로 만들어 내려준다 —
        │   │                                  <a href> 로 바로 받으면 세션·CSRF 헤더가 빠진다
        │   ├── vehicles/
        │   │   ├── components/
        │   │   │   └── info-form/VehicleInfoForm.tsx
        │   │   │                         차량 정보(번호판·제조사·모델·연식) 수정.
        │   │   │                         닫혀 있을 땐 값 4개, 열면 폼(.form-open).
        │   │   │                         바뀐 필드만 PATCH
        │   │   ├── api/
        │   │   │   ├── endpoints/endpoints.ts  차량 엔드포인트 6개
        │   │   │   └── types/types.ts          백엔드 vehicle.dto 대응
        │   │   └── pages/
        │   │       ├── list/VehicleListPage.tsx
        │   │       │                     괘선으로 나눈 행 + 페이지네이션 + 빈 상태.
        │   │       │                     행 왼쪽 1px 표식이 hover·focus-visible 에 세로로 그어진다
        │   │       ├── new/VehicleNewPage.tsx
        │   │       │                     등록 폼. 409(번호판 중복) → 폼 에러
        │   │       └── detail/VehicleDetailPage.tsx
        │   │                             lg에서 2단. 왼쪽=차량정보·주행거리·삭제(sticky),
        │   │                             오른쪽=다음정비·이력
        │   ├── fuel/                     주유 기록·연비. pages/ 가 없다 — maintenance 와 같이
        │   │   │                         자기 라우트 없이 차량 상세에 얹힌다
        │   │   ├── api/{endpoints,types}/  sort 를 보내지 않는다(서버가 고정)
        │   │   └── components/
        │   │       ├── summary/FuelSummaryCard.tsx   평균 연비 히어로 + 통계 4칸
        │   │       ├── section/FuelSection.tsx       목록 + 페이지네이션 + 삭제 + 폼 토글
        │   │       └── form/FuelForm.tsx             등록·수정 겸용. 입력 중 리터당 단가 표시
        │   └── maintenance/
        │       ├── api/
        │       │   ├── endpoints/endpoints.ts  정비 이력 엔드포인트 5개
        │       │   └── types/types.ts          ServiceType 유니온 + SERVICE_TYPE_LABELS + DTO
        │       └── components/           pages/ 가 없다 — 자기 라우트 없이 차량 상세에 얹힌다
        │           ├── next-service/NextServiceCard.tsx
        │           │                     이력 있는 종류의 다음 정비 시점(요청 1번) + 차량별 주기 폼.
        │           │                     재조회는 부모가 key 를 바꿔 재생성
        │           ├── section/MaintenanceSection.tsx
        │           │                     목록 + 페이지네이션 + 삭제 + 폼 토글
        │           └── form/MaintenanceForm.tsx
        │                                 등록·수정 겸용 (record가 null이면 등록)
        │
        └── shared/  ───────────────── 어느 기능에도 속하지 않는 것. 백엔드의 common과 같은 자리
            ├── api/
            │   ├── client/client.test.ts BASE_URL 대비책과 네트워크 실패 변환을 고정한다
            │   ├── client/client.ts      fetch 래퍼. credentials:'include' / ApiError /
            │   │                         204 처리 / 401 전역 핸들러 등록 창구
            │   └── types/types.ts        PageResponse<T> / ErrorResponse 둘뿐.
            │                             기능별 DTO는 features/*/api/types/ 로 옮겼다
            ├── theme/                    라이트/다크. AuthContext와 똑같이 3파일로 나뉜다
            │   ├── context/ThemeContext.ts   Theme 타입 + localStorage 키 + useTheme 훅
            │   ├── provider/ThemeProvider.tsx 저장·복원, OS 설정 추적, View Transition 전환
            │   └── toggle/ThemeToggle.tsx    해/모니터/달 3칸 세그먼트 컨트롤 (헤더에 배치)
            ├── lib/
            │   ├── limits/limits.ts      주행거리·금액 상한 + 비밀번호 바이트 계산.
            │   │                         maxLength 는 글자 수만 세서 한글 24자(=72바이트)를
            │   │                         못 막는다 — 저장 전에 알려 주려면 직접 세야 한다.
            │   │                         백엔드 InputLimits 와 같은 숫자다 —
            │   │                         브라우저가 먼저 막아 주면 저장을 누르기 전에 알고,
            │   │                         서버는 화면을 안 거치는 요청까지 막는다.
            │   │                         한쪽만 고치면 "화면은 되는데 저장이 안 되는" 상태가 된다
            │   ├── format/format.ts      formatNumber / formatKm / formatWon / formatDate /
            │   │                         formatCompact / formatMonth / todayString(UTC 함정 회피)
            │   ├── format/format.test.ts  todayString 을 자정 직후·직전 두 시각으로 본다 —
            │   │                         어느 표준시대에서 돌려도 결과가 같아야 한다
            │   └── hooks/
            │       ├── useAsyncData.ts   조회 4곳의 공통 훅. data/loading/error +
            │       │                     reload()/setData. cancelled 플래그가 여기 한 곳에만
            │       └── useCountUp.ts     직전 값에서 새 값으로 굴러가는 숫자.
            │                             연출 도중 값이 또 바뀌면 **화면에 보이던 값**에서
            │                             이어간다 — 옛 목표에서 다시 시작하면 숫자가 한 번 튄다.
            │                             **첫 렌더에서는 안 움직인다** — 값이 실제로 바뀐
            │                             순간에만. 지속 시간은 변화 폭에 비례(0.45~1.4s)
            │       └── useCountUp.test.ts  위 두 줄을 고정한다. matchMedia 는 jsdom 에 없어
            │                             직접 심고, rAF 는 가짜 타이머로 돌린다
            └── ui/                       **base/ 만 shadcn 이 건드리는 자리이고 나머지는 우리 것.**
                │                         전에는 한 폴더(12개)에 섞여 있어서 문서로만 구분했다
                ├── base/                 shadcn CLI 가 복사해 넣는 자리 (components.json 이 여길 가리킨다)
                │   ├── button.tsx        asChild 없음. Base UI의 render prop 사용
                │   ├── card.tsx
                │   ├── input.tsx
                │   ├── label.tsx
                │   └── textarea.tsx
                ├── form/
                │   ├── date-input.tsx    날짜 입력. 터치 기기면 드럼 휠(년/월/일), 아니면
                │   │                     네이티브 date 입력. scroll-snap 이 드래그를 대신한다.
                │   │                     **칸 목록도 오늘에서 끊는다**(2026-09-23) — 년만 막고
                │   │                     월·일을 열어 두면 올해 남은 달이 그대로 선택되고
                │   │                     저장할 때야 400 이 난다. 데스크톱은 max 로 막는 자리라
                │   │                     안 맞추면 기기마다 되는 날짜가 달라진다
                │   ├── date-parts.ts     날짜 문자열 ↔ 년·월·일. 일수 보정(1/31 → 2/28)은
                │                         윤년을 직접 계산하지 않고 Date 에 맡긴다.
                │                         lastSelectableMonth/Day 가 "오늘 이후 금지" 를 쥐고 있고
                │                         join 이 년→월→일 순서로 자른다 — 굴린 칸의 뜻을
                │                         최대한 살리려고 순서가 있다
                │   ├── field.tsx         라벨+입력+도움말 한 벌. htmlFor 필수(접근성)
                │   └── control.ts        입력 요소 공통 클래스 문자열.
                │                         input·textarea·네이티브 select 셋이 공유한다.
                │                         .tsx 가 아닌 이유는 AuthContext 와 같다 —
                │                         컴포넌트와 값을 한 파일에서 내보내면 핫 리로드가 깨진다
                ├── layout/
                │   ├── page.tsx          Page — 앱 화면 한 장의 껍데기(뒤로가기·머리말·간격).
                │   │                     FormActions — 폼 맨 아래 버튼 줄.
                │   │                     **모든 앱 화면이 이 둘을 쓴다** (랜딩만 예외)
                │   └── section.tsx       설정 화면용 2단(왼쪽 설명 / 오른쪽 내용).
                │                         넓은 화면의 남는 폭을 여백이 아니라 정보로 채운다
                ├── feedback/state.tsx    LoadingText / ErrorText / NoticeText / Skeleton
                ├── nav/pagination.tsx    목록 2곳이 복사해 쓰던 페이지 이동 UI
                ├── brand/mark.tsx        계기판 로고 SVG. 헤더·로그인·빈 상태 3곳이 공유
                └── cn-usage.test.ts      한 기능에 속하지 않는 가드라 여기 있다. 아래 참고

### 프론트엔드 — 테스트

**테스트는 대상 파일 옆에 둔다**(`format.ts` 옆에 `format.test.ts`). 백엔드가 테스트 경로를
대상과 맞추는 것과 같다. `npm run test` 로 돌리고 **총 45개**다.

    cn-usage.test.ts        cn() 과 cva() 인자에 타입 스케일 토큰이 없는지 소스를 훑는다.
                            **이 가드가 없던 8일 동안 CardTitle 이 17px·600 을 잃고
                            16px·400 으로 렌더됐다** — tsc·oxlint·vite build 가 전부 통과했고
                            빌드 CSS 에도 규칙이 멀쩡히 있었다. 클래스가 DOM 에 닿지 못하는
                            문제라 그 어느 것으로도 안 잡힌다.
                            소스는 import.meta.glob 의 ?raw 로 읽는다 — node:fs 를 쓰면
                            tsconfig 의 types 에 node 를 더해야 하고 앱 코드까지 영향을 받는다
    format.test.ts          todayString 을 자정 직후·직전 두 시각으로 본다. 앞의 것은
                            동쪽(UTC+), 뒤의 것은 서쪽(UTC-) 에서 깨지므로 둘이 짝이다
    niceMax.test.ts         873 → 1,000 / 45,000 → 50,000 / 120,000 → 200,000.
                            0 과 음수도 본다 — max 가 0 이면 막대 높이가 NaN% 가 된다
    date-parts.test.ts      일수 보정과 윤년(2026·2024·2100·2000). 빈 값·깨진 값이 오늘로
                            되돌아가는지 — 네이티브 date 입력은 지우면 빈 문자열을 준다
    useCountUp.test.ts      **첫 렌더에서 안 움직이는 것**과 변화 폭에 비례하는 지속 시간.
                            jsdom 에 matchMedia 가 없어 직접 심는다

**빌드에 섞이지 않게 두 가지를 해 뒀다.** `index.css` 의 `@source not` 으로 Tailwind 스캔에서
빼고(안 빼면 테스트가 적은 클래스가 운영 CSS 에 생긴다 — 실제로 `text-red-500` 이 들어갔다),
`vitest` 설정은 `vite.config.ts` 안에 둔다(별도 파일이면 `@` 별칭이 두 곳으로 갈린다).

**폴더는 파일의 성격을 드러낼 때 만든다 — 파일 개수로 정하지 않는다.**
전에는 "폴더는 파일이 2개가 될 때 만든다"였고, 그 기준으로 2026-09-09에 "세분화는 끝났다"고
결론 냈었다. 2026-09-13에 **사용자 요청으로 그 기준을 바꿨다.** 바뀐 기준에서는
`dto/request/LoginRequest.java` 보다 `dto/request/login/LoginRequest.java` 가 낫다 —
폴더 이름이 "이 DTO는 로그인 유스케이스의 것"이라고 말해 주고, 형제 폴더 목록이 곧
그 기능의 유스케이스 목록이 된다.

**치르는 값은 정직하게 적어 둔다.** 파일을 보유한 폴더가 45개 → 81개, 그중 파일 1개짜리가
24개 → 74개가 됐다(그 뒤로 fuel·summary 가 붙어 **2026-09-18 기준 97개 / 89개**).
경로가 길어지고, 새 파일을 놓을 자리를 매번 판단해야 한다.
`exception/type/` 처럼 4개가 모인 곳이나 `ui/base/` 처럼 **소유자가 다른 파일을 갈라놓는**
자리는 값을 치를 만하고, `service/application/` 처럼 형제가 생길 기약이 없는 곳은 순수 비용이다.

### 의존 방향

    백엔드:  {account, summary} → {fuel, maintenance, vehicle, user},  전부 common 을 쓴다
             fuel → vehicle → user
             maintenance → vehicle → user
    프론트:  app → features → shared

**`account` 와 `summary` 가 백엔드의 조율 층이다** (2026-09-16 / 09-17 신설).
`summary` 는 홈 화면 요약을 위해 세 기능을 **읽어서 합치고**, `account` 는 회원 탈퇴에서
**순서를 조율한다**. 그래서 `summary` 는 리포지토리를, `account` 는 서비스를 주입받는다 —
집계에는 각 기능의 비즈니스 규칙이 필요 없고, 삭제에는 필요하기 때문이다. 프론트의 `app/` 과 정확히 같은 성격 —
**여러 기능을 동시에 알아도 되는 유일한 자리**다. 회원 탈퇴가 user·vehicle·maintenance 를 모두
건드리는데, 이걸 `UserService` 에 넣으면 `user → vehicle` 역방향 의존이 생긴다. `common` 도 안 된다
— 세 기능이 전부 `common` 을 의존하므로 `common` 이 `vehicle` 을 알면 진짜 순환이 된다.
`account` 는 **순서만 정하고 실제 삭제는 각 기능에 맡긴다.** 여기서 리포지토리를 직접 부르면
조율 층이 남의 테이블 구조를 알게 된다.

`shared/theme` 는 `shared/ui` 와 같은 층이다. `ThemeToggle` 이 `Button` 대신 평범한 `<button>` 을
쓰는 이유가 이것 — 같은 층끼리 얽히는 것보다 20줄짜리 버튼을 직접 쓰는 편이 싸다.

`app` 은 **여러 기능을 동시에 알아도 되는 유일한 층**이다. `Header`(내부에서 `useAuth` 사용)와
`ProtectedRoute` 가 이 층에 있는 이유다. 전에는 `shared/layout/Header.tsx` 가
`features/auth` 를 import 해서 "shared가 features를 아는" 역방향 의존이 있었는데 이것으로 없앴다.
같은 이유로 모든 기능의 DTO를 담고 있던 `shared/api/types.ts` 도 기능별로 나눴다.

기능 간 참조는 현재 **`vehicles → maintenance` 한 방향뿐**이다 (차량 상세 화면이
`MaintenanceSection`·`NextServiceCard` 를 얹는다). 이 문서에 한동안 반대로(`maintenance →
vehicles`) 적혀 있었으나, 실제 import 를 세어 바로잡았다.

반대 방향 의존(`user`가 `vehicle`을 알거나, `shared`가 `features`를 아는 것)이 생기면
설계가 잘못된 신호로 보고 재검토한다.

**백엔드에는 알려진 예외가 하나 있다.** 패키지 수준으로 보면 `vehicle` 과 `maintenance` 는
서로를 안다:

    vehicle/service/application/VehicleService
        → maintenance/repository/jpa/MaintenanceRecordRepository
    maintenance/service/application/MaintenanceRecordService
        → vehicle/service/application/VehicleService

차량 삭제 시 "이력 먼저, 차량 나중" 순서를 서비스가 직접 제어하려고 `VehicleService` 가
`MaintenanceRecordRepository` 를 주입받기 때문이다. 서비스끼리 주입하면 스프링이 잡아내는
진짜 순환 참조가 되므로 리포지토리를 골랐고, 그래서 **클래스 수준에서는 순환이 아니다.**
위의 한 줄 요약(`maintenance → vehicle`)이 이 사실을 가리고 있어 여기 적어 둔다.

### 세분화가 멈추는 두 지점

깊이를 더 내려갈 수 없는 자리가 둘 있다. 둘 다 **바깥에서 경로를 이름으로 붙잡고 있기** 때문이다.

- `com/odolog/app/OdoLogApplication.java` — `@SpringBootApplication` 의 컴포넌트 스캔 기점이다.
  `bootstrap/` 으로 내리면 `scanBasePackages` / `@EntityScan` / `@EnableJpaRepositories` 를
  손으로 지정해야 하고, 그 순간 "어디까지 스캔되는가"가 코드에서 안 보이게 된다.
- `frontend/src/main.tsx` — `index.html` 의 `<script type="module" src="/src/main.tsx">` 가
  이 경로를 문자열로 가리킨다. 옮기면 HTML 도 같이 고쳐야 하는데,
  `'odolog-theme'` 문자열이 HTML 과 `ThemeContext.ts` 양쪽에 중복인 것과 같은 종류의 함정이다.

## 진행 상황

완료한 작업과 그 근거는 **`HISTORY.md`** 에 있다. 2026-09-20 에 갈라냈다 — 이 파일이 3,200줄이
되면서 그중 절반이 완료 기록이었고, 지금 지켜야 할 규칙과 이미 끝난 일이 한 파일에 섞여 있으면
규칙 쪽이 안 보인다. 완료 기록은 계속 늘기만 하는 반면 규칙은 그대로다.

새 작업을 마치면 `HISTORY.md` 맨 위에 항목을 더하고, 아래 체크리스트에서 그 줄을 지운다.

**지금 열려 있는 것은 Phase 6 의 눈 확인 하나뿐이다.**
## 완성까지의 로드맵

**"완성"의 정의**: 회원/차량/정비 이력을 관리하는 백엔드 API + 그걸 실제로 쓸 수 있는
프론트엔드 웹앱까지. 배포(서버 인프라, 도메인, CI/CD)는 범위 밖 — **로컬에서 완전히 동작하는 것**까지가 목표다.

**플랫폼은 웹 하나다** (2026-09-16 확정). iOS/Android 네이티브 앱은 만들지 않고, 세 개를
병행하지도 않는다. 근거와 "다시 꺼낼 조건"은 `HISTORY.md` 의 2026-09-16 항목에 적어 뒀다.
한 줄 요약: **브라우저가 못 하는 일을 이 앱이 아직 하나도 안 한다.**

- **Phase 1 — 백엔드 마무리** (완료)
  프론트가 호출할 API 표면 완성. 단건 조회, 날짜 기준 다음 정비, 페이지네이션, CORS, API 문서화.
- **Phase 2 — 프론트엔드 프로젝트 셋업** (완료)
  `frontend/`에 Vite+React+TypeScript, Tailwind/shadcn, API 클라이언트, 세션 쿠키 연동 확인.
- **Phase 3 — 인증 화면** (완료)
  회원가입/로그인/로그아웃, 로그인 상태 전역 관리, 보호 라우트.
- **Phase 4 — 차량 관리 화면** (완료)
  차량 목록/등록/상세/주행거리 갱신/삭제.
- **Phase 5 — 정비 이력 관리 화면** (완료)
  이력 목록/등록/수정/삭제, 다음 정비 시점(주행거리+날짜) 표시.
- **Phase 6 — 다듬기** (코드는 사실상 끝. **눈 확인만 남았다**)
  로딩/에러/빈 상태·반응형·포맷팅은 재설계 때 함께 끝났다. 남은 건 브라우저에서 실제로 보는 일.

아래 체크리스트는 **지금 시점의 계획**이다. 프론트엔드는 아직 한 줄도 안 짜본 영역이라
막상 시작하면 순서나 범위가 바뀔 수 있다. 각 항목은 착수할 때 다시 쪼갠다.
체크리스트 안의 순서는 **의존 관계가 있는 것만** 순서를 뜻하고, 나머지는 우선순위가 아니다.

---

## Phase 1 (잔여) — 백엔드 마무리

Phase 1은 **완료**. 아래는 조건이 갖춰지면 재검토할 보류 항목뿐이다.

### 1-B. 보류 (조건이 갖춰지면 재검토)

- [x] `@EnableJpaAuditing` 도입 — **2026-09-16 완료.** 걸어 뒀던 조건("엔티티가 4개째 생기는
      시점")이 `FuelRecord` 로 충족됐다. 자세한 내용은 `HISTORY.md` 의 2026-09-16 항목에.

여기 남은 보류 항목은 없다.

---

## Phase 2 — 프론트엔드 프로젝트 셋업 (완료)

셋업 자체는 끝났고 CORS·세션 쿠키도 커맨드라인으로 검증했다. 남은 것은 사용자의 눈 확인 하나뿐:

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. `JSESSIONID` 확인은 B-12 에 있다.

---

## Phase 3 — 인증 화면 (완료)

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. 해당 항목은 B-10 ~ B-12, B-105 ~ B-106.

---

## Phase 4 — 차량 관리 화면 (완료)

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. 해당 항목은 B-13 ~ B-33, B-109.

---

## Phase 5 — 정비 이력 관리 화면 (완료)

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. 해당 항목은 B-34 ~ B-48.

---

## Phase 6 — 다듬기

코드는 사실상 끝났다. **남은 것은 눈으로 보는 일 하나다.**

**화면을 크게 고치거나 새로 만든 작업이 열 번 넘게 연속으로 쌓였다.** 재설계(9/11) →
모서리 각지게 → 홈 통계·차트 → 편집 레이아웃 → 모션 재조정 → 모바일 최적화 →
주유 기록·연비 → 정비 종류 15개 → 날짜 드럼 휠 → 홈 요약 API → 연비 초기화 →
빠진 기록 감지(9/17). **전부 `tsc -b` / `oxlint` / `vite build` 통과만으로 끝냈고,
실제 화면은 아직 아무도 본 적이 없다.** 여기에 코드를 더 쌓으면 깨진 게 나왔을 때
어느 작업에서 깨졌는지 못 찾는다.

**체크리스트는 그 사이에 두 번 낡았다.** 기능이 붙을 때 항목을 같이 안 적으면, 한 바퀴를
다 돌고도 새 기능은 한 번도 안 본 채로 끝난다. 2026-09-17 에 훑어 10건을 고치고 14건을 더했다
(연비 초기화 · 이상 연비 · 빠진 기록 안내 · 과거 기록 입력 · 최근 활동 · 차량별 평균 연비).
**앞으로 화면에 무언가를 더할 때는 6-B 에 줄을 같이 넣는다.**

**Phase 2~5 에 흩어져 있던 "브라우저에서 확인" 네 줄을 여기로 합쳤다.** 같은 말이 네 군데
있으면 어디까지 봤는지 알 수가 없다.

### 볼 화면은 10개가 아니라 13개다

라우트는 10개지만(`/` `/login` `/signup` `/forgot-password` `/reset-password` `/vehicles`
`/vehicles/new` `/vehicles/:vehicleId` `/me` `*`), **`/` 가 세 얼굴을 갖는다** —
비로그인 랜딩 / 로그인+0대 등록 권유 / 로그인+차량 있음 통계.
`/forgot-password` 도 **보내기 전과 보낸 뒤 둘**이고, `/reset-password` 는 **토큰이 있을 때와
없을 때 둘**이다. `*` 는 화면이 아니라 `/` 로 보내는 리다이렉트다.
그래서 눈으로 볼 상태는 **13개**이고, 여기에 각 화면의 로딩·빈 상태·에러가 더 붙는다.

※ 프로필 경로는 `/profile` 이 아니라 **`/me`** 다.

### 규칙: 발견한 건 적어만 두고 그 자리에서 고치지 않는다

한 바퀴 다 돌고 나서 모아서 고친다. 중간에 고치면 그 수정 때문에 뒷부분이 또 달라져서,
지금 보고 있는 게 원래 그런 건지 방금 내가 만든 건지 구분이 안 된다.
발견한 것은 **6-F 표에 줄을 추가**한다.

---

### 6-A. 준비

- [ ] **A-1** IntelliJ 에서 `OdoLogApplication` 실행 → 로그에 `Started OdoLogApplication` 확인
      → 운영 계정 `odolog` 비밀번호가 IntelliJ 실행 구성 환경변수에만 있다. 터미널로 띄우지 말 것
      → 접속이 실패하면 계정 문제부터 의심:
        `/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW TABLES;"`
- [ ] **A-2** `cd frontend && npm run dev` → `http://localhost:5173`
- [ ] **A-3** DevTools 를 열고 **Console 탭을 계속 띄워 둔다**. 이후 모든 단계에서 "빨간 줄이
      새로 생기지 않는다"가 판정 기준의 일부다
- [ ] **A-4** Network 탭 > `Disable cache` 체크 — 옛 CSS·파비콘이 캐시로 남아 헛다리 짚는 걸 막는다
- [ ] **A-5** 1회차 기준 상태를 맞춘다: **OS 라이트 모드 / 브라우저 폭 1440px 근처 / 확대 100%**
- [ ] **A-6** 시작 데이터를 정한다. **2026-09-17 기준 운영 스키마는 비어 있지 않다** —
      `users 1 · vehicles 2 · maintenance_records 3 · fuel_records 4`.
      1회차는 **회원가입부터 데이터가 쌓이는 순서**로 짜여 있고, 특히 B-107(남의 차량이 안 보임) ·
      B-109(차량 삭제 시 동반 삭제를 `COUNT(*)` 로 확인) · B-114(탈퇴 후 0건)은 **빈 상태여야
      판정이 선명하다.** 남은 행 위에서 하면 "원래 있던 행인지 방금 만든 행인지"를 매번 따져야 한다.
      → 비우고 시작한다면 **자식 테이블 먼저**다(FK 제약):

          /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; \
            DELETE FROM fuel_records; DELETE FROM maintenance_records; \
            DELETE FROM vehicles; DELETE FROM users;"

      → 남겨 두고 한다면 위 세 항목만 새 계정을 따로 파서 본다

---

### 6-B. 1회차 — 기능 한 바퀴 (라이트 / 1440px)

데이터가 쌓이는 순서로 배열했다. 위에서부터 그대로 따라가면 완료 판정 기준 10개가 전부 덮인다.

#### 6-B-1. 비로그인 화면

- [ ] **B-01** `/` — 랜딩이 뜬다. 히어로 문장 + `시작하기`/`로그인` 버튼.
      **`Page` 를 안 쓰는 유일한 화면**이라 가운데 정렬 + 큰 세로 리듬(`gap-28`)이어야 한다
- [ ] **B-02** 스크롤 — 기능 3칸·미리보기가 `.reveal` 로 따라 들어온다.
      스크롤을 **되감으면 연출도 되감기는지**(`animation-timeline: view()` 의 특징)
- [ ] **B-03** 기능 3칸의 칸 사이 선이 **1px 로 보이는지**(2px 로 보이면 `gap-px` 격자가 깨진 것)
- [ ] **B-04** 헤더 로고 클릭 → `/` 유지. 로그인 여부와 무관하게 **항상 `/`**
- [ ] **B-05** `/login` 이동 → **헤더의 `로그인` 버튼이 사라진다**(지금 있는 곳을 가리키는 버튼)
- [ ] **B-06** `AuthLayout` — 왼쪽 문장 + "오도로그가 하는 일 →", **가운데 세로 괘선**,
      오른쪽 폼이 Card 안. 선 기준 좌우 여백이 **같아 보이는지**(둘 다 64px 로 맞춰 뒀다)
- [ ] **B-07** eyebrow 가 **비어 있는지** — 로그인 전에는 아직 아무 데도 속하지 않는다
- [ ] **B-07-1** ⚠️ **DevTools > Application > Cookies 에 `XSRF-TOKEN` 이 있는지.**
      없으면 이후 모든 저장·삭제가 403 이 된다. `HttpOnly` 가 **체크 안 되어** 있어야 한다 —
      화면이 읽어 헤더에 실어야 하는 값이다(`JSESSIONID` 는 반대로 체크돼 있어야 한다)
- [ ] **B-08** 없는 계정으로 로그인 → 401 → **폼 안 인라인 에러**. 0.24s / 4px 로 **짧고 가깝게**
      나타나는지(다른 연출처럼 길게 감속하면 급한 소식으로 안 읽힌다)
      → ⚠️ **백엔드를 끄고 로그인해 본다.** `서버에 연결하지 못했습니다…` 가 떠야 한다.
        `로그인에 실패했습니다` 가 뜨면 옛 코드다 — 그러면 서버가 죽은 줄 모르고
        비밀번호만 계속 다시 치게 된다

#### 6-B-2. 가입과 세션

- [ ] **B-08-0** ⚠️ **백엔드를 끈 채 `/login` 을 연 뒤 백엔드를 켜고 로그인** (2026-09-25).
      이 순서면 XSRF 쿠키를 아직 못 받은 상태라 첫 POST 가 CSRF 403 이다. **폼에
      `요청을 확인할 수 없습니다…` 가 떠야 한다** — `서버에 연결하지 못했습니다` 가 뜨면
      403 에 CORS 헤더가 빠진 옛 상태다(CORS 필터가 CSRF 필터 뒤에 있었다)
- [ ] **B-08-1** 로그인 화면 아래 `비밀번호를 잊으셨나요?` → `/forgot-password` 로 가는지.
      머리말이 다른 화면과 같은 자리·크기인지(`Page` 를 쓰는지), eyebrow 가 **비어 있는지**
- [ ] **B-08-2** ⚠️ **없는 주소로 재설정 요청** → 에러가 아니라 **"가입된 주소라면 보냈습니다"**.
      가입된 주소로 요청했을 때와 **화면이 똑같아야 한다** — 다르면 그게 가입 여부 조회가 된다
- [ ] **B-08-3** 서버 로그에 재설정 링크가 찍히는지(메일 설정을 안 했다면 `발송 실패` ERROR).
      `MAIL_USERNAME`/`MAIL_PASSWORD` 를 넣었다면 **실제 메일함**에 링크가 오는지
- [ ] **B-08-4** 링크를 눌러 `/reset-password?token=…` → 새 비밀번호 두 칸.
      **서로 다르게 넣으면 폼 안에서 막히는지**(서버로 안 보낸다)
- [ ] **B-08-5** ⚠️ 변경 성공 → **자동 로그인되지 않고 `/login` 으로 가는지**.
      메일 링크를 누른 사람이 곧 계정 주인이라고 믿지 않는다는 뜻이다.
      **새 비밀번호로 로그인되고 옛 비밀번호로는 안 되는지**
      → 로그인 화면에 **`비밀번호를 바꿨습니다. 새 비밀번호로 로그인해 주세요.`** 가 뜨는지(2026-09-26).
        **새로고침하면 사라지는지** — 주소(쿼리)가 아니라 라우터 state 로 넘겨서 이번 이동에만 붙는다.
        로그인에 한 번 실패하면 안내 대신 실패 문구만 보이는지
- [ ] **B-08-6** **같은 링크를 한 번 더** 누르고 저장 → `링크가 만료되었거나 이미 사용되었습니다`
- [ ] **B-08-6-1** ⚠️ **다른 브라우저(또는 시크릿 창)에 같은 계정으로 로그인해 둔 채 재설정** →
      그쪽에서 새로고침하면 **로그아웃돼 있는지**. 재설정하는 이유가 "누가 들어와 있는 것
      같아서" 일 수 있는데, 세션이 14일이라 안 끊으면 그 사람이 그대로 남는다
- [ ] **B-08-7** `token=` 없이 `/reset-password` 를 직접 열면 폼 대신 안내 + `다시 받기` 버튼인지
- [ ] **B-09** `/signup` → 비밀번호 7자로 제출 → 400. 가입 폼도 인라인 에러인지
      → 도움말이 `8자 이상 · 한글은 24자까지` 인지
      → ⚠️ **한글 25자 비밀번호로 가입** → 500 이 아니라 **400 + 72바이트 안내**인지.
        BCrypt 는 72바이트가 상한인데 `@Size` 는 글자 수라 한글에서 3배로 벌어진다.
        한글 24자(=72바이트)는 **통과해야** 한다
- [ ] **B-10** 정상 가입 → **이어서 로그인까지 자동으로** 되는지(가입 API 는 세션을 안 만든다)
      → 가입 뒤 로그인만 실패하면(예: 백엔드를 그 사이 끔) 오류가 아니라 **로그인 화면 +
        `가입했습니다. 로그인해 주세요.`** 인지(2026-09-27). 다시 눌러 409 를 받지 않아야 한다
- [ ] **B-11** 헤더가 `닉네임 + 로그아웃` 으로 바뀌는지. 닉네임은 `max-w-24 truncate`
- [ ] **B-12** **새로고침** → 로그인이 유지되는지.
      → Network 탭에 `GET /api/users/me` 가 **1건만** 뜨는지
      → Application > Cookies 에 **`JSESSIONID`** 가 있는지
      → 이 응답 전까지 `loading` 이라 **로그인 화면이 깜빡이면 안 된다**

#### 6-B-3. 차량

- [ ] **B-13** `/` → 차량 0대 → **등록 권유 화면**(랜딩도 통계도 아닌 세 번째 얼굴)
- [ ] **B-14** `/vehicles` → 빈 상태. 로고 + "첫 차량 등록하기" 버튼
- [ ] **B-15** `/vehicles/new` → `Page` 머리말: eyebrow `GARAGE`, **뒤로가기 있음**(목록에서 파고든 화면)
- [ ] **B-16** 머리말 아래 **1px 괘선이 0.7s 동안 그어지는지**. 화면당 하나뿐인 '자리 잡기' 연출
- [ ] **B-17** 연식에 `1899` → 막히는지(프론트 `min` + 백엔드 `@Min(1900)` 이중)
- [ ] **B-18** 차량 등록 성공 → 목록으로. 제출 중 버튼이 잠기는지
- [ ] **B-19** **같은 번호판**으로 또 등록 → 409 → 폼 에러에 번호판이 그대로 보이는지
      (소유자별 유니크라 남의 차량을 노출하지 않으므로 안전하다)
- [ ] **B-20** 두 번째 차량 등록(다음 항목의 비교용)
- [ ] **B-21** 목록 행에 마우스 → **왼쪽 1px 표식이 세로로 그어지는지**.
      가로로 늘어나면 안 된다(글자를 밀어내는 것처럼 보인다). 배경도 `wash` 로 옅게
- [ ] **B-22** 행이 **카드가 아니라 괘선으로 나뉘어** 있고, 번호판은 번호판끼리 주행거리는
      주행거리끼리 **세로로 정렬**되는지. 여러 대를 훑어 비교할 수 있는 게 목적이다
- [ ] **B-23** 상세 진입 → eyebrow 가 **번호판**, 뒤로가기 있음. 머리말이 사이드바 밖 전체 폭인지
      → 주소창이 `/vehicles/1` 이 아니라 **`/vehicles/k3Xq9mTa2LpZ` 같은 12자**인지(2026-09-26).
        홈의 `차량별`·`최근 활동` 링크로 들어가도 같은 주소인지
      → ⚠️ 운영 DB 는 **앱을 띄우기 전에** README 의 "공개 id" SQL(차량·정비·주유 세 테이블)을
        먼저 실행해야 한다. 안 하면 기존 링크가 전부 `/vehicles/` 로 깨지고 기록 수정·삭제가 404 가 된다
      → Network 탭에서 정비·주유 **수정·삭제 요청 주소 끝도 12자**인지(`…/fuel-records/aB3…`)
      → 예전 주소 `/vehicles/1` 을 직접 치면 **`존재하지 않는 차량입니다`** 인지
- [ ] **B-24** 주행거리 히어로 숫자가 **화면을 열 때 굴러오르지 않는지**(중요 — 0 에서 굴러오는
      건 대시보드 템플릿의 상투구라 일부러 걷어냈다). `tabular-nums` 도 안 붙어 있어야 한다
- [ ] **B-25** 주행거리를 **크게** 갱신 → 직전 값에서 새 값으로 굴러가는지. 변화 폭이 클수록
      오래(0.45~1.4s) 걸리는지
- [ ] **B-26** **더 작은 값**으로 갱신 → **409 가 아니라 확인 창**이 뜨는지
      ("계기판을 교체했거나 잘못 입력한 값을 고치는 경우에만 진행하세요"). **취소하면 아무 일도
      일어나지 않는지** — 요청이 나가지 않아야 한다(Network 탭)
- [ ] **B-27** 같은 자리에서 **확인을 누르면 실제로 낮아지는지**. ⚠️ 이게 되돌릴 수 있는 유일한
      길이다 — 기록을 고쳐도 차량 값은 따라 내려오지 않으므로, 여기가 막히면 자리수를 한 번
      잘못 넣은 차량은 영영 그 값으로 남는다
- [ ] **B-28** '차량 정보' 카드가 닫혀 있을 때 **값 4개**(번호판·제조사·모델·연식)가 보이는지.
      긴 모델명이 라벨을 밀어내지 않는지(`min-w-0 truncate`)
- [ ] **B-29** `수정` → 0.36s 펼쳐지는지. 칸이 열린 뒤 글자가 0.12s 늦게 들어오는지
- [ ] **B-30** ⚠️ **제조사만 바꾸고 저장 → 409 가 나지 않는지.** 이게 이 기능의 핵심 함정이다 —
      번호판을 그대로 둔 채 다른 필드만 고칠 때 자기 자신이 중복으로 잡히면 안 된다
- [ ] **B-31** 번호판을 **두 번째 차량의 번호로** 바꾸기 → 409 가 폼 안에 뜨는지
- [ ] **B-32** 저장 성공 → **머리말이 같이 바뀌는지**(eyebrow=번호판, 제목=제조사+모델,
      설명=연식). 같은 객체를 보므로 따로 새로고침할 필요가 없어야 한다
- [ ] **B-33** 아무것도 안 바꾸고 `저장` → **Network 탭에 요청이 안 나가는지**(빈 PATCH 방지)

#### 6-B-4. 정비 이력

- [ ] **B-34** 다음 정비 카드 — 이력이 없으므로 **"아직 계산할 이력이 없습니다…" 한 문장**만
      보이는지. ⚠️ **종류 15개가 "기록 없음"으로 줄줄이 늘어서면 안 된다** — 이력이 없는 종류는
      서버가 응답에서 빼므로(2026-09-16), 빈칸 목록이 나오면 옛 화면이 남은 것이다.
      요청은 **1번**인지도 Network 탭에서 확인(종류마다 1번씩 보내던 것을 걷어냈다)
- [ ] **B-35** 정비 폼 열기 → **자리를 밀어내며 0.36s 펼쳐지고**, 안쪽 글자는 0.12s 늦게 들어오는지
      (동시에 나타나면 글자가 찌그러지며 늘어나 보인다)
- [ ] **B-36** 폼 닫기 → **연출 없이 즉시** 닫히는지(이미 사용자가 결정한 일이다)
- [ ] **B-37** 날짜 입력칸의 기본값이 **오늘**인지. UTC 가 아니라 로컬 기준(`todayString()`)이라
      **오전 9시 이전에도 어제가 아닌지**
      → ⚠️ 이 칸은 **기기에 따라 컨트롤이 갈린다.** 마우스·키보드면 네이티브 date 입력,
        터치면 드럼 휠이다(`DateInput` 이 `pointer: coarse` 로 판단). 여기 1회차는 데스크톱
        기준이라 네이티브 쪽을 본다. 휠은 **6-C 의 C-16~C-21** 에서 따로 확인한다
- [ ] **B-38** 정비 폼에서 주행거리 칸을 **지우면** **"주행거리를 적지 않으면 다음 정비 시점을
      계산할 수 없습니다."** 가 뜨는지. 주유 폼(B-52)과 같은 방식이다
- [ ] **B-39** 날짜 칸에 **내일 이후를 고를 수 있는지** — 달력에서 회색으로 막혀 있어야 한다
      (`max`). 주소창이나 개발자 도구로 억지로 미래 날짜를 보내면 **400** 인지.
      막지 않으면 그 기록이 목록 맨 위에 고정되고 **다음 정비 시점까지 그 값으로 계산된다**
- [ ] **B-40** 엔진오일 이력 등록 → 목록에 표시
- [ ] **B-41** 다음 정비 카드가 갱신되는지. **주행거리 기준과 날짜 기준 둘 다** 나오는지
- [ ] **B-41-1** ⚠️ **지난 정비가 구분되는지**(2026-09-25 신설). 오래된 날짜로 엔진오일 이력을
      하나 넣어 본다 → 그 행에 `지남` 라벨이 붙고 **목록 맨 위로 올라오는지**.
      빨강이 아니라 **테두리 + 진한 글자**인지 — 지난 정비는 실패가 아니라 할 일이다
      → **주행거리로만 지난 경우**와 **날짜로만 지난 경우**(차를 안 탔는데 개월이 지남)
        둘 다 `지남` 인지. 권장 주기는 "먼저 오는 것"이라 하나면 충분하다
      → `기타(OTHER)` 는 아무리 오래돼도 `지남` 이 안 붙는지(주기가 둘 다 없다)
- [ ] **B-41-2** 홈 `차량별` 카드에 **`정비 N건 지남`** 이 붙는지. 그 수가 차량 상세의
      `지남` 라벨 수와 **같은지** — 같은 계산(NextService)을 쓰므로 달라지면 안 된다
      (`NextServiceCard` 가 `key` 로 재생성되므로 "불러오는 중…"이 잠깐 보이는 건 정상)
- [ ] **B-42** `기타(OTHER)` 로 등록 → 다음 정비가 **계산되지 않는지**(주기가 둘 다 null)
- [ ] **B-43** **같은 날짜**로 두 건 등록 → 목록 순서가 뒤집히지 않는지
      (동점 기준 `id DESC`. 이게 없으면 새로고침마다 순서가 바뀐다)
- [ ] **B-44** 이력 수정에서 **비용만** 바꾸기 → Network 의 PATCH 요청 바디에
      **`cost` 하나만** 들어 있는지(바뀐 필드만 보낸다)
- [ ] **B-45** ⚠️ 수정 폼에서 **비용 칸을 비우고 저장** → 브라우저가 막는지.
      2026-09-18 에 여기만 `required` 가 빠져 있어서, 비우면 `Number('')` 가 **0** 이 되어
      **비용이 0원으로 덮어써졌다**(등록도 0원으로 저장됐다). 주유 폼의 금액 칸과 비교해 볼 것
- [ ] **B-46** 이력 삭제 → 204. 삭제되는 **그 행만** 잠기고 목록 전체가 잠기지 않는지
      (`deletingId` 로 잠근다. boolean 하나면 어느 줄을 지우는 중인지 안 보인다)
- [ ] **B-47** 이력을 11건 이상 등록 → 페이지네이션. 2페이지에서 한 건 삭제 시
      요청이 **두 번 나가지 않는지**(Network 탭에서 GET 개수 확인)
- [ ] **B-47-1** 정비 이력 머리의 **종류 필터가 옆 `이력 추가` 버튼과 같은 높이(32px)**인지.
      전에는 입력칸 높이(44px)·전체 폭이 이겨서 버튼보다 컸다. 데스크톱에서 글자가 13px,
      **모바일에서는 16px** 인지(더 작으면 iOS 사파리가 누를 때 화면을 확대한다)
- [ ] **B-48** 차량 상세가 `lg` 에서 2단인지. **왼쪽 사이드바가 스크롤에 붙어 따라오는지**
      (`lg:sticky` + `self-start`)

#### 6-B-5. 주유 기록과 연비

**이 기능은 코드를 쓴 뒤 한 번도 눈으로 못 봤다.** 연비는 서버가 계산해 주는 값이라,
화면에 찍힌 숫자가 맞는지는 손으로 나눠 봐야 안다.

- [ ] **B-49** 차량 상세 오른쪽에 **연비 카드 + 주유 기록 카드**가 있는지
- [ ] **B-50** 기록이 0건일 때 연비 카드가 **"첫 주유 기록은 기준점이 됩니다. 다음 주유
      기록부터 연비를 계산합니다."** 를 띄우는지 (0.00 km/L 이 찍히면 안 된다).
      목록 쪽 빈 상태는 "아직 주유 기록이 없습니다. 두 번째 기록부터 연비가 계산됩니다."
      → 기록이 없으면 **`연비 초기화` 버튼이 아예 없어야** 한다 — 눌러도 아무 일 없는 버튼을 두지 않는다
- [ ] **B-51** `주유 추가` → 날짜는 **오늘**, 주행거리는 **차량의 현재 값**이 미리 채워지는지
- [ ] **B-52** 주행거리 칸을 **지우면** 도움말이 **"주행거리를 적지 않으면 연비를 계산할 수
      없습니다."** 로 바뀌는지. 다시 입력하면 원래 도움말("계기판 숫자…")로 돌아오는지.
      비운 채 저장을 누르면 브라우저가 막는지(`required`).
      **등록과 수정 둘 다** — 같은 폼이라 한쪽만 되는 일은 없어야 한다
- [ ] **B-53** ⚠️ 주행거리를 **미리 채워진 값 그대로 두고** 저장 → **확인 창**이 뜨는지
      ("주행거리가 차량의 현재 값과 같습니다 / 이대로 저장하면 이번 구간의 연비가 계산되지
      않고, 차량 주행거리도 올라가지 않습니다"). **취소하면 저장이 안 되는지**(Network 탭에
      요청이 없어야 한다), **확인하면 저장되는지**(막는 게 아니라 묻는 것이다).
      → 계기판 값으로 **고쳐서** 저장하면 창이 **안 떠야** 한다 — 아무 때나 뜨면 아무도 안 본다
      → 수정 폼에서는 뜨지 않는다(등록 전용). 이미 저장된 값을 다시 확인할 이유가 없다
- [ ] **B-54** 주유량·금액을 입력하는 동안 **"리터당 약 N원"이 실시간으로** 바뀌는지.
      영수증과 대조해 오타를 그 자리에서 잡으라고 둔 것이다
- [ ] **B-54-1** ⚠️ **주유량·결제 금액을 비워 두고 저장** (2026-09-23 신설) → 막히지 않고
      **확인 창**이 뜨는지. 창에 비운 칸마다 한 줄씩(`주유량이 비어 있어…` / `결제 금액이 비어…`)
      적혀 있는지. 주행거리까지 안 고쳤다면 **세 줄이 한 창에** 모여 있는지 —
      조건마다 창을 띄우면 두 번째는 읽지 않고 누른다
      → 취소하면 저장이 안 되는지(Network 탭에 요청 없음), 확인하면 **201** 인지
      → 저장 뒤 목록 행이 **`— L` · `— 원`** 인지. **`0.00 L` 이나 `0원` 이면 잘못된 것이다** —
        0 은 "0리터를 0원에 넣었다" 는 뜻이고 적은 값처럼 보인다
      → 그 행의 **연비와 단가는 비어 있는데 거리(`+N km`)는 나오는지.** 모르는 것은
        "얼마나 넣었나" 뿐이고 계기판은 이어진다 — 실제로 여기를 한 번 잘못 묶어 거리까지 사라졌다
      → **다음 기록의 연비는 멀쩡한지.** 자기 구간만 빠진다
      → 요약의 `총 유류비`·`총 주유량`이 **적힌 것만 더한 값**인지(건수는 그대로 센다)
- [ ] **B-54-2-1** 두 번째 기록만 주유량을 비운 차량의 연비 카드가 **`아직 연비를 계산할 수 있는
      구간이 없습니다…`** 인지(2026-09-27). 전에는 달린 거리가 있는데도 "주행거리가 늘어난 기록이 없어"
      라고 말했다
- [ ] **B-54-2** 비운 칸의 도움말이 `비우면 이번 구간의 연비를 계산할 수 없습니다.` /
      `비우면 유류비 합계에서 빠집니다.` 인지. 값을 넣으면 사라지는지
- [ ] **B-54-3** ⚠️ **수정 폼에서 메모만 고쳐 저장 → 주유량이 그대로 남는지.**
      이게 이 기능의 핵심 함정이다 — JSON 은 "키가 없음"과 "null"이 서버에 똑같이 도착해서,
      null 을 비움으로 읽으면 메모만 고치는 요청이 주유량을 지운다(`clearLiters` 를 따로 둔 이유)
      → 반대로 **값을 지우고 저장하면 실제로 비워지는지**
- [ ] **B-55** 주유량에 `0` → 막히는지(연비 계산이 0으로 나누기가 된다).
      ⚠️ **비우는 것과 0 은 다르다** — 비우기는 "모름"이라 통과하고, 0 은 "0L를 넣었다"라 400 이다
- [ ] **B-56** 주유량에 `25.123`(소수 셋째 자리) → 막히는지(`@Digits fraction = 2`)
- [ ] **B-57** 첫 기록 등록 → 목록에 **`기준 기록 · 다음 주유부터 계산`** 으로 뜨는지.
      **0.00 이면 잘못된 것이고**(직전 기록이 없으면 null), 그냥 `—` 만 있어도 낡은 화면이다 —
      연비가 없는 이유가 둘(첫 기록 / 기준점)이라 **말도 둘로 갈라 뒀다**
- [ ] **B-58** ⚠️ **차량 주행거리가 따라 올라갔는지.** 주유 주행거리를 차량 값보다 크게 넣으면
      위쪽 히어로 숫자가 바뀌고, **그 차이만큼 굴러가는 연출**이 보여야 한다
- [ ] **B-59** ⚠️ 같은 순간 **왼쪽 `주행거리 갱신` 폼의 입력칸도 새 값으로 바뀌는지.**
      히어로 숫자만 바뀌고 입력칸이 옛 값에 머물면, 그걸 그대로 저장했을 때 **감소 확인 창을
      거쳐 주행거리가 되돌아간다.** 정비 이력을 추가했을 때도 똑같이 본다
- [ ] **B-60** 과거 주유를 **작은 주행거리로** 등록 → 차량 주행거리가 **안 내려가는지**
- [ ] **B-61** 그때 주행거리 칸 도움말이 **`차량에 기록된 N km 보다 작습니다. 과거 기록이면
      그대로 두세요.`** 로 바뀌는지. ⚠️ **막히지 않고 저장까지 되어야 한다** — 지난달 영수증을
      정리하는 건 정상적인 사용이고 계기판을 교체했을 수도 있다.
      평소 도움말은 "계기판 숫자. 이 값이 차량 주행거리보다 크면 차량 쪽도 함께 올라갑니다."
- [ ] **B-62** 두 번째 기록 등록(주행거리를 500km 올리고 25L) → 연비가 **20.00 km/L** 인지.
      손으로 나눠서 맞춰 볼 것
- [ ] **B-63** 연비 카드의 **평균 연비 히어로 숫자**가 뜨는지. 통계 4칸(기록·주행·주유량·총 유류비)
- [ ] **B-64** ⚠️ **카드 제목이 본문보다 크고 굵은지**(17px·굵기 600). 2026-09-19 까지
      `cn` 이 `text-section` 을 지워서 **16px·굵기 400 으로 렌더되고 있었다** — 카드 제목이
      본문과 거의 같아 구역 구분이 흐렸다. `연비`·`주유 기록`·`다음 정비 시점`·`차량 정보` 넷을
      나란히 보고, 그 아래 본문과 굵기 차이가 분명한지 확인한다
- [ ] **B-65** ⚠️ 주유를 저장할 때 **연비 카드가 사라지지 않는지.** 이 카드는 key 로 재생성되므로
      매번 로딩을 거치는데, 껍데기까지 없애면 **그때마다 아래 주유 기록 카드가 위로 튀었다
      내려온다.** 제목과 테두리는 남고 안쪽만 Skeleton 이어야 한다
      (`다음 정비 시점` 카드가 같은 방식이니 나란히 비교해 볼 것)
- [ ] **B-66** 히어로 숫자에 `tabular-nums` 가 **안 붙어 있는지**(맞출 상대가 없는 큰 숫자)
- [ ] **B-67** 목록이 **주행거리 내림차순**인지(최신이 위). 날짜순이 아니다 — 정렬이 곧 연비
      계산의 전제라 서버가 고정한다
- [ ] **B-68** 기록을 11건 이상 만들어 **페이지네이션**. ⚠️ **2페이지 첫 행에도 연비가 나오는지** —
      그 행의 짝은 1페이지에 있어서 서버가 따로 한 건 더 조회한다. 여기가 비어 있으면 버그다
      → 수정 폼을 **열어 둔 채 페이지를 옮기면 폼이 닫히는지.** 수정 중인 행이 화면에서
        사라졌는데 폼만 남아 있으면 무엇을 고치는 중인지 알 수 없다 (정비 이력도 같다)
- [ ] **B-69** 중간 기록의 **주행거리를 수정** → 그 기록과 **바로 뒤 기록의 연비가 둘 다** 바뀌는지
      (계산 값을 저장하지 않고 읽을 때 계산하는 이유가 이것이다)
- [ ] **B-70** ⚠️ 같은 순간 **`연비` 카드가 하나인지.** 2026-09-18 에 실제로 둘로 보였다 —
      형제 셋의 `key` 가 전부 0 에서 시작해 충돌했다. 수정을 **여러 번 반복**해도 하나인지,
      콘솔에 `Encountered two children with the same key` 가 없는지까지 본다
      (정비 이력 수정 뒤 `다음 정비 시점` 카드도 같은 구조라 함께 확인)
**이상 연비와 빠진 기록** (2026-09-17 신설. 만들고 눈으로 못 봤다):

- [ ] **B-71** 주행거리를 **한 자리 크게** 적은 기록을 만들어 연비를 50 넘게 만들기 →
      그 행에 **`확인 필요`** 가 빨갛게 붙는지. ⚠️ **숫자가 지워지지 않고 옆에 붙어야 한다** —
      무엇을 잘못 적었는지 보려면 그 값이 남아 있어야 한다
- [ ] **B-72** 같은 순간 **평균 연비가 멀쩡한지.** ⚠️ 그 값이 평균에 섞이면 히어로 숫자가
      90,000 km/L 같은 수가 되고 홈의 차량별 평균까지 같이 오염된다. 카드 아래에
      **"계산할 수 없는 구간 N곳을 평균에서 뺐습니다"** 가 떠야 한다 —
      말없이 빼면 그것도 거짓말이라 개수를 밝히는 자리다
- [ ] **B-73** 구간을 **고르게 4개 이상** 쌓은 뒤 중간 주유 하나를 삭제 (2026-09-23 개편)
      → ⚠️ **그 다음 행의 연비가 평균에 섞이지 않는지.** 지우면 그 구간 거리는 두 배가 되는데
        주유량은 한 번치뿐이라 연비가 두 배로 뜬다. 50 을 넘지 않아 `확인 필요` 에는 안 걸리므로,
        **빼지 않으면 조용히 평균을 끌어올린다** — 이게 이 항목의 핵심이다
      → 그 행에 **`기록 빠짐?`** 이 붙는지. 빨강이 아니라 회색인지(잘못이 아니라 빈자리다).
        **숫자는 지워지지 않고 옆에 남는지**
      → 연비 카드에 **"주유 기록이 빠진 것으로 보이는 구간 N곳을 평균에서 뺐습니다."** 가 뜨는지.
        그 N 이 `기록 빠짐?` 이 붙은 행 수와 **같은지**
      → ⚠️ **장거리 여행은 안 잡는지.** 평소의 두 배를 달리고 **주유량도 두 배**로 넣은 기록을
        하나 만들어 본다 — 거리만 보고 빼면 멀쩡한 구간을 버린다. 아무 표시도 없어야 한다
      → 구간이 **3개 미만이면 아무 표시도 없어야 한다**("평소"라는 게 없는데 의심부터 하면 안 된다).
        그 대신 삭제 확인 문구가 미리 말해 준다(B-79)
      → 지운 기록을 **다시 넣으면 표시와 안내가 사라지는지**. 문구가 빈말이 아니어야 한다
      → **한 행에 `확인 필요` 와 `기록 빠짐?` 이 같이 붙지 않는지** — 불가능한 값 쪽이 먼저다

**연비 초기화** (2026-09-17 신설. 만들고 눈으로 못 봤다):

- [ ] **B-74** 카드 오른쪽 위 `연비 초기화` → confirm **"지금까지의 기록을 연비 계산에서
      빼고 다시 셉니다. 계속할까요?"**
- [ ] **B-75** 확인 → 평균 연비 히어로 숫자가 사라지고 **"연비를 초기화했습니다. 다음 주유
      기록부터 다시 계산합니다."** 로 바뀌는지
- [ ] **B-76** ⚠️ **같은 순간 목록도 갱신되는지** — 기준점이 된 행이 `연비 기준점 · 다음
      주유부터 계산` 으로 바뀌어야 한다. 카드만 바뀌고 목록이 옛 연비를 들고 있으면
      `fuelListVersion` 이 안 도는 것이다(작업 중 실제로 빠뜨렸던 자리)
- [ ] **B-77** ⚠️ **통계 4칸(기록·주행·주유량·총 유류비)은 그대로인지.** 초기화는 연비만
      다시 세는 것이지 **지출을 없던 일로 만드는 게 아니다.** 여기가 줄어들면 잘못된 것이다
- [ ] **B-78** 주유를 한 건 더 등록 → 평균이 다시 나오고, 그 아래 **"연비 초기화 이후 구간만
      계산한 값입니다."** 가 붙는지. `초기화 해제` 를 누르면 전체 평균으로 돌아오는지
- [ ] **B-79** 기록 삭제 → 확인 문구가 **"지운 기록의 주유량이 함께 사라져 다음 기록의 연비가
      실제보다 높게 나옵니다."** 인지(2026-09-23). 삭제 후 실제로 그 행의 연비가 오르는지 —
      **오르는 게 맞다.** 틀린 건 숫자가 아니라 사라진 기록이고, 그래서 평균에서만 뺀다
- [ ] **B-80** 삭제 중 **그 행만** 잠기는지(목록 전체가 아니라)

#### 6-B-6. 홈 통계와 차트

**여기가 눈으로 처음 보는 화면이다** — 통계·차트는 만든 뒤 한 번도 못 봤다.

- [ ] **B-81** `/` → 통계 화면(세 번째 얼굴). eyebrow 가 `Overview`(0대일 때의 `Garage` 가 아니다),
      타일 4개는 `Vehicles` · `Distance` · `Records` · `Cost`.
      ⚠️ **`Records` 는 정비 + 주유 건수 합계**이고 **`Cost` 는 정비비 + 유류비**다 —
      정비만 세고 있으면 9/16 에 고친 것이 되돌아간 것이다
- [ ] **B-82** `Cost` 타일 아래에 **`정비 N · 주유 N`** 구성 한 줄이 붙는지.
      합계만 주면 어느 쪽이 큰지 알 수 없어서 둔 줄이다
      → 여기서 **"일부 기록만 합산됨" 같은 단서가 보이면 안 된다.** 요약 API 가 서버에서
        전부 더하므로 상한이 없어졌고, 화면이 변명하던 자리는 사라졌다
- [ ] **B-83** 타일 값에 `tabular-nums` 가 **안 붙어 있는지**(큰 숫자 하나는 맞출 상대가 없다)
- [ ] **B-84** 타일 사이 `gap-px` 격자의 선이 1px 인지. 칸 배경이 새지 않는지
- [ ] **B-85** 차트 제목이 **`지난 12개월 유지비`**(정비 + 주유)인지. 정비비만 세는 옛 제목이면
      타일의 `Cost` 와 기준이 달라져 같은 화면에 두 가지 총액이 놓인다.
      **12칸이 항상 다 있는지** — 기록 없는 달도 빈 칸으로 남아야 한다
      (기록 있는 달만 모으면 띄엄띄엄 정비한 것이 꾸준히 한 것처럼 보인다)
- [ ] **B-86** 막대 **네 모서리가 전부 각진지**, 두께가 24px 이하인지
- [ ] **B-87** 세로축 눈금이 `1,873` 같은 수가 아니라 **1·2·5 × 10ⁿ** 으로 끝나는지(`niceMax()`)
- [ ] **B-88** 눈금선이 **점선이 아니라 1px 실선**인지(점선은 "예측·임계선"으로 읽힌다)
- [ ] **B-89** **가장 높은 막대 하나에만** 값이 적혀 있는지(전부 적으면 축이 무의미해진다)
- [ ] **B-90** 막대에 마우스 → 말풍선. 판정 영역이 막대(24px)가 아니라 **칸 전체**인지
- [ ] **B-91** `<details>` 표를 펼쳐 말풍선과 **같은 값**이 나오는지.
      열이 **`정비 / 주유 / 합계`** 로 나뉘어 있는지 — 말풍선에만 구성이 있으면
      **키보드·스크린리더 사용자에게는 없는 값**이다
- [ ] **B-92** 종류별 가로 막대 — **전부 같은 색**인지(클수록 진하게가 아니다). **범례가 없는지**.
      아래에 **"유류비는 포함하지 않습니다"** 가 적혀 있는지 — 주유는 정비 종류가 아니라
      여기 들어갈 자리가 없는데, 옆 차트가 '유지비'가 되면서 헷갈리기 쉬워졌다
- [ ] **B-93** 막대가 자라는 동안 모양이 찌그러지지 않는지(`clip-path`, 칸마다 45ms)
- [ ] **B-94** `최근 활동` 카드에 **정비와 주유가 섞여** 나오는지(`최근 정비` 가 아니다).
      → 금액을 비운 주유가 **`0원` 이 아니라 `— 원`** 인지(2026-09-27)
      각 줄이 어느 쪽인지 구분되는지
- [ ] **B-95** **같은 날짜의 정비와 주유** → 정비가 위인지. ⚠️ 여기서 id 로 비교하면 안 된다 —
      테이블이 달라 주유 3번이 정비 3번보다 나중이라는 보장이 없다
- [ ] **B-96** `차량별` 카드에 **평균 연비**(`N.Nkm/L`)가 붙는지. 주유 2건 미만인 차량은
      그 자리를 **아예 비우는지**(`연비 —` 를 붙이면 없는 값이 자리를 차지한다)

#### 6-B-7. 프로필 · 비밀번호 · 탈퇴 · 계정 격리

- [ ] **B-97** 헤더 닉네임 클릭 → `/me`. eyebrow `ACCOUNT`, **`Section` 4개**
      (계정 / 비밀번호 / 화면 / 회원 탈퇴)
- [ ] **B-98** 닉네임만 변경 → PATCH 바디에 `nickname` 하나만. 헤더 표시도 같이 바뀌는지
      → 앞뒤에 공백을 넣어 저장하면 **입력칸도 공백이 잘린 값으로** 바뀌는지. 그대로 다시 저장하면
        `변경된 내용이 없습니다.` 인지(2026-09-27)
- [ ] **B-99** '화면' 구역의 설명이 현재 테마를 맞게 말하는지(`system` 이면 "지금은 ○○입니다")
- [ ] **B-100** '비밀번호' 구역 — 새 비밀번호 두 칸이 **서로 다르게** 입력되면 폼 안에서 막히는지
      (서버로 보내지 않는다. 확인란은 오타 방지 장치일 뿐이다)
- [ ] **B-101** 새 비밀번호를 7자로 → 400. 가입 때와 같은 제한인지
      → 한글 25자로도 → **400**. 가입만 막으면 가입으로 못 만드는 비밀번호가 변경으로 통과한다
- [ ] **B-102** ⚠️ **현재 비밀번호를 틀리게 입력 → 401 이 폼 안에 뜨는데 로그아웃되지 않는지.**
      작업 중 실제로 냈던 버그다 — 전역 401 핸들러가 돌면 오타 한 번에 `/login` 으로 쫓겨난다
- [ ] **B-103** 변경 성공 → 안내 문구 + **입력칸 3개가 비워지는지** + **로그인이 유지되는지**
- [ ] **B-103-0** ⚠️ 두 번째 브라우저에 같은 계정을 로그인해 두고 비밀번호 변경 →
      **지금 창은 그대로, 다른 창은 새로고침하면 로그아웃**인지
- [ ] **B-103-1** ⚠️ **비밀번호를 11번 연속 틀려 본다** → 11번째에 401 이 아니라
      **429 + `로그인 시도가 너무 많습니다. 10분 후…`** 인지. 그 뒤 **올바른 비밀번호로도
      막히는지**(잠긴 동안에는 맞혀도 안 들어간다). 다른 계정은 멀쩡한지.
      → 확인했으면 앱을 재시작해 푼다 — 잠금은 인메모리라 재시작하면 사라진다
- [ ] **B-104** 로그아웃 후 **새 비밀번호로** 로그인되는지. 옛 비밀번호로는 안 되는지
- [ ] **B-105** 비밀번호 관리자를 쓴다면 '현재'와 '새것'을 구분해 채우는지(`autoComplete`)
- [ ] **B-106** 로그아웃 → 헤더가 `로그인` 버튼으로 돌아가는지
- [ ] **B-107** 주소창에 `/vehicles` 직접 입력 → **`/login` 으로 튕기는지**.
      로그인하면 **원래 가려던 `/vehicles` 로 복귀**하는지
- [ ] **B-108** **두 번째 계정**으로 가입 → 차량 목록이 비어 있는지(**남의 차량이 안 보인다**)
- [ ] **B-109** 두 번째 계정에서 첫 계정 차량의 상세 주소(첫 계정에서 복사한 12자 주소)를 직접 입력 →
      Network 탭에서 **403 이 아니라 404** 인지. 없는 id(`/vehicles/zzzzzzzzzzzz`)와 **응답 본문까지
      같은지** — 문구가 다르면 그 문구가 존재 여부를 알려준다 (2026-09-22 통일)
- [ ] **B-110** 첫 계정 복귀 → 차량 삭제. ⚠️ confirm 문구가 **"이 차량과 정비 이력, 주유 기록이
      모두 삭제됩니다"** 인지 — 주유가 빠져 있으면 9/17 에 고친 것이 되돌아간 것이고,
      "주유 기록은 남겠지" 하고 누른 사용자가 유류비와 연비를 통째로 잃는다 →
      **이력과 주유 기록이 같이 사라졌는지 DB 로 확인**:
      `/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SELECT COUNT(*) FROM maintenance_records; SELECT COUNT(*) FROM fuel_records;"`
- [ ] **B-110-1** '내 기록' 구역에서 `JSON 내려받기` → `odolog-2026-09-21.json` 이 받아지는지.
      열어서 **차량 밑에 정비·주유가 중첩**돼 있는지, **비밀번호 해시가 없는지**,
      연비·단가 같은 계산값이 없는지(백업이라 원본만 담는다)
- [ ] **B-111** '회원 탈퇴' 구역이 **접힌 채로** 시작하는지. 버튼이 빨갛게 **채워져 있지 않은지**
- [ ] **B-112** 탈퇴에서 비밀번호를 **틀리게** → 401 이 폼 안에 뜨고 **로그인이 유지되는지**
- [ ] **B-113** 탈퇴 성공 → `/` 로 이동하고 헤더가 로그아웃 상태인지.
      **뒤로가기로 방금 화면에 돌아가지지 않는지**(`replace: true`)
- [ ] **B-113-1** 두 번째 브라우저에 같은 계정을 로그인해 두고 탈퇴 → 그쪽이 **500 이 아니라
      로그아웃 상태**가 되는지(전에는 없는 사용자 id 를 든 세션이 14일 동안 남았다)
- [ ] **B-114** 탈퇴한 계정으로 로그인 시도 → 401
- [ ] **B-115** 탈퇴 후 DB 에 그 사용자의 차량·정비 이력·주유 기록이 **하나도 안 남았는지**
- [ ] **B-116** 없는 주소(`/asdf`) → `/` 로 가는지(`/vehicles` 로 보내면 비로그인 사용자가 또 튕긴다)
- [ ] **B-117** 화면을 옮겨 다니며 **페이지 전환이 0.18s 페이드만**인지.
      화면이 통째로 떠오르면 `.stagger` 잔재가 남은 것이다

---

### 6-C. 2회차 — 폭 3개

1회차 데이터가 쌓인 상태 그대로, DevTools 반응형 모드에서 본다.

- [ ] **C-1 · 375px** 헤더에서 **화면 모드 토글이 사라지는지**(로고+닉네임+로그아웃만 286px)
- [ ] **C-2 · 375px** 닉네임이 말줄임만 남지 않는지
- [ ] **C-3 · 375px** 차량 목록 행의 **`ODOMETER` 라벨이 숨는지**(바로 아래 `km` 이 같은 말을 한다)
- [ ] **C-4 · 375px** 정비 이력 행이 **두 줄로 접히는지**(`flex-wrap` + `basis-full`)
- [ ] **C-5 · 375px** 차트 가로축 라벨이 **홀수 칸만** 남는지. 막대는 12개 그대로인지
- [ ] **C-6 · 375px** 히어로 숫자가 7자리를 넘어도 줄이 안 넘치는지(`clamp()` 40~80px)
- [ ] **C-7 · 375px** **가로 스크롤바가 생기지 않는지**(어느 화면에서든)
- [ ] **C-8 · 375px** 입력창 글자가 16px 인지 — 그보다 작으면 **iOS 사파리가 화면을 확대**한다
- [ ] **C-9 · 375px** 연비 카드의 통계 4칸이 **2열로 접히는지**(`grid-cols-2 sm:grid-cols-4`).
      주유 기록 행도 두 줄로 접히는지 — 정비 이력과 같은 구조다
- [ ] **C-10 · 375px** 홈 `Cost` 타일 아래 `정비 N · 주유 N` 줄이 **넘치지 않는지**.
      7자리 금액 두 개가 한 줄에 들어간다
- [ ] **C-11 · 1024px** 차량 상세가 2단인지. 폼 화면이 `Section` 2단인지
- [ ] **C-12 · 1024px** 긴 메모·긴 닉네임을 넣어도 **격자가 넘치지 않는지**(`min-w-0`)
- [ ] **C-13 · 1440px** 제목이 52px 까지 커지는지. 글이 담긴 열이 **700px 를 안 넘는지**
- [ ] **C-14 · 1440px** 본문이 76rem 에서 멈추고 가운데 정렬인지
- [ ] **C-15** 폭을 **천천히 끌어서** 줄여 본다 — 특정 폭에서만 깨지는 자리가 있는지
      (`clamp()` 를 쓴 이유가 `sm:` 계단의 분기점 누락을 막기 위해서다)

**날짜 휠** (2026-09-17 신설. 터치 기기에서만 나타나므로 여기에 둔다.
**실제 폰에서 봐야 한다** — 브라우저 반응형 모드는 `pointer: coarse` 를 흉내 내지 못한다):

- [ ] **C-16** 정비 폼·주유 폼의 날짜 칸이 **캘린더가 아니라** `2026. 9. 17.  변경` 모양인지.
      눌렀을 때 년/월/일 세 칸이 펼쳐지는지
- [ ] **C-16-1** ⚠️ **올해를 고르면 월 칸이 이번 달에서 끝나는지**(2026-09-23). 이번 달을 고르면
      일 칸이 **오늘에서** 끝나는지. 지난 해를 고르면 12월·31일까지 다시 늘어나는지
      → 지난 해 날짜를 고른 뒤 년만 올해로 굴려 본다 — 월·일이 순서대로 잘려 오늘 이하로
        내려오는지. 데스크톱의 `max` 와 같은 선이라야 기기마다 안 갈린다
- [ ] **C-17** ⚠️ **칸을 끝까지 세게 굴려도 페이지가 안 튀는지.** 남은 관성이 바깥으로 새면
      (scroll chaining) 화면이 통째로 스크롤된다. `overscroll-contain` 이 막고 있는 지점이다
- [ ] **C-18** ⚠️ **폼 맨 아래 날짜 칸에서 눌렀을 때 휠이 화면 안으로 따라 들어오는지.**
      이미 잘 보이는 자리에서는 **화면이 움직이지 않아야** 한다(`block: 'nearest'`)
- [ ] **C-19** 손가락을 떼면 칸이 **딱 맞춰 멈추는지**(scroll-snap). 반쯤 걸치지 않는지
- [ ] **C-20** 1월 31일로 맞춘 뒤 **월을 2월로** 굴리기 → 일이 **28(윤년이면 29)로 끌려오는지**.
      31일 자리에 멈춘 채 값만 바뀌면 위치와 값이 어긋난 것이다
- [ ] **C-21** 굴리는 **도중에는 값이 안 바뀌고** 멈춘 뒤에 바뀌는지.
      `변경` 을 다시 눌러(`완료`) 접으면 고른 날짜가 위에 그대로 있는지
- [ ] **C-22** 값이 폼 제출에 제대로 실리는지 — 등록 후 목록의 날짜가 고른 날인지

### 6-D. 3회차 — 테마 3개

- [ ] **D-1** 헤더 토글로 라이트 → 다크 → 시스템 전환
- [ ] **D-2** 전환이 **누른 버튼 자리에서 원형으로 번지는지**(View Transitions).
      원이 화면을 벗어난 뒤에도 계속 도는 것처럼 보이지 않는지
- [ ] **D-3** **다크로 두고 새로고침 → 흰 화면이 번쩍이지 않는지(FOUC).**
      `index.html` 인라인 스크립트가 일하는 순간이라 여기가 가장 중요하다
- [ ] **D-4** 모바일 주소창 색이 테마를 따라오는지(`theme-color` — 3중 중복인 자리)
- [ ] **D-5** `system` 으로 두고 **OS 설정을 바꿔** 따라오는지
- [ ] **D-6** 10개 화면을 다크로 다시 훑어 **안 보이는 글자가 있는지**.
      `faint`(대비 3.2)가 날짜·번호판 같은 읽어야 하는 값에 쓰이면 **그 값만** 안 보인다
      → 2026-09-19 에 두 곳을 고쳤다. 특히 확인할 자리: 주유 목록의
        **"· 다음 주유부터 계산"** 과 월별 차트 말풍선의 **"정비 N · 주유 N"**.
        지금 `faint` 가 남은 곳은 eyebrow 라벨 둘·화살표 아이콘·휠의 비선택 항목뿐이다
- [ ] **D-7** 카드가 배경과 **구분되는지**(다크 바닥 `#17171a` 위 카드 `#232325`)
- [ ] **D-8** `<input type="date">` 달력 아이콘, `<select>` 펼침 목록, **스크롤바**가
      반대 테마로 남지 않는지(`color-scheme` 이 하는 일 — CSS 로는 못 고치는 영역)
- [ ] **D-9** 라이트에서 **노이즈 텍스처가 안 보이는지**(다크 전용으로 뒀다)
- [ ] **D-10** 브라우저 탭의 **파비콘이 각진지**(2026-09-16 에 `rx="8"` 을 지우고 배경도
      `#17171a` 로 맞췄다. 캐시가 있으면 하드 리로드 `⌘⇧R`)

### 6-E. 4회차 — 키보드와 접근성

- [ ] **E-1** 마우스를 쓰지 않고 **Tab 만으로** 로그인 → 차량 등록까지 가 본다
- [ ] **E-2** 포커스 링이 **모든 요소에서 보이는지**(`focus-visible`)
- [ ] **E-3** 차량 목록 행에 Tab → **1px 표식이 hover 때와 똑같이** 그어지는지
- [ ] **E-4** 차트 막대에 Tab → 마우스 없이도 **같은 값**이 뜨는지
- [ ] **E-5** 페이지 이동 버튼에 `aria-label` 이 읽히는지
- [ ] **E-6** 스크린리더의 **heading 목록**(VoiceOver `⌃⌥U` → 제목)으로 차량 상세를 훑었을 때
      카드 여섯의 제목이 전부 잡히는지. `CardTitle` 이 `div` 였던 동안에는 h1 하나만 잡혀
      **화면 전체가 제목 없는 한 덩어리로 보였다**
- [ ] **E-7** DevTools > Rendering > `Emulate prefers-reduced-motion` → **연출이 전부 멈추는지**.
      특히 차트 막대가 **잠깐 안 보이는 시간으로 남지 않는지**(지연도 0 이어야 한다)
- [ ] **E-8** 브라우저 확대 200% 에서 레이아웃이 버티는지
- [ ] **E-9** 폼을 열면 **첫 칸에 포커스가 가는지**(`autoFocus`). 주유·정비는 각각
      주행거리·정비 종류, 차량 정보는 번호판이다 — 열자마자 Tab 을 여러 번 누르지 않아야 한다
- [ ] **E-10** 주행거리를 갱신했을 때 **스크린리더가 새 값을 한 번 읽는지**(macOS 는 VoiceOver
      `⌘F5`). ⚠️ **굴러가는 중간 숫자를 여러 번 읽으면 안 된다** — 화면의 숫자는
      `aria-hidden` 이고 확정 값만 `aria-live` 로 따로 알린다
- [ ] **E-11** 날짜 휠에 Tab 으로 들어가 **위·아래 화살표 키로** 년/월/일을 바꿀 수 있는지.
      스크롤로만 조작되면 키보드 사용자는 못 쓴다
      (터치 기기 + 키보드 조합에서만 닿는 자리라 데스크톱에서는 재현되지 않는다)

---

### 6-F. 발견한 것 (1~4회차를 돌며 채운다)

| # | 화면 | 폭·테마 | 증상 | 고침 |
|---|---|---|---|---|
|  |  |  |  |  |

→ 다 채운 뒤 **심각도 순으로 정렬해서** 한 번에 고친다.
→ 여기 적힌 것 중 **실제로 막혔던 것**만 `README.md` 트러블슈팅으로 옮긴다
  (문서 작성 규칙: 겪을 법한 문제·일반적인 팁은 적지 않는다).

### 6-G. 눈 확인이 끝난 뒤에 판단할 것

지금 정하면 추측이 된다. 1회차에서 실제로 불편했는지 보고 결정한다.

- [ ] 에러 토스트 / 인라인 에러 구분 기준 정리
      → 지금 방침: 폼 검증 실패(400/409)는 해당 필드 아래 인라인, 그 외(500 등)는 토스트.
      → **토스트 컴포넌트가 아직 없다.** B-08·B-19 에서 인라인만으로 충분했는지 보고 정한다.
- [ ] 백엔드 400 검증 응답과 폼 필드 연결
      → 현재 `ErrorResponse`는 `message` 하나뿐이라 **어느 필드가 틀렸는지 모른다.**
        필드별 표시가 꼭 필요하면 백엔드에 `fieldErrors` 추가가 선행돼야 한다(백로그).
      → B-09(비밀번호 7자)에서 메시지만으로 어느 칸이 문제인지 알 수 있었는지가 판단 근거다.
- [ ] PWA 로 만들지 (manifest + 아이콘 2종 + 서비스 워커)
      → 웹으로 확정했으므로 "홈 화면 아이콘"을 얻는 유일한 길이다. 비용이 거의 0 이고
        지금 코드와 디자인 시스템이 그대로 산다.
      → ⚠️ 하면 **`theme-color` 가 네 곳 중복**이 된다(`index.css`·`index.html`·
        `ThemeProvider.tsx`·manifest). manifest 는 정적이라 테마를 못 따라간다.

---

**재설계에서 함께 끝난 것** (2026-09-11):
`목록 스켈레톤` / `숫자·날짜 포맷`(tabular-nums 포함) / `접근성 기본`(Field가 label-htmlFor를
강제, 페이지 이동 버튼에 aria-label, focus-visible 링 유지, prefers-reduced-motion 대응) /
`document.title`·`파비콘`.

`반응형`은 모바일 폭까지 대응했고, 이후 PC 최적화에서 데스크톱 레이아웃까지 마쳤다
(`HISTORY.md` 의 2026-09-11 항목). **한때 "차량 목록을 그리드로 바꾸지 않는다"고 적어 두었으나 뒤집혔다** —
그 판단은 본문 폭 44rem을 전제로 한 것이었고, 76rem으로 넓히면서 전제가 사라졌다.
그 그리드마저 2026-09-14 에 **괘선 행**으로 다시 바뀌었다.

---

## 완료 판정 기준 (Definition of Done)

아래 시나리오를 브라우저에서 처음부터 끝까지 막힘없이 수행할 수 있으면 "완성"이다.

**Phase 6 의 1회차(6-B) 116개를 순서대로 따라가면 아래가 전부 덮인다.** 오른쪽이 그 항목
번호다 — 따로 한 번 더 돌 필요가 없다.

- [ ] 회원가입 → 로그아웃 → 로그인 — B-10, B-105
- [ ] 새로고침해도 로그인 상태 유지 — B-12
- [ ] 차량 등록 → 목록에 보임 → 상세 진입 — B-18, B-22, B-23
- [ ] 차량 정보 수정, 번호판을 안 바꿨을 때 409 가 나지 않음 — B-30
- [ ] 주행거리 갱신 — B-25
- [ ] 더 작은 값으로 갱신할 때 확인을 거쳐 정정할 수 있음(자리수 오타 복구) — B-26, B-27
- [ ] 미래 날짜를 정비·주유 어디에도 넣을 수 없음 — B-39
- [ ] 정비 이력 등록/수정/삭제 — B-40, B-44, B-46
- [ ] 다음 정비 시점이 주행거리·날짜 두 기준으로 표시됨 — B-41
- [ ] 주유 기록 등록/수정/삭제 — B-51, B-68, B-78
- [ ] 연비가 계산되고, 첫 기록은 `기준 기록 · 다음 주유부터 계산` 으로 이유까지 말함 — B-57, B-62
- [ ] 페이지가 넘어가도 연비가 끊기지 않음 — B-67
- [ ] 주유 기록이 차량 주행거리를 따라 올림 — B-58
- [ ] 연비 초기화 — 기록·지출은 그대로 두고 연비만 다시 셈 — B-73, B-76, B-77
- [ ] 불가능한 연비가 평균을 오염시키지 않고, 뺐다는 사실을 밝힘 — B-71
- [ ] 빠진 주유 기록을 평소 구간과 견줘 알려 줌 — B-72
- [ ] 비밀번호 변경, 현재 비밀번호를 틀려도 로그아웃되지 않음 — B-101, B-102
- [ ] 비밀번호를 잊어도 메일로 재설정할 수 있음 — B-08-2, B-08-5, B-08-6
- [ ] 탈퇴 전에 기록을 JSON 으로 챙겨 갈 수 있음 — B-110-1
- [ ] 회원 탈퇴 후 그 계정의 데이터가 남지 않음 — B-113, B-114
- [ ] 차량 삭제 시 정비 이력·주유 기록도 함께 사라짐 — B-109
- [ ] 로그인 안 한 상태로 `/vehicles` 직접 접근 시 로그인 페이지로 이동 — B-106
- [ ] 다른 계정으로 로그인했을 때 남의 차량이 안 보임 — B-107, B-108
- [ ] 백엔드 테스트 전체 통과 — `./gradlew test` (264개)
- [ ] 프론트엔드 테스트 전체 통과 — `npm run test` (45개)

---

## 백로그 — 프론트를 만들다 필요해질 백엔드 보강 (후보)

지금 당장 하지 않는다. 프론트에서 실제로 불편해지면 그때 꺼내 쓴다.

- [ ] `ErrorResponse`에 `fieldErrors` 추가 — 어느 필드가 왜 틀렸는지 프론트가 알 수 있게
- [x] ~~홈 통계 요약 API~~ — **2026-09-17 완료** (`GET /api/summary`). 아래는 당시 배경.
      → 지금은 프론트가 차량 목록 1번 + 차량마다 정비 이력 1번씩 받아서 **직접 더한다.**
        건수는 `totalElements` 라 정확하지만 **합계는 받아 온 행만 더한 값**이라, 한 차량의
        이력이 200건을 넘으면 일부만 반영된다(화면에 "일부 기록만 합산됨"으로 표시 중).
        SQL 한 번이면 정확하고 요청도 1번으로 준다.
- [x] 다음 정비 시점을 **전체 종류 한 번에** 반환하는 API — **2026-09-16 완료.**
      종류가 15개가 되면서 "실제 필요할 때"가 왔다.
- [x] ~~정비 이력 등록 시 차량 주행거리 자동 갱신~~ — **2026-09-17 완료.**
      규칙은 `Vehicle.liftOdometerTo()` 에 있고 주유·정비가 같이 쓴다.
- [ ] 이메일 중복 확인 API (`GET /api/users/exists?email=`) — 회원가입 폼 실시간 피드백용
      → 단, 이건 계정 존재 여부를 노출하는 API다. 로그인 실패 메시지를 일부러 통일해 둔 것과
        모순되므로 **도입 전에 트레이드오프를 다시 따진다.**
      → 2026-09-22 에 한 번 따졌다. 가입 409 가 이미 같은 것을 알려주고 있었고,
        **없애는 대신 IP 로 속도를 제한하기로 했다**(규칙 15). 이 API 를 따로 만들면
        그 제한 바깥에 같은 오라클을 하나 더 두는 셈이라, 만든다면 **같은 리미터를 통과시켜야** 한다.
- [x] ~~정비 이력 종류별 필터링 (`GET .../maintenance-records?type=`)~~ — **2026-09-25 완료.**
      정비 이력 머리의 종류 필터가 쓴다. 백로그에 체크를 안 해 두어 이틀 동안 남은 일로 보였다
- [x] ~~차량 목록에 각 차량의 "임박한 정비" 요약 포함~~ — **2026-09-25 완료.**
      "임박" 이 아니라 **이미 지난 것**을 센다(`overdueServiceCount` → `정비 N건 지남`).
      곧 다가오는 것까지 세려면 "얼마나 남았으면 임박인가" 기준부터 정해야 해서 뒤로 뒀다
- [ ] 만탱크 연비 — 지금은 매 주유마다 직전 기록과의 차이로 계산한다(단순법).
      **가득 채우지 않은 주유가 섞이면 그 구간만 실제보다 높게 나온다**(거리는 그대로인데
      리터가 적어서). 기록에 "가득 채웠는가" 플래그를 두면 가득→가득 구간으로 정확히 낼 수 있다.
      → 지금은 `FuelAnomaly` 가 **평소 구간과 견줘 이상한 구간을 알려 주는 것**으로 대신하고 있다.
- [x] ~~로그인 실패 응답 시간이 계정 존재 여부에 따라 다르다~~ — **2026-09-26 완료.**
      없는 이메일도 BCrypt 를 한 번 돌린다(규칙 15).
- [x] ~~배포한다면 먼저 따질 것 셋~~ — **2026-09-21 완료.** CSRF(double submit 쿠키),
      로그인 시도 제한(10분/10회), 쿠키 `secure`(환경변수 스위치). 자세한 내용은 `HISTORY.md` 에.

**PWA 는 여기 없다** — 할지 말지를 눈 확인 뒤에 정하기로 해서 **6-G** 에 뒀다.

---

단계를 완료할 때마다 `HISTORY.md` 맨 위에 항목을 더하고, 해당 줄을 위 체크리스트에서 제거한다.
