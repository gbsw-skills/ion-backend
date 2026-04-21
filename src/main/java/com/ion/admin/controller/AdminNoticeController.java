package com.ion.admin.controller;

import com.ion.common.response.ApiResponse;
import com.ion.notice.dto.AdminNoticeResponse;
import com.ion.notice.dto.NoticeUpsertRequest;
import com.ion.notice.service.NoticeService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/notices")
@RequiredArgsConstructor
public class AdminNoticeController {

    private final NoticeService noticeService;

    @PostMapping
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> create(
            @AuthenticationPrincipal Long adminId,
            @Valid @RequestBody NoticeUpsertRequest request) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(noticeService.createNotice(adminId, request)));
    }

    @PutMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminNoticeResponse>> update(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody NoticeUpsertRequest request) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.updateNotice(adminId, id, request)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long id) {
        noticeService.deleteNotice(adminId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
