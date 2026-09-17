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
10. **로그인한 사용자 식별은 세션에서만 한다.** 요청 바디나 URL의 사용자 ID는 클라이언트가
    조작할 수 있으므로 신뢰하지 않는다 (`SessionConst.LOGIN_USER_ID`).
11. **예외는 의미에 맞는 상태 코드로 세분화한다**: 400(입력 검증 실패) / 401(미인증) /
    403(권한 없음) / 404(리소스 없음) / 409(리소스 중복). 서버 쪽 불변식이 깨진 경우
    (예: 세션엔 있는데 DB엔 없는 사용자)는 일부러 핸들러를 만들지 않고 500으로 흘려보내
    로그에 남긴다 — 모든 예외를 친절한 응답으로 감쌀 필요는 없다.
12. **예외는 전용 타입으로 던진다.** `IllegalArgumentException` 같은 JDK 범용 예외를 핸들러에
    매핑하지 않는다. 우리가 안 던진 예외까지 잡혀서 500이어야 할 것이 조용히 4xx로 나간다.
    상태 코드 하나당 예외 클래스 하나(`ConflictException`/`AuthenticationFailedException`/
    `ForbiddenAccessException`/`ResourceNotFoundException`).
13. **서비스는 클래스에 `@Transactional(readOnly = true)`, 쓰기 메서드에만 `@Transactional`.**
    메서드 쪽이 클래스 쪽을 덮어쓴다. 새 메서드를 깜빡했을 때 기본이 안전한 쪽(읽기 전용)이라
    쓰기가 실패해서 바로 드러난다. 반대로 하면 아무 일도 안 일어나 영영 모른다.

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
       text-display  52→80px clamp 굵기300 히어로 숫자 하나
       text-figure   22px                목록 행의 수치
       text-section  17px                카드·구역 제목
       text-lede     16px                제목 아래 설명

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
    ├── .gitignore                      Gradle·IntelliJ·macOS 산출물 + .env
    ├── CLAUDE.md                       설계 결정·이유·진행 상황·체크리스트 (작업용)
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
    │                                         **이 파일만 더 내려가지 못한다.** 컴포넌트 스캔이
    │                                         이 클래스의 패키지부터 시작하므로 bootstrap/ 같은
    │                                         하위 폴더로 옮기면 scanBasePackages·@EntityScan·
    │                                         @EnableJpaRepositories 를 전부 손으로 지정해야 한다
    │
    ├── user/  ────────────────────────────── 회원가입·로그인·프로필
    │   ├── domain/
    │   │   └── entity/
    │   │       └── User.java                 @Entity(users). uk_users_email 유니크 제약.
    │   │                                     changeNickname()/changePhone() — setter 없음
    │   ├── repository/
    │   │   └── jpa/
    │   │       └── UserRepository.java       findByEmail, existsByEmail
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
    │   │   └── application/
    │   │       └── UserService.java          signUp(중복 체크·BCrypt), login(사유 통일),
    │   │                                     findById, updateProfile(널 아닌 필드만),
    │   │                                     verifyPassword(되돌릴 수 없는 동작 앞의 관문 —
    │   │                                     changePassword 와 탈퇴가 공유), changePassword, delete
    │   └── controller/
    │       └── rest/
    │           └── UserController.java       POST /api/users, /login(+changeSessionId),
    │                                         /logout(204), GET·PATCH /api/users/me,
    │                                         PATCH /api/users/me/password(204)
    │
    ├── vehicle/  ─────────────────────────── 차량 등록·조회·주행거리·삭제
    │   ├── domain/entity/Vehicle.java        @Entity(vehicles). owner→User(@ManyToOne LAZY).
    │   │                                     uk_vehicles_user_plate_number(소유자+번호판 복합).
    │   │                                     updateOdometer()는 감소 시 ConflictException.
    │   │                                     liftOdometerTo()는 크면 올리고 작으면 그냥 넘어간다
    │   │                                     — 정비·주유를 기록하다 따라오는 경로용
    │   ├── repository/jpa/VehicleRepository.java
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
    │   │                                     register, findMyVehicles(Pageable),
    │   │                                     update(번호판이 실제로 바뀔 때만 중복 검사),
    │   │                                     updateOdometer(dirty checking),
    │   │                                     delete(이력 먼저 → 차량),
    │   │                                     deleteAllOwnedBy(탈퇴용 일괄 삭제),
    │   │                                     findOwnedVehicle(404/403 — maintenance도 재사용)
    │   └── controller/rest/VehicleController.java
    │                                         POST·GET /api/vehicles,
    │                                         GET·PATCH·DELETE /api/vehicles/{id},
    │                                         PATCH /api/vehicles/{id}/odometer
    │
    ├── maintenance/  ─────────────────────── 정비 이력·다음 정비 시점
    │   ├── domain/
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
    │   │                                     findTopByVehicleIdAndTypeOrderByServiceDateDescIdDesc,
    │   │                                     findByIdAndVehicleId(타 차량 소속 차단),
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
    │   │       └── schedule/NextServiceResponse.java         주행거리·날짜 두 기준
    │   ├── service/application/MaintenanceRecordService.java
    │   │                                     register, findByVehicle(Pageable),
    │   │                                     calculateNextService(km·개월),
    │   │                                     findOne, update(부분), delete.
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
    │   │                                     단가가 아니라 총액(total_cost)을 저장한다
    │   ├── repository/jpa/FuelRecordRepository.java
    │   │                                     findByVehicleId(Pageable), findByIdAndVehicleId,
    │   │                                     findTopByVehicleIdAndOdometerLessThan...(직전 1건),
    │   │                                     findAllByVehicleIdOrderByOdometerAscIdAsc(요약용),
    │   │                                     deleteByVehicleId
    │   ├── dto/
    │   │   ├── request/{register,update}/    liters 는 @Positive — 0 이면 연비가 0으로 나누기다
    │   │   └── response/
    │   │       ├── record/FuelRecordResponse.java
    │   │       │                             저장값 + 계산값(단가·거리·연비)이 함께 온다.
    │   │       │                             **계산값은 DB 에 없다** — 직전 기록이 바뀌면
    │   │       │                             달라지므로 읽을 때 계산해야 언제나 맞다.
    │   │       │                             구간이 성립 안 하면 null(0 이 아니다)
    │   │       └── summary/FuelSummaryResponse.java
    │   │                                     평균 연비 = 총 거리 ÷ (총 주유량 − 첫 주유량)
    │   ├── service/application/FuelRecordService.java
    │   │                                     register(+차량 주행거리 자동 갱신),
    │   │                                     findByVehicle(페이지당 쿼리 2번), findOne,
    │   │                                     update, delete, summary.
    │   │                                     **FIXED_SORT 로 정렬을 고정한다** — 정렬이 곧
    │   │                                     연비 계산의 전제라 sort 파라미터를 무시한다
    │   └── controller/rest/FuelRecordController.java
    │                                         POST·GET  .../fuel-records,
    │                                         GET  .../summary (리터럴이 {recordId} 보다 우선),
    │                                         PATCH·DELETE  .../{recordId}
    │
    ├── account/  ─────────────────────────── 조율 층. **여러 기능을 동시에 알아도 되는 유일한 자리**
    │   │                                     (프론트의 app/ 과 같은 성격 — 위 "의존 방향" 참고)
    │   ├── dto/request/withdraw/WithdrawRequest.java
    │   │                                     비밀번호 @NotBlank. 체크박스로 대신하지 않는다 —
    │   │                                     그건 실수만 막고 본인 확인이 아니다
    │   ├── service/application/AccountWithdrawalService.java
    │   │                                     withdraw(비밀번호 확인 → 차량·이력 → 사용자).
    │   │                                     순서만 정하고 실제 삭제는 각 기능이 한다
    │   └── controller/rest/AccountController.java
    │                                         DELETE /api/users/me(204) + 세션 invalidate.
    │                                         **URL 은 users 인데 패키지는 account** — UserController
    │                                         에 두면 user 가 account 를 알게 되어 순환이다.
    │                                         비밀번호는 본문에 싣는다(URL 에 넣으면 로그에 남는다)
    │
    └── common/  ──────────────────────────── 기능 어디에도 속하지 않는 공통 인프라
        ├── auth/
        │   ├── annotation/LoginUser.java     @Target(PARAMETER) 커스텀 애노테이션
        │   ├── resolver/LoginUserArgumentResolver.java
        │   │                                 세션 LOGIN_USER_ID → Long 주입. 없으면 401
        │   └── constant/SessionConst.java    세션 키 상수
        ├── domain/
        │   └── entity/BaseTimeEntity.java    @MappedSuperclass + @EntityListeners.
        │                                     createdAt/updatedAt 을 네 엔티티가 상속받는다.
        │                                     테이블을 만들지 않고 필드만 자식에 합쳐지므로
        │                                     컬럼 이름이 그대로다(ddl-auto 가 안 건드린다)
        ├── config/
        │   ├── web/WebConfig.java            ArgumentResolver 등록 + CORS(5173, credentials)
        │   ├── jpa/JpaAuditingConfig.java    @EnableJpaAuditing 스위치.
        │   │                                 **OdoLogApplication 에 두면 @WebMvcTest 가 전부
        │   │                                 깨진다**(JPA 메타모델이 비어 있음). 대신 여기 두면
        │   │                                 @DataJpaTest 가 못 집어 가므로 리포지토리 테스트에
        │   │                                 @Import 가 필요하다 — 실제로 둘 다 밟고 정했다
        │   └── openapi/OpenApiConfig.java    문서 제목/설명 + @LoginUser를 스펙에서 제외
        ├── dto/
        │   └── response/                     요청 DTO가 없어 response만 있다
        │       ├── error/ErrorResponse.java   record(message)
        │       └── page/PageResponse.java     record<T>(items/page/size/totalElements/
        │                                      totalPages/hasNext) + Page<T>.from()
        └── exception/                        ※ 기능별로 나누지 않는다. 세 기능이 모두 쓰는
            │                                   것이라 어느 한 기능으로 옮기면 잘못된 방향의
            │                                   의존이 생긴다
            ├── type/                         예외 타입만 모아 둔다 (상태 코드 하나당 하나)
            │   ├── ConflictException.java              409 전용
            │   ├── AuthenticationFailedException.java  401 전용
            │   ├── ForbiddenAccessException.java       403 전용
            │   └── ResourceNotFoundException.java      404 전용
            └── handler/GlobalExceptionHandler.java
                                              409/401/403/404/400 매핑. 전용 예외만
                                              잡는다 — IllegalArgumentException 같은
                                              JDK 범용 예외는 매핑하지 않음(규칙 12).
                                              IllegalStateException은 미처리 → 500.
                                              PropertyReferenceException(잘못된 sort) → 400.
                                              DataIntegrityViolationException 은 cause 의
                                              kind 가 UNIQUE 일 때만 409, 아니면 다시 던져 500

**같은 패키지였던 것이 갈라지면 import 가 새로 필요해진다.** 세분화하면서 실제로 컴파일이
세 곳에서 깨졌다: `LoginUserArgumentResolver`(→`LoginUser`,`SessionConst`),
`GlobalExceptionHandler`(→예외 4개), `MaintenanceRecord`(→`ServiceType`). 전에는 같은 패키지라
import 없이 쓰던 것들이다. **이건 부작용이 아니라 세분화가 드러낸 결합이다** — 이제 파일 맨 위만
봐도 그 클래스가 무엇에 기대는지 보인다.

### 백엔드 — 리소스와 테스트

    src/main/resources/application.yml   MariaDB 접속(${DB_USERNAME}/${DB_PASSWORD}),
                                         ddl-auto=update, open-in-view=false, SQL 로깅
    src/test/resources/application.yml   odolog_test 스키마, ddl-auto=create-drop.
                                         계정이 이 스키마 전용이라 파일에 그대로 적혀 있음

**테스트는 대상과 같은 경로를 그대로 따라간다.** 총 134개.

    src/test/java/com/odolog/app/
    ├── user/
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
    │   ├── repository/jpa/FuelRecordRepositoryTest.java
    │   │                                           @DataJpaTest — 직전 기록 조회, 타 차량 차단,
    │   │                                           BigDecimal 소수 보존, 일괄 삭제
    │   ├── service/application/FuelRecordServiceTest.java
    │   │                                           Mockito — 연비 계산, 페이지 경계(쿼리 2번),
    │   │                                           차량 주행거리 자동 갱신, 평균 연비
    │   └── controller/rest/FuelRecordControllerTest.java
    │                                               @WebMvcTest — 201/401/400(0L·누락·소수 3자리),
    │                                               /summary 라우팅, 목록 페이지
    ├── account/
    │   ├── service/application/AccountWithdrawalServiceTest.java
    │   │                                           Mockito — 삭제 순서(InOrder),
    │   │                                           비밀번호 틀리면 아무것도 안 지움
    │   └── controller/rest/AccountControllerTest.java
    │                                               @WebMvcTest — 204+세션 무효화, 401, 400
    └── maintenance/
        ├── repository/jpa/MaintenanceRecordRepositoryTest.java
        │                                               @DataJpaTest — 같은 날짜 동점 처리,
        │                                               페이징, 타 차량 차단, 이력 일괄 삭제
        ├── service/application/MaintenanceRecordServiceTest.java
        │                                               Mockito — 다음정비 3케이스, 부분수정
        └── controller/rest/MaintenanceRecordControllerTest.java
                                                        @WebMvcTest — next-service, enum 400,
                                                        목록 페이지 응답, delete 204

    ※ Mockito 테스트는 스프링 프록시를 안 거치므로 `@Transactional` 이 아예 적용되지 않고,
      `@WebMvcTest` 는 서비스가 `@MockitoBean` 이라 진짜 코드가 돌지 않는다. 즉 트랜잭션 설정
      실수는 이 둘로는 절대 못 잡는다 — 그래서 `VehicleServiceTransactionTest` 하나를 둔다.

### 프론트엔드 — `frontend/`

백엔드와 같은 기준으로 한 겹 더 내려간다. 화면은 화면 이름 폴더 안에(`pages/login/LoginPage.tsx`),
`api` 는 `endpoints/` 와 `types/` 로, `shared/ui` 는 성격별로.

    frontend/
    ├── package.json                  스크립트: dev / build / lint / preview
    ├── package-lock.json             설치된 정확한 버전 고정 — 반드시 커밋
    ├── vite.config.ts                react + tailwindcss 플러그인, '@' → ./src 별칭
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
        │                             .reveal(스크롤 진입 연출)과 View Transition 규칙도 여기
        ├── env.d.ts                  import.meta.env 타입 선언
        │
        ├── app/  ──────────────────── 조립층. **여러 기능을 동시에 알아도 되는 유일한 자리**
        │   ├── root/App.tsx          라우트 8개 정의 + Header 배치. 본문 폭 76rem
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
        │   │   └── stats/homeStats.ts
        │   │                         통계 조회·계산. 요청 수 = 1 + 차량 수
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
        │   │       ├── login/LoginPage.tsx    401 → 폼 에러. 원래 가려던 곳으로 복귀
        │   │       ├── signup/SignUpPage.tsx  가입 후 이어서 로그인까지. 409 → 폼 에러
        │   │       └── profile/ProfilePage.tsx
        │   │                                  Section 2개(계정 / 화면). 바뀐 필드만 PATCH.
        │   │                                  null 걸러내는 겉 + 폼 2단 구조
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
        │           │                     종류 5개 다음 정비 시점 (Promise.all 동시 요청).
        │           │                     재조회는 부모가 key 를 바꿔 재생성
        │           ├── section/MaintenanceSection.tsx
        │           │                     목록 + 페이지네이션 + 삭제 + 폼 토글
        │           └── form/MaintenanceForm.tsx
        │                                 등록·수정 겸용 (record가 null이면 등록)
        │
        └── shared/  ───────────────── 어느 기능에도 속하지 않는 것. 백엔드의 common과 같은 자리
            ├── api/
            │   ├── client/client.ts      fetch 래퍼. credentials:'include' / ApiError /
            │   │                         204 처리 / 401 전역 핸들러 등록 창구
            │   └── types/types.ts        PageResponse<T> / ErrorResponse 둘뿐.
            │                             기능별 DTO는 features/*/api/types/ 로 옮겼다
            ├── theme/                    라이트/다크. AuthContext와 똑같이 3파일로 나뉜다
            │   ├── context/ThemeContext.ts   Theme 타입 + localStorage 키 + useTheme 훅
            │   ├── provider/ThemeProvider.tsx 저장·복원, OS 설정 추적, View Transition 전환
            │   └── toggle/ThemeToggle.tsx    해/모니터/달 3칸 세그먼트 컨트롤 (헤더에 배치)
            ├── lib/
            │   ├── format/format.ts      formatNumber / formatKm / formatWon / formatDate /
            │   │                         todayString(UTC 함정 회피)
            │   └── hooks/useAsyncData.ts 조회 4곳의 공통 훅. data/loading/error +
            │                             reload()/setData. cancelled 플래그가 여기 한 곳에만
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
                │   │                     네이티브 date 입력. scroll-snap 이 드래그를 대신한다
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
                └── brand/mark.tsx        계기판 로고 SVG. 헤더·로그인·빈 상태 3곳이 공유

**폴더는 파일의 성격을 드러낼 때 만든다 — 파일 개수로 정하지 않는다.**
전에는 "폴더는 파일이 2개가 될 때 만든다"였고, 그 기준으로 2026-09-09에 "세분화는 끝났다"고
결론 냈었다. 2026-09-13에 **사용자 요청으로 그 기준을 바꿨다.** 바뀐 기준에서는
`dto/request/LoginRequest.java` 보다 `dto/request/login/LoginRequest.java` 가 낫다 —
폴더 이름이 "이 DTO는 로그인 유스케이스의 것"이라고 말해 주고, 형제 폴더 목록이 곧
그 기능의 유스케이스 목록이 된다.

**치르는 값은 정직하게 적어 둔다.** 파일을 보유한 폴더가 45개 → 81개, 그중 파일 1개짜리가
24개 → 74개가 됐다. 경로가 길어지고, 새 파일을 놓을 자리를 매번 판단해야 한다.
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

## 진행 상황 (완료)

- [x] 눈 확인 체크리스트·문서 최신화 — 코드를 못 따라간 곳 (2026-09-17)
      → **눈 확인을 시작하기 직전에 체크리스트부터 점검했다.** 목록대로 한 바퀴 돌고도
        **새 기능은 한 번도 안 본 채로 끝날** 뻔했다 — 9/16~17 에 붙인 것들이 6-B 에
        한 줄도 없었다.
      → **빠져 있던 것 14건을 더했다**: 연비 초기화(버튼·confirm·해제·"이후 구간만" 안내),
        ⚠️ 초기화 시 **목록도 같이 갱신되는지**(작업 중 실제로 빠뜨렸던 자리),
        ⚠️ **통계 4칸은 그대로인지**(초기화는 지출을 없던 일로 만드는 게 아니다),
        이상 연비 `확인 필요`, 빠진 기록 안내와 **구간 3개 미만이면 뜨면 안 되는 것**,
        과거 주행거리 입력 도움말과 **막히지 않고 저장되는지**, 홈 `최근 활동`(정비+주유),
        **같은 날짜면 정비가 먼저**, 차량별 평균 연비, `Cost` 타일의 구성 한 줄.
      → **화면과 어긋난 것 10건을 고쳤다.** 전부 "코드는 고쳤는데 체크리스트가 옛 화면을
        설명하던" 경우다:
        · **B-32** 다음 정비 카드 — "종류 5개가 전부 기록 없음" → 이력 없는 종류는 응답에서
          빠지므로(9/16) 실제로는 **한 문장**만 나온다. 그대로 뒀으면 **맞게 동작하는 화면을
          보고 "빈칸 목록이 안 나오네" 하며 버그로 적었을 것이다.**
        · **B-45 / B-50** 연비 없을 때의 문구 — `연비 —` 는 9/17 에 이유를 말하는 두 문구로
          갈라졌다(`기준 기록` / `연비 기준점`).
        · **B-69 / B-73 / B-79 / B-80** 홈 — `Records`·`Cost` 가 주유를 포함하고 월별 차트가
          `유지비` 가 된 것(9/16), 표의 `정비/주유/합계` 열.
        · **B-98** 차량 삭제 confirm — 주유 기록이 빠진 옛 문구를 그대로 적고 있었다.
        · **A-6** "스키마에 사용자가 없다" — 실제로는 `users 1 · vehicles 2 ·
          maintenance_records 3 · fuel_records 4`. 비우고 시작할지 정하는 항목으로 바꿨다.
        · **D-10** 파비콘 항목의 "오늘 고쳤다" — 그 "오늘"은 9/16 이다. 날짜를 적어 뒀다.
          **상대적인 날짜 표현은 문서에서 하루 만에 거짓말이 된다.**
      → **운영 DB 두 곳을 눈으로 확인했다.** `maintenance_records.type` 은 `varchar(30)`
        (9/16 의 ALTER 반영됨), `fuel_records.reset_point` 는 `bit(1) NOT NULL DEFAULT b'0'`
        (`@ColumnDefault` 덕에 기존 3건이 제대로 채워졌다). **둘 다 할 일이 남아 있지 않다.**
      → 항목 수 **137 → 151개**(준비 6 · 기능 105 · 폭 22 · 테마 10 · 접근성 8).
        README 는 "156개(… 폭 13 · 접근성 7)"라고 적고 있었는데 **세어 보니 137개였다**
        (폭은 13 이 아니라 20, 접근성은 7 이 아니라 8). 어림수를 적어 두면 다음 사람이
        그걸 믿고 다시 세지 않는다 — 스크립트로 세어 고쳤다.
      → 디자인 시스템 10번의 eyebrow 목록에 `Overview`(홈 통계)가 빠져 있던 것,
        백로그가 두 문서에서 갈려 있던 것(만탱크 연비·로그인 응답 시간은 README 에만,
        이메일 중복 확인·임박한 정비는 CLAUDE.md 에만)도 합쳤다.
      → **교훈을 Phase 6 머리말에 적어 뒀다**: 화면에 무언가를 더할 때 6-B 에 줄을 같이 넣는다.
        체크리스트가 낡으면 **한 바퀴를 다 돌아도 새 기능은 안 본 것**이 된다.
      → 코드는 한 줄도 안 건드렸다. 문서 2개(`CLAUDE.md`·`README.md`)만 고쳤다.

- [x] 기록을 빼먹었을 때를 잡아낸다 — 이상 연비 표시 + 과거 기록 안내 (2026-09-17)
      → 사용자 질문에서 출발했다: "기록을 까먹고 나중에 넣으면 주행거리가 되돌아가지 않나?"
      → **되돌아가지 않는다.** `liftOdometerTo` 가 작은 값을 무시한다(전날 고친 것이 이 경우다).
        그리고 **빠진 기록을 나중에 채우면 연비가 저절로 맞아진다** — 계산을 저장하지 않고
        읽을 때마다 하기 때문이다. 앞뒤 구간이 알아서 다시 계산된다.
      → **진짜 문제는 둘이었다.** ① 주유를 한 번 빼먹으면 그 구간 연비가 **정확히 두 배**로
        나오고 평균에도 섞인다 — 사용자는 "내 차 연비가 25?" 할 뿐 어디가 틀렸는지 모른다.
        ② `@PositiveOrZero` 말고는 검증이 없어 **날짜와 주행거리가 모순되는 입력**이 그대로 저장된다.
      → **절대 임계값만으로는 ①을 못 잡는다.** 12.5 가 25 가 되는데 25 는 불가능한 값이 아니다.
        그래서 그 차량의 **평소 구간과 견준다**(`FuelAnomaly`) — 늘 400km 쯤 달리고 넣던 차가
        갑자기 800km 를 달렸다면 중간에 기록하지 않은 주유가 있을 가능성이 높다.
      → **중앙값을 기준으로 삼았다.** 평균은 이상한 구간 자체에 끌려 올라가서 **잡으려는 것이
        기준을 오염시킨다.** 중앙값은 튀는 값 하나에 거의 움직이지 않는다.
      → 임계는 중앙값의 **1.8배**. 2배("한 번 빼먹음")는 잡고 계절 편차(1.5배쯤)는 넘긴다 —
        아무 때나 경고하면 아무도 안 본다. 구간이 **셋 미만이면 아예 판단하지 않는다**.
        "평소"라는 게 없는데 의심부터 하면 첫 기록마다 경고가 뜬다.
      → 별개로 **물리적으로 불가능한 연비**(50 초과 · 2 미만)는 행에 `확인 필요` 로 표시한다.
        숫자를 지우지 않고 옆에 붙인다 — 무엇을 잘못 적었는지 보려면 그 값이 남아 있어야 한다.
      → **② 는 새 API 없이 해결했다.** 차량 주행거리는 `liftOdometerTo` 때문에 **지금까지
        기록된 최댓값**이다. 폼이 이미 그 값을 `defaultOdometer` 로 받고 있어서, 입력값이
        그보다 작으면 "차량에 기록된 N km 보다 작습니다" 를 도움말에 띄운다.
      → **막지 않고 알려만 준다.** 지난달 영수증을 정리하는 건 정상적인 사용이고 계기판을
        교체했을 수도 있다. **사용자가 옳을 수 있는 자리에서 길을 막지 않는다.**
      → 안내 문구도 해법을 말한다: "주유 기록이 빠졌다면 채워 넣으면 연비가 다시 계산됩니다."
        실제로 그렇게 동작하므로 빈말이 아니다.
      → 테스트 128 → **134개**. `FuelAnomalyTest` 는 임계 자체보다 **판단하지 않아야 할 때**를
        더 많이 검증한다 — 고른 구간, 1.5배 편차, 구간 3개 미만, 기준점 건너뛰기.
      → 검증: `./gradlew test` 134개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과.

- [x] 화면의 말이 코드를 못 따라간 곳 5건 (2026-09-17)
      → 주유 기록을 넣으면서 **코드는 따라갔는데 화면의 문구가 안 따라갔다.**
        특히 되돌릴 수 없는 동작 둘이 실제보다 약하게 말하고 있었다.
      → 🔴 **차량 삭제 확인**: "이 차량과 정비 이력이 모두 삭제됩니다" → 주유 기록도 지워진다
        (`VehicleService.delete` 가 `fuelRecordRepository.deleteByVehicleId` 를 부른다).
        "주유 기록은 남겠지" 하고 누르면 유류비와 연비가 통째로 사라진다.
        같은 화면의 삭제 버튼 옆 설명도 같은 문제였다.
      → 🔴 **회원 탈퇴 설명**: "계정과 등록한 차량·정비 이력이 모두 삭제됩니다" → 역시 주유 누락.
      → 🟡 홈 머리말("등록한 차량과 정비 기록을 한눈에") — 며칠에 걸쳐 유지비·연비·최근 활동을
        넣어 고친 바로 그 화면인데 머리말만 옛날 그대로였다.
      → 🟡 차량 목록 빈 상태 — 랜딩은 주유·연비를 넣어 고쳤는데 여기를 놓쳤다.
      → 고친 뒤 **삭제 코드와 문구를 나란히 놓고 대조했다.** 이번 문제의 본질이 그 어긋남이라
        "고쳤다"로 끝내면 같은 일이 반복된다.
      → **교훈**: 기능을 더할 때 `delete`·`withdraw` 처럼 **되돌릴 수 없는 동작의 설명**을
        먼저 찾아본다. 거기서 틀리면 단순 오타가 아니라 사용자가 잘못된 판단을 하게 된다.
      → 정비 이력 목록·종류별 차트의 "아직 등록된 정비 이력이 없습니다" 는 정비만 다루는
        자리라 그대로 뒀다. 전부 바꾸는 게 아니라 **범위가 틀린 것만** 고쳤다.
      → 검증: 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) / `vite build` 통과.
        백엔드는 안 건드렸다(128개 그대로).

- [x] 정비 이력도 차량 주행거리를 올린다 — 규칙을 엔티티로 (2026-09-17)
      → 주유엔 있고 정비엔 없던 비대칭을 없앴다. 정비소에서 5만km 에 엔진오일을 갈았다고
        적어도 차량 주행거리는 그대로여서, **같은 숫자를 주행거리 갱신에 한 번 더** 넣어야 했다.
      → **규칙을 서비스가 아니라 엔티티에 뒀다**(`Vehicle.liftOdometerTo`). 전에는
        `FuelRecordService` 의 private 헬퍼였는데, 정비도 쓰게 되면서 서비스에 두면 둘이
        나눠 갖거나 복사해야 한다. 코드 설계 원칙 2("비즈니스 규칙은 엔티티 안에")대로다.
      → **`updateOdometer` 와 나눠 둔 것이 핵심이다.** 같은 필드를 건드리지만 의도가 다르다:
        · `updateOdometer` — 사용자가 주행거리 자체를 고치는 동작. **줄어들면 예외**로 막는다.
        · `liftOdometerTo` — 기록하다 따라오는 것. **작으면 조용히 넘어간다.**
        후자에서 예외를 던지면 **과거 정비를 뒤늦게 입력하는 것 자체가 막힌다** — 그건 정상적인
        사용이다. 이름을 둘로 나눠 두면 호출부에서 어느 의도인지 보인다.
      → 등록뿐 아니라 **수정에도** 적용했다. 주유에서 같은 구멍을 한 번 밟았던 자리다
        (자리수를 잘못 넣었다 고치면 기록만 고쳐지고 차량은 틀린 채로 남는다).
      → **프론트에 같은 구멍이 있었다.** 주유는 등록 후 차량을 다시 받는데(`reloadVehicle`)
        정비는 안 받고 있었다. 서버가 주행거리를 올리기 시작했으므로 이제 화면이 옛 값을
        들고 있게 된다. 정비 쪽에도 붙였다 — 히어로 숫자가 오른 만큼 굴러가는 연출도 거기서 나온다.
      → 테스트 125 → **128개**. 새 3개를 **갱신 코드를 뺀 상태에 돌려 실제로 2개가 실패하는
        것까지 확인**했다(나머지 1개는 "작으면 안 올린다"라 뺀 상태에서도 통과하는 게 맞다).
      → 백로그에서 이 항목을 지웠다.
      → 검증: `./gradlew test` 128개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과.

- [x] 연비 초기화 + 연비 없을 때의 안내 (2026-09-17)
      → 요청: 연비가 안 뜰 때 `연비 —` 만 나와서 **왜 없는지 알 수 없다.** 그리고 연비를
        다시 세는 기능이 있으면 좋겠다.
      → **초기화를 "기록 삭제"로 만들지 않았다.** 주유 기록을 지우면 **유류비 통계까지
        함께 사라진다.** 사용자가 원하는 건 "연비를 다시 시작"이지 "지출을 없던 일로"가 아니다.
        대신 `FuelRecord.resetPoint` 로 **"여기서부터 다시 센다"는 기준점**을 찍는다.
      → 그래서 건수·비용·주유량은 **전체**를 세고 연비만 기준점 이후를 본다. 테스트로 못박았다.
      → 기준점이 여럿이면 **가장 최근 것이 이긴다**(오름차순 목록을 뒤에서부터 찾는다).
        한 번만 찍게 막지 않은 이유: 여러 번 초기화하는 게 자연스럽고, 그때마다 옛 표시를
        지우러 다닐 이유가 없다.
      → **기준점 기록 자체는 구간 연비가 없다.** 직전과의 연결을 끊는다는 뜻이므로
        첫 기록과 같은 처지가 된다. 그래서 초기화 직후에는 평균도 null 이고, 화면이
        **"다음 주유 기록부터 다시 계산합니다"** 라고 말한다 — 요청한 문구와 맞아떨어진다.
      → **전용 엔드포인트를 만들지 않았다.** "여기서부터 다시 센다"는 그 기록의 속성이지
        차량의 동작이 아니다. `FuelRecordUpdateRequest` 에 `resetPoint` 를 얹으니 아무 기록이나
        기준점으로 삼거나 풀 수 있고 엔드포인트도 안 늘었다.
      → **공식이 두 벌이던 것을 이때 발견해 하나로 합쳤다.** 평균 연비 계산이
        `FuelRecordService.summary` 와 `GarageSummaryService` 에 복사돼 있었다. 기준점을 한쪽에만
        반영하면 **차량 상세와 홈에서 다른 연비가 뜬다** — 사용자가 어느 쪽을 믿어야 할지 모르는
        상태다. `fuel/domain/calculation/FuelEfficiency` 로 뽑아 둘이 같은 것을 쓴다.
      → 요약 응답에 `latestRecordId`·`resetPointId` 를 담았다. 없으면 화면이 "가장 최근 기록"을
        알려고 목록을 한 번 더 받아야 한다.
      → **`@ColumnDefault("false")` 를 붙였다.** `ddl-auto: update` 가 기존 행이 있는 테이블에
        NOT NULL 컬럼을 default 없이 붙이면 옛 행에 무엇이 들어갈지는 DB 구현에 달린다.
        운영 `fuel_records` 에 이미 3건이 있어서 그냥 넘길 자리가 아니었다.
      → **목록 갱신을 빠뜨렸다가 잡았다.** 기준점을 바꾸면 각 행의 구간 연비도 달라지는데
        그 동작은 카드에서 일어난다. 카드만 재생성하면 목록은 옛 값을 그대로 들고 있다.
        `fuelListVersion` 을 따로 둬 그때만 목록도 재생성한다 — 평소(추가·수정·삭제)에는
        목록이 스스로 갱신하므로 건드리지 않는다. 한 값으로 묶으면 목록이 자기 변경 때마다
        스스로를 재생성해 페이지와 폼 상태를 잃는다.
      → 목록의 `연비 —` 를 **`기준 기록 · 다음 주유부터 계산`**(첫 기록) /
        **`연비 기준점 · 다음 주유부터 계산`**(초기화) 으로 갈랐다. 이유가 둘이라 말도 둘이다.
      → 초기화 이후 평균 옆에는 **"초기화 이후 구간만 계산한 값입니다"** 를 적는다.
        안 적으면 전체 평균으로 오해한다.
      → 테스트 122 → **125개**. 새 테스트 3개: 초기화 이후 구간만 평균에 들어가는지(건수·비용은
        전체인지 함께 확인), 기준점이 마지막 기록이면 평균이 없는지, 기준점 기록의 구간 연비가
        끊기는지.
      → 검증: `./gradlew test` 125개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과. **브라우저 눈 확인은 사용자 몫이다.**

- [x] 홈 통계 요약 API — 요청 7번 → 1번, 상한 제거 (2026-09-17)
      → **화면이 사용자에게 변명하던 유일한 자리를 없앴다.** 타일 아래 "일부 정비 기록만
        합산됨" 은 페이지 상한(차량 100 / 기록 200)을 넘으면 합계가 틀린다는 고백이었다.
      → `GET /api/summary` 하나가 타일·월별 차트·종류별 차트·차량별·최근 활동을 전부 준다.
        **경로가 `/api/vehicles/summary` 가 아닌 이유**: 정비 이력과 주유 기록까지 담으므로
        차량의 하위 자원이 아니다.
      → **새 패키지 `summary/`.** 회원 탈퇴(`account`)와 같은 문제 — 세 기능을 모두 알아야 하는데
        어느 하나에 넣으면 의존이 역방향이 된다. 다만 탈퇴는 *순서를 조율*하고 이쪽은
        *읽어서 합친다*는 점이 달라 성격이 섞이지 않게 따로 뒀다. 조율 층이 둘이 된 셈이다.
      → 여기서는 **리포지토리를 직접 쓴다.** 하는 일이 집계뿐이라 각 기능 서비스의 비즈니스
        규칙(소유권 검사·삭제 순서)이 필요 없고, 소유자 id 로 조회하므로 남의 데이터가
        애초에 섞이지 않는다. `account` 가 서비스를 주입받는 것과 반대 선택이고 이유도 반대다.
      → 조회는 `findByVehicle_Owner_Id...` — vehicle → owner → id 를 타고 올라가는 파생 쿼리다.
        차량마다 따로 조회하면 차량 수만큼 쿼리가 나간다. **쿼리 3번, HTTP 1번.**
      → **N+1 이 될 뻔한 자리를 막았다.** 최근 활동에 차량 이름이 필요한데
        `record.getVehicle().getManufacturer()` 로 읽으면 LAZY 프록시가 초기화된다
        (`getId()` 와 달리 FK 만으로는 알 수 없는 값이다). 같은 트랜잭션이라 1차 캐시가
        받아 주긴 하지만 **그 사실에 기대는 코드**가 된다. 이미 읽어 둔 차량으로 지도를
        만들어 로딩 순서와 무관하게 했다.
      → **정렬에서 한 번 틀렸다.** `thenComparing(RecentActivity::kind)` 로 두면
        `"FUEL" < "MAINTENANCE"` 라 주유가 앞선다. 프론트는 정비를 먼저 두고 있었으므로
        순서가 뒤집혔을 것이다 — 알파벳에 맡기지 않고 `MAINTENANCE → 0` 으로 직접 정했다.
      → "오늘"을 서비스 밖(컨트롤러)에서 넘긴다. 안에서 `LocalDate.now()` 를 부르면 월별
        12칸이 실행 시각에 좌우되어 테스트에서 고정할 수 없다.
      → 프론트: `homeStats.ts` **258 → 81줄**. `monthlyCost`/`costByType`/`lastMonths`/`sum`,
        `sumsComplete`, 페이지 크기 상수 두 개가 통째로 사라졌다. 계산이 한 줄도 남지 않았다.
        `HomePage` 에서는 `exact` 분기와 안내 문구가 빠지고 **차량별 평균 연비가 돌아왔다**
        (프론트에서 계산하면 "첫 주유량을 뺀다"는 규칙이 두 곳에 생겨 뺐던 것이다).
      → 번들 349.6 → **347.0 kB**. 기능이 늘었는데 줄었다.
      → 테스트 113 → **122개**, 엔드포인트 23 → **24개**.
      → 검증: `./gradlew test` 122개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과 + 남은 import 가 전부 쓰이는지 확인.
        **브라우저 눈 확인은 사용자 몫이다.**

- [x] 폰에서 날짜를 드럼 휠로 고르게 (2026-09-17)
      → 요청: 폰에서 캘린더 말고 **년·월·일을 각각 굴려서** 고르고 싶다.
        `<input type="date">` 가 OS 기본 선택기를 띄우는데, 안드로이드는 달력 격자라
        몇 달 전 기록을 넣으려면 달을 여러 번 넘겨야 한다.
      → **드래그를 직접 구현하지 않았다.** 세로 스크롤이 곧 드래그이고 `scroll-snap` 이 칸
        맞춤을 해 준다. pointer 이벤트로 만들면 관성·경계 바운스·스크린리더 대응을 전부 다시
        만들어야 하는데 브라우저가 이미 갖고 있다. JS 는 "멈췄을 때 어느 칸인가"만 계산한다.
      → **터치 기기에서만 휠이다**(`matchMedia('(pointer: coarse)')`). 마우스·키보드에서는
        날짜를 **타이핑**하는 게 제일 빠르고, 드럼을 마우스 휠로 굴리는 건 그보다 느리다.
        디자인 11-1 이 이미 같은 기준으로 터치를 가르고 있다. CSS 로는 컴포넌트를 갈아 끼울 수
        없어 `matchMedia` 를 쓴다.
      → **커밋 시점은 `scrollend`.** 굴리는 도중에 확정하면 지나가는 숫자마다 값이 바뀐다.
        `scrollend` 가 없는 브라우저에서는 마지막 `scroll` 에서 120ms 기다린다.
      → **"내가 굴린 것인지"를 기억하지 않는다.** 값이 바뀌면 그 칸으로 스크롤을 맞추는데,
        사용자가 굴려서 바뀐 경우에는 이미 그 자리라 아무 일도 안 일어난다.
        **실제로 움직이는 건 일수가 잘린 경우뿐**이다(1/31 에서 2월로 가면 2/28).
        플래그를 두면 그 보정까지 막혀서, 위치와 값이 어긋난 채로 남는다.
      → 일수 보정은 `new Date(연, 월, 0).getDate()` 에 맡긴다. 윤년을 직접 계산하지 않는다.
        **node 로 8케이스를 돌려 확인**했다(1/31→2월 평년·윤년, 3/31→4월, 2100년(윤년 아님),
        2000년(윤년) 등).
      → 접근성: 칸마다 `role="listbox"` + `aria-label`, 항목에 `role="option"`/`aria-selected`,
        **위·아래 화살표 키**로도 고를 수 있다. 스크롤로만 조작되면 키보드 사용자는 못 쓴다.
      → 닫혀 있을 때는 입력창처럼 보이는 `<button>` 이다. **button 도 label 이 가리킬 수 있는
        요소**라 `Field` 의 htmlFor 가 그대로 동작한다. 펼침은 정비 폼과 같은 `.form-open`.
      → 200px 짜리 휠을 폼에 항상 펼쳐 두지 않고 접어 둔 이유: 정비 폼은 필드가 다섯 개라
        휠이 상시 열려 있으면 폼이 화면 두 배가 된다.
      → `oxlint react(set-state-in-effect)` 를 한 번 밟았다(포인터 종류를 effect 에서 setState).
        규칙을 끄지 않고 **초기화 함수에서 읽도록** 고쳤다 — 값이 렌더 시점에 이미 있다.
        그대로 뒀으면 첫 프레임에 네이티브 입력이 보였다가 휠로 바뀐다.
      → 검증: `tsc -b` / `oxlint`(기존 shadcn 경고 1건) / `vite build` 통과 +
        빌드 CSS 에 `snap-y`·`snap-mandatory`·`snap-center` 생성 확인 + 일수 보정 8케이스.
        **실제 기기에서 굴려 보는 건 사용자 몫이다** — 관성과 손맛은 코드로 확인할 수 없다.
      → **후속(2026-09-17): 점검에서 UI 버그 5건을 찾아 고쳤다. 상위 셋이 이 휠에 있었다.**
      → ⚠️ **스크롤 연쇄(chaining)를 안 막고 있었다.** 칸을 끝까지 굴리면 남은 관성이 바깥
        페이지로 넘어가 화면이 통째로 스크롤된다 — 폼 중간에서 날짜를 고르다 화면이 저 아래로
        날아간다. `overscroll-contain` 한 줄이면 막힌다.
        **"네이티브 스크롤을 쓰면 공짜로 얻는다"는 판단은 대부분 맞았지만 연쇄만은 예외다** —
        관성·스냅·스크린리더는 브라우저가 주지만 연쇄는 명시적으로 꺼야 한다.
      → **펼쳐도 화면이 따라오지 않았다.** 정비 폼은 필드가 다섯 개라 맨 아래 날짜 칸에서
        누르면 200px 짜리 휠이 화면 밖에서 펼쳐져 아무 일도 안 일어난 것처럼 보였다.
        `scrollIntoView({ block: 'nearest' })` — 이미 보이면 가만두고 잘렸을 때만 그만큼만 움직인다.
      → **빈 값·깨진 값이 오면 NaN 이 찍혔다.** 네이티브 date 입력은 사용자가 지우면 빈 문자열을
        주는데, 그 상태로 태블릿을 키보드에서 빼면 휠이 그 값을 받는다. `parse()` 가 오늘로
        되돌리게 했다(node 로 5케이스 확인). `required` 는 네이티브 경로 전용임을 주석에 못박았다
        — 휠은 `join()` 이 언제나 완전한 날짜를 만들어 빈 값이 될 수 없다.
      → **`VehicleInfoForm` 의 연식에서 `required` 를 뺐다.** 연식이 없는 차량(등록 API 에
        `@NotNull` 이 붙기 전 데이터)은 이 칸이 비어서 시작하는데, required 면 **모르는 연식을
        지어내야만 제조사 오타를 고칠 수 있었다.** 비워 두면 request 에 안 담기고, 부분 수정에서
        "안 보냄"은 "그대로 둠"이다. 도움말에 그렇게 적었다.
      → **월별 유지비의 `표로 보기` 에 `overscroll-x-contain`.** 열이 5개라 좁은 화면에서
        가로로 미는데, iOS 사파리는 가로 스크롤이 끝에 닿으면 뒤로가기 제스처로 넘어갈 수 있다.
      → 빌드 CSS 에 `overscroll-contain`·`overscroll-x-contain` 이 실제로 생성됐는지 확인했다.

- [x] 점검에서 찾은 결함 5건 수정 (2026-09-16)
      → **① 홈 통계의 `Cost` 가 유류비를 빼고 있었다.** `homeStats.ts` 가 정비 이력만 조회하는데
        타일 라벨은 그냥 `Cost` 라, **틀릴 수 있는 값을 맞는 값처럼 보여주고 있었다** —
        `sumsComplete` 로 "일부만 합산됨"을 표시해 둔 이 프로젝트의 원칙과 정면으로 어긋난다.
        이제 `정비비 + 유류비` 이고, 타일 아래에 **구성을 한 줄로 적는다**(합계만 주면 어느 쪽이
        큰지 알 수 없다). `Records` 도 정비 + 주유 건수 합계로 바꿨다.
      → 유류비는 차량별 `/fuel-records/summary` 로 받는다. **서버가 전부 합산하므로 정비 비용과
        달리 페이지 상한에 안 걸린다** — `sumsComplete` 는 이제 정비에만 해당하고, 안내 문구도
        "일부 **정비** 기록만 합산됨"으로 좁혔다. 요청은 `1 + 차량수` → `1 + 차량수 × 2`.
      → **후속(같은 날): 타일만 고치고 차트를 빠뜨렸다.** 월별 차트의 히어로 숫자가 여전히
        정비비만 더한 값이라, 같은 화면에 "유류비를 포함한 총 지출"과 "정비비만"이 나란히
        놓였다. 제목이 "정비 비용"이라 틀린 말은 아니었지만, 타일을 고친 순간 기준이 둘이 됐다.
      → 월별 차트를 **`지난 12개월 유지비`(정비+주유)** 로 바꿨다. 표에 `정비 / 주유 / 합계`
        열을 나눠 구성을 보여주고(열이 5개라 좁은 화면에서는 가로 스크롤), 말풍선에도 같은
        구성을 붙였다 — **말풍선에만 있으면 키보드·스크린리더 사용자에게는 없는 값이다.**
        `정비 종류별 비용` 차트에는 "유류비는 포함하지 않습니다"를 적었다. 주유는 정비 종류가
        아니라 거기 들어갈 자리가 없는데, 옆 차트가 '유지비'가 되면서 헷갈리기 쉬워졌다.
      → **출처를 하나로 통일하느라 주유 조회를 요약 API → 목록으로 되돌렸다.**
        `/fuel-records/summary` 는 정확한 합계를 주지만 **달별 내역이 없다.** 합계는 요약에서,
        달별은 목록에서 가져오면 같은 데이터가 두 출처에서 오게 되어 숫자가 어긋났을 때 어느
        쪽이 맞는지 알 수 없다. 대가로 유류비도 페이지 상한(200건)에 걸리게 됐고, 그래서
        `sumsComplete` 가 정비·주유 양쪽에 똑같이 적용된다.
      → 그 결과 **차량별 카드의 평균 연비를 도로 뺐다.** 요약 API 를 안 부르니 값이 없고,
        프론트에서 다시 계산하면 "첫 주유량을 빼는" 비즈니스 규칙이 두 곳에 생긴다.
        연비는 차량 상세에서 본다. 제대로 된 해법은 백로그의 홈 통계 요약 API 다.
      → 버킷 병합 로직(정비·주유를 같은 달에 합치는 부분)은 **node 로 직접 돌려 확인**했다 —
        Map 을 두 번 갱신하는 자리라 한쪽이 다른 쪽을 덮기 쉽다.
      → **후속: `최근 정비` → `최근 활동` 으로 바꿔 주유까지 담는다.** 같은 이유다 — 홈이
        유지비 전체를 말하게 됐는데 목록만 정비였다.
      → 타입은 `kind` 를 판별 필드로 둔 **유니온**이다(`RecentActivity`). 정비의 `serviceDate` 와
        주유의 `fueledAt` 을 `date` 한 이름으로 모아, 정렬할 때 어느 필드를 봐야 하는지 따지지
        않게 했다. 화면에서 `kind` 를 확인하면 `record` 타입이 좁혀지므로 **두 종류를 다 다루지
        않으면 타입 검사가 잡는다** — 실제로 렌더링을 고칠 때 네 군데를 잡아 줬다.
      → **같은 날짜의 동점 기준에 id 를 쓰면 안 된다.** 정비와 주유는 테이블이 달라 id 가 서로
        무관하다(주유 3번이 정비 3번보다 나중이라는 보장이 없다). 종류가 다르면 정비를 먼저 두고,
        **같은 종류끼리만** id 내림차순으로 비교한다. React `key` 도 같은 이유로
        `` `${kind}-${id}` `` 다 — id 가 겹친다.
      → 정렬 순서도 node 로 돌려 확인했다(최신 날짜 먼저 · 같은 날은 정비 먼저 · 같은 종류는 id 역순).
      → **② 주유 기록을 수정할 때는 차량 주행거리가 안 따라갔다.** 등록에만 있던 규칙이라,
        주행거리를 10,000 으로 잘못 넣고 100,000 으로 고치면 기록만 고쳐지고 차량은 틀린 채로
        남았다. 자리수 오타는 흔하다. `liftVehicleOdometer()` 로 뽑아 등록·수정이 같이 쓴다.
      → **③ 랜딩이 주유·연비를 몰랐다.** 문자열 0건이었다. 하이라이트를 3 → 4칸으로 늘리고
        (`sm:grid-cols-3` 이면 4번째만 홀로 남아서 `sm:grid-cols-2 lg:grid-cols-4` 로),
        정비 종류 설명도 "엔진오일·타이어·브레이크 패드·배터리"에서 "15가지 종류"로 고쳤다.
      → **미리보기가 거짓말을 하고 있었다**: `타이어 · 이력 없음` 줄이 있었는데, 다음 정비 시점을
        일괄 조회로 바꾸면서 **실제 화면은 이력 없는 종류를 아예 안 보여준다.** 평균 연비 줄로 바꿨다.
      → **④⑤ 죽은 코드와 미사용 엔드포인트를 걷어냈다.** 프론트의 `fetchNextService`(일괄 조회로
        바뀌며 호출 0), 백엔드의 `GET .../maintenance-records/{recordId}`,
        `GET .../fuel-records/{recordId}`, `GET .../next-service`(단수).
        엔드포인트 26 → **23개**.
      → **지우면서 테스트를 잃지 않으려고 옮겼다.** `calculateNextService` 에 매달려 있던 종류별
        계산 검증 5개는 `calculateAllNextServices` 로 이관했고(종류 하나만 담긴 목록을 주는
        헬퍼를 만들었다), 리포지토리의 동점 기준 테스트는 `findByVehicleIdOrderByServiceDateDescIdDesc`
        로 겨눴다 — **그 쿼리에도 동점 기준은 그대로 필요하다.**
      → **`MethodArgumentTypeMismatchException` 400 핸들러 테스트가 갈 곳을 잃을 뻔했다.**
        `/next-service` 의 enum 파라미터로 확인하고 있었는데 그 엔드포인트가 사라졌다.
        핸들러 자체는 타입 변환이 필요한 모든 파라미터에 적용되는 범용이라,
        `GET /api/vehicles/abc/maintenance-records`(경로 변수 타입 불일치)로 다시 겨눴다.
      → 테스트 116 → **113개**. 줄어든 건 지운 엔드포인트의 테스트가 옮겨지며 통합된 결과다.
      → 검증: `./gradlew test` 113개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과.

- [x] 정비 종류 5 → 15개 확장 + enum 컬럼을 varchar 로 (2026-09-16)
      → 미션오일(`TRANSMISSION_FLUID`)을 비롯해 10종을 더했다. 엔진·구동 5 / 제동 2 /
        타이어·조향 3 / 소모품 4 / 기타 1.
      → **가장 중요한 건 종류가 아니라 컬럼 타입이다.** `@Enumerated(STRING)` 을 그냥 두면
        Hibernate 6 이 MariaDB 에서 네이티브 `enum('BATTERY','BRAKE_PAD',...)` 컬럼을 만든다.
        운영 DB 를 확인하니 실제로 **값 5개짜리 enum 컬럼**이었다. `ddl-auto: update` 는 컬럼
        타입을 바꿔 주지 않으므로, **코드만 고치면 새 종류를 저장하는 순간 데이터 잘림 오류가
        나고 `odolog_test` 는 create-drop 이라 테스트는 멀쩡히 통과한다** — 이 저장소가 이미
        한 번 크게 밟은 "테스트는 통과하는데 운영만 안 바뀌는" 함정의 재판이다.
      → **`@JdbcTypeCode(SqlTypes.VARCHAR)` 로 varchar(30) 을 강제했다. 2026-09-13 에 한 번
        반려했던 선택인데 근거가 뒤집혔다** — 그때는 "잘 도는 컬럼을 바꾸려고 ALTER 를 하긴
        아깝다"였지만, 이제 ALTER 는 어차피 필요하고 varchar 로 바꿔 두면 **앞으로 종류를 몇 개
        더 넣든 다시는 필요 없다.** 테스트 스키마를 `create` 로 잠깐 돌려 `varchar(30)` 으로
        생성되는 것을 눈으로 확인했다.
      → **운영 DB 는 ALTER 가 한 번 필요하다**(아래 "DB 접속 시 주의" 위의 항목 참고).
        enum → varchar 는 값이 문자열로 저장돼 있어 데이터가 그대로 보존된다.
      → **주기가 한쪽만 있는 종류가 처음 생겼다.** 와이퍼는 고무가 굳는 시간 문제라 개월만,
        타이밍 벨트는 주행거리만 본다. 계산 로직이 원래 둘을 독립적으로 다루고 있어서 코드는
        안 고쳐도 됐지만, 그 사실에 기대는 테스트를 새로 넣었다.
      → **`TIRE` 주기를 10,000km → 50,000km 로 고쳤다.** `TIRE_ROTATION`(10,000km)이 생기면서
        둘이 같은 값이면 앞뒤가 안 맞는다. 타이어 교체는 5만, 위치 교환은 1만이 맞다.
        **나머지 기존 4종의 주기는 건드리지 않았다** — 종류를 늘리는 작업이 기존 계산을 조용히
        바꾸면 안 된다.
      → **종류가 늘면서 `NextServiceCard` 가 터졌다.** 종류마다 요청을 보내는 구조라 5요청이
        **15요청**이 됐다. 그 파일 주석에 "실제로 느려지면 전체 종류 한 번에 API 를 검토한다"고
        적어 둔 그 시점이다 — 백로그에서 꺼내 `GET .../next-services` 를 만들었다. **요청 1번.**
      → 서버 쪽도 종류마다 쿼리를 돌리지 않는다. 차량의 이력을 **한 번** 정렬해 읽고 종류별로
        처음 만나는 줄을 집는다(`EnumMap` + `putIfAbsent`). 한 차량의 이력은 많아야 수백 건이다.
      → **이력이 없는 종류는 응답에서 뺀다.** "다음 정비 시점"은 마지막 정비가 있어야 나오는
        값이라, 15줄 중 13줄이 "이력 없음"이면 카드가 빈칸 목록이 된다. 덤으로 프론트의
        "5개 중 3개만 뜨는 부분 실패" 상태가 원리적으로 사라졌다.
      → 계산 공식을 `toNextService()` 하나로 모았다. 단건과 전체가 각자 계산하면 언젠가 갈린다.
      → 선택 목록은 `<optgroup>` 으로 묶었다. 5개일 때는 평평한 목록으로 충분했지만 15개는
        훑어 찾기 어렵다. 그룹 정의(`SERVICE_TYPE_GROUPS`)가 15종을 빠짐없이 덮는지 스크립트로 확인.
      → **백엔드 enum 과 프론트 `SERVICE_TYPES` 를 순서까지 diff 로 대조했다.** 하나만 어긋나면
        그 종류만 조용히 깨진다.
      → 새 테스트: `ServiceTypeTest` 는 값을 다시 적지 않고 **약속만** 못박는다 —
        "OTHER 를 뺀 모든 종류는 주기가 최소 하나는 있다"(빠뜨리면 다음 시점이 영영 안 뜨는데
        에러도 안 난다), 주기는 양수, 이름은 30자 이하(컬럼 폭).
      → 테스트 106 → **116개**.
      → 검증: `./gradlew test` 116개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과. **브라우저 눈 확인은 사용자 몫이다.**

- [x] 주유 기록 + 연비 — 네 번째 기능, 그리고 `@EnableJpaAuditing` (2026-09-16)
      → **정비보다 자주 쓰는 기능이다.** 정비는 1년에 몇 번이지만 주유는 주 단위다. 더 중요한 건
        이 앱 이름이 오도로그(주행거리 기록)인데 **주행거리가 아무것도 계산하지 않고 있었다는
        것**이다. 주유 기록이 들어오면서 `주행거리 ÷ 리터 = 연비` 가 나온다 — 이미 있던 것과
        새로 들어온 것이 곱해지는 자리다.
      → **먼저 `@EnableJpaAuditing` 부터 했다.** 주유 기록을 만든 뒤에 하면 `@PrePersist` 를
        네 번째로 복사한 다음 다시 지우는 셈이다. Phase 1-B 에 "엔티티 4개째" 조건을 걸어 둔 게
        정확히 이 순간이었다. `common/domain/entity/BaseTimeEntity` 로 8줄 × 3벌이 사라졌다.
      → **`@EnableJpaAuditing` 을 어디 두느냐로 두 번 틀렸다.** ① `OdoLogApplication` 에 붙이니
        **@WebMvcTest 22개가 전부 깨졌다** — 웹 계층만 띄우느라 JPA 가 없는데 이 애노테이션은
        엔티티 메타모델을 요구한다(`IllegalArgumentException: JPA metamodel must not be empty`).
        ② 별도 `@Configuration`(`common/config/jpa/JpaAuditingConfig`)으로 옮기니 이번엔
        **@DataJpaTest 가 깨졌다** — JPA 와 무관한 `@Configuration` 을 전부 걸러내기 때문이다.
        결론: 별도 설정 클래스 + 리포지토리 테스트에 `@Import(JpaAuditingConfig.class)`.
        **빠뜨리면 created_at 이 null 로 INSERT 되어 NOT NULL 위반으로 터지므로 조용히 넘어가지
        않는다** — 그게 이 선택의 안전장치다.
      → `@MappedSuperclass` 는 테이블을 만들지 않고 필드만 자식 테이블에 합친다. 그래서 컬럼
        이름이 그대로고 `ddl-auto: update` 가 아무것도 건드리지 않는다. 실제 DB 로 확인했다
        (`created_at datetime(6) NO`).
      → **연비는 단순법으로 정했다** (사용자 선택). 매 기록마다 `(이번 주행거리 − 직전 주행거리)
        ÷ 이번 주유량`. 만탱크법이 더 정확하지만 "가득 채웠는가" 플래그가 필요하다.
        **대가는 문서에 적어 둔다: 반만 넣은 주유가 섞이면 그 구간만 실제보다 높게 나온다**
        (거리는 그대로인데 리터가 적어서).
      → **liters 는 `BigDecimal(6,2)`.** 32.45L 같은 값이라 `int` 가 안 되고, `double` 은 2진
        부동소수라 리터를 합산해 평균 연비를 내는 순간 오차가 쌓인다.
      → **단가를 저장하지 않고 총액을 저장한다.** 단가 × 리터는 반올림 때문에 영수증 총액과
        어긋난다. 실제로 나간 돈이 총액이므로 그쪽을 저장하고 단가는 나눠서 보여준다.
      → **계산 값(연비·거리·단가)을 DB 에 넣지 않는다.** 직전 기록이 수정·삭제되면 연비가
        달라지는데, 저장해 두면 그때마다 뒤따르는 기록을 전부 다시 써야 한다. 읽을 때 계산하면
        언제나 맞다.
      → **목록 정렬을 서비스가 고정한다**(`odometer DESC, id DESC`). 차량·정비 목록이 `sort` 를
        허용하는 것과 다르다 — 거기서는 정렬이 표시 순서일 뿐이지만 **여기서는 정렬이 곧 계산의
        전제**다. 총액 순으로 정렬하면 옆 행이 직전 주유가 아니게 되어 연비가 조용히 틀린다.
      → **페이지 하나에 쿼리 2번.** 각 행의 연비는 바로 앞 행이 있어야 나오는데, 페이지 안쪽
        행들은 서로가 서로의 짝이다. **마지막 행만 짝이 다음 페이지에 있어서** 그 한 건만 따로
        가져온다. 행마다 직전을 조회하면 N+1 이다.
      → **평균 연비에서 첫 주유량을 뺀다.** 그 연료는 첫 기록 이전 구간을 달린 것이라 우리가 아는
        거리(첫 기록 → 마지막 기록)와 짝이 맞지 않는다. 안 빼면 연비가 낮게 나온다 —
        테스트 데이터로 20.00 이 나와야 할 것이 12.50 이 된다.
      → **구간 연비들의 평균을 내지 않는 이유**도 같다. 30km 구간과 600km 구간이 같은 무게로
        들어가면 짧은 구간의 오차가 전체를 흔든다. 총 거리 ÷ 총 주유량이 맞다.
      → **차량 주행거리 자동 갱신을 넣었다**(백로그에 있던 항목). 주유할 때 계기판을 보고 적는
        값이라 차량의 현재 주행거리보다 크면 그쪽이 더 최신이다. 같은 숫자를 두 번 입력하게 하지
        않는다. 작거나 같으면 건드리지 않는다(과거 기록을 뒤늦게 넣는 경우).
      → **차량 삭제가 주유 기록도 지우게 고쳤다.** 안 하면 FK 제약 위반이다. `VehicleService` 가
        `FuelRecordRepository` 를 주입받는데, 서비스를 주입하면 `FuelRecordService` 가 이미
        `VehicleService` 를 쓰고 있어 **스프링이 잡아내는 진짜 순환 참조**가 된다.
        `MaintenanceRecordRepository` 를 그렇게 둔 것과 같은 이유다.
      → 프론트: `features/fuel/` 신설(`maintenance` 와 같은 모양). 차량 상세 오른쪽에 **연비
        요약 카드**(평균 연비를 히어로 숫자로) + **주유 기록 목록**. 폼에서 금액·리터를 입력하는
        동안 **리터당 단가를 실시간으로 보여준다** — 영수증과 대조해 오타를 그 자리에서 잡는다.
      → 주유를 등록하면 차량을 다시 조회한다. 서버가 주행거리를 올렸을 수 있고, 그래야 위쪽
        히어로 숫자가 맞고 **값이 바뀐 만큼 굴러가는 연출**도 거기서 나온다.
      → `distance`/`efficiency` 는 계산이 성립하지 않으면 **null 이다(0 이 아니다).** 0 으로 두면
        "연비 0km/L" 라는 틀린 값이 화면에 찍힌다. 프론트 타입에도 `number | null` 로 적었다.
      → 테스트 84 → **106개**(+22). 리포지토리 테스트를 특히 신경 썼다 — 파생 쿼리
        (`findTopByVehicleIdAndOdometerLessThanOrderByOdometerDescIdDesc`)는 이름 오타가
        컴파일에 안 걸리고 **앱 기동 때야 터진다.**
      → 검증: `./gradlew test` 106개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과. **브라우저 눈 확인은 사용자 몫이다.**

- [x] 미구현 기능 3개 구현 — 차량 수정 · 비밀번호 변경 · 회원 탈퇴 (2026-09-16)
      → **엔드포인트를 코드로 직접 세어 보고 찾은 구멍이다.** 정비 이력은 필드 5개를 다 고칠 수
        있는데 차량은 `updateOdometer()` 하나뿐이었다. 번호판 오타를 고치려면 삭제 후 재등록인데,
        `delete()` 가 정비 이력을 먼저 지우므로 **오타 하나에 그 차의 기록이 전부 날아갔다.**
      → **① 차량 수정** `PATCH /api/vehicles/{id}`. 엔티티에 필드별 change 메서드 4개
        (`MaintenanceRecord` 와 같은 모양). 주행거리는 안 넣었다 — 감소 금지 규칙이 붙어 있어
        성격이 다르고 전용 엔드포인트가 이미 있다.
      → **함정: 등록 때 쓰던 중복 검사를 그대로 가져오면 안 된다.**
        `existsByOwnerIdAndPlateNumber` 를 무조건 돌리면 번호판을 그대로 두고 제조사만 고쳐도
        **자기 자신이 검색되어 409** 가 난다. `...AndIdNot` 쿼리를 새로 만드는 방법도 있지만
        **"값이 실제로 바뀔 때만 검사한다"** 가 더 단순하고 리포지토리도 안 늘어난다.
        이 함정을 고정하는 테스트를 넣고, 조건을 빼면 **그 테스트만 실패하는 것까지 확인**했다.
      → 번호판을 **가장 먼저** 처리한다. 다른 필드를 먼저 바꾸면 엔티티가 dirty 상태가 되고,
        exists 쿼리 직전에 Hibernate 가 자동 flush 해서 **방금 쓴 값을 내가 다시 조회해 "중복"으로
        판정**할 수 있다.
      → **② 비밀번호 변경** `PATCH /api/users/me/password` → 204.
        **현재 비밀번호를 반드시 확인한다** — 로그인만으로 바꿀 수 있으면 자리를 비운 사이 열린
        세션을 잡은 사람이 계정을 통째로 가져간다. 세션은 유지한다(본인이 바꾼 것이라 쫓아낼 이유가
        없다). 엔티티의 `changePassword()` 는 **이미 암호화된 문자열만** 받는다 — 평문을 받아
        직접 인코딩하면 도메인이 스프링 시큐리티에 묶이고 `new User(...)` 에 빈이 필요해진다.
      → `verifyPassword()` 를 뽑아 변경과 탈퇴가 같은 관문을 쓰게 했다. `changePassword` 안에서
        `findById` 가 두 번 불리지만 **쿼리는 한 번만 나간다** — 같은 트랜잭션의 1차 캐시가 받는다.
      → **③ 회원 탈퇴** `DELETE /api/users/me` → 204. **여기서 설계 결정이 하나 필요했다.**
        탈퇴는 user·vehicle·maintenance 를 모두 건드리는데 `UserService` 에 넣으면
        `user → vehicle` 역방향 의존이 생기고, `common` 에 넣으면 진짜 순환이 된다.
        → **`account` 패키지를 새로 만들었다.** 프론트의 `app/` 과 같은 성격의 조율 층이고,
        **순서만 정하고 실제 삭제는 각 기능에 맡긴다.** 자세한 이유는 위 "의존 방향"에.
      → 탈퇴 컨트롤러는 **URL 이 `/api/users/me` 인데 패키지는 `account`** 다. URL 은 클라이언트가
        보는 주소이고 패키지는 코드의 소속이라 같을 필요가 없다 — `UserController` 에 두면
        `user` 가 `account` 를 알게 되어 순환이다.
      → **DELETE 에 본문을 싣는다.** 비밀번호를 쿼리 파라미터에 넣으면 접근 로그와 브라우저 기록에
        평문으로 남는다. 본문 있는 DELETE 를 꺼리는 중간 장비가 있지만 그쪽이 낫다.
        `api.del()` 이 본문을 안 받고 있어서 인자 하나를 열었다(`request()` 는 원래 지원했다).
      → 탈퇴 후 **세션을 끊는다.** 안 끊으면 없는 사용자 id 를 든 세션이 남아 다음 요청의
        `findById` 가 `IllegalStateException` → 500 이 된다.
      → **작업 중에 만든 버그를 하나 잡았다.** 비밀번호 변경이 401 을 돌려주면 `client.ts` 의
        전역 401 핸들러가 돌아 **사용자 정보를 비우고 /login 으로 쫓아냈다** — 현재 비밀번호를
        한 번 잘못 치면 로그아웃되는 셈이다. `SKIP_UNAUTHORIZED_HANDLER` 는 `includes` 라
        정확히 일치하는 문자열만 찾으므로 `/api/users/me/password` 를 따로 적어야 했다.
        (탈퇴는 경로가 `/api/users/me` 라 우연히 이미 제외돼 있었다. 우연이라 주석에 적어 뒀다.)
      → 프론트: 차량 상세 사이드바에 **차량 정보 카드**(닫혀 있을 땐 값 4개, 열면 폼 — 정비 이력과
        같은 `.form-open` 펼침), 프로필에 **비밀번호 구역**과 **탈퇴 구역**.
        `AuthProvider` 에 `withdraw` 를 추가했다 — "성공하면 로그인 상태가 사라진다"는 화면의
        사정이 아니라 그 동작의 정의라서. 단 **로그아웃과 달리 실패를 삼키지 않는다**:
        탈퇴가 실패했는데 로그아웃된 것처럼 보이면 계정이 지워졌다고 믿게 된다.
      → 비밀번호 확인란은 **서버에 보내지 않는다.** "두 번 같게 쳤는가"는 오타 방지 장치일 뿐이고,
        보내면 비밀번호를 한 번 더 전송하는 셈이다.
      → 테스트 62 → **84개**(+22). 탈퇴는 **순서**(InOrder)와 **"비밀번호가 틀리면 아무것도 안
        지운다"** 를 고정했다 — 트랜잭션 롤백이 막아 주긴 하지만, 순서로 막을 수 있는 것을
        롤백에 기대지 않는다.
      → 검증: `./gradlew test` 84개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과. **브라우저 눈 확인은 사용자 몫이다** — Phase 6 체크리스트에 항목을 더했다.

- [x] 플랫폼을 웹으로 확정 + 파비콘 정리 (2026-09-16)
      → **네이티브 앱(iOS/Android)을 만들지 않기로 했다.** "앱이 접근성이 더 좋지 않나"에서
        출발한 검토였는데, 따져 보니 지금 상황에서는 반대였다. 심사를 통과하지 않은 앱의 도달
        범위는 0 이고 웹은 링크 하나다. 게다가 **네이티브는 배포가 선택이 아니라 필수**라서,
        "배포는 범위 밖"이라고 못박아 둔 완성 정의와 정면으로 어긋난다. 접근성을 위해 앱을
        만든다는 말은 곧 배포를 하겠다는 뜻이고, 그렇다면 **가장 싼 배포는 웹 배포**다.
      → **버려야 하는 것이 컸다.** React Native 에는 CSS 가 없어서 `index.css` 가 통째로
        날아간다: `clamp()` 타입 스케일, View Transitions(테마 전환 원형 번짐),
        `animation-timeline: view()`(랜딩 `.reveal`), `grid-template-rows: 0fr → 1fr`(정비 폼
        — **CSS Grid 자체가 없다**), `gap-px` 격자(홈 통계 타일), `env(safe-area-inset-*)`,
        `cubic-bezier(0.16, 1, 0.3, 1)` 하나로 통일한 곡선. Flutter 면 Dart 까지 새로 배우고,
        Swift+Kotlin 이면 이 작업을 **두 번** 한다.
      → **인증도 갈아엎어야 한다.** 지금은 세션 쿠키(`JSESSIONID` + `credentials: 'include'`
        + `WebConfig` 의 `allowedOrigins`)인데 네이티브에는 origin 이라는 개념이 없다.
        토큰 기반(JWT + 리프레시)으로 바꾸면 `SessionConst`·`LoginUserArgumentResolver`·
        세션 고정 공격 방지(`changeSessionId`)까지 다시 설계다. 배울 가치는 있지만 지금은 아니다.
      → **판단 기준은 "브라우저가 못 하는 일을 하는가"로 잡았다.** 이 앱이 하는 일은 차량 등록,
        주행거리 입력, 정비 이력 CRUD, 다음 정비 시점 표시가 전부다 — 카메라·GPS·백그라운드
        실행·오프라인·블루투스를 하나도 안 쓴다. 게다가 **운전 중에 쓰는 앱이 아니라 정비소
        다녀온 뒤 기록하는 앱**이라 하루 한 번도 안 연다. 네이티브가 주는 건 사실상 홈 화면
        아이콘 하나뿐인데 그건 PWA 로 얻는다.
      → **다시 꺼낼 조건**: OBD-II 블루투스 연동처럼 브라우저가 원리적으로 못 하는 기능이
        생길 때. 그때 경로는 `웹 배포 → Capacitor`(지금 React 코드와 디자인 시스템을 그대로
        감싼다). **순서를 뒤집을 수 없다** — Capacitor 는 `capacitor://localhost` origin 이라
        세션 쿠키가 cross-site 가 되고, `SameSite=None; Secure` 가 필요해 HTTPS 배포가 전제다.
        (계기판 사진 OCR 은 `<input capture>`, 정비 알림은 iOS 16.4+ PWA 웹 푸시로 웹도 된다.)
      → **파비콘 2건을 고쳤다.** ① `rx="8"` 제거 — 디자인 시스템 16번이 "예외를 하나도 두지
        않았다"인데 파비콘만 `public/` 에 있어서 그 작업에서 빠졌다. **`grep rounded src/` 로는
        안 잡히는 자리**다(CSS 클래스가 아니라 SVG 속성이라서). `viewBox` 32 에 `rx` 8 이면
        가로폭의 1/4 을 깎고 있었다. 반경을 줄이는 대안은 "한 군데만 둥글면 장식으로 보인다"는
        16번의 논지에 답하지 않아서 안 골랐다. 덤으로 iOS·안드로이드 런처는 아이콘을 자기
        모양으로 다시 깎으므로, 각진 원본을 주는 편이 이중으로 둥글어지지 않는다.
      → ② 배경 `#0D0D0D` → `#17171a`. 이건 **네 번째 중복이었다.** 2026-09-13 에 다크 바닥을
        올리며 `index.css`·`index.html`·`ThemeProvider.tsx` 셋은 고쳤는데 파비콘만 남았고,
        `#0D0D0D` 는 애초에 셋 중 어느 것도 아닌 제4의 값이었다. **정적 파일이라 테마를 따라갈
        수 없으므로** 다크 바닥으로 고정한다(밝은 탭 배경에서도 어두운 판이 잘 보인다).
      → 바늘 `0.96`(=`--strong`)과 눈금호 `0.28`(=`--ring`)은 토큰과 정확히 같은 값이라
        안 건드렸다. 토큰을 보고 만든 파일인데 배경만 뒤의 변경을 못 따라간 것이었다.
      → 문서도 함께 최신화: 구조 트리의 `public/favicon.svg`("아직 Vite 기본 로고" — 틀린 지
        오래됐다)와 `VehicleListPage`("카드 격자" → 실제로는 9/14 부터 괘선 행).
      → 검증: `npm run build`(`tsc -b` 포함) 통과, `dist/favicon.svg` 복사 확인.
        CSS 38.78 kB 로 변화 없음 — 파비콘은 번들에 안 들어가는 정적 파일이다.
        **브라우저 탭에서 눈으로 보는 건 남아 있다**(캐시된 파비콘이 있으면 하드 리로드 필요).

- [x] 모바일 최적화 (2026-09-14)
      → 편집 레이아웃 재설계를 데스크톱 기준으로 했던 것을 375px 기준으로 다시 맞췄다.
        기준은 디자인 시스템 11-1 에 정리했다.
      → **헤더가 실제로 넘치고 있었다.** 375px 에서 내용 폭은 335px 인데 로고 92 + 토글 92 +
        닉네임 96 + 로그아웃 80 + 간격이 386px 다. 닉네임 버튼만 찌부러져 말줄임이 된다.
        화면 모드 토글을 `sm` 미만에서 숨겨 286px 로 맞췄다. 토글은 프로필에도 있다.
      → **정비 이력 행은 한 줄에 안 들어간다.** 수치(85) + 버튼 둘(98) + 간격(48)을 빼면
        왼쪽에 104px 가 남는데, 종류와 날짜만 132px 다. `flex-wrap` + `basis-full` 로
        좁은 화면에서만 두 줄로 접고 `sm` 부터 원래 3열로 돌아온다.
      → **터치 판정 영역을 `::after` 로 넓혔다**(44px, `@media (pointer: coarse)`).
        버튼 자체를 키우면 목록 행의 리듬이 무너진다. 정비 이력의 수정·삭제 버튼은
        28px 라 이게 없으면 누르기 어렵다. 호버로만 진해지던 것도 터치에서는 항상 진하게.
      → **노치 대응을 빠뜨리고 있었다.** `viewport-fit=cover` 를 켜 두고 `env(safe-area-inset-*)`
        를 안 줘서 가로 모드에서 내용이 깎인다. 바깥 래퍼와 본문 아래에 넣었다.
      → 크기를 `clamp()` 로 바꿨다. 히어로 숫자는 최소 52 → 40px(7자리가 넘으면 넘쳤다),
        통계 타일은 고정 44px → 32~44px.
      → 차트는 축 라벨 자리(52 → 40px)와 막대 간격을 줄이고, 가로축 숫자를 홀수 칸만 남겼다.
        칸 하나가 20px 남짓이라 12개를 다 적으면 숫자가 서로 붙는다.
      → 여백을 전반적으로 줄였다: 본문 상하 `pt-10/pb-28`, 좌우 `px-5`, 헤더 높이 72 → 56px,
        카드 안쪽 28 → 20px, 빈 상태 `py-32` → `py-20`. `sm` 부터는 전부 원래 값이다.
      → 폼의 2단 배치는 이미 전부 `sm:grid-cols-2` 라 저절로 쌓인다. 손댈 것이 없었다.
      → **후속(같은 날): 프로필 '화면' 구역에서 토글이 아래로 밀려 내려가는 문제.**
        `flex-wrap` + `justify-between` 인데 왼쪽 글 덩어리에 `min-w-0` 이 없어서, 글이
        줄어들지 못하고 토글을 밀어냈다. 그래서 남는 폭이 **설명 문구 길이에 좌우됐고**
        문구가 가장 긴 `system` 일 때만 증상이 나타났다.
        좁은 화면에서는 아예 세로로 쌓고(`sm:flex-row`), 글에 `min-w-0`, 토글에 `shrink-0`
        을 줬다. 문구도 한 단계 줄였다("기기 설정을 따릅니다. 지금은 다크입니다.").
        **`flex-wrap` 은 줄바꿈을 허용할 뿐 어디서 끊길지는 정해 주지 않는다** — 내용 길이가
        바뀌는 자리에서는 끊길 지점을 `sm:` 으로 직접 정해야 한다.
      → 검증: `tsc -b` / `oxlint`(기존 shadcn 경고 1건) / `vite build` 통과 +
        빌드 CSS 에 `pointer:coarse` 블록과 `env(safe-area-inset-*)` 가 들어갔는지 확인 +
        반응형 접두사 없는 `grid-cols` 와 고정 `min-w` 가 남아 있지 않은지 grep.
        **실제 기기 확인은 사용자 몫이다.**

- [x] 모션을 다시 잡았다 — 일괄 등장 연출을 걷어냈다 (2026-09-14)
      → **바로 앞 작업(등장 연출)을 상당 부분 되돌린 것이다.** 사용자 지적: "AI 티 안 나고
        사람이 하나하나 코딩한 것처럼". 그 지적이 맞았다 — `.stagger` 를 만들어 **모든
        페이지의 모든 블록**을 같은 거리(10px)·같은 시간(0.75s)으로 띄우고, 히어로 숫자마다
        카운트업을 붙였다. 규칙 하나를 화면 전체에 일괄 적용한 것이라 **개별 요소를 들여다본
        흔적이 남지 않는다.** 그게 "어디서 본 듯한" 인상의 정체다.
      → 걷어낸 것: `.stagger` 유틸과 `enter-up` 키프레임, `Page`·차량 목록의 일괄 등장,
        차트 총비용의 카운트업, 죽은 토큰 `--animate-rise`. 페이지 전환 페이드는 0.5s → 0.18s.
      → **남긴 기준: 움직일 이유가 있을 때만 움직인다.** 지금 연출은 전부 "방금 무슨 일이
        일어났는지" 또는 "여기서 무엇을 할 수 있는지"를 말한다. 시간도 성격에 따라 다르다 —
        오류는 0.24s/4px 로 가장 짧고 가깝게(급한 소식), 폼 펼치기는 0.36s, 괘선은 0.7s.
      → **카운트업을 "값이 바뀐 순간에만" 돌게 바꿨다.** 0 에서 굴러 오르는 건 처음 한 번만
        근사하고 두 번째부터는 값을 읽기까지 기다리는 시간이 된다. 이제 주행거리를
        50,000 → 52,000 으로 갱신할 때만 그 구간을 굴러가고, **그 움직임이 "2,000km 올랐다"는
        뜻을 나른다.** 지속 시간도 변화 폭에 비례시켰다(0.45~1.4s) — 10km 와 20,000km 가
        같은 시간이면 작은 변화는 굼뜨고 큰 변화는 순식간에 지나간다.
      → **새로 넣은 것은 셋뿐이고 셋 다 상호작용에 붙어 있다.**
        ① 차량 목록 행 왼쪽의 1px 표식이 세로로 그어진다(호버·키보드 포커스). 배경만 옅게
           바뀌면 어느 행에 있는지 훑어봐야 안다. 가로로 늘리면 글자를 밀어내는 것처럼 보여서
           세로로 세웠고, 목록의 가로 괘선과 같은 1px 이라 **있던 선이 세로로 서는 것**으로 읽힌다.
        ② 정비 폼이 자리를 밀어내며 펼쳐진다(`grid-template-rows: 0fr → 1fr`). `height` 는
           내용 높이를 JS 로 재야 하고 창 크기가 바뀔 때마다 다시 재야 한다. 안쪽 내용은
           0.12s 늦게 들어온다 — 동시에 나타나면 글자가 찌그러지며 늘어나 보인다.
           **닫을 때는 연출하지 않는다** — 이미 사용자가 결정한 일이고, 사라지는 것을 붙잡으려면
           상태를 하나 더 들고 있어야 한다.
        ③ 오류·안내가 짧고 가깝게(0.24s/4px) 나타난다. **움직임의 성격이 곧 "급한 소식"이라고
           말한다** — 다른 연출처럼 길게 감속시키면 그만큼 늦게 읽힌다.
      → 유지한 것: 괘선 그리기(화면당 하나뿐인 자리 잡기 연출, 0.9s → 0.7s 로 단축),
        차트 막대(데이터가 그려지는 동작), 테마 전환 View Transition.
      → oxlint 의 `react(set-state-in-effect)` 를 한 번 밟았다. 감소 모션 분기에서 effect 안에
        동기 `setState` 를 부르고 있었다 — 규칙을 끄지 않고 첫 프레임 안으로 옮겼다.
      → 검증: `tsc -b` / `oxlint`(기존 shadcn 경고 1건) / `vite build` 통과 +
        빌드 CSS 에서 `.stagger` 가 0건, `form-open`·`alert-in`·`draw-x` 가 생성됐는지 확인.
        **브라우저 눈 확인은 사용자 몫이다.**

- [x] 편집 레이아웃으로 전면 재조정 — 타입 스케일 + 괘선 (2026-09-14)
      → 사용자 요청: "있어보이게, 디자인 상 탈 정도로 고급스럽게". 방향은 두 가지를 물어
        정했다: **산세리프 유지 + 대비 극단화**, **괘선 중심 편집 레이아웃**.
        (세리프 디스플레이와 웹폰트 도입은 후보였으나 선택되지 않았다 — 규칙 6 유지.)
      → **타입 스케일을 토큰으로 만들었다**(규칙 6). 전에는 화면마다 `text-[2rem]` 처럼
        크기를 직접 적었는데, 크기만 적으면 자간·행간이 따라오지 않는다. 이제 크기·행간·
        자간·굵기가 한 벌로 묶여 있다: eyebrow / title / display / figure / section / lede.
        제목과 히어로 숫자는 `clamp()` 라 분기점을 빠뜨릴 수가 없다.
      → **위계를 3.5배로 벌렸다.** 제목 32→52px(clamp), 히어로 숫자 52→80px.
        글꼴이 하나뿐이라 크기·굵기·자간 말고는 위계를 만들 수단이 없고, 어중간하게 벌리면
        "조금 큰 글씨"로 보인다. **히어로 숫자는 굵기 300** — 크기가 이미 강조라 굵기까지
        올리면 뭉쳐 보이고, 얇게 두면 같은 크기에서도 훨씬 정밀해 보인다.
      → **유리를 전부 걷어냈다**(규칙 5 뒤집음). `backdrop-blur-[20px]` 4곳 + 헤더의
        `backdrop-saturate-150` + 상단 방사형 광채(`--glow`). 흐림은 템플릿 UI 의 서명 같은
        것이라 어디에나 있고, **뒤가 비치는 면은 그 위 글자의 배경을 불확실하게 만든다.**
        광채는 유리를 받쳐 주던 것이라 유리가 없으니 뿌연 얼룩으로만 남았다.
      → **차량 목록을 카드 격자 → 괘선 행으로 바꿨다**(규칙 11 수정). 카드를 하나씩 씌우면
        차량 수만큼 상자가 늘어 화면이 상자 목록이 된다. 행으로 깔면 번호판은 번호판끼리,
        주행거리는 주행거리끼리 세로로 정렬되어 **여러 대를 훑어 비교할 수 있다** —
        목록에서 실제로 하는 일이 그것이다. 표 형태의 행은 700px 제한의 예외로 적어 뒀다.
      → **`Page` 머리말 아래에 기준선(1px)을 그었다.** 여백만으로 떼어 놓으면 표제와 본문의
        경계가 흐릿해 화면이 한 덩어리로 흐른다. 선을 하나 그으면 위는 표제, 아래는 내용이
        한눈에 잡힌다. `Section` 도 구역마다 위에 선을 얹어 목차처럼 보이게 했고,
        `AuthLayout` 은 두 단 사이에 **세로 괘선**을 세웠다.
      → **한글에는 eyebrow 처리가 안 통한다는 걸 확인했다.** `Section` 제목을 11px +
        자간 0.2em 으로 바꿨다가 되돌렸다 — 한글은 대문자가 없어서 자간만 벌어진 2글자
        ("계정")가 그냥 흩어져 보인다. 라틴 분류 라벨(`GARAGE`·`ODOMETER`)에만 쓴다.
      → 여백을 전반적으로 키웠다: 본문 상하 `pt-16/pb-40`, 카드 안쪽 24→28px,
        목록 행 14→20px, 머리말→본문 `gap-12/14`.
      → **괘선을 세울 때는 좌우 폭을 맞춰야 한다.** `AuthLayout` 에서 열 간격(96px)에
        오른쪽 여백(96px)이 더해져 글과 폼이 192px 벌어졌다. 선을 기준으로 좌우가 같아야
        선이 가운데 있는 것으로 읽힌다 — 둘 다 64px 로 맞췄다.
      → 검증: `tsc -b` / `oxlint`(기존 shadcn 경고 1건) / `vite build` 통과 +
        빌드 CSS 에서 새 타입 유틸 6종이 자간·굵기까지 생성됐는지, `backdrop-blur` 와
        `glow` 가 0건인지 직접 확인. CSS 36.53 → 34.97 kB.
        **브라우저 눈 확인은 사용자 몫이다.**

- [x] 모서리를 전부 각지게 — 반경 0 (2026-09-13)
      → 사용자 요청: "둥글지 않게, 직선의 미를 살려서 AI 티 나지 않게".
      → **반경 척도를 지우지 않고 전부 0 으로 만들었다**(`--radius-sm ~ --radius-3xl`).
        지워 버리면 `shadcn add` 로 새로 받는 컴포넌트의 `rounded-md` 같은 유틸이
        Tailwind 기본값으로 되살아난다. 0 으로 두면 그것들까지 자동으로 각지게 나온다.
      → **컴포넌트의 `rounded-*` 32곳을 전부 걷어냈다.** 토큰만 0 으로 두고 클래스를 남기면
        `rounded-2xl` 인데 안 둥근 상태가 되어 **클래스 이름이 거짓말을 한다.**
        지금 `frontend/src` 에 `rounded` 문자열은 0건이고, 빌드된 CSS 에 남은 `border-radius`
        는 Tailwind 가 폼 요소에 거는 초기화 두 줄(`border-radius:0`)뿐이다.
      → **예외를 하나도 두지 않았다**: 버튼(알약 → 사각), 입력창, 카드, 차트 막대,
        세그먼트 컨트롤의 미끄러지는 블록, 로딩 점(원 → 사각)까지.
        한 군데만 둥글게 남기면 그것이 장식으로 보인다 — 전에 알약 버튼이 정확히 그랬다.
      → **주석 세 곳이 코드와 반대를 말하고 있어서 같이 고쳤다.** `button.tsx` 는
        "알약 형태를 기본으로 삼는다. 사각에 가까운 버튼은 웹 부트스트랩 인상을 남긴다"고
        적혀 있었고, `index.css` 의 `grow-up` 은 `clip-path` 를 쓰는 이유를 "막대 위쪽 4px
        라운드가 눌려 찌그러진다"로 들고 있었다. **후자는 이유가 사라졌지만 코드는 그대로 뒀다**
        — transform 은 테두리와 포커스 링까지 같이 누르는 반면 `clip-path` 는 막대 자체를
        건드리지 않으므로, 다른 이유로 여전히 옳다. 이유를 새로 적었다.
      → 디자인 시스템에 16번 규칙을 추가하고, 차트 규칙(8번)의
        "데이터가 끝나는 쪽만 4px 둥글게"를 "네 모서리 전부 각지게"로 바꿨다.
        그대로 뒀으면 다음에 차트를 손볼 때 규칙을 따라 다시 둥글려졌을 것이다.
      → **컴포넌트 로직은 한 줄도 안 바뀌었다.** 클래스 문자열만 짧아졌다.
      → 검증: `tsc -b` / `oxlint`(기존 shadcn 경고 1건) / `vite build` 통과 +
        빌드 CSS 의 `border-radius` 를 직접 세어 확인. CSS 36.94 → 36.53 kB.
        **브라우저 눈 확인은 사용자 몫이다.**

- [x] 다크 모드가 너무 어두운 문제 — 바닥을 올렸다 (2026-09-13)
      → **원인은 바닥이 순수 검정(`#000000`)이었던 것.** 그 위의 카드가 흰색 3%라 실제 색이
        `#080808` 이었다. 배경과 카드가 사실상 같은 색이라 층이 안 생기고 화면 전체가
        한 덩어리로 가라앉아 보였다. **라이트에서 배경을 `#FFFFFF` 가 아니라 `#F4F4F6` 으로
        둔 것과 정확히 같은 문제**인데 다크에만 그 처리가 빠져 있었다.
      → `--background: #000000 → #17171A`. 바닥을 올리면 그 위의 면들이 상대적으로 어두워지므로
        면 α 도 함께 올렸다: card 0.03→0.05, card-hover 0.055→0.075, fill 0.04→0.06,
        fill-hover 0.065→0.085, wash 0.06→0.07, sunken 0.02→0.03.
      → **글자 4단계와 선은 건드리지 않았다.** 바닥이 밝아지면 대비가 내려가므로 계산으로
        확인했다(눈대중 금지 — 라이트/다크 도입 때 이미 이걸로 버그 2건을 냈다).
        배경 위 16.55 / 9.63 / 5.26, 카드 위 14.52 / 8.72 / 4.98 — **앞의 세 단계는
        카드 위에서도 AA(4.5:1)를 넘는다.** `faint` 는 3.2 안팎으로 여전히 장식 전용.
        빨강(`--destructive`)도 카드 위 5.76 로 확인했다.
      → 후보를 `#0E0E10` / `#131316` / `#171719` / `#1B1B1E` 로 놓고 전부 계산해 비교했다.
        가장 밝은 후보는 카드 위 보조 텍스트가 4.86 까지 내려가 여유가 거의 없어서 뺐다.
      → **노이즈를 0.035 → 0.025 로 낮췄다.** 넓은 검정 면의 색 띠(banding)를 깨려고 넣은
        것인데, 바닥이 밝아지면 깰 띠가 줄어드는 반면 노이즈 자체는 더 잘 보인다.
      → **다크 배경색이 세 곳에 중복이라는 걸 이번에 확인했다**: `index.css`의 `--background`,
        `index.html` 인라인 스크립트의 `theme-color`, `ThemeProvider.tsx`. 셋을 다 고쳤다 —
        `index.css` 만 고쳤으면 **모바일 주소창만 검정으로 남는다.** `'odolog-theme'` 문자열이
        HTML 과 `ThemeContext.ts` 양쪽에 중복인 것과 같은 종류의 함정이라 문서에 적어 뒀다.
      → 컴포넌트는 한 줄도 안 고쳤다. 값이 전부 토큰에 있어서 가능했던 것이고,
        **디자인 시스템 0번("컴포넌트에 색 값을 적지 않는다")이 실제로 값을 한 셈이다.**
      → 검증: `vite build` 통과 + 빌드된 CSS 의 `:root.dark` 블록에 새 값이 들어갔는지 직접 확인.
        라이트 블록은 그대로. **브라우저 눈 확인은 사용자 몫이다.**

- [x] 전체 점검에서 찾은 결함 12건 수정 (2026-09-13)
      → README 의 "남은 작업" 1~4번을 전부 비웠다. 항목별 표는 `README.md` 에 있고,
        여기에는 **왜 그렇게 고쳤는지**만 남긴다.
      → **요청 DTO 에서 `int` 는 "안 보냄"을 표현하지 못한다.** 필드가 없으면 Jackson 이
        조용히 0 을 채우고 `@PositiveOrZero` 가 그 0 을 통과시킨다. 정비 이력 등록의
        `cost`/`serviceOdometer`, 주행거리 갱신의 `odometer` 세 곳이 그랬다 →
        `@NotNull Integer`. **엔티티는 그대로 `int` 가 맞다** — 규칙 8의 "없음이 존재하는 값만
        래퍼"는 저장된 값에 대한 이야기이고, 요청 DTO 는 "클라이언트가 보낸 JSON"이라
        "필드를 안 보냄"이라는 상태가 실제로 존재한다. `MaintenanceRecordUpdateRequest` 는
        이미 같은 이유로 `Integer` 였는데 등록 DTO 에만 그 논리가 빠져 있었다.
      → **부분 수정 DTO 에 `@NotBlank` 를 쓰면 안 된다.** null 까지 막아서 "보낸 필드만 변경"이
        깨진다. `@Size(min = 1)` + `@Pattern(".*\S.*")` 은 둘 다 null 을 통과시키므로
        부분 수정과 짝이 맞는다.
      → **3-2 는 추측으로 못 고쳤다.** JPA 를 거치면 `DuplicateKeyException` 이 아니라
        `DataIntegrityViolationException` 이 올라온다(탐색용 테스트로 직접 던져 보고 확인).
        그런데 이 예외는 NOT NULL 위반 같은 우리 쪽 버그도 함께 타고 오므로 전부 409 로
        감싸면 500 이어야 할 것이 조용히 4xx 로 나간다 — cause 가 Hibernate
        `ConstraintViolationException` 이고 `kind == UNIQUE` 일 때만 409, 아니면 다시 던져
        500 으로 보낸다. 그 "예외의 모양"에 기대는 코드라 `UserRepositoryTest` 에
        가정을 못박는 테스트를 뒀다.
      → **`sort` 동점 기준은 정비 이력에만 붙였다**(`{"serviceDate", "id"}`).
        차량 목록의 `createdAt` 은 `datetime(6)` 이라 같은 마이크로초에 두 대를 등록해야
        동점이 되지만, 정비 이력의 `serviceDate` 는 날짜라 **같은 날 두 건이면 일상적으로**
        동점이다. 같은 종류의 위험이라도 발생 조건이 다르면 대응도 달라진다.
      → **로그아웃 실패는 `AuthProvider` 가 책임진다.** "서버 요청이 실패해도 클라이언트
        상태는 비운다"는 로그아웃이라는 동작의 정의이지 헤더라는 화면의 사정이 아니다.
        `Header` 에 try/catch 를 넣으면 로그아웃 버튼이 하나 더 생기는 날 같은 코드를 또 쓴다.
      → **`@Column(length = 20)` 이 거짓말이었다.** Hibernate 6 이 MariaDB 에서
        `@Enumerated(STRING)` 을 varchar 가 아니라 네이티브
        `enum('BATTERY','BRAKE_PAD','ENGINE_OIL','OTHER','TIRE')` 로 만든다(테스트 로그의
        실제 DDL 로 확인). `@JdbcTypeCode(SqlTypes.VARCHAR)` 로 varchar 를 강제할 수도 있지만
        잘 도는 컬럼 타입을 바꾸느라 운영 DB 에 `alter table` 이 필요해지므로 숫자만 지웠다.
      → **정비 이력 삭제 후 페이지 이동은 `page` 만 바꾼다.** `useAsyncData` 가 `load` 의
        정체성 변화로 재조회하므로 `reload()` 를 같이 부르면 요청이 두 번 나간다.
      → 테스트 56 → 62개. 새 테스트 6개는 **고치기 전 코드에 돌려 실제로 실패하는 것까지
        확인**했다(연식·전화번호 때와 같은 절차).
      → 검증: `./gradlew test` 62개 통과, 프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건) /
        `vite build` 통과. **브라우저 눈 확인은 여전히 안 했다.**

- [x] 디렉토리 전면 세분화 — 계층 아래 "성격" 한 겹 추가 (2026-09-13)
      → **사용자 요청으로 기준 자체를 바꿨다.** 전에는 "폴더는 파일이 2개가 될 때 만든다"였고,
        2026-09-09 점검에서 그 기준으로 "백엔드 세분화는 이미 끝났다"고 결론 냈었다.
        새 기준은 **"폴더는 파일의 성격을 드러낼 때 만든다"** — 개수로 정하지 않는다.
      → 백엔드: `domain/entity` · `domain/type`(enum) · `repository/jpa` ·
        `dto/request/<유스케이스>` · `dto/response/<유스케이스>` · `service/application` ·
        `controller/rest`, `common` 은 `auth/{annotation,resolver,constant}` ·
        `config/{web,openapi}` · `dto/response/{error,page}` · `exception/{type,handler}`.
        파일 36개 이동, `package` 선언과 import 를 전부 갱신.
      → 프론트: 화면은 화면 이름 폴더 안으로(`pages/login/LoginPage.tsx`), `api` 는
        `endpoints/`·`types/` 로, `shared/ui` 는 `base`(shadcn) / `form` / `layout` /
        `feedback` / `nav` / `brand` 로. 파일 44개 이동.
      → **`shared/ui/base/` 가 이번 세분화에서 가장 값이 있는 자리다.** 전에는 shadcn이 복사해
        넣은 파일 5개와 우리가 쓴 파일 7개가 한 폴더(12개)에 섞여 있고 **문서로만** 구분돼 있었다.
        이제 소유자가 폴더로 갈려 있다. `components.json` 의 `aliases.ui` 도
        `@/shared/ui/base` 로 함께 바꿨다 — **안 바꾸면 다음 `shadcn add` 가 base/ 밖에 파일을
        만들어 원래대로 섞인다.**
      → **컴파일이 세 곳에서 깨졌고, 그게 세분화의 소득이었다.** 같은 패키지라 import 없이 쓰던
        것들이 갈라지면서 드러났다: `LoginUserArgumentResolver`→`LoginUser`/`SessionConst`,
        `GlobalExceptionHandler`→예외 4개, `MaintenanceRecord`→`ServiceType`.
        이제 파일 맨 위만 봐도 무엇에 기대는지 보인다.
      → **깊이를 더 내려갈 수 없는 자리 2곳을 확인했다.** `OdoLogApplication`(컴포넌트 스캔 기점 —
        옮기면 `scanBasePackages`/`@EntityScan`/`@EnableJpaRepositories` 를 손으로 지정해야 함)과
        `main.tsx`(`index.html` 이 경로를 문자열로 가리킴). 둘 다 제자리에 뒀다.
      → **대가는 문서에 그대로 적었다**: 파일 보유 폴더 45개 → 81개, 그중 파일 1개짜리
        24개 → 74개. `exception/type/`(4개)이나 `ui/base/`(소유자 분리)처럼 값을 치를 만한 곳과
        `service/application/`(형제가 생길 기약 없음)처럼 순수 비용인 곳이 섞여 있다.
      → 이동은 전부 `git mv`(히스토리 보존).
      → 검증: 백엔드 `./gradlew test` **56개 전부 통과**(테스트 클래스 10개도 새 패키지에서 실행됨),
        프론트 `tsc -b` / `oxlint`(기존 shadcn 경고 1건만) / `vite build` 통과.
        옛 import 잔재 0건을 grep 으로 확인. **브라우저 눈 확인은 하지 않았다** — 구조 변경이라
        화면은 그대로여야 한다.

- [x] 홈 통계에 그래프 추가 (2026-09-13)
      → **"화려하게"를 색으로 풀지 않았다.** 이 프로젝트의 규칙이 "Accent 하나, 쨍한 색 금지"이고,
        차트에서 색을 늘리는 건 보통 정보가 아니라 노이즈를 늘리는 일이다. 두 차트 모두
        **계열이 하나**라 색이 구분할 것이 애초에 없다 — 그래서 범례도 없다.
        화려함은 **크기(히어로 숫자 48px+) · 밀도 · 자라나는 움직임**이 만든다.
      → 차트 2개: **월별 정비 비용**(세로 막대 12칸, 히어로 숫자 + 말풍선 + 표) /
        **정비 종류별 비용**(가로 막대). 차트 라이브러리를 넣지 않고 HTML·CSS 로만 그렸다 —
        모노톤 토큰을 그대로 쓰므로 테마를 바꾸면 차트도 같이 바뀌고, 번들도 안 늘어난다.
      → 규칙은 위 "디자인 시스템" 8번에 정리했다. 특히:
        **값을 모든 막대에 적지 않는다**(가장 높은 달 하나만), **점선 눈금을 쓰지 않는다**,
        **순서 없는 항목에 "클수록 진하게"를 쓰지 않는다**(막대 길이가 이미 말한 것을
        색으로 또 말하는 꼴).
      → **말풍선만으로 값을 가두지 않았다.** 마우스를 올려야만 보이는 값은 키보드·스크린리더
        사용자에게 없는 것이나 마찬가지다. 막대 칸에 `tabIndex`를 줘 Tab 으로도 같은 값이 뜨고,
        `<details>` 표를 같이 뒀다. 판정 영역은 막대(24px)가 아니라 **칸 전체**다.
      → **막대 자라는 연출은 `clip-path`.** 처음에 떠올린 `transform: scaleY()` 는 위쪽
        4px 라운드까지 같이 눌러서 자라는 동안 모양이 찌그러진다.
      → **큰 숫자에서 `tabular-nums` 를 걷어냈다(규칙 7 정정).** 모든 글자를 `0` 너비로 맞추는
        설정이라 맞출 상대가 없는 큰 글씨에서는 `1` 주변이 휑하게 벌어진다.
        히어로 숫자·통계 타일·차량 상세 주행거리 세 곳을 비례 숫자로 바꿨다. 표와 축은 그대로.
      → **축 눈금을 `niceMax()` 로 1·2·5 × 10ⁿ 에 맞춘다.** `1,873` 로 끝나는 축은 못 읽는다.
        `873 → 1,000` / `45,000 → 50,000` / `120,000 → 200,000` 을 직접 돌려 확인했다.
      → **빈 달을 빼지 않고 12칸을 항상 채운다.** 기록 있는 달만 모으면 가로축이 등간격이
        아니게 되어, 띄엄띄엄 정비한 것이 꾸준히 정비한 것처럼 보인다.
      → `lastMonths()` 는 `new Date(년, 월 - i, 1)` 에 맡긴다. 월이 음수면 연도를 알아서
        넘겨 준다. **연말 경계(2026-01 기준)와 31일(2026-03-31) 기준으로 직접 돌려 확인**했다 —
        날짜를 1일로 고정해서 31일에도 달이 밀리지 않는다.
      → 검증: `tsc -b` / `oxlint` / `vite build` 통과 + 축 계산·월 생성 로직을 node 로 직접
        실행해 확인 + 빌드 CSS에 키프레임·`group-focus-visible` 생성 확인.
        **브라우저 눈 확인은 아직 안 했다.**

- [x] 홈(`/`)에 차량 통계 추가 (2026-09-13)
      → **한 주소가 세 얼굴을 갖는다**: 비로그인 → 소개 화면(그대로), 로그인+0대 → 등록 권유,
        로그인+차량 있음 → 통계. 갈림은 `HomePage` 안에 있고 `LandingPage` 는 이제
        비로그인 전용이라 로그인 여부를 따지는 코드가 통째로 빠졌다.
      → 통계 4칸(차량 수 · 총 주행거리 · 정비 건수 · 총 비용) + 차량별 요약 + 최근 정비 5건.
      → **정확한 값과 그렇지 않은 값을 구분해서 표시한다.** 건수는 서버가 준 `totalElements`
        라서 언제나 맞지만, **주행거리·비용 합계는 받아 온 행을 프론트가 직접 더한 값**이다.
        차량 100 / 이력 200건 상한을 넘으면 일부만 더해지므로, `sumsComplete` 로 그 사실을
        감지해 해당 타일에만 "일부 기록만 합산됨"을 붙인다. 틀릴 수 있는 값을 맞는 값처럼
        보여주면 나중에 숫자가 안 맞을 때 어디가 문제인지 알 수 없다.
        제대로 된 해법은 백엔드 요약 API — 백로그에 올렸다.
      → **"임박한 정비"는 넣지 않았다.** 권장 주기(`recommendedIntervalKm/Months`)가 백엔드
        `ServiceType` enum 안에만 있고 프론트 타입에는 없다. 프론트에 숫자를 베껴 오면
        비즈니스 규칙이 두 곳에 생긴다(코드 설계 원칙 2번 위배). 차량마다 next-service 를
        5번씩 부르는 방법도 있지만 차량 3대면 요청이 15번이라 홈 화면에 쓸 수 없다.
      → 요청 수는 **1 + 차량 수**(차량 목록 1번, 차량마다 이력 1번씩 `Promise.all` 동시에).
      → 조회·계산을 `homeStats.ts` 로 떼어 냈다. 순수 계산이라 JSX와 섞이면 둘 다 읽기 나쁘다.
      → 최근 정비 정렬의 동점 기준은 **id 내림차순** — 백엔드가
        `findTopBy...OrderByServiceDateDescIdDesc` 에서 쓰는 기준과 같게 맞췄다.
      → `lastServiceDate` 는 이력 목록의 첫 줄에서 가져온다. **컨트롤러의
        `@PageableDefault(sort = "serviceDate DESC")` 에 기대는 코드**라 그게 바뀌면 같이 틀어진다.
      → 덤: 헤더에 `내 차량` 링크가 없던 문제가 저절로 풀렸다. 로고 → 홈(통계) → `내 차량` 버튼.
      → 검증: `tsc -b` / `oxlint` / `vite build` 통과. **브라우저 눈 확인은 아직 안 했다.**

- [x] 화면 레이아웃 일관성 정리 (2026-09-11)
      → 화면 7장을 훑어보니 **페이지 루트 간격이 `gap-8`/`gap-10`/`gap-12` 셋으로 갈려 있었고**,
        **차량 상세는 머리말을 손으로 다시 그리다 `sm:text-[2rem]` 을 빠뜨려** 넓은 화면에서만
        제목이 혼자 작았다. 폼 버튼 줄도 `mt-1 self-start` / `mt-1 flex gap-2` / `flex gap-2` /
        `size="lg" w-full` 네 가지였다.
      → **`page-header.tsx` → `page.tsx` 로 승격**(`git mv`, 히스토리 보존). 머리말만 갖고 있던
        컴포넌트가 이제 바깥 래퍼와 간격까지 함께 쥔다. **간격과 머리말이 같은 컴포넌트에 있어야
        드리프트가 안 생긴다** — 따로 두면 한쪽만 고치게 된다.
      → 규칙을 정하고 전부 맞췄다(위 "디자인 시스템" 9번): eyebrow는 소속, back은 목록에서
        파고든 화면에만, action은 하나, 폼 버튼 줄은 `FormActions`.
      → **로그인·회원가입의 eyebrow(`Odolog`)를 없앴다.** 다른 eyebrow(`Garage`/`Account`)는
        "어디에 속하는가"인데 이것만 브랜드명이라 성격이 달랐다. 로그인 전에는 아직 아무 데도
        속하지 않았으므로 비우는 게 맞다.
      → **인증 폼을 Card 안으로 넣었다 — 이전 결정을 뒤집은 것이다.** 재설계 때 "폼을 카드에
        넣지 않는다"고 적었지만, 그 결과 앱에서 유일하게 카드 없는 폼이 됐다. 게다가 지금은
        `AuthLayout`(왼쪽 소개 / 오른쪽 폼)이 `Section`(왼쪽 설명 / 오른쪽 폼)과 구조가 같아서,
        오른쪽이 카드인 편이 오히려 자연스럽다.
      → **인증 버튼의 `size="lg" w-full` 도 없앴다.** 관례상 흔한 모양이지만 앱 안에서 혼자
        달랐다. 예외를 문서로 변호하느니 규칙 하나로 가는 쪽을 택했다.
      → **차량 상세의 머리말을 사이드바 밖으로 꺼냈다.** 이제 머리말이 다른 화면과 같은 자리에
        전체 폭으로 있고, 사이드바는 주행거리부터 시작한다.
      → 검증: `tsc -b` / `oxlint` / `vite build` 통과 + `PageHeader` 잔재 0건,
        `Page` 밖에서 직접 그린 `<h1>` 은 랜딩 히어로 하나뿐임을 grep 으로 확인.

- [x] 소개 화면(랜딩) 신설 — `/` (2026-09-11)
      → **지금까지 소개 화면이라는 게 없었다.** `/` → `/vehicles` → `/login` 으로 두 번 튕겨서,
        처음 온 사람이 이 앱이 무엇인지 알 방법이 로그인 폼밖에 없었다.
      → 구성: 히어로(문장 + 시작하기/로그인) → 기능 3칸 → 화면 미리보기 → 마무리 CTA.
      → **미리보기를 스크린샷으로 넣지 않았다.** 같은 토큰으로 다시 그렸기 때문에
        테마를 바꾸면 미리보기도 같이 바뀐다. 캡처는 디자인이 바뀌는 순간 옛날 화면이 된다.
        보여주는 값(45,000km 등)은 예시이며, 섹션 제목이 "미리보기"임을 분명히 해 뒀다.
      → **`.reveal` — JS 없는 스크롤 진입 연출.** `animation-timeline: view()` 는 "시간" 대신
        "요소가 화면에 들어온 정도"를 진행도로 쓴다. IntersectionObserver 없이 되고,
        스크롤을 되돌리면 연출도 되감긴다.
        **`@supports` 로 감싼 게 핵심이다** — 미지원 브라우저에서 `opacity: 0` 인 채로 굳으면
        내용이 영영 안 보인다. 지원하는 브라우저에서만 opacity를 건드리게 막아 뒀다.
      → 기능 3칸은 `gap-px` + 바깥 배경을 선 색으로 두는 방식. 칸마다 border를 주면 맞닿는
        자리에서 선이 두 겹이 되어 1px이 2px로 보인다.
      → **`AuthLayout` 의 기능 목록을 걷어냈다.** 랜딩과 같은 내용을 두 곳에 두면 한쪽만 고치게
        된다. 로그인 화면 왼쪽은 문장 + "오도로그가 하는 일 →" 링크만 남겼다 —
        로그인하러 온 사람에게 읽을거리를 더 줄 이유도 없다.
      → 로고는 로그인 여부와 상관없이 **항상 `/`(소개 화면)** 로 간다. 처음엔 로그인 시
        `/vehicles` 로 보내게 했다가 되돌렸다 — 같은 버튼이 상황에 따라 다른 곳으로 가면
        누를 때마다 어디로 갈지 예측해야 한다. 로그인한 사람이 차량 목록으로 가는 길은
        홈의 "내 차량 보기" 버튼이다.
      → `*`(404) 목적지를 `/vehicles` → `/` 로 바꿨다. 예전에는 로그인 안 한 사람이
        거기서 다시 `/login` 으로 튕겨 두 번 이동했다.
      → 로그인 전 헤더에 `로그인` 버튼 추가. 단 `/login`·`/signup` 에서는 숨긴다 —
        지금 있는 곳을 가리키는 버튼이 되기 때문.
      → 검증: `tsc -b` / `oxlint` / `vite build` 통과 + 빌드 CSS에서 `@supports` 블록과
        `@keyframes reveal` 생성 확인. **브라우저 눈 확인은 아직 안 했다.**

- [x] PC 화면 최적화 — 넓은 화면 레이아웃 (2026-09-11)
      → 본문 폭 44rem(704px) → **76rem(1216px)**. 다만 **폭을 늘린 게 아니라 열을 나눴다.**
        글이 담기는 열은 어디서도 700px를 넘지 않는다 — 한 줄이 1200px가 되면 읽기가 더 나빠진다.
      → 화면별 레이아웃: 목록은 **카드 격자**(sm 2열 / xl 3열), 차량 상세는 **사이드바 21rem +
        본문**(사이드바는 `lg:sticky`), 설정·등록 폼은 **`Section`**(왼쪽 설명 / 오른쪽 폼),
        로그인·가입은 **`AuthLayout`**(왼쪽 소개 / 오른쪽 폼, lg 이상에서만).
      → **lg에서 3열로 가지 않고 xl부터 3열이다.** lg(1024px)에서 3열이면 카드가 315px까지
        좁아져 "현대 아반떼" 같은 짧은 이름도 줄바꿈된다. 직접 계산해 보고 정했다.
      → **`AuthLayout` 을 `app/` 에 뒀다.** 로그인·회원가입 두 화면이 공유하는 껍데기이고
        라우트를 감싸는 울타리라, 이미 `ProtectedRoute` 가 있는 자리와 같은 성격이다.
        `features/auth/components/` 를 새로 만들면 파일 하나짜리 폴더가 생긴다.
      → **`grid` 함정 2개를 실제로 밟았다.** ① grid 자식의 기본 `min-width` 가 `auto` 라서
        긴 메모 한 줄이 열을 밀어내 격자가 넘친다 → `minmax(0,1fr)` / `min-w-0`.
        ② `lg:sticky` 는 `self-start` 없이는 안 걸린다 — 격자 칸이 옆 열 높이만큼 늘어나
        "붙을 여유"가 사라지기 때문.
      → **넓어진 자리를 여백이 아니라 데이터로 채웠다.** `NextServiceCard` 가 3열
        (종류 / 마지막 정비 / 다음 정비)이 되면서 **`lastServiceOdometer` 가 처음으로 화면에
        나왔다** — 백엔드가 계속 내려주고 있었는데 여태 아무도 안 쓰던 값이다.
        정비 이력 행도 수치를 오른쪽 열로 빼 `tabular-nums` 로 세로 정렬했다.
      → 프로필에 **'화면' 섹션**을 추가했다(화면 모드 + 현재 상태 설명). 헤더 컨트롤은
        "지금 바꾸는" 자리, 설정은 "무엇이 기억돼 있는지 확인하는" 자리다.
        `useTheme().resolved` 가 여기서 처음 쓰인다 — system 일 때 지금 어느 쪽인지 글로 알려준다.
      → 로고 SVG가 헤더·로그인·빈 상태 3곳에 복사돼 있어 `shared/ui/mark.tsx` 로 뽑았다.
        로고는 언젠가 반드시 바뀌는 것이라 세 벌로 두면 그때 한두 곳을 놓친다.
      → 검증: `tsc -b` / `oxlint` / `vite build` 통과. **브라우저 눈 확인은 아직 안 했다.**

- [x] 라이트/다크 모드 (2026-09-11)
      → 다크 전용으로 만든 걸 하루 만에 되돌린 게 아니라, **토큰 이름은 그대로 두고 값만 두 벌로**
        늘렸다. 컴포넌트 코드는 `bg-card` / `bg-fill` / `hover:bg-wash` 를 그대로 쓴다.
      → **선행 작업이 있었다: 컴포넌트에 박혀 있던 `white/[0.04]` 류 8곳을 먼저 의미 토큰으로
        바꿔야 했다.** 그대로 두면 라이트에서 그 8개 요소만 안 보인다. 새로 만든 토큰은
        `fill`/`fill-hover`/`wash`/`sunken`/`card-hover`/`border-strong`/`shimmer`/`selection`.
        재설계 때 "역할 이름"이 아니라 값으로 적어 둔 대가를 바로 치른 셈이다.
      → **대비 검사에서 버그 2건.** 라이트 `muted` 가 3.2:1 로 AA 미달이었고,
        **이미 커밋한 다크의 `faint`(2.16:1)에 번호판·정비 날짜가 들어가 있었다.**
        네 단계를 다시 잡고(라이트 15.30/8.16/4.82, 다크 19.23/10.54/5.28),
        `faint` 는 placeholder·장식 아이콘 전용으로 못박은 뒤 본문 용도 7곳을 `muted` 로 올렸다.
        수치는 알파 합성 후 실제 픽셀 색으로 계산해 확인했다(눈대중 금지).
      → **전환은 View Transitions API 로 한다.** 누른 버튼 좌표에서 새 화면이 원형으로 번진다.
        `::view-transition-old/new(root)` 의 기본 교차 페이드를 끄고(`animation: none`)
        새 화면에만 `clip-path: circle()` 애니메이션을 건다. 반지름은 버튼 중심에서 화면
        가장 먼 모서리까지의 거리 — `150vmax` 같은 큰 수를 넣으면 원이 화면을 벗어난 뒤에도
        애니메이션이 계속 돌아 끝부분이 멈춘 것처럼 보인다.
      → **`flushSync` 가 반드시 필요하다.** View Transition 은 콜백이 끝난 직후의 화면을 찍는데,
        React의 평소 비동기 렌더로는 그 시점에 DOM이 아직 안 바뀌어 있어 **옛 화면을 두 번 찍는다.**
      → 이 API가 없는 브라우저에서는 연출만 빠지고 즉시 바뀐다(점진적 향상).
        `prefers-reduced-motion` 이 켜져 있어도 연출을 건너뛴다.
      → **FOUC는 React 안에서 못 막는다.** 브라우저는 번들을 받기 훨씬 전에 첫 화면을 그린다.
        `index.html` 인라인 스크립트로 `<html>` 에 클래스를 미리 붙인다. 그래서 `'odolog-theme'`
        문자열이 HTML과 `ThemeContext.ts` 양쪽에 중복이다 — import 를 쓸 수 없는 자리다.
      → **노이즈 텍스처는 다크에만 남겼다.** 검정 면의 색 띠(banding)를 깨려고 넣은 것이라
        밝은 바탕에서는 깰 띠가 없고 얼룩으로만 보인다. "다크에 있으니 라이트에도" 로 옮기지 않는다.
      → 세그먼트 컨트롤은 macOS 시스템 설정의 '외관' 과 같은 형태(해/모니터/달 + 미끄러지는 알약).
        아이콘 하나짜리 토글을 쓰지 않은 이유: `system` 을 표현할 자리가 없고,
        "지금이 다크라는 뜻인지 누르면 다크가 된다는 뜻인지"가 늘 헷갈린다.
      → 헤더에 칸이 하나 늘어 닉네임 버튼에 `max-w-24 truncate` 를 붙였다.
        닉네임은 30자까지 가능해서, 안 막으면 긴 닉네임 하나가 로그아웃 버튼을 화면 밖으로 민다.
      → 검증: `tsc -b` / `oxlint` / `vite build` 통과 + 빌드된 CSS에서 `:root` / `:root.dark` /
        `theme-reveal` / hover 유틸 생성까지 직접 확인. **브라우저 눈 확인은 아직 안 했다.**

- [x] UI/UX 전면 재설계 — 다크 모노톤 디자인 시스템 (2026-09-11)
      → 이때는 **다크 전용**으로 만들었다. (2026-09-11 같은 날 라이트/다크 양쪽으로 확장 — 위 항목)
        규칙은 위 "디자인 시스템" 섹션에.
      → shadcn 기본 oklch 팔레트(라이트/다크 두 벌, sidebar·chart 토큰 포함 60여 개)를
        `rgba(255,255,255,α)` 모노톤 한 벌로 교체. **사용처가 없는 sidebar·chart 토큰은 지웠다** —
        나중에 `shadcn add sidebar` 를 하면 CLI가 다시 넣어 준다.
      → **웹폰트 제거.** `@fontsource-variable/geist` import 를 빼고 시스템 폰트 스택으로 갔다.
        패키지는 `package.json` 에 남아 있으므로 `npm uninstall @fontsource-variable/geist` 필요.
      → **중복 3건을 컴포넌트로 뽑았다**: `Field`(라벨+입력 묶음이 15번 복사돼 있었다),
        `Pagination`(차량 목록·정비 이력이 같은 마크업을 각자 보유), `PageHeader`.
        `controlClassName` 은 input·textarea·네이티브 select 세 곳이 공유한다.
      → **인증 화면에서 Card 를 걷어냈다.** 검은 바탕 위에 입력창만 떠 있는 편이 조용하고,
        카드 테두리가 없어지면 화면의 선이 입력창 개수만큼으로 줄어든다.
      → **차량 상세의 주행거리를 화면 주인공으로 올렸다.** 표(`<dl>`) 한 줄이었는데, 이 앱에서
        가장 자주 확인하는 숫자이고 앱 이름도 여기서 왔다. 64px 숫자 + 작은 단위(km).
      → **`<select>` 에 `appearance-none` 을 주면 화살표가 사라진다.** OS 기본 화살표는 색을
        바꿀 수 없어 다크 배경에서 혼자 튀는데, 지우고 나서 대체 아이콘을 얹는 걸 처음에 빠뜨렸다.
        `pointer-events-none` 을 안 주면 아이콘이 클릭을 가로채 목록이 안 열린다.
      → **Tailwind v4 함정**: `@theme inline` 안에서 정의한 변수를 같은 블록의 다른 변수 값에서
        `var()` 로 참조하면 런타임에 해석되지 않는다. `--ease-out-apple` 을 `:root` 에 따로 두고
        `@theme` 이 그걸 가리키게 했다 (색 토큰이 쓰던 것과 같은 방식).
      → 검증: `tsc -b` / `oxlint` / `vite build` 통과. 린트 경고 1건(`button.tsx` 의
        `buttonVariants` export)은 shadcn 원본에 원래 있던 것이라 그대로 뒀다.
        **브라우저 눈 확인은 아직 안 했다** — 아래 각 Phase의 "브라우저에서 확인" 항목이 그대로 남아 있다.

- [x] 전체 점검 + 프론트엔드 디렉토리 세분화 (2026-09-09)
      → **점검 결과 세분화가 필요한 쪽은 프론트엔드뿐이었다.** 백엔드는 파일 37개가 디렉토리
        23개에 있고 그중 13개가 파일 1개짜리라, 더 쪼개면 경로만 길어진다. 반면 프론트는
        `features/*` 가 평평했다(auth 6·vehicles 4·maintenance 4개 파일이 한 폴더에).
      → **`app/` 층 신설.** `shared/layout/Header.tsx` 가 `@/features/auth/AuthContext` 를
        import 하고 있었다 — 문서가 금지한 "shared가 features를 아는" 역방향 의존이 실제로
        있었던 것. `Header`·`ProtectedRoute`·`App` 을 `app/` 으로 올려 해결했다.
        의존 방향이 `app → features → shared` 3층으로 정리됐다.
      → **`shared/api/types.ts` 를 기능별로 분해.** 한 파일이 user·vehicle·maintenance의 DTO를
        전부 알고 있었다(127줄). import 화살표는 없었지만 지식의 방향이 역방향이다.
        지금 `shared/api/types.ts` 에는 `PageResponse`/`ErrorResponse` 둘만 남았다.
      → **`auth/api/endpoints.ts` 신설.** vehicles·maintenance 에는 있는데 auth 만 없어서
        `'/api/users/login'` 같은 URL 문자열이 `AuthProvider`·`SignUpPage`·`ProfilePage`
        세 곳에 흩어져 있었다. 이제 화면 파일(`*.tsx`)에 URL 문자열이 하나도 없다.
        `AuthProvider` 는 지역 변수 `login`/`logout` 과 이름이 겹쳐 `requestLogin`/
        `requestLogout` 별칭으로 가져온다.
      → **문서의 의존 방향이 틀려 있었다.** 프론트는 `maintenance → vehicles` 라고 적혀 있었지만
        실제 import 는 `vehicles → maintenance` 하나뿐이다(차량 상세가 정비 컴포넌트를 얹는다).
        백엔드도 `vehicle ↔ maintenance` 가 패키지 수준에서 서로를 아는데(삭제 순서 제어용
        `MaintenanceRecordRepository` 주입) 한 줄 요약이 그걸 가리고 있었다. 둘 다 바로잡았다.
      → 기준: **폴더는 파일 2개부터 만든다.** 그래서 `shared/lib` 을 `hooks`/`lib` 로 쪼개지 않았고
        `maintenance` 에 `pages/` 를 만들지 않았다.
        **※ 이 기준은 2026-09-13에 뒤집혔다** (맨 위 항목 참고). 지금은 `shared/lib` 이
        `format/` 과 `hooks/` 로 나뉘어 있다.
      → 검증: 이동은 전부 `git mv`(히스토리 보존), 타입 검사·린트 통과, **빌드 CSS 해시가
        재구성 전과 동일**(`index-SCKbKCcF.css`). JS 만 296.49 → 296.62 kB 로 늘었는데
        새로 만든 `auth/api/endpoints.ts` 모듈 하나에 해당한다.

- [x] 요청 DTO 검증 구멍 2건 (2026-09-07 점검)
      → **`SignUpRequest.phone` 에 `@Size(max = 20)` 이 없었다.** `User.phone` 컬럼이 `length = 20`
        이고 MariaDB `sql_mode` 가 `STRICT_TRANS_TABLES` 라, 21자를 보내면 잘리는 게 아니라 DB에서
        에러가 나고 `DataIntegrityViolationException` → 핸들러 없음 → **500**이 됐다. 400이어야 한다.
        같은 필드인데 `UpdateProfileRequest.phone` 에는 이미 `@Size(max = 20)` 이 있었다.
      → **`VehicleRegisterRequest.modelYear` 는 검증이 전혀 없었다.** 화면이 `required min max` 로
        막고 있어서 정상 경로로는 안 터졌지만, **검증을 프론트에만 두면 API는 무방비다.**
        `@NotNull @Min(1900) @Max(2100)` 추가.
      → 프론트 `types.ts` 의 거짓말 2개도 함께: `VehicleResponse.modelYear` 는 `number` 라고 단언했지만
        백엔드는 `Integer`(nullable) — 검증이 붙기 전 데이터는 null 일 수 있어 `number | null` 로 고치고
        화면 2곳에 "연식 미상" 처리를 넣었다. `SignUpRequest.phone` 도 `string` → `string?`.
      → 테스트 3개 추가(53 → 56). 애노테이션을 다시 빼고 돌려서 **셋 다 실패하는 것을 확인**했다.

- [x] Phase 6 (3) — 삭제 버튼 중복 클릭 방지
      → 점검하다 "폼 제출 버튼 비활성화"가 **이미 6곳 모두 되어 있음**을 확인했다(로그인·회원가입·
        프로필·차량 등록·정비 이력·주행거리). 체크리스트의 그 줄은 미완료 항목이 아니라 이유 설명이었다.
      → 실제로 안 막혀 있던 곳은 **삭제 버튼 2개**였다. `window.confirm` 이 모달이라 진짜 더블클릭은
        막히지만, 확인을 누른 뒤 요청이 도는 동안 버튼이 살아 있어 두 번째 DELETE 를 보낼 수 있었다.
        그러면 삭제는 됐는데 화면엔 두 번째 요청의 404 가 뜬다.
      → 정비 이력은 `deletingId`(눌린 행의 id)로 잠근다. boolean 하나면 목록 전체가 잠겨서
        어느 줄을 지우는 중인지 안 보인다.
      → 차량 삭제는 성공 시 화면을 떠나므로 `finally` 가 아니라 **실패했을 때만** 되돌린다.
        `finally` 에 두면 사라지는 컴포넌트에 setState 하게 된다.

- [x] Phase 6 (2) — 로딩·에러 표시를 `LoadingText` / `ErrorText` 로 통일
      → 같은 마크업이 로딩 5곳, 에러 10곳에 **글자까지 똑같이** 복사돼 있었다. 에러는 조회 화면만이
        아니라 로그인·회원가입·프로필·차량 등록·정비 폼에도 있었다(폼 검증 실패 표시).
      → **빈 상태는 일부러 뽑지 않았다.** 차량 목록은 카드 + "첫 차량 등록하기" 버튼이고 정비 이력은
        한 줄짜리 문장이라, 한 컴포넌트로 덮으면 옵션만 늘어난다. 같은 개념이라고 무조건 합치지 않는다.
      → 두 컴포넌트는 `shared/ui/state.tsx` 에 뒀다. 이 폴더는 원래 shadcn이 복사해 넣는 자리라
        우리가 쓴 파일임을 파일 주석과 구조 문서에 표시했다.
      → `MaintenanceSection` 에서 `(error ?? actionError) as string` 이 필요해졌는데, 변수로 한 번
        받으니(`const errorMessage = error ?? actionError`) 단언 없이 좁혀졌다. JSX 안에서 같은 식을
        두 번 쓰면 TypeScript 가 매번 새 식으로 봐서 좁히지 못한다. **단언이 필요해지면 대개
        타입이 아니라 코드 모양이 문제다.**
      → 다음 항목(목록 스켈레톤)이 이제 `LoadingText` 한 곳만 고치면 되는 작업이 됐다.

- [x] Phase 6 (1) — 조회 로직 공용 훅 `useAsyncData` 추출
      → 화면 4곳(차량 목록·차량 상세·정비 이력·다음 정비 시점)이 `data/loading/error` 3개 상태와
        `cancelled` 플래그 + try/catch/finally를 **각자 복사해서** 들고 있었다. 131줄 → 53줄.
      → `load` 는 `useCallback` 으로 감싸서 넘긴다. 무엇이 바뀌면 다시 부를지를 그 의존성 배열이
        정하므로, 훅이 deps 배열을 받아 그대로 넘기는 설계(린터가 검사를 못 함)를 피할 수 있다.
      → **`reloadKey` 를 가짜 의존성으로 넣는 방식을 버렸다.** 처음엔 `useCallback(..., [id, page,
        reloadKey])` 로 썼는데 oxlint 가 `exhaustive-deps: unnecessary dependency` 로 잡았다.
        규칙을 끄는 대신 훅이 `reload()` 를 돌려주도록 바꿨다 — `MaintenanceSection` 에서
        `reloadKey` 상태가 통째로 사라졌다.
      → `NextServiceCard` 는 `reloadKey` prop 자체를 없애고 부모가 `key={maintenanceVersion}` 로
        **재생성**한다. React 가 컴포넌트를 버리고 새로 만드는 게 상태 초기화의 정석이다.
        재조회 시 "불러오는 중…"이 잠깐 보이지만, 값이 실제로 다시 계산되는 것이라 오히려 정직하다.
      → `setData` 도 함께 돌려준다. `VehicleDetailPage` 는 주행거리 갱신 응답을 그대로 갈아끼워야
        해서(다시 조회하면 낭비) 쓰기가 필요하다.
      → 조회 실패(`error`)와 행동 실패(`actionError`)를 분리했다. "목록을 못 불러옴"과
        "삭제 버튼을 눌렀는데 실패"는 사라져야 하는 시점이 다르다.

- [x] 전체 점검 후속 정리 (2026-09-06) — 코드 6건 + 문서 2건
      → **정비 이력 "최신 1건" 조회가 비결정적이던 버그.** 정렬 기준이 `serviceDate` 하나뿐이라
        같은 날 같은 종류를 두 번 등록하면 어느 쪽이 뽑힐지 DB가 정했다. 고치기 전에 테스트로
        재현했고(`expected: 20000 but was: 10000`) `...OrderByServiceDateDescIdDesc` 로 해결.
        `createdAt` 대신 `id` 를 동점 기준으로 쓴 이유는 `LocalDateTime.now()` 가 같은 밀리초에
        또 동점이 될 수 있는 반면 IDENTITY 는 절대 중복되지 않기 때문.
      → **`MaintenanceRecordRepositoryTest` 신설.** 파생 쿼리는 메서드 이름 오타가 컴파일에
        안 걸리고 앱 기동 때야 터지는데, maintenance 만 `@DataJpaTest` 가 없었다.
      → **`ConflictException` 도입(규칙 12).** `IllegalArgumentException` → 409 매핑이 너무 넓어
        JDK/스프링 내부에서 난 예외까지 409로 포장돼 나갔다. 프로덕션 3곳(이메일 중복·번호판
        중복·주행거리 감소)만 전용 예외로 바꾸고 범용 예외 핸들러를 제거.
      → **번호판 유니크를 전역 → 소유자별로 변경.** `uk_vehicles_plate_number` →
        `uk_vehicles_user_plate_number(user_id, plate_number)`. 전역 유니크는 중고차 이전과
        가족 공유 차량을 막고, 409 메시지가 "남이 이미 등록했다"를 알려줬다(로그인 실패 사유를
        일부러 통일해 둔 방침과 모순). 이제 내 차량만 검사하므로 메시지에 번호판을 그대로
        보여줘도 안전하다.
        `findByPlateNumber`(`Optional<Vehicle>`)는 **지워야만 했다** — 같은 번호판이 여러
        사용자에게 존재할 수 있게 되어 결과가 2건 이상 나올 수 있고, 그러면 Spring Data가
        `IncorrectResultSizeDataAccessException` 을 던진다. 반환 타입 자체가 거짓말이 된 것.
      → **`NextServiceCard` 의 로딩/에러 분리.** `results === null` 하나에 "요청 중"과 "실패"가
        겹쳐 있어서, 실패하면 카드가 통째로 사라지고 사용자는 영영 이유를 몰랐다.
        재조회 때는 `setLoading(true)` 를 하지 않는다 — 이미 보이던 목록이 깜빡인다.
      → **서비스 3개에 `@Transactional(readOnly = true)` 클래스 기본값(규칙 13).**
      → 문서: `README.md` 의 `user@localhost` 설명이 아래 트러블슈팅과 어긋나 있어 정정,
        로드맵의 Phase 5 `(완료)` 표기 누락 보완.

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
- [x] 미사용 정리 + 디렉토리 세분화 (프론트 기능별 재구성, 백엔드 DTO request/response 분리)
      → 정리: `frontend/src/assets/`(템플릿 잔재 2개), `public/icons.svg`(참조 없음),
        `frontend/README.md`(Vite 템플릿 → 실제 실행법), `VehicleRepository.findByOwner`
        (테스트에서만 쓰이고 프로덕션 미사용) + 그 테스트. 테스트 48 → 47개.
      → 백엔드: 모든 `dto/`를 `dto/request/`와 `dto/response/`로 분리(13개 이동).
        `common/dto`는 요청 DTO가 없어 `response/`만 생겼다.
      → 백엔드 `common/exception`은 **일부러 기능별로 나누지 않았다.** 세 기능이 모두 쓰는 것이라
        어느 한 기능으로 옮기면 잘못된 방향의 의존이 생긴다.
      → 프론트: `pages/lib/components`(역할별) → `features/{auth,vehicles,maintenance}` +
        `shared/{api,lib,layout,ui}`(기능별)로 재구성. 백엔드와 같은 원칙이 되었다.
      → `components.json`의 aliases를 `@/shared/ui`로 갱신. 안 하면 다음 `shadcn add`가
        예전 위치(`src/components/ui`)에 파일을 다시 만든다.
      → 이미 커밋된 파일은 `git mv`로 히스토리를 보존했고, 아직 커밋 전인 Phase 5 파일만 `mv`.
      → 검증: 백엔드 47개 테스트 통과, 프론트 타입 검사·린트 통과, **빌드 결과 JS 해시가
        재구성 전과 동일**(`index-BfMQAYcH.js`) — 순수 구조 변경이고 동작은 안 바뀌었다는 증거.

- [x] Phase 5 — 정비 이력 관리 화면 (목록/등록/수정/삭제 + 다음 정비 시점)
      → 차량 상세 페이지 안에 `NextServiceCard` + `MaintenanceSection`을 배치.
      → 등록 폼과 수정 폼을 **한 컴포넌트로 겸용**한다(`record === null`이면 등록).
        필드 구성이 같은데 파일을 둘로 나누면 한쪽만 고치는 실수가 생긴다.
      → 수정은 `record`의 값과 비교해 **바뀐 필드만** PATCH. 비용을 0으로 바꾸는 것과
        안 보내는 것은 다르므로 값 비교로 판단한다.
      → 정비 종류 선택은 shadcn Select 대신 **브라우저 기본 `<select>`**. 선택지가 5개뿐이라
        커스텀 드롭다운의 복잡한 구조가 필요 없고, 모바일에서 OS 기본 UI가 뜨는 게 더 편하다.
      → `<input type="date">`의 값이 곧 `YYYY-MM-DD` 문자열이라 백엔드 `LocalDate`와 그대로 맞는다.
      → **`new Date().toISOString().slice(0,10)`을 쓰지 않는다.** UTC 기준이라 한국 시간
        오전 9시 이전에는 하루 전 날짜가 나온다. `todayString()`이 로컬 기준으로 만든다.
      → 이력이 바뀌면 부모(`VehicleDetailPage`)의 `maintenanceVersion`을 1 올리고, 그 값을
        `NextServiceCard`의 `reloadKey`로 내려 다음 정비 시점을 다시 계산시킨다.
      → `NextServiceCard`는 종류 5개를 `Promise.all`로 동시에 요청한다(순차로 기다리면 5배 느림).
        요청 수 자체를 줄이려면 백로그의 "전체 종류 한 번에" API가 필요하다.
      → 검증: 등록/목록/부분수정(cost만)/삭제(204)와 next-service 3케이스
        (계산됨 45,000km+2026-07-15 / OTHER null / 이력 없음 null) 모두 확인.

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

**플랫폼은 웹 하나다** (2026-09-16 확정). iOS/Android 네이티브 앱은 만들지 않고, 세 개를
병행하지도 않는다. 근거와 "다시 꺼낼 조건"은 위 진행 상황의 2026-09-16 항목에 적어 뒀다.
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
      시점")이 `FuelRecord` 로 충족됐다. 자세한 내용은 위 진행 상황의 2026-09-16 항목에.

여기 남은 보류 항목은 없다.

---

## Phase 2 — 프론트엔드 프로젝트 셋업 (완료)

셋업 자체는 끝났고 CORS·세션 쿠키도 커맨드라인으로 검증했다. 남은 것은 사용자의 눈 확인 하나뿐:

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. `JSESSIONID` 확인은 B-12 에 있다.

---

## Phase 3 — 인증 화면 (완료)

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. 해당 항목은 B-10 ~ B-12, B-94 ~ B-95.

---

## Phase 4 — 차량 관리 화면 (완료)

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. 해당 항목은 B-13 ~ B-31, B-98.

---

## Phase 5 — 정비 이력 관리 화면 (완료)

→ 눈 확인은 **Phase 6 의 6-B(1회차)** 로 합쳤다. 해당 항목은 B-32 ~ B-43.

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

### 볼 화면은 8개가 아니라 10개다

라우트는 8개지만(`/` `/login` `/signup` `/vehicles` `/vehicles/new` `/vehicles/:vehicleId`
`/me` `*`), **`/` 가 세 얼굴을 갖는다** — 비로그인 랜딩 / 로그인+0대 등록 권유 /
로그인+차량 있음 통계. `*` 는 화면이 아니라 `/` 로 보내는 리다이렉트다.
그래서 눈으로 볼 상태는 **10개**이고, 여기에 각 화면의 로딩·빈 상태·에러가 더 붙는다.

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
      1회차는 **회원가입부터 데이터가 쌓이는 순서**로 짜여 있고, 특히 B-96(남의 차량이 안 보임) ·
      B-98(차량 삭제 시 동반 삭제를 `COUNT(*)` 로 확인) · B-103(탈퇴 후 0건)은 **빈 상태여야
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
- [ ] **B-08** 없는 계정으로 로그인 → 401 → **폼 안 인라인 에러**. 0.24s / 4px 로 **짧고 가깝게**
      나타나는지(다른 연출처럼 길게 감속하면 급한 소식으로 안 읽힌다)

#### 6-B-2. 가입과 세션

- [ ] **B-09** `/signup` → 비밀번호 7자로 제출 → 400. 가입 폼도 인라인 에러인지
- [ ] **B-10** 정상 가입 → **이어서 로그인까지 자동으로** 되는지(가입 API 는 세션을 안 만든다)
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
- [ ] **B-24** 주행거리 히어로 숫자가 **화면을 열 때 굴러오르지 않는지**(중요 — 0 에서 굴러오는
      건 대시보드 템플릿의 상투구라 일부러 걷어냈다). `tabular-nums` 도 안 붙어 있어야 한다
- [ ] **B-25** 주행거리를 **크게** 갱신 → 직전 값에서 새 값으로 굴러가는지. 변화 폭이 클수록
      오래(0.45~1.4s) 걸리는지. **더 작은 값** → 409 + "(현재 N km)"
- [ ] **B-26** '차량 정보' 카드가 닫혀 있을 때 **값 4개**(번호판·제조사·모델·연식)가 보이는지.
      긴 모델명이 라벨을 밀어내지 않는지(`min-w-0 truncate`)
- [ ] **B-27** `수정` → 0.36s 펼쳐지는지. 칸이 열린 뒤 글자가 0.12s 늦게 들어오는지
- [ ] **B-28** ⚠️ **제조사만 바꾸고 저장 → 409 가 나지 않는지.** 이게 이 기능의 핵심 함정이다 —
      번호판을 그대로 둔 채 다른 필드만 고칠 때 자기 자신이 중복으로 잡히면 안 된다
- [ ] **B-29** 번호판을 **두 번째 차량의 번호로** 바꾸기 → 409 가 폼 안에 뜨는지
- [ ] **B-30** 저장 성공 → **머리말이 같이 바뀌는지**(eyebrow=번호판, 제목=제조사+모델,
      설명=연식). 같은 객체를 보므로 따로 새로고침할 필요가 없어야 한다
- [ ] **B-31** 아무것도 안 바꾸고 `저장` → **Network 탭에 요청이 안 나가는지**(빈 PATCH 방지)

#### 6-B-4. 정비 이력

- [ ] **B-32** 다음 정비 카드 — 이력이 없으므로 **"아직 계산할 이력이 없습니다…" 한 문장**만
      보이는지. ⚠️ **종류 15개가 "기록 없음"으로 줄줄이 늘어서면 안 된다** — 이력이 없는 종류는
      서버가 응답에서 빼므로(2026-09-16), 빈칸 목록이 나오면 옛 화면이 남은 것이다.
      요청은 **1번**인지도 Network 탭에서 확인(종류마다 1번씩 보내던 것을 걷어냈다)
- [ ] **B-33** 정비 폼 열기 → **자리를 밀어내며 0.36s 펼쳐지고**, 안쪽 글자는 0.12s 늦게 들어오는지
      (동시에 나타나면 글자가 찌그러지며 늘어나 보인다)
- [ ] **B-34** 폼 닫기 → **연출 없이 즉시** 닫히는지(이미 사용자가 결정한 일이다)
- [ ] **B-35** 날짜 입력칸의 기본값이 **오늘**인지. UTC 가 아니라 로컬 기준(`todayString()`)이라
      **오전 9시 이전에도 어제가 아닌지**
      → ⚠️ 이 칸은 **기기에 따라 컨트롤이 갈린다.** 마우스·키보드면 네이티브 date 입력,
        터치면 드럼 휠이다(`DateInput` 이 `pointer: coarse` 로 판단). 여기 1회차는 데스크톱
        기준이라 네이티브 쪽을 본다. 휠은 **6-C 의 C-16~C-21** 에서 따로 확인한다
- [ ] **B-36** 엔진오일 이력 등록 → 목록에 표시
- [ ] **B-37** 다음 정비 카드가 갱신되는지. **주행거리 기준과 날짜 기준 둘 다** 나오는지
      (`NextServiceCard` 가 `key` 로 재생성되므로 "불러오는 중…"이 잠깐 보이는 건 정상)
- [ ] **B-38** `기타(OTHER)` 로 등록 → 다음 정비가 **계산되지 않는지**(주기가 둘 다 null)
- [ ] **B-39** **같은 날짜**로 두 건 등록 → 목록 순서가 뒤집히지 않는지
      (동점 기준 `id DESC`. 이게 없으면 새로고침마다 순서가 바뀐다)
- [ ] **B-40** 이력 수정에서 **비용만** 바꾸기 → Network 의 PATCH 요청 바디에
      **`cost` 하나만** 들어 있는지(바뀐 필드만 보낸다)
- [ ] **B-41** 이력 삭제 → 204. 삭제되는 **그 행만** 잠기고 목록 전체가 잠기지 않는지
      (`deletingId` 로 잠근다. boolean 하나면 어느 줄을 지우는 중인지 안 보인다)
- [ ] **B-42** 이력을 11건 이상 등록 → 페이지네이션. 2페이지에서 한 건 삭제 시
      요청이 **두 번 나가지 않는지**(Network 탭에서 GET 개수 확인)
- [ ] **B-43** 차량 상세가 `lg` 에서 2단인지. **왼쪽 사이드바가 스크롤에 붙어 따라오는지**
      (`lg:sticky` + `self-start`)

#### 6-B-5. 주유 기록과 연비

**이 기능은 코드를 쓴 뒤 한 번도 눈으로 못 봤다.** 연비는 서버가 계산해 주는 값이라,
화면에 찍힌 숫자가 맞는지는 손으로 나눠 봐야 안다.

- [ ] **B-44** 차량 상세 오른쪽에 **연비 카드 + 주유 기록 카드**가 있는지
- [ ] **B-45** 기록이 0건일 때 연비 카드가 **"첫 주유 기록은 기준점이 됩니다. 다음 주유
      기록부터 연비를 계산합니다."** 를 띄우는지 (0.00 km/L 이 찍히면 안 된다).
      목록 쪽 빈 상태는 "아직 주유 기록이 없습니다. 두 번째 기록부터 연비가 계산됩니다."
      → 기록이 없으면 **`연비 초기화` 버튼이 아예 없어야** 한다 — 눌러도 아무 일 없는 버튼을 두지 않는다
- [ ] **B-46** `주유 추가` → 날짜는 **오늘**, 주행거리는 **차량의 현재 값**이 미리 채워지는지
- [ ] **B-47** 주유량·금액을 입력하는 동안 **"리터당 약 N원"이 실시간으로** 바뀌는지.
      영수증과 대조해 오타를 그 자리에서 잡으라고 둔 것이다
- [ ] **B-48** 주유량에 `0` → 막히는지(연비 계산이 0으로 나누기가 된다)
- [ ] **B-49** 주유량에 `25.123`(소수 셋째 자리) → 막히는지(`@Digits fraction = 2`)
- [ ] **B-50** 첫 기록 등록 → 목록에 **`기준 기록 · 다음 주유부터 계산`** 으로 뜨는지.
      **0.00 이면 잘못된 것이고**(직전 기록이 없으면 null), 그냥 `—` 만 있어도 낡은 화면이다 —
      연비가 없는 이유가 둘(첫 기록 / 기준점)이라 **말도 둘로 갈라 뒀다**
- [ ] **B-51** ⚠️ **차량 주행거리가 따라 올라갔는지.** 주유 주행거리를 차량 값보다 크게 넣으면
      위쪽 히어로 숫자가 바뀌고, **그 차이만큼 굴러가는 연출**이 보여야 한다
- [ ] **B-52** 과거 주유를 **작은 주행거리로** 등록 → 차량 주행거리가 **안 내려가는지**
- [ ] **B-53** 그때 주행거리 칸 도움말이 **`차량에 기록된 N km 보다 작습니다. 과거 기록이면
      그대로 두세요.`** 로 바뀌는지. ⚠️ **막히지 않고 저장까지 되어야 한다** — 지난달 영수증을
      정리하는 건 정상적인 사용이고 계기판을 교체했을 수도 있다.
      평소 도움말은 "계기판 숫자. 이 값이 차량 주행거리보다 크면 차량 쪽도 함께 올라갑니다."
- [ ] **B-54** 두 번째 기록 등록(주행거리를 500km 올리고 25L) → 연비가 **20.00 km/L** 인지.
      손으로 나눠서 맞춰 볼 것
- [ ] **B-55** 연비 카드의 **평균 연비 히어로 숫자**가 뜨는지. 통계 4칸(기록·주행·주유량·총 유류비)
- [ ] **B-56** 히어로 숫자에 `tabular-nums` 가 **안 붙어 있는지**(맞출 상대가 없는 큰 숫자)
- [ ] **B-57** 목록이 **주행거리 내림차순**인지(최신이 위). 날짜순이 아니다 — 정렬이 곧 연비
      계산의 전제라 서버가 고정한다
- [ ] **B-58** 기록을 11건 이상 만들어 **페이지네이션**. ⚠️ **2페이지 첫 행에도 연비가 나오는지** —
      그 행의 짝은 1페이지에 있어서 서버가 따로 한 건 더 조회한다. 여기가 비어 있으면 버그다
- [ ] **B-59** 중간 기록의 **주행거리를 수정** → 그 기록과 **바로 뒤 기록의 연비가 둘 다** 바뀌는지
      (계산 값을 저장하지 않고 읽을 때 계산하는 이유가 이것이다)
**이상 연비와 빠진 기록** (2026-09-17 신설. 만들고 눈으로 못 봤다):

- [ ] **B-60** 주행거리를 **한 자리 크게** 적은 기록을 만들어 연비를 50 넘게 만들기 →
      그 행에 **`확인 필요`** 가 빨갛게 붙는지. ⚠️ **숫자가 지워지지 않고 옆에 붙어야 한다** —
      무엇을 잘못 적었는지 보려면 그 값이 남아 있어야 한다
- [ ] **B-61** 구간을 **고르게 3개 이상** 쌓은 뒤 중간 주유 하나를 삭제 → 연비 카드에
      **"평소보다 긴 구간이 N곳 있습니다. 주유 기록이 빠졌다면 채워 넣으면 연비가 다시
      계산됩니다."** 가 뜨는지
      → 구간이 **3개 미만이면 뜨면 안 된다**("평소"라는 게 없는데 의심부터 하면 안 된다)
      → 지운 기록을 **다시 넣으면 안내가 사라지는지**. 문구가 빈말이 아니어야 한다

**연비 초기화** (2026-09-17 신설. 만들고 눈으로 못 봤다):

- [ ] **B-62** 카드 오른쪽 위 `연비 초기화` → confirm **"지금까지의 기록을 연비 계산에서
      빼고 다시 셉니다. 계속할까요?"**
- [ ] **B-63** 확인 → 평균 연비 히어로 숫자가 사라지고 **"연비를 초기화했습니다. 다음 주유
      기록부터 다시 계산합니다."** 로 바뀌는지
- [ ] **B-64** ⚠️ **같은 순간 목록도 갱신되는지** — 기준점이 된 행이 `연비 기준점 · 다음
      주유부터 계산` 으로 바뀌어야 한다. 카드만 바뀌고 목록이 옛 연비를 들고 있으면
      `fuelListVersion` 이 안 도는 것이다(작업 중 실제로 빠뜨렸던 자리)
- [ ] **B-65** ⚠️ **통계 4칸(기록·주행·주유량·총 유류비)은 그대로인지.** 초기화는 연비만
      다시 세는 것이지 **지출을 없던 일로 만드는 게 아니다.** 여기가 줄어들면 잘못된 것이다
- [ ] **B-66** 주유를 한 건 더 등록 → 평균이 다시 나오고, 그 아래 **"연비 초기화 이후 구간만
      계산한 값입니다."** 가 붙는지. `초기화 해제` 를 누르면 전체 평균으로 돌아오는지
- [ ] **B-67** 기록 삭제 → 확인 문구에 "연비가 다시 계산됩니다"가 있는지. 삭제 후 연비가 바뀌는지
- [ ] **B-68** 삭제 중 **그 행만** 잠기는지(목록 전체가 아니라)

#### 6-B-6. 홈 통계와 차트

**여기가 눈으로 처음 보는 화면이다** — 통계·차트는 만든 뒤 한 번도 못 봤다.

- [ ] **B-69** `/` → 통계 화면(세 번째 얼굴). eyebrow 가 `Overview`(0대일 때의 `Garage` 가 아니다),
      타일 4개는 `Vehicles` · `Distance` · `Records` · `Cost`.
      ⚠️ **`Records` 는 정비 + 주유 건수 합계**이고 **`Cost` 는 정비비 + 유류비**다 —
      정비만 세고 있으면 9/16 에 고친 것이 되돌아간 것이다
- [ ] **B-70** `Cost` 타일 아래에 **`정비 N · 주유 N`** 구성 한 줄이 붙는지.
      합계만 주면 어느 쪽이 큰지 알 수 없어서 둔 줄이다
      → 여기서 **"일부 기록만 합산됨" 같은 단서가 보이면 안 된다.** 요약 API 가 서버에서
        전부 더하므로 상한이 없어졌고, 화면이 변명하던 자리는 사라졌다
- [ ] **B-71** 타일 값에 `tabular-nums` 가 **안 붙어 있는지**(큰 숫자 하나는 맞출 상대가 없다)
- [ ] **B-72** 타일 사이 `gap-px` 격자의 선이 1px 인지. 칸 배경이 새지 않는지
- [ ] **B-73** 차트 제목이 **`지난 12개월 유지비`**(정비 + 주유)인지. 정비비만 세는 옛 제목이면
      타일의 `Cost` 와 기준이 달라져 같은 화면에 두 가지 총액이 놓인다.
      **12칸이 항상 다 있는지** — 기록 없는 달도 빈 칸으로 남아야 한다
      (기록 있는 달만 모으면 띄엄띄엄 정비한 것이 꾸준히 한 것처럼 보인다)
- [ ] **B-74** 막대 **네 모서리가 전부 각진지**, 두께가 24px 이하인지
- [ ] **B-75** 세로축 눈금이 `1,873` 같은 수가 아니라 **1·2·5 × 10ⁿ** 으로 끝나는지(`niceMax()`)
- [ ] **B-76** 눈금선이 **점선이 아니라 1px 실선**인지(점선은 "예측·임계선"으로 읽힌다)
- [ ] **B-77** **가장 높은 막대 하나에만** 값이 적혀 있는지(전부 적으면 축이 무의미해진다)
- [ ] **B-78** 막대에 마우스 → 말풍선. 판정 영역이 막대(24px)가 아니라 **칸 전체**인지
- [ ] **B-79** `<details>` 표를 펼쳐 말풍선과 **같은 값**이 나오는지.
      열이 **`정비 / 주유 / 합계`** 로 나뉘어 있는지 — 말풍선에만 구성이 있으면
      **키보드·스크린리더 사용자에게는 없는 값**이다
- [ ] **B-80** 종류별 가로 막대 — **전부 같은 색**인지(클수록 진하게가 아니다). **범례가 없는지**.
      아래에 **"유류비는 포함하지 않습니다"** 가 적혀 있는지 — 주유는 정비 종류가 아니라
      여기 들어갈 자리가 없는데, 옆 차트가 '유지비'가 되면서 헷갈리기 쉬워졌다
- [ ] **B-81** 막대가 자라는 동안 모양이 찌그러지지 않는지(`clip-path`, 칸마다 45ms)
- [ ] **B-82** `최근 활동` 카드에 **정비와 주유가 섞여** 나오는지(`최근 정비` 가 아니다).
      각 줄이 어느 쪽인지 구분되는지
- [ ] **B-83** **같은 날짜의 정비와 주유** → 정비가 위인지. ⚠️ 여기서 id 로 비교하면 안 된다 —
      테이블이 달라 주유 3번이 정비 3번보다 나중이라는 보장이 없다
- [ ] **B-84** `차량별` 카드에 **평균 연비**(`N.Nkm/L`)가 붙는지. 주유 2건 미만인 차량은
      그 자리를 **아예 비우는지**(`연비 —` 를 붙이면 없는 값이 자리를 차지한다)

#### 6-B-7. 프로필 · 비밀번호 · 탈퇴 · 계정 격리

- [ ] **B-85** 헤더 닉네임 클릭 → `/me`. eyebrow `ACCOUNT`, **`Section` 4개**
      (계정 / 비밀번호 / 화면 / 회원 탈퇴)
- [ ] **B-86** 닉네임만 변경 → PATCH 바디에 `nickname` 하나만. 헤더 표시도 같이 바뀌는지
- [ ] **B-87** '화면' 구역의 설명이 현재 테마를 맞게 말하는지(`system` 이면 "지금은 ○○입니다")
- [ ] **B-88** '비밀번호' 구역 — 새 비밀번호 두 칸이 **서로 다르게** 입력되면 폼 안에서 막히는지
      (서버로 보내지 않는다. 확인란은 오타 방지 장치일 뿐이다)
- [ ] **B-89** 새 비밀번호를 7자로 → 400. 가입 때와 같은 제한인지
- [ ] **B-90** ⚠️ **현재 비밀번호를 틀리게 입력 → 401 이 폼 안에 뜨는데 로그아웃되지 않는지.**
      작업 중 실제로 냈던 버그다 — 전역 401 핸들러가 돌면 오타 한 번에 `/login` 으로 쫓겨난다
- [ ] **B-91** 변경 성공 → 안내 문구 + **입력칸 3개가 비워지는지** + **로그인이 유지되는지**
- [ ] **B-92** 로그아웃 후 **새 비밀번호로** 로그인되는지. 옛 비밀번호로는 안 되는지
- [ ] **B-93** 비밀번호 관리자를 쓴다면 '현재'와 '새것'을 구분해 채우는지(`autoComplete`)
- [ ] **B-94** 로그아웃 → 헤더가 `로그인` 버튼으로 돌아가는지
- [ ] **B-95** 주소창에 `/vehicles` 직접 입력 → **`/login` 으로 튕기는지**.
      로그인하면 **원래 가려던 `/vehicles` 로 복귀**하는지
- [ ] **B-96** **두 번째 계정**으로 가입 → 차량 목록이 비어 있는지(**남의 차량이 안 보인다**)
- [ ] **B-97** 두 번째 계정에서 첫 계정 차량의 상세 주소(`/vehicles/1` 등)를 직접 입력 →
      404 와 403 을 **구분해서 보여주지 않는지**(남의 차량 존재 자체를 알리지 않는다)
- [ ] **B-98** 첫 계정 복귀 → 차량 삭제. ⚠️ confirm 문구가 **"이 차량과 정비 이력, 주유 기록이
      모두 삭제됩니다"** 인지 — 주유가 빠져 있으면 9/17 에 고친 것이 되돌아간 것이고,
      "주유 기록은 남겠지" 하고 누른 사용자가 유류비와 연비를 통째로 잃는다 →
      **이력과 주유 기록이 같이 사라졌는지 DB 로 확인**:
      `/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SELECT COUNT(*) FROM maintenance_records; SELECT COUNT(*) FROM fuel_records;"`
- [ ] **B-99** '회원 탈퇴' 구역이 **접힌 채로** 시작하는지. 버튼이 빨갛게 **채워져 있지 않은지**
- [ ] **B-100** 탈퇴에서 비밀번호를 **틀리게** → 401 이 폼 안에 뜨고 **로그인이 유지되는지**
- [ ] **B-101** 탈퇴 성공 → `/` 로 이동하고 헤더가 로그아웃 상태인지.
      **뒤로가기로 방금 화면에 돌아가지지 않는지**(`replace: true`)
- [ ] **B-102** 탈퇴한 계정으로 로그인 시도 → 401
- [ ] **B-103** 탈퇴 후 DB 에 그 사용자의 차량·정비 이력·주유 기록이 **하나도 안 남았는지**
- [ ] **B-104** 없는 주소(`/asdf`) → `/` 로 가는지(`/vehicles` 로 보내면 비로그인 사용자가 또 튕긴다)
- [ ] **B-105** 화면을 옮겨 다니며 **페이지 전환이 0.18s 페이드만**인지.
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
- [ ] **E-6** DevTools > Rendering > `Emulate prefers-reduced-motion` → **연출이 전부 멈추는지**.
      특히 차트 막대가 **잠깐 안 보이는 시간으로 남지 않는지**(지연도 0 이어야 한다)
- [ ] **E-7** 브라우저 확대 200% 에서 레이아웃이 버티는지
- [ ] **E-8** 날짜 휠에 Tab 으로 들어가 **위·아래 화살표 키로** 년/월/일을 바꿀 수 있는지.
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
(위 2026-09-11 항목). **한때 "차량 목록을 그리드로 바꾸지 않는다"고 적어 두었으나 뒤집혔다** —
그 판단은 본문 폭 44rem을 전제로 한 것이었고, 76rem으로 넓히면서 전제가 사라졌다.
그 그리드마저 2026-09-14 에 **괘선 행**으로 다시 바뀌었다.

---

## 완료 판정 기준 (Definition of Done)

아래 시나리오를 브라우저에서 처음부터 끝까지 막힘없이 수행할 수 있으면 "완성"이다.

**Phase 6 의 1회차(6-B) 105개를 순서대로 따라가면 아래가 전부 덮인다.** 오른쪽이 그 항목
번호다 — 따로 한 번 더 돌 필요가 없다.

- [ ] 회원가입 → 로그아웃 → 로그인 — B-10, B-94
- [ ] 새로고침해도 로그인 상태 유지 — B-12
- [ ] 차량 등록 → 목록에 보임 → 상세 진입 — B-18, B-22, B-23
- [ ] 차량 정보 수정, 번호판을 안 바꿨을 때 409 가 나지 않음 — B-28
- [ ] 주행거리 갱신, 더 작은 값으로 갱신 시 알아들을 수 있는 에러 — B-25
- [ ] 정비 이력 등록/수정/삭제 — B-36, B-40, B-41
- [ ] 다음 정비 시점이 주행거리·날짜 두 기준으로 표시됨 — B-37
- [ ] 주유 기록 등록/수정/삭제 — B-46, B-59, B-67
- [ ] 연비가 계산되고, 첫 기록은 `기준 기록 · 다음 주유부터 계산` 으로 이유까지 말함 — B-50, B-54
- [ ] 페이지가 넘어가도 연비가 끊기지 않음 — B-58
- [ ] 주유 기록이 차량 주행거리를 따라 올림 — B-51
- [ ] 연비 초기화 — 기록·지출은 그대로 두고 연비만 다시 셈 — B-62, B-65, B-66
- [ ] 빠진 주유 기록을 평소 구간과 견줘 알려 줌 — B-61
- [ ] 비밀번호 변경, 현재 비밀번호를 틀려도 로그아웃되지 않음 — B-90, B-91
- [ ] 회원 탈퇴 후 그 계정의 데이터가 남지 않음 — B-102, B-103
- [ ] 차량 삭제 시 정비 이력·주유 기록도 함께 사라짐 — B-98
- [ ] 로그인 안 한 상태로 `/vehicles` 직접 접근 시 로그인 페이지로 이동 — B-95
- [ ] 다른 계정으로 로그인했을 때 남의 차량이 안 보임 — B-96, B-97
- [ ] 백엔드 테스트 전체 통과 — `./gradlew test` (134개)

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
- [ ] 정비 이력 종류별 필터링 (`GET .../maintenance-records?type=`)
- [ ] 차량 목록에 각 차량의 "임박한 정비" 요약 포함 (목록 화면에서 바로 보이게)
- [ ] 만탱크 연비 — 지금은 매 주유마다 직전 기록과의 차이로 계산한다(단순법).
      **가득 채우지 않은 주유가 섞이면 그 구간만 실제보다 높게 나온다**(거리는 그대로인데
      리터가 적어서). 기록에 "가득 채웠는가" 플래그를 두면 가득→가득 구간으로 정확히 낼 수 있다.
      → 지금은 `FuelAnomaly` 가 **평소 구간과 견줘 이상한 구간을 알려 주는 것**으로 대신하고 있다.
- [ ] 로그인 실패 응답 시간이 계정 존재 여부에 따라 다르다
      → 이메일이 없으면 BCrypt 검증을 건너뛰어 빨리 답한다. 실패 메시지를 일부러 통일해 둔
        방침(user enumeration 방지)과 어긋나는 지점이라 언젠가 따져 볼 것.

**PWA 는 여기 없다** — 할지 말지를 눈 확인 뒤에 정하기로 해서 **6-G** 에 뒀다.

---

단계를 완료할 때마다 "진행 상황 (완료)" 섹션을 갱신하고, 해당 항목을 위 체크리스트에서 제거한다.
