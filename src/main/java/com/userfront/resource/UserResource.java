package com.userfront.resource;

import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@RestController
@RequestMapping("/api")
@PreAuthorize("hasRole('ADMIN')")
public class UserResource {

    static final int DEFAULT_PAGE_SIZE = 50;
    static final int MAX_PAGE_SIZE = 200;

    @Autowired
    private UserService userService;

    @Autowired
    private TransactionService transactionService;

    @RequestMapping(value = "/user/all", method = RequestMethod.GET)
    public List<User> userList() {
        return userService.findUserList();
    }

    @RequestMapping(value = "/user/primary/transaction", method = RequestMethod.GET)
    public ResponseEntity<List<PrimaryTransaction>> getPrimaryTransactionList(
            @RequestParam("username") String username,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        validatePaging(page, size);
        return pageResponse(transactionService.findPrimaryTransactionPage(username, page, size));
    }

    @RequestMapping(value = "/user/savings/transaction", method = RequestMethod.GET)
    public ResponseEntity<List<SavingsTransaction>> getSavingsTransactionList(
            @RequestParam("username") String username,
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "" + DEFAULT_PAGE_SIZE) int size) {
        validatePaging(page, size);
        return pageResponse(transactionService.findSavingsTransactionPage(username, page, size));
    }

    @RequestMapping("/user/{username}/enable")
    public void enableUser(@PathVariable("username") String username) {
        userService.enableUser(username);
    }

    @RequestMapping("/user/{username}/disable")
    public void diableUser(@PathVariable("username") String username) {
        userService.disableUser(username);
    }

    private static void validatePaging(int page, int size) {
        if (page < 0 || size < 1 || size > MAX_PAGE_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST,
                    "page must be >= 0 and size between 1 and " + MAX_PAGE_SIZE);
        }
    }

    private static <T> ResponseEntity<List<T>> pageResponse(Page<T> page) {
        HttpHeaders headers = new HttpHeaders();
        headers.set("X-Total-Count", String.valueOf(page.getTotalElements()));
        headers.set("X-Total-Pages", String.valueOf(page.getTotalPages()));
        headers.set("X-Page", String.valueOf(page.getNumber()));
        headers.set("X-Page-Size", String.valueOf(page.getSize()));
        return new ResponseEntity<>(page.getContent(), headers, HttpStatus.OK);
    }
}
