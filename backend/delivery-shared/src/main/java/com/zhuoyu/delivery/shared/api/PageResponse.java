package com.zhuoyu.delivery.shared.api;

import java.util.List;

public record PageResponse<T>(
    List<T> items,
    long pageNo,
    long pageSize,
    long total
) {
}
