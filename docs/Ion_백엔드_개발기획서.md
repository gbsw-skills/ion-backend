# Ion 백엔드 개발 기획서

> Ion — 경북소마고 전용 AI 챗봇 백엔드 개발 기획서  
> 작성일: 2026-04-21 | 버전: 1.0

---

## 목차

1. [개요 및 목적](#1-개요-및-목적)
2. [시스템 아키텍처](#2-시스템-아키텍처)
3. [모듈 설계](#3-모듈-설계)
4. [데이터베이스 설계](#4-데이터베이스-설계)
5. [AI 연동 설계 (OpenAI API 규격)](#5-ai-연동-설계-openai-api-규격)
6. [메시지 큐 설계 (RabbitMQ)](#6-메시지-큐-설계-rabbitmq)
7. [캐시 설계 (Redis)](#7-캐시-설계-redis)
8. [인증 및 보안](#8-인증-및-보안)
9. [에러 처리 및 로깅](#9-에러-처리-및-로깅)
10. [개발 환경 및 빌드](#10-개발-환경-및-빌드)
11. [테스트 전략](#11-테스트-전략)
12. [개발 일정](#12-개발-일정)

---

## 1. 개요 및 목적

Ion은 경북소마고 학생·교직원을 위한 전용 AI 챗봇 서비스다.  
본 문서는 Ion의 **백엔드(Spring Boot)** 범위 — REST API 서버, DB, 메시지 큐, 캐시, 인증, AI 연동 — 를 기술한 개발 기획서다.

### 핵심 목표

| 목표 | 내용 |
|------|------|
| 전용 AI 답변 | 학교 공지사항·학사 문서를 기반으로 한 맥락적 답변 제공 |
| 실시간 스트리밍 | SSE(Server-Sent Events)를 통한 타이핑 효과 답변 출력 |
| 관리자 기능 | 공지사항 및 학사 문서 관리, 감사 로그 |
| 확장성 | 비동기 처리 구조로 다중 동시 요청 대응 |

---

## 2. 시스템 아키텍처

### 2.1 전체 구성도

```
[클라이언트 (Mobile App / Web)]
          │  HTTPS
          ▼
   ┌─────────────┐
   │  Spring Boot │  ← 메인 API 서버
   │   API Server │
   └──────┬───────┘
          │
    ┌─────┼──────┐
    │     │      │
    ▼     ▼      ▼
 [PostgreSQL] [Redis] [RabbitMQ]
                          │
                          ▼
                   ┌─────────────┐
                   │  LLM Worker │  ← OpenAI API 규격 호환 엔드포인트
                   │   Service   │
                   └─────────────┘
```

### 2.2 레이어별 역할

| 레이어 | 기술 | 역할 |
|--------|------|------|
| API 서버 | Spring Boot 4.x | REST API, SSE, JWT 인증, 비즈니스 로직 |
| 데이터베이스 | PostgreSQL 16 | 사용자, 채팅, 공지사항, 문서 영속화 |
| 캐시 | Redis 7 | 세션 토큰, 공지사항 캐싱 |
| 메시지 큐 | RabbitMQ 3.x | LLM 요청 비동기 처리 |
| LLM | OpenAI API 규격 호환 서버 | AI 답변 생성 (스트리밍 포함) |

### 2.3 비동기 처리 흐름

LLM 추론은 수 초가 소요되므로 비동기 처리 + SSE 조합을 사용한다.

```
클라이언트                  API 서버              RabbitMQ           LLM Worker
    │                          │                     │                    │
    │── POST /messages ──────▶ │                     │                    │
    │                          │── Publish ─────────▶│                    │
    │◀── 202 Accepted ─────── │                     │── Consume ────────▶│
    │                          │                     │                    │ (추론 중)
    │── GET /stream (SSE) ───▶ │                     │                    │
    │                          │◀─── Result ─────────┤◀── Publish ───────│
    │◀── data: {token} ─────── │                     │                    │
    │◀── data: [DONE] ──────── │                     │                    │
```

---

## 3. 모듈 설계

### 3.1 모듈 목록

| 모듈 | 패키지 | 역할 |
|------|--------|------|
| auth | `com.ion.auth` | JWT 발급·검증, 로그인/로그아웃 |
| user | `com.ion.user` | 사용자 CRUD, 프로필 |
| chat | `com.ion.chat` | 채팅 세션·메시지 관리, SSE 스트리밍 |
| llm | `com.ion.llm` | OpenAI API 규격 클라이언트, 응답 파싱 |
| notice | `com.ion.notice` | 공지사항 조회 |
| document | `com.ion.document` | 학사 문서 업로드·관리 (관리자) |
| admin | `com.ion.admin` | 관리자 대시보드, 감사 로그 |
| common | `com.ion.common` | 공통 응답 포맷, 예외, 유틸 |

### 3.2 패키지 구조

```
com.ion
├── auth
│   ├── controller/AuthController.java
│   ├── service/AuthService.java
│   ├── dto/LoginRequest.java
│   └── jwt/JwtProvider.java
├── chat
│   ├── controller/ChatController.java
│   ├── service/ChatService.java
│   ├── service/SseEmitterService.java
│   ├── domain/ChatSession.java
│   └── domain/ChatMessage.java
├── llm
│   ├── client/OpenAiCompatibleClient.java
│   ├── dto/ChatCompletionRequest.java
│   └── dto/ChatCompletionResponse.java
├── notice
├── document
├── admin
└── common
    ├── response/ApiResponse.java
    ├── exception/GlobalExceptionHandler.java
    └── config/
```

---

## 4. 데이터베이스 설계

### 4.1 ERD 개요

PostgreSQL 사용. 핵심 엔티티는 `users`, `chat_sessions`, `chat_messages`, `notices`, `documents`, `admin_logs` 6개.

### 4.2 테이블 명세

#### users — 사용자

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| username | VARCHAR(50) | UNIQUE NOT NULL | 로그인 ID |
| password_hash | VARCHAR(255) | NOT NULL | bcrypt 해시 |
| role | VARCHAR(20) | NOT NULL | `STUDENT` / `TEACHER` / `ADMIN` |
| display_name | VARCHAR(100) | NOT NULL | 표시 이름 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| updated_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

#### chat_sessions — 채팅 세션

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | UUID | PK DEFAULT gen_random_uuid() | |
| user_id | BIGINT | FK → users.id NOT NULL | |
| title | VARCHAR(200) | | 자동 생성 또는 사용자 지정 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |
| last_active_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

#### chat_messages — 채팅 메시지

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| session_id | UUID | FK → chat_sessions.id NOT NULL | |
| role | VARCHAR(20) | NOT NULL | `user` / `assistant` / `system` |
| content | TEXT | NOT NULL | 메시지 본문 |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

#### source_refs — 출처 참조

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| message_id | BIGINT | FK → chat_messages.id NOT NULL | |
| ref_type | VARCHAR(20) | NOT NULL | `notice` / `document` |
| ref_id | BIGINT | NOT NULL | 참조 대상 PK |

#### notices — 공지사항

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| title | VARCHAR(300) | NOT NULL | |
| content | TEXT | NOT NULL | |
| author_id | BIGINT | FK → users.id | |
| published_at | TIMESTAMPTZ | | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

#### documents — 학사 문서

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| title | VARCHAR(300) | NOT NULL | |
| file_path | VARCHAR(500) | NOT NULL | 서버 내 저장 경로 |
| file_type | VARCHAR(20) | | `pdf` / `docx` 등 |
| uploaded_by | BIGINT | FK → users.id | |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

#### admin_logs — 관리자 감사 로그

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | BIGSERIAL | PK | |
| admin_id | BIGINT | FK → users.id NOT NULL | |
| action | VARCHAR(100) | NOT NULL | 수행한 작업 |
| target_type | VARCHAR(50) | | 대상 도메인 |
| target_id | BIGINT | | 대상 ID |
| created_at | TIMESTAMPTZ | NOT NULL DEFAULT now() | |

### 4.3 인덱스 설계

```sql
CREATE INDEX idx_chat_sessions_user_id ON chat_sessions(user_id);
CREATE INDEX idx_chat_messages_session_id ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at DESC);
CREATE INDEX idx_notices_published_at ON notices(published_at DESC);
CREATE INDEX idx_admin_logs_admin_id ON admin_logs(admin_id);
```

---

## 5. AI 연동 설계 (OpenAI API 규격)

Ion의 AI 답변 생성은 **OpenAI API 규격 호환 서버**에 요청을 전송하는 방식으로 구현한다.  
이를 통해 OpenAI GPT, 오픈소스 LLM(Ollama, vLLM 등) 등 어떤 백엔드든 교체 가능하다.

단, 운영 설정은 더 이상 단일 환경변수에 고정하지 않는다.  
관리자는 **여러 개의 LLM 엔드포인트를 관리자 API에서 직접 등록/수정/삭제**할 수 있어야 하며, 각 엔드포인트는 아래 값을 독립적으로 가진다.

- 엔드포인트 이름
- Base URL
- API Key
- 모델명
- 시스템 프롬프트
- Temperature
- Max Tokens
- 활성화 여부
- 기본(Default) 여부

실제 채팅 요청은 **활성화된 기본(Default) 엔드포인트 1개**를 사용한다.  
환경변수는 애플리케이션 최초 기동 시 DB에 기본 엔드포인트를 생성하기 위한 **bootstrap seed 용도**로만 사용한다.

### 5.1 요청 규격 — Chat Completions

```
POST {LLM_BASE_URL}/v1/chat/completions
Authorization: Bearer {LLM_API_KEY}
Content-Type: application/json
```

**Request Body**

```json
{
  "model": "ion-model",
  "messages": [
    {
      "role": "system",
      "content": "당신은 경북소마고 전용 AI 어시스턴트 Ion입니다. ..."
    },
    {
      "role": "user",
      "content": "내일 급식 메뉴가 뭐야?"
    }
  ],
  "stream": true,
  "temperature": 0.7,
  "max_tokens": 1024
}
```

**스트리밍 응답 (stream: true)**

```
data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","choices":[{"delta":{"content":"내일"},"index":0}]}

data: {"id":"chatcmpl-xxx","object":"chat.completion.chunk","choices":[{"delta":{"content":" 급식"},"index":0}]}

data: [DONE]
```

**비스트리밍 응답 (stream: false)**

```json
{
  "id": "chatcmpl-xxx",
  "object": "chat.completion",
  "model": "ion-model",
  "choices": [
    {
      "index": 0,
      "message": {
        "role": "assistant",
        "content": "내일 급식 메뉴는 ..."
      },
      "finish_reason": "stop"
    }
  ],
  "usage": {
    "prompt_tokens": 120,
    "completion_tokens": 85,
    "total_tokens": 205
  }
}
```

### 5.2 Spring Boot 클라이언트 구현 방향

```java
// OpenAiCompatibleClient.java
@Component
public class OpenAiCompatibleClient {

    private final WebClient.Builder webClientBuilder;

    public Flux<String> streamChat(LlmEndpointConfig endpoint, ChatCompletionRequest request) {
        return webClientBuilder
                .baseUrl(endpoint.getBaseUrl())
                .defaultHeader(HttpHeaders.AUTHORIZATION, "Bearer " + endpoint.getApiKey())
                .build();
        
        return webClient.post()
                .uri("/v1/chat/completions")
                .bodyValue(request)
                .retrieve()
                .bodyToFlux(String.class);
    }
}
```

### 5.3 관리자 설정 정책

| 항목 | 정책 |
|----|------|
| 설정 저장소 | DB (`llm_endpoint_configs`) |
| 설정 관리 주체 | 관리자 API |
| 다중 엔드포인트 | 지원 |
| 엔드포인트별 독립 설정 | `baseUrl`, `apiKey`, `model`, `systemPrompt`, `temperature`, `maxTokens` |
| 실제 호출 대상 | `enabled = true` 이면서 `isDefault = true` 인 엔드포인트 |
| 환경변수 역할 | DB가 비어 있을 때 최초 기본 엔드포인트를 생성하는 bootstrap seed |

### 5.4 Bootstrap 환경변수

| 키 | 예시 값 | 설명 |
|----|---------|------|
| `ION_LLM_BOOTSTRAP_NAME` | `default-ollama` | 최초 기본 엔드포인트 이름 |
| `ION_LLM_BOOTSTRAP_BASE_URL` | `http://localhost:11434` | 최초 기본 LLM 서버 주소 |
| `ION_LLM_BOOTSTRAP_API_KEY` | `ollama` | 최초 기본 엔드포인트 API 키 |
| `ION_LLM_BOOTSTRAP_MODEL` | `ion-model` | 최초 기본 엔드포인트 모델명 |
| `ION_LLM_BOOTSTRAP_SYSTEM_PROMPT` | `"당신은 Ion입니다..."` | 최초 기본 엔드포인트 시스템 프롬프트 |
| `ION_LLM_BOOTSTRAP_TEMPERATURE` | `0.7` | 최초 기본 엔드포인트 temperature |
| `ION_LLM_BOOTSTRAP_MAX_TOKENS` | `1024` | 최초 기본 엔드포인트 max tokens |

---

## 6. 메시지 큐 설계 (RabbitMQ)

### 6.1 Exchange / Queue 구성

| 이름 | 종류 | 설명 |
|------|------|------|
| `ion.llm.exchange` | Direct | LLM 요청 라우팅 |
| `ion.llm.request.queue` | Durable Queue | 처리 대기 LLM 요청 |
| `ion.llm.result.queue` | Durable Queue | LLM 처리 결과 |
| `ion.llm.dlq` | Dead Letter Queue | 재처리 실패 메시지 |

### 6.2 메시지 포맷

**LLM 요청 메시지**

```json
{
  "requestId": "uuid-v4",
  "sessionId": "chat-session-uuid",
  "userId": 42,
  "messages": [
    { "role": "system", "content": "..." },
    { "role": "user", "content": "질문 내용" }
  ],
  "stream": true,
  "model": "ion-model",
  "createdAt": "2026-04-21T10:00:00Z"
}
```

**LLM 결과 메시지**

```json
{
  "requestId": "uuid-v4",
  "sessionId": "chat-session-uuid",
  "status": "SUCCESS",
  "content": "AI 답변 전체 텍스트",
  "finishReason": "stop",
  "completedAt": "2026-04-21T10:00:05Z"
}
```

### 6.3 재처리 및 DLQ 정책

- 최대 재시도: **3회**
- 재시도 간격: 1s → 5s → 30s (지수 백오프)
- 3회 실패 시 `ion.llm.dlq`로 이동 후 알림

---

## 7. 캐시 설계 (Redis)

| 키 패턴 | TTL | 내용 |
|---------|-----|------|
| `session:{sessionId}` | 30분 | SSE Emitter 연결 상태 |
| `jwt:refresh:{userId}` | 7일 | Refresh Token |
| `notices:list:{page}` | 10분 | 공지사항 목록 캐시 |
| `notice:{id}` | 30분 | 공지사항 단건 캐시 |
| `user:{userId}` | 1시간 | 사용자 기본 정보 캐시 |

---

## 8. 인증 및 보안

### 8.1 JWT 인증 구조

```
로그인 요청 → 서버 검증 → Access Token(15분) + Refresh Token(7일) 발급
→ Access Token 만료 시 Refresh Token으로 재발급 (Refresh Token Rotation)
→ 로그아웃 시 Redis에서 Refresh Token 삭제
```

| 항목 | 값 |
|------|----|
| 알고리즘 | HS256 |
| Access Token 유효기간 | 15분 |
| Refresh Token 유효기간 | 7일 |
| 저장 위치 | Access: 메모리 / Refresh: Redis |

### 8.2 보안 설정

- HTTPS 강제 적용
- CORS: 허용 도메인 화이트리스트
- 비밀번호: BCrypt (strength=12)
- SQL Injection: JPA Parameterized Query 사용
- 관리자 API: `ADMIN` role 전용 Spring Security 필터 적용

---

## 9. 에러 처리 및 로깅

### 9.1 공통 응답 포맷

```json
{
  "success": true,
  "data": { },
  "error": null,
  "timestamp": "2026-04-21T10:00:00Z"
}
```

오류 시:

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

### 9.2 에러 코드 정의

| 코드 | HTTP | 설명 |
|------|------|------|
| AUTH_001 | 401 | 인증 토큰 없음 또는 만료 |
| AUTH_002 | 401 | 인증 정보 불일치 |
| AUTH_003 | 403 | 권한 없음 |
| CHAT_001 | 404 | 채팅 세션 없음 |
| CHAT_002 | 400 | 메시지 내용 없음 |
| LLM_001 | 503 | LLM 서버 연결 실패 |
| LLM_002 | 504 | LLM 응답 타임아웃 |
| COMMON_001 | 500 | 서버 내부 오류 |
| COMMON_002 | 400 | 요청 유효성 검사 실패 |

### 9.3 로깅 전략

| 레벨 | 대상 |
|------|------|
| INFO | 요청/응답 요약, 인증 이벤트 |
| WARN | 재시도 발생, 느린 쿼리 (> 1s) |
| ERROR | 예외 스택 트레이스, LLM 오류 |
| DEBUG | 개발 환경 전체 요청·쿼리 |

- 로그 파일: `/logs/ion-backend.log`
- 보관 기간: 30일
- 운영 환경 레벨: `INFO`

---

## 10. 개발 환경 및 빌드

### 10.1 기술 스택

| 구분 | 기술 | 버전 |
|------|------|------|
| 언어 | Java | 21 |
| 프레임워크 | Spring Boot | 4.x |
| ORM | Spring Data JPA + Hibernate | |
| DB | PostgreSQL | 16 |
| 캐시 | Spring Data Redis + Lettuce | |
| MQ | Spring AMQP (RabbitMQ) | |
| HTTP 클라이언트 | Spring WebFlux WebClient | |
| 인증 | Spring Security + JJWT | 0.12.x |
| 빌드 | Gradle | 8.x |
| 컨테이너 | Docker + Docker Compose | |

### 10.2 환경변수

```env
# 데이터베이스
SPRING_DATASOURCE_URL=jdbc:postgresql://localhost:5432/ion
SPRING_DATASOURCE_USERNAME=ion
SPRING_DATASOURCE_PASSWORD=secret

# Redis
SPRING_REDIS_HOST=localhost
SPRING_REDIS_PORT=6379

# RabbitMQ
SPRING_RABBITMQ_HOST=localhost
SPRING_RABBITMQ_PORT=5672
SPRING_RABBITMQ_USERNAME=guest
SPRING_RABBITMQ_PASSWORD=guest

# JWT
ION_JWT_SECRET=your-256-bit-secret
ION_JWT_ACCESS_EXPIRY=900
ION_JWT_REFRESH_EXPIRY=604800

# LLM bootstrap seed (최초 1회 DB 초기값)
ION_LLM_BOOTSTRAP_NAME=default-ollama
ION_LLM_BOOTSTRAP_BASE_URL=http://localhost:11434
ION_LLM_BOOTSTRAP_API_KEY=ollama
ION_LLM_BOOTSTRAP_MODEL=ion-model
ION_LLM_BOOTSTRAP_SYSTEM_PROMPT=당신은 경북소마고 전용 AI 어시스턴트 Ion입니다.
ION_LLM_BOOTSTRAP_TEMPERATURE=0.7
ION_LLM_BOOTSTRAP_MAX_TOKENS=1024
```

---

## 11. 테스트 전략

| 단계 | 도구 | 대상 |
|------|------|------|
| 단위 테스트 | JUnit 5 + Mockito | Service 레이어 로직 |
| 통합 테스트 | Spring Boot Test + Testcontainers | DB, Redis, RabbitMQ |
| API 테스트 | MockMvc | Controller 엔드포인트 |
| LLM 클라이언트 테스트 | WireMock | OpenAI API 규격 모킹 |

---

## 12. 개발 일정

---
---

# MVP / Post-MVP 구현 범위

---

## MVP (Minimum Viable Product)

> 핵심 기능만으로 서비스 동작 가능한 최소 범위

### MVP 포함 기능

#### 인증
- [x] 사용자 로그인 (username/password → JWT 발급)
- [x] JWT Access Token 검증 미들웨어
- [x] 로그아웃 (Refresh Token 무효화)

#### 채팅
- [x] 채팅 세션 생성 / 목록 조회
- [x] 메시지 전송 (질의 저장 → LLM 요청)
- [x] SSE 스트리밍 응답 (`GET /stream`)
- [x] 메시지 내역 조회 (페이징)

#### AI 연동
- [x] OpenAI API 규격 클라이언트 (`/v1/chat/completions`)
- [x] 스트리밍 응답 파싱 및 SSE 중계
- [x] 시스템 프롬프트 설정

#### 공지사항
- [x] 공지사항 목록 조회 (페이징)
- [x] 공지사항 단건 조회

#### 데이터베이스
- [x] `users`, `chat_sessions`, `chat_messages` 테이블
- [x] `notices` 테이블 (수동 seed 데이터)
- [x] 기본 인덱스

#### 인프라
- [x] Docker Compose (PostgreSQL + 앱 서버)
- [x] 공통 응답 포맷 (`ApiResponse`)
- [x] 글로벌 예외 핸들러

---

### MVP 미포함 (Post-MVP) 기능

#### 비동기 처리
- [ ] RabbitMQ 메시지 큐 도입
- [ ] LLM Worker 분리 (별도 서비스)
- [ ] Dead Letter Queue 및 재처리 로직

#### 캐싱
- [ ] Redis 기반 공지사항 캐시
- [ ] Redis 기반 사용자 정보 캐시
- [ ] Refresh Token Redis 저장 (MVP는 DB 저장 또는 무상태)

#### RAG (Retrieval-Augmented Generation)
- [ ] `documents` 테이블 및 파일 업로드 API
- [ ] 문서 텍스트 추출 및 벡터 임베딩
- [ ] 벡터 DB 연동 (pgvector 등)
- [ ] 질의 시 관련 문서 검색 후 컨텍스트 삽입
- [ ] `source_refs` 출처 참조 저장

#### 관리자 기능
- [ ] 관리자 전용 인증 가드 (`ADMIN` role)
- [ ] 공지사항 CRUD (관리자)
- [ ] 문서 업로드 / 삭제 (관리자)
- [ ] 감사 로그 (`admin_logs`) 조회
- [ ] 사용자 관리 (비활성화, 권한 변경)

#### 모니터링 및 운영
- [ ] 구조화 로깅 (JSON 포맷, Logstash 연동)
- [ ] Actuator + Prometheus + Grafana
- [ ] Slow Query 경고
- [ ] Sentry 에러 트래킹
- [ ] API Rate Limiting

#### 보안 강화
- [ ] Refresh Token Rotation
- [ ] HTTPS 강제 리다이렉션 설정
- [ ] IP 기반 Rate Limit
- [ ] 비밀번호 변경 API

---

## 구현 우선순위 요약

```
Phase 1 (MVP)
├── 인증 (로그인 / JWT)
├── 채팅 세션 + 메시지 전송
├── OpenAI API 규격 LLM 연동 (스트리밍)
├── SSE 응답 중계
└── 공지사항 조회

Phase 2 (Post-MVP - 인프라)
├── Redis 캐싱
├── RabbitMQ 비동기 처리
└── LLM Worker 분리

Phase 3 (Post-MVP - 기능)
├── 문서 업로드 + RAG 파이프라인
├── 관리자 기능 (공지 CRUD, 문서 관리)
└── 감사 로그

Phase 4 (Post-MVP - 운영)
├── 모니터링 (Prometheus / Grafana)
├── 구조화 로깅
└── Rate Limiting / 보안 강화
```

---

> 본 문서는 초안으로, API 명세 확정 및 팀 협의를 통해 변경될 수 있습니다.  
> API 상세 명세는 [`Ion_API_명세서.md`](./Ion_API_명세서.md)를 참고하세요.	
