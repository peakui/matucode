package com.peakui.common.result;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

/**
 * 分页结果。
 */
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PageResponse<T> {

    private Long pageNum;
    private Long pageSize;
    private Long total;
    private Long totalPages;
    private List<T> records;

    public static <T> PageResponse<T> of(long pageNum, long pageSize, long total, List<T> records) {
        long totalPages = pageSize <= 0 ? 0 : (long) Math.ceil(total * 1.0 / pageSize);
        return PageResponse.<T>builder()
                .pageNum(pageNum)
                .pageSize(pageSize)
                .total(total)
                .totalPages(totalPages)
                .records(records)
                .build();
    }
}
