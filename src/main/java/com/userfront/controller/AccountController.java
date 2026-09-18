package com.userfront.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.PrimaryTransaction;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.SavingsTransaction;
import com.userfront.domain.User;
import com.userfront.exception.InsufficientFundsException;
import com.userfront.exception.InvalidAmountException;
import com.userfront.service.AccountService;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.util.AmountParser;

@Controller
@RequestMapping("/account")
public class AccountController {

	private static final String CONCURRENT_UPDATE_ERROR = "This account was updated by another transaction. Please try again.";
	
	@Autowired
    private UserService userService;
	
	@Autowired
	private AccountService accountService;
	
	@Autowired
	private TransactionService transactionService;
	
	@RequestMapping("/primaryAccount")
	public String primaryAccount(Model model, Principal principal) {
		List<PrimaryTransaction> primaryTransactionList = transactionService.findPrimaryTransactionList(principal.getName());
		
		User user = userService.findByUsername(principal.getName());
        PrimaryAccount primaryAccount = user.getPrimaryAccount();

        model.addAttribute("primaryAccount", primaryAccount);
        model.addAttribute("primaryTransactionList", primaryTransactionList);
		
		return "primaryAccount";
	}

	@RequestMapping("/savingsAccount")
    public String savingsAccount(Model model, Principal principal) {
		List<SavingsTransaction> savingsTransactionList = transactionService.findSavingsTransactionList(principal.getName());
        User user = userService.findByUsername(principal.getName());
        SavingsAccount savingsAccount = user.getSavingsAccount();

        model.addAttribute("savingsAccount", savingsAccount);
        model.addAttribute("savingsTransactionList", savingsTransactionList);

        return "savingsAccount";
    }
	
	@RequestMapping(value = "/deposit", method = RequestMethod.GET)
    public String deposit(Model model) {
        model.addAttribute("accountType", "");
        model.addAttribute("amount", "");

        return "deposit";
    }

    @RequestMapping(value = "/deposit", method = RequestMethod.POST)
    public String depositPOST(@ModelAttribute("amount") String amount, @ModelAttribute("accountType") String accountType, Model model, Principal principal) {
        BigDecimal depositAmount;
        try {
            depositAmount = AmountParser.parse(amount);
        } catch (InvalidAmountException e) {
            model.addAttribute("error", e.getMessage());
            return "deposit";
        }

        try {
            accountService.deposit(accountType, depositAmount, principal);
        } catch (ObjectOptimisticLockingFailureException e) {
            model.addAttribute("error", CONCURRENT_UPDATE_ERROR);
            return "deposit";
        }

        return "redirect:/userFront";
    }
    
    @RequestMapping(value = "/withdraw", method = RequestMethod.GET)
    public String withdraw(Model model) {
        model.addAttribute("accountType", "");
        model.addAttribute("amount", "");

        return "withdraw";
    }

    @RequestMapping(value = "/withdraw", method = RequestMethod.POST)
    public String withdrawPOST(@ModelAttribute("amount") String amount, @ModelAttribute("accountType") String accountType, Model model, Principal principal) {
        BigDecimal withdrawAmount;
        try {
            withdrawAmount = AmountParser.parse(amount);
        } catch (InvalidAmountException e) {
            model.addAttribute("error", e.getMessage());
            return "withdraw";
        }

        try {
            accountService.withdraw(accountType, withdrawAmount, principal);
        } catch (InsufficientFundsException e) {
            model.addAttribute("error", e.getMessage());
            return "withdraw";
        } catch (ObjectOptimisticLockingFailureException e) {
            model.addAttribute("error", CONCURRENT_UPDATE_ERROR);
            return "withdraw";
        }

        return "redirect:/userFront";
    }
}
