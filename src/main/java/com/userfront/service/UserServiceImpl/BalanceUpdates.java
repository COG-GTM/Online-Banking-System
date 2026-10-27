package com.userfront.service.UserServiceImpl;

import java.math.BigDecimal;
import java.util.function.Function;

import com.userfront.service.InsufficientFundsException;
import com.userfront.service.InvalidTransactionException;

final class BalanceUpdates {

    private static final int MAX_ATTEMPTS = 10;

    private BalanceUpdates() {
    }

    /*
     * Compare-and-set on the balance column: each UPDATE is atomic on its own (the MySQL5Dialect
     * tables are MyISAM, so there are no row locks), it can never drive the balance negative, and
     * the balance it writes is exactly the one recorded in this operation's ledger row.
     */
    static BigDecimal adjust(Function<Long, BigDecimal> balanceOf, BalanceSwap swap, Long accountId, BigDecimal delta) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            BigDecimal current = balanceOf.apply(accountId);
            if (current == null) {
                throw new IllegalStateException("Account not found.");
            }
            BigDecimal updated = current.add(delta);
            if (updated.signum() < 0) {
                throw new InsufficientFundsException();
            }
            if (swap.compareAndSet(accountId, current, updated) == 1) {
                return updated;
            }
        }
        throw new InvalidTransactionException("The account is busy. Please try again.");
    }

    static boolean isPrimary(String accountType) {
        return "Primary".equalsIgnoreCase(accountType);
    }

    static boolean isSavings(String accountType) {
        return "Savings".equalsIgnoreCase(accountType);
    }

    static InvalidTransactionException unknownAccountType() {
        return new InvalidTransactionException("Please select the Primary or Savings account.");
    }

    @FunctionalInterface
    interface BalanceSwap {
        int compareAndSet(Long accountId, BigDecimal expected, BigDecimal updated);
    }
}
