# 오도로그 프론트엔드

Vite + React + TypeScript. 백엔드(Spring Boot)는 저장소 루트의 `src/`에 있다.

## 실행

```
npm install
npm run dev     # http://localhost:5173
```

**백엔드가 http://localhost:8080 에 떠 있어야 한다.** 세션 쿠키 인증이라
백엔드 CORS 설정(`WebConfig`)의 `allowedOrigins`와 이 개발 서버 포트가 일치해야 한다.

## 스크립트

| 명령 | 설명 |
|---|---|
| `npm run dev` | 개발 서버 |
| `npm run build` | 타입 검사(`tsc -b`) 후 프로덕션 빌드 |
| `npm run lint` | oxlint |
| `npm run preview` | 빌드 결과 미리보기 |

프로젝트 전체 구조와 설계 결정은 저장소 루트의 `README.md`와 `CLAUDE.md` 참고.
