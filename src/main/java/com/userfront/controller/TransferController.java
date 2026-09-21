package com.userfront.controller;

import java.security.Principal;

import javax.persistence.EntityNotFoundException;
import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestParam;

import com.userfront.domain.Recipient;
import com.userfront.domain.User;
import com.userfront.dto.RecipientForm;
import com.userfront.service.TransactionService;
import com.userfront.service.UserService;
import com.userfront.util.Amounts;

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
            Principal principal
    ) {
        User user = userService.findByUsername(principal.getName());
        transactionService.betweenAccountsTransfer(transferFrom, transferTo, Amounts.parse(amount), user.getPrimaryAccount(), user.getSavingsAccount());

        return "redirect:/userFront";
    }
    
    @RequestMapping(value = "/recipient", method = RequestMethod.GET)
    public String recipient(Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal));
        model.addAttribute("recipient", new RecipientForm());

        return "recipient";
    }

    @RequestMapping(value = "/recipient/save", method = RequestMethod.POST)
    public String recipientPost(@Valid @ModelAttribute("recipient") RecipientForm recipientForm, BindingResult bindingResult, Model model, Principal principal) {

        if (bindingResult.hasErrors()) {
            model.addAttribute("recipientList", transactionService.findRecipientList(principal));
            return "recipient";
        }

        User user = userService.findByUsername(principal.getName());
        Recipient recipient = transactionService.findRecipientByName(recipientForm.getName(), user.getUsername());

        if (recipient == null) {
            recipient = new Recipient();
            recipient.setUser(user);
        }

        recipientForm.applyTo(recipient);
        transactionService.saveRecipient(recipient);

        return "redirect:/transfer/recipient";
    }

    @RequestMapping(value = "/recipient/edit", method = RequestMethod.GET)
    public String recipientEdit(@RequestParam(value = "recipientName") String recipientName, Model model, Principal principal){

        Recipient recipient = requireOwnedRecipient(recipientName, principal);

        model.addAttribute("recipientList", transactionService.findRecipientList(principal));
        model.addAttribute("recipient", new RecipientForm(recipient));

        return "recipient";
    }

    @RequestMapping(value = "/recipient/delete", method = RequestMethod.POST)
    public String recipientDelete(@RequestParam(value = "recipientName") String recipientName, Principal principal){

        requireOwnedRecipient(recipientName, principal);
        transactionService.deleteRecipientByName(recipientName, principal.getName());

        return "redirect:/transfer/recipient";
    }

    @RequestMapping(value = "/toSomeoneElse",method = RequestMethod.GET)
    public String toSomeoneElse(Model model, Principal principal) {
        model.addAttribute("recipientList", transactionService.findRecipientList(principal));
        model.addAttribute("accountType", "");

        return "toSomeoneElse";
    }

    @RequestMapping(value = "/toSomeoneElse",method = RequestMethod.POST)
    public String toSomeoneElsePost(@ModelAttribute("recipientName") String recipientName, @ModelAttribute("accountType") String accountType, @ModelAttribute("amount") String amount, Principal principal) {
        User user = userService.findByUsername(principal.getName());
        Recipient recipient = requireOwnedRecipient(recipientName, principal);
        transactionService.toSomeoneElseTransfer(recipient, accountType, Amounts.parse(amount), user.getPrimaryAccount(), user.getSavingsAccount());

        return "redirect:/userFront";
    }

    private Recipient requireOwnedRecipient(String recipientName, Principal principal) {
        Recipient recipient = transactionService.findRecipientByName(recipientName, principal.getName());
        if (recipient == null) {
            throw new EntityNotFoundException("Recipient not found");
        }

        return recipient;
    }
}
