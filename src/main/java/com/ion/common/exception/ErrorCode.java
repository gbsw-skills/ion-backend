package com.ion.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {
    AUTH_001(HttpStatus.UNAUTHORIZED, "인증 토큰이 없거나 만료되었습니다."),
    AUTH_002(HttpStatus.UNAUTHORIZED, "아이디 또는 비밀번호가 일치하지 않습니다."),
    AUTH_003(HttpStatus.FORBIDDEN, "해당 리소스에 대한 권한이 없습니다."),
    CHAT_001(HttpStatus.NOT_FOUND, "채팅 세션을 찾을 수 없습니다."),
    CHAT_002(HttpStatus.BAD_REQUEST, "메시지 내용이 비어 있습니다."),
    CHAT_003(HttpStatus.NOT_FOUND, "채팅 메시지를 찾을 수 없습니다."),
    LLM_001(HttpStatus.SERVICE_UNAVAILABLE, "LLM 서버에 연결할 수 없습니다."),
    LLM_002(HttpStatus.GATEWAY_TIMEOUT, "LLM 응답 시간이 초과되었습니다."),
    LLM_003(HttpStatus.SERVICE_UNAVAILABLE, "활성화된 기본 LLM 엔드포인트가 없습니다."),
    LLM_004(HttpStatus.CONFLICT, "동일한 이름의 LLM 엔드포인트가 이미 존재합니다."),
    LLM_005(HttpStatus.BAD_REQUEST, "최소 1개의 활성화된 LLM 엔드포인트가 필요합니다."),
    LLM_006(HttpStatus.BAD_REQUEST, "기본 LLM 엔드포인트는 활성화 상태여야 합니다."),
    LLM_007(HttpStatus.NOT_FOUND, "LLM 엔드포인트를 찾을 수 없습니다."),
    NOTICE_001(HttpStatus.NOT_FOUND, "공지사항을 찾을 수 없습니다."),
    DOCUMENT_001(HttpStatus.NOT_FOUND, "문서를 찾을 수 없습니다."),
    DOCUMENT_002(HttpStatus.BAD_REQUEST, "지원하지 않는 파일 형식입니다."),
    DOCUMENT_003(HttpStatus.PAYLOAD_TOO_LARGE, "파일 크기 제한(50MB)을 초과했습니다."),
    COMMON_001(HttpStatus.INTERNAL_SERVER_ERROR, "서버 내부 오류가 발생했습니다."),
    COMMON_002(HttpStatus.BAD_REQUEST, "요청 유효성 검사에 실패했습니다.");

    private final HttpStatus status;
    private final String message;

    ErrorCode(HttpStatus status, String message) {
        this.status = status;
        this.message = message;
    }

    public HttpStatus getStatus() { return status; }
    public String getMessage() { return message; }
}
