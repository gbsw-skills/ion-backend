package com.ion.notice.service;

import com.ion.common.exception.ErrorCode;
import com.ion.common.exception.IonException;
import com.ion.common.response.PageResponse;
import com.ion.notice.domain.Notice;
import com.ion.notice.dto.NoticeDetailResponse;
import com.ion.notice.dto.NoticeListItemResponse;
import com.ion.notice.repository.NoticeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class NoticeService {

    private final NoticeRepository noticeRepository;

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
                notice.getPublishedAt(),
                notice.getCreatedAt()
        );
    }

    private NoticeListItemResponse toListItem(Notice n) {
        return new NoticeListItemResponse(n.getId(), n.getTitle(), n.getPublishedAt());
    }
}
