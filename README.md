# 오도로그 (OdoLog)

차량 정비 이력과 주유 기록을 관리하는 웹 앱. 기록이 쌓이면 **연비(km/L)** 와
**다음 정비 시점**(주행거리 기준·날짜 기준)이 계산된다.

Spring Boot 3.5 + MariaDB 백엔드에 React 19 SPA 를 붙인 구성이고, 인증은 **세션 쿠키**다.
한국어·영어 두 벌이고, 계정마다 언어·시간대·통화·단위(km/L · L/100km · mpg)를 따로 가진다.
**이미 타던 차**를 등록하는 경우를 기본으로 본다 — 현재 주행거리와 기억나는 정비 몇 가지만 적으면
다음 정비 시점이 바로 뜨고, 이후 주유·정비를 적을 때마다 기록이 쌓인다.

백엔드 API 29개 · 화면 12 라우트가 동작하고, 테스트 462개(백엔드 365 · 프론트 97)와 실제 브라우저 E2E 14개가 통과한다.
플랫폼은 웹 하나다(근거는 `HISTORY.md` 의 2026-09-16 항목). 개인 학습 프로젝트라
**로컬에서 완전히 동작하는 것**까지가 범위이고 배포는 범위 밖이다 — 다만 "올린다면"은 운영 프로파일로 준비해 뒀다.

| 문서 | 내용 |
|---|---|
| `CLAUDE.md` | 설계 원칙과 그 이유, 구조 지도, 의존 방향 |
| `docs/DESIGN.md` | 프론트엔드 디자인 시스템 |
| `docs/QA.md` | 로드맵, 눈 확인 체크리스트, 완료 판정 기준, 백로그 |
| `HISTORY.md` | 작업 일지 — 무엇을 왜 그렇게 정했는지 |

## 도메인 규칙 — 계산되는 값 둘

둘 다 **저장하지 않고 조회할 때 계산한다** — 앞 기록이 수정되면 뒤 기록의 값이 따라 바뀌므로,
계산 결과를 컬럼으로 들고 있으면 언젠가 반드시 원본과 어긋난다.

### 연비

**구간(segment) 단위**로 계산한다. 구간은 주유 기록 두 건 사이다.

    구간 연비 = (이번 odometer − 직전 odometer) ÷ 이번 주유량(L)
    평균 연비 = Σ(구간 거리) ÷ Σ(구간 주유량)

총 거리 ÷ 총 주유량과 값은 같지만, **구간 단위라야 말이 안 되는 구간을 골라낼 수 있다.**

- **주유량과 결제 금액은 비워 둘 수 있다.** 비운 기록은 자기 구간의 연비만 빠지고 거리는 그대로 나온다.
  `0` 으로 채우지 않는다 — 0 은 "0L 를 0원에 넣었다"는 다른 사실이고, 연비가 0 으로 나누기가 된다
- 첫 기록과 "연비 초기화" 기준점에는 짝이 없다 → 응답이 `0` 이 아니라 **`null`** 이고, 화면은
  `기준 기록 · 다음 주유부터 계산` 으로 이유를 말한다
- 성립할 수 없는 구간(odometer 를 한 자리 잘못 적어 200 km/L)은 **평균에서 빼고**, 몇 개를 뺐는지(`excludedSegmentCount`) 함께 내려보낸다
- **기록이 빠진 구간도 뺀다**(`longSegmentCount`). 한 번 안 적으면 그 구간은 거리만 두 배라 연비가 두 배로 뜨는데,
  "불가능"하지는 않아 조용히 평균을 끌어올린다. 기준은 그 차량의 평소 구간 **중앙값의 1.8배**이고(평균은 이상값에 끌려간다),
  **거리와 연비가 둘 다** 넘어야 한다 — 거리만 보면 장거리 여행을 잡는다. 구간이 3개 미만이면 판단하지 않는다
- 뺀 구간의 숫자는 목록에서 지우지 않고 `확인 필요` / `기록 빠짐?` 만 붙인다 — 무엇이 잘못됐는지 보려면 값이 남아 있어야 한다
- 지금은 단순법(매 주유마다 직전과 비교)이다. 만탱크 플래그는 백로그에 있다

### 다음 정비 시점

`ServiceType` enum 15종이 각각 권장 주기(km·개월)를 들고 있다. 해당 종류의 마지막 이력에 주기를 더해
**두 기준을 모두** 내려보내고, 먼저 오는 쪽이 실제 시기다.

- **권장 주기는 차량마다 바꿀 수 있다.** 기본값은 광유 기준이라 엔진오일이 5,000km 인데 합성유는 10,000~15,000km 다 —
  손잡이가 없으면 `지남` 이 늘 켜진 경고등이 되고, 늘 켜진 경고는 아무도 안 본다
- 지난 것은 **`지남`**, 다음 시점까지 1,000km 또는 한 달 안(주기의 마지막 1/5 이내)이면 **`곧`**. 정렬은 지남 → 곧 → 나머지.
  차량 목록과 홈에도 `정비 N건 지남/곧` 이 붙는다. 판정은 서버가 한다 — 화면마다 오늘과 비교하면 서로 다른 말을 한다
- 색을 늘리지 않고 대비로 표현한다 — 빨강은 "실패"이고, 정비 시기가 지난 것은 실패가 아니라 할 일이다
- 정비 기록의 비용과 그때 주행거리는 비워 둘 수 있다(타던 차의 "석 달 전쯤 갈았다"). 주행거리가 비면 날짜 기준만 계산한다
- `OTHER` 는 주기가 없어 계산하지 않는다. **이력이 없는 종류는 응답에서 뺀다** — 15종을 "기록 없음"으로 늘어놓지 않는다

## 기술 스택

**백엔드** — Spring Boot 3.5.6 / Java 17 / Gradle · Spring Data JPA (Hibernate 6.6) ·
MariaDB 12.3 (`mariadb-java-client`) · **Flyway** · **Spring Session JDBC** · springdoc-openapi 2.8 ·
`spring-boot-starter-mail`(비밀번호 재설정)

**프론트엔드** — React 19 / TypeScript 6 / Vite 8 · Tailwind CSS v4 + shadcn/ui · **TanStack Query** ·
vitest + Testing Library · **Playwright**(E2E) · 린터는 ESLint 가 아니라 **oxlint**

**일부러 안 쓴 것 넷**이 이 저장소의 성격을 말해 준다.

- **Lombok 없음** — 생성자·getter 를 직접 적는다. 무엇이 생성되는지 눈으로 보는 것이 목적이다
- **Spring Security 없음** — `spring-security-crypto`(BCrypt)만 쓰고 세션·CSRF·시도 제한·보안 헤더는 직접 만들었다 → **보안** 참고
- **차트 라이브러리 없음** — 홈의 차트 2종은 HTML/CSS 로 그린다
- **번역 라이브러리 없음** — 문구는 `shared/i18n/messages` 의 객체 두 벌이다. 영어판 모양이 다르면 컴파일이 실패하고,
  숫자·날짜·통화 표기는 브라우저 `Intl` 이 한다

## 실행 방법

### 준비물

Java 17+ · Node 26(`frontend/.nvmrc`) · MariaDB 12 · IntelliJ IDEA(백엔드를 여기서 띄운다).

### 1. 스키마와 계정

운영용 `odolog` 와 테스트용 `odolog_test` **둘 다** 필요하다. 테스트는 매 실행마다 표를 지우고 다시 만든다.

```sql
CREATE DATABASE odolog DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'odolog'@'localhost' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON odolog.* TO 'odolog'@'localhost';

CREATE DATABASE odolog_test DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
CREATE USER 'odolog_test'@'localhost' IDENTIFIED BY 'odolog_test';
GRANT ALL PRIVILEGES ON odolog_test.* TO 'odolog_test'@'localhost';

FLUSH PRIVILEGES;
```

표는 앱이 뜰 때 **Flyway** 가 `src/main/resources/db/migration` 으로 만든다. Hibernate 는 `ddl-auto: validate` 로
확인만 하고, 엔티티와 스키마가 다르면 기동을 막는다. 권한을 스키마 단위로 좁힌 것은 계정이 새어도 다른 스키마로
번지지 않게 하기 위해서다. `odolog_test` 의 자격증명은 `src/test/resources/application.yml` 에 그대로 있다(매번 비워지는 로컬 전용).

### 2. 백엔드

IntelliJ 에서 `OdoLogApplication` 을 실행한다. 실행 구성의 Environment variables 에 접속 정보를 넣는다.

```
DB_USERNAME=odolog
DB_PASSWORD=<1단계에서 정한 비밀번호>
```

`application.yml` 에는 `${DB_USERNAME:root}` 형태만 있다 — 파일에 적으면 지워도 커밋 히스토리에 남는다.
`Started OdoLogApplication` 이 찍히면 <http://localhost:8080/swagger-ui.html> 에서 API 를 쏴 볼 수 있다(CSRF 토큰도 알아서 싣는다).

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

**포트 5173 은 고정이다.** 백엔드 CORS 허용 주소와 짝이라, 5173 이 이미 점유돼 Vite 가 5174 로 옮겨 뜨면
**모든 요청이 CORS 에서 막힌다.**

### 4. 검사

```
./gradlew test                  # 백엔드 365개
cd frontend && npm run test     # 프론트 97개 (vitest)
cd frontend && npm run e2e      # 실제 브라우저 14개 (Playwright). 처음엔 npx playwright install chromium
cd frontend && npm run lint     # oxlint
cd frontend && npm run build    # tsc -b + vite build (번들 500kB 경고는 알려진 것)
```

`main` 푸시와 PR 마다 GitHub Actions 가 백엔드·프론트·E2E 를 돌린다(`.github/workflows/ci.yml`).
**CI 도 H2 가 아니라 MariaDB 컨테이너를 띄운다** — 다른 DB 로 검증하면 "테스트는 통과하는데 운영만 안 바뀌는" 상황이 생긴다.

`npm run e2e` 는 백엔드(`./gradlew bootTestRun`, 테스트 설정, 18080)와 화면(Vite, 5174)을 **따로 띄운다.**
평소 서버(8080·5173)는 운영 DB 를 쓰므로 재사용하지 않는다. 콘솔 오류가 하나라도 나면 실패한다.

### 선택 — 비밀번호 재설정 메일

```
MAIL_USERNAME=<보내는 주소>
MAIL_PASSWORD=<앱 비밀번호>      # Gmail 이면 2단계 인증 후 발급
```

안 넣어도 앱은 뜬다. 발송만 실패하고 로그에 `ERROR` 로 남는다 — 실패를 올려보내면 가입된 주소에서만 500 이 나서
가입 여부를 알려 주게 된다. 로컬에서는 보내지 못한 링크가 `WARN [개발용] 보내지 못한 재설정 링크: …` 로 찍혀
메일 없이도 흐름을 끝까지 볼 수 있다(운영 프로파일은 끈다). SMTP 연결·읽기·쓰기에는 5초 제한이 있다.

### 선택 — 운영 프로파일

`SPRING_PROFILES_ACTIVE=prod` 하나가 `application-prod.yml` 을 겹쳐 읽게 한다.

| 바뀌는 것 | 이유 |
|---|---|
| Swagger 문서 끔 | 인증이 없고 GET 이라 CSRF 대상도 아니어서 관문이 하나도 없다 |
| SQL·bind 로깅 끔 | 개발 설정이 이메일·닉네임·BCrypt 해시까지 평문으로 찍는다 |
| 세션 쿠키 `secure` | 세션 쿠키와 CSRF 쿠키가 같은 스위치를 읽는다 |
| 재설정 링크 로그 끔 | 토큰이 로그에 남으면 로그를 보는 사람이 남의 비밀번호를 바꾼다 |
| `DB_USERNAME`·`DB_PASSWORD`·`APP_BASE_URL`·`CORS_ALLOWED_ORIGINS` 기본값 없음 | 빠뜨리면 조용히 뜨지 않고 기동이 실패한다 |

주석으로 "배포할 때 끄세요"라고 적는 것과의 차이는 — 주석은 사람이 기억해야 하고, 프로파일은 환경변수가 대신 기억한다.

### 2026-10-03 이전에 만든 DB 를 가져온다면

Flyway 는 기존 DB 를 V1(`db/migration/V1__baseline.sql`)로 **간주**하고 V2 부터 돈다. 그 전에 `ddl-auto: update` 가 만들던 시절의
DB 는 V1 과 다를 수 있고, 다르면 Hibernate 검증이 기동을 막는다. 새로 만드는 DB 는 할 일이 없다.
옛 DB 는 해당하는 것만, **앱을 띄우기 전에** 실행한다(`ddl-auto` 가 지우지도 바꾸지도 않던 것들이다).

```
# 번호판 유니크를 전역 → 소유자별로 (2026-09-07) — 옛 전역 유니크가 남아 있으면 더 엄격한 쪽이 이겨
# 다른 계정끼리도 같은 번호판을 못 쓴다. 검증도 Flyway 도 잡지 못한다
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults \
  -e "USE odolog; ALTER TABLE vehicles DROP INDEX uk_vehicles_plate_number;"

# 정비 종류 5 → 15개 (2026-09-16) — 네이티브 enum 컬럼을 varchar 로. 값은 보존된다
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults \
  -e "USE odolog; ALTER TABLE maintenance_records MODIFY COLUMN type VARCHAR(30) NOT NULL;"

# 주유량·금액 선택 입력 (2026-09-23)
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; \
  ALTER TABLE fuel_records MODIFY COLUMN liters DECIMAL(6,2) NULL; \
  ALTER TABLE fuel_records MODIFY COLUMN total_cost INT NULL;"

# 공개 id (2026-09-26) — 기존 행을 채운 뒤 NOT NULL·유니크. 순서가 중요하다
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog;
  ALTER TABLE vehicles ADD COLUMN public_id VARCHAR(12) NULL;
  UPDATE vehicles SET public_id = LEFT(REPLACE(REPLACE(REPLACE(
    TO_BASE64(RANDOM_BYTES(24)), '+', ''), '/', ''), '=', ''), 12);
  ALTER TABLE vehicles MODIFY public_id VARCHAR(12) NOT NULL;
  ALTER TABLE vehicles ADD CONSTRAINT uk_vehicles_public_id UNIQUE (public_id);
  ALTER TABLE maintenance_records ADD COLUMN public_id VARCHAR(12) NULL;
  UPDATE maintenance_records SET public_id = LEFT(REPLACE(REPLACE(REPLACE(
    TO_BASE64(RANDOM_BYTES(24)), '+', ''), '/', ''), '=', ''), 12);
  ALTER TABLE maintenance_records MODIFY public_id VARCHAR(12) NOT NULL;
  ALTER TABLE maintenance_records ADD CONSTRAINT uk_maintenance_records_public_id UNIQUE (public_id);
  ALTER TABLE fuel_records ADD COLUMN public_id VARCHAR(12) NULL;
  UPDATE fuel_records SET public_id = LEFT(REPLACE(REPLACE(REPLACE(
    TO_BASE64(RANDOM_BYTES(24)), '+', ''), '/', ''), '=', ''), 12);
  ALTER TABLE fuel_records MODIFY public_id VARCHAR(12) NOT NULL;
  ALTER TABLE fuel_records ADD CONSTRAINT uk_fuel_records_public_id UNIQUE (public_id);"

# 영어권 대응 (2026-09-29) — 더 이상 받지 않는 전화번호 컬럼
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; ALTER TABLE users DROP COLUMN phone;"

# 정비 비용·주행거리 선택 입력 (2026-09-30)
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; ALTER TABLE maintenance_records MODIFY COLUMN cost INT NULL, MODIFY COLUMN service_odometer INT NULL;"
```

`RANDOM_BYTES` 는 암호학적 난수라 앱의 `SecureRandom` 과 같은 성질이다. 그 사이 `ddl-auto` 가 알아서 붙이던 컬럼
(09-29 의 사용자 설정 넷과 기록의 `currency` — 기존 행은 `KO`·`Asia/Seoul`·`KRW`·`KM_PER_L` 로 채운다 —, 10-03 의 `vehicles.version` 등)이
빠져 있다면 `V1__baseline.sql` 과 `SHOW CREATE TABLE` 을 견줘 맞춘다. 맞지 않으면 검증이 기동을 막으므로 조용히 틀어지지는 않는다.

## 프로젝트 구조

백엔드와 프론트엔드 모두 **기능별(package-by-feature)** 이다. 폴더 단위 지도와 각 자리의 이유는 `CLAUDE.md` 에 있다.

    src/main/java/com/odolog/app/
    ├── user/  vehicle/  maintenance/  fuel/      기능 넷
    ├── garage/  account/  summary/               조율 층 — 여러 기능을 동시에 알아도 되는 자리
    │                                             (차량 목록·삭제 / 탈퇴·내보내기·가져오기 / 홈 요약)
    └── common/                                   auth · config · domain · dto · exception · validation · web

    frontend/src/
    ├── app/                라우트, 레이아웃, 홈 갈림, 랜딩·약관 — 여러 기능을 아는 유일한 층
    ├── features/           auth · account · vehicles · maintenance · fuel · summary
    └── shared/             api(클라이언트·조회 캐시 키) · i18n · lib(단위·금액·포맷·훅) · theme · ui

백엔드의 기능 하나는 모두 같은 모양이다 — `domain/`(엔티티·enum·값 계산), `dto/request/`, `dto/response/`,
그리고 기능 폴더 바로 아래의 리포지토리·서비스·컨트롤러. 테스트도 같은 경로를 따라간다.

의존 방향은 `fuel·maintenance → vehicle → user`, 조율 층 → 기능, 프론트는 `app → features → shared` 한 방향이다.
**문서가 아니라 테스트가 지킨다** — 백엔드 `DependencyDirectionTest`, 프론트 `src/dependency-direction.test.ts` 가
import 를 훑어 허용 목록 밖이면 실패한다. 회원 탈퇴처럼 여러 기능을 지우는 일을 `UserService` 에 넣으면
`user → vehicle` 역방향이 생겨서, 그런 일은 조율 층이 맡는다.

## API 개요

| 기능 | 엔드포인트 |
|---|---|
| 회원가입 | `POST /api/users` |
| 로그인 / 로그아웃 | `POST /api/users/login` · `POST /api/users/logout` |
| 내 정보 조회/수정 | `GET`, `PATCH /api/users/me` |
| 비밀번호 변경 | `PATCH /api/users/me/password` |
| 비밀번호 재설정 요청/확정 | `POST`, `PATCH /api/users/password-reset` |
| 내 기록 내보내기 / 가져오기 | `GET /api/users/me/export` · `POST /api/users/me/restore` |
| 회원 탈퇴 | `DELETE /api/users/me` |
| 홈 요약 (통계·차트·최근 활동) | `GET /api/summary` |
| 차량 등록/목록 | `POST`, `GET /api/vehicles` — 등록은 현재 `odometer` 필수 |
| 차량 상세/수정/삭제 | `GET`, `PATCH`, `DELETE /api/vehicles/{vehicleId}` — `vehicleId` 는 12자 공개 id |
| 주행거리 갱신 | `PATCH /api/vehicles/{vehicleId}/odometer` |
| 정비 이력 등록/목록 | `POST`, `GET /api/vehicles/{vehicleId}/maintenance-records` (`?type=` 로 거르기) |
| 정비 이력 수정/삭제 | `PATCH`, `DELETE …/maintenance-records/{recordId}` — `recordId` 도 12자 공개 id |
| 다음 정비 시점 | `GET …/maintenance-records/next-services` |
| 차량별 권장 주기 | `PATCH …/maintenance-records/intervals/{type}` |
| 주유 기록 등록/목록 | `POST`, `GET /api/vehicles/{vehicleId}/fuel-records` |
| 주유 기록 수정/삭제 | `PATCH`, `DELETE …/fuel-records/{recordId}` |
| 연비 요약 | `GET …/fuel-records/summary` |

### 공통 규약

| | |
|---|---|
| 인증 | 세션 쿠키(`JSESSIONID`). 로그인 없이 부르면 **401** |
| CSRF | 쓰기 요청에 `X-XSRF-TOKEN` 헤더 필요. 없으면 **403** `CSRF_REJECTED` |
| 시도 제한 | 로그인 · 재설정 요청 · 회원가입 · 비밀번호 확인(변경·탈퇴). 걸리면 **429** |
| 동시 수정 | 늦게 온 쪽, 데드락에 진 쪽은 **409** `CONCURRENT_UPDATE` — 다시 보내면 된다 |
| 본문 크기 | 10MB 까지. 넘으면 **413** `PAYLOAD_TOO_LARGE` |
| 남의 자원 | **404**. 403 을 주지 않는다(존재 자체를 숨긴다) |
| 페이지네이션 | `PageResponse<T>` — `items` / `page` / `size` / `totalElements` / `totalPages` / `hasNext`. 크기 상한 100 |
| 정렬 | `sort` 는 화이트리스트(밖이면 400). **주유 목록은 `sort` 를 받지 않는다** — 정렬이 연비 계산의 전제라 고정 |
| 에러 본문 | `{"code", "message", "field"?, "retryAfterMinutes"?}`. 화면은 `code` 로 문구를 고른다. `message` 는 개발자용 한국어 |
| 검증 실패 | **400**. 여러 칸이 틀려도 첫 칸 하나만 `field` 에 실린다 |
| 필수 입력의 공백 | 앞뒤 공백은 서버가 잘라 저장한다. 잘라서 비면(전각 공백만 포함) **400** |

API 를 쓸 때 걸려 넘어지기 쉬운 것:

- **계산할 수 없는 값은 `0` 이 아니라 `null`** — `efficiency`·`distance`(첫 기록·기준점·주행거리가 늘지 않은 구간),
  비워 둔 주유량·금액·정비 비용
- **날짜는 미래를 받지 않는다**(400). "오늘"은 서버가 아니라 **계정의 시간대**(`America/Los_Angeles` 등) 기준이다.
  오늘은 통과한다. JVM 시간대(`Asia/Seoul`)는 `createdAt` 같은 기록 시각에만 쓰인다
- **금액은 통화의 최소 단위 정수 + 기록마다 통화 코드** — `4567` + `USD` 는 $45.67, `50000` + `KRW` 는 50,000원.
  통화는 기록할 때 사용자 설정에서 가져와 나중에 설정을 바꿔도 옛 기록의 뜻이 안 바뀐다.
  통화 칸이 없는 옛 백업 파일은 원화로 읽는다
- **주행거리·금액에는 상한이 있다**(200만 km / 최소 단위 1억). 차량 주행거리는 기록을 따라 오르기만 해서,
  한 번 크게 잘못 넣으면 되돌릴 수 없게 망가지기 때문이다
- **주행거리는 줄이면 409 다.** 자리수 오타·계기판 교체는 의도를 밝히면 낮출 수 있다:

```json
PATCH /api/vehicles/k3Xq9mTa2LpZ/odometer
{"odometer": 50000}                 → 409 (현재보다 작으면)
{"odometer": 50000, "force": true}  → 200
```

## 보안

Spring Security 를 안 쓰므로 아래가 전부 직접 만든 것이다. 근거와 경위는 `CLAUDE.md` 규칙 10·11·15 와 `HISTORY.md` 에 있다.

| 영역 | 한 것 |
|---|---|
| 비밀번호 | BCrypt 해시만 저장. 상한은 글자 수가 아니라 **UTF-8 72바이트**(BCrypt 한계, `@MaxBytes`) — 가입·변경·재설정이 같다 |
| 세션 | 사용자 id 만 담고 로그인 때 `changeSessionId()`(고정 공격 방어). `HttpOnly`·`SameSite=Lax`·운영에서 `secure`, 14일. **DB 에 보관**(Spring Session JDBC) — 재시작해도 유지 |
| 세션 끊기 | 비밀번호 변경은 지금 세션만 남기고 나머지를, 재설정·탈퇴는 전부 끊는다. 로그인은 세션 저장 뒤 비밀번호가 그대로인지 다시 본다(그 사이 재설정이 끝난 경우) |
| CSRF | double submit 쿠키 — `XSRF-TOKEN` 쿠키와 `X-XSRF-TOKEN` 헤더 비교. **경로와 무관하게** 모든 쓰기 요청. 세션 보관 방식을 안 쓴 이유: 비로그인 방문자에게도 세션이 생긴다. 한계: 하위 도메인이 장악되면 쿠키를 심을 수 있다 |
| 시도 제한 | 10분 안 10회 초과 시 10분 잠금. 키는 용도 접두사 + 대상: `login:`이메일 · `login-ip:`IP(한도 5배) · `password-reset:`이메일 · `password-reset-ip:`IP · `signup:`IP · `password-check:`사용자 id. 시도는 비교 **전에** 센다. IPv6 는 앞 64비트. `X-Forwarded-For` 는 읽지 않는다 |
| 사용자 열거 | 로그인 실패 문구 통일, 없는 계정도 BCrypt 한 번(응답 시간 맞춤)·횟수 집계. 재설정은 늘 204, 토큰·메일은 다른 스레드(대기 200개 상한). 남의 차량은 404(문구까지 같게). **회원가입만 409 로 알려 주고** 대신 IP 로 속도를 막는다. 이메일은 출력 가능한 ASCII 만 |
| 재설정 토큰 | 256비트 난수를 메일로, DB 에는 SHA-256 해시. 30분·1회용, 재발급·비밀번호 변경 시 삭제. 성공해도 자동 로그인하지 않는다 |
| 본문 크기 | 10MB(`RequestSizeLimitFilter`) — 검증 애노테이션은 Jackson 이 다 읽은 뒤에야 돌아서 그 앞에서 끊는다 |
| 응답 헤더 | `nosniff` · `X-Frame-Options: DENY` · `Referrer-Policy: no-referrer` · HSTS(`request.isSecure()` 일 때만). 프런트 `index.html` 에도 `no-referrer` 메타 |
| 숫자 id | URL·API 에는 12자 무작위 공개 id 만 나간다 — 연속 id 는 서비스 규모와 등록 순서를 말한다 |

**아직 안 한 것** — 시도 잠금 기록은 메모리에 있어 재시작하면 풀리고 서버를 두 대 띄우면 따로 센다.
이메일 키 때문에 남의 주소로 10번 틀려 그 사람을 10분 잠글 수 있다. 프런트의 클릭재킹 방어는 정적 호스팅의 헤더 설정이 할 일이다.

## 진행 상황

코드와 브라우저 확인은 끝났다(2026-10-10). 남은 것은 실제 폰과 Windows 에서만 볼 수 있는 몇 줄이다 — 체크리스트는 `docs/QA.md`,
완료한 작업과 그 근거는 `HISTORY.md`.
공개 전에 사람이 채워야 할 것(약관의 운영자 정보, 메일 발송 서비스)도 `docs/QA.md` 에 있다.

## 트러블슈팅

### 타입 스케일 토큰을 쓰자 버튼 글자가 작아졌다

랜딩의 `시작하기` 버튼 글자가 작아져 눈에 잘 안 들어왔다.

**원인**: `text-[0.9375rem]` 같은 임의 값을 `text-body` 토큰으로 일괄 교체하면서
`shared/ui/base/button.tsx` 까지 바꿨는데, 이 파일은 `cva` 로 클래스를 조립한다.

```
base  : "… text-[0.875rem] font-medium …"     ← 14px
size  : { lg: "h-13 px-8 text-body" }         ← 15px
```

`cva` 는 클래스를 **이어 붙이기만** 하고 충돌을 해결하지 않는다. 둘 다 최종 `class` 에 남고, 승자는
CSS 에 쓰인 순서가 정한다. Tailwind 는 **토큰 유틸리티를 임의 값보다 먼저** 배치한다.

```css
/* 교체 전 — 15px 이 뒤에 있어 이긴다 */
.text-\[0\.875rem\]  { font-size: .875rem  }
.text-\[0\.9375rem\] { font-size: .9375rem }

/* 교체 후 — 토큰이 앞으로 가서 14px 이 이긴다 */
.text-body            { font-size: .9375rem }
.text-\[0\.875rem\]  { font-size: .875rem  }
```

**해결**: 당시에는 `button.tsx` 의 size variant 만 임의 값으로 되돌렸다. 되돌린 뒤 빌드 CSS 의 규칙 순서가 교체 전과 같은지 확인했다.
`tsc`·`oxlint`·`vite build` 는 전부 통과했고, 빌드 CSS 를 선언 단위로 비교해도 "사라진 선언 없음"이었다 —
**순서가 바뀐 것은 그 비교로 잡히지 않는다.** (2026-10-05 부터는 `cva` 결과를 아래 항목의 프로젝트 `cn` 에 다시 넣어
size 의 토큰이 base 의 14px 을 대체한다.)

### 카드 제목이 굵지 않고, 라벨 크기가 없었다

위 문제를 쫓다 더 큰 것이 나왔다. **기본 `cn`(tailwind-merge 계열)은 커스텀 테마 이름을 크기로 인식하지 못하고
색 유틸리티로 추정한다.** 그래서 한 문자열에 크기 토큰과 색이 같이 있으면 크기가 지워진다.

```js
cn('text-caption', 'text-strong')      // → 'text-strong'    크기가 사라진다
cn('text-[0.8125rem]', 'text-strong')  // → 둘 다 남는다      임의 값은 크기로 인식
cn('text-sm', 'text-strong')           // → 둘 다 남는다      기본 스케일도 인식
```

`CardTitle` 은 `cn("font-heading text-section text-strong", …)` 이었다. `text-section` 이 DOM 에 닿지 못해,
카드 제목 열 곳이 17px·굵기 600·자간 -0.022em 을 전부 잃고 16px·굵기 400 으로 렌더되고 있었다(2026-09-11 부터).

**해결**: 처음에는 `cn()` 을 거치는 네 파일(`card`·`label`·`button`·`state`)에서 크기를 임의 값으로 풀어 적었다.
**2026-10-05 에 뿌리를 고쳤다** — `shared/ui/cn.ts` 가 `createCn` 에 타입 스케일 토큰을 `font-size` 그룹으로 등록해,
`cn('text-caption', 'text-strong')` 이 둘 다 남는다. 지금은 모든 파일이 토큰을 쓰고, `cn.test.ts` 가 토큰마다 확인한다.

**검증**: 클래스 이름을 바꾸는 작업에서는 빌드 CSS 비교만으로는 부족하고, **병합 함수의 출력을 직접 봐야 한다.**

### 주유 기록을 수정하면 연비 카드가 두 개로 보인다

차량 상세에서 주유 기록을 수정하고 저장하면 `연비` 카드가 화면에 둘 나타났다. 새로고침하면 하나로 돌아왔다.
콘솔에는 이 경고가 떠 있었다.

```
Warning: Encountered two children with the same key, `0`.
Keys should be unique so that components maintain their identity across updates.
```

**원인**: 당시에는 "다시 계산시켜야 하는 카드"를 `key` 를 바꿔 재생성하는 방식으로 갱신했는데, 오른쪽 열의 형제 셋이
**같은 `key` 를 갖고 있었다.**

```tsx
<NextServiceCard  key={maintenanceVersion} />   // 0
<FuelSummaryCard  key={fuelVersion} />          // 0  ← 충돌
<FuelSection      key={fuelListVersion} />      // 0  ← 충돌
```

React 는 같은 부모 안에서 key 로 자식을 짝짓는다. 버전 값이 전부 `0` 에서 시작해 짝이 밀리면서 카드가 둘로 남았다.

**해결**: key 에 접두사를 붙여 형제 사이에서 유일하게 만들었다(`` key={`fuel-summary-${fuelVersion}`} ``).
**`key` 를 재생성 장치로 쓸 때는 값이 아니라 "형제 사이에서 유일한 문자열"이어야 한다.** 타입 검사·린트·빌드는 이걸 잡지 못한다.
(2026-10-05 에 차량 상세가 TanStack Query 로 옮기면서 재생성용 key 자체가 없어졌다.)

### `@EnableJpaAuditing` 을 어디에 두느냐로 테스트가 두 번 깨졌다

**1차 — `OdoLogApplication` 에 붙였더니 `@WebMvcTest` 22개가 전부 실패했다.**

```
UserControllerTest > 회원가입 성공 시 201과 사용자 정보를 반환한다 FAILED
    java.lang.IllegalArgumentException: JPA metamodel must not be empty
```

**원인**: `@WebMvcTest` 는 JPA 를 로드하지 않는데, `@EnableJpaAuditing` 이 등록하는 리스너는 엔티티 메타모델을 요구한다.
메인 클래스의 애노테이션은 `@WebMvcTest` 에도 그대로 적용된다.

**2차 — 별도 `@Configuration` 으로 옮겼더니 이번엔 `@DataJpaTest` 가 깨졌다.**

```
UserRepositoryTest > 사용자를 저장하면 id와 createdAt이 채워진다 FAILED
```

**원인**: `@DataJpaTest` 는 JPA 와 무관한 `@Configuration` 을 걸러낸다. Auditing 이 꺼져 `created_at` 이 null 인 채로 INSERT 되어 NOT NULL 위반이 났다.

**해결**: 별도 설정 클래스(`common/config/JpaAuditingConfig`)에 두고, 리포지토리 테스트에만 `@Import` 로 끌어온다.

```java
@DataJpaTest
@Import(JpaAuditingConfig.class)   // 빠뜨리면 created_at 이 null 로 INSERT 된다
class UserRepositoryTest { ... }
```

`@Import` 를 깜빡하면 조용히 넘어가지 않고 NOT NULL 위반으로 바로 터진다 — 이 선택의 안전장치가 그것이다.
`@MappedSuperclass` 는 테이블을 만들지 않고 필드만 자식에 합치므로 컬럼 이름·타입은 그대로였다(실제 DB 로 확인):

```
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW COLUMNS FROM users LIKE '%_at';"
```

### `mysql` 명령어로 접속 시 `Access denied`

터미널의 `mysql` 명령어는 사실 MariaDB 클라이언트다. `~/.my.cnf` 에 이전에 쓰던 계정의 비밀번호가 남아 있어
접속 시 자동으로 같이 전송되는데, 이게 현재 DB 계정 정보와 달라서 `--no-defaults` 없이 접속하면 `Access denied` 가 났다.

**원인**: `~/.my.cnf` 에 저장된 자격증명이 실제 DB 계정과 다름.

**해결**: `--no-defaults` 로 기존 설정 파일을 무시하고 접속한다.

```
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults -e "USE odolog; SHOW TABLES;"
```

`user@localhost` 계정은 비밀번호 해시가 문자 그대로 `'invalid'` 라서 비밀번호로는 접속되지 않고 유닉스 소켓으로만 붙는다.
그래서 TCP 로 접속하는 JDBC 에는 쓸 수 없고 진단 용도로만 쓴다(아래 항목).

### IntelliJ에서 실행 시 `Access denied for user 'root'@'localhost'`

애플리케이션 시작이 실패하고 아래 로그가 남았다.

```
SQL Error: 1698, SQLState: 28000
(conn=300) Access denied for user 'root'@'localhost'
...
Unable to determine Dialect without JDBC metadata
```

**원인**: 두 가지가 겹쳐 있었다.

1. IntelliJ 실행 구성에 환경변수 `DB_USERNAME` / `DB_PASSWORD` 가 없어 `application.yml` 의 기본값 `${DB_USERNAME:root}` 가 쓰였다.
2. 애초에 `odolog` 스키마에 접근할 수 있는 계정이 없었다. 평소 쓰던 `user@localhost` 는 `unix_socket` 인증이라 JDBC 로는 쓸 수 없다.

**해결**: 앱 전용 계정을 만들고 `odolog` 스키마 권한만 준 뒤, IntelliJ 실행 구성의 Environment variables 에 넣었다.

```sql
CREATE USER 'odolog'@'localhost' IDENTIFIED BY '<비밀번호>';
GRANT ALL PRIVILEGES ON odolog.* TO 'odolog'@'localhost';
FLUSH PRIVILEGES;
```

`Unable to determine Dialect without JDBC metadata` 는 별개의 원인이 아니라, 연결에 실패해 Hibernate 가 DB 종류를 알아내지 못해 따라온 2차 에러다.

## 라이선스

[MIT](LICENSE)
