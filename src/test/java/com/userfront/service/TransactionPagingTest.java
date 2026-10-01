package com.userfront.service;

import static org.junit.Assert.assertEquals;

import org.junit.Test;
import org.springframework.data.domain.Pageable;

public class TransactionPagingTest {

    @Test
    public void defaultsToFirstPageOfDefaultSize() {
        Pageable pageable = TransactionPaging.of(null, null);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(TransactionPaging.DEFAULT_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    public void clampsOversizedPageSize() {
        assertEquals(TransactionPaging.MAX_PAGE_SIZE, TransactionPaging.of(0, 1000000).getPageSize());
    }

    @Test
    public void replacesInvalidValuesWithDefaults() {
        Pageable pageable = TransactionPaging.of(-3, 0);

        assertEquals(0, pageable.getPageNumber());
        assertEquals(TransactionPaging.DEFAULT_PAGE_SIZE, pageable.getPageSize());
    }

    @Test
    public void keepsValidValues() {
        Pageable pageable = TransactionPaging.of(4, 50);

        assertEquals(4, pageable.getPageNumber());
        assertEquals(50, pageable.getPageSize());
    }
}
