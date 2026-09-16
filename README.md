# 오도로그 (OdoLog)

차량 관리 앱. 자기 차량을 등록하고 **정비 이력과 주유 기록**을 관리한다.
주유 기록이 쌓이면 **연비(km/L)** 가 계산된다.

플랫폼은 **웹 하나**다. 네이티브 앱은 만들지 않는다 — 이유는 `CLAUDE.md` 의 2026-09-16 항목에.

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

## 프로젝트 구조

백엔드와 프론트엔드 모두 기능별(package-by-feature)로 구성되어 있다.

    odolog/
    ├── src/                       백엔드 (Spring Boot)
    │   └── main/java/com/odolog/app/
    │       ├── user/              회원가입, 로그인/로그아웃, 프로필, 비밀번호 변경
    │       ├── vehicle/           차량 등록·조회·수정·주행거리 갱신·삭제
    │       ├── maintenance/       정비 이력, 다음 정비 시점 계산
    │       ├── fuel/              주유 기록, 연비 계산
    │       ├── account/           회원 탈퇴 — 여러 기능을 조율하는 자리
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
`feedback` / `nav` / `brand` 로 나뉜다.

프론트엔드의 의존 방향은 `app → features → shared` 한 방향이다. `app`은 여러 기능을 동시에
알아도 되는 유일한 층이라, `useAuth`를 쓰는 `Header`와 `ProtectedRoute`가 여기에 있다.

백엔드는 `fuel → vehicle → user`, `maintenance → vehicle → user` 이고, **`account` 가
프론트의 `app` 과 같은 자리**다. 회원 탈퇴는 회원·차량·정비 이력·주유 기록을 모두 지워야 하는데,
이걸 `UserService` 에 넣으면 `user → vehicle` 역방향 의존이 생긴다. `account` 는 **순서만 정하고
실제 삭제는 각 기능에 맡긴다.**

시간 필드(`createdAt`/`updatedAt`)는 네 엔티티가 `common/domain/entity/BaseTimeEntity` 를
상속해서 얻는다. 엔티티가 3개일 때는 `@PrePersist` 를 복사하는 편이 나았고, 4개째에서 뒤집혔다.

설계 결정과 진행 상황은 `CLAUDE.md`에 상세히 기록되어 있다.

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
| 차량 등록/목록조회 | `POST`, `GET /api/vehicles` |
| 차량 상세조회 | `GET /api/vehicles/{vehicleId}` |
| 차량 정보 수정 | `PATCH /api/vehicles/{vehicleId}` |
| 주행거리 갱신 | `PATCH /api/vehicles/{vehicleId}/odometer` |
| 차량 삭제 | `DELETE /api/vehicles/{vehicleId}` |
| 정비 이력 등록/목록조회 | `POST`, `GET /api/vehicles/{vehicleId}/maintenance-records` |
| 정비 이력 상세조회/수정/삭제 | `GET`/`PATCH`/`DELETE /api/vehicles/{vehicleId}/maintenance-records/{recordId}` |
| 다음 정비 시점 조회 | `GET /api/vehicles/{vehicleId}/maintenance-records/next-service?type=` |
| 주유 기록 등록/목록조회 | `POST`, `GET /api/vehicles/{vehicleId}/fuel-records` |
| 주유 기록 상세조회/수정/삭제 | `GET`/`PATCH`/`DELETE /api/vehicles/{vehicleId}/fuel-records/{recordId}` |
| 연비 요약 조회 | `GET /api/vehicles/{vehicleId}/fuel-records/summary` |

주유 기록 목록은 다른 목록 API 와 달리 **`sort` 를 받지 않는다.** 연비가 "바로 앞 기록과의
주행거리 차이"로 계산되기 때문에 **정렬이 곧 계산의 전제**라, 서버가 주행거리 내림차순으로 고정한다.

응답의 `efficiency`(연비)·`distance`(구간 거리)는 **계산할 수 없으면 `0` 이 아니라 `null`** 이다.
첫 기록이거나 주행거리가 직전보다 크지 않으면 그렇다.

## 진행 상황

백엔드 API **25개**와 프론트엔드 화면 8장(라우트 기준. `/` 가 세 얼굴을 가져 실제로 볼 상태는
10개)이 모두 동작하는 상태다. 백엔드 테스트 **106개**가 통과하고, 프론트엔드는
`tsc -b` / `oxlint` / `vite build` 를 통과한다.

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

아래는 요약이다. **항목별로 "무엇을 봐야 하는지"까지 쪼갠 156개짜리 전체 목록은
`CLAUDE.md` 의 Phase 6** 에 있다 (준비 6 · 기능 한 바퀴 93 · 폭 13 · 테마 10 · 접근성 7).

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
- [ ] 더 작은 값으로 갱신 → "주행거리는 줄어들 수 없습니다. (현재 50,000km)"
- [ ] 정비 이력 등록 → 목록에 보이고 "다음 정비 시점" 카드가 갱신됨
- [ ] 다음 정비 시점이 **주행거리·날짜 두 기준** 모두 표시되는지
- [ ] `OTHER`(기타) 종류는 권장 주기가 없어 계산되지 않는 것이 맞는지
- [ ] 정비 이력 수정 — 비용만 바꿨을 때 그 필드만 PATCH 되는지 (Network 탭 확인)
- [ ] 정비 이력 삭제 (204)
- [ ] **차량 정보 수정** — 제조사만 바꿨을 때 **409 가 나지 않는지**(번호판이 안 바뀌었으면
      중복 검사를 건너뛰어야 한다. 이 기능의 핵심 함정이다)
- [ ] **주유 기록 등록** → 첫 기록은 연비가 `—` 인지(`0.00` 이면 잘못된 것)
- [ ] **두 번째 주유 기록** → 연비가 나오는지. 주행거리 500km · 25L 이면 **20.00 km/L**
- [ ] **주유가 차량 주행거리를 따라 올리는지** — 위쪽 히어로 숫자가 그만큼 굴러가야 한다
- [ ] 주유 기록 11건 이상 → **2페이지 첫 행에도 연비가 나오는지**(그 행의 짝은 1페이지에 있다)
- [ ] **비밀번호 변경** — 현재 비밀번호를 틀렸을 때 **로그아웃되지 않는지**
- [ ] 로그아웃 후 **새 비밀번호로** 로그인되는지
- [ ] **회원 탈퇴** — 비밀번호 확인 후 `/` 로 이동, 그 계정으로 로그인 불가
- [ ] 차량 삭제 → **정비 이력과 주유 기록이** 함께 사라짐
- [ ] 다른 계정으로 로그인 → 앞 계정의 차량이 보이지 않음

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

- [ ] 홈 통계 요약 API (`GET /api/vehicles/summary`)
      → 지금은 프론트가 차량 목록 1번 + 차량마다 정비 이력 1번씩 받아 **직접 더한다.**
        건수는 `totalElements` 라 정확하지만 **합계는 받아 온 행만 더한 값**이라 상한
        (차량 100 / 이력 200건)을 넘으면 일부만 반영된다. 화면에 "일부 기록만 합산됨" 으로
        **표시는 하고 있다** — 틀릴 수 있는 값을 맞는 값처럼 보여주지 않으려고.
        SQL 한 번이면 정확하고 요청도 1번이다.
- [ ] 다음 정비 시점을 **전체 종류 한 번에** 반환하는 API (`GET .../next-services`)
      → 지금은 화면 하나를 그리는 데 요청이 5번 나간다.
- [ ] **정비 이력** 등록 시 `serviceOdometer` 가 차량 `odometer` 보다 크면 차량 주행거리 자동 갱신
      → **주유 기록에는 이미 넣었다.** 주유가 훨씬 잦아 그쪽부터 했고, 같은 규칙을 정비에도
        옮길지는 화면을 써 보고 정한다.
- [ ] 정비 이력 종류별 필터링 (`GET .../maintenance-records?type=`)
- [ ] 만탱크 연비 — 지금은 매 주유마다 직전 기록과의 차이로 계산한다(단순법).
      **가득 채우지 않은 주유가 섞이면 그 구간만 실제보다 높게 나온다.** 기록에
      "가득 채웠는가" 플래그를 두면 가득→가득 구간으로 정확히 낼 수 있다.
- [ ] PWA (manifest + 아이콘 + 서비스 워커)
      → 웹으로 확정했으므로 "홈 화면 아이콘"을 얻는 유일한 길이다. 하면 `theme-color` 가
        네 곳 중복이 된다(`index.css`·`index.html`·`ThemeProvider.tsx`·manifest).
- [ ] 로그인 실패 응답 시간이 계정 존재 여부에 따라 다르다
      → 이메일이 없으면 BCrypt 검증을 건너뛰어 빨리 답한다. 실패 메시지를 일부러 통일해
        둔 방침과 어긋나는 지점이라 언젠가 따져 볼 것.

### 완료 판정 기준

위 0번을 처음부터 끝까지 막힘없이 수행할 수 있고, `./gradlew test` 가 통과하면 "완성"으로 본다.
(테스트 **106개**는 지금 통과 중이다. **남은 것은 사람 눈 확인 하나뿐이다.**)
배포(서버 인프라, 도메인, CI/CD)는 이 프로젝트의 범위 밖이며, **로컬에서 완전히 동작하는 것**까지가 목표다.

## 트러블슈팅

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
