package com.userfront.controller;

import java.security.Principal;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;

import com.userfront.domain.User;
import com.userfront.service.UserService;
import com.userfront.web.ProfileForm;

import jakarta.validation.Valid;

@Controller
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @RequestMapping(value = "/profile", method = RequestMethod.GET)
    public String profile(Principal principal, Model model) {
        User user = userService.findByUsername(principal.getName());
        model.addAttribute("profileForm", toForm(user));
        addAccountNumbers(user, model);

        return "profile";
    }

    @RequestMapping(value = "/profile", method = RequestMethod.POST)
    public String profilePost(@Valid @ModelAttribute("profileForm") ProfileForm profileForm, BindingResult bindingResult, Principal principal, Model model) {
        if (bindingResult.hasErrors()) {
            addAccountNumbers(userService.findByUsername(principal.getName()), model);

            return "profile";
        }

        User user = userService.findByUsername(principal.getName());
        user.setFirstName(profileForm.getFirstName());
        user.setLastName(profileForm.getLastName());
        user.setEmail(profileForm.getEmail());
        user.setPhone(profileForm.getPhone());

        userService.saveUser(user);

        model.addAttribute("profileForm", toForm(user));
        addAccountNumbers(user, model);

        return "profile";
    }

    private void addAccountNumbers(User user, Model model) {
        model.addAttribute("primaryAccountNumber", user.getPrimaryAccount().getAccountNumber());
        model.addAttribute("savingsAccountNumber", user.getSavingsAccount().getAccountNumber());
    }

    private ProfileForm toForm(User user) {
        ProfileForm form = new ProfileForm();
        form.setUsername(user.getUsername());
        form.setFirstName(user.getFirstName());
        form.setLastName(user.getLastName());
        form.setEmail(user.getEmail());
        form.setPhone(user.getPhone());

        return form;
    }
}
