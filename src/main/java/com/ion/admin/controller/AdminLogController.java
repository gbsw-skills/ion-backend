package com.ion.admin.controller;

import com.ion.admin.dto.AdminLogResponse;
import com.ion.admin.service.AdminLogService;
import com.ion.common.response.ApiResponse;
import com.ion.common.response.PageResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/logs")
@RequiredArgsConstructor
public class AdminLogController {

    private final AdminLogService adminLogService;

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<AdminLogResponse>>> getLogs(
            @RequestParam(required = false) Long adminId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(adminLogService.getLogs(adminId, page, size)));
    }
}
