# 오도로그 (OdoLog)

차량 정비 이력과 주유 기록을 관리하는 웹 앱. 기록이 쌓이면 **연비(km/L)** 와
**다음 정비 시점**(주행거리 기준·날짜 기준)이 계산된다.

Spring Boot 3.5 + MariaDB 백엔드에 React 19 SPA 를 붙인 구성이고, 인증은 **세션 쿠키**다.
백엔드 API 27개 · 화면 10 라우트가 모두 동작하고 테스트 241개(백엔드 202 · 프론트 39)가 통과한다.

플랫폼은 웹 하나다 — 네이티브 앱은 만들지 않는다(근거는 `HISTORY.md` 의 2026-09-16 항목).
개인 학습 프로젝트라 **로컬에서 완전히 동작하는 것**까지가 범위이고 배포는 범위 밖이다.
다만 "올린다면 무엇부터 해야 하는가"는 코드로 준비해 뒀다 — 아래 **보안** 과 운영 프로파일.

## 도메인 규칙 — 계산되는 값 둘

이 앱의 알맹이다. 둘 다 **저장하지 않고 조회할 때 계산한다** — 앞 기록이 수정되면 뒤 기록의
값이 따라 바뀌므로, 계산 결과를 컬럼으로 들고 있으면 언젠가 반드시 원본과 어긋난다.

### 연비

**구간(segment) 단위**로 계산한다. 구간은 주유 기록 두 건 사이다.

    구간 연비 = (이번 odometer − 직전 odometer) ÷ 이번 주유량(L)
    평균 연비 = Σ(구간 거리) ÷ Σ(구간 주유량)

총 거리 ÷ 총 주유량과 값은 같지만, **구간 단위라야 말이 안 되는 구간을 골라낼 수 있다.**

- 첫 기록과 "연비 초기화" 기준점에는 짝이 없다 → 응답이 `0` 이 아니라 **`null`** 이다.
  화면도 그 자리에 `0.00` 대신 `기준 기록 · 다음 주유부터 계산` 을 띄워 **이유를 말한다**
- 성립할 수 없는 구간(odometer 를 한 자리 잘못 적어 200 km/L 이 나오는 식)은 **평균에서 빼고**,
  `excludedSegmentCount` 로 **몇 개를 뺐는지 함께 내려보낸다** — 말없이 빼면 그것도 거짓말이다
- **기록이 빠진 구간도 뺀다**(`longSegmentCount`). 한 번 안 적거나 기록을 지우면 그 구간은
  거리만 두 배가 되고 주유량은 한 번치 그대로라 연비가 두 배로 뜬다. 25 km/L 는 "불가능"이
  아니라서 위 검사에 안 걸리고, 그대로 두면 **조용히 평균을 끌어올린다.**
  판정 기준은 그 차량의 평소 구간이고, 평균이 아니라 **중앙값의 1.8배**다 —
  평균은 잡아내려는 이상값 자체에 끌려 올라간다.
  **거리와 연비가 둘 다** 넘어야 한다. 거리만 보면 장거리 여행을 잡는다 — 멀리 갔으면
  그만큼 넣었으므로 거리는 길어도 연비는 평소와 같다. 구간이 3개 미만이면 판단하지 않는다
  ("평소"가 없는데 의심부터 할 수는 없다)
- **뺀 구간의 숫자를 목록에서 지우지는 않는다.** 그 행에 `확인 필요`(값이 이상) 또는
  `기록 빠짐?`(구간이 비어 보임) 만 붙는다 — 무엇이 잘못됐는지 보려면 값이 남아 있어야 한다
- 목록 API 가 **`sort` 를 받지 않는다.** 정렬이 곧 계산의 전제라 서버가 `odometer DESC, id DESC` 로
  고정한다. 페이지 경계의 마지막 행은 짝이 다음 페이지에 있어 **한 건을 따로 더 조회한다**
- 지금은 단순법(매 주유마다 직전과 비교)이다. 만탱크 플래그가 없어 **가득 채우지 않은 주유가
  섞이면 그 구간만 실제보다 높게 나온다** — 백로그에 있고, 위의 이상 구간 감지가 대신하고 있다

### 다음 정비 시점

`ServiceType` enum 15종이 각각 `recommendedIntervalKm` / `recommendedIntervalMonths` 를 들고 있다.
해당 종류의 마지막 이력에 주기를 더해 **두 기준을 모두** 내려보내고, 먼저 오는 쪽이 실제 시기다.

`OTHER` 는 주기가 둘 다 `null` 이라 계산하지 않는다. **이력이 없는 종류는 응답에서 아예 뺀다** —
15종을 "기록 없음"으로 늘어놓으면 화면이 목록이 아니라 빈칸 더미가 된다.
종류 전체를 한 번에 주는 엔드포인트(`/next-services`)가 따로 있다. 종류가 5개일 때는
화면이 종류마다 한 번씩 불렀는데, 15개가 되면서 요청 15번이 되어 서버로 옮겼다.

## 기술 스택

**백엔드** — Spring Boot 3.5.6 / Java 17 / Gradle · Spring Data JPA (Hibernate 6.6) ·
MariaDB 12.3 (`mariadb-java-client`, `jdbc:mariadb://`) · springdoc-openapi 2.8 ·
`spring-boot-starter-mail`(비밀번호 재설정)

**프론트엔드** — React 19 / TypeScript 6 / Vite 8 · Tailwind CSS v4 (`@tailwindcss/vite`) +
shadcn/ui · vitest · **린터는 ESLint 가 아니라 oxlint** (Vite 템플릿 기본값, Rust 기반)

**일부러 안 쓴 것 셋**이 이 저장소의 성격을 말해 준다.

- **Lombok 없음** — 생성자·getter 를 직접 적는다. 무엇이 생성되는지 눈으로 보는 것이 목적이다
- **Spring Security 없음** — `spring-security-crypto`(BCrypt)만 쓰고 세션·CSRF·시도 제한·
  보안 헤더는 직접 만들었다. 공짜로 따라오던 것이 하나도 없다는 뜻이라 대가가 있다 → **보안** 참고
- **차트 라이브러리 없음** — 홈의 차트 2종은 HTML/CSS 로 그린다

설계 원칙과 그 근거는 `CLAUDE.md`, 완료한 작업의 기록은 `HISTORY.md` 에 있다.

## 실행 방법

### 준비물

Java 17+ · Node 26(`frontend/.nvmrc`) · MariaDB 12 · IntelliJ IDEA(백엔드를 여기서 띄운다).

### 1. 스키마와 계정

운영용 `odolog` 와 테스트용 `odolog_test` **둘 다** 필요하다. 테스트는 매 실행마다
`ddl-auto: create-drop` 으로 테이블을 지우고 다시 만들기 때문에 같은 스키마를 쓸 수 없다.

```sql
CREATE DATABASE odolog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'odolog'@'localhost' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON odolog.* TO 'odolog'@'localhost';

CREATE DATABASE odolog_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'odolog_test'@'localhost' IDENTIFIED BY 'odolog_test';
GRANT ALL PRIVILEGES ON odolog_test.* TO 'odolog_test'@'localhost';

FLUSH PRIVILEGES;
```

DDL 은 만들지 않아도 된다 — 운영 스키마는 `ddl-auto: update` 가 채운다.
권한을 `*.*` 가 아니라 스키마 단위로 좁힌 것은 이 계정이 새어도 같은 인스턴스의 다른 스키마로
번지지 않게 하기 위해서다. **`odolog_test` 의 자격증명은 `src/test/resources/application.yml` 에
그대로 적혀 있다** — 매번 비워지는 로컬 전용이라 숨길 값이 아니다.

### 2. 백엔드

IntelliJ 에서 `OdoLogApplication` 을 실행한다. 실행 구성(Run/Debug Configurations →
Environment variables)에 접속 정보를 넣어야 한다.

```
DB_USERNAME=odolog
DB_PASSWORD=<1단계에서 정한 비밀번호>
```

`application.yml` 에는 `${DB_USERNAME:root}` 형태만 있고 평문은 어디에도 없다 —
파일에 적으면 나중에 지워도 커밋 히스토리에 남는다.

`Started OdoLogApplication` 이 찍히면 <http://localhost:8080/swagger-ui.html> 에서 API 를
직접 쏴 볼 수 있다. Swagger UI 는 CSRF 토큰도 알아서 실어 보낸다(`springdoc.swagger-ui.csrf`).

터미널에서 띄워야 한다면 테스트 계정을 쓴다(운영 계정 비밀번호는 IntelliJ 안에만 있다):

```
SPRING_DATASOURCE_URL='jdbc:mariadb://localhost:3306/odolog_test' \
SPRING_DATASOURCE_USERNAME=odolog_test SPRING_DATASOURCE_PASSWORD=odolog_test ./gradlew bootRun
```

### 3. 프론트엔드

```
cd frontend
npm install
npm run dev     # http://localhost:5173
```

**포트 5173 은 고정이다.** 백엔드 `WebConfig` 의 CORS `allowedOrigins` 와 짝이라,
5173 이 이미 점유돼 Vite 가 5174 로 옮겨 뜨면 **모든 요청이 CORS 에서 막힌다.**

### 4. 검사

```
./gradlew test                  # 백엔드 202개
cd frontend && npm run test     # 프론트 39개 (vitest)
cd frontend && npm run lint     # oxlint
cd frontend && npm run build    # tsc -b + vite build
```

커밋마다 GitHub Actions 가 넷을 전부 돌린다(`.github/workflows/ci.yml`).
**CI 도 H2 가 아니라 MariaDB 컨테이너를 띄운다** — `ddl-auto` 가 만드는 스키마가 DB 구현마다
달라서, 다른 DB 로 검증하면 "테스트는 통과하는데 운영만 안 바뀌는" 상황이 생긴다.
이 저장소는 그걸 이미 두 번 겪었다.

### 선택 — 비밀번호 재설정 메일

```
MAIL_USERNAME=<보내는 주소>
MAIL_PASSWORD=<앱 비밀번호>      # Gmail 이면 2단계 인증 후 발급
```

**안 넣어도 앱은 뜬다.** 발송만 실패하고 로그에 `ERROR` 로 남는다 — 실패를 호출자에게
올려보내면 가입된 주소에서만 500 이 나서, 그 차이가 곧 가입 여부를 알려주기 때문이다.

### 선택 — 운영 프로파일

배포는 범위 밖이지만 **"올린다면 이렇게"** 는 코드로 준비해 뒀다.
`SPRING_PROFILES_ACTIVE=prod` 하나가 `application-prod.yml` 을 겹쳐 읽게 한다.

| 바뀌는 것 | 이유 |
|---|---|
| `springdoc.api-docs/swagger-ui` `enabled: false` | 문서에 인증이 없다. `CsrfTokenFilter` 도 `/api` 밖은 통과시켜 **관문이 하나도 없다** |
| `show-sql: false`, `org.hibernate.orm.jdbc.bind: info` | 개발 설정이 **이메일·전화번호·BCrypt 해시까지** 로그에 평문으로 찍는다 |
| `session.cookie.secure: true` | 세션 쿠키와 CSRF 쿠키가 같은 스위치를 읽는다 |

`@Profile("prod")` 를 코드에 다는 대신 yml 로 모은 이유는, 그래야 **운영과 개발의 차이가
파일 하나에 전부 보이기** 때문이다. 주석으로 "배포할 때 끄세요"라고 적는 것과의 차이는
더 크다 — 주석은 사람이 기억해야 하고, 프로파일은 환경변수가 대신 기억한다.

### ⚠️ 이미 쓰던 DB 가 있다면 — 정비 종류 확장 (2026-09-16)

정비 종류를 5개에서 15개로 늘리면서 `maintenance_records.type` 컬럼 타입을 바꿨다.
**새로 스키마를 만드는 경우에는 할 일이 없다.** 이 저장소를 예전부터 쓰던 DB 에만 한 번 실행한다.

```
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults \
  -e "USE odolog; ALTER TABLE maintenance_records MODIFY COLUMN type VARCHAR(30) NOT NULL;"
```

Hibernate 6 은 MariaDB 에서 `@Enumerated(STRING)` 을 varchar 가 아니라 네이티브
`enum('BATTERY','BRAKE_PAD',...)` 컬럼으로 만든다. `ddl-auto: update` 는 컬럼 타입을 바꿔 주지
않으므로 자바 enum 에 값을 더해도 DB 는 옛 5개 그대로고, **새 종류를 저장하는 순간 데이터 잘림
오류가 난다.** 테스트 스키마는 매번 새로 만들어지므로 `./gradlew test` 는 멀쩡히 통과한다 —
**테스트로는 절대 못 잡는 종류의 문제**다.

`@JdbcTypeCode(SqlTypes.VARCHAR)` 로 컬럼을 varchar 에 고정했으므로 이 ALTER 는 한 번이면 되고,
enum 은 값을 문자열로 저장하므로 기존 데이터는 보존된다.

## 프로젝트 구조

백엔드와 프론트엔드 모두 기능별(package-by-feature)로 구성되어 있다.

    odolog/
    ├── src/                       백엔드 (Spring Boot)
    │   └── main/java/com/odolog/app/
    │       ├── user/              회원가입, 로그인/로그아웃, 프로필, 비밀번호 변경
    │       ├── vehicle/           차량 등록·조회·수정·주행거리 갱신·삭제
    │       ├── maintenance/       정비 이력, 다음 정비 시점 계산
    │       ├── fuel/              주유 기록, 연비 계산
    │       ├── account/           회원 탈퇴 — 여러 기능의 삭제 순서를 조율한다
    │       ├── summary/           홈 화면 요약 — 여러 기능을 읽어서 합친다
    │       └── common/            인증(세션·CSRF·시도 제한), 보안 응답 헤더,
    │                              전역 예외 처리, 설정, BaseTimeEntity
    └── frontend/                  프론트엔드 (Vite + React + TypeScript)
        └── src/
            ├── app/               라우트 정의, Header, ProtectedRoute
            ├── features/          auth / vehicles / maintenance / fuel
            └── shared/            api 클라이언트, 포맷 함수, 공용 훅, UI 컴포넌트

백엔드의 각 기능 패키지는 `domain / repository / dto / service / controller`로 나뉘고,
그 아래 한 겹이 더 있다 — 파일의 성격을 폴더 이름으로 드러내는 층이다.

    user/
    ├── domain/entity/User.java
    ├── repository/jpa/UserRepository.java
    ├── dto/request/signup/SignUpRequest.java
    ├── dto/request/login/LoginRequest.java
    ├── dto/request/profile/UpdateProfileRequest.java
    ├── dto/response/profile/UserResponse.java
    ├── service/application/UserService.java
    └── controller/rest/UserController.java

`domain/entity` 와 `domain/type`(enum), `repository/jpa`(구현 기술),
`dto/request/<유스케이스>`, `controller/rest`(노출 방식) 같은 식이다.
테스트도 같은 경로를 그대로 따라간다.

프론트엔드의 각 기능 폴더는 `api`(엔드포인트 + 그 기능의 DTO 타입)와
`pages`(라우트가 있는 화면) 또는 `components`(다른 화면에 얹히는 조각)로 나뉘고,
역시 그 아래 한 겹이 더 있다 (`pages/login/LoginPage.tsx`,
`api/endpoints/endpoints.ts`). `auth`에는 로그인 상태를 들고 있는 `context`가 추가로 있다.
`shared/ui` 는 성격별로 `base`(shadcn이 복사해 넣는 자리) / `form` / `layout` /
`feedback` / `nav` / `brand` 로 나뉜다. `form/date-input.tsx` 는 기기에 따라 **터치면 드럼 휠,
아니면 네이티브 date 입력**으로 갈린다 — 폰에서 OS 캘린더는 달을 여러 번 넘겨야 하고,
데스크톱에서는 날짜를 타이핑하는 게 제일 빠르기 때문이다.

프론트엔드의 의존 방향은 `app → features → shared` 한 방향이다. `app`은 여러 기능을 동시에
알아도 되는 유일한 층이라, `useAuth`를 쓰는 `Header`와 `ProtectedRoute`가 여기에 있다.

백엔드는 `fuel → vehicle → user`, `maintenance → vehicle → user` 이고, **`account` 와 `summary` 가
프론트의 `app` 과 같은 자리**다 — 여러 기능을 동시에 알아도 되는 층이다.
회원 탈퇴는 회원·차량·정비 이력·주유 기록을 모두 지워야 하는데, 이걸 `UserService` 에 넣으면
`user → vehicle` 역방향 의존이 생긴다.

둘은 하는 일이 달라서 주입받는 것도 다르다. `account` 는 **순서를 조율**하므로 각 기능의
**서비스**를 받아 실제 삭제는 그쪽에 맡기고, `summary` 는 **읽어서 합치기**만 하므로
**리포지토리**를 직접 받는다 — 집계에는 소유권 검사나 삭제 순서 같은 규칙이 필요 없다.

시간 필드(`createdAt`/`updatedAt`)는 네 엔티티가 `common/domain/entity/BaseTimeEntity` 를
상속해서 얻는다. 엔티티가 3개일 때는 `@PrePersist` 를 복사하는 편이 나았고, 4개째에서 뒤집혔다.

설계 결정과 지켜야 할 규칙은 `CLAUDE.md`, 완료한 작업과 그 근거는 `HISTORY.md` 에 있다.

프론트엔드 실행 방법은 `frontend/README.md` 참고.

## API 개요

| 기능 | 엔드포인트 |
|---|---|
| 회원가입 | `POST /api/users` |
| 로그인 | `POST /api/users/login` |
| 로그아웃 | `POST /api/users/logout` |
| 내 정보 조회/수정 | `GET`, `PATCH /api/users/me` |
| 비밀번호 변경 | `PATCH /api/users/me/password` |
| 비밀번호 재설정 요청/확정 | `POST`, `PATCH /api/users/password-reset` |
| 내 기록 내보내기 | `GET /api/users/me/export` |
| 회원 탈퇴 | `DELETE /api/users/me` |
| 홈 요약 (통계·차트·최근 활동) | `GET /api/summary` |
| 차량 등록/목록조회 | `POST`, `GET /api/vehicles` |
| 차량 상세조회 | `GET /api/vehicles/{vehicleId}` |
| 차량 정보 수정 | `PATCH /api/vehicles/{vehicleId}` |
| 주행거리 갱신 | `PATCH /api/vehicles/{vehicleId}/odometer` |
| 차량 삭제 | `DELETE /api/vehicles/{vehicleId}` |
| 정비 이력 등록/목록조회 | `POST`, `GET /api/vehicles/{vehicleId}/maintenance-records` |
| 정비 이력 수정/삭제 | `PATCH`/`DELETE /api/vehicles/{vehicleId}/maintenance-records/{recordId}` |
| 다음 정비 시점 조회 | `GET /api/vehicles/{vehicleId}/maintenance-records/next-services` |
| 주유 기록 등록/목록조회 | `POST`, `GET /api/vehicles/{vehicleId}/fuel-records` |
| 주유 기록 수정/삭제 | `PATCH`/`DELETE /api/vehicles/{vehicleId}/fuel-records/{recordId}` |
| 연비 요약 조회 | `GET /api/vehicles/{vehicleId}/fuel-records/summary` |

### 공통 규약

| | |
|---|---|
| 인증 | 세션 쿠키(`JSESSIONID`). 로그인 없이 부르면 **401** |
| CSRF | 쓰기 요청에 `X-XSRF-TOKEN` 헤더 필요. 없으면 **403**. Swagger UI 는 알아서 실어 보낸다 |
| 시도 제한 | 로그인 · 재설정 요청 · 회원가입에 걸려 있다. 걸리면 **429** |
| 남의 자원 | **404**. 403 을 주지 않는다(존재 자체를 숨긴다) |
| 페이지네이션 | `PageResponse<T>` — `items` / `page` / `size` / `totalElements` / `totalPages` / `hasNext` |
| 에러 본문 | `{"message": "..."}` **하나뿐이다.** 어느 필드가 틀렸는지는 아직 못 알려준다(백로그) |
| 검증 실패 | **400**. 여러 필드가 틀려도 `findFirst()` 로 **하나만** 내려간다 |

근거는 위 **보안** 에 모아 뒀다. 아래는 API 를 쓸 때 걸려 넘어지기 쉬운 것 넷이다.

**1. 주유 목록은 `sort` 를 받지 않는다.** 정렬이 곧 연비 계산의 전제라 서버가
`odometer DESC, id DESC` 로 고정한다. 다른 목록 API 는 `sort` 를 받고, 잘못된 속성명을 주면
500 이 아니라 **400** 이다(`PropertyReferenceException` 을 매핑해 뒀다).

**2. `efficiency`·`distance` 는 계산할 수 없으면 `0` 이 아니라 `null` 이다.**
첫 기록이거나, 연비 초기화 기준점이거나, odometer 가 직전보다 크지 않은 경우다.
`0` 으로 내려보내면 "연비가 0 인 차"와 구분되지 않는다.

**3. 정비·주유 날짜는 미래를 받지 않는다**(`@PastOrPresent`, 400). 오늘은 통과한다 —
가장 흔한 입력이라 여기서 막으면 기능이 멈춘다. 미래 기록을 허용하면 그 행이 목록 맨 위에
고정되고 **다음 정비 시점까지 그 값으로 계산된다.**

**4. 주행거리는 줄이면 409 다.** 계기판은 되돌아가지 않기 때문이다. 다만 자리수 오타나
계기판 교체까지 막으면 되돌릴 길이 없어지므로, **의도를 밝히면** 낮출 수 있다.
화면은 입력값이 현재보다 작을 때 한 번 확인한 뒤 이 플래그를 싣는다.

```json
PATCH /api/vehicles/1/odometer
{"odometer": 50000}                 → 409 (현재보다 작으면)
{"odometer": 50000, "force": true}  → 200
```

같은 "주행거리를 올린다"라도 경로가 셋이라 엔티티 메서드도 셋이다 —
`updateOdometer()`(감소 시 409) · `liftOdometerTo()`(크면 올리고 작으면 무시.
정비·주유를 기록할 때 따라오는 경로) · `correctOdometer()`(감소도 그대로. `force` 전용).

## 보안

Spring Security 를 안 쓰므로 **세션·CSRF·시도 제한·보안 헤더가 전부 직접 만든 것**이다.
무엇을 왜 그렇게 했는지, 그리고 **아직 안 한 것**까지 적어 둔다.

### 비밀번호

BCrypt 해시만 저장한다(`spring-security-crypto`). 길이 상한은 **UTF-8 72바이트** —
BCrypt 자체의 한계다. `@Size` 는 글자 수를 세므로 한글에서 3배로 벌어져
`@Size(max = 100)` 이 한글 25자(75바이트)를 통과시켰고 인코딩 단계에서 500 이 났다.
그래서 바이트로 세는 `@MaxBytes` 를 따로 만들었다. 가입·변경·재설정 **세 곳의 제한이 같다** —
한 곳만 느슨하면 그쪽이 우회로가 된다.

### 세션

로그인 성공 시 `HttpSession` 에 사용자 id 만 담고 **`changeSessionId()` 로 세션 ID 를 교체**한다
(세션 고정 방어). 요청 바디나 URL 의 사용자 id 는 어디서도 신뢰하지 않고,
`@LoginUser` 아규먼트 리졸버가 **세션에서만** 꺼낸다.

쿠키 속성은 브라우저 기본값에 기대지 않고 명시한다 — `HttpOnly` · `SameSite=Lax` ·
`secure`(운영 프로파일에서 켜짐). 저장소는 톰캣 인메모리라 **재시작하면 전원 로그아웃**이다.

### CSRF — double submit 쿠키

`CsrfTokenFilter` 가 `XSRF-TOKEN` 쿠키를 발급하고, 상태를 바꾸는 메서드
(GET·HEAD·OPTIONS·TRACE 외)에는 같은 값이 `X-XSRF-TOKEN` 헤더로 돌아왔는지 본다. 없으면 403.

**세션 보관 방식(synchronizer token)을 안 쓴 이유**는 토큰을 내주려면 세션이 있어야 하고,
그러면 비로그인 방문자에게도 세션이 생기기 때문이다. 여기는 비교만 하므로 서버가 아무것도 안 든다.

성립 근거는 둘이다 — 다른 출처의 스크립트는 이 쿠키를 **읽을 수 없고**, 커스텀 헤더는
프리플라이트를 통과해야 붙는데 CORS 가 `localhost:5173` 만 허용한다.
**한계**: 같은 사이트의 하위 도메인이 장악되면 쿠키를 심을 수 있다. 그때는 세션 보관 방식이 맞다.

테스트에서는 `odolog.csrf.enabled=false` 로 꺼 둔다. `@WebMvcTest` 가 Filter 빈을 함께 올려서,
켜 두면 기존 쓰기 테스트 30여 개가 **검증하려는 것과 무관한 이유로** 전부 403 이 된다.
필터 자체는 `CsrfTokenFilterTest` 가 직접 호출해서 본다.

### 시도 제한 (`LoginAttemptLimiter`)

| 대상 | 키 | 한도 |
|---|---|---|
| 로그인 | 이메일(소문자·trim) | 10분 내 10회 실패 → 10분 잠금 |
| 재설정 요청 | `password-reset:` + 이메일 | 같음 |
| 회원가입 | `signup:` + **remote IP** | 같음 |

가입만 IP 로 세는 이유는 **반복되는 주체가 받는 주소가 아니라 보내는 쪽**이기 때문이다.
이메일로 세면 매번 다른 주소를 넣는 열거자는 카운터가 늘 1 이라 그냥 빠져나간다.
**성공한 가입도 센다** — 409 만 세면 아직 없는 주소를 찔러 보는 쪽이 안 걸리는데,
그쪽은 계정을 실제로 만들어 버려 더 나쁘다.

`X-Forwarded-For` 는 읽지 않는다. 클라이언트가 적는 값이라 **그 값을 덮어쓰는 신뢰 가능한
프록시가 앞에 있을 때만** 뜻이 있고, 없는 지금 읽으면 헤더 한 줄로 제한을 우회하게 만들 뿐이다.

잠겼을 때의 문구는 리미터가 아니라 **부르는 쪽이 넘긴다.** 안 그러면 가입 화면에
"로그인 시도가 너무 많습니다"가 뜬다. 인메모리라 **재시작하면 잠금도 함께 풀린다.**

### 사용자 열거 (user enumeration)

- 로그인 실패는 사유를 통일한다 — 없는 계정이든 비밀번호 불일치든 같은 메시지
- **없는 계정의 실패도 카운트한다.** 안 세면 "빨리 답하는 쪽"이 곧 없는 계정이다
- 재설정 요청은 가입 여부와 무관하게 **204**, **메일 발송 실패도 삼킨다**(로그로만 남긴다)
- 남의 차량은 403 이 아니라 **404** 다. 403 은 "권한 없음"과 동시에 **"존재함"** 을 나르고,
  id 가 연속이라 둘이 갈리면 훑어서 사용 중인 번호를 셀 수 있다. **응답 문구까지 동일하다** —
  상태 코드만 맞추면 메시지가 대신 알려준다. 정비·주유는 `findByIdAndVehicleId` 라 원래 404 하나였다

**예외는 회원가입 하나다.** 이미 가입된 주소에 409 를 준다 — 가입하려는 사람에게는 이게 맞는
안내이고, 없애려면 가입을 메일 확인 흐름으로 바꿔야 하는데 **메일 설정이 없으면 아무도 가입을
끝내지 못하게 된다.** 응답은 그대로 두고 **쓸어 보는 것만 위의 IP 제한으로 막았다.**

### 비밀번호 재설정 토큰

256비트 `SecureRandom` 값을 메일로 한 번 보내고, DB 에는 **SHA-256 해시**를 저장한다
(BCrypt 가 아닌 이유: 우리가 만든 고엔트로피 난수라 사전 공격 대상이 아니고,
BCrypt 는 같은 값도 매번 다른 해시를 내놓아 조회 키로 못 쓴다).
**30분 · 1회용**(`used_at`)이고, 재발급하면 이전 토큰은 삭제된다.
확정에 성공해도 **자동 로그인시키지 않는다** — 메일 링크를 누른 사람이 계정 주인이라고 단정하지 않는다.

### 보안 응답 헤더 (`SecurityHeadersFilter`)

| 헤더 | 값 | 비고 |
|---|---|---|
| `X-Content-Type-Options` | `nosniff` | |
| `X-Frame-Options` | `DENY` | 클릭재킹 |
| `Referrer-Policy` | `no-referrer` | 재설정 토큰이 쿼리스트링에 실린다 |
| `Strict-Transport-Security` | `max-age=31536000` | **`request.isSecure()` 일 때만** — http 에서 붙이면 도메인이 https 전용으로 굳는다 |

`/reset-password?token=…` 은 프런트가 띄우는 화면이라 백엔드 헤더가 닿지 않는다.
그래서 `frontend/index.html` 에 `<meta name="referrer" content="no-referrer">` 를 따로 넣었다.
**프런트의 클릭재킹 방어는 저장소 안에서 못 한다** — 정적 호스팅의 헤더 설정이 할 일이다.

### 아직 안 한 것

로컬 단독 사용에서는 문제가 아니지만, 올린다면 순서대로 따져야 할 것들이다.

- **비밀번호 변경·재설정이 다른 세션을 끊지 않는다.** 계정 탈취를 당했을 때 비밀번호 변경이
  상대를 쫓아내지 못한다. 톰캣 인메모리 세션은 "이 사용자의 세션 전부"를 열거할 수 없어
  **지금 구조로는 고칠 수 없다** — Spring Session(JDBC) 도입이나 `passwordChangedAt` 비교가 필요하다
- **세션·잠금 기록이 재시작으로 사라진다.** 배포가 잦으면 시도 제한에 구멍이 생긴다
- **이메일 단위 잠금이라 표적 잠금이 가능하다.** 남의 주소로 10번 틀리면 10분간 잠글 수 있다
- **`verifyPassword`(비밀번호 변경·탈퇴)에는 시도 제한이 없다.** 세션을 쥔 상태에서만 닿지만,
  "되돌릴 수 없는 동작 앞의 관문"이 무제한인 건 앞뒤가 맞지 않는다
- **로그인 실패 응답 시간이 계정 유무에 따라 다르다.** 없는 이메일은 BCrypt 검증을 건너뛰어
  더 빨리 답한다 — 위 방침과 어긋나는 마지막 틈이다
- **페이지 크기 상한이 없다.** `@PageableDefault(size = 20)` 은 기본값일 뿐이고
  `?size=2000`(스프링 기본 상한)이 통과한다

## 진행 상황

백엔드 API **27개**와 프론트엔드 화면 10장(라우트 기준. `/` 가 세 얼굴을 가져 실제로 볼 상태는
13개)이 모두 동작하는 상태다. 백엔드 테스트 **202개**, 프론트엔드 테스트 **39개**가 통과하고,
프론트엔드는 `tsc -b` / `oxlint` / `vite build` 도 통과한다.
커밋마다 GitHub Actions 가 이 넷을 전부 돌린다 (`.github/workflows/ci.yml`).

남은 것은 **브라우저 실동작 확인**과, 할지 말지부터 정해야 하는 두 가지(토스트 / 필드별 에러)뿐이다.

| 단계 | 내용 | 상태 |
|---|---|---|
| Phase 1 | 백엔드 API 표면 (단건 조회, 페이지네이션, CORS, Swagger) | 완료 |
| Phase 2 | 프론트엔드 셋업 (Vite/Tailwind/shadcn/API 클라이언트) | 완료 |
| Phase 3 | 인증 화면 (회원가입·로그인·보호 라우트) | 완료 |
| Phase 4 | 차량 관리 화면 | 완료 |
| Phase 5 | 정비 이력 화면 + 다음 정비 시점 | 완료 |
| Phase 6 | 다듬기 (로딩·에러·반응형·접근성) | 코드는 끝, **눈 확인만 남음** |

Phase 6 이후에 기능이 더 붙었다.

| 추가 | 내용 | 날짜 |
|---|---|---|
| 차량 정보 수정 | 번호판·제조사·모델·연식. 그전에는 주행거리만 고칠 수 있었다 | 2026-09-16 |
| 비밀번호 변경 | 현재 비밀번호를 확인하는 관문 | 2026-09-16 |
| 회원 탈퇴 | 계정과 딸린 데이터 전부. `account` 조율 층 신설 | 2026-09-16 |
| `@EnableJpaAuditing` | `BaseTimeEntity` 로 시간 필드 일원화 | 2026-09-16 |
| **주유 기록과 연비** | 주유 CRUD + km/L 계산 + 요약. 차량 주행거리 자동 갱신 | 2026-09-16 |
| 정비 종류 5 → 15개 | 미션오일·냉각수·점화 플러그·타이밍 벨트 등. `type` 컬럼을 varchar 로 | 2026-09-16 |
| 점검 결함 5건 수정 | 홈 통계 유류비 누락, 주유 수정 시 주행거리, 랜딩, 죽은 코드 | 2026-09-16 |
| 날짜 드럼 휠 | 폰에서 년·월·일을 굴려서 고른다. 데스크톱은 네이티브 date 입력 유지 | 2026-09-17 |
| UI 버그 5건 수정 | 휠 스크롤 연쇄·화면 밖 펼침·깨진 값, 연식 required, 표 가로 스크롤 | 2026-09-17 |
| 홈 요약 API | 요청 7번 → 1번, 합계의 200건 상한 제거. 프론트 계산 258 → 81줄 | 2026-09-17 |
| 연비 초기화 | 기록을 지우지 않고 "여기서부터 다시" 기준점을 찍는다 | 2026-09-17 |
| 정비도 주행거리 갱신 | 규칙을 `Vehicle.liftOdometerTo()` 로 옮겨 주유·정비가 공유 | 2026-09-17 |
| 안내 문구 5건 정정 | 삭제·탈퇴 설명이 주유 기록을 빼먹고 있었다 | 2026-09-17 |
| 빠진 기록 감지 | 평소보다 긴 구간을 중앙값으로 찾아 알려 준다 + 과거 기록 입력 안내 | 2026-09-17 |
| 점검 결함 3건 수정 | 주행거리 정정 경로, 미래 날짜 차단, 정비 메모 길이 | 2026-09-18 |
| 평균 연비 오염 수정 | 불가능한 구간을 평균에서 빼고 뺀 개수를 알린다 | 2026-09-18 |
| 연비 카드 중복 수정 | 형제 `key` 충돌. 아래 트러블슈팅 참고 | 2026-09-18 |
| 정비 비용 `required` | 비우면 0원으로 덮어써지던 것 | 2026-09-18 |
| 주행거리 안내 | 주유·정비 폼에서 비우면 왜 필요한지 말해 준다 | 2026-09-19 |
| 주행거리 미수정 경고 | 미리 채워진 값 그대로 저장하면 아무 계산도 안 된다고 묻는다 | 2026-09-19 |
| 주행거리 갱신 폼 동기화 | 정비·주유로 차량 값이 오르면 입력칸도 따라간다 | 2026-09-19 |
| 점검 결함 5건 수정 | 연비 카드 레이아웃 점프, 폼 기준값 고정, 포커스, 폼 닫힘, 스크린리더 | 2026-09-19 |
| 디자인 규칙 3건 수정 | 안 보이던 글자, 카드 제목을 heading 으로, 타입 스케일 토큰 둘 신설 | 2026-09-19 |
| `cn` 이 지우던 크기 복원 | 카드 제목 굵기·라벨 크기가 DOM 에 없던 문제. 아래 트러블슈팅 | 2026-09-19 |
| 비밀번호 재설정 · 내보내기 | 메일 링크로 재설정, 탈퇴 전 기록을 JSON 으로 챙기기 | 2026-09-21 |
| 배포 전 보안 셋 | CSRF 토큰 · 로그인 시도 제한 · 쿠키 `secure` 스위치 | 2026-09-21 |
| **전체 점검 + 보안 보강** | 운영 프로파일(문서·로그·쿠키) · 보안 응답 헤더 · CSRF 쿠키 `secure` | 2026-09-22 |
| 사용자 열거 좁히기 | 가입 시도 IP 제한 · 남의 차량도 404 · 잠금 문구를 상황별로 | 2026-09-22 |
| **빠진 구간을 평균에서 제외** | 기록을 지우면 다음 구간 연비가 뛰던 문제. 탐지는 하고 있었으나 평균과 연결이 안 돼 있었다 | 2026-09-23 |

Phase 6에서 **이미 끝난 것**. 아래 "남은 작업"에 다시 적지 않는다.

- 조회 로직 공용 훅 `useAsyncData` 추출 (화면 4곳의 중복 제거)
- 로딩·에러·안내 표시 통일 — `LoadingText` / `ErrorText` / `NoticeText` / `Skeleton`
- 삭제 버튼 중복 클릭 잠금 (정비 이력·차량 삭제 2곳)
- 숫자·날짜 포맷 — `formatKm` / `formatWon` / `formatDate` / `formatCompact`,
  `tabular-nums` 를 어디에 붙이고 어디에 안 붙이는지까지 규칙으로 확정
- 목록 스켈레톤 — 차량 목록 · 차량 상세 · 정비 이력 · 다음 정비 시점 4곳
- 반응형 — `sm:`/`md:`/`lg:`/`xl:` 57곳. 모바일 폭부터 데스크톱 2단 레이아웃까지
- 접근성 기본 — `Field` 가 `label`-`htmlFor` 짝을 강제, 페이지 이동 버튼 `aria-label`,
  `ErrorText` 에 `role="alert"`, `focus-visible` 링 유지, `prefers-reduced-motion` 대응,
  차트 값을 말풍선에만 가두지 않고 `<details>` 표와 `tabIndex` 로도 제공
- 문서 제목(`<title>오도로그</title>`)과 파비콘 — 파비콘은 Vite 기본 로고에서 계기판 로고로 교체 완료
- 라이트/다크/시스템 테마, FOUC 방지, View Transition 전환
- UI/UX 전면 재설계 (모노톤 디자인 토큰), 홈 통계 화면과 차트 2종, 소개(랜딩) 화면
- **전체 점검에서 찾은 결함 12건 수정 (2026-09-13)** — 아래에 무엇이었는지 남겨 둔다

### 수정한 결함 12건 (2026-09-13)

전부 **실행 중인 앱에 실제 요청을 보내거나 코드 경로를 따라가 재현한 것**이다.
백엔드 테스트는 56 → 62개가 됐고, 새 테스트 6개는 **고치기 전 코드에 돌려서 실제로 실패하는
것까지 확인**했다.

| # | 무엇이 문제였나 | 어떻게 고쳤나 |
|---|---|---|
| 1-1 | 정비 이력 등록에서 `cost`/`serviceOdometer` 를 빼고 보내면 `int` 라 Jackson이 0을 채워 201 | `@NotNull Integer` |
| 1-2 | 주행거리 갱신에 `{}` 를 보내면 0으로 해석 → 영문 모를 409 | `@NotNull Integer` |
| 1-3 | 닉네임을 `""` 로 수정 가능 (`@Size(max=30)` 은 빈 값을 막지 않는다) | `@Size(min=1,max=30)` + `@Pattern` (공백만도 차단) |
| 1-4 | `?sort=nonexistent` 가 500 | `PropertyReferenceException` → 400 |
| 2-1 | 정비 이력 마지막 페이지의 마지막 1건을 지우면 빈 페이지에 갇힘 (돌아갈 버튼도 사라짐) | 그 페이지의 마지막 항목이었으면 한 장 물러난다 |
| 2-2 | 로그아웃 요청이 실패하면 상태도 안 비고 이동도 안 해 "아무 일도 안 일어남" | `AuthProvider.logout` 이 `finally` 로 상태를 비운다 |
| 3-1 | 정비 이력 목록 정렬에 동점 기준이 없어 페이지 경계에서 행이 중복·누락될 수 있음 | `sort = {"serviceDate", "id"}` |
| 3-2 | 중복 검사와 저장 사이에 끼어든 요청이 DB 유니크 제약에 막히면 500 | 유니크 위반일 때만 409 (NOT NULL 위반은 그대로 500) |
| 4-1 | 최고 비용이 동점이면 차트 라벨이 두 개 | 최고값을 가진 첫 칸만 |
| 4-2 | 전화번호를 안 적어도 `""` 로 저장돼 nullable 컬럼에 null 이 안 생김 | 빈 값은 보내지 않고, 프로필 수정도 빈 값을 null 로 저장 |
| 4-3 | `MaintenanceRecord.type` 의 `@Column(length = 20)` 이 실제 DDL에서 무시됨 (네이티브 `enum(...)` 컬럼) | 숫자를 지웠다 |
| 4-4 | 안 쓰는 `@fontsource-variable/geist` 가 `package.json` 에 남아 있음 | `npm uninstall` |

**3-2 는 추측으로 고칠 수 없었다.** JPA를 거치면 `DuplicateKeyException` 이 아니라
`DataIntegrityViolationException` 이 올라온다 — 실제로 던져 보고 확인한 뒤에야 핸들러가 맞았다.
그 예외는 NOT NULL 위반 같은 우리 쪽 버그도 함께 타고 오므로, cause 가 Hibernate
`ConstraintViolationException` 이고 그 `kind` 가 `UNIQUE` 일 때만 409로 내린다.
`UserRepositoryTest` 가 그 "예외의 모양"을 못박아 두고 있다.

## 남은 작업

**코드로 잡을 수 있는 결함은 전부 고쳤다**(위 "수정한 결함 12건"). 여기 남은 셋은 성격이 다르다.
0번은 코드로 판정할 수 없어 사람이 직접 눌러 봐야 하고, 1·2번은 **무엇을 만들지가 아니라
할지 말지부터 정해야 하는** 것이다.

### 0. 브라우저 실동작 확인 (사람만 할 수 있다)

여기까지 코드는 다 있지만 **브라우저에서 끝까지 눌러본 적이 없다.**
백엔드(IntelliJ `OdoLogApplication`)와 프론트(`cd frontend && npm run dev`)를 함께 띄우고
`http://localhost:5173` 에서 확인한다.

아래는 요약이다. **항목별로 "무엇을 봐야 하는지"까지 쪼갠 176개짜리 전체 목록은
`CLAUDE.md` 의 Phase 6** 에 있다 (준비 6 · 기능 한 바퀴 127 · 폭 22 · 테마 10 · 접근성 11).

기능:

- [ ] 로그인하지 않은 상태로 `/` 접속 → 소개 화면이 보이는지
- [ ] 회원가입 → 가입 직후 자동 로그인되어 차량 목록으로 이동
- [ ] DevTools → Application → Cookies 에 `JSESSIONID` 가 있는지
- [ ] 새로고침해도 로그인 유지 (`GET /api/users/me` 1회 호출로 복구)
- [ ] 로그아웃 → 주소창에 `/vehicles` 직접 입력 → `/login` 으로 이동
- [ ] 로그인 후 `/` → 차량이 0대면 "첫 차량 등록하기", 1대 이상이면 통계 화면
- [ ] 차량 등록 → 목록에 보임 → 상세 진입
- [ ] 같은 번호판을 한 번 더 등록 → 409 가 폼 에러로 표시
- [ ] 주행거리 갱신 성공
- [ ] **더 작은 값**으로 갱신 → 409 가 아니라 **확인 창**이 뜨고, 확인하면 실제로 낮아지는지
      (자리수를 잘못 넣었을 때 되돌릴 수 있는 유일한 길이다)
- [ ] 정비·주유 날짜 칸에서 **내일 이후를 고를 수 없는지**
- [ ] 주행거리를 한 자리 크게 적은 주유 기록을 만들었을 때 **평균 연비가 멀쩡한지**
      (그 행에는 `확인 필요`, 카드 아래에는 "계산할 수 없는 구간 N곳을 평균에서 뺐습니다")
- [ ] 정비 이력 등록 → 목록에 보이고 "다음 정비 시점" 카드가 갱신됨
- [ ] 다음 정비 시점이 **주행거리·날짜 두 기준** 모두 표시되는지
- [ ] `OTHER`(기타) 종류는 권장 주기가 없어 계산되지 않는 것이 맞는지
- [ ] 정비 이력 수정 — 비용만 바꿨을 때 그 필드만 PATCH 되는지 (Network 탭 확인)
- [ ] 정비 이력 삭제 (204)
- [ ] **차량 정보 수정** — 제조사만 바꿨을 때 **409 가 나지 않는지**(번호판이 안 바뀌었으면
      중복 검사를 건너뛰어야 한다. 이 기능의 핵심 함정이다)
- [ ] **미션오일 등 새 종류**로 정비 이력이 등록되는지 (⚠️ 위 "정비 종류 확장" 참고)
- [ ] 정비 종류 선택 목록이 **부위별로 묶여** 보이는지(엔진·구동 / 제동 / …)
- [ ] "다음 정비 시점" 카드가 **이력 있는 종류만** 보여주는지. 요청은 **1번**인지(Network 탭)
- [ ] **주유 기록 등록** → 첫 기록이 `기준 기록 · 다음 주유부터 계산` 으로 **이유까지 말하는지**
      (`0.00` 이면 잘못된 것이고, `—` 만 있어도 낡은 화면이다)
- [ ] **두 번째 주유 기록** → 연비가 나오는지. 주행거리 500km · 25L 이면 **20.00 km/L**
- [ ] **주유가 차량 주행거리를 따라 올리는지** — 위쪽 히어로 숫자가 그만큼 굴러가야 한다
- [ ] 주유 기록 11건 이상 → **2페이지 첫 행에도 연비가 나오는지**(그 행의 짝은 1페이지에 있다)
- [ ] 차량 주행거리보다 **작은 값**을 넣으면 도움말이 바뀌는지. ⚠️ **막히지 않고 저장되는지** —
      지난달 영수증을 정리하는 건 정상적인 사용이다
- [ ] **연비 초기화** → 평균이 사라지고 "다음 주유 기록부터 다시 계산합니다" 로 바뀌는지.
      ⚠️ **기록·주유량·유류비 통계는 그대로인지**(연비만 다시 세는 것이지 지출을 지우는 게 아니다)
- [ ] 초기화 뒤 한 건 더 넣으면 **"초기화 이후 구간만 계산한 값입니다"** 가 붙는지.
      `초기화 해제` 로 전체 평균이 돌아오는지
- [ ] 중간 주유를 지워 구간을 두 배로 만들면 **"평소보다 긴 구간이 N곳 있습니다"** 가 뜨는지.
      다시 채워 넣으면 사라지는지
- [ ] 홈의 `최근 활동` 에 **정비와 주유가 함께** 나오는지. `차량별` 에 평균 연비가 붙는지
- [ ] 홈 `Cost` 타일 아래에 **`정비 N · 주유 N`** 구성이 보이는지
- [ ] **비밀번호 변경** — 현재 비밀번호를 틀렸을 때 **로그아웃되지 않는지**
- [ ] 로그아웃 후 **새 비밀번호로** 로그인되는지
- [ ] **회원 탈퇴** — 비밀번호 확인 후 `/` 로 이동, 그 계정으로 로그인 불가
- [ ] 차량 삭제 → **정비 이력과 주유 기록이** 함께 사라짐
- [ ] 다른 계정으로 로그인 → 앞 계정의 차량이 보이지 않음

**폰에서만 확인할 수 있는 것** (브라우저 반응형 모드는 `pointer: coarse` 를 흉내 내지 못한다):

- [ ] 날짜 칸이 캘린더가 아니라 **드럼 휠**(년/월/일)로 뜨는지
- [ ] 칸을 **끝까지 세게 굴려도 페이지가 안 튀는지**(스크롤 연쇄 차단)
- [ ] 폼 맨 아래 날짜 칸에서 눌렀을 때 휠이 **화면 안으로 따라 들어오는지**
- [ ] 1월 31일 → 2월로 굴리면 일이 **28(윤년이면 29)로 끌려오는지**

화면(아직 아무도 눈으로 본 적이 없는 부분):

- [ ] **테마 3가지**: 라이트 / 다크 / 시스템(OS 설정을 바꿨을 때 따라오는지)
- [ ] **다크로 두고 새로고침했을 때 흰 화면이 번쩍이지 않는지**(FOUC).
      `index.html` 의 인라인 스크립트가 막고 있는 지점이다
- [ ] **폭 3가지**: 375px / 1024px / 1440px. 특히 차량 상세의 2단이 `lg` 아래에서 풀려 쌓이는지
- [ ] 홈 차트 — 막대가 자라는 연출, 가장 높은 달에만 값이 붙는지, `<details>` 표가 열리는지
- [ ] 긴 닉네임(30자)으로 로그인했을 때 헤더가 밀리지 않는지
- [ ] 긴 메모를 넣은 정비 이력이 격자를 넘치게 만들지 않는지

### 1. 에러 표시 구분 (인라인 vs 토스트)

지금은 모든 에러가 `ErrorText` 인라인이다. 토스트는 아직 한 줄도 없다.

- [ ] 기준 확정: **폼 검증 실패(400/409)는 인라인**, 그 외(500·네트워크 끊김)는 토스트
- [ ] `ApiError.status` 로 분기 — `shared/api/client/client.ts` 가 이미 status 를 들고 있다
- [ ] 토스트 구현 선택: shadcn `sonner` 도입 vs 직접 만든 최소 구현
      (도입한다면 `components.json` 의 `aliases.ui` 가 `@/shared/ui/base` 를 가리키는지 먼저 확인)
- [ ] Provider 를 `main.tsx` 의 어느 층에 끼울지 (`AuthProvider` 안쪽/바깥쪽)
- [ ] 전환 1순위: 행동 실패(`actionError`) — 정비 이력 삭제, 차량 삭제
- [ ] 조회 실패(`error`)는 인라인 유지 — 화면에 대체할 내용이 없어 계속 보여야 한다

### 2. 백엔드 400 응답을 폼 필드에 연결 (할지 말지부터 결정)

현재 `ErrorResponse` 는 `message` 하나뿐이라 **어느 필드가 틀렸는지 프론트가 알 수 없다.**
게다가 `GlobalExceptionHandler` 는 여러 필드가 틀려도 `findFirst()` 로 **하나만** 보여준다.
지금은 프론트 입력 요소의 `required`/`min`/`max`/`maxLength` 가 1차로 막고 있어 급하지 않다.

- [ ] 도입 여부 결정 — 안 하면 백로그로 남긴다
- [ ] (도입 시) `ErrorResponse` 에 `fieldErrors` 추가
- [ ] (도입 시) `GlobalExceptionHandler` 에서 `BindingResult` 의 필드명·메시지를 전부 추출
- [ ] (도입 시) 프론트 `shared/api/types/types.ts` 갱신
- [ ] (도입 시) 폼 6곳(로그인·회원가입·프로필·차량 등록·주행거리·정비)에서 필드 아래 표시로 연결

### 백로그 — 지금 하지 않는다

프론트를 쓰다 실제로 불편해지면 그때 꺼낸다. 자세한 배경은 `CLAUDE.md` 에 있다.

- [x] ~~홈 통계 요약 API~~ — **완료** (`GET /api/summary`).
      요청 `1 + 차량수 × 2` → **1번**, 합계의 200건 상한이 사라지면서 화면의
      "일부 기록만 합산됨" 단서도 없어졌다. 프론트의 집계 코드 258 → 81줄.
- [x] ~~다음 정비 시점을 **전체 종류 한 번에** 반환하는 API~~ — **완료.**
      정비 종류가 15개가 되면서 요청 15번이 되어 더 미룰 수 없었다.
- [x] ~~정비 이력 등록 시 차량 주행거리 자동 갱신~~ — **완료.** 규칙은 `Vehicle.liftOdometerTo()` 에 있고 주유·정비가 같이 쓴다.
- [ ] 정비 이력 종류별 필터링 (`GET .../maintenance-records?type=`)
- [ ] 차량 목록에 각 차량의 "임박한 정비" 요약 포함 (목록 화면에서 바로 보이게)
- [ ] 이메일 중복 확인 API (`GET /api/users/exists?email=`) — 회원가입 폼 실시간 피드백용
      → 단, 이건 계정 존재 여부를 노출하는 API다. 로그인 실패 메시지를 일부러 통일해 둔 것과
        모순되므로 **도입 전에 트레이드오프를 다시 따진다.**
      → 2026-09-22 에 한 번 따졌다. 가입 409 가 이미 같은 것을 알려주고 있었고, **없애는 대신
        IP 로 속도를 제한하기로 했다.** 이 API 를 따로 만들면 그 제한 바깥에 같은 통로를 하나 더
        여는 셈이라, 만든다면 **같은 제한을 통과시켜야 한다.**
- [ ] 만탱크 연비 — 지금은 매 주유마다 직전 기록과의 차이로 계산한다(단순법).
      **가득 채우지 않은 주유가 섞이면 그 구간만 실제보다 높게 나온다.** 기록에
      "가득 채웠는가" 플래그를 두면 가득→가득 구간으로 정확히 낼 수 있다.
      → 지금은 **평소 구간과 견줘 이상한 구간을 평균에서 빼는 것**으로 대신하고 있다
        (거리·연비가 둘 다 중앙값의 1.8배를 넘을 때). 다만 만탱크가 아닌 주유는 그 구간을
        **낮게** 만들어 방향이 반대라, 이 장치로는 안 잡힌다.
- [ ] PWA (manifest + 아이콘 + 서비스 워커)
      → 웹으로 확정했으므로 "홈 화면 아이콘"을 얻는 유일한 길이다. 하면 `theme-color` 가
        네 곳 중복이 된다(`index.css`·`index.html`·`ThemeProvider.tsx`·manifest).
- [ ] 로그인 실패 응답 시간이 계정 존재 여부에 따라 다르다
      → 이메일이 없으면 BCrypt 검증을 건너뛰어 빨리 답한다. 실패 메시지를 일부러 통일해
        둔 방침과 어긋나는 지점이라 언젠가 따져 볼 것.
- [x] ~~배포한다면 먼저 따질 것 셋~~ — **2026-09-21 완료.** CSRF 토큰(double submit),
      로그인 시도 제한(10분/10회), 쿠키 `secure` 환경변수 스위치.
- [x] ~~배포 전 보안 점검~~ — **2026-09-22 완료.** 운영 프로파일(`SPRING_PROFILES_ACTIVE=prod`)로
      Swagger 문서와 SQL 값 로깅을 닫고, 보안 응답 헤더를 붙이고, CSRF 쿠키도 `secure` 를 따르게 했다.
- [ ] **세션을 DB 에 보관하기** (Spring Session) — 지금 남은 것 중 가장 크다.
      → 비밀번호를 바꿔도 **다른 기기의 로그인이 끊기지 않는다.** 계정을 도둑맞았을 때
        비밀번호 변경이 상대를 쫓아내지 못한다는 뜻이다. 세션이 서버 메모리에 있어서
        "이 사용자의 세션 전부"를 찾을 방법이 없기 때문이라, 보관 위치를 옮겨야 고칠 수 있다.
      → 덤으로 **재시작해도 로그인이 유지**되고, 로그인 시도 잠금도 재시작으로 안 풀린다.
- [ ] 로그인 시도 제한을 **IP 와 함께** 세기
      → 지금은 이메일로만 세서, 남의 주소로 일부러 10번 틀리면 그 사람을 10분간 잠글 수 있다.

### 완료 판정 기준

위 0번을 처음부터 끝까지 막힘없이 수행할 수 있고, `./gradlew test` 가 통과하면 "완성"으로 본다.
(테스트 **241개**(백엔드 202 · 프론트 39)는 지금 통과 중이다. **남은 것은 사람 눈 확인 하나뿐이다.**)
배포(서버 인프라, 도메인, CI/CD)는 이 프로젝트의 범위 밖이며, **로컬에서 완전히 동작하는 것**까지가 목표다.

## 트러블슈팅

### 타입 스케일 토큰을 쓰자 버튼 글자가 작아졌다

랜딩의 `시작하기` 버튼 글자가 작아져 눈에 잘 안 들어왔다.

**원인**: `text-[0.9375rem]` 같은 임의 값을 `text-body` 토큰으로 일괄 교체하면서
`shared/ui/base/button.tsx` 까지 바꿨는데, 이 파일은 `cva` 로 클래스를 조립한다.

```
base  : "… text-[0.875rem] font-medium …"     ← 14px
size  : { lg: "h-13 px-8 text-body" }         ← 15px
```

`cva` 는 클래스를 **이어 붙이기만** 하고 Tailwind 충돌을 해결하지 않는다(그건
`tailwind-merge` 의 일이다). 그래서 둘 다 최종 `class` 에 남고, 승자는 **CSS 에 먼저 쓰인
규칙이 지느냐 이기느냐**로 정해진다. 그리고 Tailwind 는 **토큰 유틸리티를 임의 값보다 먼저**
배치한다.

```css
/* 교체 전 — 15px 이 뒤에 있어 이긴다 */
.text-\[0\.875rem\]  { font-size: .875rem  }
.text-\[0\.9375rem\] { font-size: .9375rem }

/* 교체 후 — 토큰이 앞으로 가서 14px 이 이긴다 */
.text-body            { font-size: .9375rem }
.text-\[0\.875rem\]  { font-size: .875rem  }
```

**해결**: `button.tsx` 의 size variant 만 임의 값으로 되돌렸다. 나머지 45곳은 base 와 크기가
겹치지 않아 토큰을 그대로 쓴다. 되돌린 뒤 빌드 CSS 의 규칙 순서가 교체 전과 동일한지 확인했다.

**교훈**: 한 요소에 같은 속성을 주는 클래스가 둘 이상 남을 수 있는 구조(`cva` 처럼
조립만 하는 곳)에서는 **클래스 이름을 바꾸는 것만으로 렌더 결과가 달라진다.**
`tsc`·`oxlint`·`vite build` 는 전부 통과했고, 빌드 CSS 를 선언 단위로 비교했을 때도
"사라진 선언 없음"이라 안전해 보였다 — **순서가 바뀐 것은 그 비교로 잡히지 않았다.**

### 카드 제목이 굵지 않고, 라벨 크기가 없었다

위 문제를 쫓다 더 큰 것이 나왔다. **`cn` 은 커스텀 테마 이름을 크기로 인식하지 못하고
색 유틸리티로 추정한다.** 그래서 한 문자열에 크기 토큰과 색이 같이 있으면 **크기가 지워진다.**

```js
cn('text-caption', 'text-strong')      // → 'text-strong'    크기가 사라진다
cn('text-[0.8125rem]', 'text-strong')  // → 둘 다 남는다      임의 값은 크기로 인식
cn('text-sm', 'text-strong')           // → 둘 다 남는다      기본 스케일도 인식
```

`CardTitle` 은 `cn("font-heading text-section text-strong", …)` 이었다. 즉 **`text-section` 이
DOM 에 도달하지 못했고**, 카드 제목 열 곳이 17px·굵기 600·자간 -0.022em 을 전부 잃고
16px·굵기 400(`body` 가 상속시키는 값)으로 렌더되고 있었다. 2026-09-11 에 이 토큰을 넣은
뒤로 계속 그랬다.

**해결**: `cn()` 을 거치는 네 파일(`base/card.tsx`·`base/label.tsx`·`base/button.tsx`·
`feedback/state.tsx`)에서 크기를 임의 값으로 적는다. 토큰이 행간·굵기·자간까지 묶고 있던
자리는 그 값들을 그대로 풀어 썼다. 화면 파일은 `cn()` 을 거치지 않으므로 토큰을 그대로 쓴다.

**검증**: `cn` 을 실제로 호출해 `cn()` 안의 모든 문자열에서 사라지는 클래스가 없는지
스크립트로 확인했다. 클래스 이름을 바꾸는 작업에서는 **빌드 CSS 비교만으로는 부족하고,
병합 함수의 출력을 직접 봐야 한다.**

### 주유 기록을 수정하면 연비 카드가 두 개로 보인다

차량 상세에서 주유 기록을 수정하고 저장하면 `연비` 카드가 화면에 둘 나타났다.
새로고침하면 하나로 돌아왔다.

**원인**: 오른쪽 열의 형제 셋이 **같은 `key` 를 갖고 있었다.**

```tsx
const [maintenanceVersion, setMaintenanceVersion] = useState(0)
const [fuelVersion, setFuelVersion] = useState(0)
const [fuelListVersion, setFuelListVersion] = useState(0)

<NextServiceCard  key={maintenanceVersion} />   // 0
<MaintenanceSection />
<FuelSummaryCard  key={fuelVersion} />          // 0  ← 충돌
<FuelSection      key={fuelListVersion} />      // 0  ← 충돌
```

이 앱은 "다시 계산시켜야 하는 카드"를 `key` 를 바꿔 재생성하는 방식으로 갱신한다.
그런데 버전 값이 전부 `0` 에서 시작하다 보니, 같은 부모 안의 세 자식이 같은 key 를 갖게 됐다.
React 는 **같은 부모 안에서 key 로 자식을 짝짓기** 때문에 이 상태에서 어느 컴포넌트를
재사용할지가 어긋난다. 주유 기록을 수정해 `fuelVersion` 만 `0 → 1` 이 되는 순간
짝이 밀리면서 카드가 둘로 남았다.

브라우저 콘솔에 아래 경고가 함께 떠 있었다.

```
Warning: Encountered two children with the same key, `0`.
Keys should be unique so that components maintain their identity across updates.
```

**해결**: key 에 접두사를 붙여 형제 사이에서 유일하게 만들었다.

```tsx
<NextServiceCard  key={`next-service-${maintenanceVersion}`} />
<FuelSummaryCard  key={`fuel-summary-${fuelVersion}`} />
<FuelSection      key={`fuel-list-${fuelListVersion}`} />
```

**`key` 를 재생성 장치로 쓸 때는 값이 아니라 "형제 사이에서 유일한 문자열"이어야 한다.**
숫자 카운터를 그대로 쓰면 서로 다른 컴포넌트의 카운터가 같은 값에서 만나는 순간 충돌한다.
타입 검사·린트·빌드는 이걸 잡지 못한다 — 화면을 봐야만 드러난다.

### `@EnableJpaAuditing` 을 어디에 두느냐로 테스트가 두 번 깨졌다

엔티티가 4개가 되면서 `@PrePersist`/`@PreUpdate` 복사를 걷어내고 `BaseTimeEntity` +
`@EnableJpaAuditing` 으로 바꿨다. 그 애노테이션을 놓을 자리를 두 번 잘못 짚었다.

**1차 — `OdoLogApplication` 에 붙였더니 `@WebMvcTest` 22개가 전부 실패했다.**

```
UserControllerTest > 회원가입 성공 시 201과 사용자 정보를 반환한다 FAILED
    java.lang.IllegalArgumentException: JPA metamodel must not be empty
```

**원인**: `@WebMvcTest` 는 웹 계층만 띄우고 JPA 를 로드하지 않는데,
`@EnableJpaAuditing` 이 등록하는 `AuditingEntityListener` 는 엔티티 메타모델을 요구한다.
메인 클래스에 붙은 애노테이션은 `@WebMvcTest` 의 설정 기점이라 그대로 적용된다.

**2차 — 별도 `@Configuration` 으로 옮겼더니 이번엔 `@DataJpaTest` 가 깨졌다.**

```
UserRepositoryTest > 사용자를 저장하면 id와 createdAt이 채워진다 FAILED
```

**원인**: `@DataJpaTest` 는 JPA 와 무관한 `@Configuration` 을 전부 걸러낸다.
Auditing 이 켜지지 않아 `created_at` 이 null 인 채로 INSERT 되고 NOT NULL 위반이 났다.

**해결**: 별도 설정 클래스(`common/config/jpa/JpaAuditingConfig`)에 두고,
리포지토리 테스트에만 `@Import` 로 직접 끌어온다.

```java
@DataJpaTest
@Import(JpaAuditingConfig.class)   // 빠뜨리면 created_at 이 null 로 INSERT 된다
class UserRepositoryTest { ... }
```

`@Import` 를 깜빡하면 조용히 넘어가지 않고 NOT NULL 위반으로 바로 터진다 —
이 선택의 안전장치가 그것이다.

> 컬럼은 그대로다. `@MappedSuperclass` 는 테이블을 만들지 않고 필드만 자식 테이블에 합치므로
> `created_at` / `updated_at` 의 이름과 타입이 바뀌지 않고, `ddl-auto: update` 가 아무것도
> 건드리지 않는다. 실제 DB 로 확인했다:
>
> ```
> /opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW COLUMNS FROM users LIKE '%_at';"
> ```

### `mysql` 명령어로 접속 시 `Access denied`

터미널의 `mysql` 명령어는 사실 MariaDB 클라이언트다. `~/.my.cnf`에 이전에 쓰던 계정의 비밀번호가
남아 있으면 접속 시 자동으로 같이 전송되는데, 이게 현재 DB 계정 정보와 달라서 `--no-defaults` 옵션
없이 접속하면 `Access denied`가 발생했다.

**원인**: `~/.my.cnf`에 저장된 자격증명이 실제 DB 계정과 다름.

**해결**: `--no-defaults` 옵션으로 기존 설정 파일을 무시하고 접속한다.

```
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW TABLES;"
```

참고로 `user@localhost` 계정은 비밀번호 해시가 문자 그대로 `'invalid'` 라서 비밀번호로는
접속되지 않고, 유닉스 소켓으로만 붙는다. 그래서 TCP로 접속하는 애플리케이션(JDBC)에는 쓸 수 없고
진단(테이블 확인 등) 용도로만 사용한다. 자세한 경위는 아래 항목에 있다.

### IntelliJ에서 실행 시 `Access denied for user 'root'@'localhost'`

애플리케이션 시작이 실패하고 아래 로그가 남았다.

```
SQL Error: 1698, SQLState: 28000
(conn=300) Access denied for user 'root'@'localhost'
...
Unable to determine Dialect without JDBC metadata
```

**원인**: 두 가지가 겹쳐 있었다.

1. IntelliJ 실행 구성에 환경변수 `DB_USERNAME` / `DB_PASSWORD` 가 설정되지 않아
   `application.yml` 의 기본값 `${DB_USERNAME:root}` 가 그대로 쓰였다.
2. 애초에 `odolog` 스키마에 접근할 수 있는 계정이 없었다. 평소 터미널에서 쓰던
   `user@localhost` 는 `unix_socket` 인증(비밀번호 해시가 `'invalid'`)이라
   TCP로 접속하는 JDBC로는 쓸 수 없다.

**해결**: 앱 전용 계정을 만들고 `odolog` 스키마 권한만 부여한 뒤,
IntelliJ 실행 구성(Run/Debug Configurations → Environment variables)에 넣었다.

```sql
CREATE USER 'odolog'@'localhost' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON odolog.* TO 'odolog'@'localhost';
FLUSH PRIVILEGES;
```

`Unable to determine Dialect without JDBC metadata` 는 별개의 원인이 아니라,
연결에 실패해 Hibernate가 DB 종류를 알아낼 수 없어서 따라온 2차 에러다.

## 라이선스

[MIT](LICENSE)
