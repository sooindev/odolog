# 오도로그 (OdoLog)

차량 관리 앱. 사용자가 자기 차량을 등록하고 정비 이력을 관리한다.

## 기술 스택

- Spring Boot 3.5.6 / Java 17 / Gradle
- Spring Data JPA (Hibernate 6.6.x)
- MariaDB 12.3.2

## 실행 방법

- IntelliJ IDEA에서 `OdoLogApplication`을 실행한다.
- 실행 구성의 환경변수로 `DB_USERNAME`, `DB_PASSWORD`를 설정해야 한다 (평문 비밀번호를 파일에 적지 않음).
- 빌드/컴파일만 확인할 때는 터미널에서 `./gradlew build`.

## 패키지 구조

기능별(package-by-feature)로 구성되어 있다.

    src/main/java/com/odolog/app/
    ├── user/          회원가입, 로그인/로그아웃, 프로필
    ├── vehicle/        차량 등록·조회·주행거리 갱신·삭제
    ├── maintenance/    정비 이력, 다음 정비 시점 계산
    └── common/         인증(세션), 전역 예외 처리, 설정 등 공통 인프라

각 기능 패키지는 `domain / repository / dto / service / controller`로 나뉜다.
설계 결정과 진행 상황은 `CLAUDE.md`에 상세히 기록되어 있다.

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
| 정비 이력 등록/조회 | `POST`, `GET /api/vehicles/{vehicleId}/maintenance-records` |
| 다음 정비 시점 조회 | `GET /api/vehicles/{vehicleId}/maintenance-records/next-service?type=` |

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

참고로 `user@localhost` 계정은 `unix_socket` 인증이라 비밀번호 없이 붙는다. 애플리케이션이
쓰는 계정이 아니라 진단(테이블 확인 등) 용도로만 사용한다.
