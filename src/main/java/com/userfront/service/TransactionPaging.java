package com.userfront.service;

import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

public final class TransactionPaging {

    public static final int DEFAULT_PAGE_SIZE = 20;
    public static final int MAX_PAGE_SIZE = 100;

    private TransactionPaging() {}

    public static Pageable of(Integer page, Integer size) {
        int safePage = (page == null || page < 0) ? 0 : page;
        int safeSize = (size == null || size < 1) ? DEFAULT_PAGE_SIZE : Math.min(size, MAX_PAGE_SIZE);
        return PageRequest.of(safePage, safeSize);
    }
}
