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
     * Compare-and-set on the balance column: the balance is read with SELECT ... FOR UPDATE, so on InnoDB
     * it is a current (not snapshot) read and the row stays locked until commit; on MyISAM (the
     * MySQL5Dialect default, no row locks) the conditional UPDATE is still atomic and retried. Either way
     * the balance can never go negative and the value written is the one recorded in the ledger row.
     */
    static BigDecimal adjust(Function<Long, BigDecimal> balanceOf, BalanceSwap swap, Long accountId, BigDecimal delta) {
        for (int attempt = 0; attempt < MAX_ATTEMPTS; attempt++) {
            BigDecimal current = balanceOf.apply(accountId);
            if (current == null) {
                throw new IllegalStateException("Account not found.");
            }
            BigDecimal updated = current.add(delta);
            if (delta.signum() < 0 && updated.signum() < 0) {
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
