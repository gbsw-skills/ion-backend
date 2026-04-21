package com.ion.document.service;

import com.ion.admin.service.AdminLogService;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.common.response.PageResponse;
import com.ion.document.domain.Document;
import com.ion.document.dto.DocumentResponse;
import com.ion.document.repository.DocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.Instant;
import java.util.Locale;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class DocumentService {

    private static final long MAX_FILE_SIZE = 50L * 1024 * 1024;

    private final DocumentRepository documentRepository;
    private final AdminLogService adminLogService;

    @Value("${ion.storage.documents-dir}")
    private String documentsDir;

    @Transactional
    public DocumentResponse upload(Long adminId, String title, MultipartFile file) {
        validateFile(title, file);

        String fileType = extractExtension(file.getOriginalFilename());
        String storedFileName = UUID.randomUUID() + "." + fileType;
        Path storageDirectory = Path.of(documentsDir).toAbsolutePath().normalize();
        Path targetPath = storageDirectory.resolve(storedFileName);

        try {
            Files.createDirectories(storageDirectory);
            try (InputStream inputStream = file.getInputStream()) {
                Files.copy(inputStream, targetPath, StandardCopyOption.REPLACE_EXISTING);
            }
        } catch (IOException e) {
            throw new IonException(ErrorCode.COMMON_001);
        }

        Document document = documentRepository.save(Document.builder()
                .title(title.trim())
                .filePath(targetPath.toString())
                .fileType(fileType)
                .uploadedBy(adminId)
                .build());

        adminLogService.log(adminId, "UPLOAD_DOCUMENT", "document", document.getId());
        return toResponse(document);
    }

    @Transactional(readOnly = true)
    public PageResponse<DocumentResponse> getDocuments(int page, int size) {
        return PageResponse.from(documentRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(page, size))
                .map(this::toResponse));
    }

    @Transactional
    public void delete(Long adminId, Long id) {
        Document document = documentRepository.findById(id)
                .orElseThrow(() -> new IonException(ErrorCode.DOCUMENT_001));

        try {
            Files.deleteIfExists(Path.of(document.getFilePath()));
        } catch (IOException e) {
            throw new IonException(ErrorCode.COMMON_001);
        }

        documentRepository.delete(document);
        adminLogService.log(adminId, "DELETE_DOCUMENT", "document", id);
    }

    private void validateFile(String title, MultipartFile file) {
        if (!StringUtils.hasText(title)) {
            throw new IonException(ErrorCode.COMMON_002);
        }
        if (file == null || file.isEmpty()) {
            throw new IonException(ErrorCode.COMMON_002);
        }
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new IonException(ErrorCode.DOCUMENT_003);
        }

        String extension = extractExtension(file.getOriginalFilename());
        if (!"pdf".equals(extension) && !"docx".equals(extension)) {
            throw new IonException(ErrorCode.DOCUMENT_002);
        }
    }

    private String extractExtension(String filename) {
        if (!StringUtils.hasText(filename) || !filename.contains(".")) {
            throw new IonException(ErrorCode.DOCUMENT_002);
        }
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase(Locale.ROOT);
    }

    private DocumentResponse toResponse(Document document) {
        return new DocumentResponse(
                document.getId(),
                document.getTitle(),
                document.getFileType(),
                document.getCreatedAt()
        );
    }
}
