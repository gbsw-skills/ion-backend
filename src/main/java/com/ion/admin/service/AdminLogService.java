package com.ion.admin.service;

import com.ion.admin.domain.AdminLog;
import com.ion.admin.dto.AdminLogResponse;
import com.ion.admin.repository.AdminLogRepository;
import com.ion.common.response.PageResponse;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Map;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AdminLogService {

    private final AdminLogRepository adminLogRepository;
    private final UserRepository userRepository;

    @Transactional
    public void log(Long adminId, String action, String targetType, Long targetId) {
        adminLogRepository.save(AdminLog.builder()
                .adminId(adminId)
                .action(action)
                .targetType(targetType)
                .targetId(targetId)
                .build());
    }

    @Transactional(readOnly = true)
    public PageResponse<AdminLogResponse> getLogs(Long adminId, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var logPage = adminId == null
                ? adminLogRepository.findAllByOrderByCreatedAtDesc(pageable)
                : adminLogRepository.findByAdminIdOrderByCreatedAtDesc(adminId, pageable);

        Map<Long, String> adminNames = userRepository.findAllById(
                        logPage.getContent().stream().map(AdminLog::getAdminId).distinct().toList()
                ).stream()
                .collect(Collectors.toMap(com.ion.user.domain.User::getId, com.ion.user.domain.User::getDisplayName));

        return PageResponse.from(logPage.map(log -> new AdminLogResponse(
                log.getId(),
                adminNames.getOrDefault(log.getAdminId(), "알 수 없음"),
                log.getAction(),
                log.getTargetType(),
                log.getTargetId(),
                log.getCreatedAt()
        )));
    }
}
