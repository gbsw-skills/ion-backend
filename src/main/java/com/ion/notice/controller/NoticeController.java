package com.ion.notice.controller;

import com.ion.common.response.ApiResponse;
import com.ion.common.response.PageResponse;
import com.ion.notice.dto.NoticeDetailResponse;
import com.ion.notice.dto.NoticeListItemResponse;
import com.ion.notice.service.NoticeService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/notices")
@RequiredArgsConstructor
public class NoticeController {

    private final NoticeService noticeService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<NoticeListItemResponse>>> getNotices(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getNotices(keyword, page, size)));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<NoticeDetailResponse>> getNotice(@PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.ok(noticeService.getNotice(id)));
    }
}
