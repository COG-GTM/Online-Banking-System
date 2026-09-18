package com.userfront.controller;

import java.math.BigDecimal;
import java.security.Principal;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;

import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.exception.InsufficientFundsException;
import com.userfront.exception.InvalidAmountException;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.util.AmountParser;

@Controller
@RequestMapping("/transfer")
public class TransferController {

    @Autowired
    private TransactionService transactionService;

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/betweenAccounts", method = RequestMethod.GET)
    public String betweenAccounts(Model model) {
        model.addAttribute("transferFrom", "");
        model.addAttribute("transferTo", "");
        model.addAttribute("amount", "");

        return "betweenAccounts";
    }

    @RequestMapping(value = "/betweenAccounts", method = RequestMethod.POST)
    public String betweenAccountsPost(
            @ModelAttribute("transferFrom") String transferFrom,
            @ModelAttribute("transferTo") String transferTo,
            @ModelAttribute("amount") String amount,
            Model model,
            Principal principal
    ) throws Exception {
        BigDecimal transferAmount;
        try {
            transferAmount = AmountParser.parse(amount);
        } catch (InvalidAmountException e) {
            model.addAttribute("error", e.getMessage());
            return "betweenAccounts";
        }

        try {
            transactionService.betweenAccountsTransfer(transferFrom, transferTo, transferAmount, principal.getName());
        } catch (InsufficientFundsException e) {
            model.addAttribute("error", e.getMessage());
            return "betweenAccounts";
        }

        return "redirect:/userFront";
    }
    
    @RequestMapping(value = "/recipient", method = RequestMethod.GET)
    public String recipient(Model model, Principal principal) {
        List<Recipient> recipientList = transactionService.findRecipientList(principal);

        Recipient recipient = new Recipient();

        model.addAttribute("recipientList", recipientList);
        model.addAttribute("recipient", recipient);

        return "recipient";
    }

    @RequestMapping(value = "/recipient/save", method = RequestMethod.POST)
    public String recipientPost(@ModelAttribute("recipient") Recipient recipient, Principal principal) {

        User user = userService.findByUsername(principal.getName());
        recipient.setUser(user);
        transactionService.saveRecipient(recipient);

        return "redirect:/transfer/recipient";
    }

    @RequestMapping(value = "/recipient/edit", method = RequestMethod.GET)
    public String recipientEdit(@RequestParam(value = "recipientName") String recipientName, Model model, Principal principal){

        Recipient recipient = transactionService.findRecipientByName(recipientName);
        List<Recipient> recipientList = transactionService.findRecipientList(principal);

        model.addAttribute("recipientList", recipientList);
        model.addAttribute("recipient", recipient);

        return "recipient";
    }

    @RequestMapping(value = "/recipient/delete", method = RequestMethod.GET)
    @Transactional
    public String recipientDelete(@RequestParam(value = "recipientName") String recipientName, Model model, Principal principal){

        transactionService.deleteRecipientByName(recipientName);

        List<Recipient> recipientList = transactionService.findRecipientList(principal);

        Recipient recipient = new Recipient();
        model.addAttribute("recipient", recipient);
        model.addAttribute("recipientList", recipientList);


        return "recipient";
    }

    @RequestMapping(value = "/toSomeoneElse",method = RequestMethod.GET)
    public String toSomeoneElse(Model model, Principal principal) {
        List<Recipient> recipientList = transactionService.findRecipientList(principal);

        model.addAttribute("recipientList", recipientList);
        model.addAttribute("accountType", "");

        return "toSomeoneElse";
    }

    @RequestMapping(value = "/toSomeoneElse",method = RequestMethod.POST)
    public String toSomeoneElsePost(@ModelAttribute("recipientName") String recipientName, @ModelAttribute("accountType") String accountType, @ModelAttribute("amount") String amount, Model model, Principal principal) {
        BigDecimal transferAmount;
        try {
            transferAmount = AmountParser.parse(amount);
        } catch (InvalidAmountException e) {
            return toSomeoneElseError(e.getMessage(), model, principal);
        }

        Recipient recipient = transactionService.findRecipientByName(recipientName);

        try {
            transactionService.toSomeoneElseTransfer(recipient, accountType, transferAmount, principal.getName());
        } catch (InsufficientFundsException e) {
            return toSomeoneElseError(e.getMessage(), model, principal);
        }

        return "redirect:/userFront";
    }

    private String toSomeoneElseError(String message, Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal));
        model.addAttribute("accountType", "");
        model.addAttribute("error", message);

        return "toSomeoneElse";
    }
}
