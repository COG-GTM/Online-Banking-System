package com.userfront.controller;

import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;

import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.service.Amounts;
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
        transactionService.betweenAccountsTransfer(transferFrom, transferTo, Amounts.parse(amount), principal);

        return "redirect:/userFront";
    }

    @GetMapping("/recipient")
    public String recipient(Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal));
        model.addAttribute("recipient", new Recipient());

        return "recipient";
    }

    @PostMapping("/recipient/save")
    public String recipientPost(@ModelAttribute("recipient") Recipient recipient, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        recipient.setUser(user);
        transactionService.saveRecipient(recipient);

        return "redirect:/transfer/recipient";
    }

    @GetMapping("/recipient/edit")
    public String recipientEdit(@RequestParam(value = "recipientName") String recipientName, Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal));
        model.addAttribute("recipient", transactionService.findRecipientByName(recipientName, principal));

        return "recipient";
    }

    @PostMapping("/recipient/delete")
    public String recipientDelete(@RequestParam(value = "recipientName") String recipientName, Principal principal) {
        transactionService.deleteRecipientByName(recipientName, principal);

        return "redirect:/transfer/recipient";
    }

    @GetMapping("/toSomeoneElse")
    public String toSomeoneElse(Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal));
        model.addAttribute("accountType", "");

        return "toSomeoneElse";
    }

    @PostMapping("/toSomeoneElse")
    public String toSomeoneElsePost(@ModelAttribute("recipientName") String recipientName,
                                    @ModelAttribute("accountType") String accountType,
                                    @ModelAttribute("amount") String amount,
                                    Principal principal) {
        transactionService.toSomeoneElseTransfer(recipientName, accountType, Amounts.parse(amount), principal);

        return "redirect:/userFront";
    }
}
