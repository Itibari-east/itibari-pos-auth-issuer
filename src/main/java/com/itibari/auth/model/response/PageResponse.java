package com.itibari.auth.model.response;

import org.springframework.data.domain.Page;

import java.util.List;

public record PageResponse<T>(
        long totalElements,
        int pageSize,
        int totalPages,
        boolean last,
        boolean first,
        int pageNumber,
        List<T> content
) {
    public static <T> PageResponse<T> from(Page<T> page) {
        return new PageResponse<>(
                page.getTotalElements(),
                page.getNumberOfElements(),
                page.getTotalPages(),
                page.isLast(),
                page.isFirst(),
                page.getNumber(),
                page.getContent()
        );
    }
}
