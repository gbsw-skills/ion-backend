# Ion API 명세서

> Ion 백엔드 REST API 상세 명세  
> Base URL: `/api/v1`  
> 작성일: 2026-04-21 | 버전: 1.0

---

## 목차

- [공통 규약](#공통-규약)
- [인증 API](#인증-api)
- [채팅 API](#채팅-api)
- [공지사항 API](#공지사항-api)
- [관리자 API (Post-MVP)](#관리자-api-post-mvp)
- [에러 코드 목록](#에러-코드-목록)

---

## 공통 규약

### 요청 헤더

| 헤더 | 필수 여부 | 설명 |
|------|-----------|------|
| `Content-Type` | 필수 (Body 있는 요청) | `application/json` |
| `Authorization` | 인증 필요 시 필수 | `Bearer {accessToken}` |

### 공통 응답 포맷

**성공**

```json
{
  "success": true,
  "data": { ... },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

**실패**

```json
{
  "success": false,
  "data": null,
  "error": {
    "code": "AUTH_001",
    "message": "인증 토큰이 유효하지 않습니다."
  },
  "timestamp": "2026-04-21T10:00:00Z"
}
```

### 페이징 응답 포맷

```json
{
  "success": true,
  "data": {
    "content": [ ... ],
    "page": 0,
    "size": 20,
    "totalElements": 100,
    "totalPages": 5,
    "last": false
  }
}
```

---

## 인증 API

### POST `/api/v1/auth/login` — 로그인

> 인증 불필요

**Request Body**

```json
{
  "username": "student01",
  "password": "plaintext-password"
}
```

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| username | string | ✅ | 로그인 ID |
| password | string | ✅ | 비밀번호 (평문, HTTPS 전송) |

**Response 200 OK**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900,
    "user": {
      "id": 42,
      "username": "student01",
      "displayName": "홍길동",
      "role": "STUDENT"
    }
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

**오류 케이스**

| 상황 | HTTP | 에러 코드 |
|------|------|-----------|
| 아이디/비밀번호 불일치 | 401 | `AUTH_002` |
| 요청 형식 오류 | 400 | `COMMON_002` |

---

### POST `/api/v1/auth/refresh` — 토큰 갱신

> 인증 불필요

**Request Body**

```json
{
  "refreshToken": "eyJhbGciOiJIUzI1NiJ9..."
}
```

**Response 200 OK**

```json
{
  "success": true,
  "data": {
    "accessToken": "eyJhbGciOiJIUzI1NiJ9...",
    "refreshToken": "eyJhbGciOiJIUzI1NiJ9...",
    "tokenType": "Bearer",
    "expiresIn": 900
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

**오류 케이스**

| 상황 | HTTP | 에러 코드 |
|------|------|-----------|
| Refresh Token 만료 또는 무효 | 401 | `AUTH_001` |

---

### POST `/api/v1/auth/logout` — 로그아웃

> `Authorization` 헤더 필요

**Request Body** — 없음

**Response 200 OK**

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

## 채팅 API

### POST `/api/v1/chat/sessions` — 새 채팅 세션 생성

> 인증 필요

**Request Body** — 없음 (서버가 자동 UUID 생성)

**Response 201 Created**

```json
{
  "success": true,
  "data": {
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "title": "새 대화",
    "createdAt": "2026-04-21T10:00:00Z",
    "lastActiveAt": "2026-04-21T10:00:00Z"
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

### GET `/api/v1/chat/sessions` — 세션 목록 조회

> 인증 필요 | 본인 세션만 조회됨

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | integer | 0 | 페이지 번호 |
| size | integer | 20 | 페이지 크기 |

**Response 200 OK**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "title": "급식 메뉴 문의",
        "lastActiveAt": "2026-04-21T10:00:00Z",
        "createdAt": "2026-04-21T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 5,
    "totalPages": 1,
    "last": true
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

### DELETE `/api/v1/chat/sessions/{sessionId}` — 세션 삭제

> 인증 필요 | 본인 세션만 삭제 가능

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| sessionId | UUID | 채팅 세션 ID |

**Response 200 OK**

```json
{
  "success": true,
  "data": null,
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

**오류 케이스**

| 상황 | HTTP | 에러 코드 |
|------|------|-----------|
| 세션 없음 | 404 | `CHAT_001` |
| 타인 세션 접근 | 403 | `AUTH_003` |

---

### POST `/api/v1/chat/sessions/{sessionId}/messages` — 질의 전송

> 인증 필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| sessionId | UUID | 채팅 세션 ID |

**Request Body**

```json
{
  "content": "내일 급식 메뉴가 뭐야?"
}
```

| 필드 | 타입 | 필수 | 제약 |
|------|------|------|------|
| content | string | ✅ | 최대 2000자, 공백 불가 |

**Response 202 Accepted**

서버는 메시지를 저장하고 LLM 처리를 비동기 시작. 응답 스트리밍은 `/stream` 엔드포인트에서 수신.

```json
{
  "success": true,
  "data": {
    "messageId": 1234,
    "sessionId": "550e8400-e29b-41d4-a716-446655440000",
    "role": "user",
    "content": "내일 급식 메뉴가 뭐야?",
    "createdAt": "2026-04-21T10:00:00Z"
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

**오류 케이스**

| 상황 | HTTP | 에러 코드 |
|------|------|-----------|
| 세션 없음 | 404 | `CHAT_001` |
| 메시지 내용 없음 | 400 | `CHAT_002` |

---

### GET `/api/v1/chat/sessions/{sessionId}/stream` — SSE 스트리밍 수신

> 인증 필요 | `Accept: text/event-stream` 헤더 권장

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| sessionId | UUID | 채팅 세션 ID |

**Response — `text/event-stream`**

LLM이 토큰을 생성할 때마다 SSE 이벤트로 전송.

```
event: token
data: {"token": "내일"}

event: token
data: {"token": " 급식"}

event: token
data: {"token": " 메뉴는"}

event: done
data: {"messageId": 1235, "finishReason": "stop"}
```

| 이벤트 | 설명 |
|--------|------|
| `token` | AI 응답 토큰 조각 |
| `done` | 스트리밍 완료, `messageId`는 저장된 assistant 메시지 ID |
| `error` | LLM 오류 발생 시 에러 정보 |

**오류 이벤트**

```
event: error
data: {"code": "LLM_001", "message": "LLM 서버에 연결할 수 없습니다."}
```

---

### GET `/api/v1/chat/sessions/{sessionId}/messages` — 메시지 내역 조회

> 인증 필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| sessionId | UUID | 채팅 세션 ID |

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | integer | 0 | 페이지 번호 |
| size | integer | 20 | 페이지 크기 |

**Response 200 OK**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1234,
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "role": "user",
        "content": "내일 급식 메뉴가 뭐야?",
        "createdAt": "2026-04-21T10:00:00Z",
        "sourceRefs": []
      },
      {
        "id": 1235,
        "sessionId": "550e8400-e29b-41d4-a716-446655440000",
        "role": "assistant",
        "content": "내일 급식 메뉴는 ...",
        "createdAt": "2026-04-21T10:00:05Z",
        "sourceRefs": [
          { "refType": "notice", "refId": 42, "title": "4월 22일 급식 안내" }
        ]
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 2,
    "totalPages": 1,
    "last": true
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

## 공지사항 API

### GET `/api/v1/notices` — 공지사항 목록 조회

> 인증 필요

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | integer | 0 | 페이지 번호 |
| size | integer | 20 | 페이지 크기 |
| keyword | string | (없음) | 제목 검색 (선택) |

**Response 200 OK**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 42,
        "title": "4월 22일 급식 안내",
        "authorName": "행정실",
        "publishedAt": "2026-04-20T09:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 50,
    "totalPages": 3,
    "last": false
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

### GET `/api/v1/notices/{id}` — 공지사항 단건 조회

> 인증 필요

**Path Parameters**

| 파라미터 | 타입 | 설명 |
|----------|------|------|
| id | long | 공지사항 ID |

**Response 200 OK**

```json
{
  "success": true,
  "data": {
    "id": 42,
    "title": "4월 22일 급식 안내",
    "content": "내일 급식은 ...",
    "authorName": "행정실",
    "publishedAt": "2026-04-20T09:00:00Z",
    "createdAt": "2026-04-20T08:50:00Z"
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

**오류 케이스**

| 상황 | HTTP | 에러 코드 |
|------|------|-----------|
| 공지 없음 | 404 | `NOTICE_001` |

---

## 관리자 API (Post-MVP)

> 모든 관리자 API는 `Authorization` 헤더 필수 + `ADMIN` role 필요  
> role 불일치 시 `403 AUTH_003` 반환

---

### POST `/api/v1/admin/notices` — 공지사항 등록

**Request Body**

```json
{
  "title": "공지사항 제목",
  "content": "공지사항 본문",
  "publishedAt": "2026-04-22T09:00:00Z"
}
```

**Response 201 Created**

```json
{
  "success": true,
  "data": {
    "id": 43,
    "title": "공지사항 제목",
    "publishedAt": "2026-04-22T09:00:00Z",
    "createdAt": "2026-04-21T10:00:00Z"
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

### PUT `/api/v1/admin/notices/{id}` — 공지사항 수정

**Request Body**

```json
{
  "title": "수정된 제목",
  "content": "수정된 본문",
  "publishedAt": "2026-04-22T09:00:00Z"
}
```

**Response 200 OK** — 수정된 공지사항 반환

---

### DELETE `/api/v1/admin/notices/{id}` — 공지사항 삭제

**Response 200 OK**

---

### POST `/api/v1/admin/documents` — 학사 문서 업로드

**Request** — `multipart/form-data`

| 필드 | 타입 | 필수 | 설명 |
|------|------|------|------|
| file | File | ✅ | PDF, DOCX (최대 50MB) |
| title | string | ✅ | 문서 제목 |

**Response 201 Created**

```json
{
  "success": true,
  "data": {
    "id": 10,
    "title": "2026 학사일정",
    "fileType": "pdf",
    "uploadedAt": "2026-04-21T10:00:00Z"
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

### GET `/api/v1/admin/documents` — 문서 목록 조회

**Query Parameters**: `page`, `size` (공통 페이징)

---

### DELETE `/api/v1/admin/documents/{id}` — 문서 삭제

**Response 200 OK**

---

### GET `/api/v1/admin/logs` — 감사 로그 조회

**Query Parameters**

| 파라미터 | 타입 | 기본값 | 설명 |
|----------|------|--------|------|
| page | integer | 0 | |
| size | integer | 20 | |
| adminId | long | (없음) | 특정 관리자 필터 |

**Response 200 OK**

```json
{
  "success": true,
  "data": {
    "content": [
      {
        "id": 1,
        "adminName": "관리자",
        "action": "DELETE_NOTICE",
        "targetType": "notice",
        "targetId": 42,
        "createdAt": "2026-04-21T10:00:00Z"
      }
    ],
    "page": 0,
    "size": 20,
    "totalElements": 10,
    "totalPages": 1,
    "last": true
  },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

---

## 에러 코드 목록

| 코드 | HTTP 상태 | 설명 |
|------|-----------|------|
| `AUTH_001` | 401 | 인증 토큰 없음 또는 만료 |
| `AUTH_002` | 401 | 아이디 또는 비밀번호 불일치 |
| `AUTH_003` | 403 | 해당 리소스에 대한 권한 없음 |
| `CHAT_001` | 404 | 채팅 세션을 찾을 수 없음 |
| `CHAT_002` | 400 | 메시지 내용이 비어 있음 |
| `CHAT_003` | 404 | 채팅 메시지를 찾을 수 없음 |
| `NOTICE_001` | 404 | 공지사항을 찾을 수 없음 |
| `DOCUMENT_001` | 404 | 문서를 찾을 수 없음 |
| `DOCUMENT_002` | 400 | 지원하지 않는 파일 형식 |
| `DOCUMENT_003` | 413 | 파일 크기 초과 (50MB 제한) |
| `LLM_001` | 503 | LLM 서버 연결 실패 |
| `LLM_002` | 504 | LLM 응답 타임아웃 |
| `COMMON_001` | 500 | 서버 내부 오류 |
| `COMMON_002` | 400 | 요청 유효성 검사 실패 (필수 필드 누락 등) |

---

> API 명세는 초안이며 팀 협의 후 변경될 수 있습니다.  
> 전체 백엔드 설계는 [`Ion_백엔드_개발기획서.md`](./Ion_백엔드_개발기획서.md)를 참고하세요.
