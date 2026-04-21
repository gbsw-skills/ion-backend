package com.ion.admin.controller;

import com.ion.common.response.ApiResponse;
import com.ion.common.response.PageResponse;
import com.ion.document.dto.DocumentResponse;
import com.ion.document.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {

    private final DocumentService documentService;

    @PostMapping(consumes = "multipart/form-data")
    public ResponseEntity<ApiResponse<DocumentResponse>> upload(
            @AuthenticationPrincipal Long adminId,
            @RequestPart("title") String title,
            @RequestPart("file") MultipartFile file) {
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.ok(documentService.upload(adminId, title, file)));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<PageResponse<DocumentResponse>>> getDocuments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ResponseEntity.ok(ApiResponse.ok(documentService.getDocuments(page, size)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<ApiResponse<Void>> delete(
            @AuthenticationPrincipal Long adminId,
            @PathVariable Long id) {
        documentService.delete(adminId, id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
