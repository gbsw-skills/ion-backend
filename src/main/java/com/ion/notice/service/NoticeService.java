package com.ion.notice.service;

import com.ion.admin.service.AdminLogService;
import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.common.response.PageResponse;
import com.ion.notice.domain.Notice;
import com.ion.notice.dto.AdminNoticeResponse;
import com.ion.notice.dto.NoticeDetailResponse;
import com.ion.notice.dto.NoticeListItemResponse;
import com.ion.notice.dto.NoticeUpsertRequest;
import com.ion.notice.repository.NoticeRepository;
import com.ion.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;
    private final UserRepository userRepository;
    private final AdminLogService adminLogService;

    @Transactional(readOnly = true)
    public PageResponse<NoticeListItemResponse> getNotices(String keyword, int page, int size) {
        var pageable = PageRequest.of(page, size);
        var noticePage = StringUtils.hasText(keyword)
                ? noticeRepository.findByTitleContainingIgnoreCaseOrderByPublishedAtDesc(keyword, pageable)
                : noticeRepository.findAllByOrderByPublishedAtDesc(pageable);
        return PageResponse.from(noticePage.map(this::toListItem));
    }

    @Transactional(readOnly = true)
    public NoticeDetailResponse getNotice(Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IonException(ErrorCode.NOTICE_001));
        return new NoticeDetailResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getContent(),
                resolveAuthorName(notice.getAuthorId()),
                notice.getPublishedAt(),
                notice.getCreatedAt()
        );
    }

    @Transactional
    public AdminNoticeResponse createNotice(Long adminId, NoticeUpsertRequest request) {
        Notice notice = noticeRepository.save(Notice.builder()
                .title(request.title().trim())
                .content(request.content().trim())
                .authorId(adminId)
                .publishedAt(request.publishedAt())
                .build());

        adminLogService.log(adminId, "CREATE_NOTICE", "notice", notice.getId());
        return toAdminResponse(notice);
    }

    @Transactional
    public AdminNoticeResponse updateNotice(Long adminId, Long id, NoticeUpsertRequest request) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IonException(ErrorCode.NOTICE_001));

        notice.update(request.title().trim(), request.content().trim(), request.publishedAt());
        adminLogService.log(adminId, "UPDATE_NOTICE", "notice", notice.getId());
        return toAdminResponse(notice);
    }

    @Transactional
    public void deleteNotice(Long adminId, Long id) {
        Notice notice = noticeRepository.findById(id)
                .orElseThrow(() -> new IonException(ErrorCode.NOTICE_001));
        noticeRepository.delete(notice);
        adminLogService.log(adminId, "DELETE_NOTICE", "notice", id);
    }

    private NoticeListItemResponse toListItem(Notice n) {
        return new NoticeListItemResponse(n.getId(), n.getTitle(), resolveAuthorName(n.getAuthorId()), n.getPublishedAt());
    }

    private AdminNoticeResponse toAdminResponse(Notice notice) {
        return new AdminNoticeResponse(
                notice.getId(),
                notice.getTitle(),
                notice.getPublishedAt(),
                notice.getCreatedAt()
        );
    }

    private String resolveAuthorName(Long authorId) {
        if (authorId == null) {
            return null;
        }
        return userRepository.findById(authorId)
                .map(user -> user.getDisplayName())
                .orElse("알 수 없음");
    }
}
