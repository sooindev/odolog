# 오도로그 (OdoLog)

차량 관리 앱. 자기 차량을 등록하고 **정비 이력과 주유 기록**을 관리한다.
주유 기록이 쌓이면 **연비(km/L)** 가 계산된다.

플랫폼은 **웹 하나**다. 네이티브 앱은 만들지 않는다 — 이유는 `HISTORY.md` 의 2026-09-16 항목에.

## 기술 스택

- Spring Boot 3.5.6 / Java 17 / Gradle
- Spring Data JPA (Hibernate 6.6.x)
- MariaDB 12.3.2
- 프론트엔드: React 19 / Vite 8 / TypeScript 6 / Tailwind CSS v4 / shadcn-ui

## 실행 방법

- IntelliJ IDEA에서 `OdoLogApplication`을 실행한다.
- 실행 구성의 환경변수로 `DB_USERNAME`, `DB_PASSWORD`를 설정해야 한다 (평문 비밀번호를 파일에 적지 않음).
- 빌드/컴파일만 확인할 때는 터미널에서 `./gradlew build`.
- API 문서: 실행 후 `http://localhost:8080/swagger-ui.html`

### ⚠️ 이미 쓰던 DB 가 있다면 — 정비 종류 확장 (2026-09-16)

정비 종류를 5개에서 15개로 늘리면서 `maintenance_records.type` 컬럼 타입을 바꿨다.
**처음 받아서 스키마를 새로 만드는 경우에는 할 일이 없다.** 이 저장소를 예전부터 쓰던
DB 에만 아래를 한 번 실행해야 한다.

```
/opt/homebrew/opt/mariadb/bin/mariadb --no-defaults \
  -e "USE odolog; ALTER TABLE maintenance_records MODIFY COLUMN type VARCHAR(30) NOT NULL;"
```

**왜 필요한가**: Hibernate 6 은 MariaDB 에서 `@Enumerated(STRING)` 을 varchar 가 아니라
네이티브 `enum('BATTERY','BRAKE_PAD',...)` 컬럼으로 만든다. `ddl-auto: update` 는 컬럼 타입을
바꿔 주지 않으므로, 자바 enum 에 값을 더해도 DB 컬럼은 옛 5개 그대로다 —
**새 종류를 저장하는 순간 데이터 잘림 오류가 난다.** 테스트 스키마는 매번 새로 만들어지므로
`./gradlew test` 는 멀쩡히 통과한다.

이번에 `@JdbcTypeCode(SqlTypes.VARCHAR)` 로 컬럼을 varchar 로 고정했으므로,
**이 ALTER 는 한 번만 하면 되고 앞으로 종류를 더 늘려도 다시 필요하지 않다.**
enum 은 값을 문자열로 저장하므로 기존 데이터는 그대로 보존된다.

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
    │       └── common/            인증(세션), 전역 예외 처리, 설정, BaseTimeEntity
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

주유 기록 목록은 다른 목록 API 와 달리 **`sort` 를 받지 않는다.** 연비가 "바로 앞 기록과의
주행거리 차이"로 계산되기 때문에 **정렬이 곧 계산의 전제**라, 서버가 주행거리 내림차순으로 고정한다.

응답의 `efficiency`(연비)·`distance`(구간 거리)는 **계산할 수 없으면 `0` 이 아니라 `null`** 이다.
첫 기록이거나 주행거리가 직전보다 크지 않으면 그렇다.

정비 이력과 주유 기록의 날짜는 **미래를 받지 않는다**(`@PastOrPresent`). 오늘은 통과한다 —
가장 흔한 입력이라 여기서 막히면 기능이 멈춘다.

주행거리 갱신은 기본적으로 **줄어들면 409** 다. 계기판은 되돌아가지 않기 때문이다.
다만 자리수를 잘못 넣었거나 계기판을 교체한 경우까지 막으면 되돌릴 방법이 없어지므로,
`{"odometer": 50000, "force": true}` 처럼 **의도를 밝히면** 낮출 수 있다.
화면은 입력값이 현재보다 작을 때 한 번 묻고 나서 이 플래그를 싣는다.

```json
PATCH /api/vehicles/1/odometer
{"odometer": 50000}                 → 409 (현재보다 작으면)
{"odometer": 50000, "force": true}  → 200
```

## 진행 상황

백엔드 API **24개**와 프론트엔드 화면 8장(라우트 기준. `/` 가 세 얼굴을 가져 실제로 볼 상태는
10개)이 모두 동작하는 상태다. 백엔드 테스트 **147개**, 프론트엔드 테스트 **30개**가 통과하고,
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

아래는 요약이다. **항목별로 "무엇을 봐야 하는지"까지 쪼갠 166개짜리 전체 목록은
`CLAUDE.md` 의 Phase 6** 에 있다 (준비 6 · 기능 한 바퀴 117 · 폭 22 · 테마 10 · 접근성 11).

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
- [ ] 만탱크 연비 — 지금은 매 주유마다 직전 기록과의 차이로 계산한다(단순법).
      **가득 채우지 않은 주유가 섞이면 그 구간만 실제보다 높게 나온다.** 기록에
      "가득 채웠는가" 플래그를 두면 가득→가득 구간으로 정확히 낼 수 있다.
      → 지금은 **평소 구간과 견줘 이상한 구간을 알려 주는 것**으로 대신하고 있다(중앙값의 1.8배).
- [ ] PWA (manifest + 아이콘 + 서비스 워커)
      → 웹으로 확정했으므로 "홈 화면 아이콘"을 얻는 유일한 길이다. 하면 `theme-color` 가
        네 곳 중복이 된다(`index.css`·`index.html`·`ThemeProvider.tsx`·manifest).
- [ ] 로그인 실패 응답 시간이 계정 존재 여부에 따라 다르다
      → 이메일이 없으면 BCrypt 검증을 건너뛰어 빨리 답한다. 실패 메시지를 일부러 통일해
        둔 방침과 어긋나는 지점이라 언젠가 따져 볼 것.
- [ ] **배포한다면 먼저 따질 것 셋** (로컬에서는 문제가 아니다)
      → **CSRF 토큰이 없다.** 세션 쿠키 인증인데 Spring Security 를 안 써서 보호 장치가 없다.
        지금은 CORS 가 `localhost:5173` 만 허용하고 쿠키가 `SameSite=Lax` 라 브라우저가 막아 준다.
      → **로그인 시도 제한이 없다.** 브루트포스를 막는 것이 아무것도 없다.
      → **쿠키에 `secure` 가 없다.** 로컬이 http 라 켜지 않았다 — HTTPS 로 올리면 켜야 한다.

### 완료 판정 기준

위 0번을 처음부터 끝까지 막힘없이 수행할 수 있고, `./gradlew test` 가 통과하면 "완성"으로 본다.
(테스트 **147개**는 지금 통과 중이다. **남은 것은 사람 눈 확인 하나뿐이다.**)
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
