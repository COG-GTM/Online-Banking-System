package com.userfront.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.redirectedUrlPattern;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

@SpringBootTest
@AutoConfigureMockMvc
class TransactionLogSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void transactionCsvExportsRequireLogin() throws Exception {
        for (String url : new String[] {"/account/primaryAccount/transactions.csv", "/account/savingsAccount/transactions.csv"}) {
            mockMvc.perform(get(url))
                    .andExpect(status().is3xxRedirection())
                    .andExpect(redirectedUrlPattern("**/index"));
        }
    }
}
