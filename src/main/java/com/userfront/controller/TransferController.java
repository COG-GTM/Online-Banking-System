package com.userfront.controller;

import java.math.BigDecimal;
import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.userfront.domain.PrimaryAccount;
import com.userfront.domain.Recipient;
import com.userfront.domain.SavingsAccount;
import com.userfront.domain.User;
import com.userfront.service.InvalidTransactionException;
import com.userfront.service.MoneyAmount;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;

@Controller
@RequestMapping("/transfer")
public class TransferController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @GetMapping("/betweenAccounts")
    public String betweenAccounts(Model model) {
        model.addAttribute("transferFrom", "");
        model.addAttribute("transferTo", "");
        model.addAttribute("amount", "");

        return "betweenAccounts";
    }

    @PostMapping("/betweenAccounts")
    public String betweenAccountsPost(
            @ModelAttribute("transferFrom") String transferFrom,
            @ModelAttribute("transferTo") String transferTo,
            @ModelAttribute("amount") String amount,
            Principal principal
    ) {
        BigDecimal transferAmount = MoneyAmount.parsePositive(amount);

        User user = userService.findByUsername(principal.getName());
        PrimaryAccount primaryAccount = user.getPrimaryAccount();
        SavingsAccount savingsAccount = user.getSavingsAccount();
        transactionService.betweenAccountsTransfer(transferFrom, transferTo, transferAmount, primaryAccount, savingsAccount);

        return "redirect:/userFront";
    }

    @GetMapping("/recipient")
    public String recipient(Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal.getName()));
        model.addAttribute("recipient", new Recipient());

        return "recipient";
    }

    @PostMapping("/recipient/save")
    public String recipientPost(@ModelAttribute("recipient") Recipient recipient, Principal principal) {
        User user = userService.findByUsername(principal.getName());

        if (recipient.getId() != null) {
            Recipient existing = transactionService.findRecipientById(recipient.getId(), principal.getName());
            if (existing == null) {
                throw new InvalidTransactionException("Recipient not found");
            }
        }

        recipient.setUser(user);
        transactionService.saveRecipient(recipient);

        return "redirect:/transfer/recipient";
    }

    @GetMapping("/recipient/edit")
    public String recipientEdit(@RequestParam("recipientName") String recipientName, Model model, Principal principal) {
        Recipient recipient = transactionService.findRecipientByName(recipientName, principal.getName());
        if (recipient == null) {
            return "redirect:/transfer/recipient";
        }

        model.addAttribute("recipientList", transactionService.findRecipientList(principal.getName()));
        model.addAttribute("recipient", recipient);

        return "recipient";
    }

    @PostMapping("/recipient/delete")
    public String recipientDelete(@RequestParam("recipientName") String recipientName, Principal principal) {
        transactionService.deleteRecipientByName(recipientName, principal.getName());

        return "redirect:/transfer/recipient";
    }

    @GetMapping("/toSomeoneElse")
    public String toSomeoneElse(Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal.getName()));
        model.addAttribute("accountType", "");

        return "toSomeoneElse";
    }

    @PostMapping("/toSomeoneElse")
    public String toSomeoneElsePost(@ModelAttribute("recipientName") String recipientName, @ModelAttribute("accountType") String accountType, @ModelAttribute("amount") String amount, Principal principal) {
        BigDecimal transferAmount = MoneyAmount.parsePositive(amount);

        User user = userService.findByUsername(principal.getName());
        Recipient recipient = transactionService.findRecipientByName(recipientName, principal.getName());
        if (recipient == null) {
            throw new InvalidTransactionException("Recipient not found");
        }

        transactionService.toSomeoneElseTransfer(recipient, accountType, transferAmount, user.getPrimaryAccount(), user.getSavingsAccount());

        return "redirect:/userFront";
    }
}
