package com.userfront.service.UserServiceImpl;

import java.sql.Savepoint;
import java.sql.Timestamp;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

import com.userfront.dao.PrimaryAccountDao;
import com.userfront.dao.SavingsAccountDao;

/**
 * Claims account numbers in one table shared by primary and savings accounts.
 * The claim joins the caller's signup transaction, so it uses the signup's connection
 * and disappears if the signup rolls back. A number claimed concurrently fails on the
 * primary key and is rolled back to a savepoint, leaving the signup transaction usable.
 */
@Component
public class AccountNumberAllocator {

    static final String INSERT_RESERVATION =
            "insert into account_number_reservation (account_number, reserved_at) values (?, ?)";

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Autowired
    private PrimaryAccountDao primaryAccountDao;

    @Autowired
    private SavingsAccountDao savingsAccountDao;

    @Transactional(propagation = Propagation.MANDATORY)
    public boolean reserve(int accountNumber) {
        if (primaryAccountDao.findByAccountNumber(accountNumber) != null
                || savingsAccountDao.findByAccountNumber(accountNumber) != null) {
            return false;
        }
        return jdbcTemplate.execute((ConnectionCallback<Boolean>) connection -> {
            Savepoint savepoint = connection.setSavepoint();
            try {
                jdbcTemplate.update(INSERT_RESERVATION, accountNumber, new Timestamp(System.currentTimeMillis()));
            } catch (DuplicateKeyException alreadyReserved) {
                connection.rollback(savepoint);
                return false;
            }
            connection.releaseSavepoint(savepoint);
            return true;
        });
    }
}
