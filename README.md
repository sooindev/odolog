# 오도로그 (OdoLog)

차량 관리 앱. 사용자가 자기 차량을 등록하고 정비 이력을 관리한다.

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
    │       ├── user/              회원가입, 로그인/로그아웃, 프로필
    │       ├── vehicle/           차량 등록·조회·주행거리 갱신·삭제
    │       ├── maintenance/       정비 이력, 다음 정비 시점 계산
    │       └── common/            인증(세션), 전역 예외 처리, 설정 등 공통 인프라
    └── frontend/                  프론트엔드 (Vite + React + TypeScript)
        └── src/
            ├── features/          auth / vehicles / maintenance
            └── shared/            api 클라이언트, 포맷 함수, 레이아웃, UI 컴포넌트

각 기능 패키지는 `domain / repository / dto / service / controller`로 나뉘고,
`dto`는 다시 `request / response`로 나뉜다.
설계 결정과 진행 상황은 `CLAUDE.md`에 상세히 기록되어 있다.

프론트엔드 실행 방법은 `frontend/README.md` 참고.

## API 개요

| 기능 | 엔드포인트 |
|---|---|
| 회원가입 | `POST /api/users` |
| 로그인 | `POST /api/users/login` |
| 로그아웃 | `POST /api/users/logout` |
| 내 정보 조회/수정 | `GET`, `PATCH /api/users/me` |
| 차량 등록/목록조회 | `POST`, `GET /api/vehicles` |
| 차량 상세조회 | `GET /api/vehicles/{vehicleId}` |
| 주행거리 갱신 | `PATCH /api/vehicles/{vehicleId}/odometer` |
| 차량 삭제 | `DELETE /api/vehicles/{vehicleId}` |
| 정비 이력 등록/목록조회 | `POST`, `GET /api/vehicles/{vehicleId}/maintenance-records` |
| 정비 이력 상세조회/수정/삭제 | `GET`/`PATCH`/`DELETE /api/vehicles/{vehicleId}/maintenance-records/{recordId}` |
| 다음 정비 시점 조회 | `GET /api/vehicles/{vehicleId}/maintenance-records/next-service?type=` |

## 진행 상황

백엔드 API(16개)와 프론트엔드 화면은 모두 동작하는 상태다. 백엔드 테스트 56개가 통과하고,
프론트엔드는 타입 검사·린트를 통과한다. 남은 것은 **다듬기(Phase 6)** 와 **브라우저 실동작 확인**이다.

| 단계 | 내용 | 상태 |
|---|---|---|
| Phase 1 | 백엔드 API 표면 (단건 조회, 페이지네이션, CORS, Swagger) | 완료 |
| Phase 2 | 프론트엔드 셋업 (Vite/Tailwind/shadcn/API 클라이언트) | 완료 |
| Phase 3 | 인증 화면 (회원가입·로그인·보호 라우트) | 완료 |
| Phase 4 | 차량 관리 화면 | 완료 |
| Phase 5 | 정비 이력 화면 + 다음 정비 시점 | 완료 |
| Phase 6 | 다듬기 (로딩·에러·반응형·접근성) | 진행 중 |

Phase 6에서 이미 끝난 것: 조회 로직 공용 훅(`useAsyncData`) 추출, 로딩·에러 표시 통일
(`LoadingText`/`ErrorText`), 삭제 버튼 중복 클릭 잠금, 숫자 포맷(`formatKm`/`formatWon`).

## 남은 작업

번호는 **권장 순서**다. 0번은 코드로 판정할 수 없어 사람이 직접 해야 하고,
나머지는 뒤로 갈수록 손대는 파일이 많아진다.

### 0. 브라우저 실동작 확인

여기까지 코드는 다 있지만 **브라우저에서 끝까지 눌러본 적이 없다.** 아래가 곧 완료 판정 기준이다.
백엔드(IntelliJ `OdoLogApplication`)와 프론트(`cd frontend && npm run dev`)를 함께 띄우고
`http://localhost:5173` 에서 확인한다.

- [ ] 회원가입 → 가입 직후 자동 로그인되어 차량 목록으로 이동
- [ ] DevTools → Application → Cookies 에 `JSESSIONID` 가 있는지
- [ ] 새로고침해도 로그인 유지 (`GET /api/users/me` 1회 호출로 복구)
- [ ] 로그아웃 → 주소창에 `/vehicles` 직접 입력 → `/login` 으로 이동
- [ ] 차량이 없을 때 빈 상태 + "첫 차량 등록하기" 버튼
- [ ] 차량 등록 → 목록에 보임 → 상세 진입
- [ ] 같은 번호판을 한 번 더 등록 → 409 가 폼 에러로 표시
- [ ] 주행거리 갱신 성공
- [ ] 더 작은 값으로 갱신 → "주행거리는 줄어들 수 없습니다. (현재 50,000km)"
- [ ] 정비 이력 등록 → 목록에 보이고 "다음 정비 시점" 카드가 갱신됨
- [ ] 다음 정비 시점이 **주행거리·날짜 두 기준** 모두 표시되는지
- [ ] `OTHER`(기타) 종류는 권장 주기가 없어 계산되지 않는 것이 맞는지
- [ ] 정비 이력 수정 — 비용만 바꿨을 때 그 필드만 PATCH 되는지 (Network 탭 확인)
- [ ] 정비 이력 삭제 (204)
- [ ] 차량 삭제 → 정비 이력도 함께 사라짐
- [ ] 다른 계정으로 로그인 → 앞 계정의 차량이 보이지 않음

### 1. 페이지 타이틀 · 파비콘

가장 작고, 눈에 바로 띄는 항목.

- [ ] `frontend/index.html` 의 `<title>frontend</title>` → `오도로그`
- [ ] `frontend/public/favicon.svg` 가 아직 **Vite 기본 로고**다 — 교체하거나 유지 결정
- [ ] 라우트별 `document.title` — 훅 하나(`shared/lib/useDocumentTitle.ts`)를 만들어 재사용
- [ ] 적용 대상 라우트 6개: `/login` `/signup` `/vehicles` `/vehicles/new` `/vehicles/:vehicleId` `/me`
- [ ] 차량 상세는 차량 이름이 들어가야 함 — 데이터 로드 후에 제목이 정해지는 순서 주의

### 2. 목록 로딩을 스켈레톤으로

`LoadingText` 한 곳으로 이미 모여 있어서 바꿀 지점이 명확하다. 현재 사용처는 5곳.

- [ ] 스켈레톤을 shadcn(`npx shadcn@latest add skeleton`)으로 받을지 직접 쓸지 결정
- [ ] 차량 목록(`VehicleListPage`) — 카드 3개 모양
- [ ] 정비 이력 목록(`MaintenanceSection`) — 행 3개 모양
- [ ] 다음 정비 시점(`NextServiceCard`) — 5줄 모양
- [ ] 차량 상세(`VehicleDetailPage`)와 `ProtectedRoute` 는 **일부러 텍스트로 남길지** 판단
      (화면 전체가 잠깐 뜨는 경우라 스켈레톤이 오히려 산만할 수 있다)

### 3. 에러 표시 구분 (인라인 vs 토스트)

지금은 모든 에러가 `ErrorText` 인라인이다. 토스트는 아직 한 줄도 없다.

- [ ] 기준 확정: **폼 검증 실패(400/409)는 인라인**, 그 외(500·네트워크 끊김)는 토스트
- [ ] `ApiError.status` 로 분기 — `client.ts` 가 이미 status 를 들고 있어 추가 작업 없음
- [ ] 토스트 구현 선택: shadcn `sonner` 도입 vs 직접 만든 최소 구현
- [ ] Provider 를 `main.tsx` 의 어느 층에 끼울지 (`AuthProvider` 안쪽/바깥쪽)
- [ ] 전환 1순위: 행동 실패(`actionError`) — 정비 이력 삭제, 차량 삭제
- [ ] 조회 실패(`error`)는 인라인 유지 — 화면에 대체할 내용이 없어 계속 보여야 한다

### 4. 반응형

현재 `sm:`/`md:`/`lg:` 가 전체 화면 통틀어 4곳뿐이라 사실상 미착수다.
차량 관리 앱은 정비소·주차장에서 폰으로 볼 가능성이 높아 **모바일 우선**으로 잡는다.

- [ ] 차량 목록 — 1열 → `md` 2열 → `lg` 3열 그리드
- [ ] 차량 상세의 정보 `dl` — 모바일 1열, 데스크톱 2열
- [ ] 정비 이력 한 줄(`45,000km · 50,000원`) — 좁은 화면에서 줄바꿈 처리
- [ ] 폼(로그인·회원가입·차량 등록·정비) 최대 너비 지정 — 데스크톱에서 가로로 늘어지지 않게
- [ ] `Header` — 좁을 때 닉네임/로그아웃이 겹치지 않는지
- [ ] 확인 폭: 375px(iPhone SE) / 768px / 1280px

### 5. 접근성 기본

- [ ] `<Label htmlFor>` 와 입력의 `id` 짝이 전부 맞는지 확인 (라벨 19개 / 입력 18개)
- [ ] 정비 종류 `<select>` 에 포커스 링이 없다 — `Input` 과 스타일을 맞출지 결정
      (`MaintenanceForm.tsx`, 브라우저 기본 포커스만 있는 상태)
- [ ] 에러 메시지(`ErrorText`)에 `role="alert"` — 스크린리더가 변화를 읽도록
- [ ] 아이콘만 있는 버튼이 생기면 `aria-label` (현재는 전부 텍스트 버튼)
- [ ] 삭제 확인용 `window.confirm` 2곳을 다이얼로그로 바꿀지 결정 (Phase 4에서 미룬 항목)
- [ ] 페이지네이션 버튼의 비활성 상태가 시각·스크린리더 양쪽에 전달되는지

### 6. 백엔드 400 응답을 폼 필드에 연결 (할지 말지부터 결정)

현재 `ErrorResponse` 는 `message` 하나뿐이라 **어느 필드가 틀렸는지 프론트가 알 수 없다.**
지금은 프론트 입력 요소의 `required`/`min`/`max` 가 1차로 막고 있어 급하지 않다.

- [ ] 도입 여부 결정 — 안 하면 백로그로 남긴다
- [ ] (도입 시) `ErrorResponse` 에 `fieldErrors` 추가
- [ ] (도입 시) `GlobalExceptionHandler` 에서 `BindingResult` 의 필드명·메시지 추출
- [ ] (도입 시) 프론트 `shared/api/types.ts` 갱신
- [ ] (도입 시) 폼 5곳에서 필드 아래 표시로 연결

### 완료 판정 기준

위 0번을 처음부터 끝까지 막힘없이 수행할 수 있고, `./gradlew test` 가 통과하면 "완성"으로 본다.
배포(서버 인프라, 도메인, CI/CD)는 이 프로젝트의 범위 밖이며, **로컬에서 완전히 동작하는 것**까지가 목표다.

## 트러블슈팅

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
